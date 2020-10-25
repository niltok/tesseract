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

@ExperimentalStdlibApi
typealias SubscribeType = (UniBot) -> Any

@ExperimentalStdlibApi
class UniBot(private val fileName: String, envName: String) {


    val table = {
        val env = File(envName).readLines()
        SyncClient(env[0], env[1], env[2], env[3])
    }()

    val suMgr = SUManager(this)

    val tgToken = table.read("core", listOf("key" to "tg"))!!["token"]!!.asString()!!

    val qq = {
        val row = table.read("core", listOf("key" to "qq"))!!
        qqBot(row["id"]!!.asLong(), row["pwd"]!!.asString())
    } ()
    val tg = tgBot.createPolling("", tgToken)
    val tgListener = mutableListOf<suspend (Message) -> Unit>()
    val connections = Connections(this)
    val history = History()
    val actionMgr = TransactionManager(this)
    val imageMgr = ImageMgr(this)

    init {
        tg.onMessage { msg -> tgListener.forEach { it(msg) } }
        GlobalScope.launch { tg.start() }
        runBlocking { qq.alsoLogin() }
    }

    private val subscribes: MutableList<SubscribeType> = mutableListOf()

    fun subscribeAll(subscribes: Collection<SubscribeType>): UniBot {
        this.subscribes.addAll(subscribes)
        return this
    }

    suspend fun start() {
        subscribes.forEach {
            try { it(this) } catch (e: Exception) { qq.logger.error(e) }
        }
        qq.join()
    }
}
