package one.nfolio.dto.line

import kotlinx.serialization.Serializable

@Serializable
data class VerifyToken(
  val iss: String,
  val sub: String,
  val aud: String,
  val exp: Int,
  val iat: Int,
  val amr: List<String>,
  val name: String,
  val picture: String
)
