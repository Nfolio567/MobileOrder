package one.nfolio

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

fun configureClient(): HttpClient =
  HttpClient(CIO) {
    install(ContentNegotiation) {
      json()
    }
    install(WebSockets) {
      //pingInterval = 15.seconds
      contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
  }
