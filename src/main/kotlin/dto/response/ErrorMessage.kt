package one.nfolio.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class ErrorMessage(val title: String, val description: String)
