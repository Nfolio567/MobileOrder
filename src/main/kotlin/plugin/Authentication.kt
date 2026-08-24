package one.nfolio.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import io.ktor.server.auth.session
import io.ktor.server.request.path
import io.ktor.server.response.respondRedirect
import one.nfolio.dto.sessions.LineUserSession
import one.nfolio.security.GenerateHash
import one.nfolio.service.DirectusService
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun Application.configureAuthentication(directus: DirectusService, generateHash: GenerateHash) {
  install(Authentication) {
    session<LineUserSession>("line-user-session") {
      validate { session ->
        val userID = directus.getLineUserID(session.linePrimaryID)

        if (userID != null) session else null
      }

      challenge {
        val encodedPath = URLEncoder.encode(call.request.path(), StandardCharsets.UTF_8)
        call.respondRedirect("/?redirect=$encodedPath") // 認証成功したら元のページに戻れるように
        call.application.log.info("Redirect to '/' from '$encodedPath'")
      }
    }
    bearer("kds-auth") {
      authenticate { credential ->
        if (generateHash.sha256(credential.token) == directus.getKdsHashedToken()) {
          UserIdPrincipal("kds")
        } else {
          null
        }
      }
    }
  }
}
