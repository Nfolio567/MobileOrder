package dto.receive

import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(val lineIDToken: String, val productOptionsList: List<ProductOptions>)
