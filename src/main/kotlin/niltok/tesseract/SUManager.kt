package niltok.tesseract

import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object SUManager {
    fun addSuperuser(u: IMUser) {
        transaction {
            Superusers.replace {
                it[user] = SJson.encodeToString(u)
            }
        }
    }
    fun isSuperuser(u: IMUser): Boolean = transaction {
        Superusers.select { Superusers.user eq SJson.encodeToString(u) }.count()
    } != 0L
}