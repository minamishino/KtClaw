package ktclaw.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.Instant
import java.util.UUID

/**
 * KtClaw Database Entities
 * Exposed DAO Entity Classes
 */

// ============================================
// 1. Agent Entity - 代理配置
// ============================================
class Agent(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Agent>(Agents)

    var name by Agents.name
    var description by Agents.description
    var model by Agents.model
    var provider by Agents.provider
    var apiKeyEncrypted by Agents.apiKeyEncrypted
    var systemPrompt by Agents.systemPrompt
    var temperature by Agents.temperature
    var maxTokens by Agents.maxTokens
    var timeoutMs by Agents.timeoutMs
    var isActive by Agents.isActive
    var createdAt by Agents.createdAt
    var updatedAt by Agents.updatedAt
    var metadata by Agents.metadata

    // Relations
    val channels by Channel referrersOn Channels.agentId
    val sessions by Session referrersOn Sessions.agentId

    fun toDTO(): AgentDTO = AgentDTO(
        id = id.value,
        name = name,
        description = description,
        model = model,
        provider = provider,
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxTokens = maxTokens,
        timeoutMs = timeoutMs,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        metadata = metadata
    )
}

data class AgentDTO(
    val id: UUID,
    val name: String,
    val description: String?,
    val model: String,
    val provider: String,
    val systemPrompt: String?,
    val temperature: java.math.BigDecimal,
    val maxTokens: Int,
    val timeoutMs: Int,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, Any>
)

