package goldimax.tesseract

import com.beust.klaxon.JsonArray

inline class QQUser(val id: Long)
inline class TGUser(val id: Long)
@ExperimentalStdlibApi
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