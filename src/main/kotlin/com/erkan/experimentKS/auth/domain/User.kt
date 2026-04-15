package com.erkan.experimentks.auth.domain

import com.erkan.experimentks.shared.domain.SoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User() : SoftDeletableEntity() {

	@Column(nullable = false, length = 255)
	lateinit var email: String

	@Column(name = "display_name", nullable = false, length = 120)
	lateinit var displayName: String

	@Column(name = "password_hash", nullable = false, length = 255)
	lateinit var passwordHash: String

	constructor(
		email: String,
		displayName: String,
		passwordHash: String,
	) : this() {
		this.email = email
		this.displayName = displayName
		this.passwordHash = passwordHash
	}
}
