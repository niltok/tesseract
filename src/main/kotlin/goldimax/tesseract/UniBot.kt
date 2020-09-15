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
import com.elbekD.bot.Bot as tgBot
import net.mamoe.mirai.Bot as qqBot

@ExperimentalStdlibApi
typealias SubscribeType = (UniBot) -> Unit

@ExperimentalStdlibApi
class UniBot(private val fileName: String, envName: String) {

    val conf = getJson(fileName)
    private val env = File(envName).readLines()

    val table = SyncClient(env[0], env[1], env[2], env[3])

    val suMgr = SUManager(this)

    val tgToken = conf.string("tg_token")!!
    private val qqID = conf.long("qq_id")!!
    private val qqPwd = conf.string("qq_pwd")!!

    val qq = qqBot(qqID, qqPwd)
    val tg = tgBot.createPolling("", tgToken)
    val tgListener = mutableListOf<suspend (Message) -> Unit>()
    val connections = Connections(conf.array("connect")!!)
    val history = History()

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

    fun save() {
        conf["connect"] = JsonArray(connections.internal.value.map {
            JsonObject(mapOf("qq" to it.qq, "tg" to it.tg)) })
        putJson(fileName, conf)
    }

    suspend fun start() {
        subscribes.forEach { it(this) }
        qq.join()
    }
}
