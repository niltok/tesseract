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
import net.mamoe.mirai.internal.utils.*

const val envName = "env"

@Serializable
data class QQBotInfo(val id: Long, val pwd: String)

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
object UniBot {
    val env = File(envName).readLines()

    init {
        Database.connect(env[0], "org.postgresql.Driver", env[1], env[2])
        transaction {
            SchemaUtils.create(Configs, ConnectInfo, MarkovData, MarkovConfig, PicData, PicIndex, Superusers, Histories)
        }
    }

    val tgToken = kvget("core:tgToken")!!

    val qq = runBlocking {
        // https://github.com/cssxsh/fix-protocol-version
        MiraiProtocolInternal.protocols[BotConfiguration.MiraiProtocol.ANDROID_PHONE] = MiraiProtocolInternal(
            "com.tencent.mobileqq",
            537151218,
            "8.9.33.10335",
            "6.0.0.2534",
            150470524,
            0x10400,
            16724722,
            "A6 B7 45 BF 24 A2 C2 77 52 77 16 F6 F3 6E B6 8D",
            1673599898L,
            19,
        )
        val row: List<QQBotInfo> = SJson.decodeFromString(kvget("core:qqInfo")!!)
        BotFactory.newBot(row[0].id, row[0].pwd) {
            fileBasedDeviceInfo()
            highwayUploadCoroutineCount = 5
            protocol = BotConfiguration.MiraiProtocol.ANDROID_PHONE
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
        withContext(tgThread) {
            tg.start()
            println("telegram online")
        }
        qq.join()
    }

}
