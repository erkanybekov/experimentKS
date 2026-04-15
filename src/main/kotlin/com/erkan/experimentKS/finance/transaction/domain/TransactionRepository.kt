package com.erkan.experimentks.finance.transaction.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.Instant
import java.util.UUID

interface TransactionRepository : JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
	fun findByIdAndUserIdAndDeletedAtIsNull(id: UUID, userId: UUID): Transaction?

	fun findTop5ByUserIdAndDeletedAtIsNullOrderByOccurredAtDescCreatedAtDesc(userId: UUID): List<Transaction>

	fun findAllByUserIdAndDeletedAtIsNullAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
		userId: UUID,
		startInclusive: Instant,
		endExclusive: Instant,
	): List<Transaction>
}
