package one.nfolio

import io.ktor.server.application.Application
import one.nfolio.plugin.*
import one.nfolio.service.DirectusService
import one.nfolio.service.LineOauthService
import one.nfolio.service.MyVerifyService
import security.HMAC

fun main(args: Array<String>) {
  io.ktor.server.cio.EngineMain
    .main(args)
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
    verifyService,
  )
}
