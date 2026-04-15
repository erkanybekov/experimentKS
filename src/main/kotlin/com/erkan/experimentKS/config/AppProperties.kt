package com.erkan.experimentks.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app")
data class AppProperties(
	val name: String,
	val apiBasePath: String,
	val baseUrl: String?,
)
