package ktclaw.db.repository

import ktclaw.db.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.Row
import java.time.Instant
import java.util.UUID

/**
 * Message Repository - 消息数据访问层
 * 使用 R2DBC DatabaseClient 实现，支持 suspend 函数和 Flow 流式查询
 */
class MessageRepository(databaseClient: DatabaseClient) : R2DBCRepository(databaseClient) {

    /**
     * 根据 ID 查询消息
     */
    suspend fun findById(id: UUID): MessageDTO? = fetchOne(
        """
        SELECT m.*, s.session_key, c.channel_id as channel_external_id
        FROM messages m
        JOIN sessions s ON m.session_id = s.id
        JOIN channels c ON m.channel_id = c.id
        WHERE m.id = :id
        """,
        mapOf("id" to id)
    )

    /**
     * 根据会话 ID 查询消息（分页）- 使用 Flow 进行流式查询
     */
    fun findBySessionIdFlow(sessionId: UUID, limit: Int, offset: Int): Flow<MessageDTO> = fetchMany(
        """
        SELECT m.*, s.session_key, c.channel_id as channel_external_id
        FROM messages m
        JOIN sessions s ON m.session_id = s.id
        JOIN channels c ON m.channel_id = c.id
        WHERE m.session_id = :sessionId
        ORDER BY m.created_at DESC
        LIMIT :limit OFFSET :offset
        """,
        mapOf(
            "sessionId" to sessionId,
            "limit" to limit,
            "offset" to offset
        )
    )

    /**
     * 根据会话 ID 查询消息（分页）- suspend 版本
     */
    suspend fun findBySessionId(sessionId: UUID, limit: Int, offset: Int): List<MessageDTO> {
        return findBySessionIdFlow(sessionId, limit, offset).toList()
    }

    /**
     * 根据会话 ID 和角色查询消息 - 使用 Flow
     */
    fun findBySessionIdAndRoleFlow(
        sessionId: UUID,
        role: MessageRole,
        limit: Int
    ): Flow<MessageDTO> = fetchMany(
        """
        SELECT m.*, s.session_key, c.channel_id as channel_external_id
        FROM messages m
        JOIN sessions s ON m.session_id = s.id
        JOIN channels c ON m.channel_id = c.id
        WHERE m.session_id = :sessionId AND m.role = :role
        ORDER BY m.created_at DESC
        LIMIT :limit
        """,
        mapOf(
            "sessionId" to sessionId,
            "role" to role.name,
            "limit" to limit
        )
    )

    /**
     * 全文搜索消息内容 - 使用 Flow 流式返回
     */
    fun searchByContentFlow(keyword: String, limit: Int, offset: Int): Flow<MessageDTO> = fetchMany(
        """
        SELECT m.*, s.session_key, c.channel_id as channel_external_id
        FROM messages m
        JOIN sessions s ON m.session_id = s.id
        JOIN channels c ON m.channel_id = c.id
        WHERE m.content ILIKE :keyword
        ORDER BY m.created_at DESC
        LIMIT :limit OFFSET :offset
        """,
        mapOf(
            "keyword" to "%$keyword%",
            "limit" to limit,
            "offset" to offset
        )
    )

    /**
     * 统计会话消息数量
     */
    suspend fun countBySessionId(sessionId: UUID): Long {
        return fetchOne<Long>(
            "SELECT COUNT(*) as count FROM messages WHERE session_id = :sessionId",
            mapOf("sessionId" to sessionId)
        ) ?: 0L
    }

    /**
     * 获取某时间段内的消息统计 - 使用 Flow
     */
    fun getMessageStatsByDateRangeFlow(
        startTime: Instant,
        endTime: Instant
    ): Flow<MessageStatDTO> = fetchMany(
        """
        SELECT DATE_TRUNC('day', created_at) as date, COUNT(*) as count
        FROM messages
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY DATE_TRUNC('day', created_at)
        ORDER BY date DESC
        """,
        mapOf(
            "startTime" to startTime,
            "endTime" to endTime
        )
    )

