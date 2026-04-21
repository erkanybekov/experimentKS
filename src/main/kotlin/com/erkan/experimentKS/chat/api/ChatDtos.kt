package com.erkan.experimentks.chat.api

import com.erkan.experimentks.chat.domain.ChatMessage
import com.erkan.experimentks.chat.domain.ChatRoom
import com.erkan.experimentks.chat.domain.ChatRoomMember
import com.erkan.experimentks.shared.domain.createdAtOrThrow
import com.erkan.experimentks.shared.domain.updatedAtOrThrow
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
	val senderUserId: UUID,
	val senderDisplayName: String,
	val senderAvatarUrl: String?,
	val id: UUID,
	val clientMessageId: UUID,
	val roomId: UUID,
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
		createdAt = createdAtOrThrow,
		updatedAt = updatedAtOrThrow,
	)

fun ChatMessage.toResponse(): ChatMessageResponse =
	ChatMessageResponse(
		senderUserId = senderUser.id,
		senderDisplayName = senderUser.displayName,
		senderAvatarUrl = null,
		id = id,
		clientMessageId = clientMessageId,
		roomId = room.id,
		content = content,
		createdAt = createdAtOrThrow,
		updatedAt = updatedAtOrThrow,
	)
