package ktclaw.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import org.jetbrains.exposed.sql.json.jsonb

/**
 * KtClaw Database Tables
 * Exposed ORM Table Definitions
 */

// ============================================
// 1. AGENTS - 代理配置表
// ============================================
object Agents : UUIDTable("agents") {
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val model = varchar("model", 100)
    val provider = varchar("provider", 50)
    val apiKeyEncrypted = text("api_key_encrypted").nullable()
    val systemPrompt = text("system_prompt").nullable()
    val temperature = decimal("temperature", 3, 2).default(java.math.BigDecimal("0.70"))
    val maxTokens = integer("max_tokens").default(2048)
    val timeoutMs = integer("timeout_ms").default(30000)
    val isActive = bool("is_active").default(true)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(org.jetbrains.exposed.sql.functions.CurrentTimestamp)
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(org.jetbrains.exposed.sql.functions.CurrentTimestamp)
    val metadata = jsonb<Map<String, Any>>("metadata", JsonbSerializer).default(emptyMap())
}

// ============================================
// 2. CHANNELS - 频道配置表
// ============================================
object Channels : UUIDTable("channels") {
    val channelId = varchar("channel_id", 100).uniqueIndex()
    val channelType = varchar("channel_type", 50)
    val name = varchar("name", 200).nullable()
    val description = text("description").nullable()
    val agentId = reference("agent_id", Agents).nullable()
    val isActive = bool("is_active").default(true)
    val config = jsonb<Map<String, Any>>("config", JsonbSerializer).default(emptyMap())
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(org.jetbrains.exposed.sql.functions.CurrentTimestamp)
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(org.jetbrains.exposed.sql.functions.CurrentTimestamp)
}

// ============================================
// 3. SESSIONS - 会话管理表
// ============================================
object Sessions : UUIDTable("sessions") {
    val sessionKey = varchar("session_key", 200).uniqueIndex()
    val userId = varchar("user_id", 100)
    val channelId = reference("channel_id", Channels)
    val agentId = reference("agent_id", Agents).nullable()
    val title = varchar("title", 200).nullable()
    val contextWindow = integer("context_window").default(10)
    val lastMessageAt = timestampWithTimeZone("last_message_at").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(org.jetbrains.exposed.sql.functions.CurrentTimestamp)
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(org.jetbrains.exposed.sql.functions.CurrentTimestamp)
    val metadata = jsonb<Map<String, Any>>("metadata", JsonbSerializer).default(emptyMap())
}

// ============================================
// 4. MESSAGES - 消息记录表
// ============================================
enum class MessageType {
    TEXT, IMAGE, VOICE, VIDEO, FILE, LOCATION, SYSTEM
}

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

enum class MessageStatus {
    PENDING, SENT, DELIVERED, FAILED, DELETED
}

object Messages : UUIDTable("messages") {
    val sessionId = reference("session_id", Sessions)
    val channelId = reference("channel_id", Channels)
    val messageId = varchar("message_id", 100).nullable()
    val replyToId = reference("reply_to_id", Messages).nullable()
    val role = enumerationByName<MessageRole>("role", 20)
    val type = enumerationByName<MessageType>("type", 20).default(MessageType.TEXT)
    val content = text("content")
    val contentEncrypted = bool("content_encrypted").default(false)
    val mediaUrl = text("media_url").nullable()
    val mediaSize = long("media_size").nullable()
    val mediaMimeType = varchar("media_mime_type", 100).nullable()
    val status = enumerationByName<MessageStatus>("status", 20).default(MessageStatus.SENT)
    val tokensUsed = integer("tokens_used").nullable()
    val latencyMs = integer("latency_ms").nullable()
    val errorMessage = text("error_message").nullable()
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(org.jetbrains.exposed.sql.functions.CurrentTimestamp)
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(org.jetbrains.exposed.sql.functions.CurrentTimestamp)
    val metadata = jsonb<Map<String, Any>>("metadata", JsonbSerializer).default(emptyMap())
}

// ============================================
// 5. CONFIGS - 系统配置表
// ============================================
enum class ConfigScope {
    GLOBAL, CHANNEL, USER, SESSION
}

object Configs : UUIDTable("configs") {
    val scope = enumerationByName<ConfigScope>("scope", 20).default(ConfigScope.GLOBAL)
    val scopeId = varchar("scope_id", 100).nullable()
    val key = varchar("key", 200)
    val value = text("value")
    val valueType = varchar("value_type", 20).default("string")
    val description = text("description").nullable()
    val isEncrypted = bool("is_encrypted").default(false)
    val isActive = bool("is_active").default(true)
    val createdAt = timestampWithTimeZone("created_at").defaultExpression(org.jetbrains.exposed.sql.functions.CurrentTimestamp)
    val updatedAt = timestampWithTimeZone("updated_at").defaultExpression(org.jetbrains.exposed.sql.functions.CurrentTimestamp)

    init {
        uniqueIndex(scope, scopeId, key)
    }
}

// ============================================
// JSONB 序列化器
// ============================================
object JsonbSerializer : org.jetbrains.exposed.sql.json.JsonSerializer<Map<String, Any>> {
    private val mapper = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun serialize(value: Map<String, Any>): String {
        return mapper.encodeToString(
            kotlinx.serialization.builtins.MapSerializer(String.serializer(), kotlinx.serialization.json.JsonElement.serializer()),
            value.mapValues { 
                when (val v = it.value) {
                    is String -> kotlinx.serialization.json.JsonPrimitive(v)
                    is Number -> kotlinx.serialization.json.JsonPrimitive(v)
                    is Boolean -> kotlinx.serialization.json.JsonPrimitive(v)
                    else -> kotlinx.serialization.json.JsonPrimitive(v.toString())
                }
            }
        )
    }

    override fun deserialize(value: String): Map<String, Any> {
        val element = mapper.parseToJsonElement(value)
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
}
