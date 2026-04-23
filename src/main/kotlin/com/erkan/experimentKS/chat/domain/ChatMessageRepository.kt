package com.erkan.experimentks.chat.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {
	fun findByRoomIdOrderByCreatedAtDesc(roomId: UUID, pageable: Pageable): Page<ChatMessage>

	fun findByIdAndRoomIdAndSenderUserId(
		id: UUID,
		roomId: UUID,
		senderUserId: UUID,
	): ChatMessage?

	fun findByRoomIdAndSenderUserIdAndClientMessageId(
		roomId: UUID,
		senderUserId: UUID,
		clientMessageId: UUID,
	): ChatMessage?

	fun findTopByRoomIdOrderByCreatedAtDesc(roomId: UUID): ChatMessage?
}
