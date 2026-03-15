package ktclaw.db.repository

import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Row
import ktclaw.db.R2DBCDatabaseClient
import ktclaw.db.SessionDTO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Session Repository - 会话数据访问层
 * 纯 Kotlin 实现，使用 R2DBC DatabaseClient
 */
class SessionRepository(connectionFactory: ConnectionFactory) {
    private val logger = LoggerFactory.getLogger(SessionRepository::class.java)
    private val dbClient = R2DBCDatabaseClient(connectionFactory)

    /**
     * 根据 ID 查询会话
     */
    suspend fun findById(id: UUID): SessionDTO? {
        logger.debug("Finding session by id: $id")
        return dbClient.fetchOne(
            """
            SELECT s.*, c.channel_id as channel_external_id, a.name as agent_name
            FROM sessions s
            JOIN channels c ON s.channel_id = c.id
            LEFT JOIN agents a ON s.agent_id = a.id
            WHERE s.id = '$id'
            """
        ) { row -> mapRowToSessionDTO(row) }
    }

    /**
     * 根据会话键查询
     */
    suspend fun findBySessionKey(sessionKey: String): SessionDTO? {
        logger.debug("Finding session by key: $sessionKey")
        return dbClient.fetchOne(
            """
            SELECT s.*, c.channel_id as channel_external_id, a.name as agent_name
            FROM sessions s
            JOIN channels c ON s.channel_id = c.id
            LEFT JOIN agents a ON s.agent_id = a.id
            WHERE s.session_key = '$sessionKey'
            """
        ) { row -> mapRowToSessionDTO(row) }
    }

    /**
     * 根据用户 ID 查询活跃会话 - 使用 Flow
     */
    fun findActiveByUserIdFlow(userId: String, limit: Int, offset: Int): Flow<SessionDTO> {
        logger.debug("Finding active sessions for user: $userId")
        return dbClient.fetchMany(
            """
            SELECT s.*, c.channel_id as channel_external_id, a.name as agent_name
            FROM sessions s
            JOIN channels c ON s.channel_id = c.id
            LEFT JOIN agents a ON s.agent_id = a.id
            WHERE s.user_id = '$userId' AND s.is_active = true
            ORDER BY s.last_message_at DESC NULLS LAST
            LIMIT $limit OFFSET $offset
            """
        ) { row -> mapRowToSessionDTO(row) }
    }

    /**
     * 根据频道 ID 查询会话
     */
    fun findByChannelIdFlow(channelId: UUID, limit: Int, offset: Int): Flow<SessionDTO> {
        logger.debug("Finding sessions for channel: $channelId")
        return dbClient.fetchMany(
            """
            SELECT s.*, c.channel_id as channel_external_id, a.name as agent_name
            FROM sessions s
            JOIN channels c ON s.channel_id = c.id
            LEFT JOIN agents a ON s.agent_id = a.id
            WHERE s.channel_id = '$channelId'
            ORDER BY s.last_message_at DESC NULLS LAST
            LIMIT $limit OFFSET $offset
            """
        ) { row -> mapRowToSessionDTO(row) }
    }

    /**
     * 保存会话
     */
    suspend fun save(session: SessionDTO): SessionDTO {
        logger.debug("Saving session: ${session.sessionKey}")
        return if (session.id == null) {
            insert(session)
        } else {
            update(session)
        }
    }

    /**
     * 插入新会话
     */
    private suspend fun insert(session: SessionDTO): SessionDTO {
        val id = UUID.randomUUID()
        val now = Instant.now()

        dbClient.execute(
            """
            INSERT INTO sessions (id, session_key, user_id, channel_id, agent_id, title, 
                                 context_window, is_active, created_at, updated_at, metadata)
            VALUES ('$id', '${session.sessionKey}', '${session.userId}', '${session.channelId}',
                    ${session.agentId?.let { "'$it'" } ?: "NULL"}, 
                    ${session.title?.let { "'$it'" } ?: "NULL"},
                    ${session.contextWindow}, ${session.isActive}, '$now', '$now',
                    '${session.metadata}')
            """
        )

        return session.copy(id = id, createdAt = now, updatedAt = now)
    }

    /**
     * 更新会话
     */
    private suspend fun update(session: SessionDTO): SessionDTO {
        val now = Instant.now()

        dbClient.execute(
            """
            UPDATE sessions
            SET title = ${session.title?.let { "'$it'" } ?: "NULL"},
                agent_id = ${session.agentId?.let { "'$it'" } ?: "NULL"},
                context_window = ${session.contextWindow},
                is_active = ${session.isActive},
                last_message_at = ${session.lastMessageAt?.let { "'$it'" } ?: "NULL"},
                updated_at = '$now',
                metadata = '${session.metadata}'
            WHERE id = '${session.id}'
            """
        )

        return session.copy(updatedAt = now)
    }

    /**
     * 更新最后消息时间
     */
    suspend fun updateLastMessageAt(sessionId: UUID, timestamp: Instant) {
        logger.debug("Updating last message at for session: $sessionId")
        dbClient.execute(
            """
            UPDATE sessions
            SET last_message_at = '$timestamp', updated_at = '$timestamp'
            WHERE id = '$sessionId'
            """
        )
    }

    /**
     * 软删除会话
     */
    suspend fun softDelete(sessionId: UUID) {
        logger.debug("Soft deleting session: $sessionId")
        dbClient.execute(
            """
            UPDATE sessions
            SET is_active = false, updated_at = '${Instant.now()}'
            WHERE id = '$sessionId'
            """
        )
    }

    /**
     * 统计用户的活跃会话数
     */
    suspend fun countActiveByUserId(userId: String): Long {
        val result = dbClient.fetchOne(
            """
            SELECT COUNT(*) as count
            FROM sessions
            WHERE user_id = '$userId' AND is_active = true
            """
        ) { row -> row.get("count", Long::class.java) }
        return result ?: 0L
    }

    /**
     * 将数据库行映射为 SessionDTO
     */
    private fun mapRowToSessionDTO(row: Row): SessionDTO {
        return SessionDTO(
            id = row.get("id", UUID::class.java),
            sessionKey = row.get("session_key", String::class.java) ?: "",
            userId = row.get("user_id", String::class.java) ?: "",
            channelId = row.get("channel_id", UUID::class.java),
            agentId = row.get("agent_id", UUID::class.java),
            title = row.get("title", String::class.java),
            contextWindow = row.get("context_window", Int::class.java) ?: 10,
            isActive = row.get("is_active", Boolean::class.java) ?: true,
            lastMessageAt = row.get("last_message_at", Instant::class.java),
            createdAt = row.get("created_at", Instant::class.java) ?: Instant.now(),
            updatedAt = row.get("updated_at", Instant::class.java) ?: Instant.now(),
            metadata = row.get("metadata", Map::class.java) as? Map<String, Any> ?: emptyMap(),
            // 关联字段
            channelExternalId = row.get("channel_external_id", String::class.java),
            agentName = row.get("agent_name", String::class.java)
        )
    }
}
