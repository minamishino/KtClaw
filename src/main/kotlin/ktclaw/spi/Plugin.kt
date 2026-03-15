package ktclaw.spi

/**
 * SPI 基础插件接口
 * 所有插件必须实现此接口
 */
interface Plugin {
    /**
     * 插件唯一标识符
     */
    val id: String

    /**
     * 插件名称
     */
    val name: String

    /**
     * 插件版本
     */
    val version: String

    /**
     * 插件描述
     */
    val description: String

    /**
     * 插件作者
     */
    val author: String

    /**
     * 插件依赖列表 (依赖的插件ID)
     */
    val dependencies: List<String>
        get() = emptyList()

    /**
     * 插件初始化
     * 在插件加载完成后调用
     */
    fun onLoad()

    /**
     * 插件启动
     * 在所有依赖插件启动后调用
     */
    fun onStart()

    /**
     * 插件停止
     * 在插件卸载前调用
     */
    fun onStop()

    /**
     * 插件卸载
     * 清理资源
     */
    fun onUnload()

    /**
     * 获取插件状态
     */
    fun getStatus(): PluginStatus
}

/**
 * 插件状态枚举
 */
enum class PluginStatus {
    LOADED,      // 已加载
    STARTED,     // 已启动
    STOPPED,     // 已停止
    ERROR,       // 错误状态
    UNLOADED     // 已卸载
}

/**
 * 插件元数据注解
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PluginMeta(
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: String = "",
    val dependencies: Array<String> = []
)
