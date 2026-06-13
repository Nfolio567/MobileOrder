package one.nfolio

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

fun configureClient(): HttpClient {
  return HttpClient(CIO) {
    install(ContentNegotiation) {
      json()
    }
  }
}