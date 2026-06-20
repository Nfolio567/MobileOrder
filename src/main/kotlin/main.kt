package one.nfolio

import io.ktor.server.application.*
import security.HMAC

fun main(args: Array<String>) {
  io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
  val client = configureClient()
  val line = ConnectLineAPI(client, environment, "2.1")
  val directus = ConnectDirectus(client, environment)

  configureHttp()
  configureSecurity()
  configureWebsockets()
  configureSerialization()
  configureSessions()
  configureAuthentication(directus)
  configureRouting(
    directus,
    line,
    HMAC(environment)
  )
}
