package ktclaw.web.pages

import kotlinx.html.*

class ModelsPage : BasePage() {
    override fun getPageTitle(): String = "模型配置"

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
                renderSidebarLink("/models", "models", "模型配置", "fa-brain", active = true)
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
                        +"管理 AI 模型配置和 API Key，支持 OpenAI、Anthropic、Google 等提供商"
                    }
                }
                button {
                    onClick = "openAddModelModal()"
                    classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium", "flex", "items-center", "gap-2", "self-start", "sm:self-auto")
                    i { classes = setOf("fas", "fa-plus") }
                    +"添加模型"
                }
            }
            // Usage Stats
            div {
                classes = setOf("grid", "grid-cols-1", "md:grid-cols-3", "gap-6")
                renderUsageCard("今日调用次数", "2,456", "fa-tachometer-alt", "primary", "+12% 较昨日")
                renderUsageCard("本月消费", "¥128.50", "fa-credit-card", "success", "¥300 月度额度")
                renderUsageCard("可用模型", "8", "fa-brain", "warning", "3 个即将到期")
            }
            // Models List
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
                                    +"模型名称"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"提供商"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"API Key"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"状态"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"调用次数"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"到期时间"
                                }
                                th {
                                    classes = setOf("text-left", "py-4", "px-6", "text-sm", "font-semibold", "text-gray-600")
                                    +"操作"
                                }
                            }
                        }
                        tbody {
                            renderModelRow(
                                id = "1",
                                name = "GPT-4 Turbo",
                                provider = "openai",
                                apiKey = "sk-***************************",
                                status = "active",
                                usage = 1245,
                                expiresAt = "2024-12-31",
                                baseUrl = "https://api.openai.com/v1"
                            )
                            renderModelRow(
                                id = "2",
                                name = "Claude 3 Opus",
                                provider = "anthropic",
                                apiKey = "sk-ant-***************************",
                                status = "active",
                                usage = 876,
                                expiresAt = "2024-11-30",
                                baseUrl = "https://api.anthropic.com"
                            )
                            renderModelRow(
                                id = "3",
                                name = "Gemini Pro",
                                provider = "google",
                                apiKey = "AIza***************************",
                                status = "active",
                                usage = 324,
                                expiresAt = "2024-10-15",
                                baseUrl = "https://generativelanguage.googleapis.com"
                            )
                            renderModelRow(
                                id = "4",
                                name = "GPT-3.5 Turbo",
                                provider = "openai",
                                apiKey = "sk-***************************",
                                status = "expired",
                                usage = 5678,
                                expiresAt = "2024-01-10",
                                baseUrl = "https://api.openai.com/v1"
                            )
                        }
                    }
                }
                // Pagination
                div {
                    classes = setOf("px-6", "py-4", "border-t", "border-gray-200", "flex", "items-center", "justify-between")
                    div {
                        classes = setOf("text-sm", "text-gray-600")
                        +"显示 1 到 4 条，共 8 条"
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
                            +"下一页"
                        }
                    }
                }
            }
        }

        // Add Model Modal
        div {
            id = "addModelModal"
            classes = setOf("fixed", "inset-0", "bg-black/50", "flex", "items-center", "justify-center", "z-50", "hidden")
            div {
                classes = setOf("bg-white", "rounded-xl", "shadow-xl", "w-full", "max-w-2xl", "max-h-[90vh]", "overflow-y-auto")
                div {
                    classes = setOf("p-6", "border-b", "border-gray-200", "flex", "items-center", "justify-between")
                    h3 {
                        classes = setOf("text-xl", "font-semibold", "text-dark")
                        +"添加模型"
                    }
                    button {
                        onClick = "closeAddModelModal()"
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
                                +"模型提供商 *"
                            }
                            select {
                                id = "providerSelect"
                                required = true
                                onChange = "updateModelOptions()"
                                classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                                option { value = "openai"; +"OpenAI" }
                                option { value = "anthropic"; +"Anthropic" }
                                option { value = "google"; +"Google AI" }
                                option { value = "alibaba"; +"阿里云通义千问" }
                                option { value = "baidu"; +"百度文心一言" }
                                option { value = "tencent"; +"腾讯混元" }
                                option { value = "custom"; +"自定义" }
                            }
                        }
                        div {
                            label {
                                classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                +"模型名称 *"
                            }
                            select {
                                id = "modelSelect"
                                required = true
                                classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                                // OpenAI models
                                option { value = "gpt-4-turbo"; +"GPT-4 Turbo" }
                                option { value = "gpt-4"; +"GPT-4" }
                                option { value = "gpt-3.5-turbo"; +"GPT-3.5 Turbo" }
                            }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"API Key *"
                        }
                        input {
                            type = InputType.password
                            required = true
                            placeholder = "请输入 API Key"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none", "font-mono", "text-sm")
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"API 端点地址"
                        }
                        input {
                            type = InputType.text
                            id = "baseUrlInput"
                            value = "https://api.openai.com/v1"
                            placeholder = "https://api.example.com/v1"
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none", "font-mono", "text-sm")
                        }
                    }
                    div {
                        classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "gap-4")
                        div {
                            label {
                                classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                +"请求超时 (秒)"
                            }
                            input {
                                type = InputType.number
                                min = "10"
                                max = "300"
                                value = "60"
                                classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            }
                        }
                        div {
                            label {
                                classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                                +"最大重试次数"
                            }
                            input {
                                type = InputType.number
                                min = "0"
                                max = "10"
                                value = "3"
                                classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
                            }
                        }
                    }
                    div {
                        label {
                            classes = setOf("block", "text-sm", "font-medium", "text-gray-700", "mb-1")
                            +"到期时间"
                        }
                        input {
                            type = InputType.date
                            classes = setOf("w-full", "px-4", "py-2", "border", "border-gray-300", "rounded-lg", "focus:ring-2", "focus:ring-primary/50", "focus:border-primary", "outline-none")
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
                                +"启用此模型"
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
                                +"设为默认模型"
                            }
                        }
                    }
                    // Form Actions
                    div {
                        classes = setOf("flex", "items-center", "justify-end", "gap-3", "pt-4", "border-t", "border-gray-200")
                        button {
                            type = ButtonType.button
                            onClick = "testConnection()"
                            classes = setOf("px-4", "py-2", "border", "border-gray-300", "rounded-lg", "text-gray-700", "hover:bg-gray-50", "font-medium", "flex", "items-center", "gap-2")
                            i { classes = setOf("fas", "fa-plug") }
                            +"测试连接"
                        }
                        button {
                            type = ButtonType.button
                            onClick = "closeAddModelModal()"
                            classes = setOf("px-4", "py-2", "border", "border-gray-300", "rounded-lg", "text-gray-700", "hover:bg-gray-50", "font-medium")
                            +"取消"
                        }
                        button {
                            type = ButtonType.submit
                            classes = setOf("btn", "bg-primary", "hover:bg-primary/90", "text-white", "px-4", "py-2", "rounded-lg", "font-medium")
                            +"保存模型"
                        }
                    }
                </form>
            }
        }

        script {
            unsafe {
                +"""
                const modelOptions = {
                    openai: [
                        { value: 'gpt-4-turbo', label: 'GPT-4 Turbo' },
                        { value: 'gpt-4', label: 'GPT-4' },
                        { value: 'gpt-3.5-turbo', label: 'GPT-3.5 Turbo' }
                    ],
                    anthropic: [
                        { value: 'claude-3-opus', label: 'Claude 3 Opus' },
                        { value: 'claude-3-sonnet', label: 'Claude 3 Sonnet' },
                        { value: 'claude-3-haiku', label: 'Claude 3 Haiku' }
                    ],
                    google: [
                        { value: 'gemini-pro', label: 'Gemini Pro' },
                        { value: 'gemini-ultra', label: 'Gemini Ultra' }
                    ],
                    alibaba: [
                        { value: 'qwen-max', label: '通义千问 Max' },
                        { value: 'qwen-plus', label: '通义千问 Plus' },
                        { value: 'qwen-turbo', label: '通义千问 Turbo' }
                    ],
                    baidu: [
                        { value: 'ernie-4.0', label: '文心一言 4.0' },
                        { value: 'ernie-3.5', label: '文心一言 3.5' }
                    ],
                    tencent: [
                        { value: 'hunyuan-pro', label: '混元 Pro' },
                        { value: 'hunyuan-standard', label: '混元 Standard' }
                    ],
                    custom: [
                        { value: 'custom', label: '自定义模型' }
                    ]
                };

                const baseUrls = {
                    openai: 'https://api.openai.com/v1',
                    anthropic: 'https://api.anthropic.com',
                    google: 'https://generativelanguage.googleapis.com',
                    alibaba: 'https://dashscope.aliyuncs.com/api/v1',
                    baidu: 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1',
                    tencent: 'https://hunyuan.tencentcloudapi.com',
                    custom: ''
                };

                function updateModelOptions() {
                    const provider = document.getElementById('providerSelect').value;
                    const modelSelect = document.getElementById('modelSelect');
                    const baseUrlInput = document.getElementById('baseUrlInput');
                    
                    // Clear existing options
                    modelSelect.innerHTML = '';
                    
                    // Add new options
                    modelOptions[provider].forEach(model => {
                        const option = document.createElement('option');
                        option.value = model.value;
                        option.textContent = model.label;
                        modelSelect.appendChild(option);
                    });
                    
                    // Update base URL
                    baseUrlInput.value = baseUrls[provider];
                }

                function openAddModelModal() {
                    document.getElementById('addModelModal').classList.remove('hidden');
                }

                function closeAddModelModal() {
                    document.getElementById('addModelModal').classList.add('hidden');
                }

                function editModel(id) {
                    alert('编辑模型: ' + id);
                }

                function deleteModel(id) {
                    confirmAction('确定要删除这个模型配置吗？此操作不可撤销。', function() {
                        alert('删除模型: ' + id);
                    });
                }

                function toggleModelStatus(id, currentStatus) {
                    const action = currentStatus === 'active' ? '禁用' : '启用';
                    confirmAction(`确定要${action}这个模型吗？`, function() {
                        alert(`${action}模型: ` + id);
                    });
                }

                function testConnection() {
                    alert('正在测试连接...');
                    setTimeout(() => {
                        alert('连接测试成功！');
                    }, 1000);
                }

                // Close modal when clicking outside
                document.getElementById('addModelModal').addEventListener('click', function(e) {
                    if (e.target === this) {
                        closeAddModelModal();
                    }
                });
                """.trimIndent()
            }
        }
    }

    private fun FlowContent.renderUsageCard(title: String, value: String, icon: String, color: String, subtitle: String) {
        div {
            classes = setOf("card", "bg-white", "rounded-xl", "shadow-sm", "p-6")
            div {
                classes = setOf("flex", "items-center", "justify-between")
                div {
                    p {
                        classes = setOf("text-sm", "text-gray-500", "font-medium")
                        +title
                    }
                    p {
                        classes = setOf("text-3xl", "font-bold", "text-dark", "mt-2")
                        +value
                    }
                    p {
                        classes = setOf("text-xs", "text-gray-500", "mt-2")
                        +subtitle
                    }
                }
                div {
                    classes = setOf(
                        "w-12",
                        "h-12",
                        "rounded-lg",
                        "flex",
                        "items-center",
                        "justify-center",
                        "text-white",
                        when (color) {
                            "primary" -> "bg-primary"
                            "success" -> "bg-success"
                            "warning" -> "bg-warning"
                            "danger" -> "bg-danger"
                            "secondary" -> "bg-secondary"
                            else -> "bg-primary"
                        }
                    )
                    i { classes = setOf("fas", icon, "text-xl") }
                }
            }
        }
    }

    private fun FlowContent.renderModelRow(
        id: String,
        name: String,
        provider: String,
        apiKey: String,
        status: String,
        usage: Int,
        expiresAt: String,
        baseUrl: String
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
                            when (provider) {
                                "openai" -> "bg-green-500"
                                "anthropic" -> "bg-purple-500"
                                "google" -> "bg-blue-500"
                                "alibaba" -> "bg-orange-500"
                                "baidu" -> "bg-blue-600"
                                "tencent" -> "bg-cyan-500"
                                else -> "bg-gray-500"
                            }
                        )
                        i {
                            classes = setOf(
                                "fas",
                                "fa-brain"
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
                            +when (provider) {
                                "openai" -> "OpenAI"
                                "anthropic" -> "Anthropic"
                                "google" -> "Google AI"
                                "alibaba" -> "阿里云"
                                "baidu" -> "百度"
                                "tencent" -> "腾讯"
                                else -> "自定义"
                            }
                        }
                    }
                }
            }
            td {
                classes = setOf("py-4", "px-6", "text-sm", "text-gray-700")
                +when (provider) {
                    "openai" -> "OpenAI"
                    "anthropic" -> "Anthropic"
                    "google" -> "Google"
                    "alibaba" -> "阿里云"
                    "baidu" -> "百度"
                    "tencent" -> "腾讯"
                    else -> "自定义"
                }
            }
            td {
                classes = setOf("py-4", "px-6", "text-sm", "font-mono", "text-gray-700")
                +apiKey.take(8) + "..." + apiKey.takeLast(4)
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
                            "active" -> "bg-green-100 text-green-800"
                            "inactive" -> "bg-gray-100 text-gray-800"
                            "expired" -> "bg-red-100 text-red-800"
                            else -> "bg-yellow-100 text-yellow-800"
                        }
                    )
                    span {
                        classes = setOf(
                            "w-2",
                            "h-2",
                            "rounded-full",
                            when (status) {
                                "active" -> "bg-green-500 animate-pulse"
                                "inactive" -> "bg-gray-500"
                                "expired" -> "bg-red-500"
                                else -> "bg-yellow-500"
                            }
                        )
                    }
                    +when (status) {
                        "active" -> "正常"
                        "inactive" -> "禁用"
                        "expired" -> "已到期"
                        else -> "未知"
                    }
                }
            }
            td {
                classes = setOf("py-4", "px-6", "text-sm", "text-gray-700")
                +"$usage 次"
            }
            td {
                classes = setOf("py-4", "px-6", "text-sm", "text-gray-700")
                +expiresAt
            }
            td {
                classes = setOf("py-4", "px-6")
                div {
                    classes = setOf("flex", "items-center", "gap-2")
                    button {
                        onClick = "toggleModelStatus('$id', '$status')"
                        title = if (status == "active") "禁用" else "启用"
                        classes = setOf(
                            "p-2",
                            "rounded-lg",
                            "hover:bg-gray-100",
                            if (status == "active") "text-yellow-600" else "text-green-600"
                        )
                        i {
                            classes = setOf(
                                "fas",
                                if (status == "active") "fa-power-off" else "fa-play"
                            )
                        }
                    }
                    button {
                        onClick = "editModel('$id')"
                        title = "编辑"
                        classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-primary")
                        i { classes = setOf("fas", "fa-edit") }
                    }
                    button {
                        onClick = "deleteModel('$id')"
                        title = "删除"
                        classes = setOf("p-2", "rounded-lg", "hover:bg-gray-100", "text-danger")
                        i { classes = setOf("fas", "fa-trash") }
                    }
                }
            }
        }
    }
}
