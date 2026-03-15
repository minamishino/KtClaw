package ktclaw.db.repository

import ktclaw.db.*
import kotlinx.coroutines.flow.Flow
import org.jetbrains.exposed.sql.*
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Session Repository - 会话数据访问层
 */
@Repository
interface SessionRepository : CoroutineCrudRepository<SessionDTO, UUID> {

    /**
     * 根据会话键查询
     */
    @Query("SELECT * FROM sessions WHERE session_key = :sessionKey LIMIT 1")
    suspend fun findBySessionKey(sessionKey: String): SessionDTO?

    /**
     * 根据用户ID查询活跃会话
     */
    @Query("""
        SELECT * FROM sessions 
        WHERE user_id = :userId AND is_active = true
        ORDER BY last_message_at DESC NULLS LAST
        LIMIT :limit OFFSET :offset
    """)
    suspend fun findActiveByUserId(
        userId: String,
        limit: Int,
        offset: Int
    ): Flow<SessionDTO>

    /**
     * 根据频道ID查询会话
     */
    @Query("""
        SELECT * FROM sessions 
        WHERE channel_id = :channelId
        ORDER BY last_message_at DESC NULLS LAST
        LIMIT :limit OFFSET :offset
    """)
    suspend fun findByChannelId(
        channelId: UUID,
        limit: Int,
        offset: Int
    ): Flow<SessionDTO>

    /**
     * 获取过期的会话
     */
    @Query("""
        SELECT * FROM sessions 
        WHERE is_active = true 
        AND last_message_at < :beforeTime
    """)
    suspend fun findExpiredSessions(beforeTime: Instant): Flow<SessionDTO>

    /**
     * 统计用户的会话数量
     */
    @Query("SELECT COUNT(*) FROM sessions WHERE user_id = :userId AND is_active = true")
    suspend fun countActiveByUserId(userId: String): Long
}

/**
 * Session Repository Exposed 实现
 */
object SessionRepositoryExposed {

    /**
     * 获取或创建会话
     */
    fun getOrCreate(
        sessionKey: String,
        userId: String,
        channelId: UUID,
        agentId: UUID? = null,
        title: String? = null
    ): Session {
        return Session.find { Sessions.sessionKey eq sessionKey }.firstOrNull()
            ?: Session.new {
                this.sessionKey = sessionKey
                this.userId = userId
                this.channel = Channel[channelId]
                this.agent = agentId?.let { Agent[it] }
                this.title = title
            }
    }

    /**
     * 更新最后消息时间
     */
    fun updateLastMessageTime(sessionId: UUID) {
        Sessions.update({ Sessions.id eq sessionId }) {
            it[lastMessageAt] = Instant.now()
        }
    }

    /**
     * 批量关闭过期会话
     */
    fun closeExpiredSessions(timeoutMinutes: Int): Int {
        val cutoffTime = Instant.now().minusSeconds(timeoutMinutes * 60L)
        return Sessions.update({
            (Sessions.isActive eq true) and
            (Sessions.lastMessageAt less cutoffTime)
        }) {
            it[isActive] = false
        }
    }

    /**
     * 获取会话统计信息
     */
    fun getSessionStats(): SessionStatsDTO {
        val totalSessions = Sessions.selectAll().count()
        val activeSessions = Sessions.select { Sessions.isActive eq true }.count()
        val avgContextWindow = Sessions
            .slice(Sessions.contextWindow.avg())
            .selectAll()
            .firstOrNull()
            ?.get(Sessions.contextWindow.avg())
            ?.toDouble() ?: 0.0

        return SessionStatsDTO(
            totalSessions = totalSessions,
            activeSessions = activeSessions,
            inactiveSessions = totalSessions - activeSessions,
            avgContextWindow = avgContextWindow
        )
    }

    /**
     * 获取每日会话创建统计
     */
    fun getDailySessionStats(days: Int = 30): List<DailySessionStatDTO> {
        return Sessions
            .slice(
                Sessions.createdAt.date(),
                Sessions.id.count()
            )
            .selectAll()
            .groupBy(Sessions.createdAt.date())
            .orderBy(Sessions.createdAt.date() to SortOrder.DESC)
            .limit(days)
            .map {
                DailySessionStatDTO(
                    date = it[Sessions.createdAt.date()].toString(),
                    count = it[Sessions.id.count()]
                )
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
