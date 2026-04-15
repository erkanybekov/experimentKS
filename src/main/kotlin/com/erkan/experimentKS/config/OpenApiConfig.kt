package com.erkan.experimentKS.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig(
	private val appProperties: AppProperties,
) {

	@Bean
	fun experimentKsOpenApi(): OpenAPI = OpenAPI()
		.info(
			Info()
				.title("${appProperties.name} API")
				.description(appProperties.description)
				.version(appProperties.version)
				.license(License().name("Proprietary")),
		)
}
