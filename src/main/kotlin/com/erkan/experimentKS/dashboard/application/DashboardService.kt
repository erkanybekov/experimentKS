package com.erkan.experimentks.dashboard.application

import com.erkan.experimentks.dashboard.api.CategoryBreakdownResponse
import com.erkan.experimentks.dashboard.api.DashboardChartEntryResponse
import com.erkan.experimentks.dashboard.api.DashboardPeriod
import com.erkan.experimentks.dashboard.api.DashboardResponse
import com.erkan.experimentks.dashboard.api.DashboardSummaryResponse
import com.erkan.experimentks.finance.TransactionType
import com.erkan.experimentks.finance.transaction.api.TransactionResponse
import com.erkan.experimentks.finance.transaction.api.toResponse
import com.erkan.experimentks.finance.transaction.domain.Transaction
import com.erkan.experimentks.finance.transaction.domain.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Service
class DashboardService(
	private val transactionRepository: TransactionRepository,
	private val clock: Clock,
) {

	@Transactional(readOnly = true)
	fun getDashboard(
		userId: UUID,
		period: DashboardPeriod,
	): DashboardResponse {
		val window = period.toWindow(Instant.now(clock))
		val transactions = transactionRepository
			.findAllByUserIdAndDeletedAtIsNullAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
				userId = userId,
				startInclusive = window.start,
				endExclusive = window.end,
			)
		val recentTransactions = transactionRepository
			.findTop5ByUserIdAndDeletedAtIsNullOrderByOccurredAtDescCreatedAtDesc(userId)
			.map { it.toResponse() }

		val totalIncome = transactions.totalFor(TransactionType.INCOME)
		val totalExpense = transactions.totalFor(TransactionType.EXPENSE)

		return DashboardResponse(
			period = period,
			rangeStart = window.start,
			rangeEnd = window.end,
			summary = DashboardSummaryResponse(
				totalIncome = totalIncome,
				totalExpense = totalExpense,
				balance = money(totalIncome.subtract(totalExpense)),
			),
			chartEntries = buildChartEntries(period, window, transactions),
			categoryBreakdown = buildCategoryBreakdown(transactions),
			recentTransactions = recentTransactions,
		)
	}

	private fun buildChartEntries(
		period: DashboardPeriod,
		window: DashboardWindow,
		transactions: List<Transaction>,
	): List<DashboardChartEntryResponse> =
		window.buckets(period).map { bucket ->
			val bucketTransactions = transactions.filter { transaction ->
				transaction.occurredAt >= bucket.start && transaction.occurredAt < bucket.end
			}
			val income = bucketTransactions.totalFor(TransactionType.INCOME)
			val expense = bucketTransactions.totalFor(TransactionType.EXPENSE)

			DashboardChartEntryResponse(
				label = bucket.label,
				startAt = bucket.start,
				endAt = bucket.end,
				income = income,
				expense = expense,
				balance = money(income.subtract(expense)),
			)
		}

	private fun buildCategoryBreakdown(transactions: List<Transaction>): List<CategoryBreakdownResponse> {
		val totalsByType = transactions
			.groupBy { it.type }
			.mapValues { (_, items) -> items.sumOfAmounts() }

		return transactions
			.groupBy { CategoryBreakdownKey(it.category.id, it.category.name, it.type) }
			.map { (key, items) ->
				val total = items.sumOfAmounts()
				val denominator = totalsByType[key.type] ?: BigDecimal.ZERO
				val percentage = if (denominator.compareTo(BigDecimal.ZERO) == 0) {
					BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				} else {
					total.divide(denominator, 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal(100))
						.setScale(2, RoundingMode.HALF_UP)
				}

				CategoryBreakdownResponse(
					categoryId = key.categoryId,
					categoryName = key.categoryName,
					type = key.type,
					total = money(total),
					percentage = percentage,
				)
			}
			.sortedByDescending { it.total }
	}

	private fun DashboardPeriod.toWindow(now: Instant): DashboardWindow {
		val nowUtc = now.atZone(ZoneOffset.UTC)
		return when (this) {
			DashboardPeriod.WEEK -> {
				val start = nowUtc.toLocalDate()
					.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
					.atStartOfDay(ZoneOffset.UTC)
				DashboardWindow(start.toInstant(), start.plusDays(7).toInstant())
			}

			DashboardPeriod.MONTH -> {
				val start = nowUtc.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC)
				DashboardWindow(start.toInstant(), start.plusMonths(1).toInstant())
			}

			DashboardPeriod.YEAR -> {
				val start = nowUtc.withDayOfYear(1).toLocalDate().atStartOfDay(ZoneOffset.UTC)
				DashboardWindow(start.toInstant(), start.plusYears(1).toInstant())
			}
		}
	}

	private fun DashboardWindow.buckets(period: DashboardPeriod): List<DashboardBucket> {
		val buckets = mutableListOf<DashboardBucket>()
		var cursor = start.atZone(ZoneOffset.UTC)
		val endUtc = end.atZone(ZoneOffset.UTC)

		while (cursor < endUtc) {
			val next = when (period) {
				DashboardPeriod.WEEK,
				DashboardPeriod.MONTH -> cursor.plusDays(1)
				DashboardPeriod.YEAR -> cursor.plusMonths(1)
			}

			val label = when (period) {
				DashboardPeriod.WEEK -> cursor.dayOfWeek.name.take(3)
				DashboardPeriod.MONTH -> "${cursor.month.name.take(3)} ${cursor.dayOfMonth}"
				DashboardPeriod.YEAR -> cursor.month.name.take(3)
			}

			buckets += DashboardBucket(
				label = label,
				start = cursor.toInstant(),
				end = next.toInstant(),
			)
			cursor = next
		}

		return buckets
	}

	private fun Iterable<Transaction>.totalFor(type: TransactionType): BigDecimal =
		money(filter { it.type == type }.sumOfAmounts())

	private fun Iterable<Transaction>.sumOfAmounts(): BigDecimal =
		fold(BigDecimal.ZERO) { acc, transaction -> acc.add(transaction.amount) }

	private fun money(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
}

private data class DashboardWindow(
	val start: Instant,
	val end: Instant,
)

private data class DashboardBucket(
	val label: String,
	val start: Instant,
	val end: Instant,
)

private data class CategoryBreakdownKey(
	val categoryId: UUID,
	val categoryName: String,
	val type: TransactionType,
)
