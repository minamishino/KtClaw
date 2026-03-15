package ktclaw.openclaw.gateway

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * OpenClaw Gateway 服务器
 * 
 * 职责：
 * 1. WebSocket 服务器 (端口 18789)
 * 2. Agent 注册和管理
 * 3. 消息路由
 * 4. 会话生命周期管理
 */
class GatewayServer(
    private val port: Int = 18789,
    private val host: String = "0.0.0.0",
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val logger = LoggerFactory.getLogger(GatewayServer::class.java)
    private val json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
    }

    // 连接管理
    private val connections = ConcurrentHashMap<String, GatewayConnection>()
    private val sessionCounter = AtomicLong(0)

    // Agent 注册表
    private val registeredAgents = ConcurrentHashMap<String, AgentRegistration>()

    // 消息路由表
    private val messageRouter = MessageRouter()

    // 网关事件流
    private val _events = MutableSharedFlow<GatewayEvent>(extraBufferCapacity = 1000)
    val events: SharedFlow<GatewayEvent> = _events.asSharedFlow()

    // 服务器状态
    private val _status = MutableStateFlow<GatewayStatus>(GatewayStatus.Stopped)
    val status: StateFlow<GatewayStatus> = _status.asStateFlow()

    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    /**
     * 启动 Gateway 服务器
     */
    fun start() {
        if (_status.value is GatewayStatus.Running) {
            logger.warn("Gateway server is already running on port $port")
            return
        }

        logger.info("Starting OpenClaw Gateway server on $host:$port...")
        _status.value = GatewayStatus.Starting

        server = embeddedServer(Netty, port = port, host = host) {
            install(CallLogging) {
                level = Level.INFO
            }

            install(WebSockets) {
                pingPeriodMillis = 30000
                timeoutMillis = 60000
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                // Agent WebSocket 连接端点
                webSocket("/ws/agent/{agentId}") {
                    val agentId = call.parameters["agentId"] ?: "unknown"
                    handleAgentConnection(this, agentId)
                }

                // 客户端 WebSocket 连接端点
                webSocket("/ws/client/{clientId}") {
                    val clientId = call.parameters["clientId"] ?: "anonymous"
                    handleClientConnection(this, clientId)
                }

                // Channel WebSocket 连接端点
                webSocket("/ws/channel/{channelType}/{channelId}") {
                    val channelType = call.parameters["channelType"] ?: "unknown"
                    val channelId = call.parameters["channelId"] ?: "default"
                    handleChannelConnection(this, channelType, channelId)
                }

                // 管理端点
                webSocket("/ws/admin") {
                    handleAdminConnection(this)
                }
            }
        }.start(wait = false)

        _status.value = GatewayStatus.Running(port)
        _events.tryEmit(GatewayEvent.ServerStarted(host, port))
        logger.info("OpenClaw Gateway server started successfully on port $port")

        // 启动心跳检测
        scope.launch {
            startHeartbeatCheck()
        }
    }

    /**
     * 停止 Gateway 服务器
     */
    fun stop() {
        logger.info("Stopping OpenClaw Gateway server...")
        _status.value = GatewayStatus.Stopping

        // 断开所有连接
        connections.values.forEach { it.session.close() }
        connections.clear()
        registeredAgents.clear()

        scope.cancel()
        server?.stop(1000, 5000)

        _status.value = GatewayStatus.Stopped
        _events.tryEmit(GatewayEvent.ServerStopped)
        logger.info("OpenClaw Gateway server stopped")
    }

    // ============================================
    // WebSocket 连接处理
    // ============================================

    private suspend fun handleAgentConnection(session: DefaultWebSocketSession, agentId: String) {
        val sessionId = generateSessionId()
        logger.info("Agent connection request: agentId=$agentId, sessionId=$sessionId")

        // 创建连接对象
        val connection = GatewayConnection(
            sessionId = sessionId,
            session = session,
            type = ConnectionType.AGENT,
            entityId = agentId
        )

        // 注册连接
        connections[sessionId] = connection

        // 发送连接成功消息
        sendToConnection(sessionId, GatewayMessage(
            type = MessageType.CONNECTED,
            payload = mapOf(
                "sessionId" to sessionId,
                "agentId" to agentId,
                "timestamp" to System.currentTimeMillis()
            )
        ))

        _events.tryEmit(GatewayEvent.AgentConnected(agentId, sessionId))

        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> handleAgentMessage(sessionId, agentId, frame.readText())
                    is Frame.Close -> break
                    else -> {}
                }
            }
        } catch (e: Exception) {
            logger.error("Agent connection error: $sessionId", e)
        } finally {
            disconnect(sessionId)
            _events.tryEmit(GatewayEvent.AgentDisconnected(agentId, sessionId))
        }
    }

    private suspend fun handleClientConnection(session: DefaultWebSocketSession, clientId: String) {
        val sessionId = generateSessionId()
        logger.info("Client connection: clientId=$clientId, sessionId=$sessionId")

        val connection = GatewayConnection(
            sessionId = sessionId,
            session = session,
            type = ConnectionType.CLIENT,
            entityId = clientId
        )

        connections[sessionId] = connection

        sendToConnection(sessionId, GatewayMessage(
            type = MessageType.CONNECTED,
            payload = mapOf(
                "sessionId" to sessionId,
                "clientId" to clientId,
                "timestamp" to System.currentTimeMillis()
            )
        ))

        _events.tryEmit(GatewayEvent.ClientConnected(clientId, sessionId))

        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> handleClientMessage(sessionId, clientId, frame.readText())
                    is Frame.Close -> break
                    else -> {}
                }
            }
        } catch (e: Exception) {
            logger.error("Client connection error: $sessionId", e)
        } finally {
            disconnect(sessionId)
            _events.tryEmit(GatewayEvent.ClientDisconnected(clientId, sessionId))
        }
    }

    private suspend fun handleChannelConnection(
        session: DefaultWebSocketSession,
        channelType: String,
        channelId: String
    ) {
        val sessionId = generateSessionId()
        logger.info("Channel connection: type=$channelType, channelId=$channelId, sessionId=$sessionId")

        val connection = GatewayConnection(
            sessionId = sessionId,
            session = session,
