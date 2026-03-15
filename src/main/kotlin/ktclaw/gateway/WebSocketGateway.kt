package ktclaw.gateway

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * WebSocket Gateway - 管理 WebSocket 连接和实时消息推送
 */
class WebSocketGateway {
    private val logger = LoggerFactory.getLogger(WebSocketGateway::class.java)

    // 连接管理
    private val connections = ConcurrentHashMap<String, WebSocketConnection>()
    private val sessionCounter = AtomicLong(0)

    // 按类型分组的连接
    private val agentConnections = ConcurrentHashMap<String, MutableSet<String>>() // agentId -> sessionIds
    private val userConnections = ConcurrentHashMap<String, MutableSet<String>>()   // userId -> sessionIds
    private val channelConnections = ConcurrentHashMap<String, MutableSet<String>>() // channelId -> sessionIds

    /**
     * WebSocket 连接信息
     */
    data class WebSocketConnection(
        val sessionId: String,
        val session: DefaultWebSocketSession,
        val connectionType: ConnectionType,
        val entityId: String? = null,  // agentId, userId, or channelId
        val connectedAt: Long = System.currentTimeMillis()
    )

    /**
     * 连接类型
     */
    enum class ConnectionType {
        AGENT,      // Agent 连接
        CLIENT,     // 客户端连接
        CHANNEL,    // 通道连接
        ADMIN       // 管理员连接
    }

    /**
     * 注册 WebSocket 路由
     */
    fun Application.registerWebSocketRoutes() {
        routing {
            // Agent WebSocket 连接
            webSocket("/ws/agent/{agentId}") {
                val agentId = call.parameters["agentId"] ?: "unknown"
                handleConnection(this, ConnectionType.AGENT, agentId)
            }

            // 客户端 WebSocket 连接
            webSocket("/ws/client/{userId}") {
                val userId = call.parameters["userId"] ?: "anonymous"
                handleConnection(this, ConnectionType.CLIENT, userId)
            }

            // 通道 WebSocket 连接
            webSocket("/ws/channel/{channelId}") {
                val channelId = call.parameters["channelId"] ?: "default"
                handleConnection(this, ConnectionType.CHANNEL, channelId)
            }

            // 管理员 WebSocket 连接
            webSocket("/ws/admin") {
                handleConnection(this, ConnectionType.ADMIN, "admin")
            }
        }
    }

    /**
     * 处理新的 WebSocket 连接
     */
    private suspend fun handleConnection(
        session: DefaultWebSocketSession,
        type: ConnectionType,
        entityId: String
    ) {
        val sessionId = generateSessionId()
        val connection = WebSocketConnection(
            sessionId = sessionId,
            session = session,
            connectionType = type,
            entityId = entityId
        )

        // 保存连接
        connections[sessionId] = connection
        registerConnectionByType(type, entityId, sessionId)

        logger.info("WebSocket connected: sessionId=$sessionId, type=$type, entityId=$entityId")

        // 发送连接成功消息
        sendMessage(sessionId, GatewayMessage(
            type = MessageType.CONNECTED,
            payload = mapOf(
                "sessionId" to sessionId,
                "type" to type.name,
                "timestamp" to System.currentTimeMillis()
            )
        ))

        try {
            // 处理传入消息
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> handleIncomingMessage(sessionId, frame.readText())
                    is Frame.Close -> {
                        logger.debug("WebSocket close frame received: $sessionId")
                        break
                    }
                    else -> { /* ignore */ }
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            logger.debug("WebSocket closed normally: $sessionId")
        } catch (e: Exception) {
            logger.error("WebSocket error for session $sessionId: ${e.message}", e)
        } finally {
            disconnect(sessionId)
        }
    }

    /**
     * 处理传入的消息
     */
    private suspend fun handleIncomingMessage(sessionId: String, text: String) {
        try {
            val message = Json.decodeFromString<ClientMessage>(text)
            val connection = connections[sessionId]

            logger.debug("Received message from $sessionId: type=${message.type}")

            when (message.type) {
                "ping" -> {
                    sendMessage(sessionId, GatewayMessage(
                        type = MessageType.PONG,
                        payload = mapOf("timestamp" to System.currentTimeMillis())
                    ))
                }
                "subscribe" -> {
                    // 处理订阅请求
                    val channel = message.payload["channel"] as? String
                    logger.info("Session $sessionId subscribed to channel: $channel")
                }
                "unsubscribe" -> {
                    // 处理取消订阅
                    val channel = message.payload["channel"] as? String
                    logger.info("Session $sessionId unsubscribed from channel: $channel")
                }
                else -> {
                    // 转发消息到适当的处理器
                    logger.debug("Unhandled message type: ${message.type}")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process message from $sessionId: ${e.message}")
            sendMessage(sessionId, GatewayMessage(
                type = MessageType.ERROR,
                payload = mapOf("error" to "Invalid message format")
            ))
        }
    }

    /**
     * 断开连接
     */
    fun disconnect(sessionId: String) {
        val connection = connections.remove(sessionId)
        connection?.let {
            unregisterConnectionByType(it.connectionType, it.entityId, sessionId)
            logger.info("WebSocket disconnected: $sessionId")
        }
    }

    /**
     * 发送消息到指定会话
     */
    suspend fun sendMessage(sessionId: String, message: GatewayMessage): Boolean {
        val connection = connections[sessionId]
        return if (connection != null) {
            try {
                val json = Json.encodeToString(message)
                connection.session.send(Frame.Text(json))
                true
            } catch (e: Exception) {
                logger.error("Failed to send message to $sessionId: ${e.message}")
                disconnect(sessionId)
                false
            }
        } else {
            false
        }
    }

    /**
     * 发送消息到指定 Agent
     */
    suspend fun sendToAgent(agentId: String, message: GatewayMessage): Int {
        val sessionIds = agentConnections[agentId] ?: emptySet()
        var sent = 0
        for (sessionId in sessionIds) {
            if (sendMessage(sessionId, message)) sent++
        }
        return sent
    }

    /**
     * 发送消息到指定用户
     */
    suspend fun sendToUser(userId: String, message: GatewayMessage): Int {
        val sessionIds = userConnections[userId] ?: emptySet()
        var sent = 0
        for (sessionId in sessionIds) {
            if (sendMessage(sessionId, message)) sent++
        }
        return sent
    }

    /**
     * 发送消息到指定通道
     */
    suspend fun sendToChannel(channelId: String, message: GatewayMessage): Int {
        val sessionIds = channelConnections[channelId] ?: emptySet()
        var sent = 0
        for (sessionId in