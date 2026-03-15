package ktclaw.plugin

import ktclaw.spi.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 插件管理器
 * 管理插件的生命周期和依赖关系
 */
class PluginManager {
    /**
     * 已注册的插件映射
     */
    private val plugins = ConcurrentHashMap<String, Plugin>()

    /**
     * 插件状态映射
     */
    private val pluginStates = ConcurrentHashMap<String, PluginStatus>()

    /**
     * 依赖关系图
     * key: 插件ID, value: 依赖该插件的其他插件ID列表
     */
    private val dependencyGraph = ConcurrentHashMap<String, MutableList<String>>()

    /**
     * 生命周期监听器
     */
    private val lifecycleListeners = CopyOnWriteArrayList<LifecycleListener>()

    /**
     * 通道插件缓存
     */
    private val channelPlugins = ConcurrentHashMap<String, ChannelPlugin>()

    /**
     * 模型提供器缓存
     */
    private val modelProviders = ConcurrentHashMap<String, ModelProvider>()

    /**
     * 生命周期监听器接口
     */
    interface LifecycleListener {
        fun onPluginLoaded(plugin: Plugin)
        fun onPluginStarted(plugin: Plugin)
        fun onPluginStopped(plugin: Plugin)
        fun onPluginUnloaded(plugin: Plugin)
        fun onPluginError(plugin: Plugin, error: Throwable)
    }

    /**
     * 注册插件
     * @param plugin 插件实例
     * @return 是否成功
     */
    fun registerPlugin(plugin: Plugin): Boolean {
        val pluginId = plugin.id

        if (plugins.containsKey(pluginId)) {
            println("Plugin already registered: $pluginId")
            return false
        }

        // 检查依赖是否满足
        val missingDeps = checkDependencies(plugin)
        if (missingDeps.isNotEmpty()) {
            println("Plugin $pluginId has missing dependencies: ${missingDeps.joinToString()}")
            return false
        }

        try {
            // 初始化插件
            plugin.onLoad()
            plugins[pluginId] = plugin
            pluginStates[pluginId] = PluginStatus.LOADED

            // 更新依赖图
            plugin.dependencies.forEach { depId ->
                dependencyGraph.computeIfAbsent(depId) { mutableListOf() }.add(pluginId)
            }

            // 缓存特定类型插件
            when (plugin) {
                is ChannelPlugin -> channelPlugins[pluginId] = plugin
                is ModelProvider -> modelProviders[pluginId] = plugin
            }

            notifyLoaded(plugin)
            return true
        } catch (e: Exception) {
            pluginStates[pluginId] = PluginStatus.ERROR
            notifyError(plugin, e)
            println("Failed to load plugin $pluginId: ${e.message}")
            return false
        }
    }

    /**
     * 启动插件
     * @param pluginId 插件 ID
     * @return 是否成功
     */
    fun startPlugin(pluginId: String): Boolean {
        val plugin = plugins[pluginId] ?: return false
        val currentState = pluginStates[pluginId]

        if (currentState != PluginStatus.LOADED && currentState != PluginStatus.STOPPED) {
            return false
        }

        // 先启动所有依赖
        val deps = plugin.dependencies
        for (depId in deps) {
            val depState = pluginStates[depId]
            if (depState == null || depState == PluginStatus.ERROR || depState == PluginStatus.UNLOADED) {
                println("Cannot start $pluginId: dependency $depId not available")
                return false
            }
            if (depState != PluginStatus.STARTED) {
                if (!startPlugin(depId)) {
                    return false
                }
            }
        }

        try {
            plugin.onStart()
            pluginStates[pluginId] = PluginStatus.STARTED
            notifyStarted(plugin)
            return true
        } catch (e: Exception) {
            pluginStates[pluginId] = PluginStatus.ERROR
            notifyError(plugin, e)
            println("Failed to start plugin $pluginId: ${e.message}")
            return false
        }
    }

