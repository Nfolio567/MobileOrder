package one.nfolio.service

import io.ktor.server.application.ApplicationEnvironment
import one.nfolio.dto.receive.IsExistsUser
import one.nfolio.dto.receive.UserLogin
import java.time.LocalDate
import java.time.ZoneId

class MyVerifyService(private val directusService: DirectusService, private val lineOauthService: LineOauthService, environment: ApplicationEnvironment) {

  private val log = environment.log

  suspend fun baseLogin(res: UserLogin): String? {
    val lineVerifyRes = lineOauthService.verifyIDToken(res.token) // トークン検証

    return if (lineVerifyRes == null) { // 検証失敗
      null
    } else { // 検証成功
      var primaryID = directusService.getLinePrimaryID(lineVerifyRes.sub)
      if (primaryID == null) {
        val nowDate = LocalDate.now(ZoneId.of("Asia/Tokyo"))
        val isCanGetCoupon = nowDate < LocalDate.parse("2026-11-07")
        val userName = if (res.userName == "") lineVerifyRes.name else res.userName;

        primaryID = directusService.registeringLineIDAndName(lineVerifyRes.sub, userName, isCanGetCoupon)
        log.info("New Member: {}", primaryID)
        log.debug("LINE Verify response: {}", lineVerifyRes)
      }
      primaryID
    }
  }

  suspend fun isExistsUser(res: IsExistsUser): Boolean? {
    val lineVerifyRes = lineOauthService.verifyIDToken(res.token) ?: return null // そもそもトークンがおかしい場合
    // LineAccountテーブルのIDをLINE User IDをもとに取得。存在しない場合はfalse
    return directusService.getLinePrimaryID(lineVerifyRes.sub) != null
  }
}