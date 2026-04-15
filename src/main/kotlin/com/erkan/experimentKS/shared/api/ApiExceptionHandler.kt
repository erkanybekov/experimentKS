package com.erkan.experimentKS.shared.api

import com.erkan.experimentKS.experiment.domain.ExperimentNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Clock
import java.time.Instant

@RestControllerAdvice
class ApiExceptionHandler(
	private val clock: Clock,
) {

	@ExceptionHandler(ExperimentNotFoundException::class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	fun handleExperimentNotFound(exception: ExperimentNotFoundException): ApiErrorResponse =
		ApiErrorResponse(
			code = "EXPERIMENT_NOT_FOUND",
			message = exception.message ?: "Experiment was not found.",
			details = emptyList(),
			timestamp = Instant.now(clock),
		)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	fun handleValidation(exception: MethodArgumentNotValidException): ApiErrorResponse =
		ApiErrorResponse(
			code = "VALIDATION_ERROR",
			message = "Request validation failed.",
			details = exception.bindingResult.allErrors.map { error ->
				when (error) {
					is FieldError -> "${error.field}: ${error.defaultMessage}"
					else -> error.defaultMessage ?: "Validation error"
				}
			},
			timestamp = Instant.now(clock),
		)

	@ExceptionHandler(HttpMessageNotReadableException::class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	fun handleUnreadableRequest(): ApiErrorResponse =
		ApiErrorResponse(
			code = "INVALID_REQUEST_BODY",
			message = "Request body is missing or malformed.",
			details = emptyList(),
			timestamp = Instant.now(clock),
		)
}

data class ApiErrorResponse(
	val code: String,
	val message: String,
	val details: List<String>,
	val timestamp: Instant,
)
