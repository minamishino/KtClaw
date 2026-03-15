package ktclaw.db

import io.r2dbc.pool.PoolingConnectionFactoryProvider
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOneOrNull
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.r2dbc.core.flow
import java.time.Duration

/**
 * KtClaw R2DBC Configuration
 * 支持响应式数据库访问 + Exposed ORM + R2DBC DatabaseClient
 */
class R2DBCConfig(
    private val host: String = System.getenv("DB_HOST") ?: "localhost",
    private val port: Int = System.getenv("DB_PORT")?.toIntOrNull() ?: 5432,
    private val database: String = System.getenv("DB_NAME") ?: "ktclaw",
    private val username: String = System.getenv("DB_USER") ?: "ktclaw",
    private val password: String = System.getenv("DB_PASSWORD") ?: "",
    private val initialSize: Int = 10,
    private val maxSize: Int = 50,
    private val maxIdleTimeMinutes: Long = 30,
    private val maxAcquireTimeSeconds: Long = 30
) {

    /**
     * R2DBC Connection Factory with Connection Pooling
     */
    val connectionFactory: ConnectionFactory by lazy {
        ConnectionFactories.get(
            ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "pool")
                .option(ConnectionFactoryOptions.PROTOCOL, "postgresql")
                .option(ConnectionFactoryOptions.HOST, host)
                .option(ConnectionFactoryOptions.PORT, port)
                .option(ConnectionFactoryOptions.DATABASE, database)
                .option(ConnectionFactoryOptions.USER, username)
                .option(ConnectionFactoryOptions.PASSWORD, password)
                // Connection Pool Settings
                .option(PoolingConnectionFactoryProvider.INITIAL_SIZE, initialSize)
                .option(PoolingConnectionFactoryProvider.MAX_SIZE, maxSize)
                .option(PoolingConnectionFactoryProvider.MAX_IDLE_TIME, Duration.ofMinutes(maxIdleTimeMinutes))
                .option(PoolingConnectionFactoryProvider.MAX_ACQUIRE_TIME, Duration.ofSeconds(maxAcquireTimeSeconds))
                .option(PoolingConnectionFactoryProvider.MAX_LIFE_TIME, Duration.ofMinutes(60))
                .option(PoolingConnectionFactoryProvider.VALIDATION_QUERY, "SELECT 1")
                // PostgreSQL specific
                .option(PostgresqlConnectionFactoryProvider.OPTIONS, mapOf(
                    "lock_timeout" to "10s",
                    "statement_timeout" to "30s"
                ))
                .build()
        )
    }

    /**
     * Spring Data R2DBC DatabaseClient
     */
    val databaseClient: DatabaseClient by lazy {
        DatabaseClient.create(connectionFactory)
    }
}

/**
 * R2DBC Repository Base Class with suspend functions and Flow support
 */
abstract class R2DBCRepository(private val databaseClient: DatabaseClient) {

    /**
     * 执行查询并返回单个结果
     */
    protected suspend inline fun <reified T : Any> fetchOne(sql: String, bind: Map<String, Any> = emptyMap()): T? {
        var spec = databaseClient.sql(sql)
        bind.forEach { (key, value) ->
            spec = spec.bind(key, value)
        }
        return spec.map { row, _ ->
            mapRowToType<T>(row)
        }.awaitOneOrNull()
    }

    /**
     * 执行查询并返回 Flow 流式结果
     */
    protected inline fun <reified T : Any> fetchMany(sql: String, bind: Map<String, Any> = emptyMap()): Flow<T> {
        var spec = databaseClient.sql(sql)
        bind.forEach { (key, value) ->
            spec = spec.bind(key, value)
        }
        return spec.map { row, _ ->
            mapRowToType<T>(row)
        }.flow()
    }

    /**
     * 执行更新操作
     */
    protected suspend fun executeUpdate(sql: String, bind: Map<String, Any> = emptyMap()): Int {
        var spec = databaseClient.sql(sql)
        bind.forEach { (key, value) ->
            spec = spec.bind(key, value)
        }
        return spec.fetch().awaitRowsUpdated()
    }

    /**
     * 执行插入并返回生成的 ID
     */
    protected suspend fun executeInsert(sql: String, bind: Map<String, Any> = emptyMap()): Long? {
        var spec = databaseClient.sql(sql)
        bind.forEach { (key, value) ->
            spec = spec.bind(key, value)
        }
        return spec.filter { statement ->
            statement.returnGeneratedValues("id")
        }.fetch().first()
            .map { row -> row.get("id", Long::class.java) }
            .awaitFirstOrNull()
    }

    /**
     * 执行批量插入
     */
    protected suspend fun executeBatch(sql: String, binds: List<Map<String, Any>>): Int {
        if (binds.isEmpty()) return 0

        var spec = databaseClient.sql(sql)
        binds.forEach { bind ->
            spec = spec.bind(bind)
        }
        return spec.fetch().awaitRowsUpdated()
    }

    /**
     * 执行原始 SQL
     */
    protected suspend fun execute(sql: String): Int {
        return databaseClient.sql(sql)
            .fetch()
            .awaitRowsUpdated()
    }

    /**
     * 类型映射方法 - 子类可以重写
     */
    protected abstract inline fun <reified T : Any> mapRowToType(row: org.springframework.r2dbc.core.Row): T
}

/**
 * R2DBC Query Helper Extensions
 */
object R2DBCExtensions {

    /**
     * 构建分页查询
     */
    fun buildPagedQuery(
        baseQuery: String,
        page: Int,
        size: Int,
        sortBy: String? = null,
        sortDirection: String = "DESC"
    ): String {
        val offset = page * size
        val orderClause = sortBy?.let { "ORDER BY $it $sortDirection" } ?: ""
        return "$baseQuery $orderClause LIMIT $size OFFSET $offset"
    }

    /**
     * 构建计数查询
     */
    fun buildCountQuery(baseQuery: String): String {
        return "SELECT COUNT(*) FROM ($baseQuery) AS count_query"
    }

    /**
     * 构建 IN 子句
     */
    fun buildInClause(field: String, values: List<Any>): Pair<String, Map<String, Any>> {
        if (values.isEmpty()) return "1=0" to emptyMap()

        val params = values.mapIndexed { index, value ->
            "${field}_$index" to value
        }.toMap()

        val placeholders = params.keys.joinToString(", ") { ":$it" }
        return "$field IN ($placeholders)" to params
    }

    /**
     * 构建动态 WHERE 子句
     */
    fun buildWhereClause(conditions: Map<String, Any?>): Pair<String, Map<String, Any>> {
        val nonNullConditions = conditions.filterValues { it != null }
        if (nonNullConditions.isEmpty()) return "" to emptyMap()

        val clause = nonNullConditions.keys.joinToString(" AND ") { "$it = :$it" }
        return "WHERE $clause" to nonNullConditions.mapValues { it.value!! }
    }
}

/**
 * R2DBC Transaction Helper
 */
class R2DBCTransactionManager(private val connectionFactory: ConnectionFactory) {

    /**
     * 在事务中执行操作
     */
    suspend fun <T> transactional(block: suspend () -> T): T {
        val connection = connectionFactory.create().awaitSingle()
        return try {
            connection.beginTransaction().awaitSingle()
            val result = block()
            connection.commitTransaction().awaitSingle()
            result
        } catch (e: Exception) {
            connection.rollbackTransaction().awaitSingle()
            throw e
        } finally {
            connection.close().awaitSingle()
        }
    }
}
