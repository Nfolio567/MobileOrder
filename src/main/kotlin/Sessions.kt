package one.nfolio

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import one.nfolio.sessions.LineUserSession

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