package dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawOrders(
  val id: String,
  val userID: String,
  val isPos: Boolean,
  val fakeOrderID: String,
  val isCooked: Boolean,
  val isProvided: Boolean,
  val items: List<RawOrderItems>,
  val paid: Boolean,
)
