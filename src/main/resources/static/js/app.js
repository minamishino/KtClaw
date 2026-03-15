// app.js - WebSocket 客户端与核心交互逻辑
class KtClawApp {
    constructor() {
        this.ws = null;
        this.wsUrl = `ws://${window.location.host}/ws`;
        this.messageContainer = document.getElementById('message-container');
        this.alertContainer = document.getElementById('alert-container');
        this.init();
    }

    init() {
        this.connectWebSocket();
        this.initEventListeners();
        this.initFormValidation();
    }

    // WebSocket 连接管理
    connectWebSocket() {
        try {
            this.ws = new WebSocket(this.wsUrl);
            
            this.ws.onopen = () => {
                console.log('WebSocket 连接成功');
                this.showAlert('连接已建立', 'success');
            };

            this.ws.onmessage = (event) => {
                const message = JSON.parse(event.data);
                this.handleMessage(message);
            };

            this.ws.onerror = (error) => {
                console.error('WebSocket 错误:', error);
                this.showAlert('连接错误，正在尝试重连...', 'danger');
            };

            this.ws.onclose = () => {
                console.log('WebSocket 连接关闭');
                this.showAlert('连接已断开，正在尝试重连...', 'warning');
                // 3秒后自动重连
                setTimeout(() => this.connectWebSocket(), 3000);
            };
        } catch (error) {
            console.error('WebSocket 连接失败:', error);
            setTimeout(() => this.connectWebSocket(), 5000);
        }
    }

    // 处理WebSocket消息
    handleMessage(message) {
        switch (message.type) {
            case 'status_update':
                this.updateStatus(message.data);
                break;
            case 'alert':
                this.showAlert(message.content, message.level || 'info');
                break;
            case 'log':
                this.addLogMessage(message.content, message.level);
                break;
            case 'data_update':
                // 触发数据更新事件，供dashboard.js监听
                window.dispatchEvent(new CustomEvent('dataUpdate', { detail: message.data }));
                break;
            default:
                console.log('未知消息类型:', message);
        }
    }

