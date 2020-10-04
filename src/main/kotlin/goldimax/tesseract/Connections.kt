package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject

data class Connection(
    val qq: Long,
    val tg: Long
)

@ExperimentalStdlibApi
class Connections(val uniBot: UniBot) {
    val raw: JsonArray<JsonObject> = {
        val conf: JsonObject =
            uniBot.getJson("core", "key", "connections", "json")
        conf.array("connect")!!
    }()
    val internal = lazy { raw.map {
        Connection(it.long("qq")!!, it.long("tg")!!) } .toMutableList() }
    fun findQQByTG(tg: Long) = internal.value.find { it.tg == tg }?.qq
    fun findTGByQQ(qq: Long) = internal.value.find { it.qq == qq }?.tg
    fun save() {
        uniBot.putJson("core", "key", "connections", "json",
            JsonObject(mapOf("connect" to JsonArray(internal.value.map {
                JsonObject(mapOf("qq" to it.qq, "tg" to it.tg)) }))))
    }
}
