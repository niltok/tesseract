package goldimax.tesseract

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.join
import com.elbekD.bot.Bot as tgBot
import net.mamoe.mirai.Bot as qqBot

@ExperimentalStdlibApi
typealias SubscribeType = (UniBot) -> Unit

@ExperimentalStdlibApi
class UniBot(private val fileName: String) {

    val conf = getJson(fileName)

    val suMgr = SUManager(this)

    val tgToken = conf.string("tg_token")!!
    private val qqID = conf.long("qq_id")!!
    private val qqPwd = conf.string("qq_pwd")!!

    val qq = qqBot(qqID, qqPwd)
    val tg = tgBot.createPolling("", tgToken)
    val connections = Connections(conf.array("connect")!!)
    val history = History()

    init {
        GlobalScope.launch { tg.start() }
        runBlocking { qq.alsoLogin() }
    }

    private val subscribes: MutableList<SubscribeType> = mutableListOf()

    fun subscribeAll(subscribes: Collection<SubscribeType>): UniBot {
        this.subscribes.addAll(subscribes)
        return this
    }

    fun save() = putJson(fileName, conf)

    suspend fun start() {
        subscribes.forEach { it(this) }
        qq.join()
    }
}
