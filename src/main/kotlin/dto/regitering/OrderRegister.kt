package dto.regitering

import kotlinx.serialization.Serializable

@Serializable
data class OrderRegister(
  val fakeOrderID: String,
  val isPos: Boolean,
  val userID: String,
)
