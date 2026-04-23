package com.erkan.experimentks.chat.application

import com.erkan.experimentks.auth.domain.UserRepository
import com.erkan.experimentks.chat.api.AddChatRoomMemberRequest
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
import com.erkan.experimentks.shared.domain.createdAtOrThrow
import com.erkan.experimentks.shared.pagination.PageResponse
import com.erkan.experimentks.shared.pagination.toPageResponse
import org.springframework.context.ApplicationEventPublisher
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
	private val applicationEventPublisher: ApplicationEventPublisher,
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

		return toRoomResponse(room, member)
	}

	@Transactional
	fun joinRoom(
		userId: UUID,
		roomId: UUID,
	): ChatRoomResponse {
		val room = findRoom(roomId)
		val existingMembership = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
		if (existingMembership != null) {
			return toRoomResponse(room, existingMembership)
		}

		val joinedAt = Instant.now(clock)
		val membership = chatRoomMemberRepository.saveAndFlush(
			ChatRoomMember(
				room = room,
				user = userRepository.getReferenceById(userId),
				joinedAt = joinedAt,
			),
		)

		return toRoomResponse(room, membership)
	}

	@Transactional
	fun addMember(
		userId: UUID,
		roomId: UUID,
		request: AddChatRoomMemberRequest,
	): ChatRoomResponse {
		val requesterMembership = findMembership(userId, roomId)
		val normalizedEmail = request.email.trim().lowercase()
		if (normalizedEmail.isBlank()) {
			throw BadRequestException("CHAT_MEMBER_EMAIL_BLANK", "Chat member email must not be blank.")
		}

		val invitedUser = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail)
			?: throw NotFoundException("USER_NOT_FOUND", "User with email $normalizedEmail was not found.")
		val room = requesterMembership.room
		val existingMembership = chatRoomMemberRepository.findByRoomIdAndUserId(roomId, invitedUser.id)
		if (existingMembership == null) {
			chatRoomMemberRepository.saveAndFlush(
				ChatRoomMember(
					room = room,
					user = invitedUser,
					joinedAt = Instant.now(clock),
				),
			)
		}

		return toRoomResponse(room, requesterMembership)
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

	@Transactional
	fun deleteMessage(
		userId: UUID,
		roomId: UUID,
		messageId: UUID,
	) {
		val message = findOwnedMessage(userId, roomId, messageId)
		val room = message.room

		chatMessageRepository.delete(message)
		chatMessageRepository.flush()

		room.lastActivityAt = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDesc(roomId)
			?.createdAtOrThrow

		applicationEventPublisher.publishEvent(
			ChatMessageDeletedEvent(
				roomId = roomId,
				messageId = messageId,
			),
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
		findMembership(userId, roomId)
	}

	private fun findRoom(roomId: UUID): ChatRoom =
		chatRoomRepository.findById(roomId)
			.orElseThrow { NotFoundException("CHAT_ROOM_NOT_FOUND", "Chat room $roomId was not found.") }

	private fun findMembership(
		userId: UUID,
		roomId: UUID,
	): ChatRoomMember =
		chatRoomMemberRepository.findByRoomIdAndUserId(roomId, userId)
			?: throw NotFoundException("CHAT_ROOM_NOT_FOUND", "Chat room $roomId was not found.")

	private fun findOwnedMessage(
		userId: UUID,
		roomId: UUID,
		messageId: UUID,
	): ChatMessage =
		chatMessageRepository.findByIdAndRoomIdAndSenderUserId(messageId, roomId, userId)
			?: throw NotFoundException("CHAT_MESSAGE_NOT_FOUND", "Chat message $messageId was not found.")

	private fun sanitizePageable(pageable: Pageable): Pageable =
		PageRequest.of(
			pageable.pageNumber,
			pageable.pageSize.coerceIn(1, 100),
			if (pageable.sort.isSorted) pageable.sort else DEFAULT_SORT,
		)

	private fun toRoomResponse(
		room: ChatRoom,
		member: ChatRoomMember,
	): ChatRoomResponse =
		room.toResponse(
			member = member,
			memberCount = chatRoomMemberRepository.countByRoomId(room.id),
			lastMessagePreview = chatMessageRepository.findTopByRoomIdOrderByCreatedAtDesc(room.id)
				?.content
				?.take(MAX_MESSAGE_PREVIEW_LENGTH),
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
