package com.erkan.experimentKS.experiment.api

import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExperimentControllerTest(
	@Autowired private val mockMvc: MockMvc,
) {

	@Test
	fun `create and fetch experiment`() {
		val experimentId = createExperiment(
			title = "Offline Sync",
			description = "Validate sync behaviour between mobile and backend.",
			platform = "KMP",
		)

		mockMvc.perform(get("/api/v1/experiments/$experimentId"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id", equalTo(experimentId)))
			.andExpect(jsonPath("$.title", equalTo("Offline Sync")))
			.andExpect(jsonPath("$.platform", equalTo("KMP")))
			.andExpect(jsonPath("$.status", equalTo("PLANNED")))
	}

	@Test
	fun `list experiments returns newest first`() {
		createExperiment(
			title = "First",
			description = "Older experiment",
			platform = "ANDROID",
		)
		createExperiment(
			title = "Second",
			description = "Newer experiment",
			platform = "BACKEND",
		)

		mockMvc.perform(get("/api/v1/experiments"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$[0].title", equalTo("Second")))
			.andExpect(jsonPath("$[1].title", equalTo("First")))
	}

	@Test
	fun `status update persists`() {
		val experimentId = createExperiment(
			title = "Push Notifications",
			description = "Validate notification delivery flow.",
			platform = "IOS",
		)

		mockMvc.perform(
			patch("/api/v1/experiments/$experimentId/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"status":"IN_PROGRESS"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status", equalTo("IN_PROGRESS")))
			.andExpect(jsonPath("$.id", equalTo(experimentId)))
	}

	@Test
	fun `missing experiment returns structured 404`() {
		mockMvc.perform(get("/api/v1/experiments/00000000-0000-0000-0000-000000000001"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code", equalTo("EXPERIMENT_NOT_FOUND")))
			.andExpect(jsonPath("$.message", containsString("00000000-0000-0000-0000-000000000001")))
	}

	@Test
	fun `invalid request returns validation error payload`() {
		mockMvc.perform(
			post("/api/v1/experiments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"title":" ","description":"Missing title","platform":"KMP"}"""),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
			.andExpect(jsonPath("$.details[0]", containsString("title")))
	}

	private fun createExperiment(
		title: String,
		description: String,
		platform: String,
	): String {
		val response = mockMvc.perform(
			post("/api/v1/experiments")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "title": "$title",
					  "description": "$description",
					  "platform": "$platform"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(header().string("Location", containsString("/api/v1/experiments/")))
			.andReturn()
			.response

		return """"id":"([^"]+)"""".toRegex()
			.find(response.contentAsString)
			?.groupValues
			?.get(1)
			?: error("Experiment id was not present in response: ${response.contentAsString}")
	}
}
