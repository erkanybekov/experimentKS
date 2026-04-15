package com.erkan.experimentks.auth.api

import com.erkan.experimentks.auth.application.AuthFacade
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
	private val authFacade: AuthFacade,
) {

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	suspend fun signup(@Valid @RequestBody request: SignupRequest): AuthResponse =
		authFacade.signup(request)

	@PostMapping("/login")
	suspend fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
		authFacade.login(request)

	@PostMapping("/refresh")
	suspend fun refresh(@Valid @RequestBody request: RefreshTokenRequest): AuthResponse =
		authFacade.refresh(request)

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	suspend fun logout(@Valid @RequestBody request: LogoutRequest) {
		authFacade.logout(request)
	}
}
