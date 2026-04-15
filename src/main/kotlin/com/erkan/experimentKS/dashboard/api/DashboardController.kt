package com.erkan.experimentks.dashboard.api

import com.erkan.experimentks.dashboard.application.DashboardFacade
import com.erkan.experimentks.shared.security.AuthenticatedUser
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/dashboard")
@SecurityRequirement(name = "bearerAuth")
class DashboardController(
	private val dashboardFacade: DashboardFacade,
) {

	@GetMapping
	suspend fun getDashboard(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@RequestParam period: DashboardPeriod,
	): DashboardResponse = dashboardFacade.getDashboard(currentUser.id, period)
}
