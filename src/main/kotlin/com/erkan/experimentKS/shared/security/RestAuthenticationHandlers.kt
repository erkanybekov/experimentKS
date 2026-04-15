package com.erkan.experimentks.shared.security

import com.erkan.experimentks.shared.api.ApiErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Instant

@Component
class RestAuthenticationEntryPoint(
	private val objectMapper: ObjectMapper,
	private val clock: Clock,
) : AuthenticationEntryPoint {

	override fun commence(
		request: HttpServletRequest,
		response: HttpServletResponse,
		authException: AuthenticationException,
	) {
		writeError(
			response = response,
			status = HttpStatus.UNAUTHORIZED,
			body = ApiErrorResponse(
				code = "UNAUTHORIZED",
				message = "Authentication is required to access this resource.",
				path = request.requestURI,
				timestamp = Instant.now(clock),
			),
		)
	}

	private fun writeError(
		response: HttpServletResponse,
		status: HttpStatus,
		body: ApiErrorResponse,
	) {
		response.status = status.value()
		response.contentType = MediaType.APPLICATION_JSON_VALUE
		response.writer.write(objectMapper.writeValueAsString(body))
	}
}

@Component
class RestAccessDeniedHandler(
	private val objectMapper: ObjectMapper,
	private val clock: Clock,
) : AccessDeniedHandler {

	override fun handle(
		request: HttpServletRequest,
		response: HttpServletResponse,
		accessDeniedException: AccessDeniedException,
	) {
		response.status = HttpStatus.FORBIDDEN.value()
		response.contentType = MediaType.APPLICATION_JSON_VALUE
		response.writer.write(
			objectMapper.writeValueAsString(
				ApiErrorResponse(
					code = "FORBIDDEN",
					message = "You do not have permission to access this resource.",
					path = request.requestURI,
					timestamp = Instant.now(clock),
				),
			),
		)
	}
}
