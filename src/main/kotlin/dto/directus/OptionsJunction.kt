package one.nfolio.dto.directus

import dto.directus.RawOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OptionsJunction(
  @SerialName("options_id")
  val optionsID: RawOptions,
)
