package one.nfolio.dto.receive

import kotlinx.serialization.Serializable

@Serializable
data class ChangePermission(val targetUserID: String, val isAdmin: Boolean, val isStaff: Boolean)
