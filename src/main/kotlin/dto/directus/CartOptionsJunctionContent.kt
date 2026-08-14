package one.nfolio.dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class CartOptionsJunctionContent(val id: Int, val name: String, val price: Int)
