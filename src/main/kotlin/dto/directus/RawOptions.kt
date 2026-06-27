package dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawOptions(val id: Int, val name: String, val price: Int)
