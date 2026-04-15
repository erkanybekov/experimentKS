package com.erkan.experimentks.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class RenderDatabaseUrlEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {

	override fun postProcessEnvironment(
		environment: ConfigurableEnvironment,
		application: SpringApplication,
	) {
		val rawUrl = environment.getProperty("spring.datasource.url")
			?: environment.getProperty("SPRING_DATASOURCE_URL")
			?: environment.getProperty("DATABASE_URL")
			?: return

		val derived = DerivedDatasourceProperties.from(rawUrl) ?: return
		val properties = linkedMapOf<String, Any>(
			"spring.datasource.url" to derived.jdbcUrl,
		)

		val existingUsername = environment.getProperty("spring.datasource.username")
			?: environment.getProperty("SPRING_DATASOURCE_USERNAME")
		if (existingUsername.isNullOrBlank() && derived.username != null) {
			properties["spring.datasource.username"] = derived.username
		}
		val existingPassword = environment.getProperty("spring.datasource.password")
			?: environment.getProperty("SPRING_DATASOURCE_PASSWORD")
		if (existingPassword.isNullOrBlank() && derived.password != null) {
			properties["spring.datasource.password"] = derived.password
		}

		environment.propertySources.addFirst(
			MapPropertySource("renderDatabaseUrl", properties),
		)
	}

	override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

	internal data class DerivedDatasourceProperties(
		val jdbcUrl: String,
		val username: String?,
		val password: String?,
	) {
		companion object {
			fun from(rawUrl: String): DerivedDatasourceProperties? {
				if (rawUrl.startsWith("jdbc:", ignoreCase = true)) {
					return null
				}
				if (!rawUrl.startsWith("postgres://", ignoreCase = true) &&
					!rawUrl.startsWith("postgresql://", ignoreCase = true)
				) {
					return null
				}

				val uri = URI(rawUrl)
				val database = uri.rawPath.removePrefix("/")
				require(database.isNotBlank()) { "Database name is missing in datasource URL." }

				val jdbcUrl = buildString {
					append("jdbc:postgresql://")
					append(requireNotNull(uri.host) { "Host is missing in datasource URL." })
					if (uri.port != -1) {
						append(":")
						append(uri.port)
					}
					append("/")
					append(database)
					if (!uri.rawQuery.isNullOrBlank()) {
						append("?")
						append(uri.rawQuery)
					}
				}

				val userInfo = uri.rawUserInfo.orEmpty()
				val userParts = userInfo.split(":", limit = 2)
				val username = userParts.getOrNull(0)
					?.takeIf { it.isNotBlank() }
					?.let(::decodeComponent)
				val password = userParts.getOrNull(1)
					?.takeIf { it.isNotBlank() }
					?.let(::decodeComponent)

				return DerivedDatasourceProperties(
					jdbcUrl = jdbcUrl,
					username = username,
					password = password,
				)
			}

			private fun decodeComponent(value: String): String =
				URLDecoder.decode(value, StandardCharsets.UTF_8)
		}
	}
}
