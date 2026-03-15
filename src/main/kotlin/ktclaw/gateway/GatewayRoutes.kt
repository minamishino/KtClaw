package ktclaw.gateway

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Gateway Routes - REST API 路由和认证中间件
 */
class GatewayRoutes(private val webSocketGateway: WebSocketGateway) {
    private val logger = LoggerFactory.getLogger(GatewayRoutes::class.java)
    
    // 简单的 token 存储 (生产环境应使用数据库存储)
    private val apiTokens = mutableSetOf<String>()
    private val userSessions = mutableMapOf<String, UserSession>()
    
    init {
        // 添加默认的 admin token
        apiTokens.add("ktclaw-admin-token")
    }
    
    /**
     * 注册所有路由
     */
    fun Application.registerRoutes() {
        // 认证配置
        install(Authentication) {
            bearer("auth-bearer") {
                realm = "KtClaw Gateway"
                authenticate { tokenCredential ->
                    if (apiTokens.contains(tokenCredential.token)) {
                        UserIdPrincipal("admin")
                    } else {
                        null
                    }
                }
            }
            
            session<UserSession>("auth-session") {
                validate { session ->
                    if (session.isValid()) session else null
                }
                challenge {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired session"))
                }
            }
        }
        
        routing {
            // 健康检查 (公开)
            get("/health") {
                val stats = webSocketGateway.getStats()
                call.respond(HealthResponse(
                    status = "ok",
                    service = "KtClaw Gateway",
                    version = "0.1.0",
                    connections = stats
                ))
            }
            
            // 公开 API
            publicRoutes()
            
            // 认证 API
            authenticate("auth-bearer", "auth-session", strategy = AuthenticationStrategy.FirstSuccessful) {
                protectedRoutes()
            }
            
            // WebSocket 路由
            webSocketGateway.registerWebSocketRoutes()
        }
    }
    
