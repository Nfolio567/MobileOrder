package one.nfolio

import dto.receive.OrderRequest
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlin.collections.mapOf

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
      call.respond(mapOf(
        "products" to directus.getProducts()
      ))
    }

    get("/api/get/options") {
      call.respond(mapOf(
        "options" to directus.getOptions()
      ))
    }

    post("/api/post/order") {
      val res = call.receive<OrderRequest>()

      directus.registeringOrder(res)
    }

    get("/dto/kotlinx-serialization") {
      call.respond(mapOf("hello" to "world"))
    }
  }
}