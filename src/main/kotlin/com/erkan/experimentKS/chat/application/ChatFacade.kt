package com.erkan.experimentks.chat.application

import com.erkan.experimentks.chat.api.ChatMessageResponse
import com.erkan.experimentks.chat.api.ChatRoomResponse
import com.erkan.experimentks.chat.api.CreateChatRoomRequest
import com.erkan.experimentks.shared.pagination.PageResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ChatFacade(
	private val chatService: ChatService,
	@Qualifier("blockingTaskDispatcher")
	private val blockingTaskDispatcher: CoroutineDispatcher,
) {

	suspend fun listRooms(userId: UUID): List<ChatRoomResponse> =
		withContext(blockingTaskDispatcher) { chatService.listRooms(userId) }

	suspend fun createRoom(
		userId: UUID,
		request: CreateChatRoomRequest,
	): ChatRoomResponse =
		withContext(blockingTaskDispatcher) { chatService.createRoom(userId, request) }

	suspend fun joinRoom(
		userId: UUID,
		roomId: UUID,
	): ChatRoomResponse =
		withContext(blockingTaskDispatcher) { chatService.joinRoom(userId, roomId) }

	suspend fun listMessages(
		userId: UUID,
		roomId: UUID,
		pageable: Pageable,
	): PageResponse<ChatMessageResponse> =
		withContext(blockingTaskDispatcher) { chatService.listMessages(userId, roomId, pageable) }

	suspend fun deleteMessage(
		userId: UUID,
		roomId: UUID,
		messageId: UUID,
	) {
		withContext(blockingTaskDispatcher) { chatService.deleteMessage(userId, roomId, messageId) }
	}
}