    // 发送WebSocket消息
    sendMessage(type, data) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({ type, data }));
            return true;
        } else {
            this.showAlert('连接未就绪，请稍后重试', 'warning');
            return false;
        }
    }

    // 状态更新
    updateStatus(status) {
        const statusElement = document.getElementById('system-status');
        if (statusElement) {
            statusElement.textContent = status.status;
            statusElement.className = `badge bg-${status.online ? 'success' : 'danger'}`;
        }
    }

    // 显示提示消息
    showAlert(message, type = 'info', duration = 5000) {
        if (!this.alertContainer) return;

        const alertId = 'alert-' + Date.now();
        const alertHtml = `
            <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;
        
        this.alertContainer.insertAdjacentHTML('beforeend', alertHtml);

        // 自动关闭
        if (duration > 0) {
            setTimeout(() => {
                const alert = document.getElementById(alertId);
                if (alert) {
                    const bsAlert = new bootstrap.Alert(alert);
                    bsAlert.close();
                }
            }, duration);
        }
    }

    // 添加日志消息
    addLogMessage(content, level = 'info') {
        if (!this.messageContainer) return;

        const logElement = document.createElement('div');
        logElement.className = `log-item log-${level}`;
        logElement.innerHTML = `
            <span class="log-time">${new Date().toLocaleTimeString()}</span>
            <span class="log-content">${content}</span>
        `;
        
        this.messageContainer.prepend(logElement);
        
        // 限制最多显示100条日志
        while (this.messageContainer.children.length > 100) {
            this.messageContainer.removeChild(this.messageContainer.lastChild);
        }
    }

    // 初始化事件监听器
    initEventListeners() {
        // 表单提交事件
        document.querySelectorAll('form[data-ajax]').forEach(form => {
            form.addEventListener('submit', (e) => this.handleFormSubmit(e));
        });

        // 按钮点击事件
        document.querySelectorAll('[data-action]').forEach(button => {
            button.addEventListener('click', (e) => this.handleAction(e));
        });
    }

    // 表单验证
    initFormValidation() {
        // 自定义验证规则
        const validators = {
            required: (value) => value.trim() !== '',
            email: (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value),
            number: (value) => !isNaN(parseFloat(value)) && isFinite(value),
            minLength: (value, min) => value.length >= min,
            maxLength: (value, max) => value.length <= max
        };

        // 实时验证
        document.querySelectorAll('input, select, textarea').forEach(input => {
            input.addEventListener('blur', () => this.validateField(input));
            input.addEventListener('input', () => {
                if (input.classList.contains('is-invalid')) {
                    this.validateField(input);
                }
            });
        });

        this.validators = validators;
    }

    // 验证单个字段
    validateField(field) {
        const rules = field.dataset.validate ? field.dataset.validate.split('|') : [];
        let isValid = true;
        let errorMessage = '';

        for (const rule of rules) {
            const [ruleName, ruleValue] = rule.split(':');
            const value = field.value;

            if (this.validators[ruleName]) {
                if (!this.validators[ruleName](value, ruleValue)) {
                    isValid = false;
                    errorMessage = field.dataset[`${ruleName}Message`] || `字段验证失败: ${ruleName}`;
                    break;
                }
            }
        }

        if (isValid) {
            field.classList.remove('is-invalid');
            field.classList.add('is-valid');
            const feedback = field.parentElement.querySelector('.invalid-feedback');
            if (feedback) feedback.remove();
        } else {
            field.classList.remove('is-valid');
            field.classList.add('is-invalid');
            
            // 显示错误信息
            let feedback = field.parentElement.querySelector('.invalid-feedback');
            if (!feedback) {
                feedback = document.createElement('div');
                feedback.className = 'invalid-feedback';
                field.parentElement.appendChild(feedback);
            }
            feedback.textContent = errorMessage;
        }

        return isValid;
    }

    // 验证整个表单
    validateForm(form) {
        let isValid = true;
        const fields = form.querySelectorAll('input, select, textarea');
        
        fields.forEach(field => {
            if (!this.validateField(field)) {
                isValid = false;
            }
        });

        return isValid;
    }

    // 处理表单提交
    async handleFormSubmit(event) {
        event.preventDefault();
        const form = event.target;
        
        if (!this.validateForm(form)) {
            this.showAlert('请检查表单中的错误', 'danger');
            return;
        }

        const formData = new FormData(form);
        const action = form.action || window.location.href;
        const method = form.method || 'POST';
        const submitButton = form.querySelector('button[type="submit"]');
        
        // 显示加载状态
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.dataset.originalText = submitButton.textContent;
            submitButton.textContent = '提交中...';
        }

        try {
            const response = await fetch(action, {
                method: method.toUpperCase(),
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            const result = await response.json();

            if (response.ok) {
                this.showAlert(result.message || '操作成功', 'success');
                
                // 重置表单
                if (form.dataset.reset === 'true') {
                    form.reset();
                    form.querySelectorAll('.is-valid, .is-invalid').forEach(field => {
                        field.classList.remove('is-valid', 'is-invalid');
                    });
                }

                // 触发成功事件
                window.dispatchEvent(new CustomEvent('formSuccess', { 
                    detail: { form, result } 
                }));

                // 跳转或刷新
                if (form.dataset.redirect) {
                    window.location.href = form.dataset.redirect;
                } else if (form.dataset.reload === 'true') {
                    window.location.reload();
                }
            } else {
                this.showAlert(result.message || '操作失败', 'danger');
                
                // 显示字段级错误
                if (result.errors) {
                    Object.keys(result.errors).forEach(fieldName => {
                        const field = form.querySelector(`[name="${fieldName}"]`);
                        if (field) {
                            field.classList.add('is-invalid');
                            let feedback = field.parentElement.querySelector('.invalid-feedback');
                            if (!feedback) {
                                feedback = document.createElement('div');
                                feedback.className = 'invalid-feedback';
                                field.parentElement.appendChild(feedback);
                            }
                            feedback.textContent = result.errors[fieldName];
                        }
                    });
                }
            }
        } catch (error) {
            console.error('表单提交错误:', error);
            this.showAlert('网络错误，请稍后重试', 'danger');
        } finally {
            // 恢复按钮状态
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = submitButton.dataset.originalText;
            }
        }
    }

    // 处理按钮动作
    async handleAction(event) {
        const button = event.currentTarget;
        const action = button.dataset.action;
        const url = button.dataset.url;
        const confirmMessage = button.dataset.confirm;

        // 确认提示
        if (confirmMessage && !confirm(confirmMessage)) {
            return;
        }

        // 显示加载状态
        button.disabled = true;
        const originalText = button.innerHTML;
        button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> 处理中...';

        try {
            switch (action) {
                case 'ajax-get':
                    await this.ajaxRequest(url, 'GET');
                    break;
                case 'ajax-post':
                    await this.ajaxRequest(url, 'POST', JSON.parse(button.dataset.data || '{}'));
                    break;
                case 'send-ws':
                    this.sendMessage(button.dataset.wsType, JSON.parse(button.dataset.wsData || '{}'));
                    break;
                case 'reload':
                    window.location.reload();
                    break;
                default:
                    console.log('未知动作:', action);
            }
        } catch (error) {
            console.error('动作执行错误:', error);
            this.showAlert('操作失败，请稍后重试', 'danger');
        } finally {
            // 恢复按钮状态
            button.disabled = false;
            button.innerHTML = originalText;
        }
    }

    // 通用AJAX请求
    async ajaxRequest(url, method = 'GET', data = null) {
        const options = {
            method: method.toUpperCase(),
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Content-Type': 'application/json'
            }
        };

        if (data && method !== 'GET') {
            options.body = JSON.stringify(data);
        }

        const response = await fetch(url, options);
        const result = await response.json();

        if (response.ok) {
            if (result.message) {
                this.showAlert(result.message, 'success');
            }
            
            // 触发数据更新
            if (result.data) {
                window.dispatchEvent(new CustomEvent('dataUpdate', { detail: result.data }));
            }

            // 跳转或刷新
            if (result.redirect) {
                window.location.href = result.redirect;
            } else if (result.reload) {
                window.location.reload();
            }
        } else {
            this.showAlert(result.message || '请求失败', 'danger');
            throw new Error(result.message || '请求失败');
        }

        return result;
    }
}

// 初始化应用
document.addEventListener('DOMContentLoaded', () => {
    window.ktclaw = new KtClawApp();
});
