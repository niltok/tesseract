package goldimax.tesseract

import com.beust.klaxon.JsonArray

class QQUser(val id: Long)
class TGUser(val id: Long)

object SUManager {
    private val raw = UniBot.table.read("core", listOf("key" to "sumgr"))!!
    val qqAdmin = raw["qq"]!!.asString().split(",")
        .map { it.trim().toLong() }.toMutableSet()
    val tgAdmin = raw["tg"]!!.asString().split(",")
        .map { it.trim().toLong() }.toMutableSet()

    // Notice: Use 'contains' would not work.
    fun isSuperuser(user: QQUser) = qqAdmin.contains(user.id)
    fun isSuperuser(user: TGUser) = tgAdmin.contains(user.id)

    fun save() {
        UniBot.table.write("core", listOf("key" to "sumgr"), listOf(
            "qq" to cVal(qqAdmin.toList().joinToString(",")),
            "tg" to cVal(tgAdmin.toList().joinToString(","))))
    }
}