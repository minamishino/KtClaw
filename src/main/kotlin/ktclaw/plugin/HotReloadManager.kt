package ktclaw.plugin

import ktclaw.spi.Plugin
import java.io.File
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 热加载管理器
 * 监听插件文件变化并自动重载
 */
class HotReloadManager(
    private val pluginLoader: PluginLoader,
    private val pluginManager: PluginManager
) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchKeys = ConcurrentHashMap<WatchKey, Path>()
    private val executor = Executors.newSingleThreadExecutor()
    private val fileChecksums = ConcurrentHashMap<String, String>()
    private val debounceMap = ConcurrentHashMap<String, Long>()

    /**
     * 是否启用热加载
     */
    var enabled: Boolean = true

    /**
     * 防抖时间（毫秒）
     */
    var debounceMs: Long = 1000

    /**
     * 文件变化监听器
     */
    private val listeners = mutableListOf<FileChangeListener>()

    /**
     * 正在监听的目录
     */
    private val watchedDirectories = mutableSetOf<Path>()

    /**
     * 文件变化监听器接口
     */
    fun interface FileChangeListener {
        fun onFileChanged(event: FileChangeEvent)
    }

    /**
     * 文件变化事件
     */
    data class FileChangeEvent(
        val type: ChangeType,
        val file: File,
        val pluginId: String? = null
    )

    /**
     * 变化类型
     */
    enum class ChangeType {
        CREATED,
        MODIFIED,
        DELETED
    }

    init {
        startWatching()
    }

    /**
     * 开始监听目录
     * @param directory 要监听的目录
     */
    fun watchDirectory(directory: File) {
        if (!directory.exists() || !directory.isDirectory) {
            directory.mkdirs()
        }

        val path = directory.toPath()
        if (path in watchedDirectories) {
            return
        }

        val watchKey = path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        )

        watchKeys[watchKey] = path
        watchedDirectories.add(path)

        // 初始化现有文件的校验和
        directory.listFiles { file -> file.extension == "jar" }?.forEach { file ->
            fileChecksums[file.absolutePath] = calculateChecksum(file)
        }
    }

    /**
     * 停止监听目录
     * @param directory 要停止监听的目录
     */
    fun unwatchDirectory(directory: File) {
        val path = directory.toPath()
        watchKeys.entries.removeIf { (key, watchedPath) ->
            if (watchedPath == path) {
                key.cancel()
                true
            } else {
                false
            }
        }
        watchedDirectories.remove(path)
    }

    /**
     * 添加文件变化监听器
     * @param listener 监听器
     */
    fun addListener(listener: FileChangeListener) {
        listeners.add(listener)
    }

    /**
     * 移除文件变化监听器
     * @param listener 监听器
     */
    fun removeListener(listener: FileChangeListener) {
        listeners.remove(listener)
    }

    /**
     * 开始监听线程
     */
    private fun startWatching() {
        executor.submit {
            while (enabled && !Thread.currentThread().isInterrupted) {
                try {
                    val key = watchService.poll(100, TimeUnit.MILLISECONDS)
                    if (key != null) {
                        processWatchKey(key)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    println("Error watching files: ${e.message}")
                }
            }
        }
    }

    /**
     * 处理 WatchKey 事件
     * @param key WatchKey
     */
    private fun processWatchKey(key: WatchKey) {
        val dir = watchKeys[key] ?: return

        for (event in key.pollEvents()) {
            val kind = event.kind()
            val fileName = event.context() as Path
            val file = dir.resolve(fileName).toFile()

            // 只处理 JAR 文件
            if (file.extension != "jar") {
                continue
            }

            // 防抖处理
            val now = System.currentTimeMillis()
            val lastTime = debounceMap[file.absolutePath] ?: 0
            if (now - lastTime < debounceMs) {
                continue
            }
            debounceMap[file.absolutePath] = now

            when (kind) {
                StandardWatchEventKinds.ENTRY_CREATE -> {
                    handleFileCreated(file)
                }
                StandardWatchEventKinds.ENTRY_MODIFY -> {
                    handleFileModified(file)
                }
                StandardWatchEventKinds.ENTRY_DELETE -> {
                    handleFileDeleted(file)
                }
            }
        }

        key.reset()
    }

    /**
     * 处理文件创建事件
     * @param file 创建的文件
     */
    private fun handleFileCreated(file: File) {
        println("Plugin JAR created: ${file.name}")

        // 等待文件写入完成
        Thread.sleep(500)

        try {
            val plugins = pluginLoader.loadFromJar(file)
            plugins.forEach { plugin ->
                pluginManager.registerPlugin(plugin)
                pluginManager.startPlugin(plugin.id)
            }

            fileChecksums[file.absolutePath] = calculateChecksum(file)

            notifyListeners(FileChangeEvent(ChangeType.CREATED, file, plugins.firstOrNull()?.id))
        } catch (e: Exception) {
            println("Failed to load new plugin: ${e.message}")
        }
    }

    /**
     * 处理文件修改事件
     * @param file 修改的文件
     */
    private fun handleFileModified(file: File) {
        println("Plugin JAR modified: ${file.name}")

        // 检查校验和是否真正改变
        val newChecksum = calculateChecksum(file)
        val oldChecksum = fileChecksums[file.absolutePath]

        if (newChecksum == oldChecksum) {
            return
        }

        try {
            // 找到相关的插件
            val pluginInfo = pluginLoader.getAllPluginClasses().entries.find { (_, clazz) ->
                val jarInfo = pluginLoader.getPluginJarInfo(
                    clazz.getConstructor().newInstance().id
                )
                jarInfo?.filePath == file.absolutePath
            }

            val pluginId = pluginInfo?.key

            // 停止并卸载旧插件
            if (pluginId != null) {
                pluginManager.stopPlugin(pluginId)
                pluginManager.unregisterPlugin(pluginId)
                pluginLoader.unloadPlugin(pluginId)
            }

            // 等待文件写入完成
            Thread.sleep(500)

            // 重新加载
            val plugins = pluginLoader.loadFromJar(file)
            plugins.forEach { plugin ->
                pluginManager.registerPlugin(plugin)
                pluginManager.startPlugin(plugin.id)
            }

            fileChecksums[file.absolutePath] = newChecksum

            notifyListeners(FileChangeEvent(ChangeType.MODIFIED, file, pluginId))
        } catch (e: Exception) {
            println("Failed to reload plugin: ${e.message}")
        }
    }

    /**
     * 处理文件删除事件
     * @param file 删除的文件
     */
    private fun handleFileDeleted(file: File) {
        println("Plugin JAR deleted: ${file.name}")

        // 找到相关的插件并卸载
        val pluginsToRemove = pluginManager.getAllPlugins().filter { plugin ->
            val jarInfo = pluginLoader.getPluginJarInfo(plugin.id)
            jarInfo?.filePath == file.absolutePath
        }

        pluginsToRemove.forEach { plugin ->
            pluginManager.stopPlugin(plugin.id)
            pluginManager.unregisterPlugin(plugin.id)
            pluginLoader.unloadPlugin(plugin.id)

            notifyListeners(FileChangeEvent(ChangeType.DELETED, file, plugin.id))
        }

        fileChecksums.remove(file.absolutePath)
    }

    /**
     * 通知所有监听器
     * @param event 文件变化事件
     */
    private fun notifyListeners(event: FileChangeEvent) {
        listeners.forEach { listener ->
            try {
                listener.onFileChanged(event)
            } catch (e: Exception) {
                println("Listener error: ${e.message}")
            }
        }
    }

    /**
     * 计算文件校验和
     * @param file 文件
     * @return MD5 校验和
     */
    private fun calculateChecksum(file: File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            file.lastModified().toString()
        }
    }

    /**
     * 手动触发插件重载
     * @param pluginId 插件 ID
     * @return 是否成功
     */
    fun reloadPlugin(pluginId: String): Boolean {
        val plugin = pluginManager.getPlugin(pluginId) ?: return false
        val jarInfo = pluginLoader.getPluginJarInfo(pluginId) ?: return false

        val file = File(jarInfo.filePath)
        if (!file.exists()) {
            return false
        }

        try {
            pluginManager.stopPlugin(pluginId)
            pluginManager.unregisterPlugin(pluginId)
            pluginLoader.unloadPlugin(pluginId)

            Thread.sleep(100)

            val plugins = pluginLoader.loadFromJar(file)
            plugins.forEach { newPlugin ->
                pluginManager.registerPlugin(newPlugin)
                pluginManager.startPlugin(newPlugin.id)
            }

            fileChecksums[file.absolutePath] = calculateChecksum(file)

            return true
        } catch (e: Exception) {
            println("Manual reload failed: ${e.message}")
            return false
        }
    }

    /**
     * 获取已加载的文件校验和
     * @return 校验和映射
     */
    fun getFileChecksums(): Map<String, String> {
        return fileChecksums.toMap()
    }

    /**
     * 停止热加载管理器
     */
    fun shutdown() {
        enabled = false
        executor.shutdown()
        watchKeys.keys.forEach { it.cancel() }
        watchKeys.clear()
        watchService.close()
    }
}