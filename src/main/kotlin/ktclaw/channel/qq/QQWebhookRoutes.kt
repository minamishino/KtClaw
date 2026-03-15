package ktclaw.channel.qq

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * QQ Bot Webhook 路由配置
 */
fun Route.qqWebhookRoutes(
    appId: String,
    appSecret: String,
    messageHandler: suspend (QQWebhookPayload) -> Unit
) {
    val logger = LoggerFactory.getLogger("QQWebhookRoutes")
    val webhookHandler = QQWebhookHandler(appId, appSecret, messageHandler)

    route("/webhook/qq") {
        // GET /webhook/qq - 验证接口
        get {
            logger.info("Received verification request from QQ")
            webhookHandler.handleVerification(call)
        }

        // POST /webhook/qq - 接收事件推送
        post {
            logger.debug("Received webhook event from QQ")
            webhookHandler.handleWebhook(call)
        }
    }
}

/**
 * 配置 QQ Webhook 路由的扩展函数
 */
fun Application.configureQQWebhook(
    appId: String,
    appSecret: String,
    messageHandler: suspend (QQWebhookPayload) -> Unit
) {
    routing {
        qqWebhookRoutes(appId, appSecret, messageHandler)
    }
}
