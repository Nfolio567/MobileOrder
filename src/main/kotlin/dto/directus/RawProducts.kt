package dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawProducts(val id: String, val name: String, val price: Int, val stockQuantity: Int, val description: String)