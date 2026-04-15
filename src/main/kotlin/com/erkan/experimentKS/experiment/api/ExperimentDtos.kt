package com.erkan.experimentKS.experiment.api

import com.erkan.experimentKS.experiment.domain.Experiment
import com.erkan.experimentKS.experiment.domain.ExperimentPlatform
import com.erkan.experimentKS.experiment.domain.ExperimentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateExperimentRequest(
	@field:NotBlank
	@field:Size(max = 120)
	val title: String,

	@field:NotBlank
	@field:Size(max = 500)
	val description: String,

	val platform: ExperimentPlatform,

	val status: ExperimentStatus = ExperimentStatus.PLANNED,
)

data class UpdateExperimentStatusRequest(
	val status: ExperimentStatus,
)

data class ExperimentResponse(
	val id: UUID,
	val title: String,
	val description: String,
	val platform: ExperimentPlatform,
	val status: ExperimentStatus,
	val createdAt: Instant,
	val updatedAt: Instant,
)

fun Experiment.toResponse(): ExperimentResponse =
	ExperimentResponse(
		id = id,
		title = title,
		description = description,
		platform = platform,
		status = status,
		createdAt = requireNotNull(createdAt),
		updatedAt = requireNotNull(updatedAt),
	)
