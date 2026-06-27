package dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class Directus<T>(val data: List<T>)