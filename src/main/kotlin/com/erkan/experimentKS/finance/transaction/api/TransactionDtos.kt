package com.erkan.experimentks.finance.transaction.api

import com.erkan.experimentks.finance.TransactionType
import com.erkan.experimentks.finance.transaction.domain.Transaction
import com.erkan.experimentks.shared.domain.createdAtOrThrow
import com.erkan.experimentks.shared.domain.updatedAtOrThrow
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateTransactionRequest(
	@field:NotNull
	val categoryId: UUID,

	@field:NotNull
	val type: TransactionType?,

	@field:DecimalMin("0.01")
	val amount: BigDecimal,

	@field:Size(max = 500)
	val note: String?,

	@field:NotNull
	val occurredAt: Instant,
)

data class UpdateTransactionRequest(
	@field:NotNull
	val categoryId: UUID,

	@field:NotNull
	val type: TransactionType?,

	@field:DecimalMin("0.01")
	val amount: BigDecimal,

	@field:Size(max = 500)
	val note: String?,

	@field:NotNull
	val occurredAt: Instant,
)

data class TransactionCategoryResponse(
	val id: UUID,
	val name: String,
	val type: TransactionType,
)

data class TransactionResponse(
	val id: UUID,
	val type: TransactionType,
	val amount: BigDecimal,
	val note: String?,
	val occurredAt: Instant,
	val createdAt: Instant,
	val updatedAt: Instant,
	val deletedAt: Instant?,
	val category: TransactionCategoryResponse,
)

data class TransactionFilter(
	val from: Instant?,
	val to: Instant?,
	val categoryId: UUID?,
	val type: TransactionType?,
)

fun Transaction.toResponse(): TransactionResponse =
	TransactionResponse(
		id = id,
		type = type,
		amount = amount,
		note = note,
		occurredAt = occurredAt,
		createdAt = createdAtOrThrow,
		updatedAt = updatedAtOrThrow,
		deletedAt = deletedAt,
		category = TransactionCategoryResponse(
			id = category.id,
			name = category.name,
			type = category.type,
		),
	)