// ============================================
// 2. Channel Entity - 频道配置
// ============================================
class Channel(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Channel>(Channels)

    var channelId by Channels.channelId
    var channelType by Channels.channelType
    var name by Channels.name
    var description by Channels.description
    var agent by Agent optionalReferencedOn Channels.agentId
    var isActive by Channels.isActive
    var config by Channels.config
    var createdAt by Channels.createdAt
    var updatedAt by Channels.updatedAt

    // Relations
    val sessions by Session referrersOn Sessions.channelId
    val messages by Message referrersOn Messages.channelId

    fun toDTO(): ChannelDTO = ChannelDTO(
        id = id.value,
        channelId = channelId,
        channelType = channelType,
        name = name,
        description = description,
        agentId = agent?.id?.value,
        isActive = isActive,
        config = config,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

data class ChannelDTO(
    val id: UUID,
    val channelId: String,
    val channelType: String,
    val name: String?,
    val description: String?,
    val agentId: UUID?,
    val isActive: Boolean,
    val config: Map<String, Any>,
    val createdAt: Instant,
    val updatedAt: Instant
)

// ============================================
// 3. Session Entity - 会话管理
// ============================================
class Session(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Session>(Sessions)

    var sessionKey by Sessions.sessionKey
    var userId by Sessions.userId
    var channel by Channel referencedOn Sessions.channelId
    var agent by Agent optionalReferencedOn Sessions.agentId
    var title by Sessions.title
    var contextWindow by Sessions.contextWindow
    var lastMessageAt by Sessions.lastMessageAt
    var isActive by Sessions.isActive
    var createdAt by Sessions.createdAt
    var updatedAt by Sessions.updatedAt
    var metadata by Sessions.metadata

    // Relations
    val messages by Message referrersOn Messages.sessionId

    fun toDTO(): SessionDTO = SessionDTO(
        id = id.value,
        sessionKey = sessionKey,
        userId = userId,
        channelId = channel.id.value,
        agentId = agent?.id?.value,
        title = title,
        contextWindow = contextWindow,
        lastMessageAt = lastMessageAt,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        metadata = metadata
    )

    /**
     * 获取会话的上下文消息
     */
    fun getContextMessages(limit: Int = contextWindow): List<Message> {
        return Message.find { Messages.sessionId eq id.value }
            .orderBy(Messages.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit)
            .toList()
            .reversed()
    }
}

data class SessionDTO(
    val id: UUID,
    val sessionKey: String,
    val userId: String,
    val channelId: UUID,
    val agentId: UUID?,
    val title: String?,
    val contextWindow: Int,
    val lastMessageAt: Instant?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, Any>
)

// ============================================
// 4. Message Entity - 消息记录
// ============================================
class Message(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Message>(Messages)

    var session by Session referencedOn Messages.sessionId
    var channel by Channel referencedOn Messages.channelId
    var messageId by Messages.messageId
    var replyTo by Message optionalReferencedOn Messages.replyToId
    var role by Messages.role
    var type by Messages.type
    var content by Messages.content
    var contentEncrypted by Messages.contentEncrypted
    var mediaUrl by Messages.mediaUrl
    var mediaSize by Messages.mediaSize
    var mediaMimeType by Messages.mediaMimeType
    var status by Messages.status
    var tokensUsed by Messages.tokensUsed
    var latencyMs by Messages.latencyMs
    var errorMessage by Messages.errorMessage
    var createdAt by Messages.createdAt
    var updatedAt by Messages.updatedAt
    var metadata by Messages.metadata

    fun toDTO(): MessageDTO = MessageDTO(
        id = id.value,
        sessionId = session.id.value,
        channelId = channel.id.value,
        messageId = messageId,
        replyToId = replyTo?.id?.value,
        role = role,
        type = type,
        content = content,
        contentEncrypted = contentEncrypted,
        mediaUrl = mediaUrl,
        mediaSize = mediaSize,
        mediaMimeType = mediaMimeType,
        status = status,
        tokensUsed = tokensUsed,
        latencyMs = latencyMs,
        errorMessage = errorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt,
        metadata = metadata
    )

    /**
     * 检查是否为媒体消息
     */
    fun isMedia(): Boolean = type in setOf(
        MessageType.IMAGE, 
        MessageType.VOICE, 
        MessageType.VIDEO, 
        MessageType.FILE
    )
}

data class MessageDTO(
    val id: UUID,
    val sessionId: UUID,
    val channelId: UUID,
    val messageId: String?,
    val replyToId: UUID?,
    val role: MessageRole,
    val type: MessageType,
    val content: String,
    val contentEncrypted: Boolean,
    val mediaUrl: String?,
    val mediaSize: Long?,
    val mediaMimeType: String?,
    val status: MessageStatus,
    val tokensUsed: Int?,
    val latencyMs: Int?,
    val errorMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, Any>
)

// ============================================
// 5. Config Entity - 系统配置
// ============================================
class Config(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Config>(Configs)

    var scope by Configs.scope
    var scopeId by Configs.scopeId
    var key by Configs.key
    var value by Configs.value
    var valueType by Configs.valueType
    var description by Configs.description
    var isEncrypted by Configs.isEncrypted
    var isActive by Configs.isActive
    var createdAt by Configs.createdAt
    var updatedAt by Configs.updatedAt

    fun toDTO(): ConfigDTO = ConfigDTO(
        id = id.value,
        scope = scope,
        scopeId = scopeId,
        key = key,
        value = value,
        valueType = valueType,
        description = description,
        isEncrypted = isEncrypted,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    /**
     * 获取配置值的类型化表示
     */
    fun getTypedValue(): Any {
        return when (valueType) {
            "int" -> value.toIntOrNull() ?: 0
            "long" -> value.toLongOrNull() ?: 0L
            "float", "double" -> value.toDoubleOrNull() ?: 0.0
            "boolean" -> value.toBooleanStrictOrNull() ?: false
            "json" -> kotlinx.serialization.json.Json.parseToJsonElement(value)
            else -> value
        }
    }
}

data class ConfigDTO(
    val id: UUID,
    val scope: ConfigScope,
    val scopeId: String?,
    val key: String,
    val value: String,
    val valueType: String,
    val description: String?,
    val isEncrypted: Boolean,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

// ============================================
// Config Repository Helper
// ============================================
object ConfigRepository {
    /**
     * 获取配置值，支持层级覆盖
     * 优先级: session > user > channel > global
     */
    fun getValue(
        key: String,
        sessionId: String? = null,
        userId: String? = null,
        channelId: String? = null,
        defaultValue: String? = null
    ): String? {
        // 按优先级顺序查询
        val configs = sequence {
            if (sessionId != null) {
                yield(Config.find { 
                    (Configs.scope eq ConfigScope.SESSION) and 
                    (Configs.scopeId eq sessionId) and 
                    (Configs.key eq key) 
                }.firstOrNull())
            }
            if (userId != null) {
                yield(Config.find { 
                    (Configs.scope eq ConfigScope.USER) and 
                    (Configs.scopeId eq userId) and 
                    (Configs.key eq key) 
                }.firstOrNull())
            }
            if (channelId != null) {
                yield(Config.find { 
                    (Configs.scope eq ConfigScope.CHANNEL) and 
                    (Configs.scopeId eq channelId) and 
                    (Configs.key eq key) 
                }.firstOrNull())
            }
            yield(Config.find { 
                (Configs.scope eq ConfigScope.GLOBAL) and 
                (Configs.key eq key) 
            }.firstOrNull())
        }

        return configs.filterNotNull().firstOrNull()?.value ?: defaultValue
    }

    /**
     * 设置配置值
     */
    fun setValue(
        scope: ConfigScope,
        scopeId: String?,
        key: String,
        value: String,
        valueType: String = "string",
        description: String? = null
    ): Config {
        val existing = Config.find {
            (Configs.scope eq scope) and
            (Configs.scopeId eq scopeId) and
            (Configs.key eq key)
        }.firstOrNull()

        return if (existing != null) {
            existing.value = value
            existing.valueType = valueType
            description?.let { existing.description = it }
            existing
        } else {
            Config.new {
                this.scope = scope
                this.scopeId = scopeId
                this.key = key
                this.value = value
                this.valueType = valueType
                this.description = description
            }
        }
    }
}