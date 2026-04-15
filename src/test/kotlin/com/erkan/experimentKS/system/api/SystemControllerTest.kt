package com.erkan.experimentKS.system.api

import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
	properties = [
		"app.version=test-version",
		"app.environment=test",
	],
)
class SystemControllerTest(
	@Autowired private val mockMvc: MockMvc,
) {

	@Test
	fun `health endpoint is public and returns up`() {
		mockMvc.perform(get("/api/v1/system/health"))
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.status", equalTo("UP")))
			.andExpect(jsonPath("$.timestamp").isNotEmpty)
	}

	@Test
	fun `info endpoint returns backend metadata`() {
		mockMvc.perform(get("/api/v1/system/info"))
			.andExpect(status().isOk)
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.name", equalTo("experimentKS")))
			.andExpect(jsonPath("$.description", equalTo("Backend API for the ExperimentKmp mobile app.")))
			.andExpect(jsonPath("$.version", equalTo("test-version")))
			.andExpect(jsonPath("$.environment", equalTo("test")))
			.andExpect(jsonPath("$.serverTime").isNotEmpty)
	}
}
