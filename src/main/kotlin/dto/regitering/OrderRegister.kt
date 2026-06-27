package dto.regitering

import kotlinx.serialization.Serializable

@Serializable
data class OrderRegister(val linePrimaryID: String, val fakeOrderID: String )
