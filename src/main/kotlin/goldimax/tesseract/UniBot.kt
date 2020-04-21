package goldimax.tesseract

import com.beust.klaxon.JsonObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.*
import com.elbekD.bot.Bot as tgBot
import net.mamoe.mirai.Bot as qqBot

class UniBot(private val fileName: String) {

    val conf = getJson(fileName)

    val tgToken = conf.string("tg_token")!!
    private val qqID = conf.long("qq")!!
    private val qqPwd = conf.string("qq_pwd")!!
    val qqAdmin = conf.array<Long>("qq_admin")!!.toMutableList()

    val qq = qqBot(qqID, qqPwd)
    val tg = tgBot.createPolling("", tgToken)

    init {
        GlobalScope.launch { tg.start() }
        runBlocking { qq.alsoLogin() }
    }

    // Notice: Use 'contains' would not work.
    fun isSuperuser(id: Long) = qqAdmin.find { it == id } != null
    fun save() = putJson(fileName, conf)
    suspend fun join() = qq.join()
}