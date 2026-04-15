package com.erkan.experimentks.auth.api

import com.erkan.experimentks.support.AbstractIntegrationTest
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthIntegrationTest : AbstractIntegrationTest() {

	@Test
	fun `signup and login return token pairs`() {
		signup("auth-user@example.com", displayName = "Auth User")

		mockMvc.perform(
			post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "email": "auth-user@example.com",
					  "password": "Password123"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.accessToken").isNotEmpty)
			.andExpect(jsonPath("$.accessTokenExpiresInSeconds").value(greaterThan(0)))
			.andExpect(jsonPath("$.refreshToken").isNotEmpty)
			.andExpect(jsonPath("$.refreshTokenExpiresInSeconds").value(greaterThan(0)))
			.andExpect(jsonPath("$.user").doesNotExist())
	}

	@Test
	fun `refresh rotates refresh token and logout invalidates it`() {
		signup("rotate@example.com", displayName = "Rotate User")
		val loginTokens = login("rotate@example.com")

		val refreshResponse = mockMvc.perform(
			post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"refreshToken":"${loginTokens.refreshToken}"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.accessToken").isNotEmpty)
			.andExpect(jsonPath("$.refreshToken", not(loginTokens.refreshToken)))
			.andReturn()
			.response

		val refreshedToken = objectMapper.readTree(refreshResponse.contentAsString)["refreshToken"].asText()

		mockMvc.perform(
			post("/api/v1/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"refreshToken":"$refreshedToken"}"""),
		)
			.andExpect(status().isNoContent)

		mockMvc.perform(
			post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"refreshToken":"$refreshedToken"}"""),
		)
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value(containsString("INVALID")))
	}

	@Test
	fun `current user endpoint returns authenticated profile`() {
		val tokens = signup("me@example.com", displayName = "Me User")

		mockMvc.perform(
			get("/api/v1/users/me")
				.header("Authorization", bearer(tokens.accessToken)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.email").value("me@example.com"))
			.andExpect(jsonPath("$.displayName").value("Me User"))
			.andExpect(jsonPath("$.createdAt").isNotEmpty)
			.andExpect(jsonPath("$.updatedAt").isNotEmpty)
	}
}
