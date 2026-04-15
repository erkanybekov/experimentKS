package com.erkan.experimentks.dashboard.application

import com.erkan.experimentks.dashboard.api.DashboardPeriod
import com.erkan.experimentks.dashboard.api.DashboardResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DashboardFacade(
	private val dashboardService: DashboardService,
	@Qualifier("blockingTaskDispatcher")
	private val blockingTaskDispatcher: CoroutineDispatcher,
) {

	suspend fun getDashboard(
		userId: UUID,
		period: DashboardPeriod,
	): DashboardResponse =
		withContext(blockingTaskDispatcher) { dashboardService.getDashboard(userId, period) }
}
