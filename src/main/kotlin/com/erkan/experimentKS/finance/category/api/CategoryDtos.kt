package com.erkan.experimentks.finance.category.api

import com.erkan.experimentks.finance.TransactionType
import com.erkan.experimentks.finance.category.domain.Category
import com.erkan.experimentks.shared.domain.createdAtOrThrow
import com.erkan.experimentks.shared.domain.updatedAtOrThrow
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateCategoryRequest(
	@field:NotBlank
	@field:Size(max = 80)
	val name: String,

	@field:NotNull
	val type: TransactionType?,
)

data class CategoryResponse(
	val id: UUID,
	val name: String,
	val type: TransactionType,
	val createdAt: Instant,
	val updatedAt: Instant,
	val deletedAt: Instant?,
)

fun Category.toResponse(): CategoryResponse =
	CategoryResponse(
		id = id,
		name = name,
		type = type,
		createdAt = createdAtOrThrow,
		updatedAt = updatedAtOrThrow,
		deletedAt = deletedAt,
	)
