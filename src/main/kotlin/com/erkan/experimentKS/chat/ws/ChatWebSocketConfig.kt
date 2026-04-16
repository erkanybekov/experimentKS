package com.erkan.experimentks.chat.ws

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class ChatWebSocketConfig(
	private val chatWebSocketHandler: ChatWebSocketHandler,
	private val chatHandshakeInterceptor: ChatHandshakeInterceptor,
) : WebSocketConfigurer {

	override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
		registry.addHandler(chatWebSocketHandler, "/ws/chat")
			.addInterceptors(chatHandshakeInterceptor)
			.setAllowedOriginPatterns("*")
	}
}
