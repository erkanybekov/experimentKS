package com.erkan.experimentks.chat.application

import java.util.UUID

data class ChatMessageDeletedEvent(
	val roomId: UUID,
	val messageId: UUID,
)
