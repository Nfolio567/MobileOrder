package one.nfolio.dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawCart(
    val id: Int,
    val userID: String,
    val optionIDs: List<CartOptionsJunction>,
    val productID: Int,
    val quantity: Int
)
