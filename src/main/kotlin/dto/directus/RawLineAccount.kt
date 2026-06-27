package dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawLineAccount(val id: String, val accountID: String)
