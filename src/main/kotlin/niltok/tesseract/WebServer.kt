package niltok.tesseract

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

object WebServer {
    val server = embeddedServer(Netty, port = 8088) {
        routing {  }
    } .start()
}