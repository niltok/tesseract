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
import net.mamoe.mirai.utils.BotConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.concurrent.Executors
import com.elbekD.bot.Bot as tgBot

const val envName = "env"

@Serializable
data class QQBotInfo(val id: Long, val pwd: String)

object UniBot {
    val env = File(envName).readLines()

    init {
        Database.connect(env[0], "com.impossibl.postgres.jdbc.PGDriver", env[1], env[2])
        transaction {
            SchemaUtils.create(Configs, ConnectInfo, MarkovData, MarkovConfig, PicData, PicIndex, Superusers, Histories)
        }
    }

    val tgToken = kvget("core:tgToken")!!

    val qq = runBlocking {
        val row: List<QQBotInfo> = SJson.decodeFromString(kvget("core:qqInfo")!!)
        BotFactory.newBot(row[0].id, row[0].pwd) {
            fileBasedDeviceInfo()
            highwayUploadCoroutineCount = 5
            protocol = BotConfiguration.MiraiProtocol.MACOS
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

}
