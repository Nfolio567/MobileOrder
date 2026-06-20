package one.nfolio

import dto.receive.OrderRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
    get("/") { // 最初ここアクセスさせて、認証チェックかける
      val session = call.principal<LineUserSession>()

      // セッション自体があるか否か・ちゃんとテーブルにIDが記録されてるか否か
      if (session != null && directus.getLineUserID(session.linePrimaryID) != null) {
        call.respondRedirect("/home")
        return@get
      } else {
        call.respond(
          HttpStatusCode.Unauthorized,
          ErrorMessage(
            "Unauthorized",
            "The token is invalid or you are not logged in."
          ) // ここでクライアント側でログイン処理する
        )
      }
    }

    post("/login") {
      val res = call.receive<UserLogin>()

      val lineRes = line.verifyIDToken(res.token)
      if (lineRes != null) {
        val primaryID = directus.registeringLineID(lineRes.sub)
        call.sessions.set(LineUserSession(primaryID))
        call.respondRedirect("/home")
      } else {
        call.respond(
          HttpStatusCode.Unauthorized,
          ErrorMessage(
            "Unauthorized",
            "The token is invalid."
          )
        )
      }
    }

    authenticate("line-user-session") {
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