package com.erkan.experimentks.auth.api

import com.erkan.experimentks.auth.application.AuthFacade
import com.erkan.experimentks.shared.security.AuthenticatedUser
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me")
@SecurityRequirement(name = "bearerAuth")
class CurrentUserController(
	private val authFacade: AuthFacade,
) {

	@GetMapping
	suspend fun getCurrentUser(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
	): CurrentUserResponse = authFacade.getCurrentUser(currentUser.id)
}
