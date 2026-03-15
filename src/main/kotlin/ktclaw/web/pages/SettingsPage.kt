package ktclaw.web.pages

import kotlinx.html.*

class SettingsPage : BasePage() {
    override fun getPageTitle(): String = "系统设置"

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
                renderSidebarLink("/logs", "logs", "日志查看", "fa-file-lines")
                renderSidebarLink("/settings", "settings", "系统设置", "fa-cog", active = true)
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
            // Header
            div {
                p {
                    classes = setOf("text-gray-600", "mt-1")
                    +"配置系统全局设置、安全选项和高级功能"
                }
            }
            // Settings Tabs
            div {
                classes = setOf("card", "bg-white", "rounded-xl", "shadow-sm", "overflow-hidden")
                // Tab Headers
                div {
                    classes = setOf("flex", "border-b", "border-gray-200")
                    button {
                        onClick = "switchTab('general')"
                        id = "tab-general"
                        classes = setOf("tab-btn", "px-6", "py-4", "font-medium", "text-primary", "border-b-2", "border-primary")
                        +"通用设置"
                    }
                    button {
                        onClick = "switchTab('security')"
                        id = "tab-security"
                        classes = setOf("tab-btn", "px-6", "py-4", "font-medium", "text-gray-500", "hover:text-gray-700")
                        +"安全设置"
                    }
                    button {
                        onClick = "switchTab('advanced')"
                        id = "tab-advanced"
                        classes = setOf("tab-btn", "px-6", "py-4", "font-medium", "text-gray-500", "hover:text-gray-700")
                        +"高级设置"
                    }
                    button {
                        onClick = "switchTab('backup')"
                        id = "tab-backup"
                        classes = setOf("tab-btn", "px-6", "py-4", "font-medium", "text-gray-500", "hover:text-gray-700")
                        +"备份与恢复"
                    }
                    button {
                        onClick = "switchTab('about')"
                        id = "tab-about"
                        classes = setOf("tab-btn", "px-6", "py-4", "font-medium", "text-gray-500", "hover:text-gray-700")
                        +"关于"
                    }
                }
                // Tab Content
                div {
                    classes = setOf("p-6")
                    // General Settings Tab
                    div {
                        id = "content-general"
                        classes = setOf("tab-content", "space-y-6")
                        renderGeneralSettings()
                    }
                    // Security Settings Tab
                    div {
                        id = "content-security"
                        classes = setOf("tab-content", "space-y-6", "hidden")
                        renderSecuritySettings()
                    }
                    // Advanced Settings Tab
                    div {
                        id = "content-advanced"
                        classes = setOf("tab-content", "space-y-6", "hidden")
                        renderAdvancedSettings()
                    }
                    // Backup Settings Tab
                    div {
                        id = "content-backup"
                        classes = setOf("tab-content", "space-y-6", "hidden")
                        renderBackupSettings()
                    }
                    // About Tab
                    div {
                        id = "content-about"
                        classes = setOf("tab-content", "space-y-6", "hidden")
                        renderAbout()
                    }
                }
            }
        }

        script {
            unsafe {
                +"""
                function switchTab(tabName) {
                    // Hide all tab contents
                    document.querySelectorAll('.tab-content').forEach(content => {
                        content.classList.add('hidden');
                    });
                    
                    // Remove active state from all tab buttons
                    document.querySelectorAll('.tab-btn').forEach(btn => {
                        btn.classList.remove('text-primary', 'border-b-2', 'border-primary');
                        btn.classList.add('text-gray-500');
                    });
                    
                    // Show selected tab content
                    document.getElementById('content-' + tabName).classList.remove('hidden');
                    
                    // Add active state to selected tab button
                    const activeTab = document.getElementById('tab-' + tabName);
                    activeTab.classList.remove('text-gray-500');
                    activeTab.classList.add('text-primary', 'border-b-2', 'border-primary');
                }

                function saveSettings() {
                    alert('设置已保存成功！');
                }

                function restartSystem() {
                    confirmAction('确定要重启系统吗？所有运行中的任务将会被中断。', function() {
                        alert('系统正在重启...');
                    });
                }

                function updateSystem() {
                    confirmAction('确定要检查系统更新吗？', function() {
                        alert('正在检查更新...');
                        setTimeout(() => {
                            alert('当前已是最新版本！');
                        }, 2000);
                    });
                }

                function exportConfig() {
                    alert('配置文件已导出！');
                }

                function importConfig() {
                    document.getElementById('importFile').click();
                }

                function handleFileImport(e) {
                    const file = e.target.files[0];
                    if (file) {
                        confirmAction('确定要导入配置吗？当前配置将会被覆盖。', function() {
                            alert('配置导入成功！系统将重启以应用新配置。');
                        });
                    }
                }

                function createBackup() {
                    confirmAction('确定要创建系统备份吗？', function() {
                        alert('备份创建成功！');
                    });
                }

                function restoreBackup() {
                    alert('请选择要恢复的备份文件');
                }
                """.trimIndent()
            }
        }
    }

    private fun FlowContent.renderGeneralSettings() {
        form {
            id = "generalForm"
            classes = setOf("space-y-6")
            // Basic Settings
            div {
                classes = setOf("space-y-4")
                h4 {
                    classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                    +"基础设置"
                }
                div {
                    classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"系统名称"
                        }
                        input {
                            type = InputType.text
                            value = "KtClaw 管理后台"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"监听端口"
                        }
                        input {
                            type = InputType.number
                            min = "1"
                            max = "65535"
                            value = "8080"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"时区"
                        }
                        select {
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { value = "Asia/Shanghai"; selected = true; +"Asia/Shanghai (中国标准时间)" }
                            option { value = "UTC"; +"UTC" }
                            option { value = "America/New_York"; +"America/New_York (美国东部时间)" }
                            option { value = "Europe/London"; +"Europe/London (格林威治标准时间)" }
                            option { value = "Asia/Tokyo"; +"Asia/Tokyo (日本标准时间)" }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"语言"
                        }
                        select {
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { value = "zh-CN"; selected = true; +"简体中文" }
                            option { value = "en-US"; +"English (US)" }
                            option { value = "ja-JP"; +"日本語" }
                        }
                    }
                }
            }
            // UI Settings
            div {
                classes = setOf("space-y-4")
                h4 {
                    classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                    +"界面设置"
                }
                div {
                    classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"主题"
                        }
                        select {
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { value = "light"; selected = true; +"浅色模式" }
                            option { value = "dark"; +"深色模式" }
                            option { value = "auto"; +"跟随系统" }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"每页显示条数"
                        }
                        select {
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { value = "10"; +"10 条" }
                            option { value = "20"; selected = true; +"20 条" }
                            option { value = "50"; +"50 条" }
                            option { value = "100"; +"100 条" }
                        }
                    }
                }
                div {
                    classes = setOf("space-y-2")
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            checked = true
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"显示欢迎消息"
                        }
                    }
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            checked = true
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"启用动画效果"
                        }
                    }
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"显示使用统计"
                        }
                    }
                }
            }
            // Form Actions
            div {
                classes = setOf("flex", "items-center", "justify-end", "gap-3", "pt-4", "border-t", "border-gray-200")
                button {
                    type = ButtonType.reset
                    classes = setOf("px-4", "py-2", "border", "border-gray-300", "rounded-lg", "text-gray-700", "hover:bg-gray-50", "font-medium")
                    +"重置"
                }
                button {
                    type = ButtonType.button
                    onClick = "saveSettings()"
                    classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium")
                    +"保存设置"
                }
            }
        }
    }

    private fun FlowContent.renderSecuritySettings() {
        form {
            id = "securityForm"
            classes = setOf("space-y-6")
            // Admin Account
            div {
                classes = setOf("space-y-4")
                h4 {
                    classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                    +"管理员账户"
                }
                div {
                    classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"用户名"
                        }
                        input {
                            type = InputType.text
                            value = "admin"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"邮箱"
                        }
                        input {
                            type = InputType.email
                            value = "admin@ktclaw.dev"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"新密码"
                        }
                        input {
                            type = InputType.password
                            placeholder = "留空则不修改"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"确认新密码"
                        }
                        input {
                            type = InputType.password
                            placeholder = "再次输入新密码"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                }
            }
            // Security Options
            div {
                classes = setOf("space-y-4")
                h4 {
                    classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                    +"安全选项"
                }
                div {
                    classes = setOf("space-y-2")
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            checked = true
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"启用登录验证码"
                        }
                    }
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            checked = true
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"记录登录日志"
                        }
                    }
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"启用两步验证 (2FA)"
                        }
                    }
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            checked = true
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"自动注销空闲会话 (30 分钟)"
                        }
                    }
                }
            }
            // Access Control
            div {
                classes = setOf("space-y-4")
                h4 {
                    classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                    +"访问控制"
                }
                div {
                    label {
                        classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                        +"允许访问的 IP 段（每行一个，留空则允许所有）"
                    }
                    textarea {
                        rows = "4"
                        placeholder = "例如: 192.168.1.0/24"
                        classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none", "font-mono", "text-sm")
                    }
                }
            }
            // Form Actions
            div {
                classes = setOf("flex", "items-center", "justify-end", "gap-3", "pt-4", "border-t", "border-gray-200")
                button {
                    type = ButtonType.reset
                    classes = setOf("px-4", "py-2", "border", "border-gray-300", "rounded-lg", "text-gray-700", "hover:bg-gray-50", "font-medium")
                    +"重置"
                }
                button {
                    type = ButtonType.button
                    onClick = "saveSettings()"
                    classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium")
                    +"保存设置"
                }
            }
        }
    }

    private fun FlowContent.renderAdvancedSettings() {
        form {
            id = "advancedForm"
            classes = setOf("space-y-6")
            // Performance Settings
            div {
                classes = setOf("space-y-4")
                h4 {
                    classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                    +"性能设置"
                }
                div {
                    classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"最大并发任务数"
                        }
                        input {
                            type = InputType.number
                            min = "1"
                            max = "100"
                            value = "10"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"任务超时时间 (秒)"
                        }
                        input {
                            type = InputType.number
                            min = "10"
                            max = "3600"
                            value = "300"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"日志保留天数"
                        }
                        input {
                            type = InputType.number
                            min = "1"
                            max = "365"
                            value = "30"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"缓存大小 (MB)"
                        }
                        input {
                            type = InputType.number
                            min = "64"
                            max = "8192"
                            value = "512"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                }
            }
            // Feature Toggles
            div {
                classes = setOf("space-y-4")
                h4 {
                    classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                    +"功能开关"
                }
                div {
                    classes = setOf("space-y-2")
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            checked = true
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"启用 Agent 自动重启"
                        }
                    }
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            checked = true
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"启用自动更新检查"
                        }
                    }
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            checked = true
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"启用错误报告"
                        }
                    }
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"启用调试模式"
                        }
                    }
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"启用开发者功能"
                        }
                    }
                }
            }
            // System Actions
            div {
                classes = setOf("space-y-4")
                h4 {
                    classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                    +"系统操作"
                }
                div {
                    classes = setOf("flex", "flex-wrap", "gap-3")
                    button {
                        type = ButtonType.button
                        onClick = "updateSystem()"
                        classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2")
                        i { classes = setOf("fas", "fa-download") }
                        +"检查更新"
                    }
                    button {
                        type = ButtonType.button
                        onClick = "restartSystem()"
                        classes = setOf("btn", "bg-warning", "hover:bg-warning/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2")
                        i { classes = setOf("fas", "fa-redo") }
                        +"重启系统"
                    }
                    button {
                        type = ButtonType.button
                        onClick = "exportConfig()"
                        classes = setOf("btn", "border", "border-gray-300", "hover:bg-gray-50", "text-gray-700", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2")
                        i { classes = setOf("fas", "fa-download") }
                        +"导出配置"
                    }
                    div {
                        input {
                            type = InputType.file
                            id = "importFile"
                            accept = ".json,.yaml,.yml"
                            onChange = "handleFileImport(event)"
                            classes = setOf("hidden")
                        }
                        button {
                            type = ButtonType.button
                            onClick = "importConfig()"
                            classes = setOf("btn", "border", "border-gray-300", "hover:bg-gray-50", "text-gray-700", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2")
                            i { classes = setOf("fas", "fa-upload") }
                            +"导入配置"
                        }
                    }
                }
            }
            // Form Actions
            div {
                classes = setOf("flex", "items-center", "justify-end", "gap-3", "pt-4", "border-t", "border-gray-200")
                button {
                    type = ButtonType.reset
                    classes = setOf("px-4", "py-2", "border", "border-gray-300", "rounded-lg", "text-gray-700", "hover:bg-gray-50", "font-medium")
                    +"重置"
                }
                button {
                    type = ButtonType.button
                    onClick = "saveSettings()"
                    classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium")
                    +"保存设置"
                }
            }
        }
    }

    private fun FlowContent.renderBackupSettings() {
        div {
            classes = setOf("space-y-6")
            // Backup Options
            div {
                classes = setOf("space-y-4")
                h4 {
                    classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                    +"备份设置"
                }
                div {
                    classes = setOf("space-y-2")
                    label {
                        classes = setOf("flex", "items-center", "gap-2")
                        input {
                            type = InputType.checkBox
                            checked = true
                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                        }
                        span {
                            classes = setOf("text-sm", "font-medium", "text-gray-700")
                            +"启用自动备份"
                        }
                    }
                }
                div {
                    classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"自动备份频率"
                        }
                        select {
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { value = "daily"; selected = true; +"每天" }
                            option { value = "weekly"; +"每周" }
                            option { value = "monthly"; +"每月" }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"备份保留数量"
                        }
                        input {
                            type = InputType.number
                            min = "1"
                            max = "100"
                            value = "7"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                }
                div {
                    label {
                        classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                        +"备份存储路径"
                    }
                    input {
                        type = InputType.text
                        value = "./backups"
                        classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                    }
                }
            }
            // Backup List
            div {
                classes = setOf("space-y-4")
                h4 {
                    classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                    +"现有备份"
                }
                div {
                    classes = setOf("flex", "items-center", "justify-between", "mb-4")
                    p {
                        classes = setOf("text-sm", "text-gray-600")
                        +"共 3 个备份文件"
                    }
                    button {
                        onClick = "createBackup()"
                        classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2")
                        i { classes = setOf("fas", "fa-plus") }
                        +"创建备份"
                    }
                }
                div {
                    classes = setOf("overflow-x-auto")
                    table {
                        classes = setOf("w-full")
                        thead {
                            tr {
                                classes = setOf("bg-gray-50", "border-b", "border-gray-200")
                                th {
                                    classes = setOf("text-left", "py-3", "px-4", "text-sm", "font-semibold", "text-gray-600")
                                    +"文件名"
                                }
                                th {
                                    classes = setOf("text-left", "py-3", "px-4", "text-sm", "font-semibold", "text-gray-600")
                                    +"创建时间"
                                }
                                th {
                                    classes = setOf("text-left", "py-3", "px-4", "text-sm", "font-semibold", "text-gray-600")
                                    +"大小"
                                }
                                th {
                                    classes = setOf("text-left", "py-3", "px-4", "text-sm", "font-semibold", "text-gray-600")
                                    +"操作"
                                }
                            }
                        }
                        tbody {
                            tr {
                                classes = setOf("table-row", "border-b", "border-gray-100")
                                td {
                                    classes = setOf("py-3", "px-4", "text-sm", "font-mono", "text-gray-700")
                                    +"backup-20240115-213000.tar.gz"
                                }
                                td {
                                    classes = setOf("py-3", "px-4", "text-sm", "text-gray-600")
                                    +"2024-01-15 21:30:00"
                                }
                                td {
                                    classes = setOf("py-3", "px-4", "text-sm", "text-gray-600")
                                    +"12.5 MB"
                                }
                                td {
                                    classes = setOf("py-3", "px-4")
                                    div {
                                        classes = setOf("flex", "items-center", "gap-2")
                                        button {
                                            onClick = "restoreBackup()"
                                            classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-primary", "text-sm")
                                            title = "恢复"
                                            i { classes = setOf("fas", "fa-undo") }
                                        }
                                        button {
                                            classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-gray-700", "text-sm")
                                            title = "下载"
                                            i { classes = setOf("fas", "fa-download") }
                                        }
                                        button {
                                            classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-danger", "text-sm")
                                            title = "删除"
                                            i { classes = setOf("fas", "fa-trash") }
                                        }
                                    }
                                }
                            }
                            tr {
                                classes = setOf("table-row", "border-b", "border-gray-100")
                                td {
                                    classes = setOf("py-3", "px-4", "text-sm", "font-mono", "text-gray-700")
                                    +"backup-20240114-213000.tar.gz"
                                }
                                td {
                                    classes = setOf("py-3", "px-4", "text-sm", "text-gray-600")
                                    +"2024-01-14 21:30:00"
                                }
                                td {
                                    classes = setOf("py-3", "px-4", "text-sm", "text-gray-600")
                                    +"12.3 MB"
                                }
                                td {
                                    classes = setOf("py-3", "px-4")
                                    div {
                                        classes = setOf("flex", "items-center", "gap-2")
                                        button {
                                            onClick = "restoreBackup()"
                                            classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-primary", "text-sm")
                                            title = "恢复"
                                            i { classes = setOf("fas", "fa-undo") }
                                        }
                                        button {
                                            classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-gray-700", "text-sm")
                                            title = "下载"
                                            i { classes = setOf("fas", "fa-download") }
                                        }
                                        button {
                                            classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-danger", "text-sm")
                                            title = "删除"
                                            i { classes = setOf("fas", "fa-trash") }
                                        }
                                    }
                                }
                            }
                            tr {
                                classes = setOf("table-row", "border-b", "border-gray-100")
                                td {
                                    classes = setOf("py-3", "px-4", "text-sm", "font-mono", "text-gray-700")
                                    +"backup-20240113-213000.tar.gz"
                                }
                                td {
                                    classes = setOf("py-3", "px-4", "text-sm", "text-gray-600")
                                    +"2024-01-13 21:30:00"
                                }
                                td {
                                    classes = setOf("py-3", "px-4", "text-sm", "text-gray-600")
                                    +"12.1 MB"
                                }
                                td {
                                    classes = setOf("py-3", "px-4")
                                    div {
                                        classes = setOf("flex", "items-center", "gap-2")
                                        button {
                                            onClick = "restoreBackup()"
                                            classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-primary", "text-sm")
                                            title = "恢复"
                                            i { classes = setOf("fas", "fa-undo") }
                                        }
                                        button {
                                            classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-gray-700", "text-sm")
                                            title = "下载"
                                            i { classes = setOf("fas", "fa-download") }
                                        }
                                        button {
                                            classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-danger", "text-sm")
                                            title = "删除"
                                            i { classes = setOf("fas", "fa-trash") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderAbout() {
        div {
            classes = setOf("space-y-6")
            // System Info
            div {
                classes = setOf("text-center", "py-8")
                div {
                    classes = setOf("w-24", "h-24", "bg-primary", "rounded-2xl", "flex", "items-center", "justify-center", "text-white", "mx-auto", "mb-4")
                    i { classes = setOf("fas", "fa-rocket", "text-4xl") }
                }
                h3 {
                    classes = setOf("text-2xl", "font-bold", "text-dark", "mb-2")
                    +"KtClaw"
                }
                p {
                    classes = setOf("text-gray-600", "mb-4")
                    +"版本 v1.0.0"
                }
                p {
                    classes = setOf("text-gray-500", "max-w-2xl", "mx-auto")
                    +"KtClaw 是一个基于 Ktor 的高性能 AI 机器人框架，支持多 Agent、多频道、多模型，提供强大的扩展能力和友好的管理界面。"
                }
            }
            // System Details
            div {
                classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-6")
                div {
                    classes = setOf("bg-gray-50", "rounded-lg", "p-6")
                    h4 {
                        classes = setOf("text-md", "font-semibold", "text-dark", "mb-4")
                        +"系统信息"
                    }
                    div {
                        classes = setOf("space-y-3")
                        renderInfoItem("系统版本", "v1.0.0")
                        renderInfoItem("构建时间", "2024-01-15")
                        renderInfoItem("Ktor 版本", "3.4.1")
                        renderInfoItem("Kotlin 版本", "1.9.20")
                        renderInfoItem("JVM 版本", "17")
                    }
                }
                div {
                    classes = setOf("bg-gray-50", "rounded-lg", "p-6")
                    h4 {
                        classes = setOf("text-md", "font-semibold", "text-dark", "mb-4")
                        +"运行时信息"
                    }
                    div {
                        classes = setOf("space-y-3")
                        renderInfoItem("操作系统", "Linux x86_64")
                        renderInfoItem("运行时间", "2 天 3 小时 45 分钟")
                        renderInfoItem("内存使用", "256 MB / 1024 MB")
                        renderInfoItem("CPU 使用率", "15%")
                        renderInfoItem("磁盘使用", "12 GB / 50 GB")
                    }
                }
            }
            // Links
            div {
                classes = setOf("text-center", "py-4")
                div {
                    classes = setOf("flex", "items-center", "justify-center", "gap-6")
                    a {
                        href = "https://github.com/ktclaw/ktclaw"
                        target = "_blank"
                        classes = setOf("text-primary", "hover:underline", "flex", "items-center", "gap-2")
                        i { classes = setOf("fab", "fa-github", "text-xl") }
                        +"GitHub 仓库"
                    }
                    a {
                        href = "https://ktclaw.dev/docs"
                        target = "_blank"
                        classes = setOf("text-primary", "hover:underline", "flex", "items-center", "gap-2")
                        i { classes = setOf("fas", "fa-book", "text-xl") }
                        +"文档"
                    }
                    a {
                        href = "https://github.com/ktclaw/ktclaw/issues"
                        target = "_blank"
                        classes = setOf("text-primary", "hover:underline", "flex", "items-center", "gap-2")
                        i { classes = setOf("fas", "fa-bug", "text-xl") }
                        +"提交问题"
                    }
                }
            }
            // Copyright
            div {
                classes = setOf("text-center", "pt-4", "border-t", "border-gray-200")
                p {
                    classes = setOf("text-sm", "text-gray-500")
                    +"© 2024 KtClaw. MIT License."
                }
            }
        }
    }

    private fun FlowContent.renderInfoItem(label: String, value: String) {
        div {
            classes = setOf("flex", "items-center", "justify-between")
            span {
                classes = setOf("text-sm", "text-gray-600")
                +label
            }
            span {
                classes = setOf("text-sm", "font-medium", "text-dark")
                +value
            }
        }
    }
}
