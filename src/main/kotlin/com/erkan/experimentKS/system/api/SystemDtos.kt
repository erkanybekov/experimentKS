package com.erkan.experimentKS.system.api

import java.time.Instant

data class HealthResponse(
	val status: String,
	val timestamp: Instant,
)

data class SystemInfoResponse(
	val name: String,
	val description: String,
	val version: String,
	val environment: String,
	val serverTime: Instant,
)
