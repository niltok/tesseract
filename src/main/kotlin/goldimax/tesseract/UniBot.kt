package goldimax.tesseract

import com.alicloud.openservices.tablestore.SyncClient
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.elbekD.bot.types.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.join
import java.io.File
import java.lang.Exception
import com.elbekD.bot.Bot as tgBot
import net.mamoe.mirai.Bot as qqBot

const val envName = "env"

@ExperimentalStdlibApi
object UniBot {

    val table = {
        val env = File(envName).readLines()
        SyncClient(env[0], env[1], env[2], env[3])
    }()

    val tgToken = table.read("core", listOf("key" to "tg"))!!["token"]!!.asString()!!

    val qq = {
        val row = table.read("core", listOf("key" to "qq"))!!
        qqBot(row["id"]!!.asLong(), row["pwd"]!!.asString())
    } ()
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
