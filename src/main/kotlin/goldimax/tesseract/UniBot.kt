package goldimax.tesseract

import com.alicloud.openservices.tablestore.SyncClient
import com.elbekD.bot.types.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.alsoLogin
import java.io.File
import com.elbekD.bot.Bot as tgBot

const val envName = "env"

object UniBot {
    val env = File(envName).readLines()

    val table = SyncClient(env[0], env[1], env[2], env[3])

    val tgToken = table.read("core", listOf("key" to "tg"))!!["token"]!!.asString()!!

    val qq = run {
        val row = table.read("core", listOf("key" to "qq"))!!
        BotFactory.newBot(row["id"]!!.asLong(), row["pwd"]!!.asString())
    }
    val tg = tgBot.createPolling("", tgToken)
    val tgListener = mutableListOf<suspend (Message) -> Unit>()

    init {
        tg.onMessage { msg -> tgListener.forEach { it(msg) } }
        GlobalScope.launch { tg.start() }
        runBlocking { qq.alsoLogin() }
    }

    suspend fun start() {
        qq.join()
    }
}
