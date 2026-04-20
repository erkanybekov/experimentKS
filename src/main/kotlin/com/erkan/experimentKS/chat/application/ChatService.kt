package com.erkan.experimentks.chat.application

import com.erkan.experimentks.auth.domain.UserRepository
import com.erkan.experimentks.chat.api.ChatMessageResponse
import com.erkan.experimentks.chat.api.ChatRoomResponse
import com.erkan.experimentks.chat.api.CreateChatRoomRequest
import com.erkan.experimentks.chat.api.toResponse
import com.erkan.experimentks.chat.domain.ChatMessage
import com.erkan.experimentks.chat.domain.ChatMessageRepository
import com.erkan.experimentks.chat.domain.ChatRoom
import com.erkan.experimentks.chat.domain.ChatRoomMember
import com.erkan.experimentks.chat.domain.ChatRoomMemberRepository
import com.erkan.experimentks.chat.domain.ChatRoomRepository
import com.erkan.experimentks.shared.api.BadRequestException
import com.erkan.experimentks.shared.api.NotFoundException
import com.erkan.experimentks.shared.pagination.PageResponse
import com.erkan.experimentks.shared.pagination.toPageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class ChatMessageSendResult(
	val message: ChatMessageResponse,
	val created: Boolean,
)

@Service
class ChatService(
	private val chatRoomRepository: ChatRoomRepository,
	private val chatRoomMemberRepository: ChatRoomMemberRepository,
	private val chatMessageRepository: ChatMessageRepository,
	private val userRepository: UserRepository,
	private val clock: Clock,
) {

	@Transactional(readOnly = true)
	fun listRooms(userId: UUID): List<ChatRoomResponse> =
		chatRoomMemberRepository.findAllForUser(userId)
			.map { member ->
				val room = member.room
				room.toResponse(
					member = member,
					memberCount = chatRoomMemberRepository.countByRoomId(room.id),
					lastMessagePreview = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDesc(room.id)
						?.content
						?.take(MAX_MESSAGE_PREVIEW_LENGTH),
				)
			}

	@Transactional
	fun createRoom(
		userId: UUID,
		request: CreateChatRoomRequest,
	): ChatRoomResponse {
		val normalizedName = request.name.trim()
		if (normalizedName.isBlank()) {
			throw BadRequestException("CHAT_ROOM_NAME_BLANK", "Chat room name must not be blank.")
		}

		val user = userRepository.getReferenceById(userId)
		val joinedAt = Instant.now(clock)
		val room = chatRoomRepository.saveAndFlush(
			ChatRoom(
				createdBy = user,
				name = normalizedName,
			),
		)
		val member = chatRoomMemberRepository.saveAndFlush(
			ChatRoomMember(
				room = room,
				user = user,
				joinedAt = joinedAt,
			),
		)

		return room.toResponse(
			member = member,
			memberCount = 1,
			lastMessagePreview = null,
		)
	}

	@Transactional
	fun joinRoom(
		userId: UUID,
		roomId: UUID,
	): ChatRoomResponse {
		val room = findRoom(roomId)
		val existingMembership = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
		if (existingMembership != null) {
			return room.toResponse(
				member = existingMembership,
				memberCount = chatRoomMemberRepository.countByRoomId(roomId),
				lastMessagePreview = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDesc(roomId)
					?.content
					?.take(MAX_MESSAGE_PREVIEW_LENGTH),
			)
		}

		val joinedAt = Instant.now(clock)
		val membership = chatRoomMemberRepository.saveAndFlush(
			ChatRoomMember(
				room = room,
				user = userRepository.getReferenceById(userId),
				joinedAt = joinedAt,
			),
		)

		return room.toResponse(
			member = membership,
			memberCount = chatRoomMemberRepository.countByRoomId(roomId),
			lastMessagePreview = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDesc(roomId)
				?.content
				?.take(MAX_MESSAGE_PREVIEW_LENGTH),
		)
	}

	@Transactional(readOnly = true)
	fun listMessages(
		userId: UUID,
		roomId: UUID,
		pageable: Pageable,
	): PageResponse<ChatMessageResponse> {
		ensureMembership(userId, roomId)
		val sanitizedPageable = sanitizePageable(pageable)
		return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, sanitizedPageable)
			.map { it.toResponse() }
			.toPageResponse()
	}

	@Transactional
	fun sendMessage(
		userId: UUID,
		roomId: UUID,
		clientMessageId: UUID,
		content: String,
	): ChatMessageSendResult {
		ensureMembership(userId, roomId)
		val normalizedContent = content.trim()
		if (normalizedContent.isBlank()) {
			throw BadRequestException("CHAT_MESSAGE_BLANK", "Chat message must not be blank.")
		}
		if (normalizedContent.length > MAX_MESSAGE_LENGTH) {
			throw BadRequestException("CHAT_MESSAGE_TOO_LONG", "Chat message exceeds the maximum length.")
		}

		val existingMessage = chatMessageRepository.findByRoomIdAndSenderUserIdAndClientMessageId(
			roomId = roomId,
			senderUserId = userId,
			clientMessageId = clientMessageId,
		)
		if (existingMessage != null) {
			return ChatMessageSendResult(
				message = existingMessage.toResponse(),
				created = false,
			)
		}

		val room = findRoom(roomId)
		room.lastActivityAt = Instant.now(clock)
		val message = chatMessageRepository.saveAndFlush(
			ChatMessage(
				room = room,
				senderUser = userRepository.getReferenceById(userId),
				clientMessageId = clientMessageId,
				content = normalizedContent,
			),
		)

		return ChatMessageSendResult(
			message = message.toResponse(),
			created = true,
		)
	}

	@Transactional(readOnly = true)
	fun isMember(
		userId: UUID,
		roomId: UUID,
	): Boolean = chatRoomMemberRepository.existsByRoomIdAndUserId(roomId, userId)

	private fun ensureMembership(
		userId: UUID,
		roomId: UUID,
	) {
		if (!chatRoomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
			throw NotFoundException("CHAT_ROOM_NOT_FOUND", "Chat room $roomId was not found.")
		}
	}

	private fun findRoom(roomId: UUID): ChatRoom =
		chatRoomRepository.findById(roomId)
			.orElseThrow { NotFoundException("CHAT_ROOM_NOT_FOUND", "Chat room $roomId was not found.") }

	private fun sanitizePageable(pageable: Pageable): Pageable =
		PageRequest.of(
			pageable.pageNumber,
			pageable.pageSize.coerceIn(1, 100),
			if (pageable.sort.isSorted) pageable.sort else DEFAULT_SORT,
		)

	companion object {
		private const val MAX_MESSAGE_LENGTH = 2000
		private const val MAX_MESSAGE_PREVIEW_LENGTH = 120
		private val DEFAULT_SORT: Sort = Sort.by(
			Sort.Order.desc("createdAt"),
			Sort.Order.desc("id"),
		)
	}
}
