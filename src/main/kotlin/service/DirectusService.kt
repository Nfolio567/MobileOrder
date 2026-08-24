package one.nfolio.service

import dto.directus.Directus
import dto.directus.RawLineAccount
import dto.directus.RawOptions
import dto.directus.RawOrderItems
import dto.directus.RawOrders
import dto.directus.RawProducts
import dto.regitering.LineIDRegister
import dto.regitering.OrderItemsRegister
import dto.regitering.OrderRegister
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.application.host
import io.ktor.server.application.port
import one.nfolio.dto.directus.OptionsRelationship
import one.nfolio.dto.directus.RawCart
import one.nfolio.dto.directus.RawKdsAccessToken
import one.nfolio.dto.directus.RawRecommended
import one.nfolio.dto.directus.SingletonDirectus
import one.nfolio.dto.receive.Cart
import one.nfolio.dto.receive.OrderRequest
import one.nfolio.dto.regitering.CartRegister
import security.FakeOrderID

class DirectusService(
  private val client: HttpClient,
  val environment: ApplicationEnvironment,
) {
  private val directusUrl =
    String.format(
      "http://localhost:%s",
      environment.config.property("directus.port").getString(),
    )

  private val accessToken = environment.config.property("directus.access-token").getString()

  suspend fun getProducts(): Directus<RawProducts> { // 商品たち取得
    return authorizableGet("$directusUrl/items/products").body<Directus<RawProducts>>()
  }

  suspend fun getOptions(): Directus<RawOptions> { // オプションたち取得
    return authorizableGet("$directusUrl/items/options").body<Directus<RawOptions>>()
  }

  suspend fun getRecommendedMessage(): String? { // 本日のおすすめ文章取得
    return authorizableGet("$directusUrl/items/recommended").body<SingletonDirectus<RawRecommended>>().data.message
  }

  suspend fun getOrder(): Directus<RawOrders> {
    return authorizableGet("${directusUrl}/items/orders?fields=*,items.*,items.options.options_id.*,items.productID.*").body<Directus<RawOrders>>()
  }

  suspend fun registeringOrder(
    order: OrderRequest,
    linePrimaryID: String,
    isPos: Boolean = false,
  ): OrderIDPair { // 注文登録
    val orderIDAndFakeID = registeringLinePrimaryIDAndFakeID(linePrimaryID, isPos) // 一旦Ordersに登録(LINE ID・偽オーダーIDのみ)

    val orderItemIDs =
      order.productOptionsList.map { productOptions ->
        val optionsRelationships =
          productOptions.optionIDs.map { optionID ->
            OptionsRelationship(optionID)
          }

        authorizablePost("$directusUrl/items/order_items") {
          contentType(ContentType.Application.Json)
          setBody(
            OrderItemsRegister(
              orderIDAndFakeID.orderID,
              productOptions.productID,
              optionsRelationships,
              productOptions.quantity,
            ),
          )
        }.body<SingletonDirectus<RawOrderItems>>().data.id
      }

    authorizablePatch("$directusUrl/items/orders/${orderIDAndFakeID.orderID}") {
      // OrdersにOrderItems追加
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "items" to orderItemIDs,
        ),
      )
    }

    return orderIDAndFakeID
  }

  suspend fun registeringCart(
    cart: Cart,
    userID: String,
  ) { // カート登録
    val optionIDs =
      cart.options.map { option ->
        OptionsRelationship(option)
      }

    environment.log.info("{}", optionIDs)

    authorizablePost("$directusUrl/items/cart") {
      contentType(ContentType.Application.Json)
      setBody(
        CartRegister(
          userID,
          cart.productID,
          optionIDs,
          cart.quantity,
        ),
      )
    }
  }

  suspend fun getCart(userID: String): Directus<RawCart> { // カート取得
    return authorizableGet("$directusUrl/items/cart?filter[userID][_eq]=$userID&fields=*,optionIDs.options_id.*") {
    }.body<Directus<RawCart>>()
  }

  suspend fun updateCart(
    cartID: Int,
    quantity: Int,
  ) { // カート更新
    authorizablePatch("$directusUrl/items/cart/$cartID") {
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "quantity" to quantity,
        ),
      )
    }
  }

  suspend fun deleteCart(cartID: Int) { // カート削除
    authorizableDelete("$directusUrl/items/cart/$cartID") {}
  }

  suspend fun isCouponValid(userID: String): Boolean {
    val res = authorizableGet("$directusUrl/items/line_account?filter[id][_eq]=$userID").body<Directus<RawLineAccount>>()

    return res.data[0].isGetAndNotUsedCoupon
  }

  // ユーザーIDをもとにLINEの方のユーザーID取得。なければnullを返す。
  // 認証チェックの時にも使う。なので不正な主キーを送ってくるかもなのでnullableにしてnullを返すようにする
  suspend fun getLineUserID(id: String): String? {
    val specificID = authorizableGet("$directusUrl/items/line_account?filter[id][_eq]=$id").body<Directus<RawLineAccount>>().data

    environment.log.info("{}", specificID)

    val id = if (specificID.isEmpty()) null else specificID[0].accountID

    return id
  }

  // LINEの方のユーザーIDをもとにユーザーID取得
  suspend fun getLinePrimaryID(lineUserID: String): String? {
    val data = authorizableGet("$directusUrl/items/line_account?filter[accountID][_eq]=$lineUserID").body<Directus<RawLineAccount>>().data

    val primaryID = if (data.isEmpty()) null else data[0].id

    return primaryID
  }

  suspend fun getUserName(userID: String): String =
    authorizableGet("$directusUrl/items/line_account?filter[id][_eq]=$userID")
      .body<Directus<RawLineAccount>>()
      .data
      .first()
      .name

  suspend fun isAdminUser(userID: String): Boolean { // アカウント情報ありきなので、nullチェックはなし
    return authorizableGet("$directusUrl/items/line_account?filter[id][_eq]=$userID").body<Directus<RawLineAccount>>().data[0].isAdmin
  }

  suspend fun isStaffUser(userID: String): Boolean =
    authorizableGet("$directusUrl/items/line_account?filter[id][_eq]=$userID").body<Directus<RawLineAccount>>().data[0].isStaff

  suspend fun changeUserPermission(
    userID: String,
    isAdmin: Boolean,
    isStaff: Boolean,
  ) {
    authorizablePatch("$directusUrl/items/line_account/$userID") {
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "isAdmin" to isAdmin,
          "isStaff" to isStaff,
        ),
      )
    }
  }

  suspend fun registeringLineIDAndName(
    id: String,
    name: String,
    isGetAndNotUsedCoupon: Boolean,
  ): String { // ユーザー登録
    environment.log.info("Registering line ID: {}:{}", environment.config.host, environment.config.port)

    val res =
      authorizablePost("$directusUrl/items/line_account") {
        contentType(ContentType.Application.Json)
        setBody(LineIDRegister(id, name, isGetAndNotUsedCoupon))
      }.body<SingletonDirectus<RawLineAccount>>()

    return res.data.id
  }

  suspend fun getUserOrder(userID: String): List<RawOrders> =
    authorizableGet("$directusUrl/items/orders?filter[userID][_eq]=$userID&fields=*,items.*,items.options.options_id.*,items.productID.*").body<Directus<RawOrders>>().data

  suspend fun getKdsHashedToken(): String =
    authorizableGet("$directusUrl/items/kds_access_token").body<SingletonDirectus<RawKdsAccessToken>>().data.hashedToken


  // OrderItemsを登録するために、一旦ユーザーIDとFakeOrderIDをOrdersにinsertする。
  // 返ってきたOrderID, 生成したOrderFakeIDを返す
  private suspend fun registeringLinePrimaryIDAndFakeID(
    linePrimaryID: String,
    isPos: Boolean,
  ): OrderIDPair {
    var running = true

    lateinit var fakeOrderID: String
    lateinit var orderRes: HttpResponse
    while (running) { // FakeOrderIDはランダム文字数列なので、ほかのIDと被らなくなるまでループ
      fakeOrderID = FakeOrderID.generate(6)

      orderRes =
        authorizablePost("$directusUrl/items/orders") {
          contentType(ContentType.Application.Json)
          setBody(OrderRegister(fakeOrderID, isPos, linePrimaryID))
        }

      if (orderRes.status == HttpStatusCode.OK) running = false
    }

    return OrderIDPair(
      orderRes.body<SingletonDirectus<RawOrders>>().data.id,
      fakeOrderID,
    )
  }

  private suspend fun authorizableGet(
    path: String,
    block: HttpRequestBuilder.() -> Unit = {},
  ): HttpResponse =
    client.get(path) {
      expectSuccess = true
      header("Authorization", "Bearer $accessToken")
      block()
    }

  private suspend fun authorizablePost(
    path: String,
    block: HttpRequestBuilder.() -> Unit = {},
  ): HttpResponse =
    client.post(path) {
      expectSuccess = true
      header("Authorization", "Bearer $accessToken")
      block()
    }

  private suspend fun authorizablePatch(
    path: String,
    block: HttpRequestBuilder.() -> Unit = {},
  ): HttpResponse =
    client.patch(path) {
      expectSuccess = true
      header("Authorization", "Bearer $accessToken")
      block()
    }

  private suspend fun authorizableDelete(
    path: String,
    block: HttpRequestBuilder.() -> Unit = {},
  ): HttpResponse =
    client.delete(path) {
      expectSuccess = true
      header("Authorization", "Bearer $accessToken")
      block()
    }
}
