package com.erkan.experimentks.shared.api

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Clock
import java.time.Instant

@RestControllerAdvice
class RestExceptionHandler(
	private val clock: Clock,
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(ApiException::class)
	fun handleApiException(
		exception: ApiException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(exception.status).body(
			ApiErrorResponse(
				code = exception.code,
				message = exception.message,
				path = request.requestURI,
				timestamp = Instant.now(clock),
			),
		)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidationException(
		exception: MethodArgumentNotValidException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.badRequest().body(
			ApiErrorResponse(
				code = "VALIDATION_ERROR",
				message = "Request validation failed.",
				path = request.requestURI,
				timestamp = Instant.now(clock),
				fieldErrors = exception.bindingResult.allErrors.mapNotNull { error ->
					when (error) {
						is FieldError -> FieldValidationError(
							field = error.field,
							message = error.defaultMessage ?: "Invalid value.",
						)

						else -> null
					}
				},
			),
		)

	@ExceptionHandler(ConstraintViolationException::class)
	fun handleConstraintViolation(
		exception: ConstraintViolationException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.badRequest().body(
			ApiErrorResponse(
				code = "VALIDATION_ERROR",
				message = "Request validation failed.",
				path = request.requestURI,
				timestamp = Instant.now(clock),
				fieldErrors = exception.constraintViolations.map {
					FieldValidationError(
						field = it.propertyPath.toString(),
						message = it.message,
					)
				},
			),
		)

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleMalformedRequest(
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.badRequest().body(
			ApiErrorResponse(
				code = "MALFORMED_REQUEST",
				message = "Request body is missing or malformed.",
				path = request.requestURI,
				timestamp = Instant.now(clock),
			),
		)

	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleMethodArgumentTypeMismatch(
		exception: MethodArgumentTypeMismatchException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		val allowedValues = exception.requiredType
			?.takeIf { it.isEnum }
			?.enumConstants
			?.joinToString(", ")

		val fieldMessage = buildString {
			append("Invalid value '${exception.value}'")
			append(" for parameter '${exception.name}'.")
			if (allowedValues != null) {
				append(" Allowed values: $allowedValues.")
			}
		}

		return ResponseEntity.badRequest().body(
			ApiErrorResponse(
				code = "INVALID_PARAMETER",
				message = "Request parameter is invalid.",
				path = request.requestURI,
				timestamp = Instant.now(clock),
				fieldErrors = listOf(
					FieldValidationError(
						field = exception.name,
						message = fieldMessage,
					),
				),
			),
		)
	}

	@ExceptionHandler(Exception::class)
	fun handleUnexpectedException(
		exception: Exception,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		logger.error("Unhandled exception", exception)

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
			ApiErrorResponse(
				code = "INTERNAL_SERVER_ERROR",
				message = "Unexpected server error.",
				path = request.requestURI,
				timestamp = Instant.now(clock),
			),
		)
	}
}
