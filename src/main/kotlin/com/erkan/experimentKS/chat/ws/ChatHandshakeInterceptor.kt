package com.erkan.experimentks.chat.ws

import com.erkan.experimentks.shared.security.AuthenticatedUser
import com.erkan.experimentks.shared.security.JwtAccessTokenAuthenticationService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class ChatHandshakeInterceptor(
	private val jwtAccessTokenAuthenticationService: JwtAccessTokenAuthenticationService,
) : HandshakeInterceptor {

	override fun beforeHandshake(
		request: ServerHttpRequest,
		response: ServerHttpResponse,
		wsHandler: WebSocketHandler,
		attributes: MutableMap<String, Any>,
	): Boolean {
		val token = extractToken(request)
		if (token == null) {
			writeUnauthorized(
				response = response,
				wwwAuthenticateValue = "Bearer",
			)
			return false
		}

		val authenticatedUser = try {
			jwtAccessTokenAuthenticationService.fromBearerToken(token)
		} catch (_: Exception) {
			writeUnauthorized(
				response = response,
				wwwAuthenticateValue = """Bearer error="invalid_token"""",
			)
			return false
		}

		attributes[AUTHENTICATED_USER_ATTRIBUTE] = authenticatedUser
		return true
	}

	override fun afterHandshake(
		request: ServerHttpRequest,
		response: ServerHttpResponse,
		wsHandler: WebSocketHandler,
		exception: Exception?,
	) {
		// no-op
	}

	private fun extractToken(request: ServerHttpRequest): String? {
		return request.headers.getFirst(HttpHeaders.AUTHORIZATION)
			?.takeIf { it.isNotBlank() }
	}

	private fun writeUnauthorized(
		response: ServerHttpResponse,
		wwwAuthenticateValue: String,
	) {
		response.setStatusCode(HttpStatus.UNAUTHORIZED)
		response.headers.add(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticateValue)
	}

	companion object {
		const val AUTHENTICATED_USER_ATTRIBUTE: String = "chat.authenticatedUser"
	}
}
