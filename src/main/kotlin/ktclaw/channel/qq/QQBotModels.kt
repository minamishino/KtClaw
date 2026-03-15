package ktclaw.channel.qq

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * QQ Bot API 数据模型
 * 参考文档: https://bot.q.qq.com/wiki/develop/api-v2/
 */

/**
 * WebSocket 通用消息结构
 */
@Serializable
data class QQWebSocketMessage(
    val op: Int,                    // 操作码
    @SerialName("s")
    val sequence: Long? = null,     // 序列号
    val t: String? = null,          // 事件类型
    val d: JsonElement? = null,     // 事件数据
    val id: String? = null          // 消息ID
)

/**
 * WebSocket 操作码
 */
object QQOpCode {
    const val DISPATCH = 0          // 服务端推送消息
    const val HEARTBEAT = 1         // 客户端发送心跳
    const val IDENTIFY = 2          // 客户端发送鉴权
    const val PRESENCE_UPDATE = 3   // 客户端发送状态更新
    const val VOICE_STATE_UPDATE = 4 // 客户端发送语音状态更新
    const val RESUME = 6            // 客户端恢复连接
    const val RECONNECT = 7         // 服务端通知客户端重连
    const val INVALID_SESSION = 9   // 无效的 session
    const val HELLO = 10            // 服务端发送心跳参数
    const val HEARTBEAT_ACK = 11    // 服务端确认心跳
    const val HTTP_CALLBACK_ACK = 12 // HTTP 回调鉴权
}

/**
 * WebSocket 就绪事件数据
 */
@Serializable
data class QQReadyEvent(
    val version: Int,
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("user")
    val botUser: QQUser,
    @SerialName("shard")
    val shard: List<Int>? = null
)

/**
 * QQ 用户对象
 */
@Serializable
data class QQUser(
    val id: String,
    val username: String? = null,
    val avatar: String? = null,
    @SerialName("bot")
    val isBot: Boolean? = null,
    @SerialName("union_openid")
    val unionOpenid: String? = null,
    @SerialName("union_user_account")
    val unionUserAccount: String? = null
)

/**
 * QQ 消息对象
 */
@Serializable
data class QQMessage(
    val id: String,
    @SerialName("channel_id")
    val channelId: String? = null,
    @SerialName("guild_id")
    val guildId: String? = null,
    @SerialName("group_id")
    val groupId: String? = null,
    val content: String? = null,
    val timestamp: String? = null,
    @SerialName("edited_timestamp")
    val editedTimestamp: String? = null,
    @SerialName("mention_everyone")
    val mentionEveryone: Boolean? = null,
    val author: QQUser? = null,
    val mentions: List<QQUser>? = null,
    @SerialName("member")
    val guildMember: QQGuildMember? = null,
    val attachments: List<QQAttachment>? = null,
    val embeds: List<QQEmbed>? = null,
    @SerialName("message_reference")
    val messageReference: QQMessageReference? = null,
    @SerialName("seq_in_channel")
    val seqInChannel: Long? = null,
    val type: Int? = null
)

/**
 * 消息类型
 */
object QQMessageType {
    const val TEXT = 0              // 普通文本消息
    const val RICH_MEDIA = 1        // 富媒体消息
    const val MARKDOWN = 2          // Markdown 消息
    const val ARK = 3               // Ark 消息
    const val EMBED = 4             // Embed 消息
    const val AT = 5                // @消息
    const val REPLY = 6             // 回复消息
    const val FILE = 7              // 文件消息
}

/**
 * 消息引用（回复）
 */
@Serializable
data class QQMessageReference(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("channel_id")
    val channelId: String? = null,
    @SerialName("guild_id")
    val guildId: String? = null
)

/**
 * 频道成员信息
 */
