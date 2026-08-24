package one.nfolio.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.csrf.CSRF

fun Application.configureSecurity() {
  install(CSRF) {
    // tests Origin is an expected value
    allowOrigin("https://mac.nfolio.one")
    allowOrigin("http://localhost:4321") // TODO: 後で消す

    // tests Origin matches Host header
    // originMatchesHost()
  }
}
