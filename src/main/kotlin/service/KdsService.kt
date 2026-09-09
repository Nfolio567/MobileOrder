package one.nfolio.service

import dto.directus.RawOrders
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json
import one.nfolio.dto.directus.SubscriptionOrders

class KdsService(
  private val client: HttpClient,
  private val environment: ApplicationEnvironment,
  private val kdsWsSession: DefaultWebSocketServerSession,
) {
  private lateinit var directusSession: DefaultClientWebSocketSession
  suspend fun awaitDirectusSocket() { // DirectusのSubscription待機
    connect2Directus()
    try {
      // Directus Subscriptionの認証
      directusSession.send(Frame.Text(
        """
        {
          "type": "auth",
          "access_token": "${environment.config.property("directus.access-token").getString()}"
        }
        """.trimIndent()
      ))

      // WebSocket待機
      for (frame in directusSession.incoming) {
        when (frame) {
          is Frame.Text -> {
            val rawText = frame.readText()

            val message = Json.decodeFromString<SubscriptionOrders>(rawText)
            checkReceive(directusSession, message)
          }

          is Frame.Close -> {
            environment.log.info("Close: {}", frame.readReason())
          }

          is Frame.Ping -> {
            environment.log.info("WebSocket Ping")
          }

          is Frame.Pong -> {
            environment.log.info("WebSocket Pong")
          }

          is Frame.Binary -> {}
        }
      }
    } catch (e: ClosedReceiveChannelException) {
      environment.log.info("WebSocket received channel closed")
      environment.log.info("Close Reason: ${directusSession.closeReason.await()}")
      environment.log.info("WebSocket Closed: ", e)
    } catch (e: Exception) {
      environment.log.info("WebSocket Exception ", e)
    }
  }

  /**
   * Directusと接続
   */
  private suspend fun connect2Directus() {
    if (::directusSession.isInitialized) {
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
        directusSession = this
      }
    } // 接続はオンリーワン
  }

  /**
   * Subscriptionのwsレスポンスのチェック
   */
  private suspend fun checkReceive(session: DefaultClientWebSocketSession, message: SubscriptionOrders) {
    when (message.type) {
      "subscription" -> {
        when (message.event) {
          "init" -> {
            environment.log.info("Directus Websocket connected :)")
          }

          "create", "update" -> {
            environment.log.info("Frame text: {}", message)
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
                  "type": "subscribe",
                  "collection": "orders",
                  "query": {
                    "fields": [
                      "*",
                      "items.*",
                      "items.options.options_id.*",
                      "items.productID.*"
                    ]
                  }
                }
                """.trimIndent()
              )
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

      "ping" -> {
        environment.log.info("Directus ping")
        session.send(Frame.Text(
          """
          {"type": "pong"}
          """.trimIndent()
        ))
      }

      "server" -> {
        when(message.status) {
          "error" -> {
            environment.log.warn(
              "Directus WebSocket Subscription failed '{}': {}",
              message.error!!.code,
              message.error.message
            )
          }
        }
      }
    }
  }

  /**
   * KDSに送信
   */
  private suspend fun sendOrderDiff(diffContent: List<RawOrders>?) {
    if (diffContent == null) return
    environment.log.info("Send diff to KDS: {}", diffContent)
    for (i in diffContent) {
      kdsWsSession.send(Frame.Text(Json.encodeToString(i)))
    }
  }
}
