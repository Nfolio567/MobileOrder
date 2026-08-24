package dto.regitering

import kotlinx.serialization.Serializable
import one.nfolio.dto.directus.OptionsRelationship

@Serializable
data class OrderItemsRegister(
  val orderID: String,
  val productID: Int,
  val options: List<OptionsRelationship>,
  val quantity: Int,
)
