package com.erkan.experimentks.chat.ws

import java.time.Instant
import java.util.UUID

enum class ChatSocketAction {
	SUBSCRIBE_ROOM,
	UNSUBSCRIBE_ROOM,
	SEND_MESSAGE,
}

data class ChatSocketInboundMessage(
	val action: ChatSocketAction,
	val roomId: UUID?,
	val clientMessageId: UUID?,
	val content: String?,
)

data class ChatSocketEventEnvelope(
	val type: String,
	val payload: Any?,
	val eventId: UUID,
	val serverTime: Instant,
)

data class ChatSocketRoomSubscriptionPayload(
	val roomId: UUID,
)

data class ChatSocketErrorPayload(
	val code: String,
	val message: String,
	val details: Map<String, Any?> = emptyMap(),
)
