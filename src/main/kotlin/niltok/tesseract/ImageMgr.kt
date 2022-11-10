package niltok.tesseract

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object ImageMgr {
    operator fun get(pid: String): ByteArray? = transaction {
        PicData.select { PicData.id eq pid }.firstOrNull()?.get(PicData.data)
    }?.bytes

    operator fun get(id: UUID): ByteArray? =
        get(id.toString())
    operator fun set(pid: String, image: ByteArray) {
        transaction {
            PicData.replace {
                it[id] = pid
                it[data] = ExposedBlob(image)
            }
        }
    }
    operator fun set(id: UUID, image: ByteArray) {
        set(id.toString(), image)
    }
    fun remove(id: UUID) {
        remove(id.toString())
    }
    fun remove(pid: String) {
        transaction {
            PicData.deleteWhere { id eq pid }
        }
    }
    fun new(image: ByteArray): UUID {
        val id = UUID.randomUUID()!!
        this[id] = image
        return id
    }
}
