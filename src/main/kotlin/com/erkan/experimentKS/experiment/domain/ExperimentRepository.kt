package com.erkan.experimentKS.experiment.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExperimentRepository : JpaRepository<Experiment, UUID> {
	fun findAllByOrderByUpdatedAtDesc(): List<Experiment>
}
