package ktclaw.spi

/**
 * 模型提供器接口
 * 用于集成各种 AI 模型服务（OpenAI、Claude、Gemini 等）
 */
interface ModelProvider : Plugin {
    /**
     * 提供器名称
     */
    val providerName: String

    /**
     * 支持的模型列表
     */
    val supportedModels: List<ModelInfo>

    /**
     * 默认模型
     */
    val defaultModel: String

    /**
     * 是否支持流式输出
     */
    val supportsStreaming: Boolean
        get() = true

    /**
     * 是否支持函数调用
     */
    val supportsFunctionCalling: Boolean
        get() = false

    /**
     * 是否支持多模态（图文）
     */
    val supportsMultimodal: Boolean
        get() = false

    /**
     * 获取模型信息
     * @param modelId 模型ID
     * @return 模型信息
     */
    fun getModelInfo(modelId: String): ModelInfo?

    /**
     * 发送聊天请求
     * @param request 聊天请求
     * @return 聊天响应
     */
    fun chat(request: ChatRequest): ChatResponse

    /**
     * 发送流式聊天请求
     * @param request 聊天请求
     * @param callback 流式回调
     */
    fun chatStream(request: ChatRequest, callback: (StreamChunk) -> Unit)

    /**
     * 计算 Token 数量
     * @param text 文本
     * @param modelId 模型ID
     * @return Token 数量
     */
    fun countTokens(text: String, modelId: String): Int

    /**
     * 验证 API 密钥
     * @param apiKey API密钥
     * @return 是否有效
     */
    fun validateApiKey(apiKey: String): Boolean

    /**
     * 获取提供器配置选项
     * @return 配置项列表
     */
    fun getProviderConfig(): List<ProviderConfigOption>

    /**
     * 更新提供器配置
     * @param config 配置映射
     */
    fun updateProviderConfig(config: Map<String, String>)
}

/**
 * 模型信息
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val maxTokens: Int,
    val contextWindow: Int,
    val pricing: Pricing? = null,
    val capabilities: ModelCapabilities = ModelCapabilities()
)

/**
 * 定价信息
 */
data class Pricing(
    val inputPricePer1K: Double,   // 每1000个输入token的价格
    val outputPricePer1K: Double   // 每1000个输出token的价格
)

/**
 * 模型能力
 */
data class ModelCapabilities(
    val supportsStreaming: Boolean = true,
    val supportsFunctionCalling: Boolean = false,
    val supportsVision: Boolean = false,
    val supportsJsonMode: Boolean = false,
    val supportsSystemPrompt: Boolean = true
)

/**
 * 聊天请求
 */
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val stream: Boolean = false,
    val functions: List<FunctionDefinition>? = null,
    val functionCall: String? = null,
    val responseFormat: ResponseFormat? = null
)

/**
 * 消息
 */
data class Message(
    val role: MessageRole,
    val content: String,
    val name: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null
)

/**
 * 消息角色
 */
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

/**
 * 工具调用
 */
data class ToolCall(
    val id: String,
    val type: String,
    val function: FunctionCall
)

/**
 * 函数调用
 */
data class FunctionCall(
    val name: String,
    val arguments: String
)

/**
 * 函数定义
 */
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

/**
 * 响应格式
 */
enum class ResponseFormat {
    TEXT,
    JSON_OBJECT
}

/**
 * 聊天响应
 */
data class ChatResponse(
    val id: String,
    val model: String,
    val content: String,
    val usage: Usage,
    val finishReason: FinishReason,
    val toolCalls: List<ToolCall>? = null
)

/**
 * Token 使用统计
 */
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * 完成原因
 */
enum class FinishReason {
    STOP,
    LENGTH,
    CONTENT_FILTER,
    TOOL_CALLS,
    ERROR
}

/**
 * 流式响应块
 */
data class StreamChunk(
    val id: String,
    val model: String,
    val delta: String,
    val finishReason: FinishReason? = null,
    val usage: Usage? = null
)

/**
 * 提供器配置选项
 */
data class ProviderConfigOption(
    val key: String,
    val displayName: String,
    val type: ConfigType,
    val required: Boolean = false,
    val defaultValue: String? = null,
    val description: String = ""
)

/**
 * 模型提供器元数据注解
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModelProviderMeta(
    val id: String,
    val name: String,
    val version: String,
    val providerName: String,
    val description: String = "",
    val author: String = "",
    val supportsStreaming: Boolean = true,
    val supportsFunctionCalling: Boolean = false,
    val supportsMultimodal: Boolean = false
)
