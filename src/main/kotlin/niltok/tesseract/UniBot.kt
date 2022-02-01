package niltok.tesseract

import com.elbekD.bot.types.Message
import io.lettuce.core.RedisClient
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.alsoLogin
import java.util.concurrent.Executors
import com.elbekD.bot.Bot as tgBot

const val envName = "env"

@Serializable
data class QQBotInfo(val id: Long, val pwd: String)

object UniBot {
    val redis = RedisClient.create("redis://localhost")!!
    val redisClient = redis.connect()!!
    val redisClientRaw = redis.connectRaw()

    val tgToken = db().get("core:tgToken")!!

    val qq = runBlocking {
        val row: List<QQBotInfo> = SJson.decodeFromString(db().get("core:qqInfo")!!)
        BotFactory.newBot(row[0].id, row[0].pwd) {
            fileBasedDeviceInfo()
            highwayUploadCoroutineCount = 5
        }.alsoLogin()
    }
    val tg = tgBot.createPolling("", tgToken)
    //val tgc = telegramBot(tgToken)
    val tgListener = mutableListOf<suspend com.elbekD.bot.Bot.(Message) -> Unit>()
    private val tgThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    init {
        tg.onMessage { msg -> tgListener.forEach { tg.it(msg) } }
    }

    suspend fun start() = coroutineScope {
        withContext(tgThread) { tg.start() }
        qq.join()
    }

    suspend fun send(target: IMChat, msg: List<UniMsgType>): List<IMMsgRef> =
        when (target) {
            is IMGroup.QQ -> qq.getGroupOrFail(target.id).send(msg)
            //is IMGroup.TG -> tgc.getChat(ChatId(target.id)).send(msg)
            else -> throw Throwable("Unknown IM")
        }
}
