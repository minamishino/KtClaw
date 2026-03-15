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
import java.util.concurrent.atomic.AtomicReference

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
    private val sandbox: Boolean = false,
    private val shardId: Int = 0,
    private val shardCount: Int = 1
) {
    private val logger = LoggerFactory.getLogger(QQBotClient::class.java)
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
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
    private var messageProcessJob: Job? = null
    private val isConnected = AtomicBoolean(false)
    private val isReconnecting = AtomicBoolean(false)
    private val lastSequenceNumber = AtomicLong(0)
    private val sessionIdRef = AtomicReference<String?>(null)
    private val heartbeatInterval = AtomicLong(45000) // 默认45秒
    private var lastHeartbeatAck = AtomicLong(0)
    private val missedHeartbeats = AtomicLong(0)

    // 重连配置
    private val maxReconnectAttempts = 10
    private val reconnectDelayMs = 5000L
    private val maxReconnectDelayMs = 60000L
    private var reconnectAttempts = 0

    // 消息处理器
    val messageReceiver = QQMessageReceiver(this)
    val messageSender = QQMessageSender(this, httpClient, json)
    val eventHandler = QQEventHandler(this)

    // 事件回调
    private var onMessageCallback: ((QQMessage) -> Unit)? = null
    private var onEventCallback: ((QQEvent) -> Unit)? = null
    private var onConnectCallback: (() -> Unit)? = null
    private var onDisconnectCallback: ((Throwable?) -> Unit)? = null
    private var onReadyCallback: ((QQReadyEvent) -> Unit)? = null

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

    // 协程作用域
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 启动WebSocket连接
     */
    suspend fun connect() {
        if (isConnected.get()) {
            logger.warn("WebSocket already connected")
            return
        }

        if (isReconnecting.get()) {
            logger.warn("Reconnection already in progress")
            return
        }

        try {
            logger.info("Connecting to QQ Bot WebSocket...")
            logger.info("AppID: $appId, Sandbox: $sandbox, Shard: $shardId/$shardCount")

            // 获取WebSocket连接URL
            val wsUrl = buildWebSocketUrl()

            httpClient.webSocket({
                url(wsUrl)
                // 使用 Access Token 鉴权
                val authToken = getAccessToken()
                header(HttpHeaders.Authorization, "QQBot $authToken")
            }) {
                webSocketSession = this
                isConnected.set(true)
                isReconnecting.set(false)
                reconnectAttempts = 0

                logger.info("WebSocket connected successfully")

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
        isReconnecting.set(false)

        heartbeatJob?.cancel()
        heartbeatJob = null

        reconnectJob?.cancel()
        reconnectJob = null

        messageProcessJob?.cancel()
        messageProcessJob = null

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
            val delayMs = calculateReconnectDelay()
            logger.info("Reconnecting in ${delayMs}ms... (attempt ${reconnectAttempts + 1}/$maxReconnectAttempts)")
            delay(delayMs)
            connect()
        } catch (e: Exception) {
            logger.error("Reconnection failed", e)
            isReconnecting.set(false)
            scheduleReconnect()
        }
    }

    /**
     * 计算重连延迟（指数退避）
     */
    private fun calculateReconnectDelay(): Long {
        reconnectAttempts++
        if (reconnectAttempts >= maxReconnectAttempts) {
            logger.error("Max reconnection attempts reached")
            return maxReconnectDelayMs
        }
        val delay = reconnectDelayMs * (1 shl (reconnectAttempts - 1))
        return minOf(delay, maxReconnectDelayMs)
    }

    /**
     * 调度重连
     */
    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        if (!isConnected.get() && !isReconnecting.get()) {
            reconnectJob = clientScope.launch {
                reconnect()
            }
        }
    }

    /**
     * 构建WebSocket URL
     */
    private fun buildWebSocketUrl(): String {
        return "$wsBaseUrl"
    }

    /**
     * 处理WebSocket消息循环
     */
    private suspend fun DefaultClientWebSocketSession.handleWebSocketMessages() {
        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        handleWebSocketMessage(text)
                    }
                    is Frame.Binary -> {
                        logger.debug("Received binary frame")
                    }
                    is Frame.Close -> {
                        logger.info("Received close frame: ${frame.readReason()}")
                        isConnected.set(false)
                        scheduleReconnect()
                    }
                    else -> {
                        logger.debug("Received frame: ${