package com.erkan.experimentks.shared.security

import java.security.Principal
import java.util.UUID

data class AuthenticatedUser(
	val id: UUID,
) : Principal {
	override fun getName(): String = id.toString()
}

enum class JwtTokenType {
	ACCESS,
	REFRESH,
}
