package ktclaw.plugin

import ktclaw.spi.Plugin
import ktclaw.spi.PluginMeta
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * 插件加载器
 * 支持从 JAR 文件动态加载插件
 */
class PluginLoader {
    /**
     * 已加载的插件类加载器映射
     */
    private val classLoaders = ConcurrentHashMap<String, URLClassLoader>()

    /**
     * 已加载的插件类映射
     */
    private val pluginClasses = ConcurrentHashMap<String, Class<out Plugin>>()

    /**
     * 插件目录
     */
    var pluginDirectory: File = File("plugins")
        set(value) {
            field = value
            if (!field.exists()) {
                field.mkdirs()
            }
        }

    init {
        if (!pluginDirectory.exists()) {
            pluginDirectory.mkdirs()
        }
    }

    /**
     * 从 JAR 文件加载插件
     * @param jarFile JAR 文件
     * @return 加载的插件实例列表
     */
    fun loadFromJar(jarFile: File): List<Plugin> {
        require(jarFile.exists() && jarFile.extension == "jar") {
            "Invalid JAR file: ${jarFile.absolutePath}"
        }

        val plugins = mutableListOf<Plugin>()
        val jarUrl = jarFile.toURI().toURL()

        // 创建新的 ClassLoader
        val classLoader = URLClassLoader(
            arrayOf(jarUrl),
            this::class.java.classLoader
        )

        classLoaders[jarFile.name] = classLoader

        // 使用 ServiceLoader 发现插件
        val serviceLoader = ServiceLoader.load(Plugin::class.java, classLoader)

        serviceLoader.forEach { plugin ->
            val pluginClass = plugin::class.java
            val pluginId = extractPluginId(pluginClass)

            if (pluginId != null) {
                pluginClasses[pluginId] = pluginClass
                plugins.add(plugin)
            }
        }

        // 如果没有 ServiceLoader 配置，尝试扫描 JAR 中的类
        if (plugins.isEmpty()) {
            plugins.addAll(scanJarForPlugins(jarFile, classLoader))
        }

        return plugins
    }

    /**
     * 从目录加载所有插件
     * @param directory 插件目录
     * @return 加载的插件实例列表
     */
    fun loadAllFromDirectory(directory: File = pluginDirectory): List<Plugin> {
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }

        val allPlugins = mutableListOf<Plugin>()

        directory.listFiles { file -> file.extension == "jar" }?.forEach { jarFile ->
            try {
                val plugins = loadFromJar(jarFile)
                allPlugins.addAll(plugins)
            } catch (e: Exception) {
                println("Failed to load plugin from ${jarFile.name}: ${e.message}")
            }
        }

