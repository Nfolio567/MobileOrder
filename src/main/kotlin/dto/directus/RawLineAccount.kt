package dto.directus

import kotlinx.serialization.Serializable

@Serializable
data class RawLineAccount(val id: String, val name: String, val accountID: String, val isGetAndNotUsedCoupon: Boolean, val isAdmin: Boolean, val isStaff: Boolean)
