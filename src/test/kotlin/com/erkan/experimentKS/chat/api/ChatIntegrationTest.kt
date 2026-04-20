package com.erkan.experimentks.chat.api

import com.erkan.experimentks.chat.application.ChatService
import com.erkan.experimentks.support.AbstractIntegrationTest
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class ChatIntegrationTest : AbstractIntegrationTest() {

	@Autowired
	private lateinit var chatService: ChatService

	@Test
	fun `chat room membership message history and idempotent send work without extra join for readers`() {
		val ownerTokens = signup("chat-owner@example.com", displayName = "Chat Owner")
		val memberTokens = signup("chat-member@example.com", displayName = "Chat Member")
		val outsiderTokens = signup("chat-outsider@example.com", displayName = "Chat Outsider")

		val ownerUserId = UUID.fromString(currentUserId(ownerTokens.accessToken))
		val memberUserId = UUID.fromString(currentUserId(memberTokens.accessToken))

		val roomResponse = mockMvc.perform(
			post("/api/v1/chat/rooms")
				.header("Authorization", bearer(ownerTokens.accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "Budget Buddies"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.name").value("Budget Buddies"))
			.andExpect(jsonPath("$.memberCount").value(1))
			.andReturn()
			.response

		val roomId = UUID.fromString(objectMapper.readTree(roomResponse.contentAsString)["id"].asText())

		mockMvc.perform(
			post("/api/v1/chat/rooms/$roomId/join")
				.header("Authorization", bearer(memberTokens.accessToken)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.memberCount").value(2))

		val clientMessageId = UUID.randomUUID()
		val firstMessage = chatService.sendMessage(
			userId = ownerUserId,
			roomId = roomId,
			clientMessageId = clientMessageId,
			content = "First message from websocket path",
		)
		val secondAttempt = chatService.sendMessage(
			userId = ownerUserId,
			roomId = roomId,
			clientMessageId = clientMessageId,
			content = "First message from websocket path",
		)
		val replyMessage = chatService.sendMessage(
			userId = memberUserId,
			roomId = roomId,
			clientMessageId = UUID.randomUUID(),
			content = "Reply from second member",
		)

		org.junit.jupiter.api.Assertions.assertTrue(firstMessage.created)
		org.junit.jupiter.api.Assertions.assertFalse(secondAttempt.created)
		org.junit.jupiter.api.Assertions.assertTrue(replyMessage.created)
		org.junit.jupiter.api.Assertions.assertEquals(firstMessage.message.id, secondAttempt.message.id)

		mockMvc.perform(
			get("/api/v1/chat/rooms/$roomId/messages")
				.header("Authorization", bearer(ownerTokens.accessToken))
				.param("page", "0")
				.param("size", "20"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.items[0].id").value(replyMessage.message.id.toString()))
			.andExpect(jsonPath("$.items[0].senderDisplayName").value("Chat Member"))
			.andExpect(jsonPath("$.items[0].senderAvatarUrl").value(nullValue()))
			.andExpect(jsonPath("$.items[1].id").value(firstMessage.message.id.toString()))
			.andExpect(jsonPath("$.items[1].senderDisplayName").value("Chat Owner"))
			.andExpect(jsonPath("$.items[1].senderAvatarUrl").value(nullValue()))

		mockMvc.perform(
			get("/api/v1/chat/rooms")
				.header("Authorization", bearer(ownerTokens.accessToken)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$[0].id").value(roomId.toString()))
			.andExpect(jsonPath("$[0].lastMessagePreview").value("Reply from second member"))

		mockMvc.perform(
			get("/api/v1/chat/rooms/$roomId/messages")
				.header("Authorization", bearer(outsiderTokens.accessToken)),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code", equalTo("CHAT_ROOM_NOT_FOUND")))
	}
}
