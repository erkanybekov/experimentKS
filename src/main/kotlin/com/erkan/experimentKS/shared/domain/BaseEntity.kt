package com.erkan.experimentks.shared.domain

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@MappedSuperclass
abstract class BaseEntity {

	@Id
	@Column(nullable = false, updatable = false)
	var id: UUID = UUID.randomUUID()

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	var createdAt: Instant? = null

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant? = null
}

@MappedSuperclass
abstract class SoftDeletableEntity : BaseEntity() {

	@Column(name = "deleted_at")
	var deletedAt: Instant? = null
}
