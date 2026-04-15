package com.erkan.experimentks.auth.api

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class SignupRequest(
	@field:NotBlank
	@field:Size(max = 120)
	val displayName: String,

	@field:NotBlank
	@field:Email
	@field:Size(max = 255)
	val email: String,

	@field:NotBlank
	@field:Size(min = 8, max = 128)
	val password: String,
)

data class LoginRequest(
	@field:NotBlank
	@field:Email
	val email: String,

	@field:NotBlank
	val password: String,
)

data class RefreshTokenRequest(
	@field:NotBlank
	val refreshToken: String,
)

data class LogoutRequest(
	@field:NotBlank
	val refreshToken: String,
)

data class AuthResponse(
	val tokenType: String,
	val accessToken: String,
	val accessTokenExpiresInSeconds: Long,
	val refreshToken: String,
	val refreshTokenExpiresInSeconds: Long,
)

data class CurrentUserResponse(
	val id: UUID,
	val email: String,
	val displayName: String,
	val createdAt: Instant,
	val updatedAt: Instant,
)
