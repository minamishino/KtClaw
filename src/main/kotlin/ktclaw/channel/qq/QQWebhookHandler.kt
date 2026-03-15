package ktclaw.channel.qq

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.*

/**
 * QQ Bot Webhook 处理器
 * 处理 QQ 服务器推送的事件和消息
 *
 * 文档: https://bot.q.qq.com/wiki/develop/api-v2/
 */
class QQWebhookHandler(
    private val appId: String,
    private val appSecret: String,
    private val messageHandler: suspend (QQWebhookPayload) -> Unit
) {
    private val logger = LoggerFactory.getLogger(QQWebhookHandler::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 处理 Webhook 请求
     */
    suspend fun handleWebhook(call: ApplicationCall) {
        try {
            // 获取请求头和签名
            val signature = call.request.header("X-Signature")
            val timestamp = call.request.header("X-Timestamp")
            val eventType = call.request.header("X-Bot-Event")

            // 读取请求体
            val body = call.receiveText()

            // 验证签名
            if (!verifySignature(signature, timestamp, body)) {
                logger.warn("Invalid signature from QQ webhook")
                call.respond(HttpStatusCode.Unauthorized, "Invalid signature")
                return
            }

            // 解析 payload
            val payload = parsePayload(body, eventType)

            // 处理 payload
            messageHandler(payload)

            // 返回成功响应
            call.respond(HttpStatusCode.OK, buildJsonObject {
                put("code", 0)
                put("message", "success")
            }.toString())

        } catch (e: Exception) {
            logger.error("Failed to handle webhook", e)
            call.respond(HttpStatusCode.InternalServerError, buildJsonObject {
                put("code", 500)
                put("message", e.message ?: "Internal error")
            }.toString())
        }
    }

    /**
     * 处理验证请求 (GET /webhook/qq)
     */
    suspend fun handleVerification(call: ApplicationCall) {
        // 返回验证信息
        call.respond(HttpStatusCode.OK, buildJsonObject {
            put("app_id", appId)
            put("status", "active")
            put("version", "v2")
        }.toString())
    }

    /**
     * 验证签名
     * QQ Bot 使用 HMAC-SHA256 签名
     */
    private fun verifySignature(signature: String?, timestamp: String?, body: String): Boolean {
        if (signature == null || timestamp == null) {
            return false
        }

        try {
            // 构造签名字符串: timestamp + body
            val signString = timestamp + body

            // 使用 appSecret 计算 HMAC-SHA256
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            val secretKey = javax.crypto.spec.SecretKeySpec(appSecret.toByteArray(), "HmacSHA256")
            mac.init(secretKey)
            val calculatedSignature = mac.doFinal(signString.toByteArray())

            // Base64 编码
            val calculatedSignatureBase64 = Base64.getEncoder().encodeToString(calculatedSignature)

            // 对比签名
            return calculatedSignatureBase64 == signature
        } catch (e: Exception) {
            logger.error("Failed to verify signature", e)
            return false
        }
    }

    /**
     * 解析 payload
     */
    private fun parsePayload(body: String, eventType: String?): QQWebhookPayload {
        val jsonObject = json.parseToJsonElement(body).jsonObject

        return QQWebhookPayload(
            eventType = eventType ?: jsonObject["t"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN",
            eventId = jsonObject["id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString(),
            timestamp = jsonObject["timestamp"]?.jsonPrimitive?.contentOrNull ?: System.currentTimeMillis().toString(),
            data = jsonObject["d"]?.jsonObject,
            rawBody = body
        )
    }
}

/**
 * Webhook Payload 数据类
 */
data class QQWebhookPayload(
    val eventType: String,
    val eventId: String,
    val timestamp: String,
    val data: JsonObject?,
    val rawBody: String
) {
    /**
     * 是否为消息事件
     */
    val isMessageEvent: Boolean
        get() = eventType in listOf(
            "MESSAGE_CREATE",
            "AT_MESSAGE_CREATE",
            "DIRECT_MESSAGE_CREATE",
            "C2C_MESSAGE_CREATE",
            "GROUP_AT_MESSAGE_CREATE"
        )

    /**
     * 是否为频道消息
     */
    val isGuildMessage: Boolean
        get() = eventType in listOf("MESSAGE_CREATE", "AT_MESSAGE_CREATE")

    /**
     * 是否为私聊消息
     */
    val isDirectMessage: Boolean
        get() = eventType == "DIRECT_MESSAGE_CREATE"

    /**
     * 是否为群消息
     */
    val isGroupMessage: Boolean
        get() = eventType in listOf("C2C_MESSAGE_CREATE", "GROUP_AT_MESSAGE_CREATE")

    /**
     * 获取消息内容
     */
    fun getMessageContent(): String? {
        return data?.get("content")?.jsonPrimitive?.contentOrNull
    }

    /**
     * 获取发送者 ID
     */
    fun getAuthorId(): String? {
        return data?.get("author")?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
    }

    /**
     * 获取频道/群 ID
     */
    fun getChannelId(): String? {
        return data?.get("channel_id")?.jsonPrimitive?.contentOrNull
            ?: data?.get("group_openid")?.jsonPrimitive?.contentOrNull
    }

    /**
     * 获取消息 ID
     */
    fun getMessageId(): String? {
        return data?.get("id")?.jsonPrimitive?.contentOrNull
    }
}
