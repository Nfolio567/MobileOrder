package one.nfolio.dto.response

import dto.directus.RawOptions
import kotlinx.serialization.Serializable

@Serializable
data class AggregatedCart(
  val id: Int,
  val options: List<RawOptions>,
  val product: MinimumProduct,
  val quantity: Int,
)
