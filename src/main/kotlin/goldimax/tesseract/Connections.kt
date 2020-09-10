package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject

data class Connection(
    val qq: Long,
    val tg: Long
)


class Connections(raw: JsonArray<JsonObject>) {
    val internal = lazy { raw.map {
        Connection(it.long("qq")!!, it.long("tg")!!) } .toMutableList() }
    fun findQQByTG(tg: Long) = internal.value.find { it.tg == tg }?.qq
    fun findTGByQQ(qq: Long) = internal.value.find { it.qq == qq }?.tg
}
