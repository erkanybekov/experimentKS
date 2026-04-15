package com.erkan.experimentKS.experiment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "experiments")
class Experiment(
	@Id
	var id: UUID = UUID.randomUUID(),

	@Column(nullable = false, length = 120)
	var title: String = "",

	@Column(nullable = false, length = 500)
	var description: String = "",

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	var platform: ExperimentPlatform = ExperimentPlatform.KMP,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	var status: ExperimentStatus = ExperimentStatus.PLANNED,

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	var createdAt: Instant? = null,

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant? = null,
)

enum class ExperimentPlatform {
	ANDROID,
	IOS,
	KMP,
	BACKEND,
}

enum class ExperimentStatus {
	PLANNED,
	IN_PROGRESS,
	PAUSED,
	DONE,
}
