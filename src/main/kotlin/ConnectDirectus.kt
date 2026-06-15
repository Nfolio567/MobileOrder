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
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ConnectDirectus(val client: HttpClient, val environment: ApplicationEnvironment) {
  private val directusUrl = String.format(
    "%s:%s",
    environment.config.property("directus.host"),
    environment.config.property("directus.port")
  )

  private val accessToken = environment.config.property("directus.access-token")

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

  suspend fun registeringOrder(order: OrderRequest) {
    val lineID = getLinePrimaryUUIDorRegistering(order.lineID)

    val orderRes = client.post("${directusUrl}/items/orders") { // 一旦Ordersに登録(LINE IDのみ)
      header("Authorization", "Bearer $accessToken")
      contentType(ContentType.Application.Json)
      setBody(OrderRegister(lineID))
    }.body<RawOrders>()

    val orderItemIDs = order.productOptionsList.map { productOptions ->
      client.post("${directusUrl}/items/order_items") {
        header("Authorization", "Bearer $accessToken")
        contentType(ContentType.Application.Json)
        setBody(
          OrderItemsRegister(
            orderRes.id,
            productOptions.productID,
            productOptions.optionIDs,
            productOptions.quantity
          )
        )
      }.body<RawOrderItems>().id
    }

    client.patch("${directusUrl}/items/orders/${orderRes.id}") { // OrdersにOrderItems追加
      header("Authorization", "Bearer $accessToken")
      contentType(ContentType.Application.Json)
      setBody("items" to orderItemIDs)
    }
  }

  suspend fun getLinePrimaryUUIDorRegistering(id: String): String {
    val specificID = client.get("${directusUrl}/item/line_account?filter[account_id][_eq]=$id") {
      header("Authorization", "Bearer $accessToken")
    }.body<Directus<RawLineAccount>>().data

    val uuid = if (specificID.isEmpty()) registeringLineID(id) else specificID[0].uuid

    return uuid
  }

  suspend fun registeringLineID(id: String): String {
    val res = client.post("${directusUrl}/items/line_account") {
      contentType(ContentType.Application.Json)
      setBody(LineIDRegister(id))
    }.body<RawLineAccount>()

    return res.uuid
  }
}