@Serializable
data class QQGuildMember(
    val user: QQUser? = null,
    val nick: String? = null,
    val roles: List<String>? = null,
    @SerialName("joined_at")
    val joinedAt: String? = null,
    @SerialName("deaf")
    val isDeaf: Boolean? = null,
    @SerialName("mute")
    val isMute: Boolean? = null
)

/**
 * 附件信息
 */
@Serializable
data class QQAttachment(
    val id: String? = null,
    val url: String? = null,
    @SerialName("proxy_url")
    val proxyUrl: String? = null,
    val filename: String? = null,
    @SerialName("content_type")
    val contentType: String? = null,
    val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("content")
    val content: String? = null
)

/**
 * Embed 消息
 */
@Serializable
data class QQEmbed(
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val timestamp: String? = null,
    val color: Int? = null,
    val image: QQEmbedImage? = null,
    val thumbnail: QQEmbedImage? = null,
    val video: QQEmbedVideo? = null,
    val author: QQEmbedAuthor? = null,
    val footer: QQEmbedFooter? = null,
    val fields: List<QQEmbedField>? = null
)

@Serializable
data class QQEmbedImage(
    val url: String? = null,
    @SerialName("proxy_url")
    val proxyUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class QQEmbedVideo(
    val url: String? = null,
    @SerialName("proxy_url")
    val proxyUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class QQEmbedAuthor(
    val name: String? = null,
    val url: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("proxy_icon_url")
    val proxyIconUrl: String? = null
)

@Serializable
data class QQEmbedFooter(
    val text: String? = null,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("proxy_icon_url")
    val proxyIconUrl: String? = null
)

@Serializable
data class QQEmbedField(
    val name: String? = null,
    val value: String? = null,
    val inline: Boolean? = null
)

/**
 * 消息内容类型（用于发送消息）
 */
sealed class QQMessageContent {
    data class Text(val content: String) : QQMessageContent()
    data class Image(val url: String) : QQMessageContent()
    data class Voice(val url: String) : QQMessageContent()
    data class File(val url: String, val name: String) : QQMessageContent()
    data class Rich(val content: String, val imageUrls: List<String> = emptyList()) : QQMessageContent()
    data class Markdown(val content: String) : QQMessageContent()
    data class Ark(val templateId: String, val kv: List<QQArkKv>) : QQMessageContent()
    data class Embed(val embed: QQEmbed) : QQMessageContent()
}

@Serializable
data class QQArkKv(
    val key: String,
    val value: String
)

/**
 * 发送消息请求体
 */
@Serializable
data class QQSendMessage(
    val content: String? = null,
    @SerialName("msg_type")
    val msgType: Int = 0,
    val embed: QQEmbed? = null,
    val ark: QQArk? = null,
    val image: String? = null,
    @SerialName("msg_id")
    val msgId: String? = null,
    @SerialName("message_reference")
    val messageReference: QQMessageReference? = null,
    val markdown: QQMarkdown? = null,
    val keyboard: QQKeyboard? = null
)

@Serializable
data class QQArk(
    @SerialName("template_id")
    val templateId: String,
    val kv: List<QQArkKv>
)

@Serializable
data class QQMarkdown(
    val content: String? = null,
    @SerialName("custom_template_id")
    val customTemplateId: String? = null,
    val params: List<QQMarkdownParam>? = null
)

@Serializable
data class QQMarkdownParam(
    val key: String,
    val values: List<String>
)

@Serializable
data class QQKeyboard(
    val content: QQKeyboardContent
)

@Serializable
data class QQKeyboardContent(
    val rows: List<QQKeyboardRow>
)

@Serializable
data class QQKeyboardRow(
    val buttons: List<QQButton>
)

@Serializable
data class QQButton(
    val id: String? = null,
    @SerialName("render_data")
    val renderData: QQButtonRenderData,
    val action: QQButtonAction
)

@Serializable
data class QQButtonRenderData(
    val label: String,
    val style: Int
)

@Serializable
data class QQButtonAction(
    val type: Int,
    @SerialName("