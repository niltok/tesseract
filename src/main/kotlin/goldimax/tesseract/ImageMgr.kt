package goldimax.tesseract

import java.util.*

object ImageMgr {
    operator fun get(id: UUID): ByteArray? =
        UniBot.table.read("image", listOf("id" to id.toString()))?.get("image")?.asBinary()
    operator fun set(id: UUID, image: ByteArray) =
        UniBot.table.write("image", listOf("id" to id.toString()), listOf("image" to cVal(image)))
    fun remove(id: UUID) =
        UniBot.table.remove("image", listOf("id" to id.toString()))
    fun new(image: ByteArray): UUID {
        val id = UUID.randomUUID()!!
        this[id] = image
        return id
    }
}
