package one.nfolio.dto.regitering

import kotlinx.serialization.Serializable
import one.nfolio.dto.directus.OptionsRelationship

@Serializable
data class CartRegister(val userID: String, val productID: Int, val optionIDs: List<OptionsRelationship>, val quantity: Int)
