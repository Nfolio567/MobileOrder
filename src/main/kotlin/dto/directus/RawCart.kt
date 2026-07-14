package one.nfolio.dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawCart(val id: Int, val userID: String, val optionIDs: List<Int>, val productID: Int, val quantity: Int)
