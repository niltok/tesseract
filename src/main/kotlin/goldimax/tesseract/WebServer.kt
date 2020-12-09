package goldimax.tesseract

import com.jcabi.manifests.Manifests
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.util.*

object WebServer {
    private val server = embeddedServer(Netty, 8070) {
        routing {
            get("/info") {
                call.respondText("<h1>Copy. I am online.</h1> " +
                        "Build: ${Manifests.read("Version")}",
                    ContentType.Text.Html
                )
            }
        }
    }
    init {
         server.start(wait = false)
    }
}

