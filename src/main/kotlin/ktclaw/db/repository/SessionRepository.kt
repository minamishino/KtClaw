package ktclaw.db.repository

import ktclaw.db.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.Row
import java.time.Instant
import java.util.UUID

/**
 * Session Repository - 会话数据访问层
 * 使用 R2DBC DatabaseClient 实现，支持 suspend 函数和 Flow 流式查询
 */
class SessionRepository(databaseClient: DatabaseClient) : R2DBCRepository(databaseClient) {

    /**
     * 根据 ID 查询会话
     */
    suspend fun findById(id: UUID): SessionDTO? = fetchOne(
        """
        SELECT s.*, c.channel_id as channel_external_id, a.name as agent_name
        FROM sessions s
        JOIN channels c ON s.channel_id = c.id
        LEFT JOIN agents a ON s.agent_id = a.id
        WHERE s.id = :id
        """,
        mapOf("id" to id)
    )

    /**
     * 根据会话键查询
     */
    suspend fun findBySessionKey(sessionKey: String): SessionDTO? = fetchOne(
        """
        SELECT s.*, c.channel_id as channel_external_id, a.name as agent_name
        FROM sessions s
        JOIN channels c ON s.channel_id = c.id
        LEFT JOIN agents a ON s.agent_id = a.id
        WHERE s.session_key = :sessionKey
        """,
        mapOf("sessionKey" to sessionKey)
    )

    /**
     * 根据用户 ID 查询活跃会话 - 使用 Flow
     */
    fun findActiveByUserIdFlow(userId: String, limit: Int, offset: Int): Flow<SessionDTO> = fetchMany(
        """
        SELECT s.*, c.channel_id as channel_external_id, a.name as agent_name
        FROM sessions s
        JOIN channels c ON s.channel_id = c.id
        LEFT JOIN agents a ON s.agent_id = a.id
        WHERE s.user_id = :userId AND s.is_active = true
        ORDER BY s.last_message_at DESC NULLS LAST
        LIMIT :limit OFFSET :offset
        """,
        mapOf(
            "userId" to userId,
            "limit" to limit,
            "offset" to offset
        )
    )

    /**
     * 根据用户 ID 查询活跃会话 - suspend 版本
     */
    suspend fun findActiveByUserId(userId: String, limit: Int, offset: Int): List<SessionDTO> {
        return findActiveByUserIdFlow(userId, limit, offset).toList()
    }

    /**
     * 根据频道 ID 查询会话 - 使用 Flow
     */
    fun findByChannelIdFlow(channelId: UUID, limit: Int, offset: Int): Flow<SessionDTO> = fetchMany(
        """
        SELECT s.*, c.channel_id as channel_external_id, a.name as agent_name
        FROM sessions s
        JOIN channels c ON s.channel_id = c.id
        LEFT JOIN agents a ON s.agent_id = a.id
        WHERE s.channel_id = :channelId
        ORDER BY s.last_message_at DESC NULLS LAST
        LIMIT :limit OFFSET :offset
        """,
        mapOf(
            "channelId" to channelId,
            "limit" to limit,
            "offset" to offset
        )
    )

    /**
     * 根据频道 ID 查询会话 - suspend 版本
     */
    suspend fun findByChannelId(channelId: UUID, limit: Int, offset: Int): List<SessionDTO> {
        return findByChannelIdFlow(channelId, limit, offset).toList()
    }

    /**
     * 获取过期的会话 - 使用 Flow
     */
    fun findExpiredSessionsFlow(beforeTime: Instant): Flow<SessionDTO> = fetchMany(
        """
        SELECT s.*, c.channel_id as channel_external_id, a.name as agent_name
        FROM sessions s
        JOIN channels c ON s.channel_id = c.id
        LEFT JOIN agents a ON s.agent_id = a.id
        WHERE s.is_active = true AND s.last_message_at < :beforeTime
        """,
        mapOf("beforeTime" to beforeTime)
    )

    /**
     * 获取过期的会话 - suspend 版本
     */
    suspend fun findExpiredSessions(beforeTime: Instant): List<SessionDTO> {
        return findExpiredSessionsFlow(beforeTime).toList()
    }

    /**
     * 统计用户的活跃会话数量
     */
    suspend fun countActiveByUserId(userId: String): Long {
        return fetchOne<Long>(
            "SELECT COUNT(*) as count FROM sessions WHERE user_id = :userId AND is_active = true",
            mapOf("userId" to userId)
        ) ?: 0L
    }

    /**
     * 获取或创建会话
     */
    suspend fun getOrCreate(
        sessionKey: String,
        userId: String,
        channelId: UUID,
        agentId: UUID? = null,
        title: String? = null
    ): SessionDTO {
        val existing = findBySessionKey(sessionKey)
        if (existing != null) return existing

        val id = executeInsert(
            """
            INSERT INTO sessions (
                id, session_key, user_id, channel_id, agent_id, title,
                context_window, is_active, created_at, updated_at, metadata
            ) VALUES (
                gen_random_uuid(), :sessionKey, :userId, :channelId, :agentId, :title,
                10, true, NOW(), NOW(), '{}'::jsonb
            )
            """,
            mapOf(
                "sessionKey" to sessionKey,
                "userId" to userId,
                "channelId" to channelId,
                "agentId" to agentId,
                "title" to title
            )
        )

        return findById(UUID.nameUUIDFromBytes(id.toString().toByteArray()))
            ?: throw IllegalStateException("Failed to create session")
    }

