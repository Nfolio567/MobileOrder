package one.nfolio.dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawKdsAccessToken(
  val id: Int,
  val hashedToken: String,
)
