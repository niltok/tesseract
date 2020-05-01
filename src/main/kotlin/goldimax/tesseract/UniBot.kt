package goldimax.tesseract

import com.beust.klaxon.JsonArray
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.join
import com.elbekD.bot.Bot as tgBot
import net.mamoe.mirai.Bot as qqBot

inline class QQUser(val id: Long)
inline class TGUser(val id: Long)

typealias SubscribeType = (UniBot) -> Unit

class SUManager(private val uniBot: UniBot) {
    val qqAdmin = uniBot.conf.array<Long>("qq_admin")!!.toMutableList()
    val tgAdmin = uniBot.conf.array<Long>("tg_admin")!!.toMutableList()

    // Notice: Use 'contains' would not work.
    fun isSuperuser(user: QQUser) = qqAdmin.find { it == user.id } != null
    fun isSuperuser(user: TGUser) = tgAdmin.find { it == user.id } != null

    fun save() {
        uniBot.conf["qq_admin"] = JsonArray(qqAdmin)
        uniBot.conf["tg_admin"] = JsonArray(tgAdmin)
    }
}

class UniBot(private val fileName: String) {

    val conf = getJson(fileName)

    val suMgr = SUManager(this)

    val tgToken = conf.string("tg_token")!!
    private val qqID = conf.long("qq_id")!!
    private val qqPwd = conf.string("qq_pwd")!!

    val qq = qqBot(qqID, qqPwd)
    val tg = tgBot.createPolling("", tgToken)

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
