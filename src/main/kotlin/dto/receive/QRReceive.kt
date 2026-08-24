package one.nfolio.dto.receive

import kotlinx.serialization.Serializable

@Serializable
data class QRReceive(
  val qrContent: String,
)
