package com.erkan.experimentks.finance.transaction.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.UUID

interface TransactionRepository : JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
	@EntityGraph(attributePaths = ["category"])
	override fun findAll(
		spec: Specification<Transaction>,
		pageable: Pageable,
	): Page<Transaction>

	@EntityGraph(attributePaths = ["category"])
	fun findByIdAndUserIdAndDeletedAtIsNull(id: UUID, userId: UUID): Transaction?

	@EntityGraph(attributePaths = ["category"])
	fun findTop5ByUserIdAndDeletedAtIsNullOrderByOccurredAtDescCreatedAtDesc(userId: UUID): List<Transaction>

	@EntityGraph(attributePaths = ["category"])
	fun findAllByUserIdAndDeletedAtIsNullAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
		userId: UUID,
		startInclusive: Instant,
		endExclusive: Instant,
	): List<Transaction>
}
