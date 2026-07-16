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
import one.nfolio.dto.receive.Cart
import one.nfolio.dto.receive.QRResult
import one.nfolio.dto.receive.UpdateCartQuantity
import one.nfolio.dto.receive.UserLogin
import one.nfolio.dto.response.ErrorMessage
import one.nfolio.dto.sessions.LineUserSession
import one.nfolio.service.DirectusService
import one.nfolio.service.MyVerifyService
import security.HMAC
import java.nio.charset.StandardCharsets
import java.util.*

fun Application.configureRouting(directus: DirectusService, hmac: HMAC, myVerifyService: MyVerifyService) {
  routing {
    staticResources("/", "/static/public")

    post("/login") { // 最初にここにリクエスト送らせて、認証チェックする。未ログインならログイン処理をする。
      try {
        val session = call.principal<LineUserSession>()

        log.info("Session: {}\n{}", call.request.origin.remoteHost, session)

        // セッション自体があるか否か・ちゃんとテーブルにIDが記録されてるか否か
        if (session == null || directus.getLineUserID(session.linePrimaryID) == null) { // セッションがない
          val res = call.receive<UserLogin>()
          val primaryID = myVerifyService.baseLogin(res)

          if (primaryID != null) { // 検証成功
            log.info("Session set: {}", call.request.origin.remoteHost)
            call.sessions.set(LineUserSession(primaryID))
          } else { // 検証失敗
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
        }

        call.respond(mapOf("redirect" to "/home"))
        log.debug("Redirect request to /home or other: {}", call.request.origin.remoteHost)
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

      get("/member") {
        call.respondResource("/static/private/member/index.html")
      }

      get("/cart") {
        call.respondResource("/static/private/cart/index.html")
      }

      get("/api/get/products") {
        call.respond(
          mapOf(
            "products" to directus.getProducts().data
          )
        )
      }

      get("/api/get/options") {
        call.respond(
          mapOf(
            "options" to directus.getOptions().data
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

      get("/api/get/cart") {
        try {
          val userID = call.principal<LineUserSession>()!!.linePrimaryID

          call.respond(
            mapOf(
              "cart" to directus.getCart(userID).data
            )
          )
        } catch (e: Exception) {
          log.warn("{} error", call.request.path(), e)
          call.respond(HttpStatusCode.InternalServerError)
        }
      }

      delete("/api/delete/cart/{id}") {
        try {
          val cartID = call.parameters["id"]?.toInt()

          val userID = call.principal<LineUserSession>()!!.linePrimaryID

          if (verifyCart(cartID, userID, directus)) {
            cartID?.let { directus.deleteCart(it) }

            call.respond(mapOf(
              "status" to "success"
            ))
          } else {
            call.respond(HttpStatusCode.Unauthorized)
          }
        } catch (e: Exception) {
          log.warn("{} error", call.request.path(), e)
          call.respond(HttpStatusCode.InternalServerError)
        }
      }

      patch("/api/patch/cart") { // カート内の商品の個数の変更
        try {
          val res = call.receive<UpdateCartQuantity>()

          val userID = call.principal<LineUserSession>()!!.linePrimaryID

          if (verifyCart(res.id, userID, directus)) {
            directus.updateCart(res.id, res.quantity)

            call.respond(mapOf(
              "status" to "success"
            ))
          } else {
            call.respond(HttpStatusCode.Unauthorized)
          }
        } catch (e: Exception) {
          log.warn("{} error", call.request.path(), e)
          call.respond(HttpStatusCode.InternalServerError)
        }
      }

      post("/api/post/cart") {
        try {
          val res = call.receive<Cart>()
          val userID = call.principal<LineUserSession>()!!.linePrimaryID

          directus.registeringCart(res, userID)

          call.respond(mapOf(
            "status" to "success"
          ))
        } catch (e: Exception) {
          log.warn("{} error", call.request.path(), e)
          call.respond(
            HttpStatusCode.InternalServerError,
            mapOf(
              "status" to "failed"
            )
          )
        }
      }


      get("/api/get/coupon") {
        try {
          val userID = call.principal<LineUserSession>()!!.linePrimaryID
          val isCouponValid = directus.isCouponValid(userID)

          if (isCouponValid){
            call.respond(mapOf(
              "status" to "ok",
              "name" to "先行登録ありがとうクーポン"
            ))
          } else {
            call.respond(mapOf(
              "status" to "failed"
            ))
          }

        } catch (e: Exception) {
          log.warn("{} error", call.request.path(), e)
          call.respond(HttpStatusCode.InternalServerError)
        }
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
            "orderID" to orderIDAndFakeID.second
          )
        )
      }



      get("/api/get/memberID") {
        val session = call.principal<LineUserSession>()

        val macBase64 = Base64
          .getUrlEncoder()
          .withoutPadding()
          .encodeToString(hmac.generateMAC(session!!.linePrimaryID))

        call.respond(mapOf("id" to "${session.linePrimaryID}:$macBase64"))
      }


      get("/create-checkout-session") {

      }

      post("/webhook/payjp") {

      }


    }
  }
}

suspend fun verifyCart(cartID: Int?, userID: String, directus: DirectusService): Boolean { // そのカートが本当に本人のものなのか
  val cart = directus.getCart(userID)
  return cart.data.any { it.id == cartID }
}

suspend fun RoutingContext.verifyAdmin(directus: DirectusService) {
  val primaryID = call.principal<LineUserSession>()!!.linePrimaryID

  if (!directus.isAdminUser(primaryID)) {
    call.respondRedirect("/home")
  }
}