    /**
     * 停止插件
     * @param pluginId 插件 ID
     * @param force 是否强制停止（不检查依赖）
     * @return 是否成功
     */
    fun stopPlugin(pluginId: String, force: Boolean = false): Boolean {
        val plugin = plugins[pluginId] ?: return false

        if (pluginStates[pluginId] != PluginStatus.STARTED) {
            return false
        }

        // 检查是否有其他插件依赖于此插件
        if (!force) {
            val dependents = dependencyGraph[pluginId] ?: emptyList()
            val activeDependents = dependents.filter { pluginStates[it] == PluginStatus.STARTED }
            if (activeDependents.isNotEmpty()) {
                println("Cannot stop $pluginId: active dependents ${activeDependents.joinToString()}")
                return false
            }
        }

        try {
            plugin.onStop()
            pluginStates[pluginId] = PluginStatus.STOPPED
            notifyStopped(plugin)
            return true
        } catch (e: Exception) {
            pluginStates[pluginId] = PluginStatus.ERROR
            notifyError(plugin, e)
            println("Failed to stop plugin $pluginId: ${e.message}")
            return false
        }
    }

    /**
     * 注销插件
     * @param pluginId 插件 ID
     * @param force 是否强制注销
     * @return 是否成功
     */
    fun unregisterPlugin(pluginId: String, force: Boolean = false): Boolean {
        val plugin = plugins[pluginId] ?: return false

        // 先停止插件
        if (pluginStates[pluginId] == PluginStatus.STARTED) {
            if (!stopPlugin(pluginId, force)) {
                if (!force) {
                    return false
                }
            }
        }

        // 检查依赖
        if (!force) {
            val dependents = dependencyGraph[pluginId] ?: emptyList()
            val loadedDependents = dependents.filter {
                pluginStates[it] != null && pluginStates[it] != PluginStatus.UNLOADED
            }
            if (loadedDependents.isNotEmpty()) {
                println("Cannot unload $pluginId: loaded dependents ${loadedDependents.joinToString()}")
                return false
            }
        }

        try {
            plugin.onUnload()
            plugins.remove(pluginId)
            pluginStates[pluginId] = PluginStatus.UNLOADED

            // 从依赖图中移除
            dependencyGraph.remove(pluginId)
            plugin.dependencies.forEach { depId ->
                dependencyGraph[depId]?.remove(pluginId)
            }

            // 从缓存中移除
            channelPlugins.remove(pluginId)
            modelProviders.remove(pluginId)

            notifyUnloaded(plugin)
            return true
        } catch (e: Exception) {
            notifyError(plugin, e)
            println("Failed to unload plugin $pluginId: ${e.message}")
            return false
        }
    }

    /**
     * 启动所有插件
     */
    fun startAllPlugins() {
        // 按依赖顺序启动
        val sortedPlugins = topologicalSort()
        sortedPlugins.forEach { pluginId ->
            if (pluginStates[pluginId] == PluginStatus.LOADED) {
                startPlugin(pluginId)
            }
        }
    }

    /**
     * 停止所有插件
     */
    fun stopAllPlugins() {
        // 按依赖逆序停止
        val sortedPlugins = topologicalSort().reversed()
        sortedPlugins.forEach { pluginId ->
            if (pluginStates[pluginId] == PluginStatus.STARTED) {
                stopPlugin(pluginId, true)
            }
        }
    }

    /**
     * 获取插件
     * @param pluginId 插件 ID
     * @return 插件实例
     */
    fun getPlugin(pluginId: String): Plugin? {
        return plugins[pluginId]
    }

    /**
     * 获取所有插件
     * @return 插件列表
     */
    fun getAllPlugins(): List<Plugin> {
        return plugins.values.toList()
    }

    /**
     * 获取插件状态
     * @param pluginId 插件 ID
     * @return 插件状态
     */
    fun getPluginStatus(pluginId: String): PluginStatus? {
        return pluginStates[pluginId