    /**
     * 更新最后消息时间
     */
    suspend fun updateLastMessageTime(sessionId: UUID): Int {
        return executeUpdate(
            "UPDATE sessions SET last_message_at = NOW(), updated_at = NOW() WHERE id = :id",
            mapOf("id" to sessionId)
        )
    }

    /**
     * 批量关闭过期会话
     */
    suspend fun closeExpiredSessions(timeoutMinutes: Int): Int {
        val cutoffTime = Instant.now().minusSeconds(timeoutMinutes * 60L)
        return executeUpdate(
            """
            UPDATE sessions 
            SET is_active = false, updated_at = NOW() 
            WHERE is_active = true AND last_message_at < :cutoffTime
            """,
            mapOf("cutoffTime" to cutoffTime)
        )
    }

    /**
     * 获取会话统计信息 - 使用并行查询
     */
    suspend fun getSessionStats(): SessionStatsDTO = coroutineScope {
        val totalDeferred = async {
            fetchOne<Long>("SELECT COUNT(*) as count FROM sessions") ?: 0L
        }
        val activeDeferred = async {
            fetchOne<Long>("SELECT COUNT(*) as count FROM sessions WHERE is_active = true") ?: 0L
        }
        val avgContextDeferred = async {
            fetchOne<Double>(
                """
                SELECT AVG(context_window) as avg FROM sessions
                """
            ) ?: 0.0
        }

        val total = totalDeferred.await()
        val active = activeDeferred.await()

        SessionStatsDTO(
            totalSessions = total,
            activeSessions = active,
            inactiveSessions = total - active,
            avgContextWindow = avgContextDeferred.await()
        )
    }

    /**
     * 获取每日会话创建统计 - 使用 Flow
     */
    fun getDailySessionStatsFlow(days: Int = 30): Flow<DailySessionStatDTO> = fetchMany(
        """
        SELECT DATE(created_at) as date, COUNT(*) as count
        FROM sessions
        GROUP BY DATE(created_at)
        ORDER BY date DESC
        LIMIT :limit
        """,
        mapOf("limit" to days)
    )

    /**
     * 获取每日会话创建统计 - suspend 版本
     */
    suspend fun getDailySessionStats(days: Int = 30): List<DailySessionStatDTO> {
        return getDailySessionStatsFlow(days).toList()
    }

    /**
     * 更新会话标题
     */
    suspend fun updateTitle(sessionId: UUID, title: String): Int {
        return executeUpdate(
            "UPDATE sessions SET title = :title, updated_at = NOW() WHERE id = :id",
            mapOf("id" to sessionId, "title" to title)
        )
    }

    /**
     * 更新会话 Agent
     */
    suspend fun updateAgent(sessionId: UUID, agentId: UUID?): Int {
        return executeUpdate(
            "UPDATE sessions SET agent_id = :agentId, updated_at = NOW() WHERE id = :id",
            mapOf("id" to sessionId, "agentId" to agentId)
        )
    }

    /**
     * 关闭会话
     */
    suspend fun closeSession(sessionId: UUID): Int {
        return executeUpdate(
            "UPDATE sessions SET is_active = false, updated_at = NOW() WHERE id = :id",
            mapOf("id" to sessionId)
        )
    }

    /**
     * 删除会话
     */
    suspend fun delete(sessionId: UUID): Int {
        return executeUpdate(
            "DELETE FROM sessions WHERE id = :id",
            mapOf("id" to sessionId)
        )
    }

    @Suppress("UNCHECKED_CAST")
    override inline fun <reified T : Any> mapRowToType(row: Row): T {
        return when (T::class) {
            SessionDTO::class -> SessionDTO(
                id = row.get("id", UUID::class.java)!!,
                sessionKey = row.get("session_key", String::class.java)!!,
                userId = row.get("user_id", String::class.java)!!,
                channelId = row.get("channel_id", UUID::class.java)!!,
                agentId = row.get("agent_id", UUID::class.java),
                title = row.get("title", String::class.java),
                contextWindow = row.get("context_window", Int::class.java) ?: 10,
                lastMessageAt = row.get("last_message_at", Instant::class.java),
                isActive = row.get("is_active", Boolean::class.java) ?: true,
                createdAt = row.get("created_at", Instant::class.java)!!,
                updatedAt = row.get("updated_at", Instant::class.java)!!,
                metadata = row.get("metadata", String::class.java)?.fromJson() ?: emptyMap()
            ) as T

            DailySessionStatDTO::class -> DailySessionStatDTO(
                date = row.get("date", java.time.LocalDate::class.java).toString(),
                count = row.get("count", Long::class.java) ?: 0L
            ) as T

            else -> throw IllegalArgumentException("Unknown type: ${T::class}")
        }
    }
}

data class SessionStatsDTO(
    val totalSessions: Long,
    val activeSessions: Long,
    val inactiveSessions: Long,
    val avgContextWindow: Double
)

data class DailySessionStatDTO(
    val date: String,
    val count: Long
)

@Suppress("UNCHECKED_CAST")
private fun String.fromJson(): Map<String, Any> {
    val element = kotlinx.serialization.json.Json.parseToJsonElement(this)
    return element.jsonObject.mapValues {
        when (val v = it.value) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                if (v.isString) v.content
                else v.booleanOrNull ?: v.longOrNull ?: v.doubleOrNull ?: v.content
            }
            else -> v.toString()
        }
    }
}