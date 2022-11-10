package niltok.tesseract

import io.lettuce.core.*
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

fun main() = runBlocking {
//    val env = File("env").readLines().toMutableList()
//    if (env[4] == "redis") {
//        println("moving db")
//        val redis = RedisClient.create(env[0])!!
//        val redisClient = redis.connect()!!
//        val db = redisClient.sync()
//        val redisClientRaw = redis.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE))!!
//        val dbRaw = redisClientRaw.sync()
//        Database.connect(env[1], "com.impossibl.postgres.jdbc.PGDriver", env[2], env[3])
//        println("db connected")
//
//        transaction {
//            SchemaUtils.create(Configs, ConnectInfo, MarkovData, MarkovConfig, PicData, PicIndex, Superusers, Histories)
//            Configs.deleteAll()
//            ConnectInfo.deleteAll()
//            MarkovData.deleteAll()
//            MarkovConfig.deleteAll()
//            PicData.deleteAll()
//            PicIndex.deleteAll()
//            Superusers.deleteAll()
//        }
//
//        println("table created")
//
//        transaction {
//            Configs.insert {
//                it[key] = "core:qqInfo"
//                it[value] = db.get("core:qqInfo")!!
//            }
//            Configs.insert {
//                it[key] = "core:tgToken"
//                it[value] = db.get("core:tgToken")!!
//            }
//        }
//        println("unibot moved")
//
//        ({
//            val data : List<Connection> = SJson.decodeFromString(db.get("core:connections"))
//            for (c in data) {
//                if (c !is Connection.GroupForward) continue
//                val qid = c.groups.filterIsInstance<IMGroup.QQ>().firstOrNull()?.id ?: continue
//                val tid = c.groups.filterIsInstance<IMGroup.TG>().firstOrNull()?.id ?: continue
//                val d = db.sismember("core:drive:list",
//                    SJson.encodeToString(IMGroup.TG(tid)))
//                try {
//                    transaction {
//                        ConnectInfo.insert {
//                            it[qq] = qid
//                            it[tg] = tid
//                            it[drive] = d
//                        }
//                    }
//                } catch (e: Exception) {
//                    println(e.message)
//                }
//            }
//        }())
//        println("connections moved")
//
//        var cur: ScanCursor
//
//        cur = ScanCursor.of("0")
//        do {
//            val mcur = db.hscan("markov:data", cur)
//            cur = mcur
//            try {
//                transaction {
//                    MarkovData.batchInsert(mcur.map.toList()) {
//                        this[MarkovData.key] = it.first
//                        this[MarkovData.next] = it.second
//                    }
//                }
//            } catch (e: Exception) {
//                println(e.message)
//                println(mcur.map)
//            }
//        } while (!cur.isFinished)
//        cur = ScanCursor.of("0")
//        do {
//            val mcur = db.hscan("core:markov:p", cur)
//            cur = mcur
//            transaction {
//                MarkovConfig.batchInsert(mcur.map.toList()) {
//                    this[MarkovConfig.group] = it.first
//                    this[MarkovConfig.possibility] = it.second.toDouble()
//                }
//            }
//        } while (!cur.isFinished)
//        println("markov moved")
//
//        cur = ScanCursor.of("0")
//        do {
//            val rcur = dbRaw.hscan("image:data", cur)
//            cur = rcur
//            transaction {
//                PicData.batchInsert(rcur.map.toList()) {
//                    this[PicData.id] = it.first
//                    this[PicData.data] = ExposedBlob(it.second)
//                }
//            }
//        } while (!cur.isFinished)
//        cur = ScanCursor.of("0")
//        do {
//            val mcur = db.hscan("core:image:index", cur)
//            cur = mcur
//            transaction {
//                PicIndex.batchInsert(mcur.map.toList()) {
//                    this[PicIndex.group] = it.first
//                    this[PicIndex.pics] = it.second
//                }
//            }
//        } while (!cur.isFinished)
//        println("picture moved")
//
//        cur = ScanCursor.of("0")
//        do {
//            val vcur = db.sscan("core:admin", cur)
//            cur = vcur
//            transaction {
//                Superusers.batchInsert(vcur.values) {
//                    this[Superusers.user] = it
//                }
//            }
//        } while (!cur.isFinished)
//        println("su moved")
//
//        env[4] = "pgsql"
//        File("env").writeText(env.joinToString("\n"))
//        println("done")
//        return@runBlocking
//    }

    // Load Plugin
    UniBot
    QQOther
    TgOther
    Connections
    SUManager
    Forward
    Picture
    Markov
    WebPage
    Recorder
    FireWall

    // WebServer

    UniBot.start()

}

