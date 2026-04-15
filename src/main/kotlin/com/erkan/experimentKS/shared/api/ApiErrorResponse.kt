package com.erkan.experimentks.shared.api

import org.springframework.http.HttpStatus
import java.time.Instant

data class ApiErrorResponse(
	val code: String,
	val message: String,
	val path: String,
	val timestamp: Instant,
	val fieldErrors: List<FieldValidationError> = emptyList(),
)

data class FieldValidationError(
	val field: String,
	val message: String,
)

open class ApiException(
	val status: HttpStatus,
	val code: String,
	override val message: String,
) : RuntimeException(message)

class BadRequestException(
	code: String,
	message: String,
) : ApiException(HttpStatus.BAD_REQUEST, code, message)

class ConflictException(
	code: String,
	message: String,
) : ApiException(HttpStatus.CONFLICT, code, message)

class NotFoundException(
	code: String,
	message: String,
) : ApiException(HttpStatus.NOT_FOUND, code, message)

class UnauthorizedException(
	code: String,
	message: String,
) : ApiException(HttpStatus.UNAUTHORIZED, code, message)
