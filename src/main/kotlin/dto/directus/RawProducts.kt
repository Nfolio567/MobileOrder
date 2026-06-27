package dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawProducts(val id: Int, val name: String, val pice: Int, val description: String)