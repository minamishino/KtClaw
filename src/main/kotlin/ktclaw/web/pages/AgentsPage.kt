package ktclaw.web.pages

import kotlinx.html.*

class AgentsPage : BasePage() {
    override fun getPageTitle(): String = "Agent 管理"

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
                renderSidebarLink("/agents", "agents", "Agent 管理", "fa-robot", active = true)
                renderSidebarLink("/channels", "channels", "频道配置", "fa-comments")
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
                        +"管理和配置所有 AI Agent 实例"
                    }
                }
                button {
                    onClick = "openAddAgentModal()"
                    classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2", "self-start", "sm:self-auto")
                    i { classes = setOf("fas", "fa-plus") }
                    +"添加 Agent"
                }
            }
            // Filters
            div {
                classes = setOf("card", "bg-white", "rounded-xl", "shadow-sm", "p-4")
                div {
                    classes = setOf("flex", "flex-col", "md:flex-row", "gap-4")
                    div {
                        classes = setOf("flex-1")
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"搜索"
                        }
                        input {
                            type = InputType.text
                            placeholder = "搜索 Agent 名称、描述..."
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        classes = setOf("w-full", "md:w-48")
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"状态"
                        }
                        select {
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { +"全部" }
                            option { +"运行中" }
                            option { +"已停止" }
                            option { +"错误" }
                        }
                    }
                    div {
                        classes = setOf("w-full", "md:w-48")
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"类型"
                        }
                        select {
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { +"全部" }
                            option { +"Claude Code" }
                            option { +"Codex" }
                            option { +"Pi" }
                            option { +"Gemini" }
                        }
                    }
                }
            }
            // Agents List
            div {
                classes = setOf("card", "bg-white", "rounded-xl", "shadow-sm", "overflow-hidden")
                div {
                    classes = setOf("overflow-x-auto")
                    table {
                        classes = setOf("w-full")
                        thead {
                            tr {
                                classes = setOf("bg-gray-50", "border-b", "border-gray-200")
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"名称"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"类型"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"模型"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"状态"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"创建时间"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"操作"
                                }
                            }
                        }
                        tbody {
                            renderAgentRow(
                                id = "1",
                                name = "代码助手",
                                type = "Claude Code",
                                model = "claude-3-opus",
                                status = "running",
                                createdAt = "2024-01-10 14:30:00",
                                description = "专门用于代码生成和调试的 Agent"
                            )
                            renderAgentRow(
                                id = "2",
                                name = "通用助手",
                                type = "Codex",
                                model = "gpt-4-turbo",
                                status = "running",
                                createdAt = "2024-01-11 10:15:00",
                                description = "通用问题解答和日常助手"
                            )
                            renderAgentRow(
                                id = "3",
                                name = "创意助理",
                                type = "Pi",
                                model = "pi-2",
                                status = "stopped",
                                createdAt = "2024-01-12 16:45:00",
                                description = "内容创作和创意生成"
                            )
                            renderAgentRow(
                                id = "4",
                                name = "多模态助手",
                                type = "Gemini",
                                model = "gemini-pro",
                                status = "error",
                                createdAt = "2024-01-13 09:20:00",
                                description = "支持图片和视频理解的多模态 Agent"
                            )
                        }
                    }
                }
                // Pagination
                div {
                    classes = setOf("px-6", "py-4", "border-t", "border-gray-200", "flex", "items-center", "justify-between")
                    div {
                        classes = setOf("text-sm", "text-gray-600")
                        +"显示 1 到 4 条，共 12 条"
                    }
                    div {
                        classes = setOf("flex", "items-center", "gap-2")
                        button {
                            disabled = true
                            classes = setOf("px-3", "py-1", "border", "border-gray-300", "rounded-lg", "text-gray-400", "bg-gray-50", "cursor-not-allowed")
                            +"上一页"
                        }
                        button {
                            classes = setOf("px-3", "py-1", "bg-primary", "text-white", "rounded-lg")
                            +"1"
                        }
                        button {
                            classes = setOf("px-3", "py-1", "border", "border-gray-300", "rounded-lg", "text-gray-700", "hover:bg-gray-50")
                            +"2"
                        }
                        button {
                            classes = setOf("px-3", "py-1", "border", "border-gray-300", "rounded-lg", "text-gray-700", "hover:bg-gray-50")
                            +"3"
                        }
                        button {
                            classes = setOf("px-3", "py-1", "border", "border-gray-300", "rounded-lg", "text-gray-700", "hover:bg-gray-50")
                            +"下一页"
                        }
                    }
                }
            }
        }

        // Add Agent Modal
        div {
            id = "addAgentModal"
            classes = setOf("fixed", "inset-0", "bg-black/50", "flex", "items-center", "justify-center", "z-50", "hidden")
            div {
                classes = setOf("bg-white", "rounded-xl", "shadow-xl", "w-full", "max-w-2xl", "max-h-[90vh]", "overflow-y-auto")
                div {
                    classes = setOf("p-6", "border-b", "border-gray-200", "flex", "items-center", "justify-between")
                    h3 {
                        classes = setOf("text-xl", "font-semibold", "text-dark")
                        +"添加 Agent"
                    }
                    button {
                        onClick = "closeAddAgentModal()"
                        classes = setOf("text-gray-500", "hover:text-gray-700")
                        i { classes = setOf("fas", "fa-times", "text-xl") }
                    }
                }
                form {
                    classes = setOf("p-6", "space-y-4")
                    div {
                        classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                        div {
                            label {
                                classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                +"Agent 名称 *"
                            }
                            input {
                                type = InputType.text
                                required = true
                                placeholder = "请输入 Agent 名称"
                                classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            }
                        }
                        div {
                            label {
                                classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                +"Agent 类型 *"
                            }
                            select {
                                required = true
                                classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                                option { value = "claude-code"; +"Claude Code" }
                                option { value = "codex"; +"Codex" }
                                option { value = "pi"; +"Pi" }
                                option { value = "gemini"; +"Gemini" }
                            }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"使用模型 *"
                        }
                        select {
                            required = true
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            option { value = "claude-3-opus"; +"Claude 3 Opus" }
                            option { value = "claude-3-sonnet"; +"Claude 3 Sonnet" }
                            option { value = "gpt-4-turbo"; +"GPT-4 Turbo" }
                            option { value = "gpt-3.5-turbo"; +"GPT-3.5 Turbo" }
                            option { value = "gemini-pro"; +"Gemini Pro" }
                            option { value = "pi-2"; +"Pi 2" }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"描述"
                        }
                        textarea {
                            rows = "3"
                            placeholder = "请输入 Agent 描述信息"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                        }
                    }
                    div {
                        classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                        div {
                            label {
                                classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                +"温度 (Temperature)"
                            }
                            input {
                                type = InputType.number
                                min = "0"
                                max = "2"
                                step = "0.1"
                                value = "0.7"
                                classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            }
                        }
                        div {
                            label {
                                classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                +"最大 Token 数"
                            }
                            input {
                                type = InputType.number
                                min = "128"
                                max = "128000"
                                value = "4096"
                                classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            }
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
                                +"创建后自动启动"
                            }
                        }
                    }
                    // Form Actions
                    div {
                        classes = setOf("flex", "items-center", "justify-end", "gap-3", "pt-4", "border-t", "border-gray-200")
                        button {
                            type = ButtonType.button
                            onClick = "closeAddAgentModal()"
                            classes = setOf("px-4", "py-2", "border", "border-gray-300", "rounded-lg", "text-gray-700", "hover:bg-gray-50", "font-medium")
                            +"取消"
                        }
                        button {
                            type = ButtonType.submit
                            classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium")
                            +"创建 Agent"
                        }
                    }
                }
            }
        }

        script {
            unsafe {
                +"""
                function openAddAgentModal() {
                    document.getElementById('addAgentModal').classList.remove('hidden');
                }

                function closeAddAgentModal() {
                    document.getElementById('addAgentModal').classList.add('hidden');
                }

                function editAgent(id) {
                    alert('编辑 Agent: ' + id);
                }

                function deleteAgent(id) {
                    confirmAction('确定要删除这个 Agent 吗？此操作不可撤销。', function() {
                        alert('删除 Agent: ' + id);
                    });
                }

                function toggleAgentStatus(id, currentStatus) {
                    const action = currentStatus === 'running' ? '停止' : '启动';
                    confirmAction(`确定要${action}这个 Agent 吗？`, function() {
                        alert(`${action} Agent: ` + id);
                    });
                }

                // Close modal when clicking outside
                document.getElementById('addAgentModal').addEventListener('click', function(e) {
                    if (e.target === this) {
                        closeAddAgentModal();
                    }
                });
                """.trimIndent()
            }
        }
    }

    private fun FlowContent.renderAgentRow(
        id: String,
        name: String,
        type: String,
        model: String,
        status: String,
        createdAt: String,
        description: String
    ) {
        tr {
            classes = setOf("table-row", "border-b", "border-gray-100")
            td {
                classes = setOf("py-4", "px-6")
                div {
                    classes = setOf("flex", "items-center", "gap-3")
                    div {
                        classes = setOf(
                            "w-10",
                            "h-10",
                            "rounded-lg",
                            "flex",
                            "items-center",
                            "justify-center",
                            "text-white",
                            when (type) {
                                "Claude Code" -> "bg-blue-500"
                                "Codex" -> "bg-green-500"
                                "Pi" -> "bg-purple-500"
                                "Gemini" -> "bg-yellow-500"
                                else -> "bg-gray-500"
                            }
                        )
                        i {
                            classes = setOf(
                                "fas",
                                when (type) {
                                    "Claude Code" -> "fa-code"
                                    "Codex" -> "fa-robot"
                                    "Pi" -> "fa-lightbulb"
                                    "Gemini" -> "fa-image"
                                    else -> "fa-robot"
                                }
                            )
                        }
                    }
                    div {
                        p {
                            classes = setOf("font-medium", "text-dark")
                            +name
                        }
                        p {
                            classes = setOf("text-xs", "text-gray-500", "mt-1")
                            +description.take(50) + if (description.length > 50) "..." else ""
                        }
                    }
                }
            }
            td {
                classes = setOf("py-4", "px-6", "text-sm", "text-gray-700")
                +type
            }
            td {
                classes = setOf("py-4", "px-6", "text-sm", "text-gray-700")
                +model
            }
            td {
                classes = setOf("py-4", "px-6")
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
                        when (status) {
                            "running" -> "bg-green-100 text-green-800"
                            "stopped" -> "bg-gray-100 text-gray-800"
                            "error" -> "bg-red-100 text-red-800"
                            else -> "bg-yellow-100 text-yellow-800"
                        }
                    )
                    span {
                        classes = setOf(
                            "w-2",
                            "h-2",
                            "rounded-full",
                            when (status) {
                                "running" -> "bg-green-500 animate-pulse"
                                "stopped" -> "bg-gray-500"
                                "error" -> "bg-red-500"
                                else -> "bg-yellow-500"
                            }
                        )
                    }
                    +when (status) {
                        "running" -> "运行中"
                        "stopped" -> "已停止"
                        "error" -> "错误"
                        else -> "未知"
                    }
                }
            }
            td {
                classes = setOf("py-4", "px-6", "text-sm", "text-gray-600")
                +createdAt
            }
            td {
                classes = setOf("py-4", "px-6")
                div {
                    classes = setOf("flex", "items-center", "gap-2")
                    button {
                        onClick = "toggleAgentStatus('$id', '$status')"
                        title = if (status == "running") "停止" else "启动"
                        classes = setOf(
                            "p-2",
                            "rounded-lg",
                            "hover:bg-gray-100",
                            if (status == "running") "text-yellow-600" else "text-green-600"
                        )
                        i {
                            classes = setOf(
                                "fas",
                                if (status == "running") "fa-stop" else "fa-play"
                            )
                        }
                    }
                    button {
                        onClick = "editAgent('$id')"
                        title = "编辑"
                        classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-primary")
                        i { classes = setOf("fas", "fa-edit") }
                    }
                    button {
                        onClick = "deleteAgent('$id')"
                        title = "删除"
                        classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-danger")
                        i { classes = setOf("fas", "fa-trash") }
                    }
                }
            }
        }
    }
}
