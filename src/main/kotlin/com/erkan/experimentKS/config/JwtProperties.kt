package com.erkan.experimentks.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties("app.auth")
data class JwtProperties(
	@field:NotBlank
	val issuer: String,

	@field:NotBlank
	@field:Size(min = 32)
	val secret: String,

	val accessTokenTtl: Duration,
	val refreshTokenTtl: Duration,
)
