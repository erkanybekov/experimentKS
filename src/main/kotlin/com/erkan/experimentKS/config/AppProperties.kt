package com.erkan.experimentKS.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
	val name: String,
	val description: String,
	val version: String,
	val environment: String,
	val apiBasePath: String,
)
