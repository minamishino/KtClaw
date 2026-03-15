// dashboard.js - 仪表盘图表与数据可视化
class KtClawDashboard {
    constructor() {
        this.charts = new Map();
        this.data = {};
        this.updateInterval = null;
        this.init();
    }

    init() {
        // 监听数据更新事件
        window.addEventListener('dataUpdate', (e) => this.handleDataUpdate(e.detail));
        
        // 页面加载完成后初始化图表
        document.addEventListener('DOMContentLoaded', () => {
            this.initCharts();
            this.loadInitialData();
            this.startAutoRefresh();
        });
    }

    // 初始化所有图表
    initCharts() {
        // 系统资源监控图表
        this.initResourceChart();
        
        // 请求统计图表
        this.initRequestChart();
        
        // 状态分布饼图
        this.initStatusChart();
        
        // 响应时间折线图
        this.initResponseTimeChart();
        
        // 自定义指标图表
        this.initCustomCharts();
    }

    // 系统资源监控图表
    initResourceChart() {
        const ctx = document.getElementById('resource-chart');
        if (!ctx) return;

        const chart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: Array(20).fill(''),
                datasets: [
                    {
                        label: 'CPU 使用率',
                        data: Array(20).fill(0),
                        borderColor: '#0d6efd',
                        backgroundColor: 'rgba(13, 110, 253, 0.1)',
                        tension: 0.4,
                        fill: true
                    },
                    {
                        label: '内存使用率',
                        data: Array(20).fill(0),
                        borderColor: '#198754',
                        backgroundColor: 'rgba(25, 135, 84, 0.1)',
                        tension: 0.4,
                        fill: true
                    },
                    {
                        label: '磁盘使用率',
                        data: Array(20).fill(0),
                        borderColor: '#ffc107',
                        backgroundColor: 'rgba(255, 193, 7, 0.1)',
                        tension: 0.4,
                        fill: true
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top'
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false
                    }
                },
                scales: {
                    y: {
                        min: 0,
                        max: 100,
                        ticks: {
                            callback: function(value) {
                                return value + '%';
                            }
                        }
                    }
                },
                animation: {
                    duration: 750
                }
            }
        });

        this.charts.set('resource', chart);
    }

    // 请求统计图表
    initRequestChart() {
        const ctx = document.getElementById('request-chart');
        if (!ctx) return;

        const chart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['GET', 'POST', 'PUT', 'DELETE', '其他'],
                datasets: [{
                    label: '请求数量',
                    data: [0, 0, 0, 0, 0],
                    backgroundColor: [
                        'rgba(13, 110, 253, 0.8)',
                        'rgba(25, 135, 84, 0.8)',
                        'rgba(255, 193, 7, 0.8)',
                        'rgba(220, 53, 69, 0.8)',
                        'rgba(108, 117, 125, 0.8)'
                    ],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });

        this.charts.set('request', chart);
    }

    // 状态分布饼图
    initStatusChart() {
        const ctx = document.getElementById('status-chart');
        if (!ctx) return;

        const chart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['正常', '警告', '错误', '离线'],
                datasets: [{
                    data: [0, 0, 0, 0],
                    backgroundColor: [
                        'rgba(25, 135, 84, 0.8)',
                        'rgba(255, 193, 7, 0.8)',
                        'rgba(220, 53, 69, 0.8)',
                        'rgba(108, 117, 125, 0.8)'
                    ],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                },
                cutout: '70%'
            }
        });

        this.charts.set('status', chart);
    }

    // 响应时间折线图
    initResponseTimeChart() {
        const ctx = document.getElementById('response-time-chart');
        if (!ctx) return;

        const chart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: Array(30).fill(''),
                datasets: [{
                    label: '平均响应时间',
                    data: Array(30).fill(0),
                    borderColor: '#6f42c1',
                    backgroundColor: 'rgba(111, 66, 193, 0.1)',
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return context.parsed.y + ' ms';
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function(value) {
                                return value + 'ms';
                            }
                        }
                    }
                }
            }
        });

        this.charts.set('response-time', chart);
    }

    // 初始化自定义图表
    initCustomCharts() {
        // 遍历所有带有data-chart属性的元素
        document.querySelectorAll('[data-chart]').forEach(el => {
            const chartType = el.dataset.chart;
            const chartId = el.id;
            
            if (this.charts.has(chartId)) return;
            
            // 从data属性获取配置
            const config = JSON.parse(el.dataset.config || '{}');
            
            try {
                const chart = new Chart(el, {
                    type: chartType,
                    data: config.data || { labels: [], datasets: [] },
                    options: config.options || {}
                });
                
                this.charts.set(chartId, chart);
            } catch (error) {
                console.error('自定义图表初始化失败:', error);
            }
        });
    }

    // 加载初始数据
    async loadInitialData() {
        try {
            const response = await fetch('/api/dashboard/initial');
            if (response.ok) {
                const data = await response.json();
                this.handleDataUpdate(data);
            }
        } catch (error) {
            console.error('初始数据加载失败:', error);
        }
    }

    // 处理数据更新
    handleDataUpdate(data) {
        this.data = { ...this.data, ...data };
        
        // 更新系统资源图表
        if (data.resource) {
            this.updateResourceChart(data.resource);
        }
        
        // 更新请求统计图表
        if (data.requests) {
            this.updateRequestChart(data.requests);
        }
        
        // 更新状态分布图表
        if (data.status) {
            this.updateStatusChart(data.status);
        }
        
        // 更新响应时间图表
        if (data.responseTime) {
            this.updateResponseTimeChart(data.responseTime);
        }
        
        // 更新统计卡片
        if (data.stats) {
            this.updateStatsCards(data.stats);
        }
        
        // 更新自定义图表
        Object.keys(data).forEach(key => {
            if (this.charts.has(key) && !['resource', 'request', 'status', 'response-time'].includes(key)) {
                this.updateCustomChart(key, data[key]);
            }
        });

        // 触发UI更新事件
        window.dispatchEvent(new CustomEvent('dashboardUpdated', { detail: data }));
    }

    // 更新资源监控图表
    updateResourceChart(resourceData) {
        const chart = this.charts.get('resource');
        if (!chart) return;

        // 移除最旧的数据点
        chart.data.labels.shift();
        chart.data.datasets.forEach(dataset => dataset.data.shift());

        // 添加新的数据点
        const time = new Date().toLocaleTimeString();
        chart.data.labels.push(time);
        
        chart.data.datasets[0].data.push(resourceData.cpu || 0);
        chart.data.datasets[1].data.push(resourceData.memory || 0);
        chart.data.datasets[2].data.push(resourceData.disk || 0);

        chart.update('none');
    }

    // 更新请求统计图表
    updateRequestChart(requestData) {
        const chart = this.charts.get('request');
        if (!chart) return;

        chart.data.datasets[0].data = [
            requestData.get || 0,
            requestData.post || 0,
            requestData.put || 0,
            requestData.delete || 0,
            requestData.other || 0
        ];

        chart.update('none');
    }

    // 更新状态分布图表
    updateStatusChart(statusData) {
        const chart = this.charts.get('status');
        if (!chart) return;

        chart.data.datasets[0].data = [
            statusData.normal || 0,
            statusData.warning || 0,
            statusData.error || 0,
            statusData.offline || 0
        ];

        chart.update('none');
    }

    // 更新响应时间图表
    updateResponseTimeChart(responseTimeData) {
        const chart = this.charts.get('response-time');
        if (!chart) return;

        // 移除最旧的数据点
        chart.data.labels.shift();
        chart.data.datasets[0].data.shift();

        // 添加新的数据点
        const time = new Date().toLocaleTimeString();
        chart.data.labels.push(time);
        chart.data.datasets[0].data.push(responseTimeData.average || 0);

        chart.update('none');
    }

    // 更新统计卡片
    updateStatsCards(stats) {
        // 总请求数
        const totalRequestsEl = document.getElementById('total-requests');
        if (totalRequestsEl && stats.totalRequests !== undefined) {
            this.animateNumber(totalRequestsEl, stats.totalRequests);
        }

        // 活跃用户
        const activeUsersEl = document.getElementById('active-users');
        if (activeUsersEl && stats.activeUsers !== undefined) {
            this.animateNumber(activeUsersEl, stats.activeUsers);
        }

        // 正常运行时间
        const uptimeEl = document.getElementById('uptime');
        if (uptimeEl && stats.uptime !== undefined) {
            uptimeEl.textContent = this.formatUptime(stats.uptime);
        }

        // 错误率
        const errorRateEl = document.getElementById('error-rate');
        if (errorRateEl && stats.errorRate !== undefined) {
            errorRateEl.textContent = `${stats.errorRate.toFixed(2)}%`;
            // 根据错误率改变颜色
            if (stats.errorRate > 5) {
                errorRateEl.className = 'text-danger';
            } else if (stats.errorRate > 1) {
                errorRateEl.className = 'text-warning';
            } else {
                errorRateEl.className = 'text-success';
            }
        }
    }

    // 更新自定义图表
    updateCustomChart(chartId, data) {
        const chart = this.charts.get(chartId);
        if (!chart) return;

        // 支持不同类型的更新
        if (Array.isArray(data)) {
            // 完整替换数据
            chart.data.datasets[0].data = data;
        } else if (data.labels && data.datasets) {
            // 完整配置更新
            chart.data = { ...chart.data, ...data };
        } else if (data.point !== undefined) {
            // 追加数据点（折线图）
            chart.data.labels.shift();
            chart.data.datasets[0].data.shift();
            chart.data.labels.push(new Date().toLocaleTimeString());
            chart.data.datasets[0].data.push(data.point);
        }

        chart.update('none');
    }

    // 数字动画效果
    animateNumber(element, targetValue, duration = 1000) {
        const startValue = parseInt(element.textContent.replace(/,/g, '')) || 0;
        const difference = targetValue - startValue;
        
        if (difference === 0) return;
        
        const startTime = performance.now();
        
        const animate = (currentTime) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            const easeProgress = this.easeOutCubic(progress);
            const currentValue = Math.round(startValue + difference * easeProgress);
            
            element.textContent = currentValue.toLocaleString();
            
            if (progress < 1) {
                requestAnimationFrame(animate);
            }
        };
        
        requestAnimationFrame(animate);
    }

    // 缓动函数
    easeOutCubic(t) {
        return 1 - Math.pow(1 - t, 3);
    }

    // 格式化运行时间
    formatUptime(seconds) {
        const days = Math.floor(seconds / 86400);
        const hours = Math.floor((seconds % 86400) / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        
        if (days > 0) {
            return `${days}天 ${hours}小时`;
        } else if (hours > 0) {
            return `${hours}小时 ${minutes}分钟`;
        } else {
            return `${minutes}分钟`;
        }
    }

    // 开始自动刷新
    startAutoRefresh(interval = 5000) {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
        }
        
        // 从配置获取刷新间隔
        const refreshInterval = document.body.dataset.refreshInterval || interval;
        
        this.updateInterval = setInterval(() => {
            this.fetchLatestData();
        }, refreshInterval);
    }

    // 获取最新数据
    async fetchLatestData() {
        try {
            const response = await fetch('/api/dashboard/latest');
            if (response.ok) {
                const data = await response.json();
                this.handleDataUpdate(data);
            }
        } catch (error) {
            console.error('数据刷新失败:', error);
        }
    }

    // 停止自动刷新
    stopAutoRefresh() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
    }

    // 导出图表数据
    exportChartData(chartId) {
        const chart = this.charts.get(chartId);
        if (!chart) return null;

        const data = {
            chartId,
            type: chart.config.type,
            labels: chart.data.labels,
            datasets: chart.data.datasets.map(ds => ({
                label: ds.label,
                data: ds.data
            })),
            exportedAt: new Date().toISOString()
        };

        // 下载为JSON文件
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${chartId}-data-${Date.now()}.json`;
        a.click();
        URL.revokeObjectURL(url);

        return data;
    }

    // 刷新所有图表
    refreshAll() {
        this.charts.forEach(chart => chart.update());
    }

    // 销毁所有图表
    destroy() {
        this.stopAutoRefresh();
        this.charts.forEach(chart => chart.destroy());
        this.charts.clear();
    }
}

// 初始化仪表盘
window.ktclawDashboard = new KtClawDashboard();
