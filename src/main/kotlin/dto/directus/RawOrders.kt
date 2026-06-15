package dto.directus

data class RawOrders(val id: String, val isProvided: Boolean, val linePrimaryID: String, val items: List<Int>)
