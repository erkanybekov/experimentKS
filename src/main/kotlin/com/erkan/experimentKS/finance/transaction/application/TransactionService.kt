package com.erkan.experimentks.finance.transaction.application

import com.erkan.experimentks.auth.domain.UserRepository
import com.erkan.experimentks.finance.category.application.CategoryService
import com.erkan.experimentks.finance.transaction.api.CreateTransactionRequest
import com.erkan.experimentks.finance.transaction.api.TransactionFilter
import com.erkan.experimentks.finance.transaction.api.TransactionResponse
import com.erkan.experimentks.finance.transaction.api.UpdateTransactionRequest
import com.erkan.experimentks.finance.transaction.api.toResponse
import com.erkan.experimentks.finance.transaction.domain.Transaction
import com.erkan.experimentks.finance.transaction.domain.TransactionRepository
import com.erkan.experimentks.finance.transaction.domain.TransactionSpecifications
import com.erkan.experimentks.shared.api.BadRequestException
import com.erkan.experimentks.shared.api.NotFoundException
import com.erkan.experimentks.shared.pagination.PageResponse
import com.erkan.experimentks.shared.pagination.toPageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class TransactionService(
	private val transactionRepository: TransactionRepository,
	private val userRepository: UserRepository,
	private val categoryService: CategoryService,
	private val clock: Clock,
) {

	@Transactional(readOnly = true)
	fun listTransactions(
		userId: UUID,
		filter: TransactionFilter,
		pageable: Pageable,
	): PageResponse<TransactionResponse> {
		val pageRequest = sanitizePageable(pageable)
		var specification: Specification<Transaction> = TransactionSpecifications.activeForUser(userId)

		filter.from?.let { specification = specification.and(TransactionSpecifications.occurredFrom(it)) }
		filter.to?.let { specification = specification.and(TransactionSpecifications.occurredTo(it)) }
		filter.categoryId?.let { specification = specification.and(TransactionSpecifications.withCategory(it)) }
		filter.type?.let { specification = specification.and(TransactionSpecifications.withType(it)) }

		return transactionRepository.findAll(specification, pageRequest)
			.map { it.toResponse() }
			.toPageResponse()
	}

	@Transactional(readOnly = true)
	fun getTransaction(
		userId: UUID,
		transactionId: UUID,
	): TransactionResponse = findOwnedTransaction(userId, transactionId).toResponse()

	@Transactional
	fun createTransaction(
		userId: UUID,
		request: CreateTransactionRequest,
	): TransactionResponse {
		val category = categoryService.findOwnedActiveCategory(userId, request.categoryId)
		validateCategoryType(category.type.name, request.type.name)

		val user = userRepository.getReferenceById(userId)
		return transactionRepository.saveAndFlush(
			Transaction(
				user = user,
				category = category,
				type = request.type,
				note = request.note?.trim()?.takeIf { it.isNotBlank() },
				amount = request.amount,
				occurredAt = request.occurredAt,
			),
		).toResponse()
	}

	@Transactional
	fun updateTransaction(
		userId: UUID,
		transactionId: UUID,
		request: UpdateTransactionRequest,
	): TransactionResponse {
		val transaction = findOwnedTransaction(userId, transactionId)
		val category = categoryService.findOwnedActiveCategory(userId, request.categoryId)
		validateCategoryType(category.type.name, request.type.name)

		transaction.category = category
		transaction.type = request.type
		transaction.note = request.note?.trim()?.takeIf { it.isNotBlank() }
		transaction.amount = request.amount
		transaction.occurredAt = request.occurredAt

		return transactionRepository.saveAndFlush(transaction).toResponse()
	}

	@Transactional
	fun deleteTransaction(
		userId: UUID,
		transactionId: UUID,
	) {
		val transaction = findOwnedTransaction(userId, transactionId)
		transaction.deletedAt = Instant.now(clock)
	}

	private fun findOwnedTransaction(
		userId: UUID,
		transactionId: UUID,
	): Transaction =
		transactionRepository.findByIdAndUserIdAndDeletedAtIsNull(transactionId, userId)
			?: throw NotFoundException("TRANSACTION_NOT_FOUND", "Transaction $transactionId was not found.")

	private fun sanitizePageable(pageable: Pageable): Pageable =
		PageRequest.of(
			pageable.pageNumber,
			pageable.pageSize.coerceIn(1, 100),
			if (pageable.sort.isSorted) pageable.sort else DEFAULT_SORT,
		)

	private fun validateCategoryType(
		categoryType: String,
		requestType: String,
	) {
		if (categoryType != requestType) {
			throw BadRequestException(
				"CATEGORY_TYPE_MISMATCH",
				"Transaction type must match the selected category type.",
			)
		}
	}

	companion object {
		private val DEFAULT_SORT: Sort = Sort.by(
			Sort.Order.desc("occurredAt"),
			Sort.Order.desc("createdAt"),
		)
	}
}
