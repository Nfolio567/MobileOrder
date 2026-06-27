package one.nfolio.dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawRecommended(val id: Int, val message: String?)
