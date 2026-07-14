package one.nfolio.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.request.path
import io.ktor.server.response.respondRedirect
import one.nfolio.service.DirectusService
import one.nfolio.dto.sessions.LineUserSession

fun Application.configureAuthentication(directus: DirectusService) {
  install(Authentication) {
    session<LineUserSession>("line-user-session") {
      validate { session ->
        val userID = directus.getLineUserID(session.linePrimaryID)

        if (userID != null) session else null
      }

      challenge {
        val path = call.request.path()
        call.respondRedirect("/?redirect=$path")
        call.application.log.info("Redirect to '/' from '${path}'")
      }
    }
  }
}