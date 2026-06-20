package one.nfolio

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.session
import io.ktor.server.response.respond
import one.nfolio.dto.response.ErrorMessage
import one.nfolio.sessions.LineUserSession

fun Application.configureAuthentication(directus: ConnectDirectus) {
  install(Authentication) {
    session<LineUserSession>("line-user-session") {
      validate { session ->
        directus.getLineUserID(session.linePrimaryID)
      }

      challenge {
        call.respond(
          HttpStatusCode.Unauthorized,
          ErrorMessage(
            "Unauthorized",
            "The token is invalid or you are not logged in."
          )
        )
      }
    }
  }
}