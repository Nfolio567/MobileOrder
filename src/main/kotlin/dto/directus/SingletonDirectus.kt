package one.nfolio.dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class SingletonDirectus<T>(val data: T)
