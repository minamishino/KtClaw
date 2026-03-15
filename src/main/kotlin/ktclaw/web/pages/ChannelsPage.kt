package ktclaw.web.pages

import kotlinx.html.*

class ChannelsPage : BasePage() {
    override fun getPageTitle(): String = "频道配置"

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
                renderSidebarLink("/channels", "channels", "频道配置", "fa-comments", active = true)
                renderSidebarLink("/models", "models", "模型配置", "fa-brain")
                renderSidebarLink("/logs", "logs", "日志查看", "fa-file-lines")
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
                        +"配置和管理消息渠道，包括 QQBot、Telegram、Discord 等"
                    }
                }
                button {
                    onClick = "openAddChannelModal()"
                    classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2", "self-start", "sm:self-auto")
                    i { classes = setOf("fas", "fa-plus") }
                    +"添加频道"
                }
            }
            // Channel Cards
            div {
                classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "lg:grid-cols-3", "gap-6")
                renderChannelCard(
                    id = "1",
                    name = "QQ 主机器人",
                    type = "qq",
                    status = "running",
                    botId = "123456789",
                    guilds = 5,
                    users = 1200,
                    createdAt = "2024-01-05 14:30:00"
                )
                renderChannelCard(
                    id = "2",
                    name = "Telegram 通知",
                    type = "telegram",
                    status = "running",
                    botId = "@ktclaw_bot",
                    guilds = 1,
                    users = 150,
                    createdAt = "2024-01-08 10:15:00"
                )
                renderChannelCard(
                    id = "3",
                    name = "Discord 社区",
                    type = "discord",
                    status = "stopped",
                    botId = "KtClaw#1234",
                    guilds = 2,
                    users = 800,
                    createdAt = "2024-01-10 16:45:00"
                )
            }
            // QQBot Configuration Section
            div {
                classes = setOf("card", "bg-white", "rounded-xl", "shadow-sm", "p-6")
                div {
                    classes = setOf("flex", "items-center", "justify-between", "mb-6")
                    h3 {
                        classes = setOf("text-lg", "font-semibold", "text-dark")
                        +"QQBot 详细配置"
                    }
                    button {
                        onClick = "saveQQConfig()"
                        classes = setOf("btn", "bg-success", "hover:bg-success/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2")
                        i { classes = setOf("fas", "fa-save") }
                        +"保存配置"
                    }
                }
                form {
                    id = "qqConfigForm"
                    classes = setOf("space-y-6")
                    // Basic Settings
                    div {
                        classes = setOf("space-y-4")
                        h4 {
                            classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                            +"基础配置"
                        }
                        div {
                            classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                            div {
                                label {
                                    classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                    +"Bot AppID *"
                                }
                                input {
                                    type = InputType.text
                                    name = "appId"
                                    value = "123456789"
                                    required = true
                                    classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                                }
                            }
                            div {
                                label {
                                    classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                    +"Bot Token *"
                                }
                                input {
                                    type = InputType.password
                                    name = "token"
                                    value = "************************"
                                    required = true
                                    classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                                }
                            }
                            div {
                                label {
                                    classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                    +"Bot Secret *"
                                }
                                input {
                                    type = InputType.password
                                    name = "secret"
                                    value = "************************"
                                    required = true
                                    classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                                }
                            }
                            div {
                                label {
                                    classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                    +"沙箱模式"
                                }
                                div {
                                    classes = setOf("pt-2")
                                    label {
                                        classes = setOf("flex", "items-center", "gap-2")
                                        input {
                                            type = InputType.checkBox
                                            name = "sandbox"
                                            classes = setOf("rounded", "text-primary", "focus:ring-primary")
                                        }
                                        span {
                                            classes = setOf("text-sm", "text-gray-700")
                                            +"启用沙箱模式（仅用于测试）"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Message Settings
                    div {
                        classes = setOf("space-y-4")
                        h4 {
                            classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                            +"消息配置"
                        }
                        div {
                            classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                            div {
                                label {
                                    classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                    +"消息前缀"
                                }
                                input {
                                    type = InputType.text
                                    name = "commandPrefix"
                                    value = "/"
                                    placeholder = "/"
                                    classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                                }
                            }
                            div {
                                label {
                                    classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                    +"消息超时 (秒)"
                                }
                                input {
                                    type = InputType.number
                                    name = "messageTimeout"
                                    min = "10"
                                    max = "300"
                                    value = "60"
                                    classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                                }
                            }
                        }
                        div {
                            classes = setOf("space-y-3")
                            label {
                                classes = setOf("text-sm", "font-medium", "text-gray-700")
                                +"响应设置"
                            }
                            div {
                                classes = setOf("space-y-2")
                                label {
                                    classes = setOf("flex", "items-center", "gap-2")
                                    input {
                                        type = InputType.checkBox
                                        name = "respondToPrivate"
                                        checked = true
                                        classes = setOf("rounded", "text-primary", "focus:ring-primary")
                                    }
                                    span {
                                        classes = setOf("text-sm", "text-gray-700")
                                        +"响应私聊消息"
                                    }
                                }
                                label {
                                    classes = setOf("flex", "items-center", "gap-2")
                                    input {
                                        type = InputType.checkBox
                                        name = "respondToGroup"
                                        checked = true
                                        classes = setOf("rounded", "text-primary", "focus:ring-primary")
                                    }
                                    span {
                                        classes = setOf("text-sm", "text-gray-700")
                                        +"响应群聊消息"
                                    }
                                }
                                label {
                                    classes = setOf("flex", "items-center", "gap-2")
                                    input {
                                        type = InputType.checkBox
                                        name = "requireMention"
                                        checked = false
                                        classes = setOf("rounded", "text-primary", "focus:ring-primary")
                                    }
                                    span {
                                        classes = setOf("text-sm", "text-gray-700")
                                        +"群聊中需要 @ 才响应"
                                    }
                                }
                            }
                        }
                    }
                    // Advanced Settings
                    div {
                        classes = setOf("space-y-4")
                        h4 {
                            classes = setOf("text-md", "font-semibold", "text-dark", "pb-2", "border-b", "border-gray-200")
                            +"高级配置"
                        }
                        div {
                            classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                            div {
                                label {
                                    classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                    +"重试次数"
                                }
                                input {
                                    type = InputType.number
                                    name = "retryCount"
                                    min = "0"
                                    max = "10"
                                    value = "3"
                                    classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                                }
                            }
                            div {
                                label {
                                    classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                    +"并发数限制"
                                }
                                input {
                                    type = InputType.number
                                    name = "concurrencyLimit"
                                    min = "1"
                                    max = "100"
                                    value = "10"
                                    classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                                }
                            }
                        }
                        div {
                            label {
                                classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                +"管理员 QQ 号（每行一个）"
                            }
                            textarea {
                                name = "admins"
                                rows = "4"
                                +"""
                                123456789
                                987654321
                                """.trimIndent()
                                classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none", "font-mono", "text-sm")
                            }
                        }
                    }
                </form>
            }
        }

        // Add Channel Modal
        div {
            id = "addChannelModal"
            classes = setOf("fixed", "inset-0", "bg-black/50", "flex", "items-center", "justify-center", "z-50", "hidden")
            div {
                classes = setOf("bg-white", "rounded-xl", "shadow-xl", "w-full", "max-w-md", "max-h-[90vh]", "overflow-y-auto")
                div {
                    classes = setOf("p-6", "border-b", "border-gray-200", "flex", "items-center", "justify-between")
                    h3 {
                        classes = setOf("text-xl", "font-semibold", "text-dark")
                        +"添加频道"
                    }
                    button {
                        onClick = "closeAddChannelModal()"
                        classes = setOf("text-gray-500", "hover:text-gray-700")
                        i { classes = setOf("fas", "fa-times", "text-xl") }
                    }
                }
                form {
                    classes = setOf("p-6", "space-y-4")
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"频道类型 *"
                        }
                        select {
                            required = true
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { value = "qq"; +"QQBot" }
                            option { value = "telegram"; +"Telegram Bot" }
                            option { value = "discord"; +"Discord Bot" }
                            option { value = "wechat"; +"微信公众号" }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"频道名称 *"
                        }
                        input {
                            type = InputType.text
                            required = true
                            placeholder = "请输入频道名称"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        label {
                            classes = setOf("flex", "items-center", "gap-2")
                            input {
                                type = InputType.checkBox
                                checked = true
                                classes = setOf("rounded", "text-primary", "focus:ring-primary")
                            }
                            span {
                                classes = setOf("text-sm", "font-medium", "text-gray-700")
                                +"创建后自动启用"
                            }
                        }
                    }
                    // Form Actions
                    div {
                        classes = setOf("flex", "items-center", "justify-end", "gap-3", "pt-4", "border-t", "border-gray-200")
                        button {
                            type = ButtonType.button
                            onClick = "closeAddChannelModal()"
                            classes = setOf("px-4", "py-2", "border", "border-gray-300", "rounded-lg", "text-gray-700", "hover:bg-gray-50", "font-medium")
                            +"取消"
                        }
                        button {
                            type = ButtonType.submit
                            classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium")
                            +"创建频道"
                        }
                    }
                </form>
            }
        }

        script {
            unsafe {
                +"""
                function openAddChannelModal() {
                    document.getElementById('addChannelModal').classList.remove('hidden');
                }

                function closeAddChannelModal() {
                    document.getElementById('addChannelModal').classList.add('hidden');
                }

                function editChannel(id) {
                    alert('编辑频道: ' + id);
                }

                function deleteChannel(id) {
                    confirmAction('确定要删除这个频道吗？此操作不可撤销。', function() {
                        alert('删除频道: ' + id);
                    });
                }

                function toggleChannelStatus(id, currentStatus) {
                    const action = currentStatus === 'running' ? '停用' : '启用';
                    confirmAction(`确定要${action}这个频道吗？`, function() {
                        alert(`${action}频道: ` + id);
                    });
                }

                function saveQQConfig() {
                    const form = document.getElementById('qqConfigForm');
                    const formData = new FormData(form);
                    alert('QQBot 配置已保存');
                }

                // Close modal when clicking outside
                document.getElementById('addChannelModal').addEventListener('click', function(e) {
                    if (e.target === this) {
                        closeAddChannelModal();
                    }
                });
                """.trimIndent()
            }
        }
    }

    private fun FlowContent.renderChannelCard(
        id: String,
        name: String,
        type: String,
        status: String,
        botId: String,
        guilds: Int,
        users: Int,
        createdAt: String
    ) {
        div {
            classes = setOf("card", "bg-white", "rounded-xl", "shadow-sm", "overflow-hidden")
            // Header
            div {
                classes = setOf("p-6", "border-b", "border-gray-100")
                div {
                    classes = setOf("flex", "items-center", "justify-between")
                    div {
                        classes = setOf("flex", "items-center", "gap-3")
                        div {
                            classes = setOf(
                                "w-12",
                                "h-12",
                                "rounded-lg",
                                "flex",
                                "items-center",
                                "justify-center",
                                "text-white",
                                when (type) {
                                    "qq" -> "bg-blue-500"
                                    "telegram" -> "bg-sky-500"
                                    "discord" -> "bg-indigo-500"
                                    "wechat" -> "bg-green-500"
                                    else -> "bg-gray-500"
                                }
                            )
                            i {
                                classes = setOf(
                                    "fab",
                                    when (type) {
                                        "qq" -> "fa-qq"
                                        "telegram" -> "fa-telegram"
                                        "discord" -> "fa-discord"
                                        "wechat" -> "fa-weixin"
                                        else -> "fa-comments"
                                    },
                                    "text-xl"
                                )
                            }
                        }
                        div {
                            h4 {
                                classes = setOf("text-lg", "font-semibold", "text-dark")
                                +name
                            }
                            p {
                                classes = setOf("text-xs", "text-gray-500", "mt-1")
                                +when (type) {
                                    "qq" -> "QQ 机器人"
                                    "telegram" -> "Telegram 机器人"
                                    "discord" -> "Discord 机器人"
                                    "wechat" -> "微信公众号"
                                    else -> "消息频道"
                                }
                            }
                        }
                    }
                    div {
                        span {
                            classes = setOf(
                                "inline-flex",
                                "items-center",
                                "gap-1.5",
                                "px-2.5",
                                "py-1",
                                "rounded-full",
                                "text-xs",
                                "font-medium",
                                if (status == "running") "bg-green-100 text-green-800" else "bg-gray-100 text-gray-800"
                            )
                            span {
                                classes = setOf(
                                    "w-2",
                                    "h-2",
                                    "rounded-full",
                                    if (status == "running") "bg-green-500 animate-pulse" else "bg-gray-500"
                                )
                            }
                            +if (status == "running") "运行中" else "已停用"
                        }
                    }
                }
            }
            // Stats
            div {
                classes = setOf("p-6", "border-b", "border-gray-100")
                div {
                    classes = setOf("grid", "grid-cols-2", "gap-4")
                    div {
                        p {
                            classes = setOf("text-xs", "text-gray-500", "mb-1")
                            +"Bot ID"
                        }
                        p {
                            classes = setOf("text-sm", "font-medium", "text-dark")
                            +botId
                        }
                    }
                    div {
                        p {
                            classes = setOf("text-xs", "text-gray-500", "mb-1")
                            +"创建时间"
                        }
                        p {
                            classes = setOf("text-sm", "font-medium", "text-dark")
                            +createdAt.split(" ")[0]
                        }
                    }
                    div {
                        p {
                            classes = setOf("text-xs", "text-gray-500", "mb-1")
                            +"服务器/群数"
                        }
                        p {
                            classes = setOf("text-sm", "font-medium", "text-dark")
                            +"$guilds 个"
                        }
                    }
                    div {
                        p {
                            classes = setOf("text-xs", "text-gray-500", "mb-1")
                            +"用户数"
                        }
                        p {
                            classes = setOf("text-sm", "font-medium", "text-dark")
                            +"$users 人"
                        }
                    }
                }
            }
            // Actions
            div {
                classes = setOf("p-4", "flex", "items-center", "justify-between", "bg-gray-50")
                button {
                    onClick = "toggleChannelStatus('$id', '$status')"
                    classes = setOf(
                        "btn",
                        "px-3",
                        "py-1.5",
                        "rounded-lg",
                        "text-sm",
                        "font-medium",
                        if (status == "running") "bg-yellow-100 text-yellow-700 hover:bg-yellow-200" else "bg-green-100 text-green-700 hover:bg-green-200"
                    )
                    +if (status == "running") "停用" else "启用"
                }
                div {
                    classes = setOf("flex", "items-center", "gap-2")
                    button {
                        onClick = "editChannel('$id')"
                        classes = setOf("p-2", "rounded-lg", "hover:bg-gray-200", "text-gray-700")
                        title = "编辑"
                        i { classes = setOf("fas", "fa-edit") }
                    }
                    button {
                        onClick = "deleteChannel('$id')"
                        classes = setOf("p-2", "rounded-lg", "hover:bg-gray-200", "text-danger")
                        title = "删除"
                        i { classes = setOf("fas", "fa-trash") }
                    }
                }
            }
        }
    }
}
