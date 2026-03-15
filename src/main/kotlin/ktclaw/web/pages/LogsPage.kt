package ktclaw.web.pages

import kotlinx.html.*

class LogsPage : BasePage() {
    override fun getPageTitle(): String = "日志查看"

    override fun FlowContent.renderSidebar() {
        aside {
            id = "sidebar"
            classes = setOf("w-64", "bg-white", "border-r", "border-gray-200", "flex", "flex-col", "hidden", "md:flex")
            // Logo
            div {
                classes = setOf("p-6", "border-b", "border-gray-200")
                div {
                    classes = setOf("flex", "items-center", "gap-3")
                    div {
                        classes = setOf("w-10", "h-10", "bg-primary", "rounded-lg", "flex", "items-center", "justify-center", "text-white")
                        i { classes = setOf("fas", "fa-rocket", "text-xl") }
                    }
                    h1 {
                        classes = setOf("text-xl", "font-bold", "text-dark")
                        +"KtClaw"
                    }
                }
                p {
                    classes = setOf("text-sm", "text-gray-500", "mt-1")
                    +"管理后台 v1.0"
                }
            }
            // Navigation
            nav {
                classes = setOf("flex-1", "p-4", "space-y-1", "overflow-y-auto")
                renderSidebarLink("/", "dashboard", "仪表盘", "fa-chart-line")
                renderSidebarLink("/agents", "agents", "Agent 管理", "fa-robot")
                renderSidebarLink("/channels", "channels", "频道配置", "fa-comments")
                renderSidebarLink("/models", "models", "模型配置", "fa-brain")
                renderSidebarLink("/logs", "logs", "日志查看", "fa-file-lines", active = true)
                renderSidebarLink("/settings", "settings", "系统设置", "fa-cog")
            }
            // User Info
            div {
                classes = setOf("p-4", "border-t", "border-gray-200")
                div {
                    classes = setOf("flex", "items-center", "gap-3")
                    div {
                        classes = setOf("w-10", "h-10", "bg-gray-200", "rounded-full", "flex", "items-center", "justify-center")
                        i { classes = setOf("fas", "fa-user", "text-gray-500") }
                    }
                    div {
                        p {
                            classes = setOf("font-medium", "text-dark")
                            +"管理员"
                        }
                        p {
                            classes = setOf("text-xs", "text-gray-500")
                            +"admin@ktclaw.dev"
                        }
                    }
                }
            }
        }
    }

