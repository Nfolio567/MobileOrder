package one.nfolio.dto.directus

import dto.directus.RawOrders
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionOrders(
  val type: String,
  val status: String?,
  val event: String?,
  val error: WsError?,
  val data: List<RawOrders>?,
)
