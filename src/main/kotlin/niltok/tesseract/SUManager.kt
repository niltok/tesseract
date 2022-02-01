package niltok.tesseract

import kotlinx.serialization.encodeToString

object SUManager {
    fun addSuperuser(user: IMUser): Long = db().sadd("core:admin", SJson.encodeToString(user))
    fun isSuperuser(user: IMUser): Boolean = db().sismember("core:admin", SJson.encodeToString(user))
}