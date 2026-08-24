package one.nfolio.plugin

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureHttp() {
  install(CORS) {
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    allowMethod(HttpMethod.Patch)
    allowHeader(HttpHeaders.Authorization)
    allowHeader(HttpHeaders.ContentType)
    allowHeader(HttpHeaders.Origin)
    allowHeader(HttpHeaders.Host)
    allowHost("mac.nfolio.one")
    allowHost("localhost:4321") // TODO: 本番では消す

    allowCredentials = true
    // anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
  }
}
