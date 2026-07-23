package dto.regitering

import kotlinx.serialization.Serializable

@Serializable
data class LineIDRegister(val accountID: String, val name: String, val isGetAndNotUsedCoupon: Boolean)
