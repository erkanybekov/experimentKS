package com.erkan.experimentks.shared.security

import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class JwtAccessTokenAuthenticationService(
	private val jwtDecoder: JwtDecoder,
) {

	fun fromBearerToken(token: String): AuthenticatedUser {
		val normalizedToken = token.removePrefix("Bearer").trim()
		if (normalizedToken.isBlank()) {
			throw BadCredentialsException("Missing bearer token.")
		}

		val jwt = try {
			jwtDecoder.decode(normalizedToken)
		} catch (_: JwtException) {
			throw BadCredentialsException("Invalid access token.")
		}

		return fromDecodedJwt(jwt)
	}

	fun fromDecodedJwt(jwt: Jwt): AuthenticatedUser {
		val tokenType = jwt.getClaimAsString("tokenType")
		if (tokenType != JwtTokenType.ACCESS.name) {
			throw BadCredentialsException("Invalid token type for API access.")
		}

		return AuthenticatedUser(
			id = UUID.fromString(jwt.subject),
		)
	}
}
