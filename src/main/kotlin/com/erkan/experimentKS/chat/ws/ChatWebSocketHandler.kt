package com.erkan.experimentks.chat.ws

import com.erkan.experimentks.chat.application.ChatService
import com.erkan.experimentks.shared.api.ApiException
import com.erkan.experimentks.shared.security.AuthenticatedUser
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.TextMessage as SpringTextMessage
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatWebSocketHandler(
	private val objectMapper: ObjectMapper,
	private val chatService: ChatService,
) : TextWebSocketHandler() {

	private val roomSessions: MutableMap<UUID, MutableSet<WebSocketSession>> = ConcurrentHashMap()
	private val sessionRooms: MutableMap<String, MutableSet<UUID>> = ConcurrentHashMap()

	override fun afterConnectionEstablished(session: WebSocketSession) {
		sessionRooms.putIfAbsent(session.id, ConcurrentHashMap.newKeySet())
	}

	override fun handleTextMessage(
		session: WebSocketSession,
		message: SpringTextMessage,
	) {
		val currentUser = currentUser(session) ?: run {
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing authenticated user."))
			return
		}

		val inboundMessage = try {
			objectMapper.readValue(message.payload, ChatSocketInboundMessage::class.java)
		} catch (_: Exception) {
			sendError(session, "INVALID_PAYLOAD", "WebSocket message payload is invalid.")
			return
		}

		when (inboundMessage.action) {
			ChatSocketAction.SUBSCRIBE_ROOM -> subscribeRoom(session, currentUser, inboundMessage.roomId)
			ChatSocketAction.UNSUBSCRIBE_ROOM -> unsubscribeRoom(session, inboundMessage.roomId)
			ChatSocketAction.SEND_MESSAGE -> sendMessage(session, currentUser, inboundMessage)
		}
	}

	override fun afterConnectionClosed(
		session: WebSocketSession,
		status: CloseStatus,
	) {
		val subscribedRooms = sessionRooms.remove(session.id).orEmpty()
		subscribedRooms.forEach { roomId ->
			roomSessions[roomId]?.remove(session)
			if (roomSessions[roomId].isNullOrEmpty()) {
				roomSessions.remove(roomId)
			}
		}
	}

	private fun subscribeRoom(
		session: WebSocketSession,
		currentUser: AuthenticatedUser,
		roomId: UUID?,
	) {
		val resolvedRoomId = roomId ?: run {
			sendError(session, "ROOM_ID_REQUIRED", "roomId is required to subscribe.")
			return
		}

		if (!chatService.isMember(currentUser.id, resolvedRoomId)) {
			sendError(session, "CHAT_ROOM_NOT_FOUND", "Chat room $resolvedRoomId was not found.")
			return
		}

		roomSessions.computeIfAbsent(resolvedRoomId) { ConcurrentHashMap.newKeySet() }.add(session)
		sessionRooms.computeIfAbsent(session.id) { ConcurrentHashMap.newKeySet() }.add(resolvedRoomId)
		sendEvent(
			session = session,
			payload = ChatSocketOutboundMessage(
				type = "SUBSCRIBED",
				roomId = resolvedRoomId,
			),
		)
	}

	private fun unsubscribeRoom(
		session: WebSocketSession,
		roomId: UUID?,
	) {
		val resolvedRoomId = roomId ?: run {
			sendError(session, "ROOM_ID_REQUIRED", "roomId is required to unsubscribe.")
			return
		}

		roomSessions[resolvedRoomId]?.remove(session)
		sessionRooms[session.id]?.remove(resolvedRoomId)
		sendEvent(
			session = session,
			payload = ChatSocketOutboundMessage(
				type = "UNSUBSCRIBED",
				roomId = resolvedRoomId,
			),
		)
	}

	private fun sendMessage(
		session: WebSocketSession,
		currentUser: AuthenticatedUser,
		inboundMessage: ChatSocketInboundMessage,
	) {
		val roomId = inboundMessage.roomId ?: run {
			sendError(session, "ROOM_ID_REQUIRED", "roomId is required to send a message.")
			return
		}
		val clientMessageId = inboundMessage.clientMessageId ?: run {
			sendError(session, "CLIENT_MESSAGE_ID_REQUIRED", "clientMessageId is required to send a message.")
			return
		}
		val content = inboundMessage.content ?: run {
			sendError(session, "CONTENT_REQUIRED", "content is required to send a message.")
			return
		}

		val messageResponse = try {
			chatService.sendMessage(
				userId = currentUser.id,
				roomId = roomId,
				clientMessageId = clientMessageId,
				content = content,
			)
		} catch (exception: ApiException) {
			sendError(session, exception.code, exception.message)
			return
		}

		val outboundMessage = ChatSocketOutboundMessage(
			type = "MESSAGE_CREATED",
			roomId = roomId,
			message = messageResponse,
		)
		broadcastToRoom(roomId, outboundMessage)
	}

	private fun broadcastToRoom(
		roomId: UUID,
		payload: ChatSocketOutboundMessage,
	) {
		roomSessions[roomId].orEmpty()
			.filter { it.isOpen }
			.forEach { sendEvent(it, payload) }
	}

	private fun sendError(
		session: WebSocketSession,
		code: String,
		message: String,
	) {
		sendEvent(
			session = session,
			payload = ChatSocketOutboundMessage(
				type = "ERROR",
				error = ChatSocketErrorResponse(
					code = code,
					message = message,
				),
			),
		)
	}

	private fun sendEvent(
		session: WebSocketSession,
		payload: ChatSocketOutboundMessage,
	) {
		if (!session.isOpen) {
			return
		}

		val jsonPayload = objectMapper.writeValueAsString(payload)
		synchronized(session) {
			if (session.isOpen) {
				session.sendMessage(TextMessage(jsonPayload))
			}
		}
	}

	private fun currentUser(session: WebSocketSession): AuthenticatedUser? =
		session.attributes[ChatHandshakeInterceptor.AUTHENTICATED_USER_ATTRIBUTE] as? AuthenticatedUser
}
