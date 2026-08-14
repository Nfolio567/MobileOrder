package one.nfolio.dto.receive

import kotlinx.serialization.Serializable

@Serializable
data class Cart(val productID: Int, val options: List<Int>, val quantity: Int)
