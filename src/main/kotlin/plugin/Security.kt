package one.nfolio.plugin

import io.ktor.server.application.*
import io.ktor.server.plugins.csrf.*

fun Application.configureSecurity() {
  install(CSRF) {
    // tests Origin is an expected value
    allowOrigin("https://mac.nfolio.one")
    allowOrigin("http://localhost:4321") // TODO: 後で消す


    // tests Origin matches Host header
    //originMatchesHost()
  }
}