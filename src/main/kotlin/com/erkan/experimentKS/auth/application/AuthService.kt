package com.erkan.experimentks.auth.application

import com.erkan.experimentks.auth.api.AuthResponse
import com.erkan.experimentks.auth.api.CurrentUserResponse
import com.erkan.experimentks.auth.api.LoginRequest
import com.erkan.experimentks.auth.api.LogoutRequest
import com.erkan.experimentks.auth.api.RefreshTokenRequest
import com.erkan.experimentks.auth.api.SignupRequest
import com.erkan.experimentks.auth.domain.RefreshToken
import com.erkan.experimentks.auth.domain.RefreshTokenRepository
import com.erkan.experimentks.auth.domain.User
import com.erkan.experimentks.auth.domain.UserRepository
import com.erkan.experimentks.finance.category.application.CategoryService
import com.erkan.experimentks.shared.api.ConflictException
import com.erkan.experimentks.shared.api.BadRequestException
import com.erkan.experimentks.shared.api.NotFoundException
import com.erkan.experimentks.shared.api.UnauthorizedException
import com.erkan.experimentks.shared.security.IssuedTokenPair
import com.erkan.experimentks.shared.security.JwtTokenService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class AuthService(
	private val userRepository: UserRepository,
	private val refreshTokenRepository: RefreshTokenRepository,
	private val passwordEncoder: PasswordEncoder,
	private val jwtTokenService: JwtTokenService,
	private val categoryService: CategoryService,
	private val clock: Clock,
) {

	@Transactional
	fun signup(request: SignupRequest): AuthResponse {
		val email = request.email.trim().lowercase()
		if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
			throw ConflictException("EMAIL_ALREADY_EXISTS", "An account with this email already exists.")
		}

		validatePassword(request.password)

		val user = userRepository.saveAndFlush(
			User(
				email = email,
				displayName = request.displayName.trim(),
				passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
			),
		)

		categoryService.createDefaultCategories(user)

		return issueTokens(user)
	}

	@Transactional
	fun login(request: LoginRequest): AuthResponse {
		val user = userRepository.findByEmailAndDeletedAtIsNull(request.email.trim().lowercase())
			?: throw UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password.")

		if (!passwordEncoder.matches(request.password, user.passwordHash)) {
			throw UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password.")
		}

		return issueTokens(user)
	}

	@Transactional
	fun refresh(request: RefreshTokenRequest): AuthResponse {
		val parsedRefreshToken = jwtTokenService.parseRefreshToken(request.refreshToken)
		val refreshToken = refreshTokenRepository.findByIdAndUserId(
			id = parsedRefreshToken.tokenId,
			userId = parsedRefreshToken.userId,
		) ?: throw UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid.")

		val now = Instant.now(clock)
		if (refreshToken.revokedAt != null || refreshToken.expiresAt.isBefore(now)) {
			throw UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid.")
		}

		refreshToken.revoke(now)
		val user = refreshToken.user
		return issueTokens(user)
	}

	@Transactional
	fun logout(request: LogoutRequest) {
		val parsedRefreshToken = jwtTokenService.parseRefreshToken(request.refreshToken)
		val refreshToken = refreshTokenRepository.findByIdAndUserId(
			id = parsedRefreshToken.tokenId,
			userId = parsedRefreshToken.userId,
		) ?: throw UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid.")

		refreshToken.revoke(Instant.now(clock))
	}

	@Transactional(readOnly = true)
	fun getCurrentUser(userId: java.util.UUID): CurrentUserResponse {
		val user = userRepository.findByIdAndDeletedAtIsNull(userId)
			?: throw NotFoundException("USER_NOT_FOUND", "User $userId was not found.")

		return user.toCurrentUserResponse()
	}

	private fun issueTokens(user: User): AuthResponse {
		val tokenPair = jwtTokenService.issueTokenPair(user)
		persistRefreshToken(user, tokenPair)
		return tokenPair.toResponse()
	}

	private fun persistRefreshToken(
		user: User,
		tokenPair: IssuedTokenPair,
	) {
		refreshTokenRepository.save(
			RefreshToken(
				user = user,
				expiresAt = tokenPair.refreshTokenExpiresAt,
			).apply {
				id = tokenPair.refreshTokenId
			},
		)
	}

	private fun validatePassword(password: String) {
		val hasLetter = password.any { it.isLetter() }
		val hasDigit = password.any { it.isDigit() }
		if (!hasLetter || !hasDigit) {
			throw BadRequestException(
				"WEAK_PASSWORD",
				"Password must contain at least one letter and one digit.",
			)
		}
	}

	private fun IssuedTokenPair.toResponse(): AuthResponse =
		AuthResponse(
			tokenType = "Bearer",
			accessToken = accessToken,
			accessTokenExpiresInSeconds = java.time.Duration.between(Instant.now(clock), accessTokenExpiresAt).seconds,
			refreshToken = refreshToken,
			refreshTokenExpiresInSeconds = java.time.Duration.between(Instant.now(clock), refreshTokenExpiresAt).seconds,
		)

	private fun User.toCurrentUserResponse(): CurrentUserResponse =
		CurrentUserResponse(
			id = id,
			email = email,
			displayName = displayName,
			createdAt = requireNotNull(createdAt),
			updatedAt = requireNotNull(updatedAt),
		)
}
