package ktclaw.db

import io.r2dbc.pool.PoolingConnectionFactoryProvider
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory

/**
 * R2DBC 配置 - 纯 Kotlin 实现，不使用 Spring
 */
class R2DBCConfig(
    private val host: String = "localhost",
    private val port: Int = 5432,
    private val database: String = "ktclaw",
    private val username: String = "ktclaw",
    private val password: String = "ktclaw"
) {
    private val logger = LoggerFactory.getLogger(R2DBCConfig::class.java)

    /**
     * 创建连接工厂
     */
    fun connectionFactory(): ConnectionFactory {
        logger.info("Initializing R2DBC connection pool for PostgreSQL at $host:$port/$database")

        return ConnectionFactories.get(
            ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "pool")
                .option(ConnectionFactoryOptions.PROTOCOL, "postgresql")
                .option(ConnectionFactoryOptions.HOST, host)
                .option(ConnectionFactoryOptions.PORT, port)
                .option(ConnectionFactoryOptions.DATABASE, database)
                .option(ConnectionFactoryOptions.USER, username)
                .option(ConnectionFactoryOptions.PASSWORD, password)
                .option(PoolingConnectionFactoryProvider.MAX_SIZE, 20)
                .option(PoolingConnectionFactoryProvider.INITIAL_SIZE, 5)
                .option(PoolingConnectionFactoryProvider.MAX_IDLE_TIME, java.time.Duration.ofMinutes(10))
                .option(PoolingConnectionFactoryProvider.MAX_LIFE_TIME, java.time.Duration.ofMinutes(30))
                .build()
        )
    }

    companion object {
        /**
         * 从配置创建 R2DBCConfig
         */
        fun fromConfig(config: DatabaseConfig): R2DBCConfig {
            return R2DBCConfig(
                host = config.host,
                port = config.port,
                database = config.database,
                username = config.username,
                password = config.password
            )
        }
    }
}

/**
 * 数据库配置数据类
 */
data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 5432,
    val database: String = "ktclaw",
    val username: String = "ktclaw",
    val password: String = "ktclaw"
)

/**
 * R2DBC 数据库客户端 - 纯 Kotlin 实现
 */
class R2DBCDatabaseClient(private val connectionFactory: ConnectionFactory) {
    private val logger = LoggerFactory.getLogger(R2DBCDatabaseClient::class.java)

    /**
     * 执行查询并返回单个结果
     */
    suspend fun <T> fetchOne(sql: String, mapper: (io.r2dbc.spi.Row) -> T): T? {
        return connectionFactory.create()
            .flatMapMany { connection ->
                connection.createStatement(sql)
                    .execute()
                    .flatMap { result ->
                        result.map { row, _ -> mapper(row) }
                    }
            }
            .awaitFirstOrNull()
    }

    /**
     * 执行查询并返回 Flow
     */
    fun <T> fetchMany(sql: String, mapper: (io.r2dbc.spi.Row) -> T): Flow<T> {
        return connectionFactory.create()
            .flatMapMany { connection ->
                connection.createStatement(sql)
                    .execute()
                    .flatMap { result ->
                        result.map { row, _ -> mapper(row) }
                    }
            }
            .asFlow()
    }

    /**
     * 执行更新操作
     */
    suspend fun execute(sql: String): Int {
        return connectionFactory.create()
            .flatMap { connection ->
                connection.createStatement(sql)
                    .execute()
                    .flatMap { result ->
                        result.rowsUpdated
                    }
            }
            .awaitSingle()
    }

    /**
     * 在事务中执行
     */
    suspend fun <T> inTransaction(block: suspend (R2DBCDatabaseClient) -> T): T {
        val connection = connectionFactory.create().awaitSingle()
        return try {
            connection.beginTransaction().awaitSingle()
            val result = block(R2DBCDatabaseClient(connectionFactory))
            connection.commitTransaction().awaitSingle()
            result
        } catch (e: Exception) {
            connection.rollbackTransaction().awaitSingle()
            throw e
        } finally {
            connection.close()
        }
    }
}

/**
 * 扩展函数：将 Reactive Streams Publisher 转换为 Flow
 */
fun <T : Any> org.reactivestreams.Publisher<T>.asFlow(): Flow<T> =
    kotlinx.coroutines.reactive.asFlow(this)
