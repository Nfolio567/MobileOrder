package one.nfolio

import io.ktor.server.application.*
import io.ktor.server.plugins.csrf.*

fun Application.configureSecurity() {
  install(CSRF) {
    // tests Origin is an expected value
    allowOrigin("https://mac.nfolio.one")


    // tests Origin matches Host header
    //originMatchesHost()
  }
}