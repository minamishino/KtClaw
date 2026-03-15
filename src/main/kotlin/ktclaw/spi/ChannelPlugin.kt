package ktclaw.spi

import ktclaw.message.Message
import ktclaw.message.MessageContext

/**
 * 通道插件接口
 * 用于实现消息通道（如 QQ、Discord、Telegram 等）
 */
interface ChannelPlugin : Plugin {
    /**
     * 通道类型标识
     */
    val channelType: String

    /**
     * 是否支持发送消息
     */
    val supportsSend: Boolean
        get() = true

    /**
     * 是否支持接收消息
     */
    val supportsReceive: Boolean
        get() = true

    /**
     * 是否支持富媒体（图片、语音、视频等）
     */
    val supportsMedia: Boolean
        get() = false

    /**
     * 是否支持群组/频道
     */
    val supportsGroup: Boolean
        get() = false

    /**
     * 初始化通道连接
     */
    fun connect()

    /**
     * 断开通道连接
     */
    fun disconnect()

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean

    /**
     * 发送消息
     * @param target 目标（用户ID或群组ID）
     * @param message 消息内容
     * @return 发送结果
     */
    fun sendMessage(target: String, message: Message): SendResult

    /**
     * 发送文本消息（便捷方法）
     * @param target 目标
     * @param text 文本内容
     * @return 发送结果
     */
    fun sendText(target: String, text: String): SendResult

    /**
     * 接收消息回调
     * 当收到消息时调用
     * @param context 消息上下文
     */
    fun onMessageReceived(context: MessageContext)

    /**
     * 设置消息接收处理器
     * @param handler 消息处理器
     */
    fun setMessageHandler(handler: (MessageContext) -> Unit)

    /**
     * 获取通道配置
     * @return 配置项列表
     */
    fun getConfigOptions(): List<ConfigOption>

    /**
     * 更新通道配置
     * @param config 配置映射
     */
    fun updateConfig(config: Map<String, String>)
}

/**
 * 发送结果
 */
data class SendResult(
    val success: Boolean,
    val messageId: String? = null,
    val error: String? = null
)

/**
 * 配置选项
 */
data class ConfigOption(
    val key: String,
    val displayName: String,
    val type: ConfigType,
    val required: Boolean = false,
    val defaultValue: String? = null,
    val description: String = ""
)

/**
 * 配置类型枚举
 */
enum class ConfigType {
    STRING,
    INTEGER,
    BOOLEAN,
    PASSWORD,
    URL,
    FILE_PATH
}

/**
 * 通道插件元数据注解
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ChannelPluginMeta(
    val id: String,
    val name: String,
    val version: String,
    val channelType: String,
    val description: String = "",
    val author: String = "",
    val supportsMedia: Boolean = false,
    val supportsGroup: Boolean = false
)