    /**
     * 保存消息
     */
    suspend fun save(dto: MessageInsertDTO): UUID? {
        val id = executeInsert(
            """
            INSERT INTO messages (
                id, session_id, channel_id, message_id, reply_to_id, role, type,
                content, content_encrypted, media_url, media_size, media_mime_type,
                status, tokens_used, latency_ms, error_message, metadata, created_at, updated_at
            ) VALUES (
                gen_random_uuid(), :sessionId, :channelId, :messageId, :replyToId, :role, :type,
                :content, :contentEncrypted, :mediaUrl, :mediaSize, :mediaMimeType,
                :status, :tokensUsed, :latencyMs, :errorMessage, :metadata::jsonb, NOW(), NOW()
            )
            """,
            mapOf(
                "sessionId" to dto.sessionId,
                "channelId" to dto.channelId,
                "messageId" to dto.messageId,
                "replyToId" to dto.replyToId,
                "role" to dto.role.name,
                "type" to dto.type.name,
                "content" to dto.content,
                "contentEncrypted" to dto.contentEncrypted,
                "mediaUrl" to dto.mediaUrl,
                "mediaSize" to dto.mediaSize,
                "mediaMimeType" to dto.mediaMimeType,
                "status" to dto.status.name,
                "tokensUsed" to dto.tokensUsed,
                "latencyMs" to dto.latencyMs,
                "errorMessage" to dto.errorMessage,
                "metadata" to dto.metadata.toJsonString()
            )
        )
        return id?.let { UUID.nameUUIDFromBytes(it.toString().toByteArray()) }
    }

    /**
     * 批量插入消息
     */
    suspend fun batchInsert(messages: List<MessageInsertDTO>): Int {
        if (messages.isEmpty()) return 0

        val sql = """
            INSERT INTO messages (
                id, session_id, channel_id, message_id, reply_to_id, role, type,
                content, content_encrypted, media_url, media_size, media_mime_type,
                status, tokens_used, latency_ms, error_message, metadata, created_at, updated_at
            ) VALUES (
                gen_random_uuid(), :sessionId, :channelId, :messageId, :replyToId, :role, :type,
                :content, :contentEncrypted, :mediaUrl, :mediaSize, :mediaMimeType,
                :status, :tokensUsed, :latencyMs, :errorMessage, :metadata::jsonb, NOW(), NOW()
            )
        """

        val binds = messages.map { dto ->
            mapOf(
                "sessionId" to dto.sessionId,
                "channelId" to dto.channelId,
                "messageId" to dto.messageId,
                "replyToId" to dto.replyToId,
                "role" to dto.role.name,
                "type" to dto.type.name,
                "content" to dto.content,
                "contentEncrypted" to dto.contentEncrypted,
                "mediaUrl" to dto.mediaUrl,
                "mediaSize" to dto.mediaSize,
                "mediaMimeType" to dto.mediaMimeType,
                "status" to dto.status.name,
                "tokensUsed" to dto.tokensUsed,
                "latencyMs" to dto.latencyMs,
                "errorMessage" to dto.errorMessage,
                "metadata" to dto.metadata.toJsonString()
            )
        }

        return executeBatch(sql, binds)
    }

    /**
     * 软删除消息（标记为已删除）
     */
    suspend fun softDelete(messageIds: List<UUID>): Int {
        if (messageIds.isEmpty()) return 0

        val placeholders = messageIds.indices.joinToString(",") { ":id$it" }
        val params = messageIds.mapIndexed { index, id -> "id$index" to id }.toMap()

        return executeUpdate(
            "UPDATE messages SET status = 'DELETED', updated_at = NOW() WHERE id IN ($placeholders)",
            params
        )
    }

    /**
     * 清理过期消息
     */
    suspend fun cleanupOldMessages(beforeTime: Instant): Int {
        return executeUpdate(
            "DELETE FROM messages WHERE created_at < :beforeTime",
            mapOf("beforeTime" to beforeTime)
        )
    }

    /**
     * 获取热门会话（消息最多的会话）- 使用并行查询
     */
    suspend fun getTopSessions(limit: Int = 10): List<Pair<UUID, Long>> = coroutineScope {
        val result = async {
            fetchMany<Pair<UUID, Long>>(
                """
                SELECT session_id, COUNT(*) as count
                FROM messages
                GROUP BY session_id
                ORDER BY count DESC
                LIMIT :limit
                """,
                mapOf("limit" to limit)
            ).toList()
        }
        result.await()
    }