    override fun FlowContent.renderContent() {
        div {
            classes = setOf("space-y-6")
            // Header Actions
            div {
                classes = setOf("flex", "flex-col", "sm:flex-row", "sm:items-center", "sm:justify-between", "gap-4")
                div {
                    p {
                        classes = setOf("text-gray-600", "mt-1")
                        +"实时查看和搜索系统日志、消息日志、错误日志"
                    }
                }
                div {
                    classes = setOf("flex", "items-center", "gap-3", "self-start", "sm:self-auto")
                    button {
                        onClick = "clearLogs()"
                        classes = setOf("btn", "border", "border-gray-300", "hover:bg-gray-50", "text-gray-700", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2")
                        i { classes = setOf("fas", "fa-trash") }
                        +"清空日志"
                    }
                    button {
                        onClick = "downloadLogs()"
                        classes = setOf("btn", "border", "border-gray-300", "hover:bg-gray-50", "text-gray-700", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2")
                        i { classes = setOf("fas", "fa-download") }
                        +"导出日志"
                    }
                    button {
                        id = "autoScrollBtn"
                        onClick = "toggleAutoScroll()"
                        classes = setOf("btn", "bg-success", "hover:bg-success/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2")
                        i { id = "autoScrollIcon"; classes = setOf("fas", "fa-check") }
                        span { id = "autoScrollText"; +"自动滚动" }
                    }
                }
            }
            // Filters
            div {
                classes = setOf("card", "bg-white", "rounded-xl", "shadow-sm", "p-4")
                div {
                    classes = setOf("grid", "grid-cols-1", "md:grid-cols-4", "gap-4")
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"日志级别"
                        }
                        select {
                            id = "logLevel"
                            onChange = "filterLogs()"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { value = "all"; +"全部" }
                            option { value = "info"; +"信息" }
                            option { value = "warn"; +"警告" }
                            option { value = "error"; +"错误" }
                            option { value = "debug"; +"调试" }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"模块"
                        }
                        select {
                            id = "logModule"
                            onChange = "filterLogs()"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { value = "all"; +"全部" }
                            option { value = "system"; +"系统" }
                            option { value = "agent"; +"Agent" }
                            option { value = "channel"; +"频道" }
                            option { value = "model"; +"模型" }
                            option { value = "message"; +"消息" }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"时间范围"
                        }
                        select {
                            id = "timeRange"
                            onChange = "filterLogs()"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { value = "1h"; +"最近 1 小时" }
                            option { value = "6h"; +"最近 6 小时" }
                            option { value = "24h"; selected = true; +"最近 24 小时" }
                            option { value = "7d"; +"最近 7 天" }
                            option { value = "all"; +"全部" }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"搜索"
                        }
                        input {
                            id = "searchKeyword"
                            type = InputType.text
                            placeholder = "搜索关键词..."
                            onInput = "filterLogs()"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                }
            }
            // Log Stream
            div {
                classes = setOf("card", "bg-dark", "rounded-xl", "shadow-sm", "overflow-hidden")
                div {
                    classes = setOf("px-6", "py-4", "border-b", "border-gray-700", "flex", "items-center", "justify-between")
                    div {
                        classes = setOf("flex", "items-center", "gap-3")
                        div { classes = setOf("w-3", "h-3", "rounded-full", "bg-red-500") }
                        div { classes = setOf("w-3", "h-3", "rounded-full", "bg-yellow-500") }
                        div { classes = setOf("w-3", "h-3", "rounded-full", "bg-green-500") }
                    }
                    div {
                        classes = setOf("text-sm", "text-gray-400")
                        +"实时日志流"
                    }
                    div {
                        span {
                            id = "logCount"
                            classes = setOf("text-xs", "bg-gray-700", "text-gray-300", "px-2", "py-1", "rounded-full")
                            +"124 条日志"
                        }
                    }
                }
                div {
                    id = "logStream"
                    classes = setOf("log-stream", "p-4", "h-[600px]", "overflow-y-auto", "text-gray-300", "text-sm")
                    // Sample logs
                    renderLogEntry("2024-01-15 21:30:00", "info", "system", "系统启动成功，版本 v1.0.0")
                    renderLogEntry("2024-01-15 21:30:02", "info", "agent", "Agent 管理器初始化完成，共加载 12 个 Agent")
                    renderLogEntry("2024-01-15 21:30:05", "info", "channel", "QQBot 连接成功，Bot ID: 123456789")
                    renderLogEntry("2024-01-15 21:30:08", "info", "channel", "Telegram Bot 连接成功，Bot ID: @ktclaw_bot")
                    renderLogEntry("2024-01-15 21:30:10", "warn", "channel", "Discord Bot 已禁用，跳过连接")
                    renderLogEntry("2024-01-15 21:30:15", "info", "model", "模型管理器初始化完成，共加载 8 个模型")
                    renderLogEntry("2024-01-15 21:30:20", "info", "system", "HTTP 服务启动成功，监听端口 8080")
                    renderLogEntry("2024-01-15 21:31:00", "info", "message", "收到 QQ 群消息: 你好，帮我写个 Python 脚本")
                    renderLogEntry("2024-01-15 21:31:02", "info", "agent", "分配任务给 Claude Code Agent")
                    renderLogEntry("2024-01-15 21:31:05", "info", "model", "调用模型 gpt-4-turbo，Token 消耗: 1245")
                    renderLogEntry("2024-01-15 21:31:10", "info", "message", "发送 QQ 群消息: 这是你要的 Python 脚本...")
                    renderLogEntry("2024-01-15 21:32:00", "error", "model", "调用模型 claude-3-opus 失败: API 密钥过期")
                    renderLogEntry("2024-01-15 21:32:01", "warn", "agent", "Claude Code Agent 执行失败，切换到 GPT-4")
                    renderLogEntry("2024-01-15 21:32:05", "info", "model", "调用模型 gpt-4-turbo，Token 消耗: 876")
                    renderLogEntry("2024-01-15 21:32:10", "info", "agent", "任务执行完成，耗时 10.2 秒")
                    renderLogEntry("2024-01-15 21:33:00", "debug", "system", "内存使用: 256MB / 1024MB")
                    renderLogEntry("2024-01-15 21:33:05", "debug", "system", "CPU 使用率: 15%")
                    renderLogEntry("2024-01-15 21:34:00", "info", "message", "收到私聊消息: /help")
                    renderLogEntry("2024-01-15 21:34:02", "info", "message", "发送私聊消息: 帮助信息如下...")
                    renderLogEntry("2024-01-15 21:35:00", "warn", "system", "磁盘空间不足: 剩余 10%")
                }
            }
        }

        script {
            unsafe {
                +"""
                let autoScrollEnabled = true;
                let logCount = 20;

                function toggleAutoScroll() {
                    autoScrollEnabled = !autoScrollEnabled;
                    const btn = document.getElementById('autoScrollBtn');
                    const icon = document.getElementById('autoScrollIcon');
                    const text = document.getElementById('autoScrollText');
                    
                    if (autoScrollEnabled) {
                        btn.classList.remove('bg-gray-500', 'hover:bg-gray-600');
                        btn.classList.add('bg-success', 'hover:bg-success/90');
                        icon.classList.remove('fa-times');
                        icon.classList.add('fa-check');
                        text.textContent = '自动滚动';
                        scrollToBottom();
                    } else {
                        btn.classList.remove('bg-success', 'hover:bg-success/90');
                        btn.classList.add('bg-gray-500', 'hover:bg-gray-600');
                        icon.classList.remove('fa-check');
                        icon.classList.add('fa-times');
                        text.textContent = '手动滚动';
                    }
                }

                function scrollToBottom() {
                    const logStream = document.getElementById('logStream');
                    logStream.scrollTop = logStream.scrollHeight;
                }

                function filterLogs() {
                    const level = document.getElementById('logLevel').value;
                    const module = document.getElementById('logModule').value;
                    const keyword = document.getElementById('searchKeyword').value.toLowerCase();
                    
                    const logEntries = document.querySelectorAll('.log-entry');
                    let visibleCount = 0;
                    
                    logEntries.forEach(entry => {
                        const entryLevel = entry.dataset.level;
                        const entryModule = entry.dataset.module;
                        const entryContent = entry.textContent.toLowerCase();
                        
                        let show = true;
                        
                        if (level !== 'all' && entryLevel !== level) {
                            show = false;
                        }
                        
                        if (module !== 'all' && entryModule !== module) {
                            show = false;
                        }
                        
                        if (keyword && !entryContent.includes(keyword)) {
                            show = false;
                        }
                        
                        entry.style.display = show ? 'block' : 'none';
                        if (show) visibleCount++;
                    });
                    
                    document.getElementById('logCount').textContent = visibleCount + ' 条日志';
                }

                function clearLogs() {
                    confirmAction('确定要清空所有日志吗？此操作不可撤销。', function() {
                        document.getElementById('logStream').innerHTML = '';
                        logCount = 0;
                        document.getElementById('logCount').textContent = '0 条日志';
                    });
                }

                function downloadLogs() {
                    const logEntries = document.querySelectorAll('.log-entry');
                    let content = '';
                    
                    logEntries.forEach(entry => {
                        content += entry.textContent.trim() + '\n';
                    });
                    
                    const blob = new Blob([content], { type: 'text/plain' });
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'ktclaw-logs-' + new Date().toISOString().slice(0, 10) + '.log';
                    a.click();
                    URL.revokeObjectURL(url);
                }

                // Simulate real-time log updates
                function addRandomLog() {
                    const levels = ['info', 'warn', 'error', 'debug'];
                    const modules = ['system', 'agent', 'channel', 'model', 'message'];
                    const messages = [
                        '系统心跳检测正常',
                        'Agent 任务执行完成',
                        '收到新的消息请求',
                        '模型调用成功',
                        'API 响应时间: 234ms',
                        '用户命令执行成功',
                        '缓存清理完成',
                        '数据库备份成功'
                    ];
                    
                    const now = new Date();
                    const time = now.toISOString().slice(0, 19).replace('T', ' ');
                    const level = levels[Math.floor(Math.random() * levels.length)];
                    const module = modules[Math.floor(Math.random() * modules.length)];
                    const message = messages[Math.floor(Math.random() * messages.length)];
                    
                    const logStream = document.getElementById('logStream');
                    const entry = document.createElement('div');
                    entry.className = 'log-entry mb-1';
                    entry.dataset.level = level;
                    entry.dataset.module = module;
                    
                    let levelClass, levelIcon;
                    switch(level) {
                        case 'info':
                            levelClass = 'text-blue-400';
                            levelIcon = 'fa-info-circle';
                            break;
                        case 'warn':
                            levelClass = 'text-yellow-400';
                            levelIcon = 'fa-exclamation-triangle';
                            break;
                        case 'error':
                            levelClass = 'text-red-400';
                            levelIcon = 'fa-times-circle';
                            break;
                        case 'debug':
                            levelClass = 'text-gray-400';
                            levelIcon = 'fa-bug';
                            break;
                        default:
                            levelClass = 'text-gray-300';
                            levelIcon = 'fa-circle';
                    }
                    
                    entry.innerHTML = `
                        <span class="text-gray-500">[${time}]</span>
                        <span class="${levelClass} mx-2"><i class="fas ${levelIcon}"></i> [${level.toUpperCase()}]</span>
                        <span class="text-purple-400">[${module}]</span>
                        <span class="ml-2">${message}</span>
                    `;
                    
                    logStream.appendChild(entry);
                    logCount++;
                    document.getElementById('logCount').textContent = logCount + ' 条日志';
                    
                    // Apply filters
                    filterLogs();
                    
                    // Auto scroll
                    if (autoScrollEnabled) {
                        scrollToBottom();
                    }
                }

                // Add new log every 3-5 seconds
                setInterval(() => {
                    if (Math.random() > 0.3) { // 70% chance to add a log
                        addRandomLog();
                    }
                }, 3000 + Math.random() * 2000);

                // Initial scroll to bottom
                scrollToBottom();
                """.trimIndent()
            }
        }
    }

    private fun FlowContent.renderLogEntry(time: String, level: String, module: String, content: String) {
        div {
            classes = setOf("log-entry", "mb-1")
            attributes["data-level"] = level
            attributes["data-module"] = module
            
            val (levelClass, levelIcon) = when (level) {
                "info" -> "text-blue-400" to "fa-info-circle"
                "warn" -> "text-yellow-400" to "fa-exclamation-triangle"
                "error" -> "text-red-400" to "fa-times-circle"
                "debug" -> "text-gray-400" to "fa-bug"
                else -> "text-gray-300" to "fa-circle"
            }
            
            span { classes = setOf("text-gray-500"); +"[$time]" }
            span {
                classes = setOf(levelClass, "mx-2")
                i { classes = setOf("fas", levelIcon) }
                +" [${level.uppercase()}]"
            }
            span { classes = setOf("text-purple-400"); +"[$module]" }
            span { classes = setOf("ml-2"); +content }
        }
    }
}
