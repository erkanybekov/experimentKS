package com.erkan.experimentks.finance.transaction.application

import com.erkan.experimentks.finance.transaction.api.CreateTransactionRequest
import com.erkan.experimentks.finance.transaction.api.TransactionFilter
import com.erkan.experimentks.finance.transaction.api.TransactionResponse
import com.erkan.experimentks.finance.transaction.api.UpdateTransactionRequest
import com.erkan.experimentks.shared.pagination.PageResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TransactionFacade(
	private val transactionService: TransactionService,
	@Qualifier("blockingTaskDispatcher")
	private val blockingTaskDispatcher: CoroutineDispatcher,
) {

	suspend fun listTransactions(
		userId: UUID,
		filter: TransactionFilter,
		pageable: Pageable,
	): PageResponse<TransactionResponse> =
		withContext(blockingTaskDispatcher) {
			transactionService.listTransactions(userId, filter, pageable)
		}

	suspend fun getTransaction(
		userId: UUID,
		transactionId: UUID,
	): TransactionResponse =
		withContext(blockingTaskDispatcher) { transactionService.getTransaction(userId, transactionId) }

	suspend fun createTransaction(
		userId: UUID,
		request: CreateTransactionRequest,
	): TransactionResponse =
		withContext(blockingTaskDispatcher) { transactionService.createTransaction(userId, request) }

	suspend fun updateTransaction(
		userId: UUID,
		transactionId: UUID,
		request: UpdateTransactionRequest,
	): TransactionResponse =
		withContext(blockingTaskDispatcher) {
			transactionService.updateTransaction(userId, transactionId, request)
		}

	suspend fun deleteTransaction(
		userId: UUID,
		transactionId: UUID,
	) {
		withContext(blockingTaskDispatcher) { transactionService.deleteTransaction(userId, transactionId) }
	}
}
