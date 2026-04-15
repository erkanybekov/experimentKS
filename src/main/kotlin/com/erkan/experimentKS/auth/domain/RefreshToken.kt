package com.erkan.experimentks.auth.domain

import com.erkan.experimentks.shared.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "refresh_tokens")
class RefreshToken() : BaseEntity() {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	lateinit var user: User

	@Column(name = "expires_at", nullable = false)
	var expiresAt: Instant = Instant.EPOCH

	@Column(name = "revoked_at")
	var revokedAt: Instant? = null

	constructor(
		user: User,
		expiresAt: Instant,
	) : this() {
		this.user = user
		this.expiresAt = expiresAt
	}

	fun revoke(at: Instant) {
		if (revokedAt == null) {
			revokedAt = at
		}
	}
}
