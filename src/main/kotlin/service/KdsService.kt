package one.nfolio.service

import dto.directus.RawOrders
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json
import one.nfolio.dto.directus.SubscriptionOrders

class KdsService(
  private val client: HttpClient,
  private val environment: ApplicationEnvironment,
  private val kdsWsSession: DefaultWebSocketServerSession,
) {
  suspend fun awaitDirectusSocket() {
    client.webSocket(
      method = HttpMethod.Get,
      host = "localhost",
      port =
        environment.config
          .property("directus.port")
          .getString()
          .toInt(),
      path = "/websocket",
    ) {
      try {

        send(Frame.Text(
          """
          {
            "type": "auth",
            "access_token": "${environment.config.property("directus.access-token").getString()}"
          }
          """.trimIndent()
        ))
        environment.log.info("{}", environment.config.property("directus.access-token").getString())

        for (frame in incoming) {
          if (frame !is Frame.Text) continue
          environment.log.info("Frame: {}", frame.readText())

          val message = receiveDeserialized<SubscriptionOrders>()
          checkReceive(this, message)
        }
      } catch (e: ClosedReceiveChannelException) {
        environment.log.info("WebSocket received channel closed")
        environment.log.info("Close Reason: ${closeReason.await()}")
        environment.log.info("WebSocket Closed: ", e)
      } catch (e: Exception) {
        environment.log.info("WebSocket Exception ", e)
      }
    }
  }

  private suspend fun checkReceive(session: DefaultClientWebSocketSession, message: SubscriptionOrders) {
    when (message.type) {
      "subscription" -> {
        when (message.event) {
          "init" -> {
            environment.log.info("Directus Websocket connected:)")
          }

          "create", "update" -> {
            sendOrderDiff(message.data)
          }
        }
      }

      "auth" -> {
        when (message.status) {
          "ok" ->
            session.send(
              Frame.Text(
                """
                {
                  "type": "subscription",
                  "collection": "orders"
                }
                """.trimIndent(),
              ),
            )
          "error" -> {
            environment.log.warn(
              "Directus WebSocket Authentication failed '{}': {}",
              message.error!!.code,
              message.error.message
            )
          }
        }
      }

    }
  }

  private suspend fun sendOrderDiff(diffContent: List<RawOrders>?) {
    if (diffContent == null) return
    for (i in diffContent) {
      kdsWsSession.send(Frame.Text(Json.encodeToString(i)))
    }
  }
}
