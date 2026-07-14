package one.nfolio.dto.receive

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCartQuantity(val id: Int, val quantity: Int)
