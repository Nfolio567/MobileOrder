package one.nfolio.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import one.nfolio.dto.sessions.LineUserSession

fun Application.configureSessions() {
  install(Sessions) {
    cookie<LineUserSession>("Session") {
      cookie.path = "/"
      cookie.httpOnly = true
      cookie.secure = true
      cookie.extensions["SameSite"] = "lax"
    }
  }
}
