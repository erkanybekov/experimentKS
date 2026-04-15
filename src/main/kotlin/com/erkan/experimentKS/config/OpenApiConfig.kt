package com.erkan.experimentks.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
	private val appProperties: AppProperties,
) {

	@Bean
	fun openApi(): OpenAPI {
		val openApi = OpenAPI()
			.info(
				Info()
					.title("${appProperties.name} API")
					.description("Backend API for the experimentKmp personal finance app.")
					.version("v1")
					.license(License().name("Proprietary")),
			)
			.components(
				Components().addSecuritySchemes(
					"bearerAuth",
					SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT"),
				),
			)
			.addSecurityItem(SecurityRequirement().addList("bearerAuth"))

		appProperties.baseUrl?.takeIf { it.isNotBlank() }?.let {
			openApi.servers(listOf(Server().url(it)))
		}

		return openApi
	}
}
