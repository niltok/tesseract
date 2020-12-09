package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject

data class Connection(
    val qq: Long,
    val tg: Long
)

object Connections {
    val connect = {
        val conf =
            UniBot.table.read("core", listOf("key" to "connections"))!!["connect"]!!.asString()
        val raw = conf.split(";").map { it.split(",").map { it.toLong() } }
        raw.map { Connection(it[0], it[1]) } .toMutableList()
    }()
    fun findQQByTG(tg: Long) = connect.find { it.tg == tg }?.qq
    fun findTGByQQ(qq: Long) = connect.find { it.qq == qq }?.tg
    fun save() {
        UniBot.table.write("core", listOf("key" to "connections"), listOf("connect" to
            cVal(connect.joinToString(";") { "${it.qq},${it.tg}" })))
    }
}
