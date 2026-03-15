package ktclaw.agent

import ktclaw.db.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

/**
 * Agent Runtime - Agent 生命周期管理与任务调度核心
 * 
 * 职责：
 * 1. 管理 Agent 的生命周期（启动、运行、暂停、停止）
 * 2. 任务队列调度与优先级管理
 * 3. Agent 实例池管理
 * 4. 状态监控与事件通知
 */
class AgentRuntime(
    private val sessionManager: SessionManager,
    private val executor: AgentExecutor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = dispatcher + job

    // Agent 实例池 - 缓存活跃的 Agent 实例
    private val agentInstances = ConcurrentHashMap<UUID, AgentInstance>()

    // 任务队列 - 按优先级排序
    private val taskQueue = Channel<AgentTask>(Channel.UNLIMITED)

    // 运行状态
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // 活跃任务追踪
    private val activeTasks = ConcurrentHashMap<UUID, Job>()

    // 运行时统计
    private val _runtimeStats = MutableStateFlow(RuntimeStats())
    val runtimeStats: StateFlow<RuntimeStats> = _runtimeStats.asStateFlow()

    // 事件流
    private val _events = MutableSharedFlow<AgentEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<AgentEvent> = _events.asSharedFlow()

    init {
        logger.info { "AgentRuntime initialized" }
    }

    // ============================================
    // 1. 生命周期管理
    // ============================================

    /**
     * 启动 Runtime
     */
    fun start() {
        if (_isRunning.value) {
            logger.warn { "AgentRuntime is already running" }
            return
        }

        logger.info { "Starting AgentRuntime..." }
        _isRunning.value = true

        // 启动任务调度器
        launch {
            processTaskQueue()
        }

        // 启动状态监控
        launch {
            monitorHealth()
        }

        _events.tryEmit(AgentEvent.RuntimeStarted(Instant.now()))
        logger.info { "AgentRuntime started successfully" }
    }

    /**
     * 停止 Runtime
     */
    suspend fun stop(graceful: Boolean = true) {
        if (!_isRunning.value) {
            return
        }

        logger.info { "Stopping AgentRuntime (graceful=$graceful)..." }
        _isRunning.value = false

        if (graceful) {
            // 等待活跃任务完成
            activeTasks.values.forEach { it.join() }
        } else {
            // 取消所有活跃任务
            activeTasks.values.forEach { it.cancel() }
        }

        // 清空队列
        taskQueue.cancel()

        // 停止所有 Agent 实例
        agentInstances.values.forEach { it.shutdown() }
        agentInstances.clear()

        // 取消 SupervisorJob
        job.cancel()

        _events.tryEmit(AgentEvent.RuntimeStopped(Instant.now(), graceful))
        logger.info { "AgentRuntime stopped" }
    }

    /**
     * 暂停指定 Agent
     */
    fun pauseAgent(agentId: UUID): Boolean {
        val instance = agentInstances[agentId] ?: return false
        instance.pause()
        _events.tryEmit(AgentEvent.AgentPaused(agentId, Instant.now()))
        logger.info { "Agent $agentId paused" }
        return true
    }

    /**
     * 恢复指定 Agent
     */
    fun resumeAgent(agentId: UUID): Boolean {
        val instance = agentInstances[agentId] ?: return false
        instance.resume()
        _events.tryEmit(AgentEvent.AgentResumed(agentId, Instant.now()))
        logger.info { "Agent $agentId resumed" }
        return true
    }

    // ============================================
    // 2. 任务调度
    // ============================================

    /**
     * 提交任务到队列
     */
    suspend fun submitTask(task: AgentTask): Boolean {
        if (!_isRunning.value) {
            logger.warn { "Cannot submit task: Runtime is not running" }
            return false
        }

        val result = taskQueue.trySend(task).isSuccess
        if (result) {
            _events.tryEmit(AgentEvent.TaskSubmitted(task.id, task.agentId, Instant.now()))
            updateStats { it.copy(queuedTasks = it.queuedTasks + 1) }
        }
        return result
    }

    /**
     * 立即执行任务（不经过队列）
     */
    suspend fun executeImmediate(task: AgentTask): AgentTaskResult {
        return executeTask(task)
    }

    /**
     * 任务队列处理器
     */
    private suspend fun processTaskQueue() {
        while (_isRunning.value) {
            try {
                val task = taskQueue.receive()
                launch {
                    processTask(task)
                }
            } catch (e: CancellationException) {
                logger.debug { "Task queue processing cancelled" }
                break
            } catch (e: Exception) {
                logger.error(e) { "Error processing task queue" }
            }
        }
    }

    /**
     * 处理单个任务
     */
    private suspend fun processTask(task: AgentTask) {
        updateStats {
            it.copy(
                queuedTasks = it.queuedTasks - 1,
                activeTasks = it.activeTasks + 1
            )
        }

        val job = launch {
            try {
                _events.tryEmit(AgentEvent.TaskStarted(task.id, task.agentId, Instant.now()))
                val result = executeTask(task)
                _events.tryEmit(
                    AgentEvent.TaskCompleted(
                        task.id,
                        task.agentId,
                        result,
                        Instant.now()
                    )
                )
            } catch (e: CancellationException) {
                _events.tryEmit(
                    AgentEvent.TaskCancelled(
                        task.id,
                        task.agentId,
                        Instant.now()
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "Task ${task.id} failed" }
                _events.tryEmit(
                    AgentEvent.TaskFailed(
                        task.id,
                        task.agentId,
                        e.message ?: "Unknown error",
                        Instant.now()
                    )
                )
            } finally {
                activeTasks.remove(task.id)
                updateStats { it.copy(activeTasks = it.activeTasks - 1) }
            }
        }

        activeTasks[task.id] = job
    }

    /**
     * 执行任务核心逻辑
     */
    private suspend fun executeTask(task: AgentTask): AgentTaskResult {
        val startTime = System.currentTimeMillis()

        // 获取或创建 Agent 实例
        val agentInstance = getOrCreateAgentInstance(task.agentId)

        // 检查 Agent 状态
        if (agentInstance.state.value == AgentState.PAUSED) {
            return AgentTaskResult(
                taskId = task.id,
                success = false,
                error = "Agent is paused",
                latencyMs = System.currentTimeMillis() - startTime
            )
        }

        // 通过 Executor 执行
        val result = executor.execute(agentInstance.config, task)

        val latencyMs = System.currentTimeMillis() - startTime
        updateStats {
            it.copy(
                totalTasks = it.totalTasks + 1,
                totalLatencyMs = it.totalLatencyMs + latencyMs
            )
        }

        return result.copy(latencyMs = latencyMs)
    }

    // ============================================
    // 3. Agent 实例管理
    // ============================================

    /**
     * 获取或创建 Agent 实例
     */
    private