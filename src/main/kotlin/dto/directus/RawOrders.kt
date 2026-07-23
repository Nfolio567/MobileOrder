package dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawOrders(val id: String, val userID: String, val fakeOrderID: String, val isCooked: Boolean, val isProvided: Boolean, val linePrimaryID: String, val items: List<Int>, val paid: Boolean)
