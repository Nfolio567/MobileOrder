package one.nfolio

import io.ktor.server.application.*
import one.nfolio.plugin.configureAuthentication
import one.nfolio.plugin.configureHttp
import one.nfolio.plugin.configureLogging
import one.nfolio.plugin.configureSecurity
import one.nfolio.plugin.configureSerialization
import one.nfolio.plugin.configureSessions
import one.nfolio.plugin.configureWebsockets
import one.nfolio.service.LineOauthService
import one.nfolio.service.DirectusService
import one.nfolio.service.MyVerifyService
import security.HMAC

fun main(args: Array<String>) {
  io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
  val client = configureClient()
  val line = LineOauthService(client, environment, "2.1")
  val directus = DirectusService(client, environment)
  val verifyService = MyVerifyService(directus, line, environment)

  configureLogging()
  configureHttp()
  configureSecurity()
  configureWebsockets()
  configureSerialization()
  configureSessions()
  configureAuthentication(directus)
  configureRouting(
    directus,
    HMAC(environment),
    verifyService
  )
}
