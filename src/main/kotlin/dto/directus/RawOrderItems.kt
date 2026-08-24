package dto.directus

import kotlinx.serialization.Serializable
import one.nfolio.dto.directus.OptionsJunction

@Serializable
data class RawOrderItems(
  val id: Int,
  val orderID: String,
  val productID: RawProducts,
  val options: List<OptionsJunction>,
  val quantity: Int,
)
