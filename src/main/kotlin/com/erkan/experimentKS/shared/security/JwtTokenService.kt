package com.erkan.experimentks.shared.security

import com.erkan.experimentks.auth.domain.User
import com.erkan.experimentks.config.JwtProperties
import com.erkan.experimentks.shared.api.UnauthorizedException
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class JwtTokenService(
	private val jwtEncoder: JwtEncoder,
	private val jwtDecoder: JwtDecoder,
	private val jwtProperties: JwtProperties,
	private val clock: Clock,
) {

	fun issueTokenPair(user: User): IssuedTokenPair {
		val now = Instant.now(clock)
		val accessTokenExpiresAt = now.plus(jwtProperties.accessTokenTtl)
		val refreshTokenExpiresAt = now.plus(jwtProperties.refreshTokenTtl)
		val refreshTokenId = UUID.randomUUID()

		val accessToken = encodeToken(
			user = user,
			tokenType = JwtTokenType.ACCESS,
			expiresAt = accessTokenExpiresAt,
		)
		val refreshToken = encodeToken(
			user = user,
			tokenType = JwtTokenType.REFRESH,
			expiresAt = refreshTokenExpiresAt,
			tokenId = refreshTokenId,
		)

		return IssuedTokenPair(
			accessToken = accessToken,
			accessTokenExpiresAt = accessTokenExpiresAt,
			refreshToken = refreshToken,
			refreshTokenExpiresAt = refreshTokenExpiresAt,
			refreshTokenId = refreshTokenId,
		)
	}

	fun parseRefreshToken(token: String): ParsedRefreshToken {
		val jwt = decode(token)
		val tokenType = jwt.getClaimAsString("tokenType")
		if (tokenType != JwtTokenType.REFRESH.name) {
			throw UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid.")
		}

		val tokenId = jwt.id?.let(UUID::fromString)
			?: throw UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid.")

		return ParsedRefreshToken(
			userId = UUID.fromString(jwt.subject),
			tokenId = tokenId,
			expiresAt = jwt.expiresAt
				?: throw UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid."),
		)
	}

	private fun encodeToken(
		user: User,
		tokenType: JwtTokenType,
		expiresAt: Instant,
		tokenId: UUID? = null,
	): String {
		val headers = JwsHeader.with(MacAlgorithm.HS256).build()
		val claims = JwtClaimsSet.builder()
			.issuer(jwtProperties.issuer)
			.subject(user.id.toString())
			.issuedAt(Instant.now(clock))
			.expiresAt(expiresAt)
			.claim("tokenType", tokenType.name)

		tokenId?.let { claims.id(it.toString()) }

		return jwtEncoder.encode(
			JwtEncoderParameters.from(headers, claims.build()),
		).tokenValue
	}

	private fun decode(token: String): Jwt =
		try {
			jwtDecoder.decode(token)
		} catch (_: JwtException) {
			throw UnauthorizedException("INVALID_TOKEN", "Token is invalid or expired.")
		}
}

data class IssuedTokenPair(
	val accessToken: String,
	val accessTokenExpiresAt: Instant,
	val refreshToken: String,
	val refreshTokenExpiresAt: Instant,
	val refreshTokenId: UUID,
)

data class ParsedRefreshToken(
	val userId: UUID,
	val tokenId: UUID,
	val expiresAt: Instant,
)
