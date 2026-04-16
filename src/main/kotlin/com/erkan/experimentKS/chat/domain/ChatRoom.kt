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
@Table(name = "chat_rooms")
class ChatRoom() : BaseEntity() {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	lateinit var createdBy: User

	@Column(nullable = false, length = 120)
	lateinit var name: String

	@Column(name = "last_activity_at")
	var lastActivityAt: Instant? = null

	constructor(
		createdBy: User,
		name: String,
	) : this() {
		this.createdBy = createdBy
		this.name = name
	}
}
