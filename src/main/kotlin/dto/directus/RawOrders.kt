package dto.directus

data class RawOrders(val id: String, val fakeOrderID: String, val isProvided: Boolean, val linePrimaryID: String, val items: List<Int>)
