package ktclaw.db.repository

import ktclaw.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Message Repository - 消息数据访问层
 * 提供高性能查询和批量操作
 */
@Repository
interface MessageRepository : CoroutineCrudRepository<MessageDTO, UUID> {

    /**
     * 根据会话ID查询消息（分页）
     */
    @Query("""
        SELECT * FROM messages 
        WHERE session_id = :sessionId 
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun findBySessionId(
        sessionId: UUID,
        limit: Int,
        offset: Int
    ): Flow<MessageDTO>

    /**
     * 根据会话ID查询消息（带角色过滤）
     */
    @Query("""
        SELECT * FROM messages 
        WHERE session_id = :sessionId AND role = :role
        ORDER BY created_at DESC 
        LIMIT :limit
    """)
    suspend fun findBySessionIdAndRole(
        sessionId: UUID,
        role: String,
        limit: Int
    ): Flow<MessageDTO>

    /**
     * 全文搜索消息内容
     */
    @Query("""
        SELECT * FROM messages 
        WHERE content ILIKE '%' || :keyword || '%'
        ORDER BY created_at DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchByContent(
        keyword: String,
        limit: Int,
        offset: Int
    ): Flow<MessageDTO>

    /**
     * 统计会话消息数量
     */
    @Query("SELECT COUNT(*) FROM messages WHERE session_id = :sessionId")
    suspend fun countBySessionId(sessionId: UUID): Long

    /**
     * 获取某时间段内的消息统计
     */
    @Query("""
        SELECT DATE_TRUNC('day', created_at) as date, COUNT(*) as count
        FROM messages 
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY DATE_TRUNC('day', created_at)
        ORDER BY date DESC
    """)
    suspend fun getMessageStatsByDateRange(
        startTime: Instant,
        endTime: Instant
    ): Flow<MessageStatDTO>
}

data class MessageStatDTO(
    val date: Instant,
    val count: Long
)

/**
 * Message Repository Exposed 实现
 * 用于复杂查询和批量操作
 */
object MessageRepositoryExposed {

    /**
     * 获取会话的上下文消息（优化版本）
     * 使用窗口函数提高性能
     */
    fun getContextMessages(sessionId: UUID, limit: Int): List<Message> {
        return Message.find { Messages.sessionId eq sessionId }
            .orderBy(Messages.createdAt to SortOrder.DESC)
            .limit(limit)
            .toList()
            .reversed()
    }

    /**
     * 批量插入消息
     */
    fun batchInsert(messages: List<MessageInsertDTO>): List<UUID> {
        return Messages.batchInsert(messages, shouldReturnGeneratedValues = true) { dto ->
            this[Messages.sessionId] = dto.sessionId
            this[Messages.channelId] = dto.channelId
            this[Messages.messageId] = dto.messageId
            this[Messages.replyToId] = dto.replyToId
            this[Messages.role] = dto.role
            this[Messages.type] = dto.type
            this[Messages.content] = dto.content
            this[Messages.mediaUrl] = dto.mediaUrl
            this[Messages.status] = dto.status
            this[Messages.tokensUsed] = dto.tokensUsed
            this[Messages.latencyMs] = dto.latencyMs
            this[Messages.metadata] = dto.metadata
        }.map { it[Messages.id].value }
    }

    /**
     * 软删除消息（标记为已删除）
     */
    fun softDelete(messageIds: List<UUID>): Int {
        return Messages.update({ Messages.id inList messageIds }) {
            it[status] = MessageStatus.DELETED
        }
    }

    /**
     * 清理过期消息
     */
    fun cleanupOldMessages(beforeTime: Instant): Int {
        return Messages.deleteWhere {
            Messages.createdAt less beforeTime
        }
    }

    /**
     * 获取热门会话（消息最多的会话）
     */
    fun getTopSessions(limit: Int = 10): List<Pair<UUID, Long>> {
        return Messages
            .slice(Messages.sessionId, Messages.id.count())
            .selectAll()
            .groupBy(Messages.sessionId)
            .orderBy(Messages.id.count(), SortOrder.DESC)
            .limit(limit)
            .map { it[Messages.sessionId].value to it[Messages.id.count()] }
    }

    /**
     * 获取消息类型分布
     */
    fun getMessageTypeDistribution(): Map<MessageType, Long> {
        return Messages
            .slice(Messages.type, Messages.id.count())
            .selectAll()
            .groupBy(Messages.type)
            .associate { it[Messages.type] to it[Messages.id.count()] }
    }
}

data class MessageInsertDTO(
    val sessionId: UUID,
    val channelId: UUID,
    val messageId: String? = null,
    val replyToId: UUID? = null,
    val role: MessageRole,
    val type: MessageType = MessageType.TEXT,
    val content: String,
    val mediaUrl: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val tokensUsed: Int? = null,
    val latencyMs: Int? = null,
    val metadata: Map<String, Any> = emptyMap()
)
