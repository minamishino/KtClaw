# KtClaw 数据库性能优化指南

## 1. 索引策略

### 已创建的索引

| 表名 | 索引名 | 字段 | 用途 |
|------|--------|------|------|
| agents | idx_agents_active | is_active | 快速筛选活跃代理 |
| agents | idx_agents_provider | provider | 按提供商筛选 |
| channels | idx_channels_type | channel_type | 按类型筛选频道 |
| channels | idx_channels_active | is_active | 快速筛选活跃频道 |
| channels | idx_channels_agent | agent_id | 关联查询优化 |
| sessions | idx_sessions_user | user_id | 用户会话查询 |
| sessions | idx_sessions_channel | channel_id | 频道会话查询 |
| sessions | idx_sessions_active | is_active | 活跃会话筛选 |
| sessions | idx_sessions_last_msg | last_message_at | 按时间排序 |
| messages | idx_messages_session | session_id | 会话消息查询 |
| messages | idx_messages_channel | channel_id | 频道消息查询 |
| messages | idx_messages_created | created_at | 时间范围查询 |
| messages | idx_messages_type | type | 按类型筛选 |
| messages | idx_messages_status | status | 按状态筛选 |
| messages | idx_messages_content_trgm | content (gin_trgm_ops) | 全文搜索 |
| configs | idx_configs_scope | scope, scope_id | 配置层级查询 |
| configs | idx_configs_key | key | 配置键查询 |
| configs | idx_configs_active | is_active | 活跃配置筛选 |

## 2. 查询优化建议

### 2.1 消息查询优化

```kotlin
// ✅ 推荐：使用索引字段查询
Message.find { 
    (Messages.sessionId eq sessionId) and 
    (Messages.createdAt greaterEq startTime)
}.orderBy(Messages.createdAt to SortOrder.DESC)
 .limit(100)

// ❌ 避免：全表扫描
Message.find { Messages.content like "%keyword%" }
```

### 2.2 分页查询优化

```kotlin
// ✅ 推荐：使用游标分页（大数据量）
fun getMessagesCursor(sessionId: UUID, lastId: UUID?, limit: Int): List<Message> {
    val query = if (lastId != null) {
        Messages.select { 
            (Messages.sessionId eq sessionId) and 
            (Messages.id less lastId) 
        }
    } else {
        Messages.select { Messages.sessionId eq sessionId }
    }
    return query.orderBy(Messages.id to SortOrder.DESC)
                .limit(limit)
                .map { Message.wrapRow(it) }
}
```

### 2.3 批量操作

```kotlin
// ✅ 推荐：批量插入
Messages.batchInsert(messageList) { message ->
    // 批量插入配置
}

// ✅ 推荐：批量更新
Messages.update({ Messages.id inList ids }) {
    it[status] = MessageStatus.DELETED
}
```

## 3. 分区策略（大数据量）

当消息表超过 1000 万条时，建议启用分区：

```sql
-- 创建按月分区的消息表
CREATE TABLE messages_partitioned (
    LIKE messages INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE messages_y2026m03 PARTITION OF messages_partitioned
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE TABLE messages_y2026m04 PARTITION OF messages_partitioned
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- 自动创建未来分区（使用 cron 或应用逻辑）
```

## 4. 连接池配置

### R2DBC 连接池优化

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 10          # 初始连接数
      max-size: 50              # 最大连接数（根据 CPU 核心数调整）
      max-idle-time: 30m        # 最大空闲时间
      max-acquire-time: 30s     # 获取连接超时
      max-life-time: 60m        # 连接最大生命周期
```

### 计算公式

```
最大连接数 = (CPU 核心数 × 2) + 有效磁盘数

例如：4 核心 CPU + SSD = (4 × 2) + 1 = 9 ~ 10
```

## 5. 数据清理策略

### 5.1 自动清理过期消息

```kotlin
// 每天执行一次
cron("0 0 2 * * *") // 每天凌晨 2 点
fun cleanupOldMessages() {
    val retentionDays = configService.getInt("message.retention_days", 90)
    val cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS)
    
    val deletedCount = Messages.deleteWhere {
        Messages.createdAt less cutoffTime
    }
    logger.info("Cleaned up $deletedCount old messages")
}
```

### 5.2 归档策略

```kotlin
// 将旧数据归档到冷存储
fun archiveOldMessages() {
    val archiveTime = Instant.now().minus(30, ChronoUnit.DAYS)
    
    // 1. 导出到对象存储（S3/MinIO）
    val oldMessages = Messages.select { Messages.createdAt less archiveTime }
    
    // 2. 压缩并上传
    // 3. 删除已归档数据
}
```

## 6. 监控指标

### 6.1 关键指标

| 指标 | 健康阈值 | 说明 |
|------|----------|------|
| 查询响应时间 | < 100ms | P95 响应时间 |
| 连接池使用率 | < 80% | 活跃连接/最大连接 |
| 慢查询数量 | < 10/min | 执行时间 > 1s |
| 磁盘使用率 | < 70% | 数据目录 |

### 6.2 慢查询监控

```sql
-- 启用慢查询日志
ALTER SYSTEM SET log_min_duration_statement = '1000'; -- 1秒
ALTER SYSTEM SET log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h ';
SELECT pg_reload_conf();
```

## 7. 性能测试建议

### 7.1 压力测试脚本

```bash
# 使用 pgbench 进行压力测试
pgbench -h localhost -U ktclaw -d ktclaw \
    -f test_query.sql \
    -c 50 -j 10 -T 60
```

### 7.2 测试查询文件

```sql
-- test_query.sql
\set session_id random_uuid()
SELECT * FROM messages 
WHERE session_id = :'session_id' 
ORDER BY created_at DESC 
LIMIT 100;
```

## 8. 扩展建议

### 8.1 读写分离

```yaml
# 主库（写）
spring.r2dbc.master.url: r2dbc:postgresql://master:5432/ktclaw

# 从库（读）
spring.r2dbc.replica.url: r2dbc:postgresql://replica:5432/ktclaw
```

### 8.2 缓存策略

```kotlin
// 使用 Redis 缓存热点数据
@Cacheable(value = ["session"], key = "#sessionKey")
suspend fun getSession(sessionKey: String): SessionDTO? {
    return sessionRepository.findBySessionKey(sessionKey)
}
```

## 9. 故障排查

### 9.1 连接池耗尽

```sql
-- 查看当前连接
SELECT * FROM pg_stat_activity 
WHERE datname = 'ktclaw';

-- 查看连接状态
SELECT state, count(*) 
FROM pg_stat_activity 
WHERE datname = 'ktclaw' 
GROUP BY state;
```

### 9.2 锁等待

```sql
-- 查看锁等待
SELECT * FROM pg_locks 
WHERE NOT granted;

-- 查看阻塞查询
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```
