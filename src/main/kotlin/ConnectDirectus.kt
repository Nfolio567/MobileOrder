package one.nfolio

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
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.host
import io.ktor.server.application.port
import one.nfolio.dto.directus.RawRecommended
import one.nfolio.dto.directus.SingletonDirectus
import security.FakeOrderID

class ConnectDirectus(private val client: HttpClient, val environment: ApplicationEnvironment) {
  private val directusUrl = String.format(
    "http://%s:%s",
    environment.config.property("directus.host").getString(),
    environment.config.property("directus.port").getString()
  )

  private val accessToken = environment.config.property("directus.access-token").getString()

  suspend fun getProducts(): Directus<RawProducts> {
    return client.get("${directusUrl}/items/products") {
      header("Authorization", "Bearer $accessToken")
    }.body<Directus<RawProducts>>()
  }

  suspend fun getOptions(): Directus<RawOptions> {
    return client.get("${directusUrl}/items/options") {
      header("Authorization", "Bearer $accessToken")
    }.body<Directus<RawOptions>>()
  }

  suspend fun getRecommendedMessage(): String? {
    return client.get("${directusUrl}/items/recommended") {
      header("Authorization", "Bearer $accessToken")
    }.body<SingletonDirectus<RawRecommended>>().data.message
  }

  suspend fun registeringOrder(order: OrderRequest, linePrimaryID: String): Pair<String, String> {
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

  // 認証チェックの時にも使う。なので不正な主キーを送ってくるかもなのでnullableにしてnullを返すようにする
  suspend fun getLineUserID(id: String): String? {
    val specificID = client.get("${directusUrl}/items/line_account?filter[id][_eq]=$id") {
      header("Authorization", "Bearer $accessToken")
    }.body<Directus<RawLineAccount>>().data

    val id = if (specificID.isEmpty()) null else specificID[0].accountID

    return id
  }

  suspend fun registeringLineID(id: String): String {
    environment.log.info("Registering line ID: {}:{}", environment.config.host, environment.config.port)

    val res = client.post("${directusUrl}/items/line_account") {
      header("Authorization", "Bearer $accessToken")
      contentType(ContentType.Application.Json)
      setBody(LineIDRegister(id))
    }.body<SingletonDirectus<RawLineAccount>>()

    return res.data.id
  }

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