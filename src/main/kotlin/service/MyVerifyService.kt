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
      var userID = directusService.getLinePrimaryID(lineVerifyRes.sub)
      if (userID == null) {
        val nowDate = LocalDate.now(ZoneId.of("Asia/Tokyo"))
        val isCanGetCoupon = nowDate < LocalDate.parse("2026-11-07")
        val userName = if (res.userName == "") lineVerifyRes.name else res.userName

        userID = userName?.let { directusService.registeringLineIDAndName(lineVerifyRes.sub, it, isCanGetCoupon) }
        log.info("New Member: {}", userID)
        log.debug("LINE Verify response: {}", lineVerifyRes)
      }
      userID
    }
  }

  suspend fun isExistsUser(res: IsExistsUser): Boolean? {
    val lineVerifyRes = lineOauthService.verifyIDToken(res.token) ?: return null // そもそもトークンがおかしい場合
    // LineAccountテーブルのIDをLINE User IDをもとに取得。存在しない場合はfalse
    return directusService.getLinePrimaryID(lineVerifyRes.sub) != null
  }

  suspend fun verifyCart(cartID: Int?, userID: String, directus: DirectusService): Boolean { // そのカートが本当に本人のものなのか
    val cart = directus.getCart(userID)
    return cart.data.any { it.id == cartID }
  }
}