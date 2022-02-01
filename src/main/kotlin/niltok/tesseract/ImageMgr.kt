package niltok.tesseract

import java.util.*

object ImageMgr {
    operator fun get(id: String): ByteArray? =
        dbRaw().hget("image:data", id)
    operator fun get(id: UUID): ByteArray? =
        dbRaw().hget("image:data", id.toString())
    operator fun set(id: String, image: ByteArray) {
        dbRaw().hset("image:data", id, image)
    }
    operator fun set(id: UUID, image: ByteArray) {
        dbRaw().hset("image:data", id.toString(), image)
    }
    fun remove(id: UUID) {
        dbRaw().hdel("image:data", id.toString())
    }
    fun remove(id: String) {
        dbRaw().hdel("image:data", id)
    }
    fun new(image: ByteArray): UUID {
        val id = UUID.randomUUID()!!
        this[id] = image
        return id
    }
}
