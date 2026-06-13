package one.nfolio

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation;
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import one.nfolio.JSON.RawProducts

fun Application.configureRouting(directus: ConnectDirectus) {
  routing {
    get("/") {
      call.respondText("Hello, World!")
    }
    webSocket("/ws") { // websocketSession
      for (frame in incoming) {
        if (frame is Frame.Text) {
          val text = frame.readText()
          outgoing.send(Frame.Text("YOU SAID: $text"))
          if (text.equals("bye", ignoreCase = true)) {
            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
          }
        }
      }
    }

    get("/api/get/products") {
      directus.getProducts(environment)
    }

    get("/json/kotlinx-serialization") {
      call.respond(mapOf("hello" to "world"))
    }
  }
}