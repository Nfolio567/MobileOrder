package one.nfolio.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.server.application.ApplicationEnvironment
import dto.directus.Directus
import dto.directus.RawLineAccount
import dto.directus.RawOptions
import dto.directus.RawOrderItems
import dto.directus.RawOrders
import dto.regitering.OrderItemsRegister
import dto.directus.RawProducts
import dto.receive.OrderRequest
import dto.regitering.LineIDRegister
import dto.regitering.OrderRegister
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.host
import io.ktor.server.application.port
import one.nfolio.dto.directus.RawCart
import one.nfolio.dto.directus.RawRecommended
import one.nfolio.dto.directus.SingletonDirectus
import one.nfolio.dto.receive.Cart
import one.nfolio.dto.regitering.CartOptionsJunction
import one.nfolio.dto.regitering.CartRegister
import security.FakeOrderID

class DirectusService(private val client: HttpClient, val environment: ApplicationEnvironment) {
  private val directusUrl = String.format(
    "http://%s:%s",
    environment.config.property("directus.host").getString(),
    environment.config.property("directus.port").getString()
  )

  private val accessToken = environment.config.property("directus.access-token").getString()

  suspend fun getProducts(): Directus<RawProducts> { // 商品たち取得
    return client.get("${directusUrl}/items/products") {
      header("Authorization", "Bearer $accessToken")
    }.body<Directus<RawProducts>>()
  }

  suspend fun getOptions(): Directus<RawOptions> { // オプションたち取得
    return client.get("${directusUrl}/items/options") {
      header("Authorization", "Bearer $accessToken")
    }.body<Directus<RawOptions>>()
  }

  suspend fun getRecommendedMessage(): String? { // 本日のおすすめ文章取得
    return client.get("${directusUrl}/items/recommended") {
      header("Authorization", "Bearer $accessToken")
    }.body<SingletonDirectus<RawRecommended>>().data.message
  }

  suspend fun registeringOrder(order: OrderRequest, linePrimaryID: String): Pair<String, String> { // 注文登録
    val orderIDAndFakeID = registeringLinePrimaryIDAndFakeID(linePrimaryID) // 一旦Ordersに登録(LINE ID・偽オーダーIDのみ)

    val orderItemIDs = order.productOptionsList.map { productOptions ->
      client.post("${directusUrl}/items/order_items") {
        header("Authorization", "Bearer $accessToken")
        contentType(ContentType.Application.Json)
        setBody(
          OrderItemsRegister(
            orderIDAndFakeID.first,
            productOptions.productID,
            productOptions.optionIDs,
            productOptions.quantity
          )
        )
      }.body<RawOrderItems>().id
    }

    client.patch("${directusUrl}/items/orders/${orderIDAndFakeID.first}") { // OrdersにOrderItems追加
      header("Authorization", "Bearer $accessToken")
      contentType(ContentType.Application.Json)
      setBody("items" to orderItemIDs)
    }

    return orderIDAndFakeID
  }

  suspend fun registeringCart(cart: Cart, userID: String) { // カート登録
    val productID = 1 // 今、商品は一旦焼きそばオンリーなので静的に定義

    val optionIDs = cart.options.map {option ->
      CartOptionsJunction(option)
    }

    val res = client.post("${directusUrl}/items/cart") {
      expectSuccess = true
      header("Authorization", "Bearer $accessToken")
      contentType(ContentType.Application.Json)
      setBody(CartRegister(
        userID,
        productID,
        optionIDs,
        cart.quantity
      ))
    }
  }

  suspend fun getCart(userID: String): Directus<RawCart> { // カート取得
    return client.get("${directusUrl}/items/cart?filter[userID][_eq]=$userID") {
      header("Authorization", "Bearer $accessToken")
    }.body<Directus<RawCart>>()
  }

  suspend fun updateCart(cartID: Int, quantity: Int) { // カート更新
    client.patch("${directusUrl}/items/cart/$cartID") {
      expectSuccess = true
      header("Authorization", "Bearer $accessToken")
      contentType(ContentType.Application.Json)
      setBody(mapOf(
        "quantity" to quantity
      ))
    }
  }

  suspend fun deleteCart(cartID: Int) { // カート削除
    client.delete("${directusUrl}/items/cart/$cartID") {
      header("Authorization", "Bearer $accessToken")
      expectSuccess = true
    }
  }

  suspend fun isCouponValid(userID: String): Boolean {
    val res = client.get("${directusUrl}/items/line_account?filter[id][_eq]=$userID") {
      header("Authorization", "Bearer $accessToken")
      expectSuccess = true
    }.body<Directus<RawLineAccount>>()

    return res.data[0].isGetAndNotUsedCoupon
  }

  // ユーザーIDをもとにLINEの方のユーザーID取得。なければnullを返す。
  // 認証チェックの時にも使う。なので不正な主キーを送ってくるかもなのでnullableにしてnullを返すようにする
  suspend fun getLineUserID(id: String): String? {
    val specificID = client.get("${directusUrl}/items/line_account?filter[id][_eq]=$id") {
      header("Authorization", "Bearer $accessToken")
    }.body<Directus<RawLineAccount>>().data

    environment.log.info("{}", specificID)

    val id = if (specificID.isEmpty()) null else specificID[0].accountID

    return id
  }

  // LINEの方のユーザーIDをもとにユーザーID取得
  suspend fun getLinePrimaryID(lineUserID: String): String? {
    val data = client.get("${directusUrl}/items/line_account?filter[accountID][_eq]=$lineUserID") {
      header("Authorization", "Bearer $accessToken")
    }.body<Directus<RawLineAccount>>().data

    val primaryID = if (data.isEmpty()) null else data[0].id

    return primaryID
  }

  suspend fun isAdminUser(userID: String): Boolean { // アカウント情報ありきなので、nullチェックはなし
    return client.get("${directusUrl}/items/line_account?filter[id][_eq]=$userID") {
      header("Authorization", "Bearer $accessToken")
    }.body<Directus<RawLineAccount>>().data[0].isAdmin
  }

  suspend fun registeringLineID(id: String): String { // ユーザー登録
    environment.log.info("Registering line ID: {}:{}", environment.config.host, environment.config.port)

    val res = client.post("${directusUrl}/items/line_account") {
      header("Authorization", "Bearer $accessToken")
      contentType(ContentType.Application.Json)
      setBody(LineIDRegister(id))
    }.body<SingletonDirectus<RawLineAccount>>()

    return res.data.id
  }

  // なんかようわからんメソッド。後で書き直す。なんでwhileなんやろ。
  private suspend fun registeringLinePrimaryIDAndFakeID(linePrimaryID: String): Pair<String, String> {
    var running = true;

    lateinit var fakeID: String
    lateinit var orderRes: HttpResponse
    while (running) {
      fakeID = FakeOrderID.generate(6)

      orderRes = client.post("${directusUrl}/items/orders") {
        header("Authorization", "Bearer $accessToken")
        contentType(ContentType.Application.Json)
        setBody(OrderRegister(linePrimaryID, fakeID))
      }

      if (orderRes.status == HttpStatusCode.OK) running = false
    }

    return orderRes.body<RawOrders>().id to fakeID
  }
}