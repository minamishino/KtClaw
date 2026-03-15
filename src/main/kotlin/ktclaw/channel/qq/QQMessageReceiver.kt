package ktclaw.channel.qq

import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

/**
 * QQ 消息接收器
 * 处理 WebSocket 接收到的消息和事件
 */
class QQMessageReceiver(private val client: QQBotClient) {
    private val logger = LoggerFactory.getLogger(QQMessageReceiver::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // 消息通道
    private val messageChannel = Channel<QQMessage>(Channel.BUFFERED)
    private val eventChannel = Channel<QQEvent>(Channel.BUFFERED)

    /**
     * 消息流
     */
    val messages: Flow<QQMessage> = messageChannel.consumeAsFlow()

    /**
     * 事件流
     */
    val events: Flow<QQEvent> = eventChannel.consumeAsFlow()

    /**
     * 处理 WebSocket 帧
     */
    suspend fun handleFrame(frame: Frame) {
        when (frame) {
            is Frame.Text -> handleTextFrame(frame.readText())
            is Frame.Binary -> handleBinaryFrame(frame.readBytes())
            is Frame.Close -> handleCloseFrame(frame)
            else -> logger.debug("Received frame type: ${frame.frameType}")
        }
    }

    /**
     * 处理文本帧
     */
    private suspend fun handleTextFrame(text: String) {
        try {
            val jsonObject = json.parseToJsonElement(text).jsonObject
            val opCode = jsonObject["op"]?.jsonPrimitive?.intOrNull

            when (opCode) {
                0 -> handleDispatchEvent(jsonObject) // Dispatch Event
                11 -> handleHeartbeatAck() // Heartbeat ACK
                else -> logger.debug("Received opcode: $opCode")
            }
        } catch (e: Exception) {
            logger.error("Failed to parse message: $text", e)
        }
    }

    /**
     * 处理分发事件
     */
    private suspend fun handleDispatchEvent(jsonObject: JsonObject) {
        val eventType = jsonObject["t"]?.jsonPrimitive?.contentOrNull
        val sequence = jsonObject["s"]?.jsonPrimitive?.intOrNull
        val data = jsonObject["d"]?.jsonObject

        logger.debug("Received event: $eventType, sequence: $sequence")

        when (eventType) {
            // 频道消息事件
            "MESSAGE_CREATE" -> handleMessageCreate(data)
            "MESSAGE_DELETE" -> handleMessageDelete(data)
            "AT_MESSAGE_CREATE" -> handleAtMessageCreate(data)

            // 私聊消息事件
            "DIRECT_MESSAGE_CREATE" -> handleDirectMessageCreate(data)

            // 群消息事件 (C2C)
            "C2C_MESSAGE_CREATE" -> handleC2CMessageCreate(data)
            "GROUP_AT_MESSAGE_CREATE" -> handleGroupAtMessageCreate(data)

            // 成员事件
            "GUILD_MEMBER_ADD" -> handleGuildMemberAdd(data)
            "GUILD_MEMBER_REMOVE" -> handleGuildMemberRemove(data)

            // 频道事件
            "CHANNEL_CREATE" -> handleChannelCreate(data)
            "CHANNEL_DELETE" -> handleChannelDelete(data)

            // 机器人事件
            "READY" -> handleReady(data)
            "RESUMED" -> handleResumed()

            else -> {
                logger.debug("Unhandled event type: $eventType")
                eventChannel.send(QQEvent.Unknown(eventType, data))
            }
        }
    }

    /**
     * 处理消息创建事件
     */
    private suspend fun handleMessageCreate(data: JsonObject?) {
        data?.let {
            try {
                val message = json.decodeFromJsonElement<QQMessage>(it)
                messageChannel.send(message)
                logger.info("Received message from ${message.author.username}: ${message.content}")
            } catch (e: Exception) {
                logger.error("Failed to parse message", e)
            }
        }
    }

    /**
     * 处理消息删除事件
     */
    private suspend fun handleMessageDelete(data: JsonObject?) {
        data?.let {
            val messageId = it["id"]?.jsonPrimitive?.contentOrNull
            val channelId = it["channel_id"]?.jsonPrimitive?.contentOrNull
            logger.info("Message deleted: $messageId in channel $channelId")
            eventChannel.send(QQEvent.MessageDeleted(messageId, channelId))
        }
    }

    /**
     * 处理 @机器人消息事件
     */
    private suspend fun handleAtMessageCreate(data: JsonObject?) {
        data?.let {
            try {
                val message = json.decodeFromJsonElement<QQMessage>(it)
                messageChannel.send(message.copy(mentionsMe = true))
                logger.info("Received @ message from ${message.author.username}: ${message.content}")
            } catch (e: Exception) {
                logger.error("Failed to parse @ message", e)
            }
        }
    }

    /**
     * 处理私聊消息事件
     */
    private suspend fun handleDirectMessageCreate(data: JsonObject?) {
        data?.let {
            try {
                val message = json.decodeFromJsonElement<QQMessage>(it)
                messageChannel.send(message.copy(isDirect = true))
                logger.info("Received direct message from ${message.author.username}: ${message.content}")
            } catch (e: Exception) {
                logger.error("Failed to parse direct message", e)
            }
        }
    }

    /**
     * 处理 C2C 消息事件
     */
    private suspend fun handleC2CMessageCreate(data: JsonObject?) {
        data?.let {
            try {
                val message = json.decodeFromJsonElement<QQMessage>(it)
                messageChannel.send(message.copy(isC2C = true))
                logger.info("Received C2C message: ${message.content}")
            } catch (e: Exception) {
                logger.error("Failed to parse C2C message", e)
            }
        }
    }

    /**
     * 处理群 @消息事件
     */
    private suspend fun handleGroupAtMessageCreate(data: JsonObject?) {
        data?.let {
            try {
                val message = json.decodeFromJsonElement<QQMessage>(it)
                messageChannel.send(message.copy(isGroup = true, mentionsMe = true))
                logger.info("Received group @ message: ${message.content}")
            } catch (e: Exception) {
                logger.error("Failed to parse group @ message", e)
            }
        }
    }

    /**
     * 处理成员加入事件
     */
    private suspend fun handleGuildMemberAdd(data: JsonObject?) {
        data?.let {
            val user = it["user"]?.jsonObject
            val guildId = it["guild_id"]?.jsonPrimitive?.contentOrNull
            val username = user?.get("username")?.jsonPrimitive?.contentOrNull
            logger.info("Member joined: $username in guild $guildId")
            eventChannel.send(QQEvent.MemberJoined(username, guildId))
        }
    }

    /**
     * 处理成员离开事件
     */
    private suspend fun handleGuildMemberRemove(data: JsonObject?) {
        data?.let {
            val user = it["user"]?.jsonObject
            val guildId = it["guild_id"]?.jsonPrimitive?.contentOrNull
            val username = user?.get("username")?.jsonPrimitive?.contentOrNull
            logger.info("Member left: $username from guild $guildId")
            eventChannel.send(QQEvent.MemberLeft(username, guildId))
        }
    }

    /**
     * 处理频道创建事件
     */
    private suspend fun handleChannelCreate(data: JsonObject?) {
        data?.let {
            val channelId = it["id"]?.jsonPrimitive?.contentOrNull
            val channelName = it["name"]?.jsonPrimitive?.contentOrNull
            logger.info("Channel created: $channelName ($channelId)")
            eventChannel.send(QQEvent.ChannelCreated(channelId, channelName))
        }
    }

    /**
     * 处理频道删除事件
     */
    private suspend fun handleChannelDelete(data: JsonObject?) {
        data?.let {
            val