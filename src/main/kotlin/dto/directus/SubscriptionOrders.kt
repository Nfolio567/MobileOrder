package one.nfolio.dto.directus

import dto.directus.RawOrders
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionOrders(
  val type: String,
  val status: String? = null,
  val event: String? = null,
  val error: WsError? = null,
  val data: List<RawOrders>? = null,
)
