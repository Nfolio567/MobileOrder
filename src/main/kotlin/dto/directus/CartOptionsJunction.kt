package one.nfolio.dto.directus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CartOptionsJunction(
    @SerialName("options_id")
    val optionsID: CartOptionsJunctionContent
)
