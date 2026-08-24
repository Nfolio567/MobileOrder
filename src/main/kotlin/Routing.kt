package one.nfolio

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondResource
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.awaitCancellation
import one.nfolio.dto.receive.*
import one.nfolio.dto.response.AggregatedCart
import one.nfolio.dto.response.ErrorMessage
import one.nfolio.dto.response.MinimumProduct
import one.nfolio.dto.response.QRResult
import one.nfolio.dto.sessions.LineUserSession
import one.nfolio.service.DirectusService
import one.nfolio.service.MyVerifyService
import security.HMAC
import java.util.*

val sessions = mutableSetOf<DefaultWebSocketServerSession>()

fun Application.configureRouting(
  directus: DirectusService,
  hmac: HMAC,
  myVerifyService: MyVerifyService,
) {
  routing {
    staticResources("/", "/static/public")

    tryPost("/login") {
      // 最初にここにリクエスト送らせて、認証チェックする。未ログインならログイン処理をする。
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
              "The token is invalid or null.",
            ),
          )
          log.info("Return 401: {}", call.request.origin.remoteHost)
          return@tryPost
        }
      }

      call.respond(mapOf("redirect" to "/home"))
      log.debug("Redirect request to /home or other: {}", call.request.origin.remoteHost)
    }

    tryPost("/is-exists-user") {
      val res = call.receive<IsExistsUser>()
      val isExists = myVerifyService.isExistsUser(res)
      if (isExists == null) {
        call.respond(HttpStatusCode.Unauthorized)
      }
      call.respond(mapOf("isExists" to isExists))
    }

    authenticate("line-user-session") {
      tryGet("/home") {
        call.respondResource("/static/private/home/index.html")
      }

      tryGet("/member") {
        call.respondResource("/static/private/member/index.html")
      }

      tryGet("/cart") {
        call.respondResource("/static/private/cart/index.html")
      }

      tryGet("/api/get/products") {
        call.respond(
          mapOf(
            "products" to directus.getProducts().data,
          ),
        )
      }

      tryGet("/api/get/options") {
        call.respond(
          mapOf(
            "options" to directus.getOptions().data,
          ),
        )
      }

      tryGet("/api/get/recommended") {
        call.respond(
          mapOf(
            "message" to directus.getRecommendedMessage(),
          ),
        )
      }

      tryGet("/api/get/cart") {
        val userID = call.principal<LineUserSession>()!!.linePrimaryID

        val cartContent = directus.getCart(userID).data
        val products = directus.getProducts()
        val options = directus.getOptions()

        val aggregatedCart =
          cartContent.map { cart ->
            val addedOptions =
              cart.optionIDs.map { id ->
                log.info("{}, {}", options.data, id)
                options.data.find { it.id == id.optionsID.id }!!
              }
            val product = products.data.find { it.id == cart.productID }!!

            AggregatedCart(
              cart.id,
              addedOptions,
              MinimumProduct(product.id, product.name, product.price),
              cart.quantity,
            )
          }

        call.respond(
          mapOf(
            "cart" to aggregatedCart,
          ),
        )
      }

      tryDelete("/api/delete/cart/{id}") {
        val cartID = call.parameters["id"]?.toInt()

        val userID = call.principal<LineUserSession>()!!.linePrimaryID

        if (myVerifyService.verifyCart(cartID, userID, directus)) {
          cartID?.let { directus.deleteCart(it) }

          call.respond(
            mapOf(
              "status" to "success",
            ),
          )
        } else {
          call.respond(HttpStatusCode.Unauthorized)
        }
      }

      patch("/api/patch/cart") {
        // カート内の商品の個数の変更(めんどくさいからまだフロント実装してない）
        try {
          val res = call.receive<UpdateCartQuantity>()

          val userID = call.principal<LineUserSession>()!!.linePrimaryID

          if (myVerifyService.verifyCart(res.id, userID, directus)) {
            directus.updateCart(res.id, res.quantity)

            call.respond(
              mapOf(
                "status" to "success",
              ),
            )
          } else {
            call.respond(HttpStatusCode.Unauthorized)
          }
        } catch (e: Exception) {
          errorResponse(e)
        }
      }

      post("/api/post/cart") {
        try {
          val res = call.receive<Cart>()
          val userID = call.principal<LineUserSession>()!!.linePrimaryID

          directus.registeringCart(res, userID)

          call.respond(
            mapOf(
              "status" to "success",
            ),
          )
        } catch (e: Exception) {
          errorResponse(
            e,
            "status" to "failed",
          )
        }
      }

      tryGet("/api/get/coupon") {
        val userID = call.principal<LineUserSession>()!!.linePrimaryID
        val isCouponValid = directus.isCouponValid(userID)

        if (isCouponValid) {
          call.respond(
            mapOf(
              "status" to "ok",
              "name" to "先行登録ありがとうクーポン",
            ),
          )
        } else {
          call.respond(
            mapOf(
              "status" to "failed",
            ),
          )
        }
      }

      tryPost("/api/post/order") {
        val res = call.receive<OrderRequest>()

        val linePrimaryID = call.principal<LineUserSession>()!!.linePrimaryID
        val orderIDAndFakeID = directus.registeringOrder(res, linePrimaryID)

        call.respond(
          mapOf(
            "orderID" to orderIDAndFakeID.fakeID,
          ),
        )
      }

      tryGet("/api/get/memberID") {
        val session = call.principal<LineUserSession>()

        val macBase64 =
          Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(hmac.generateMAC(session!!.linePrimaryID))

        call.respond(mapOf("id" to "${session.linePrimaryID}:$macBase64"))
      }

      tryGet("/create-checkout-session") {
      }

      tryPost("/webhook/payjp") {
      }


      // 以下staffゾーン

      tryGet("/staff") {
        val userID = verifyStaff(directus)
        call.respondRedirect("/staff/$userID")
      }

      tryGet("/staff/{userID}") {
        staffPathCheck(directus)
        call.respondResource("/static/private/staff/index.html")
      }

      tryGet("/staff/{userID}/qr-result") {
        staffPathCheck(directus)
        call.respondResource("/static/private/qr-result/index.html")
      }

      tryGet("/staff/{userID}/is-admin") {
        staffPathCheck(directus)
        val isAdmin = directus.isAdminUser(call.parameters["userID"]!!)
        call.respond(
          mapOf(
            "isAdmin" to isAdmin,
          ),
        )
      }

      tryPost("/staff/{userID}/qr-result") {
        // フロント側で読み込んだQRコードを受け取って、検証＆結果返し
        staffPathCheck(directus)
        val res = call.receive<QRReceive>()
        val splitContent = res.qrContent.split(":") // primaryID:HMAC
        val userPrimaryID = splitContent[0]
        val decodedHMAC =
          Base64
            .getUrlDecoder()
            .decode(splitContent[1])

        // HMAC検証
        val isCorrectVerifyHMAC = hmac.verify(userPrimaryID, decodedHMAC)
        if (isCorrectVerifyHMAC) {
          val isStaff = directus.isStaffUser(userPrimaryID)
          val isAdministrator = directus.isAdminUser(userPrimaryID)
          val userName = directus.getUserName(userPrimaryID)
          val userOrders = directus.getUserOrder(userPrimaryID)

          call.respond(
            QRResult(
              isAdministrator,
              isStaff,
              userPrimaryID,
              userName,
              userOrders,
            ),
          )
        } else {
          call.respond(emptyMap<String, Any>()) // 空JSON
        }
      }

      tryPost("/staff/{userID}/change-permission") {
        val pathUserID = call.parameters["userID"]!!

        staffPathCheck(directus)
        val isAdmin = directus.isAdminUser(pathUserID)
        if (!isAdmin) {
          call.respond(HttpStatusCode.Unauthorized)
        }

        val res = call.receive<ChangePermission>()
        if (!res.isStaff && res.isAdmin) { // スタッフじゃないのに管理者になろうとしたらバカにする
          call.respond(
            HttpStatusCode.BadRequest,
            ErrorMessage(
              "Are you stupid?",
              "This account isn't even a staff member, so there's no way it can be an admin, you idiot (lol)",
            ),
          )
          return@tryPost
        }

        directus.changeUserPermission(res.targetUserID, res.isAdmin, res.isStaff)
        call.respond(
          mapOf(
            "status" to "success",
          ),
        )
      }

      tryGet("/staff/{userID}/pos") {
        staffPathCheck(directus)
        call.respondResource("/static/private/pos/index.html")
      }

      tryPost("/staff/{userID}/pos") {
        // これがnullの場合はそもそも/homeに飛ばされてるので実質none null
        val userID = staffPathCheck(directus)!!
        val res = call.receive<OrderRequest>()

        val orderIDPair = directus.registeringOrder(res, userID, true)

        call.respond(
          mapOf(
            "orderID" to orderIDPair.fakeID,
          ),
        )
      }
    }

    authenticate("kds-auth") {
      tryGet("/kds") {
        call.respond(
          mapOf(
            "orders" to directus.getOrder()
          )
        )
      }

      webSocket("/kds-ws") {
        sessions.add(this)
        try {
          awaitCancellation()
        } finally {
          sessions -= this
        }
      }
    }
  }
}

// 以下、Routing method

suspend fun RoutingContext.verifyStaff(directus: DirectusService): String? {
  val userID = call.principal<LineUserSession>()!!.linePrimaryID

  if (!directus.isStaffUser(userID)) {
    call.respondRedirect("/home")
    return null
  }

  return userID
}

suspend fun RoutingContext.staffPathCheck(directus: DirectusService): String? {
  val userID = verifyStaff(directus) // この時点でスタッフじゃなきゃ '/home' に強制送還

  if (userID != call.pathParameters["userID"]) {
    call.respondRedirect("/home")
    return null
  }

  return userID
}
