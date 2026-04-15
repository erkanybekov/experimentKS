package com.erkan.experimentKS.experiment.api

import com.erkan.experimentKS.experiment.domain.ExperimentService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

@RestController
@RequestMapping("\${app.api-base-path}/experiments")
class ExperimentController(
	private val experimentService: ExperimentService,
) {

	@GetMapping
	fun listExperiments(): List<ExperimentResponse> =
		experimentService.listExperiments().map { it.toResponse() }

	@GetMapping("/{id}")
	fun getExperiment(@PathVariable id: UUID): ExperimentResponse =
		experimentService.getExperiment(id).toResponse()

	@PostMapping
	fun createExperiment(
		@Valid @RequestBody request: CreateExperimentRequest,
	): ResponseEntity<ExperimentResponse> {
		val experiment = experimentService.createExperiment(request).toResponse()
		val location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(experiment.id)
			.toUri()

		return ResponseEntity.created(location).body(experiment)
	}

	@PatchMapping("/{id}/status")
	fun updateStatus(
		@PathVariable id: UUID,
		@Valid @RequestBody request: UpdateExperimentStatusRequest,
	): ExperimentResponse =
		experimentService.updateStatus(id, request.status).toResponse()
}
