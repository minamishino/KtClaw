package ktclaw.db

import io.r2dbc.pool.PoolingConnectionFactoryProvider
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.reactive.awaitSingle
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider
import java.time.Duration

/**
 * KtClaw R2DBC Configuration
 * 支持响应式数据库访问 + Exposed ORM
 */
@Configuration
@EnableR2dbcRepositories(basePackages = ["ktclaw.db.repository"])
class R2DBCConfig : AbstractR2dbcConfiguration() {

    @Value("\${spring.r2dbc.host:localhost}")
    private lateinit var host: String

    @Value("\${spring.r2dbc.port:5432}")
    private var port: Int = 5432

    @Value("\${spring.r2dbc.database:ktclaw}")
    private lateinit var database: String

    @Value("\${spring.r2dbc.username:ktclaw}")
    private lateinit var username: String

    @Value("\${spring.r2dbc.password:}")
    private lateinit var password: String

    @Value("\${spring.r2dbc.pool.initial-size:10}")
    private var initialSize: Int = 10

    @Value("\${spring.r2dbc.pool.max-size:50}")
    private var maxSize: Int = 50

    @Value("\${spring.r2dbc.pool.max-idle-time:30}")
    private var maxIdleTimeMinutes: Long = 30

    @Value("\${spring.r2dbc.pool.max-acquire-time:30}")
    private var maxAcquireTimeSeconds: Long = 30

    /**
     * R2DBC Connection Factory with Connection Pooling
     */
    @Bean
    override fun connectionFactory(): ConnectionFactory {
        return ConnectionFactories.get(
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
     * Exposed Database Configuration
     * 用于同步/阻塞式操作（如 Flyway 迁移）
     */
    @Bean
    fun exposedDatabase(): Database {
        return Database.connect(
            url = "jdbc:postgresql://$host:$port/$database",
            driver = "org.postgresql.Driver",
            user = username,
            password = password
        )
    }

    /**
     * Database initializer for schema.sql
     */
    @Bean
    fun databaseInitializer(): DatabaseInitializer {
        return DatabaseInitializer(connectionFactory())
    }
}

/**
 * Database Initializer
 * 用于初始化数据库 schema
 */
class DatabaseInitializer(private val connectionFactory: ConnectionFactory) {

    /**
     * 初始化数据库（如果不使用 Flyway）
     */
    suspend fun initialize() {
        val populator = ResourceDatabasePopulator(
            ClassPathResource("schema.sql")
        )
        populator.populate(connectionFactory).awaitSingle()
    }

    /**
     * 执行自定义 SQL 脚本
     */
    suspend fun executeScript(resourcePath: String) {
        val populator = ResourceDatabasePopulator(
            ClassPathResource(resourcePath)
        )
        populator.populate(connectionFactory).awaitSingle()
    }
}

/**
 * R2DBC Transaction Helper
 * 用于在协程中进行数据库事务操作
 */
object R2DBCTransaction {

    /**
     * 在事务中执行 Exposed 操作
     */
    suspend fun <T> transactional(block: suspend () -> T): T {
        return newSuspendedTransaction {
            block()
        }
    }

    /**
     * 在事务中执行 R2DBC 操作
     */
    suspend fun <T> connectionTransactional(
        connectionFactory: ConnectionFactory,
        block: suspend (io.r2dbc.spi.Connection) -> T
    ): T {
        val connection = connectionFactory.create().awaitSingle()
        return try {
            connection.beginTransaction().awaitSingle()
            val result = block(connection)
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
}
