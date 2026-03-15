-- KtClaw Database Schema - Flyway Migration V1
-- PostgreSQL 15+
-- Created: 2026-03-15

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ============================================
-- 1. AGENTS - 代理配置表
-- ============================================
CREATE TABLE IF NOT EXISTS agents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    model VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    api_key_encrypted TEXT,
    system_prompt TEXT,
    temperature DECIMAL(3,2) DEFAULT 0.7 CHECK (temperature >= 0 AND temperature <= 2),
    max_tokens INTEGER DEFAULT 2048,
    timeout_ms INTEGER DEFAULT 30000,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_agents_active ON agents(is_active);
CREATE INDEX idx_agents_provider ON agents(provider);

-- ============================================
-- 2. CHANNELS - 频道配置表
-- ============================================
CREATE TABLE IF NOT EXISTS channels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    channel_id VARCHAR(100) NOT NULL UNIQUE,
    channel_type VARCHAR(50) NOT NULL,
    name VARCHAR(200),
    description TEXT,
    agent_id UUID REFERENCES agents(id) ON DELETE SET NULL,
    is_active BOOLEAN DEFAULT TRUE,
    config JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_channels_type ON channels(channel_type);
CREATE INDEX idx_channels_active ON channels(is_active);
CREATE INDEX idx_channels_agent ON channels(agent_id);

-- ============================================
-- 3. SESSIONS - 会话管理表
-- ============================================
CREATE TABLE IF NOT EXISTS sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_key VARCHAR(200) NOT NULL UNIQUE,
    user_id VARCHAR(100) NOT NULL,
    channel_id UUID REFERENCES channels(id) ON DELETE CASCADE,
    agent_id UUID REFERENCES agents(id) ON DELETE SET NULL,
    title VARCHAR(200),
    context_window INTEGER DEFAULT 10,
    last_message_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_sessions_user ON sessions(user_id);
CREATE INDEX idx_sessions_channel ON sessions(channel_id);
CREATE INDEX idx_sessions_active ON sessions(is_active);
CREATE INDEX idx_sessions_last_msg ON sessions(last_message_at);

-- ============================================
-- 4. MESSAGES - 消息记录表
-- ============================================
CREATE TYPE message_type AS ENUM ('text', 'image', 'voice', 'video', 'file', 'location', 'system');
CREATE TYPE message_role AS ENUM ('user', 'assistant', 'system');
CREATE TYPE message_status AS ENUM ('pending', 'sent', 'delivered', 'failed', 'deleted');

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID REFERENCES sessions(id) ON DELETE CASCADE,
    channel_id UUID REFERENCES channels(id) ON DELETE CASCADE,
    message_id VARCHAR(100),
    reply_to_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    role message_role NOT NULL,
    type message_type NOT NULL DEFAULT 'text',
    content TEXT NOT NULL,
    content_encrypted BOOLEAN DEFAULT FALSE,
    media_url TEXT,
    media_size BIGINT,
    media_mime_type VARCHAR(100),
    status message_status DEFAULT 'sent',
    tokens_used INTEGER,
    latency_ms INTEGER,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_messages_session ON messages(session_id);
CREATE INDEX idx_messages_channel ON messages(channel_id);
CREATE INDEX idx_messages_created ON messages(created_at);
CREATE INDEX idx_messages_type ON messages(type);
CREATE INDEX idx_messages_status ON messages(status);
CREATE INDEX idx_messages_content_trgm ON messages USING gin(content gin_trgm_ops);

-- ============================================
-- 5. CONFIGS - 系统配置表
-- ============================================
CREATE TYPE config_scope AS ENUM ('global', 'channel', 'user', 'session');

CREATE TABLE IF NOT EXISTS configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scope config_scope NOT NULL DEFAULT 'global',
    scope_id VARCHAR(100),
    key VARCHAR(200) NOT NULL,
    value TEXT NOT NULL,
    value_type VARCHAR(20) DEFAULT 'string',
    description TEXT,
    is_encrypted BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(scope, scope_id, key)
);

CREATE INDEX idx_configs_scope ON configs(scope, scope_id);
CREATE INDEX idx_configs_key ON configs(key);
CREATE INDEX idx_configs_active ON configs(is_active);

-- ============================================
-- 触发器: 自动更新 updated_at
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_agents_updated_at BEFORE UPDATE ON agents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_channels_updated_at BEFORE UPDATE ON channels
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sessions_updated_at BEFORE UPDATE ON sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_messages_updated_at BEFORE UPDATE ON messages
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_configs_updated_at BEFORE UPDATE ON configs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 初始数据
-- ============================================
INSERT INTO configs (scope, key, value, value_type, description) VALUES
    ('global', 'app.name', 'KtClaw', 'string', '应用名称'),
    ('global', 'app.version', '0.1.0', 'string', '应用版本'),
    ('global', 'message.max_length', '4000', 'int', '单条消息最大长度'),
    ('global', 'message.retention_days', '90', 'int', '消息保留天数'),
    ('global', 'session.max_context', '20', 'int', '最大上下文消息数'),
    ('global', 'session.timeout_minutes', '30', 'int', '会话超时时间(分钟)')
ON CONFLICT (scope, scope_id, key) DO NOTHING;
