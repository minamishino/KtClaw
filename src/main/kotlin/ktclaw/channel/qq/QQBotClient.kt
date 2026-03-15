package ktclaw.channel.qq

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

/**
 * QQ Bot Webhook 客户端
 * 实现QQ频道机器人的Webhook接收和HTTP API消息发送
 *
 * 文档参考: https://bot.q.qq.com/wiki/develop/api-v2/
 */
class QQBotClient(
    val appId: String,
    val appSecret: String,
    val token: String? = null,
    val sandbox: Boolean = false
) {
    private val logger = LoggerFactory.getLogger(QQBotClient::class.java)
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    // HTTP 客户端
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
        }
    }

    // 消息处理器
    val messageReceiver = QQMessageReceiver(this)
    val messageSender = QQMessageSender(this, httpClient, json)

    // 事件回调
    private var onMessageCallback: ((QQMessage) -> Unit)? = null
    private var onEventCallback: ((QQEvent) -> Unit)? = null
    private var onReadyCallback: (() -> Unit)? = null

    // API 基础URL
    val apiBaseUrl = if (sandbox) {
        "https://sandbox.api.sgroup.qq.com"
    } else {
        "https://api.sgroup.qq.com"
    }

    // Access Token 缓存
    private var cachedAccessToken: String? = null
    private var tokenExpireTime: Long = 0

    /**
     * 启动客户端（Webhook模式下仅初始化）
     */
    suspend fun start() {
        logger.info("Starting QQ Bot Webhook client...")
        logger.info("AppID: $appId, Sandbox: $sandbox")
        
        // 预获取 Access Token
        refreshAccessToken()
        
        onReadyCallback?.invoke()
        logger.info("QQ Bot Webhook client started successfully")
    }

    /**
     * 停止客户端
     */
    suspend fun stop() {
        logger.info("Stopping QQ Bot Webhook client...")
        try {
            httpClient.close()
        } catch (e: Exception) {
            logger.error("Error closing HTTP client", e)
        }
        logger.info("QQ Bot Webhook client stopped")
    }

    /**
     * 获取 Access Token
     */
    suspend fun getAccessToken(): String {
        val now = System.currentTimeMillis()
        if (cachedAccessToken != null && now < tokenExpireTime - 60000) {
            return cachedAccessToken!!
        }
        return refreshAccessToken()
    }

    /**
     * 刷新 Access Token
     */
    private suspend fun refreshAccessToken(): String {
        try {
            logger.debug("Refreshing access token...")
            
            val response = httpClient.post("$apiBaseUrl/auth/token") {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("appId", appId)
                    put("clientSecret", appSecret)
                }.toString())
            }

            val responseBody = response.bodyAsText()
            val tokenResponse = json.decodeFromString<QQTokenResponse>(responseBody)
            
            cachedAccessToken = tokenResponse.accessToken
            tokenExpireTime = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
            
            logger.info("Access token refreshed successfully")
            return cachedAccessToken!!
        } catch (e: Exception) {
            logger.error("Failed to refresh access token", e)
            throw e
        }
    }

    /**
     * 处理 Webhook 事件
     * @param eventData 事件数据
     */
    suspend fun handleWebhookEvent(eventData: String) {
        try {
            val jsonObject = json.parseToJsonElement(eventData).jsonObject
            val eventType = jsonObject["t"]?.jsonPrimitive?.contentOrNull
            val data = jsonObject["d"]?.jsonObject

            logger.debug("Received webhook event: $eventType")

            when (eventType) {
                // 频道消息事件
                "MESSAGE_CREATE" -> handleMessageCreate(data)
                "AT_MESSAGE_CREATE" -> handleAtMessageCreate(data)
                
                // 私聊消息事件
                "DIRECT_MESSAGE_CREATE" -> handleDirectMessageCreate(data)
                
                // 群消息事件 (C2C)
                "C2C_MESSAGE_CREATE" -> handleC2CMessageCreate(data)
                "GROUP_AT_MESSAGE_CREATE" -> handleGroupAtMessageCreate(data)
                
                // 成员事件
                "GUILD_MEMBER_ADD" -> handleGuildMemberAdd(data)
                "GUILD_MEMBER_REMOVE" -> handleGuildMemberRemove(data)
                
                // 频道事件
                "CHANNEL_CREATE" -> handleChannelCreate(data)
                "CHANNEL_DELETE" -> handleChannelDelete(data)
                
                else -> {
                    logger.debug("Unhandled event type: $eventType")
                    onEventCallback?.invoke(QQEvent.Unknown(eventType, data))
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to handle webhook event", e)
        }
    }

    /**
     * 处理消息创建事件
     */
    private suspend fun handleMessageCreate(data: JsonObject?) {
        data?.let {
            try {
                val message = json.decodeFromJsonElement<QQMessage>(it)
                onMessageCallback?.invoke(message)
                logger.info("Received message from ${message.author?.username}: ${message.content}")
            } catch (e: Exception) {
                logger.error("Failed to parse message", e)
            }
        }
    }

    /**
     * 处理 @机器人消息事件
     */
    private suspend fun handleAtMessageCreate(data: JsonObject?) {
        data?.let {
            try {
                val message = json.decodeFromJsonElement<QQMessage>(it)
                onMessageCallback?.invoke(message.copy(mentionsMe = true))
                logger.info("Received @ message from ${message.author?.username}: ${message.content}")
            } catch (e: Exception) {
                logger.error("Failed to parse @ message", e)
            }
        }
    }

    /**
     * 处理私聊消息事件
     */
    private suspend fun handleDirectMessageCreate(data: JsonObject?) {
        data?.let {
            try {
                val message = json.decodeFromJsonElement<QQMessage>(it)
                onMessageCallback?.invoke(message.copy(isDirect = true))
                logger.info("Received direct message from ${message.author?.username}: ${message.content}")
            } catch (e: Exception) {
                logger.error("Failed to parse direct message", e)
            }
        }
    }

    /**
     * 处理 C2C 消息事件
     */
    private suspend fun handleC2CMessageCreate(data: JsonObject?) {
        data?.let {
            try {
                val message = json.decodeFromJsonElement<QQMessage>(it)
                onMessageCallback?.invoke(message.copy(isC2C = true))
                logger.info("Received C2C message: ${message.content}")
            } catch (e: Exception) {
                logger.error("Failed to parse C2C message", e)
            }
        }
    }

    /**
     * 处理群 @消息事件
     */
    private suspend fun handleGroupAtMessageCreate(data: JsonObject?) {
        data?.let {
            try {
                val message = json.decodeFromJsonElement<QQMessage>(it)
                onMessageCallback?.invoke(message.copy(isGroup = true, mentionsMe = true))
                logger.info("Received group @ message: ${message.content}")
            } catch (e: Exception) {
                logger.error("Failed to parse group @ message", e)
            }
        }
    }

    /**
     * 处理成员加入事件
     */
    private suspend fun handleGuildMemberAdd(data: JsonObject?) {
        data?.let {
            val user = it["user"]?.jsonObject
            val guildId = it["guild_id"]?.jsonPrimitive?.contentOrNull
            val username = user?.get("username")?.jsonPrimitive?.contentOrNull
            logger.info("Member joined: $username in guild $guildId")
            onEventCallback?.invoke(QQEvent.MemberJoined(username, guildId))
        }
    }

    /**
     * 处理成员离开事件
     */
    private suspend fun handleGuildMemberRemove(data: JsonObject?) {
        data?.let {
            val user = it["user"]?.jsonObject
            val guildId = it["guild_id"]?.jsonPrimitive?.contentOrNull
            val username = user?.get("username")?.jsonPrimitive?.contentOrNull
            logger.info("Member left: $username from guild $guildId")
            onEventCallback?.invoke(QQEvent.MemberLeft(username, guildId))
        }
    }

    /**
     * 处理频道创建事件
     */
    private suspend fun handleChannelCreate(data: JsonObject?) {
        data?.let {
            val channelId = it["id"]?.jsonPrimitive?.contentOrNull
            val channelName = it["name"]?.jsonPrimitive?.contentOrNull
            logger.info("Channel created: $channelName ($channelId)")
            onEventCallback?.invoke(QQEvent.ChannelCreated(channelId, channelName))
        }
    }

    /**
     * 处理频道删除事件
     */
    private suspend fun handleChannelDelete(data: JsonObject?) {
        data?.let {
            val channelId = it["id"]?.jsonPrimitive?.contentOrNull
            val channelName = it["name"]?.jsonPrimitive?.contentOrNull
            logger.info("Channel deleted: $channelName ($channelId)")
            onEventCallback?.invoke(QQEvent.ChannelDeleted(channelId, channelName))
        }
    }

    /**
     * 设置消息回调
     */
    fun onMessage(callback: (QQMessage) -> Unit) {
        onMessageCallback = callback
    }

    /**
     * 设置事件回调
     */
    fun onEvent(callback: (QQEvent) -> Unit) {
        onEventCallback = callback
    }

    /**
     * 设置就绪回调
     */
    fun onReady(callback: () -> Unit) {
        onReadyCallback = callback
    }
}

/**
 * Token 响应
 */
@Serializable
data class QQTokenResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("token_type")
    val tokenType: String = "Bearer"
)

/**
 * QQ 事件密封类
 */
sealed class QQEvent {
    data class Unknown(val eventType: String?, val data: JsonObject?) : QQEvent()
    data class MemberJoined(val username: String?, val guildId: String?) : QQEvent()
    data class MemberLeft(val username: String?, val guildId: String?) : QQEvent()
    data class ChannelCreated(val channelId: String?, val channelName: String?) : QQEvent()
    data class ChannelDeleted(val channelId: String?, val channelName: String?) : QQEvent()
    data class MessageDeleted(val messageId: String?, val channelId: String?) : QQEvent()
}
