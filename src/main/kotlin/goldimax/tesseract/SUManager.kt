package goldimax.tesseract

import com.beust.klaxon.JsonArray

inline class QQUser(val id: Long)
inline class TGUser(val id: Long)
@ExperimentalStdlibApi
class SUManager(private val uniBot: UniBot) {
    private val raw = uniBot.table.read("core", listOf("key" to "sumgr"))!!
    val qqAdmin = raw["qq"]!!.asString().split(",")
        .map { it.trim().toLong() }.toMutableSet()
    val tgAdmin = raw["tg"]!!.asString().split(",")
        .map { it.trim().toLong() }.toMutableSet()

    // Notice: Use 'contains' would not work.
    fun isSuperuser(user: QQUser) = qqAdmin.contains(user.id)
    fun isSuperuser(user: TGUser) = tgAdmin.contains(user.id)

    fun save() {
        uniBot.table.write("core", listOf("key" to "sumgr"), listOf(
            "qq" to cVal(qqAdmin.toList().joinToString(",")),
            "tg" to cVal(tgAdmin.toList().joinToString(","))))
    }
}