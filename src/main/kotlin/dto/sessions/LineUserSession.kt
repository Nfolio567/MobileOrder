package one.nfolio.dto.sessions

import kotlinx.serialization.Serializable

@Serializable
data class LineUserSession(
  val linePrimaryID: String,
)