    /**
     * 公开路由
     */
    private fun Route.publicRoutes() {
        // 认证相关
        route("/auth") {
            // 登录获取 session
            post("/login") {
                val request = call.receive<LoginRequest>()
                
                // 验证凭据 (简化版，实际应查询数据库)
                if (validateCredentials(request.username, request.password)) {
                    val session = createSession(request.username)
                    userSessions[session.token] = session
                    call.respond(LoginResponse(
                        success = true,
                        token = session.token,
                        expiresAt = session.expiresAt
                    ))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, LoginResponse(
                        success = false,
                        error = "Invalid credentials"
                    ))
                }
            }
            
            // 登出
            post("/logout") {
                val token = call.request.header("Authorization")?.removePrefix("Bearer ")
                token?.let { userSessions.remove(it) }
                call.respond(mapOf("success" to true, "message" to "Logged out"))
            }
            
            // 验证 token
            get("/verify") {
                val token = call.request.header("Authorization")?.removePrefix("Bearer ")
                val valid = token != null && (apiTokens.contains(token) || userSessions[token]?.isValid() == true)
                call.respond(mapOf("valid" to valid))
            }
        }
    }
    
    /**
     * 受保护路由
     */
    private fun Route.protectedRoutes() {
        // Gateway 管理 API
        route("/api/v1/gateway") {
            // 获取连接统计
            get("/stats") {
                val stats = webSocketGateway.getStats()
                call.respond(stats)
            }
            
            // 获取连接列表
            get("/connections") {
                val connections = webSocketGateway.getConnections()
                call.respond(mapOf("connections" to connections))
            }
            
            // 断开指定连接
            delete("/connections/{sessionId}") {
                val sessionId = call.parameters["sessionId"]
                if (sessionId != null) {
                    webSocketGateway.disconnect(sessionId)
                    call.respond(mapOf("success" to true, "message" to "Connection closed"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Session ID required"))
                }
            }
        }
        
        // 消息推送 API
        route("/api/v1/push") {
            // 推送到指定 Agent
            post("/agent/{agentId}") {
                val agentId = call.parameters["agentId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Agent ID required"))
                val request = call.receive<PushRequest>()
                
                val payload = TaskPayload(
                    taskId = generateTaskId(),
                    taskType = request.type,
                    data = request.data,
                    priority = request.priority
                )
                
                val sent = webSocketGateway.pushTaskToAgent(agentId, payload)
                call.respond(PushResponse(success = sent > 0, sent = sent))
            }
            
            // 推送到指定用户
            post("/user/{userId}") {
                val userId = call.parameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ID required"))
                val request = call.receive<PushRequest>()
                
                val payload = MessageUpdatePayload(
                    messageId = generateTaskId(),
                    channelId = request.data["channelId"] ?: "default",
                    content = request.data["content"] ?: "",
                    status = request.data["status"] ?: "pending",
                    timestamp = System.currentTimeMillis()
                )
                
                val sent = webSocketGateway.pushMessageUpdate(userId, payload)
                call.respond(PushResponse(success = sent > 0, sent = sent))
            }
            
            // 推送到通道
            post("/channel/{channelId}") {
                val channelId = call.parameters["channelId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Channel ID required"))
                val request = call.receive<PushRequest>()
                
                val message = GatewayMessage(
                    type = MessageType.CUSTOM,
                    payload = request.data
                )
                
                val sent = webSocketGateway.sendToChannel(channelId, message)
                call.respond(PushResponse(success = sent > 0, sent = sent))
            }
            
            // 广播消息
            post("/broadcast") {
                val request = call.receive<BroadcastRequest>()
                
                val notification = SystemNotificationPayload(
                    level = request.level,
                    title = request.title,
                    message = request.message,
                    data = request.data
                )
                
                val target = when (request.target) {
                    "agents" -> NotificationTarget.AGENTS
                    "clients" -> NotificationTarget.CLIENTS
                    else -> NotificationTarget.ALL
                }
                
                val sent = webSocketGateway.pushSystemNotification(notification, target)
                call.respond(PushResponse(success = sent > 0, sent = sent))
            }
        }
        
        // Agent 管理 API
        route("/api/v1/agents") {
            // 获取在线 Agent 列表
            get {
                val connections = webSocketGateway.getConnections()
                    .filter { it.type == WebSocketGateway.ConnectionType.AGENT.name }
                    .groupBy { it.entityId }
                    .map { (agentId, conns) ->
                        mapOf(
                            "agentId" to (agentId ?: "unknown"),
                            "connections" to conns.size,
                            "sessions" to conns.map { it.sessionId }
                        )
                    }
                call.respond(mapOf("agents" to connections))
            }
            
            // 获取指定 Agent 的连接信息
            get("/{agentId}/connections") {
                val agentId = call.parameters["agentId"]
                val connections = webSocketGateway.getConnections()
                    .filter { it.entityId == agentId && it.type == WebSocketGateway.ConnectionType.AGENT.name }
                call.respond(mapOf("connections" to connections))
            }
            
            // 向 Agent 发送命令
            post("/{agentId}/command") {
                val agentId = call.parameters["agentId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Agent ID required"))
                val request = call.receive<CommandRequest>()
                
                val payload = TaskPayload(
                    taskId = generateTaskId(),
                    taskType = "command",
                    data = mapOf(
                        "command" to request.command,
                        "args" to request.args.joinToString(",")
                    ) + request.options,
                    priority = request.priority
                )
                
                val sent = webSocketGateway.pushTaskToAgent(agentId, payload)
                call.respond(mapOf(
                    "success" to (sent > 0),
                    "sent" to sent,
                    "command" to request.command
                ))
            }
        }
        
        // 通道管理 API
        route("/api/v1/channels") {
            // 获取活跃通道列表
            get {
                val connections = webSocketGateway.getConnections()
                    .filter { it.type == WebSocketGateway.ConnectionType.CHANNEL.name }
                    .groupBy { it.entityId }
                    .map { (channelId, conns) ->
                        mapOf(
                            "channelId" to (channelId ?: "unknown"),
                            "connections" to conns.size
                        )
                    }
                call.respond(mapOf("channels" to connections))
            }
        }
        
        // Token 管理 API (仅 admin)
        route("/api/v1/admin") {
            // 生成新 token
            post("/tokens") {
                val newToken = generateToken()
                apiTokens.add(newToken)
                call.respond(mapOf("token" to newToken, "created" to true))
            }
            
            // 列出所有 token
            get("/tokens") {
                call.respond(mapOf("tokens" to apiTokens.map { it.take(8) + "..." }))
            }
            
            // 撤销 token
            delete("/tokens/{token}") {
                val token = call.parameters["token"]
                if (token != null && apiTokens.remove(token)) {
                    call.respond(mapOf("success" to true, "message" to "Token revoked"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Token not found"))
                }
            }
        }
    }
    
    // 辅助方法
    
    private fun validateCredentials(username: String, password: String): Boolean {
        // 简化版验证，实际应查询数据库
        return (username == "admin" && password == "admin") ||
               (username == "user" && password == "user")
    }
    
    private fun createSession(username: String): UserSession {
        return UserSession(
            username = username,
            token = generateToken(),
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24小时
        )
    }
    
    private fun generateToken(): String {
        return "ktclaw-${System.currentTimeMillis()}-${(Math.random() * 10000).toInt()}"
    }
    
    private fun generateTaskId(): String {
        return "task-${System.currentTimeMillis()}-${(Math.random() * 10000).toInt()}"
    }
}

// 数据类

@Serializable
data class UserSession(
    val username: String,
    val token: String,
    val createdAt: Long,
    val expiresAt: Long
) {
    fun isValid(): Boolean {
        return System.currentTimeMillis() < expiresAt
    }
}

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val expiresAt: Long? = null,
    val error: String? = null
)

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val connections: ConnectionStats
)

@Serializable
data class PushRequest(
    val type: String,
    val data: Map<String, String> = emptyMap(),
    val priority: Int = 0
)

@Serializable
data class BroadcastRequest(
    val title: String,
    val message: String,
    val level: String = "info",
    val target: String = "all", // all, agents, clients
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class PushResponse(
    val success: Boolean,
    val sent: Int
)

@Serializable
data class CommandRequest(
    val command: String,
    val args: List<String> = emptyList(),
    val options: Map<String, String> = emptyMap(),
    val priority: Int = 0
)
