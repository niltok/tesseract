package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject

data class Connection(
    val qq: Long,
    val tg: Long
)

@ExperimentalStdlibApi
class Connections(private val uniBot: UniBot) {
    val connect = {
        val conf =
            uniBot.table.read("core", listOf("key" to "connections"))!!["connect"]!!.asString()
        val raw = conf.split(";").map { it.split(",").map { it.toLong() } }
        raw.map { Connection(it[0], it[1]) } .toMutableList()
    }()
    fun findQQByTG(tg: Long) = connect.find { it.tg == tg }?.qq
    fun findTGByQQ(qq: Long) = connect.find { it.qq == qq }?.tg
    fun save() {
        uniBot.table.write("core", listOf("key" to "connections"), listOf("connect" to
            cVal(connect.joinToString(";") { "${it.qq},${it.tg}" })))
    }
}
