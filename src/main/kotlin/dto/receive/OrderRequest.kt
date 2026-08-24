package one.nfolio.dto.receive

import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
  val productOptionsList: List<ProductOptions>,
)
