package one.nfolio.dto.receive

import kotlinx.serialization.Serializable

@Serializable
data class UserLogin(val token: String?)
