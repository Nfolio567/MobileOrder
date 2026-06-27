package one.nfolio

import dto.receive.OrderRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import one.nfolio.dto.receive.UserLogin
import one.nfolio.dto.response.ErrorMessage
import one.nfolio.sessions.LineUserSession
import security.HMAC
import java.util.*

fun Application.configureRouting(directus: ConnectDirectus, line: ConnectLineAPI, hmac: HMAC) {
  routing {
    staticResources("/", "/static/public")

    post("/login") { // 最初にここにリクエスト送らせて、認証チェックする。未ログインならログイン処理をする。
      try {
        val session = call.principal<LineUserSession>()

        log.info("Session: {}\n{}", call.request.origin.remoteHost, session)

        // セッション自体があるか否か・ちゃんとテーブルにIDが記録されてるか否か
        if (session == null || directus.getLineUserID(session.linePrimaryID) == null) { // 未ログインのブロック
          val res = call.receive<UserLogin>()

          val lineRes = line.verifyIDToken(res.token) // トークン検証
          if (lineRes == null) { // 検証失敗
            call.respond(
              HttpStatusCode.Unauthorized,
              ErrorMessage(
                "Unauthorized",
                "The token is invalid or null."
              )
            )
            log.info("Return 401: {}", call.request.origin.remoteHost)
            return@post
          }

          // 検証成功
          val primaryID = directus.registeringLineID(lineRes.sub)
          log.info("LINE Verify response: {}", lineRes)

          log.info("Session set: {}", call.request.origin.remoteHost)
          call.sessions.set(LineUserSession(primaryID))
        }

        call.respond(mapOf("redirect" to "/home"))
        log.info("Redirect request to /home: {}", call.request.origin.remoteHost)
      } catch (e: Exception) {
        log.warn("'/login' receive error", e)
        call.respond(HttpStatusCode.InternalServerError)
        return@post
      }
    }

    authenticate("line-user-session") {
     get("/home") {
       call.respondResource("/static/private/home/index.html")
     }

      get("/api/get/products") {
        call.respond(
          mapOf(
            "products" to directus.getProducts()
          )
        )
      }

      get("/api/get/options") {
        call.respond(
          mapOf(
            "options" to directus.getOptions()
          )
        )
      }

      get("/api/get/recommended") {
        call.respond(
          mapOf(
            "message" to directus.getRecommendedMessage()
          )
        )
      }

      post("/api/post/order") {
        val res = call.receive<OrderRequest>()

        val linePrimaryID = call.principal<LineUserSession>()!!.linePrimaryID
        val orderIDAndFakeID = directus.registeringOrder(res, linePrimaryID)

        val macBase64 = Base64
          .getUrlEncoder()
          .withoutPadding()
          .encodeToString(hmac.generateMAC(orderIDAndFakeID.first))

        call.respond(
          mapOf(
            "orderID" to orderIDAndFakeID.second,
            "qrCode" to "${orderIDAndFakeID.first}:${macBase64}"
          )
        )
      }

      get("/dto/kotlinx-serialization") {
        call.respond(mapOf("hello" to "world"))
      }
    }
  }
}