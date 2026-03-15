package ktclaw.web.pages

import kotlinx.html.*

class DashboardPage : BasePage() {
    override fun getPageTitle(): String = "仪表盘"

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
                renderSidebarLink("/", "dashboard", "仪表盘", "fa-chart-line", active = true)
                renderSidebarLink("/agents", "agents", "Agent 管理", "fa-robot")
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
            // Stats Cards
            div {
                classes = setOf("grid", "grid-cols-1", "md:grid-cols-2", "lg:grid-cols-4", "gap-6")
                renderStatCard("运行中 Agent", "12", "fa-robot", "primary", "+2 今日新增")
                renderStatCard("活跃频道", "3", "fa-comments", "success", "QQ, Telegram, Discord")
                renderStatCard("可用模型", "8", "fa-brain", "warning", "2 个即将到期")
                renderStatCard("今日消息", "1,247", "fa-message", "secondary", "+18% 较昨日")
            }
            // Charts Row
            div {
                classes = setOf("grid", "grid-cols-1", "lg:grid-cols-2", "gap-6")
                // Message Trend Chart
                div {
                    classes = setOf("card", "bg-white", "rounded-xl", "shadow-sm", "p-6")
                    div {
                        classes = setOf("flex", "items-center", "justify-between", "mb-6")
                        h3 {
                            classes = setOf("text-lg", "font-semibold", "text-dark")
                            +"消息趋势 (近7天)"
                        }
                        div {
                            classes = setOf("text-sm", "text-gray-500")
                            +"单位: 条"
                        }
                    }
                    canvas {
                        id = "messageTrendChart"
                        height = "300"
                    }
                }
                // Agent Usage Chart
                div {
                    classes = setOf("card", "bg-white", "rounded-xl", "shadow-sm", "p-6")
                    div {
                        classes = setOf("flex", "items-center", "justify-between", "mb-6")
                        h3 {
                            classes = setOf("text-lg", "font-semibold", "text-dark")
                            +"Agent 使用占比"
                        }
                        div {
                            classes = setOf("text-sm", "text-gray-500")
                            +"过去30天"
                        }
                    }
                    canvas {
                        id = "agentUsageChart"
                        height = "300"
                    }
                }
            }
            // Recent Activity
            div {
                classes = setOf("card", "bg-white", "rounded-xl", "shadow-sm", "p-6")
                div {
                    classes = setOf("flex", "items-center", "justify-between", "mb-6")
                    h3 {
                        classes = setOf("text-lg", "font-semibold", "text-dark")
                        +"最近活动"
                    }
                    a {
                        href = "/logs"
                        classes = setOf("text-primary", "text-sm", "hover:underline")
                        +"查看全部日志"
                    }
                }
                div {
                    classes = setOf("overflow-x-auto")
                    table {
                        classes = setOf("w-full")
                        thead {
                            tr {
                                classes = setOf("border-b", "border-gray-200")
                                th {
                                    classes = setOf("text-left", "py-3", "px-4", "text-sm", "font-semibold", "text-gray-600")
                                    +"时间"
                                }
                                th {
                                    classes = setOf("text-left", "py-3", "px-4", "text-sm", "font-semibold", "text-gray-600")
                                    +"类型"
                                }
                                th {
                                    classes = setOf("text-left", "py-3", "px-4", "text-sm", "font-semibold", "text-gray-600")
                                    +"内容"
                                }
                                th {
                                    classes = setOf("text-left", "py-3", "px-4", "text-sm", "font-semibold", "text-gray-600")
                                    +"状态"
                                }
                            }
                        }
                        tbody {
                            renderActivityRow("2024-01-15 21:30:00", "消息", "QQ 群 #闲聊 收到用户消息", "success")
                            renderActivityRow("2024-01-15 21:28:00", "Agent", "Codex Agent 启动成功", "success")
                            renderActivityRow("2024-01-15 21:25:00", "系统", "模型调用成功: gpt-4", "success")
                            renderActivityRow("2024-01-15 21:20:00", "警告", "API 配额即将耗尽", "warning")
                            renderActivityRow("2024-01-15 21:15:00", "错误", "Telegram 连接失败，正在重试", "danger")
                        }
                    }
                }
            }
        }

        script {
            unsafe {
                +"""
                // Message Trend Chart
                const messageCtx = document.getElementById('messageTrendChart').getContext('2d');
                new Chart(messageCtx, {
                    type: 'line',
                    data: {
                        labels: ['1月9日', '1月10日', '1月11日', '1月12日', '1月13日', '1月14日', '1月15日'],
                        datasets: [{
                            label: '消息数量',
                            data: [420, 580, 690, 750, 920, 1050, 1247],
                            borderColor: '#3b82f6',
                            backgroundColor: 'rgba(59, 130, 246, 0.1)',
                            tension: 0.4,
                            fill: true
                        }]
                    },
                    options: {
                        responsive: true,
                        plugins: {
                            legend: {
                                display: false
                            }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                grid: {
                                    color: 'rgba(0, 0, 0, 0.05)'
                                }
                            },
                            x: {
                                grid: {
                                    display: false
                                }
                            }
                        }
                    }
                });

                // Agent Usage Chart
                const agentCtx = document.getElementById('agentUsageChart').getContext('2d');
                new Chart(agentCtx, {
                    type: 'doughnut',
                    data: {
                        labels: ['Claude Code', 'Codex', 'Pi', 'Gemini', '其他'],
                        datasets: [{
                            data: [35, 25, 20, 15, 5],
                            backgroundColor: [
                                '#3b82f6',
                                '#10b981',
                                '#f59e0b',
                                '#ef4444',
                                '#6366f1'
                            ],
                            borderWidth: 0
                        }]
                    },
                    options: {
                        responsive: true,
                        plugins: {
                            legend: {
                                position: 'bottom'
                            }
                        },
                        cutout: '70%'
                    }
                });
                """.trimIndent()
            }
        }
    }

    private fun FlowContent.renderStatCard(title: String, value: String, icon: String, color: String, subtitle: String) {
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

    private fun FlowContent.renderActivityRow(time: String, type: String, content: String, status: String) {
        tr {
            classes = setOf("table-row", "border-b", "border-gray-100")
            td {
                classes = setOf("py-3", "px-4", "text-sm", "text-gray-600")
                +time
            }
            td {
                classes = setOf("py-3", "px-4")
                span {
                    classes = setOf(
                        "px-2",
                        "py-1",
                        "rounded-full",
                        "text-xs",
                        "font-medium",
                        when (type) {
                            "消息" -> "bg-blue-100 text-blue-800"
                            "Agent" -> "bg-green-100 text-green-800"
                            "系统" -> "bg-purple-100 text-purple-800"
                            "警告" -> "bg-yellow-100 text-yellow-800"
                            "错误" -> "bg-red-100 text-red-800"
                            else -> "bg-gray-100 text-gray-800"
                        }
                    )
                    +type
                }
            }
            td {
                classes = setOf("py-3", "px-4", "text-sm", "text-gray-700")
                +content
            }
            td {
                classes = setOf("py-3", "px-4")
                span {
                    classes = setOf(
                        "inline-flex",
                        "items-center",
                        "gap-1",
                        "text-xs",
                        "font-medium",
                        when (status) {
                            "success" -> "text-green-600"
                            "warning" -> "text-yellow-600"
                            "danger" -> "text-red-600"
                            else -> "text-gray-600"
                        }
                    )
                    i {
                        classes = setOf(
                            "fas",
                            when (status) {
                                "success" -> "fa-check-circle"
                                "warning" -> "fa-exclamation-triangle"
                                "danger" -> "fa-times-circle"
                                else -> "fa-info-circle"
                            }
                        )
                    }
                    +when (status) {
                        "success" -> "成功"
                        "warning" -> "警告"
                        "danger" -> "错误"
                        else -> "信息"
                    }
                }
            }
        }
    }
}
