package com.erkan.experimentks.dashboard.api

import com.erkan.experimentks.finance.TransactionType
import com.erkan.experimentks.finance.transaction.api.TransactionResponse
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class DashboardPeriod {
	WEEK,
	MONTH,
	YEAR,
}

data class DashboardResponse(
	val period: DashboardPeriod,
	val rangeStart: Instant,
	val rangeEnd: Instant,
	val summary: DashboardSummaryResponse,
	val chartEntries: List<DashboardChartEntryResponse>,
	val categoryBreakdown: List<CategoryBreakdownResponse>,
	val recentTransactions: List<TransactionResponse>,
)

data class DashboardSummaryResponse(
	val totalIncome: BigDecimal,
	val totalExpense: BigDecimal,
	val balance: BigDecimal,
)

data class DashboardChartEntryResponse(
	val label: String,
	val startAt: Instant,
	val endAt: Instant,
	val income: BigDecimal,
	val expense: BigDecimal,
	val balance: BigDecimal,
)

data class CategoryBreakdownResponse(
	val categoryId: UUID,
	val categoryName: String,
	val type: TransactionType,
	val total: BigDecimal,
	val percentage: BigDecimal,
)
