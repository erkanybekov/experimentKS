package com.erkan.experimentks.support

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

	@Autowired
	protected lateinit var mockMvc: MockMvc

	protected val objectMapper = ObjectMapper()

	protected fun signup(
		email: String,
		displayName: String = "Test User",
		password: String = "Password123",
	): AuthTokens {
		val response = mockMvc.perform(
			post("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "displayName": "$displayName",
					  "email": "$email",
					  "password": "$password"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andReturn()
			.response

		return parseTokens(response.contentAsString)
	}

	protected fun login(
		email: String,
		password: String = "Password123",
	): AuthTokens {
		val response = mockMvc.perform(
			post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "email": "$email",
					  "password": "$password"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response

		return parseTokens(response.contentAsString)
	}

	protected fun createCategory(
		accessToken: String,
		name: String,
		type: String,
	): String {
		val response = mockMvc.perform(
			post("/api/v1/categories")
				.header("Authorization", bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "$name",
					  "type": "$type"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andReturn()
			.response

		return objectMapper.readTree(response.contentAsString)["id"].asText()
	}

	protected fun findCategoryId(
		accessToken: String,
		name: String,
		type: String,
	): String {
		val response = mockMvc.perform(
			get("/api/v1/categories")
				.header("Authorization", bearer(accessToken)),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response

		val categories = objectMapper.readTree(response.contentAsString)
		return categories.firstOrNull { node ->
			node["name"].asText() == name && node["type"].asText() == type
		}?.get("id")?.asText()
			?: error("Category $name/$type was not found in ${response.contentAsString}")
	}

	protected fun bearer(token: String): String = "Bearer $token"

	protected fun currentUserId(accessToken: String): String {
		val response = mockMvc.perform(
			get("/api/v1/users/me")
				.header("Authorization", bearer(accessToken)),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response

		return objectMapper.readTree(response.contentAsString)["id"].asText()
	}

	private fun parseTokens(content: String): AuthTokens {
		val root = objectMapper.readTree(content)
		return AuthTokens(
			accessToken = root["accessToken"].asText(),
			refreshToken = root["refreshToken"].asText(),
		)
	}

	companion object {
		@Container
		@JvmStatic
		val postgres = PostgreSQLContainer("postgres:17-alpine")
			.withDatabaseName("experimentks")
			.withUsername("postgres")
			.withPassword("postgres")

		@JvmStatic
		@DynamicPropertySource
		fun registerProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url") { postgres.jdbcUrl }
			registry.add("spring.datasource.username") { postgres.username }
			registry.add("spring.datasource.password") { postgres.password }
			registry.add("app.auth.secret") { "integration-test-secret-key-123456789012345" }
		}
	}
}

data class AuthTokens(
	val accessToken: String,
	val refreshToken: String,
)
