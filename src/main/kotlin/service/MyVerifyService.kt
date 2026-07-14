package one.nfolio.service

import io.ktor.server.application.ApplicationEnvironment
import one.nfolio.dto.receive.UserLogin
import one.nfolio.dto.sessions.LineUserSession

class MyVerifyService(val directusService: DirectusService, val lineOauthService: LineOauthService, environment: ApplicationEnvironment) {

  private val log = environment.log

  suspend fun baseLogin(res: UserLogin): String? {
    val lineVerifyRes = lineOauthService.verifyIDToken(res.token) // トークン検証

    return if (lineVerifyRes == null) { // 検証失敗
      null
    } else { // 検証成功
      var primaryID = directusService.getLinePrimaryID(lineVerifyRes.sub)
      if (primaryID == null) {
        primaryID = directusService.registeringLineID(lineVerifyRes.sub)
        log.info("New Member: {}", primaryID)
        log.debug("LINE Verify response: {}", lineVerifyRes)
      }
      primaryID
    }
  }
}