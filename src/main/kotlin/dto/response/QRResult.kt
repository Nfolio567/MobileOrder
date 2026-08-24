package one.nfolio.dto.response

import dto.directus.RawOrders
import kotlinx.serialization.Serializable

@Serializable
data class QRResult(
  val isAdministrator: Boolean,
  val isStaff: Boolean,
  val userID: String,
  val name: String,
  val orders: List<RawOrders>,
)
