package one.nfolio.dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class WsError(val code: String, val message: String)
