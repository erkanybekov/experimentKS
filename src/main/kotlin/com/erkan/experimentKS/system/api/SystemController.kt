package com.erkan.experimentKS.system.api

import com.erkan.experimentKS.config.AppProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant

@RestController
@RequestMapping("\${app.api-base-path}/system")
class SystemController(
	private val appProperties: AppProperties,
	private val clock: Clock,
) {

	@GetMapping("/health")
	fun health(): HealthResponse =
		HealthResponse(
			status = "UP",
			timestamp = Instant.now(clock),
		)

	@GetMapping("/info")
	fun info(): SystemInfoResponse =
		SystemInfoResponse(
			name = appProperties.name,
			description = appProperties.description,
			version = appProperties.version,
			environment = appProperties.environment,
			serverTime = Instant.now(clock),
		)
}
