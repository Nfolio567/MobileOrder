package one.nfolio.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class MinimumProduct(
  val id: Int,
  val name: String,
  val price: Int,
)
