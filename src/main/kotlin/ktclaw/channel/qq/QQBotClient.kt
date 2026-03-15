package ktclaw.channel.qq

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * QQ Bot WebSocket 客户端
 * 实现QQ频道机器人的WebSocket连接管理、消息收发和事件处理
 *
 * 文档参考: https://bot.q.qq.com/wiki/develop/api-v2/
 */
class QQBotClient(
    private val appId: String,
    private val appSecret: String,
    private val token: String? = null,
    private val sandbox: Boolean = false
) {
    private val logger = LoggerFactory.getLogger(QQBotClient::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // HTTP 客户端
    private val httpClient = HttpClient(CIO) {
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

    // WebSocket 会话
    private var webSocketSession: DefaultClientWebSocketSession? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private val isConnected = AtomicBoolean(false)
    private val isReconnecting = AtomicBoolean(false)
    private val lastSequenceNumber = AtomicLong(0)
    private val sessionId: String? = null

    // 消息处理器
    private val messageHandler = QQMessageHandler(this)

    // 事件回调
    private var onMessageCallback: ((QQMessage) -> Unit)? = null
    private var onEventCallback: ((QQEvent) -> Unit)? = null
    private var onConnectCallback: (() -> Unit)? = null
    private var onDisconnectCallback: ((Throwable?) -> Unit)? = null

    // 消息发送队列
    private val messageQueue = Channel<QQSendMessage>(Channel.BUFFERED)

    // API 基础URL
    private val apiBaseUrl = if (sandbox) {
        "https://sandbox.api.sgroup.qq.com"
    } else {
        "https://api.sgroup.qq.com"
    }

    private val wsBaseUrl = if (sandbox) {
        "wss://sandbox.api.sgroup.qq.com/websocket"
    } else {
        "wss://api.sgroup.qq.com/websocket"
    }

    /**
     * 启动WebSocket连接
     */
    suspend fun connect() {
        if (isConnected.get()) {
            logger.warn("WebSocket already connected")
            return
        }

        try {
            logger.info("Connecting to QQ Bot WebSocket...")
            logger.info("AppID: $appId, Sandbox: $sandbox")

            // 获取WebSocket连接URL（如果有shard信息）
            val wsUrl = buildWebSocketUrl()

            httpClient.webSocket({
                url(wsUrl)
                header(HttpHeaders.Authorization, "QQBot $token")
            }) {
                webSocketSession = this
                isConnected.set(true)
                isReconnecting.set(false)

                logger.info("WebSocket connected successfully")
                onConnectCallback?.invoke()

                // 启动心跳任务
                startHeartbeat()

                // 启动消息处理循环
                handleWebSocketMessages()
            }
        } catch (e: Exception) {
            logger.error("WebSocket connection failed", e)
            isConnected.set(false)
            onDisconnectCallback?.invoke(e)
            scheduleReconnect()
        }
    }

    /**
     * 断开WebSocket连接
     */
    suspend fun disconnect() {
        logger.info("Disconnecting WebSocket...")
        isConnected.set(false)

        heartbeatJob?.cancel()
        heartbeatJob = null

        reconnectJob?.cancel()
        reconnectJob = null

        try {
            webSocketSession?.close()
        } catch (e: Exception) {
            logger.error("Error closing WebSocket", e)
        }
        webSocketSession = null

        onDisconnectCallback?.invoke(null)
        logger.info("WebSocket disconnected")
    }

    /**
     * 重新连接
     */
    suspend fun reconnect() {
        if (isReconnecting.getAndSet(true)) {
            logger.warn("Reconnection already in progress")
            return
        }

        try {
            disconnect()
            delay(5000) // 等待5秒后重连
            connect()
        } finally {
            isReconnecting.set(false)
        }
    }

    /**
     * 发送文本消息
     */
    suspend fun sendTextMessage(channelId: String, content: String, msgId: String? = null): Result<QQMessageResponse> {
        return sendMessage(channelId, QQMessageContent.Text(content), msgId)
    }

    /**
     * 发送图片消息
     */
    suspend fun sendImageMessage(
        channelId: String,
        imageUrl: String? = null,
        imageFile: ByteArray? = null,
        msgId: String? = null
    ): Result<QQMessageResponse> {
        val media = if (imageFile != null) {
            // 先上传图片获取URL
            val uploadResult = uploadMedia(imageFile, "image")
            uploadResult.getOrNull()?.let { QQMessageContent.Image(it.url) }
                ?: return Result.failure(Exception("Failed to upload image"))
        } else {
            QQMessageContent.Image(imageUrl!!)
        }
        return sendMessage(channelId, media, msgId)
    }

    /**
     * 发送语音消息
     */
    suspend fun sendVoiceMessage(
        channelId: String,
        voiceUrl: String? = null,
        voiceFile: ByteArray? = null,
        msgId: String? = null
    ): Result<QQMessageResponse> {
        val media = if (voiceFile != null) {
            val uploadResult = uploadMedia(voiceFile, "voice")
            uploadResult.getOrNull()?.let { QQMessageContent.Voice(it.url) }
                ?: return Result.failure(Exception("Failed to upload voice"))
        } else {
            QQMessageContent.Voice(voiceUrl!!)
        }
        return sendMessage(channelId, media, msgId)
    }

    /**
     * 发送文件消息
     */
    suspend fun sendFileMessage(
        channelId: String,
        fileUrl: String,
        fileName: String,
        msgId: String? = null
    ): Result<QQMessageResponse> {
        return sendMessage(channelId, QQMessageContent.File(fileUrl, fileName), msgId)
    }

    /**
     * 发送富媒体消息（支持图文混合）
     */
    suspend fun sendRichMessage(
        channelId: String,
        content: String,
        imageUrls: List<String> = emptyList(),
        msgId: String? = null
    ): Result<QQMessageResponse> {
        return sendMessage(channelId, QQMessageContent.Rich(content, imageUrls), msgId)
    }

    /**
     * 回复消息
     */
    suspend fun replyMessage(
        channelId: String,
        replyToMsgId: String,
        content: String,
        msgType: Int = 0
    ): Result<QQMessageResponse> {
        return sendMessage(channelId, QQMessageContent.Text(content), replyToMsgId)
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
     * 设置连接回调
     */
    fun onConnect(callback: () -> Unit) {
        onConnectCallback = callback
    }

    /**
     * 设置断开连接回调
     */
    fun onDisconnect(callback: (Throwable?) -> Unit) {
        onDisconnectCallback = callback
    }

    /**
     * 获取Access