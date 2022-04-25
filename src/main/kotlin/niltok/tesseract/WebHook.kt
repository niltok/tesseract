package niltok.tesseract

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

object WebHook {
    @Serializable
    class Template(val target: IMGroup.QQ, val secret: String, val template: Node) {
        @Serializable
        @SerialName("WebHook.Template.Node")
        sealed class Node {
            fun apply(headers: Headers, json: JsonElement): String? {
                return when (this) {
                    is Text -> text
                    is Compose -> list.map { it.apply(headers, json) }.let {
                        if (it.any { it == null }) null else it.joinToString()
                    }
                    is HeaderVar -> of(headers) ?: "[!NULL ERROR: Header $name!]"
                    is JsonVar -> of(json) ?: "[!NULL ERROR: Body ${path.joinToString(".")}!]"
                    is When -> cases.firstOrNull {
                        when (it.variable) {
                            is HeaderVar -> it.variable of headers
                            is JsonVar -> it.variable of json
                        } == it.data
                    } ?.body?.apply(headers, json)
                }
            }
        }

        @Serializable
        @SerialName("WebHook.Template.Text")
        data class Text(val text: String): Node()

        @Serializable
        @SerialName("WebHook.Template.Var")
        sealed class Var: Node()

        @Serializable
        @SerialName("WebHook.Template.HeaderVar")
        data class HeaderVar(val name: String): Var() {
            infix fun of(headers: Headers): String? {
                return headers[name]
            }
        }

        @Serializable
        @SerialName("WebHook.Template.JsonVar")
        data class JsonVar(val path: List<String>): Var() {
            infix fun of(json: JsonElement): String? {
                return path.fold<String, JsonElement?>(json) { elem, s ->
                    elem?.jsonObject?.get(s)
                }?.let {
                    if (it is JsonPrimitive && it.isString) it.content
                    else it.toString()
                }
            }
        }

        @Serializable
        @SerialName("WebHook.Template.Compose")
        data class Compose(val list: List<Node>): Node()

        @Serializable
        data class Case(val variable: Var, val data: String?, val body: Node)

        @Serializable
        @SerialName("WebHook.Template.When")
        data class When(val cases: List<Case>): Node()

        fun apply(headers: Headers, json: JsonElement): String? {
            return template.apply(headers, json)
        }
    }

    fun Route.webHookRoute() {
        post("/webhook/{id}") {
            val templateInfo = db().hget("webhook:instance", context.parameters["id"].also {
                if (it == null) call.respondText("no id provide", status = HttpStatusCode.BadRequest)
            } ?: return@post).also {
                if (it == null) call.respondText("id not find", status = HttpStatusCode.NotFound)
            } ?: return@post
            val template = SJson.decodeFromString<Template>(db().hget("webhook:template", templateInfo))
            template.apply(call.request.headers, SJson.parseToJsonElement(call.receiveText()))?.let {  }
            call.respondText("ok")
        }
    }
}

fun main() {
    println(WebHook.Template.JsonVar(listOf("d")) of SJson.parseToJsonElement("""{"a":{"b":"c"}, "d":1}"""))
}