package ktclaw.channel.qq

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

/**
 * QQ 消息发送器
 * 支持发送文本、图片、语音、文件等多种消息类型
 */
class QQMessageSender(
    private val client: QQBotClient,
    private val httpClient: HttpClient,
    private val json: Json
) {
    private val logger = LoggerFactory.getLogger(QQMessageSender::class.java)
    private val apiBaseUrl = "https://api.sgroup.qq.com"

    /**
     * 发送文本消息
     * @param channelId 频道 ID
     * @param content 消息内容
     * @param msgId 回复的消息 ID (可选)
     * @return 发送结果
     */
    suspend fun sendTextMessage(
        channelId: String,
        content: String,
        msgId: String? = null
    ): Result<QQMessageResponse> {
        return try {
            val requestBody = buildJsonObject {
                put("content", content)
                msgId?.let { put("msg_id", it) }
            }

            val response = httpClient.post("$apiBaseUrl/channels/$channelId/messages") {
                header(HttpHeaders.Authorization, "Bot ${client.appId}.${client.token}")
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            val responseBody = response.bodyAsText()
            val messageResponse = json.decodeFromString<QQMessageResponse>(responseBody)

            logger.info("Text message sent to channel $channelId: $content")
            Result.success(messageResponse)
        } catch (e: Exception) {
            logger.error("Failed to send text message to channel $channelId", e)
            Result.failure(e)
        }
    }

    /**
     * 发送私聊文本消息
     * @param openId 用户 OpenID
     * @param content 消息内容
     * @param msgId 回复的消息 ID (可选)
     */
    suspend fun sendDirectMessage(
        openId: String,
        content: String,
        msgId: String? = null
    ): Result<QQMessageResponse> {
        return try {
            val requestBody = buildJsonObject {
                put("content", content)
                msgId?.let { put("msg_id", it) }
            }

            val response = httpClient.post("$apiBaseUrl/v2/users/$openId/messages") {
                header(HttpHeaders.Authorization, "Bot ${client.appId}.${client.token}")
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            val responseBody = response.bodyAsText()
            val messageResponse = json.decodeFromString<QQMessageResponse>(responseBody)

            logger.info("Direct message sent to user $openId: $content")
            Result.success(messageResponse)
        } catch (e: Exception) {
            logger.error("Failed to send direct message to user $openId", e)
            Result.failure(e)
        }
    }

    /**
     * 发送群消息
     * @param groupOpenId 群 OpenID
     * @param content 消息内容
     * @param msgType 消息类型
     * @param msgId 回复的消息 ID (可选)
     */
    suspend fun sendGroupMessage(
        groupOpenId: String,
        content: String,
        msgType: Int = 0,
        msgId: String? = null
    ): Result<QQMessageResponse> {
        return try {
            val requestBody = buildJsonObject {
                put("content", content)
                put("msg_type", msgType)
                msgId?.let { put("msg_id", it) }
            }

            val response = httpClient.post("$apiBaseUrl/v2/groups/$groupOpenId/messages") {
                header(HttpHeaders.Authorization, "Bot ${client.appId}.${client.token}")
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            val responseBody = response.bodyAsText()
            val messageResponse = json.decodeFromString<QQMessageResponse>(responseBody)

            logger.info("Group message sent to $groupOpenId: $content")
            Result.success(messageResponse)
        } catch (e: Exception) {
            logger.error("Failed to send group message to $groupOpenId", e)
            Result.failure(e)
        }
    }

    /**
     * 发送富媒体消息（图片/语音/文件）
     * @param channelId 频道 ID
     * @param fileUrl 文件 URL
     * @param msgType 消息类型 (2=图片, 3=语音, 4=文件)
     * @param msgId 回复的消息 ID (可选)
     */
    suspend fun sendMediaMessage(
        channelId: String,
        fileUrl: String,
        msgType: Int = 2,
        msgId: String? = null
    ): Result<QQMessageResponse> {
        return try {
            val requestBody = buildJsonObject {
                put("msg_type", msgType)
                putJsonObject("content") {
                    put("url", fileUrl)
                }
                msgId?.let { put("msg_id", it) }
            }

            val response = httpClient.post("$apiBaseUrl/channels/$channelId/messages") {
                header(HttpHeaders.Authorization, "Bot ${client.appId}.${client.token}")
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            val responseBody = response.bodyAsText()
            val messageResponse = json.decodeFromString<QQMessageResponse>(responseBody)

            logger.info("Media message sent to channel $channelId: type=$msgType")
            Result.success(messageResponse)
        } catch (e: Exception) {
            logger.error("Failed to send media message to channel $channelId", e)
            Result.failure(e)
        }
    }

    /**
     * 发送 Markdown 消息
     * @param channelId 频道 ID
     * @param markdownContent Markdown 内容
     * @param keyboard 自定义键盘 (可选)
     */
    suspend fun sendMarkdownMessage(
        channelId: String,
        markdownContent: String,
        keyboard: QQKeyboard? = null
    ): Result<QQMessageResponse> {
        return try {
            val requestBody = buildJsonObject {
                put("msg_type", 2) // Markdown 消息
                putJsonObject("markdown") {
                    put("content", markdownContent)
                }
                keyboard?.let {
                    putJsonObject("keyboard") {
                        putJsonArray("content") {
                            it.rows.forEach { row ->
                                addJsonObject {
                                    putJsonArray("buttons") {
                                        row.buttons.forEach { button ->
                                            addJsonObject {
                                                put("id", button.id)
                                                put("render_data", buildJsonObject {
                                                    put("label", button.label)
                                                    put("visited_label", button.visitedLabel)
                                                    put("style", button.style)
                                                })
                                                put("action", buildJsonObject {
                                                    put("type", button.action.type)
                                                    put("permission", buildJsonObject {
                                                        put("type", button.action.permission.type)
                                                    })
                                                    put("data", button.action.data)
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                        }