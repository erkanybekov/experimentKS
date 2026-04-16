package com.erkan.experimentks.chat.domain

import com.erkan.experimentks.auth.domain.User
import com.erkan.experimentks.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "chat_messages")
class ChatMessage() : BaseEntity() {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	lateinit var room: ChatRoom

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_user_id", nullable = false)
	lateinit var senderUser: User

	@Column(name = "client_message_id", nullable = false)
	var clientMessageId: UUID = UUID.randomUUID()

	@Column(nullable = false, length = 2000)
	lateinit var content: String

	constructor(
		room: ChatRoom,
		senderUser: User,
		clientMessageId: UUID,
		content: String,
	) : this() {
		this.room = room
		this.senderUser = senderUser
		this.clientMessageId = clientMessageId
		this.content = content
	}
}
