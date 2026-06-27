package dto.receive

import kotlinx.serialization.Serializable

@Serializable
data class ProductOptions(val productID: Int, val optionIDs: List<Int>, val quantity: Int)
