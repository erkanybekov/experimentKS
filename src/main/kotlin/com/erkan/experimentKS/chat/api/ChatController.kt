package com.erkan.experimentks.chat.api

import com.erkan.experimentks.chat.application.ChatFacade
import com.erkan.experimentks.shared.pagination.PageResponse
import com.erkan.experimentks.shared.security.AuthenticatedUser
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/chat/rooms")
@SecurityRequirement(name = "bearerAuth")
class ChatController(
	private val chatFacade: ChatFacade,
) {

	@GetMapping
	suspend fun listRooms(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
	): List<ChatRoomResponse> = chatFacade.listRooms(currentUser.id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	suspend fun createRoom(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@Valid @RequestBody request: CreateChatRoomRequest,
	): ChatRoomResponse = chatFacade.createRoom(currentUser.id, request)

	@PostMapping("/{roomId}/join")
	suspend fun joinRoom(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@PathVariable roomId: UUID,
	): ChatRoomResponse = chatFacade.joinRoom(currentUser.id, roomId)

	@PostMapping("/{roomId}/members")
	suspend fun addMember(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@PathVariable roomId: UUID,
		@Valid @RequestBody request: AddChatRoomMemberRequest,
	): ChatRoomResponse = chatFacade.addMember(currentUser.id, roomId, request)

	@GetMapping("/{roomId}/messages")
	suspend fun listMessages(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@PathVariable roomId: UUID,
		@PageableDefault(size = 50, sort = ["createdAt"], direction = Sort.Direction.DESC)
		pageable: Pageable,
	): PageResponse<ChatMessageResponse> =
		chatFacade.listMessages(currentUser.id, roomId, pageable)

	@DeleteMapping("/{roomId}/messages/{messageId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	suspend fun deleteMessage(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@PathVariable roomId: UUID,
		@PathVariable messageId: UUID,
	) {
		chatFacade.deleteMessage(currentUser.id, roomId, messageId)
	}
}
