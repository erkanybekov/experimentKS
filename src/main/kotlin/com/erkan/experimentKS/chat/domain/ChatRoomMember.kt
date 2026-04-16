package com.erkan.experimentks.chat.domain

import com.erkan.experimentks.auth.domain.User
import com.erkan.experimentks.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "chat_room_members")
class ChatRoomMember() : BaseEntity() {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	lateinit var room: ChatRoom

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	lateinit var user: User

	@Column(name = "joined_at", nullable = false)
	var joinedAt: Instant = Instant.EPOCH

	@Column(name = "last_read_at")
	var lastReadAt: Instant? = null

	constructor(
		room: ChatRoom,
		user: User,
		joinedAt: Instant,
	) : this() {
		this.room = room
		this.user = user
		this.joinedAt = joinedAt
	}
}