    /**
     * 获取消息类型分布 - 使用并行查询
     */
    suspend fun getMessageTypeDistribution(): Map<MessageType, Long> = coroutineScope {
        val result = async {
            fetchMany<Pair<MessageType, Long>>(
                """
                SELECT type, COUNT(*) as count
                FROM messages
                GROUP BY type
                """
            ).toList().toMap()
        }
        result.await()
    }

    /**
     * 更新消息状态
     */
    suspend fun updateStatus(messageId: UUID, status: MessageStatus): Int {
        return executeUpdate(
            "UPDATE messages SET status = :status, updated_at = NOW() WHERE id = :id",
            mapOf("id" to messageId, "status" to status.name)
        )
    }

    /**
     * 更新消息 token 使用量
     */
    suspend fun updateTokenUsage(messageId: UUID, tokensUsed: Int, latencyMs: Int): Int {
        return executeUpdate(
            """
            UPDATE messages 
            SET tokens_used = :tokensUsed, latency_ms = :latencyMs, updated_at = NOW() 
            WHERE id = :id
            """,
            mapOf(
                "id" to messageId,
                "tokensUsed" to tokensUsed,
                "latencyMs" to latencyMs
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    override inline fun <reified T : Any> mapRowToType(row: Row): T {
        return when (T::class) {
            MessageDTO::class -> MessageDTO(
                id = row.get("id", UUID::class.java)!!,
                sessionId = row.get("session_id", UUID::class.java)!!,
                channelId = row.get("channel_id", UUID::class.java)!!,
                messageId = row.get("message_id", String::class.java),
                replyToId = row.get("reply_to_id", UUID::class.java),
                role = MessageRole.valueOf(row.get("role", String::class.java)!!),
                type = MessageType.valueOf(row.get("type", String::class.java)!!),
                content = row.get("content", String::class.java)!!,
                contentEncrypted = row.get("content_encrypted", Boolean::class.java) ?: false,
                mediaUrl = row.get("media_url", String::class.java),
                mediaSize = row.get("media_size", Long::class.java),
                mediaMimeType = row.get("media_mime_type", String::class.java),
                status = MessageStatus.valueOf(row.get("status", String::class.java)!!),
                tokensUsed = row.get("tokens_used", Int::class.java),
                latencyMs = row.get("latency_ms", Int::class.java),
                errorMessage = row.get("error_message", String::class.java),
                createdAt = row.get("created_at", Instant::class.java)!!,
                updatedAt = row.get("updated_at", Instant::class.java)!!,
                metadata = row.get("metadata", String::class.java)?.fromJson() ?: emptyMap()
            ) as T

            MessageStatDTO::class -> MessageStatDTO(
                date = row.get("date", Instant::class.java)!!,
                count = row.get("count", Long::class.java) ?: 0L
            ) as T

            Pair::class -> {
                val first = row.get("session_id", UUID::class.java)
                    ?: row.get("type", String::class.java)?.let { MessageType.valueOf(it) }
                val second = row.get("count", Long::class.java) ?: 0L
                (first to second) as T
            }

            else -> throw IllegalArgumentException("Unknown type: ${T::class}")
        }
    }
}

data class MessageStatDTO(
    val date: Instant,
    val count: Long
)

data class MessageInsertDTO(
    val sessionId: UUID,
    val channelId: UUID,
    val messageId: String? = null,
    val replyToId: UUID? = null,
    val role: MessageRole,
    val type: MessageType = MessageType.TEXT,
    val content: String,
    val contentEncrypted: Boolean = false,
    val mediaUrl: String? = null,
    val mediaSize: Long? = null,
    val mediaMimeType: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val tokensUsed: Int? = null,
    val latencyMs: Int? = null,
    val errorMessage: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

// JSON 扩展函数
private fun Map<String, Any>.toJsonString(): String {
    return kotlinx.serialization.json.Json.encodeToString(
        kotlinx.serialization.builtins.MapSerializer(
            String.serializer(),
            kotlinx.serialization.json.JsonElement.serializer()
        ),
        this.mapValues {
            when (val v = it.value) {
                is String -> kotlinx.serialization.json.JsonPrimitive(v)
                is Number -> kotlinx.serialization.json.JsonPrimitive(v)
                is Boolean -> kotlinx.serialization.json.JsonPrimitive(v)
                else -> kotlinx.serialization.json.JsonPrimitive(v.toString())
            }
        }
    )
}

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
