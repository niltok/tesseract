package goldimax.tesseract

import java.util.*

@ExperimentalStdlibApi
class ImageMgr(val uniBot: UniBot) {
    operator fun get(id: UUID): ByteArray? =
        uniBot.table.read("image", listOf("id" to id.toString()))?.get("image")?.asBinary()
    operator fun set(id: UUID, image: ByteArray) =
        uniBot.table.write("image", listOf("id" to id.toString()), listOf("image" to cVal(image)))
    fun remove(id: UUID) =
        uniBot.table.remove("image", listOf("id" to id.toString()))
    fun new(image: ByteArray): UUID {
        val id = UUID.randomUUID()
        this[id] = image
        return id
    }
}
