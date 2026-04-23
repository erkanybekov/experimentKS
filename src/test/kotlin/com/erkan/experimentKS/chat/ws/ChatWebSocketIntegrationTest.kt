package com.erkan.experimentks.chat.ws

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.erkan.experimentks.support.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.net.http.WebSocketHandshakeException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class ChatWebSocketIntegrationTest : AbstractIntegrationTest() {

	@LocalServerPort
	private var port: Int = 0

	private val httpClient: HttpClient = HttpClient.newHttpClient()

	@Test
	fun `websocket uses authorization header and sends ack plus room events with stable envelope`() {
		val ownerTokens = signup("ws-owner@example.com", displayName = "Socket Owner")
		val memberTokens = signup("ws-member@example.com", displayName = "Socket Member")

		val roomId = createRoom(ownerTokens.accessToken, "Socket Room")
		joinRoom(memberTokens.accessToken, roomId)

		val ownerListener = QueueingWebSocketListener()
		val memberListener = QueueingWebSocketListener()
		val ownerSocket = connect(ownerTokens.accessToken, ownerListener)
		val memberSocket = connect(memberTokens.accessToken, memberListener)

		try {
			memberSocket.sendText(
				"""
				{
				  "action": "SUBSCRIBE_ROOM",
				  "roomId": "$roomId",
				  "clientMessageId": null,
				  "content": null
				}
				""".trimIndent(),
				true,
			).join()

			val subscribedEvent = memberListener.awaitMessage()
			assertEquals("room.subscribed", subscribedEvent["type"].asText())
			assertEquals(roomId.toString(), subscribedEvent["payload"]["roomId"].asText())
			assertNotNull(UUID.fromString(subscribedEvent["eventId"].asText()))
			assertNotNull(Instant.parse(subscribedEvent["serverTime"].asText()))

			val clientMessageId = UUID.randomUUID()
			ownerSocket.sendText(
				"""
				{
				  "action": "SEND_MESSAGE",
				  "roomId": "$roomId",
				  "clientMessageId": "$clientMessageId",
				  "content": "Message over websocket"
				}
				""".trimIndent(),
				true,
			).join()

			val ackEvent = ownerListener.awaitMessage()
			assertEquals("message.ack", ackEvent["type"].asText())
			assertNotNull(UUID.fromString(ackEvent["eventId"].asText()))
			assertNotNull(Instant.parse(ackEvent["serverTime"].asText()))
			assertEquals(clientMessageId.toString(), ackEvent["payload"]["clientMessageId"].asText())
			assertEquals("Socket Owner", ackEvent["payload"]["senderDisplayName"].asText())
			assertEquals("Message over websocket", ackEvent["payload"]["content"].asText())
			assertTrue(ackEvent["payload"]["senderAvatarUrl"].isNull)

			val createdEvent = memberListener.awaitMessage()
			assertEquals("message.created", createdEvent["type"].asText())
			assertNotNull(UUID.fromString(createdEvent["eventId"].asText()))
			assertNotNull(Instant.parse(createdEvent["serverTime"].asText()))
			assertEquals(
				ackEvent["payload"]["id"].asText(),
				createdEvent["payload"]["id"].asText(),
			)
			assertEquals("Socket Owner", createdEvent["payload"]["senderDisplayName"].asText())
			assertEquals(clientMessageId.toString(), createdEvent["payload"]["clientMessageId"].asText())
			assertEquals("Message over websocket", createdEvent["payload"]["content"].asText())
		} finally {
			ownerSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join()
			memberSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join()
		}
	}

	@Test
	fun `websocket rejects query parameter auth`() {
		val ownerTokens = signup("ws-query-owner@example.com", displayName = "Socket Owner")

		val exception = assertThrows(CompletionException::class.java) {
			httpClient.newWebSocketBuilder()
				.buildAsync(
					URI.create("ws://localhost:$port/ws/chat?access_token=${ownerTokens.accessToken}"),
					QueueingWebSocketListener(),
				)
				.join()
		}

		val handshakeException = exception.cause as WebSocketHandshakeException
		assertEquals(401, handshakeException.response.statusCode())
	}

	@Test
	fun `rest message delete broadcasts websocket deletion event`() {
		val ownerTokens = signup("ws-delete-owner@example.com", displayName = "Socket Delete Owner")
		val memberTokens = signup("ws-delete-member@example.com", displayName = "Socket Delete Member")

		val roomId = createRoom(ownerTokens.accessToken, "Socket Delete Room")
		joinRoom(memberTokens.accessToken, roomId)

		val ownerListener = QueueingWebSocketListener()
		val memberListener = QueueingWebSocketListener()
		val ownerSocket = connect(ownerTokens.accessToken, ownerListener)
		val memberSocket = connect(memberTokens.accessToken, memberListener)

		try {
			memberSocket.sendText(
				"""
				{
				  "action": "SUBSCRIBE_ROOM",
				  "roomId": "$roomId",
				  "clientMessageId": null,
				  "content": null
				}
				""".trimIndent(),
				true,
			).join()

			val subscribedEvent = memberListener.awaitMessage()
			assertEquals("room.subscribed", subscribedEvent["type"].asText())

			val clientMessageId = UUID.randomUUID()
			ownerSocket.sendText(
				"""
				{
				  "action": "SEND_MESSAGE",
				  "roomId": "$roomId",
				  "clientMessageId": "$clientMessageId",
				  "content": "Delete me over REST"
				}
				""".trimIndent(),
				true,
			).join()

			val ackEvent = ownerListener.awaitMessage()
			assertEquals("message.ack", ackEvent["type"].asText())
			val messageId = UUID.fromString(ackEvent["payload"]["id"].asText())

			val createdEvent = memberListener.awaitMessage()
			assertEquals("message.created", createdEvent["type"].asText())
			assertEquals(messageId.toString(), createdEvent["payload"]["id"].asText())

			mockMvc.perform(
				delete("/api/v1/chat/rooms/$roomId/messages/$messageId")
					.header("Authorization", bearer(ownerTokens.accessToken)),
			)
				.andExpect(status().isNoContent)

			val deletedEvent = memberListener.awaitMessage()
			assertEquals("message.deleted", deletedEvent["type"].asText())
			assertNotNull(UUID.fromString(deletedEvent["eventId"].asText()))
			assertNotNull(Instant.parse(deletedEvent["serverTime"].asText()))
			assertEquals(roomId.toString(), deletedEvent["payload"]["roomId"].asText())
			assertEquals(messageId.toString(), deletedEvent["payload"]["messageId"].asText())
		} finally {
			ownerSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join()
			memberSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join()
		}
	}

	private fun connect(
		accessToken: String,
		listener: QueueingWebSocketListener,
	): WebSocket =
		httpClient.newWebSocketBuilder()
			.header("Authorization", bearer(accessToken))
			.buildAsync(URI.create("ws://localhost:$port/ws/chat"), listener)
			.join()

	private fun createRoom(
		accessToken: String,
		name: String,
	): UUID {
		val response = mockMvc.perform(
			post("/api/v1/chat/rooms")
				.header("Authorization", bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "$name"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andReturn()
			.response

		return UUID.fromString(objectMapper.readTree(response.contentAsString)["id"].asText())
	}

	private fun joinRoom(
		accessToken: String,
		roomId: UUID,
	) {
		mockMvc.perform(
			post("/api/v1/chat/rooms/$roomId/join")
				.header("Authorization", bearer(accessToken)),
		)
			.andExpect(status().isOk)
	}
}

private class QueueingWebSocketListener : WebSocket.Listener {

	private val objectMapper = ObjectMapper()
	private val messages: LinkedBlockingQueue<String> = LinkedBlockingQueue()
	private val textBuffer = StringBuilder()

	override fun onOpen(webSocket: WebSocket) {
		webSocket.request(1)
	}

	override fun onText(
		webSocket: WebSocket,
		data: CharSequence,
		last: Boolean,
	): CompletableFuture<*> {
		textBuffer.append(data)
		if (last) {
			messages.put(textBuffer.toString())
			textBuffer.setLength(0)
		}
		webSocket.request(1)
		return CompletableFuture.completedFuture(null)
	}

	fun awaitMessage(): JsonNode =
		messages.poll(5, TimeUnit.SECONDS)?.let { rawMessage ->
			objectMapper.readTree(rawMessage)
		} ?: error("Timed out waiting for WebSocket message.")
}
