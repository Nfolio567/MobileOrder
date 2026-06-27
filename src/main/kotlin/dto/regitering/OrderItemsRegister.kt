package dto.regitering

import kotlinx.serialization.Serializable

@Serializable
data class OrderItemsRegister(val orderID: String, val productID: Int, val options: List<Int>, val quantity: Int)