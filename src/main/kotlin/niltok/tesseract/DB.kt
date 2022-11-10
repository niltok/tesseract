package niltok.tesseract

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Configs : Table() {
    val key = text("key")
    val value = text("value")
    override val primaryKey = PrimaryKey(key)
}

object Histories : Table() {
    val qq = text("qq").index()
    val tg = text("tg").index()
    val qqRef = text("qqref")
    val time = datetime("time").index()
    override val primaryKey = PrimaryKey(arrayOf(qq, tg))
}

object ConnectInfo : Table() {
    val qq = long("qq").index(isUnique = true)
    val tg = long("tg").index(isUnique = true)
    val drive = bool("drive")
    override val primaryKey = PrimaryKey(arrayOf(qq, tg))
}

object MarkovData : Table() {
    val key = text("key")
    val next = text("text")
    override val primaryKey = PrimaryKey(key)
}

object MarkovConfig : Table() {
    val group = text("group")
    val possibility = double("possibility")
    override val primaryKey = PrimaryKey(group)
}

object PicData : Table() {
    val id = text("id")
    val data = blob("data")
    override val primaryKey = PrimaryKey(id)
}

object PicIndex : Table() {
    val group = text("group")
    val pics = text("pics")
    override val primaryKey = PrimaryKey(group)
}

object Superusers : Table() {
    val user = text("user")
    override val primaryKey = PrimaryKey(user)
}
