package one.nfolio.dto.regitering

import kotlinx.serialization.Serializable

@Serializable
data class CartRegister(val userID: String, val productID: Int, val optionIDs: List<CartOptionsJunction>, val quantity: Int)
