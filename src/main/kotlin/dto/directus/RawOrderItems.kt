package dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawOrderItems(val id: Int, val orderID: String, val productID: Int, val options: List<Int>, val quantity: Int)
