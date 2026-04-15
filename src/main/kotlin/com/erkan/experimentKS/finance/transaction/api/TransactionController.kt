package com.erkan.experimentks.finance.transaction.api

import com.erkan.experimentks.shared.pagination.PageResponse
import com.erkan.experimentks.shared.security.AuthenticatedUser
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/v1/transactions")
@SecurityRequirement(name = "bearerAuth")
class TransactionController(
	private val transactionFacade: com.erkan.experimentks.finance.transaction.application.TransactionFacade,
) {

	@GetMapping
	suspend fun listTransactions(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		from: Instant?,
		@RequestParam(required = false)
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		to: Instant?,
		@RequestParam(required = false) categoryId: UUID?,
		@RequestParam(required = false) type: com.erkan.experimentks.finance.TransactionType?,
		@PageableDefault(size = 20, sort = ["occurredAt"], direction = Sort.Direction.DESC)
		pageable: Pageable,
	): PageResponse<TransactionResponse> =
		transactionFacade.listTransactions(
			userId = currentUser.id,
			filter = TransactionFilter(
				from = from,
				to = to,
				categoryId = categoryId,
				type = type,
			),
			pageable = pageable,
		)

	@GetMapping("/{id}")
	suspend fun getTransaction(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@PathVariable id: UUID,
	): TransactionResponse = transactionFacade.getTransaction(currentUser.id, id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	suspend fun createTransaction(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@Valid @RequestBody request: CreateTransactionRequest,
	): TransactionResponse = transactionFacade.createTransaction(currentUser.id, request)

	@PutMapping("/{id}")
	suspend fun updateTransaction(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@PathVariable id: UUID,
		@Valid @RequestBody request: UpdateTransactionRequest,
	): TransactionResponse = transactionFacade.updateTransaction(currentUser.id, id, request)

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	suspend fun deleteTransaction(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@PathVariable id: UUID,
	) {
		transactionFacade.deleteTransaction(currentUser.id, id)
	}
}
