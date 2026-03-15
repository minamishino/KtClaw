package ktclaw.channel.qq

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.json.*
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import java.security.Security
import java.util.*

/**
 * QQ Bot Webhook 处理器
 * 处理 QQ 服务器推送的事件和消息
 *
 * 文档: https://bot.q.qq.com/wiki/develop/api-v2/
 * 签名算法: Ed25519
 */
class QQWebhookHandler(
    private val appId: String,
    private val botSecret: String,
    private val messageHandler: suspend (QQWebhookPayload) -> Unit
) {
    private val logger = LoggerFactory.getLogger(QQWebhookHandler::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Ed25519 公钥 (从 botSecret 生成)
    private val publicKey: Ed25519PublicKeyParameters by lazy {
        generatePublicKeyFromSecret(botSecret)
    }

    init {
        // 注册 BouncyCastle Provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * 处理 Webhook 请求
     */
    suspend fun handleWebhook(call: ApplicationCall) {
        try {
            // 获取请求头
            val signature = call.request.header("X-Signature-Ed25519")
            val timestamp = call.request.header("X-Signature-Timestamp")

            // 读取请求体
            val body = call.receiveText()

            // 验证签名
            if (!verifyEd25519Signature(signature, timestamp, body)) {
                logger.warn("Invalid Ed25519 signature from QQ webhook")
                call.respond(HttpStatusCode.Unauthorized, "Invalid signature")
                return
            }

            // 解析 payload
            val payload = parsePayload(body)

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
     * 从 Bot Secret 生成 Ed25519 公钥
     *
     * 签名流程:
     * 1. Bot Secret repeat 得到 32 字节 seed
     * 2. seed 生成 Ed25519 公钥
     */
    private fun generatePublicKeyFromSecret(secret: String): Ed25519PublicKeyParameters {
        // 步骤1: 将 secret 重复直到达到 32 字节 (Ed25519 seed 大小)
        val seed = generateSeedFromSecret(secret)

        // 步骤2: 使用 seed 生成 Ed25519 密钥对，提取公钥
        // Ed25519 私钥前32字节就是 seed
        // 公钥可以通过私钥派生
        val privateKeyBytes = seed.copyOf(64) // Ed25519 私钥是64字节

        // 使用 BouncyCastle 从 seed 生成密钥对
        // Ed25519 公钥是32字节
        val publicKeyBytes = derivePublicKeyFromSeed(seed)

        return Ed25519PublicKeyParameters(publicKeyBytes, 0)
    }

    /**
     * 从 secret 生成 32 字节 seed
     * 根据文档: 将 secret 重复直到达到 32 字节
     */
    private fun generateSeedFromSecret(secret: String): ByteArray {
        val secretBytes = secret.toByteArray(Charsets.UTF_8)
        val seedSize = 32 // Ed25519 Seed Size

        if (secretBytes.size >= seedSize) {
            return secretBytes.copyOf(seedSize)
        }

        // 重复 secret 直到达到 32 字节
        val seed = ByteArray(seedSize)
        var offset = 0
        while (offset < seedSize) {
            val remaining = seedSize - offset
            val copyLength = minOf(secretBytes.size, remaining)
            secretBytes.copyInto(seed, offset, 0, copyLength)
            offset += copyLength
        }

        return seed
    }

    /**
     * 从 seed 派生 Ed25519 公钥 (32字节)
     * Ed25519 公钥生成算法: 对私钥进行哈希，取结果的一部分计算点乘
     */
    private fun derivePublicKeyFromSeed(seed: ByteArray): ByteArray {
        // 使用 BouncyCastle 的 Ed25519 实现
        // 创建私钥参数 (seed 是私钥的前32字节)
        val privateKeyParams = org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters(seed, 0)
        // 从私钥生成公钥
        return privateKeyParams.generatePublicKey().encoded
    }

    /**
     * 验证 Ed25519 签名
     *
     * 签名流程:
     * 3. hex 解码 X-Signature-Ed25519 得到 signature
     * 4. timestamp + body 组合成 msg
     * 5. 使用公钥验证 signature
     */
    private fun verifyEd25519Signature(
        signatureHex: String?,
        timestamp: String?,
        body: String
    ): Boolean {
        if (signatureHex == null || timestamp == null) {
            logger.warn("Missing signature or timestamp header")
            return false
        }

        return try {
            // 步骤3: hex 解码 X-Signature-Ed25519 得到 signature
            val signature = hexDecode(signatureHex)

            // 验证签名长度 (Ed25519 签名是 64 字节)
            if (signature.size != 64) {
                logger.warn("Invalid signature length: ${signature.size}, expected 64")
                return false
            }

            // 验证签名格式 (最后一个字节的最高3位必须为0)
            if ((signature[63].toInt() and 0xE0) != 0) {
                logger.warn("Invalid signature format")
                return false
            }

            // 步骤4: timestamp + body 组合成 msg
            val message = (timestamp + body).toByteArray(Charsets.UTF_8)

            // 步骤5: 使用公钥验证 signature
            val signer = Ed25519Signer()
            signer.init(false, publicKey) // false = 验证模式
            signer.update(message, 0, message.size)

            signer.verifySignature(signature)
        } catch (e: Exception) {
            logger.error("Failed to verify Ed25519 signature", e)
            false
        }
    }

    /**
     * Hex 解码
     */
    private fun hexDecode(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in hex.indices step 2) {
            val high = hexToNibble(hex[i])
            val low = hexToNibble(hex[i + 1])
            result[i / 2] = ((high shl 4) or low).toByte()
        }
        return result
    }

    private fun hexToNibble(c: Char): Int {
        return when (c) {
            in '0'..'9' -> c - '0'
            in 'a'..'f' -> c - 'a' + 10
            in 'A'..'F' -> c - 'A' + 10
            else -> throw IllegalArgumentException("Invalid hex character: $c")
        }
    }

    /**
     * 解析 payload
     */
    private fun parsePayload(body: String): QQWebhookPayload {
        val jsonObject = json.parseToJsonElement(body).jsonObject

        return QQWebhookPayload(
            eventType = jsonObject["t"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN",
            eventId = jsonObject["id"]?.jsonPrimitive?.contentOrNull
                ?: jsonObject["event_id"]?.jsonPrimitive?.contentOrNull
                ?: UUID.randomUUID().toString(),
            timestamp = jsonObject["timestamp"]?.jsonPrimitive?.contentOrNull
                ?: System.currentTimeMillis().toString(),
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
