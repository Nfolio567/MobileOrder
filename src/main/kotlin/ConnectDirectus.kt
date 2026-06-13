package one.nfolio

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationEnvironment
import one.nfolio.JSON.RawProducts

class ConnectDirectus(val client: HttpClient) {
  suspend fun getProducts(environment: ApplicationEnvironment) {
    val directusUrl = String.format(
      "%s:%s",
      environment.config.property("directus.host"),
      environment.config.property("directus.port")
    )

    val rawProducts = client.get("${directusUrl}/items/products") {
      header("Authorization", "Bearer")
    }.body<RawProducts>()
  }
}