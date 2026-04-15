package com.erkan.experimentKS.experiment.domain

import com.erkan.experimentKS.experiment.api.CreateExperimentRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ExperimentService(
	private val experimentRepository: ExperimentRepository,
) {

	@Transactional(readOnly = true)
	fun listExperiments(): List<Experiment> = experimentRepository.findAllByOrderByUpdatedAtDesc()

	@Transactional(readOnly = true)
	fun getExperiment(id: UUID): Experiment = findByIdOrThrow(id)

	@Transactional
	fun createExperiment(request: CreateExperimentRequest): Experiment {
		val experiment = Experiment(
			title = request.title.trim(),
			description = request.description.trim(),
			platform = request.platform,
			status = request.status,
		)

		return experimentRepository.saveAndFlush(experiment)
	}

	@Transactional
	fun updateStatus(id: UUID, status: ExperimentStatus): Experiment {
		val experiment = findByIdOrThrow(id)
		experiment.status = status
		return experimentRepository.saveAndFlush(experiment)
	}

	private fun findByIdOrThrow(id: UUID): Experiment =
		experimentRepository.findById(id)
			.orElseThrow { ExperimentNotFoundException(id) }
}

class ExperimentNotFoundException(id: UUID) :
	RuntimeException("Experiment $id was not found.")
