package com.erkan.experimentks.auth.application

import com.erkan.experimentks.auth.api.AuthResponse
import com.erkan.experimentks.auth.api.CurrentUserResponse
import com.erkan.experimentks.auth.api.LoginRequest
import com.erkan.experimentks.auth.api.LogoutRequest
import com.erkan.experimentks.auth.api.RefreshTokenRequest
import com.erkan.experimentks.auth.api.SignupRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class AuthFacade(
	private val authService: AuthService,
	@Qualifier("blockingTaskDispatcher")
	private val blockingTaskDispatcher: CoroutineDispatcher,
) {

	suspend fun signup(request: SignupRequest): AuthResponse =
		withContext(blockingTaskDispatcher) { authService.signup(request) }

	suspend fun login(request: LoginRequest): AuthResponse =
		withContext(blockingTaskDispatcher) { authService.login(request) }

	suspend fun refresh(request: RefreshTokenRequest): AuthResponse =
		withContext(blockingTaskDispatcher) { authService.refresh(request) }

	suspend fun logout(request: LogoutRequest) {
		withContext(blockingTaskDispatcher) { authService.logout(request) }
	}

	suspend fun getCurrentUser(userId: java.util.UUID): CurrentUserResponse =
		withContext(blockingTaskDispatcher) { authService.getCurrentUser(userId) }
}
