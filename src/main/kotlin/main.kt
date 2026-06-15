package one.nfolio

import io.ktor.server.engine.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
  io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
  configureHttp()
  configureSecurity()
  configureWebsockets()
  configureSerialization()
  val client = configureClient()
  configureRouting(ConnectDirectus(client, environment))
}
