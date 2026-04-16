package com.erkan.experimentks.chat.ws

import com.erkan.experimentks.chat.api.ChatMessageResponse
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

data class ChatSocketOutboundMessage(
	val type: String,
	val roomId: UUID? = null,
	val message: ChatMessageResponse? = null,
	val error: ChatSocketErrorResponse? = null,
)

data class ChatSocketErrorResponse(
	val code: String,
	val message: String,
)
