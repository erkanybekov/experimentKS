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
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatWebSocketHandler(
	private val objectMapper: ObjectMapper,
	private val chatService: ChatService,
	private val clock: Clock,
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
			sendError(
				session = session,
				code = "AUTHENTICATION_REQUIRED",
				message = "WebSocket session is not authenticated.",
			)
			closeSession(session, AUTHENTICATION_REQUIRED_CLOSE_STATUS)
			return
		}

		val inboundMessage = try {
			objectMapper.readValue(message.payload, ChatSocketInboundMessage::class.java)
		} catch (_: Exception) {
			sendError(
				session = session,
				code = "INVALID_PAYLOAD",
				message = "WebSocket message payload is invalid.",
				details = mapOf("expectedContract" to "ChatSocketInboundMessage"),
			)
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
			sendError(
				session = session,
				code = "ROOM_ID_REQUIRED",
				message = "roomId is required to subscribe.",
				details = mapOf("field" to "roomId"),
			)
			return
		}

		if (!chatService.isMember(currentUser.id, resolvedRoomId)) {
			sendError(session, "CHAT_ROOM_NOT_FOUND", "Chat room $resolvedRoomId was not found.")
			return
		}

		roomSessions.computeIfAbsent(resolvedRoomId) { ConcurrentHashMap.newKeySet() }.add(session)
		sessionRooms.computeIfAbsent(session.id) { ConcurrentHashMap.newKeySet() }.add(resolvedRoomId)
		sendEvent(session, ROOM_SUBSCRIBED_EVENT_TYPE, ChatSocketRoomSubscriptionPayload(resolvedRoomId))
	}

	private fun unsubscribeRoom(
		session: WebSocketSession,
		roomId: UUID?,
	) {
		val resolvedRoomId = roomId ?: run {
			sendError(
				session = session,
				code = "ROOM_ID_REQUIRED",
				message = "roomId is required to unsubscribe.",
				details = mapOf("field" to "roomId"),
			)
			return
		}

		roomSessions[resolvedRoomId]?.remove(session)
		sessionRooms[session.id]?.remove(resolvedRoomId)
		sendEvent(session, ROOM_UNSUBSCRIBED_EVENT_TYPE, ChatSocketRoomSubscriptionPayload(resolvedRoomId))
	}

	private fun sendMessage(
		session: WebSocketSession,
		currentUser: AuthenticatedUser,
		inboundMessage: ChatSocketInboundMessage,
	) {
		val roomId = inboundMessage.roomId ?: run {
			sendError(
				session = session,
				code = "ROOM_ID_REQUIRED",
				message = "roomId is required to send a message.",
				details = mapOf("field" to "roomId"),
			)
			return
		}
		val clientMessageId = inboundMessage.clientMessageId ?: run {
			sendError(
				session = session,
				code = "CLIENT_MESSAGE_ID_REQUIRED",
				message = "clientMessageId is required to send a message.",
				details = mapOf("field" to "clientMessageId"),
			)
			return
		}
		val content = inboundMessage.content ?: run {
			sendError(
				session = session,
				code = "CONTENT_REQUIRED",
				message = "content is required to send a message.",
				details = mapOf("field" to "content"),
			)
			return
		}

		val sendResult = try {
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

		sendEvent(
			session = session,
			type = MESSAGE_ACK_EVENT_TYPE,
			payload = sendResult.message,
		)

		if (sendResult.created) {
			broadcastToRoom(
				roomId = roomId,
				type = MESSAGE_CREATED_EVENT_TYPE,
				payload = sendResult.message,
				excludedSessionId = session.id,
			)
		}
	}

	private fun broadcastToRoom(
		roomId: UUID,
		type: String,
		payload: Any?,
		excludedSessionId: String? = null,
	) {
		roomSessions[roomId].orEmpty()
			.filter { it.isOpen && it.id != excludedSessionId }
			.forEach { sendEvent(it, type, payload) }
	}

	private fun sendError(
		session: WebSocketSession,
		code: String,
		message: String,
		details: Map<String, Any?> = emptyMap(),
	) {
		sendEvent(
			session = session,
			type = ERROR_EVENT_TYPE,
			payload = ChatSocketErrorPayload(
				code = code,
				message = message,
				details = details,
			),
		)
	}

	private fun sendEvent(
		session: WebSocketSession,
		type: String,
		payload: Any?,
	) {
		if (!session.isOpen) {
			return
		}

		val jsonPayload = objectMapper.writeValueAsString(
			ChatSocketEventEnvelope(
				type = type,
				payload = payload,
				eventId = UUID.randomUUID(),
				serverTime = Instant.now(clock),
			),
		)
		synchronized(session) {
			if (session.isOpen) {
				session.sendMessage(TextMessage(jsonPayload))
			}
		}
	}

	private fun closeSession(
		session: WebSocketSession,
		status: CloseStatus,
	) {
		if (!session.isOpen) {
			return
		}

		session.close(status)
	}

	private fun currentUser(session: WebSocketSession): AuthenticatedUser? =
		session.attributes[ChatHandshakeInterceptor.AUTHENTICATED_USER_ATTRIBUTE] as? AuthenticatedUser

	companion object {
		private val AUTHENTICATION_REQUIRED_CLOSE_STATUS = CloseStatus(4401, "AUTHENTICATION_REQUIRED")
		private const val ROOM_SUBSCRIBED_EVENT_TYPE = "room.subscribed"
		private const val ROOM_UNSUBSCRIBED_EVENT_TYPE = "room.unsubscribed"
		private const val MESSAGE_CREATED_EVENT_TYPE = "message.created"
		private const val MESSAGE_ACK_EVENT_TYPE = "message.ack"
		private const val ERROR_EVENT_TYPE = "error"
	}
}
