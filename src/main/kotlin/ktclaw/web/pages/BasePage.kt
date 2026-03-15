package ktclaw.web.pages

import kotlinx.html.*
import kotlinx.html.stream.createHTML

open class BasePage {
    open fun render(): String = createHTML().html {
        lang = "zh-CN"
        head {
            meta { charset = "UTF-8" }
            meta { name = "viewport"; content = "width=device-width, initial-scale=1.0" }
            title("KtClaw 管理后台")
            // Tailwind CSS
            script { src = "https://cdn.tailwindcss.com" }
            // Font Awesome
            link { rel = "stylesheet"; href = "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css" }
            // Chart.js
            script { src = "https://cdn.jsdelivr.net/npm/chart.js@4.4.8/dist/chart.umd.min.js" }
            // Custom Tailwind config
            script {
                unsafe {
                    +"""
                    tailwind.config = {
                        theme: {
                            extend: {
                                colors: {
                                    primary: '#3b82f6',
                                    secondary: '#6366f1',
                                    success: '#10b981',
                                    warning: '#f59e0b',
                                    danger: '#ef4444',
                                    dark: '#1e293b',
                                },
                                fontFamily: {
                                    sans: ['Inter', 'system-ui', 'sans-serif'],
                                },
                            }
                        }
                    }
                    """.trimIndent()
                }
            }
            style {
                unsafe {
                    +"""
                    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');
                    
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: 'Inter', sans-serif;
                        background-color: #f8fafc;
                        color: #1e293b;
                    }
                    
                    .sidebar-link.active {
                        background-color: rgba(59, 130, 246, 0.1);
                        color: #3b82f6;
                        border-left: 3px solid #3b82f6;
                    }
                    
                    .card {
                        transition: all 0.3s ease;
                    }
                    
                    .card:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
                    }
                    
                    .btn {
                        transition: all 0.2s ease;
                    }
                    
                    .btn:hover {
                        transform: translateY(-1px);
                    }
                    
                    .table-row:hover {
                        background-color: rgba(59, 130, 246, 0.05);
                    }
                    
                    .log-stream {
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 0.875rem;
                        line-height: 1.5;
                    }
                    """.trimIndent()
                }
            }
        }
        body {
            div {
                classes = setOf("flex", "h-screen", "overflow-hidden")
                // Sidebar
                renderSidebar()
                // Main Content
                div {
                    classes = setOf("flex-1", "flex", "flex-col", "overflow-hidden")
                    // Header
                    renderHeader()
                    // Page Content
                    main {
                        classes = setOf("flex-1", "overflow-y-auto", "p-6", "bg-gray-50")
                        renderContent()
                    }
                }
            }
            // Common Scripts
            script {
                unsafe {
                    +"""
                    // Sidebar toggle for mobile
                    function toggleSidebar() {
                        const sidebar = document.getElementById('sidebar');
                        sidebar.classList.toggle('hidden');
                        sidebar.classList.toggle('flex');
                    }
                    
                    // Confirmation dialog
                    function confirmAction(message, callback) {
                        if (confirm(message)) {
                            callback();
                        }
                    }
                    
                    // Format date
                    function formatDate(dateString) {
                        const date = new Date(dateString);
                        return date.toLocaleString('zh-CN', {
                            year: 'numeric',
                            month: '2-digit',
                            day: '2-digit',
                            hour: '2-digit',
                            minute: '2-digit',
                            second: '2-digit'
                        });
                    }
                    """.trimIndent()
                }
            }
        }
    }

    protected open fun FlowContent.renderSidebar() {
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

    protected fun FlowContent.renderSidebarLink(href: String, page: String, name: String, icon: String, active: Boolean = false) {
        a {
            this.href = href
            classes = setOf(
                "sidebar-link",
                "flex",
                "items-center",
                "gap-3",
                "px-4",
                "py-3",
                "rounded-lg",
                "text-gray-700",
                "hover:bg-gray-100",
                "transition-all"
            ) + if (active) setOf("active") else emptySet()
            i { classes = setOf("fas", icon, "w-5", "text-center") }
            span { +name }
        }
    }

    protected open fun FlowContent.renderHeader() {
        header {
            classes = setOf("bg-white", "border-b", "border-gray-200", "px-6", "py-4", "flex", "items-center", "justify-between")
            div {
                classes = setOf("flex", "items-center", "gap-4")
                // Mobile menu button
                button {
                    onClick = "toggleSidebar()"
                    classes = setOf("md:hidden", "p-2", "rounded-lg", "hover:bg-gray-100")
                    i { classes = setOf("fas", "fa-bars", "text-gray-700") }
                }
                // Page title
                h2 {
                    id = "page-title"
                    classes = setOf("text-2xl", "font-bold", "text-dark")
                    +getPageTitle()
                }
            }
            // Actions
            div {
                classes = setOf("flex", "items-center", "gap-3")
                button {
                    classes = setOf("btn", "p-2", "rounded-lg", "hover:bg-gray-100", "text-gray-700")
                    title = "通知"
                    i { classes = setOf("fas", "fa-bell") }
                }
                button {
                    classes = setOf("btn", "p-2", "rounded-lg", "hover:bg-gray-100", "text-gray-700")
                    title = "搜索"
                    i { classes = setOf("fas", "fa-search") }
                }
            }
        }
    }

    protected open fun FlowContent.renderContent() {
        // Override in child classes
    }

    protected open fun getPageTitle(): String = "KtClaw 管理后台"
}
