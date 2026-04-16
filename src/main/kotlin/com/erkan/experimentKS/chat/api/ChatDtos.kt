package com.erkan.experimentks.chat.api

import com.erkan.experimentks.chat.domain.ChatMessage
import com.erkan.experimentks.chat.domain.ChatRoom
import com.erkan.experimentks.chat.domain.ChatRoomMember
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateChatRoomRequest(
	@field:NotBlank
	@field:Size(max = 120)
	val name: String,
)

data class ChatRoomResponse(
	val id: UUID,
	val name: String,
	val createdByUserId: UUID,
	val joinedAt: Instant,
	val lastActivityAt: Instant?,
	val lastMessagePreview: String?,
	val memberCount: Long,
	val createdAt: Instant,
	val updatedAt: Instant,
)

data class ChatMessageResponse(
	val id: UUID,
	val roomId: UUID,
	val senderUserId: UUID,
	val clientMessageId: UUID,
	val content: String,
	val createdAt: Instant,
	val updatedAt: Instant,
)

fun ChatRoom.toResponse(
	member: ChatRoomMember,
	memberCount: Long,
	lastMessagePreview: String?,
): ChatRoomResponse =
	ChatRoomResponse(
		id = id,
		name = name,
		createdByUserId = createdBy.id,
		joinedAt = member.joinedAt,
		lastActivityAt = lastActivityAt,
		lastMessagePreview = lastMessagePreview,
		memberCount = memberCount,
		createdAt = requireNotNull(createdAt),
		updatedAt = requireNotNull(updatedAt),
	)

fun ChatMessage.toResponse(): ChatMessageResponse =
	ChatMessageResponse(
		id = id,
		roomId = room.id,
		senderUserId = senderUser.id,
		clientMessageId = clientMessageId,
		content = content,
		createdAt = requireNotNull(createdAt),
		updatedAt = requireNotNull(updatedAt),
	)