        return allPlugins
    }

    /**
     * 扫描 JAR 文件查找插件类
     * @param jarFile JAR 文件
     * @param classLoader 类加载器
     * @return 发现的插件实例列表
     */
    private fun scanJarForPlugins(jarFile: File, classLoader: URLClassLoader): List<Plugin> {
        val plugins = mutableListOf<Plugin>()
        val jar = java.util.jar.JarFile(jarFile)

        jar.entries().asSequence()
            .filter { it.name.endsWith(".class") }
            .forEach { entry ->
                try {
                    val className = entry.name
                        .replace('/', '.')
                        .removeSuffix(".class")

                    val clazz = classLoader.loadClass(className)

                    if (Plugin::class.java.isAssignableFrom(clazz) &&
                        !clazz.isInterface &&
                        !java.lang.reflect.Modifier.isAbstract(clazz.modifiers)
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        val pluginClass = clazz as Class<out Plugin>
                        val pluginId = extractPluginId(pluginClass)

                        if (pluginId != null) {
                            val plugin = createPluginInstance(pluginClass)
                            if (plugin != null) {
                                pluginClasses[pluginId] = pluginClass
                                plugins.add(plugin)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 忽略无法加载的类
                }
            }

        jar.close()
        return plugins
    }

    /**
     * 从类路径加载插件（用于内置插件）
     * @return 发现的插件实例列表
     */
    fun loadFromClasspath(): List<Plugin> {
        val plugins = mutableListOf<Plugin>()
        val serviceLoader = ServiceLoader.load(Plugin::class.java)

        serviceLoader.forEach { plugin ->
            val pluginClass = plugin::class.java
            val pluginId = extractPluginId(pluginClass)

            if (pluginId != null) {
                pluginClasses[pluginId] = pluginClass
                plugins.add(plugin)
            }
        }

        return plugins
    }

    /**
     * 提取插件 ID
     * @param pluginClass 插件类
     * @return 插件 ID
     */
    private fun extractPluginId(pluginClass: Class<out Plugin>): String? {
        // 优先从注解获取
        val annotation = pluginClass.getAnnotation(PluginMeta::class.java)
        if (annotation != null) {
            return annotation.id
        }

        // 从 Kotlin 注解获取
        val kotlinAnnotation = pluginClass.kotlin.findAnnotation<PluginMeta>()
        if (kotlinAnnotation != null) {
            return kotlinAnnotation.id
        }

        // 如果已实例化，从实例获取
        return try {
            val instance = createPluginInstance(pluginClass)
            instance?.id
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建插件实例
     * @param pluginClass 插件类
     * @return 插件实例
     */
    private fun createPluginInstance(pluginClass: Class<out Plugin>): Plugin? {
        return try {
            // 尝试使用 Kotlin 主构造函数
            val kClass = pluginClass.kotlin
            val constructor = kClass.primaryConstructor

            if (constructor != null && constructor.parameters.isEmpty()) {
                constructor.call() as Plugin
            } else {
                // 使用无参构造函数
                pluginClass.getDeclaredConstructor().newInstance()
            }
        } catch (e: Exception) {
            println("Failed to create plugin instance: ${e.message}")
            null
        }
    }

    /**
     * 获取已加载的插件类
     * @param pluginId 插件 ID
     * @return 插件类
     */
    fun getPluginClass(pluginId: String): Class<out Plugin>? {
        return pluginClasses[pluginId]
    }

    /**
     * 获取所有已加载的插件类
     * @return 插件类映射
     */
    fun getAllPluginClasses(): Map<String, Class<out Plugin>> {
        return pluginClasses.toMap()
    }

    /**
     * 卸载插件
     * @param pluginId 插件 ID
     */
    fun unloadPlugin(pluginId: String) {
        pluginClasses.remove(pluginId)

        // 注意：URLClassLoader 无法真正关闭，
        // 这里只是移除引用，实际类加载器会在 GC 时释放
    }

    /**
     * 获取插件 JAR 文件信息
     * @param pluginId 插件 ID
     * @return JAR 文件信息
     */
    fun getPluginJarInfo(pluginId: String): JarInfo? {
        val pluginClass = pluginClasses[pluginId] ?: return null

        val classLoader = pluginClass.classLoader as? URLClassLoader ?: return null
        val urls = classLoader.uris

        return urls.firstOrNull()?.let { url ->
            val file = File(url)
            JarInfo(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                lastModified = file.lastModified()
            )
        }
    }

    /**
     * 检查 JAR 文件是否已加载
     * @param jarFile JAR 文件
     * @return 是否已加载
     */
    fun isJarLoaded(jarFile: File): Boolean {
        return classLoaders.containsKey(jarFile.name)
    }

    /**
     * 获取所有已加载的 ClassLoader
     * @return ClassLoader 映射
     */
    fun getClassLoaders(): Map<String, URLClassLoader> {
        return classLoaders.toMap()
    }

    /**
     * 清除所有已加载的插件
     */
    fun clear() {
        pluginClasses.clear()
        classLoaders.clear()
    }
}

/**
 * JAR 文件信息
 */
data class JarInfo(
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val lastModified: Long
)