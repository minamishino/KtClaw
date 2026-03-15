package ktclaw.db.repository

import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Row
import ktclaw.db.MessageDTO
import ktclaw.db.MessageStatus
import ktclaw.db.MessageType
import ktclaw.db.R2DBCDatabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Message Repository - 消息数据访问层
 * 纯 Kotlin 实现，使用 R2DBC DatabaseClient
 */
class MessageRepository(connectionFactory: ConnectionFactory) {
    private val logger = LoggerFactory.getLogger(MessageRepository::class.java)
    private val dbClient = R2DBCDatabaseClient(connectionFactory)

    /**
     * 根据 ID 查询消息
     */
    suspend fun findById(id: UUID): MessageDTO? {
        logger.debug("Finding message by id: $id")
        return dbClient.fetchOne(
            """
            SELECT m.*, s.session_key, c.channel_id as channel_external_id
            FROM messages m
            JOIN sessions s ON m.session_id = s.id
            JOIN channels c ON m.channel_id = c.id
            WHERE m.id = '$id'
            """
        ) { row -> mapRowToMessageDTO(row) }
    }

    /**
     * 根据会话 ID 查询消息（分页）- 使用 Flow 进行流式查询
     */
    fun findBySessionIdFlow(sessionId: UUID, limit: Int, offset: Int): Flow<MessageDTO> {
        logger.debug("Finding messages for session: $sessionId")
        return dbClient.fetchMany(
            """
            SELECT m.*, s.session_key, c.channel_id as channel_external_id
            FROM messages m
            JOIN sessions s ON m.session_id = s.id
            JOIN channels c ON m.channel_id = c.id
            WHERE m.session_id = '$sessionId'
            ORDER BY m.created_at DESC
            LIMIT $limit OFFSET $offset
            """
        ) { row -> mapRowToMessageDTO(row) }
    }

    /**
     * 根据消息 ID 查询（外部消息 ID）
     */
    suspend fun findByMessageId(messageId: String): MessageDTO? {
        logger.debug("Finding message by messageId: $messageId")
        return dbClient.fetchOne(
            """
            SELECT m.*, s.session_key, c.channel_id as channel_external_id
            FROM messages m
            JOIN sessions s ON m.session_id = s.id
            JOIN channels c ON m.channel_id = c.id
            WHERE m.message_id = '$messageId'
            """
        ) { row -> mapRowToMessageDTO(row) }
    }

    /**
     * 保存消息
     */
    suspend fun save(message: MessageDTO): MessageDTO {
        logger.debug("Saving message: ${message.messageId}")
        return if (message.id == null) {
            insert(message)
        } else {
            update(message)
        }
    }

    /**
     * 插入新消息
     */
    private suspend fun insert(message: MessageDTO): MessageDTO {
        val id = UUID.randomUUID()
        val now = Instant.now()

        dbClient.execute(
            """
            INSERT INTO messages (id, session_id, channel_id, message_id, reply_to_id, role, type,
                                content, content_encrypted, media_url, media_size, media_mime_type,
                                status, tokens_used, latency_ms, error_message, created_at, updated_at, metadata)
            VALUES ('$id', '${message.sessionId}', '${message.channelId}',
                    ${message.messageId?.let { "'$it'" } ?: "NULL"},
                    ${message.replyToId?.let { "'$it'" } ?: "NULL"},
                    '${message.role}', '${message.type}',
                    '${message.content.replace("'", "''")}',
                    ${message.contentEncrypted}, 
                    ${message.mediaUrl?.let { "'$it'" } ?: "NULL"},
                    ${message.mediaSize ?: "NULL"},
                    ${message.mediaMimeType?.let { "'$it'" } ?: "NULL"},
                    '${message.status}', ${message.tokensUsed ?: "NULL"},
                    ${message.latencyMs ?: "NULL"},
                    ${message.errorMessage?.let { "'$it'" } ?: "NULL"},
                    '$now', '$now', '${message.metadata}')
            """
        )

        return message.copy(id = id, createdAt = now, updatedAt = now)
    }

    /**
     * 更新消息
     */
    private suspend fun update(message: MessageDTO): MessageDTO {
        val now = Instant.now()

        dbClient.execute(
            """
            UPDATE messages
            SET status = '${message.status}',
                tokens_used = ${message.tokensUsed ?: "NULL"},
                latency_ms = ${message.latencyMs ?: "NULL"},
                error_message = ${message.errorMessage?.let { "'$it'" } ?: "NULL"},
                updated_at = '$now',
                metadata = '${message.metadata}'
            WHERE id = '${message.id}'
            """
        )

        return message.copy(updatedAt = now)
    }

    /**
     * 更新消息状态
     */
    suspend fun updateStatus(messageId: UUID, status: MessageStatus) {
        logger.debug("Updating message status: $messageId -> $status")
        dbClient.execute(
            """
            UPDATE messages
            SET status = '$status', updated_at = '${Instant.now()}'
            WHERE id = '$messageId'
            """
        )
    }

    /**
     * 批量更新消息状态
     */
    suspend fun updateStatusBatch(messageIds: List<UUID>, status: MessageStatus) {
        if (messageIds.isEmpty()) return
        
        logger.debug("Batch updating message status for ${messageIds.size} messages")
        val ids = messageIds.joinToString(",") { "'$it'" }
        
        dbClient.execute(
            """
            UPDATE messages
            SET status = '$status', updated_at = '${Instant.now()}'
            WHERE id IN ($ids)
            """
        )
    }

    /**
     * 统计会话的消息数
     */
    suspend fun countBySessionId(sessionId: UUID): Long {
        val result = dbClient.fetchOne(
            """
            SELECT COUNT(*) as count
            FROM messages
            WHERE session_id = '$sessionId'
            """
        ) { row -> row.get("count", Long::class.java) }
        return result ?: 0L
    }

    /**
     * 统计会话的 Token 使用量
     */
    suspend fun sumTokensBySessionId(sessionId: UUID): Int {
        val result = dbClient.fetchOne(
            """
            SELECT COALESCE(SUM(tokens_used), 0) as total
            FROM messages
            WHERE session_id = '$sessionId' AND tokens_used IS NOT NULL
            """
        ) { row -> row.get("total", Int::class.java) }
        return result ?: 0
    }

    /**
     * 删除消息（软删除）
     */
    suspend fun softDelete(messageId: UUID) {
        logger.debug("Soft deleting message: $messageId")
        dbClient.execute(
            """
            UPDATE messages
            SET status = 'DELETED', updated_at = '${Instant.now()}'
            WHERE id = '$messageId'
            """
        )
    }

    /**
     * 将数据库行映射为 MessageDTO
     */
    private fun mapRowToMessageDTO(row: Row): MessageDTO {
        return MessageDTO(
            id = row.get("id", UUID::class.java),
            sessionId = row.get("session_id", UUID::class.java),
            channelId = row.get("channel_id", UUID::class.java),
            messageId = row.get("message_id", String::class.java),
            replyToId = row.get("reply_to_id", UUID::class.java),
            role = row.get("role", String::class.java) ?: "user",
            type = MessageType.valueOf(row.get("type", String::class.java) ?: "TEXT"),
            content = row.get("content", String::class.java) ?: "",
            contentEncrypted = row.get("content_encrypted", Boolean::class.java) ?: false,
            mediaUrl = row.get("media_url", String::class.java),
            mediaSize