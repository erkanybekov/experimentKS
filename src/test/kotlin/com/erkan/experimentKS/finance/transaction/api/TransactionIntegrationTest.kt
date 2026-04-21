package com.erkan.experimentks.finance.transaction.api

import com.erkan.experimentks.support.AbstractIntegrationTest
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TransactionIntegrationTest : AbstractIntegrationTest() {

	@Test
	fun `transaction CRUD filters and dashboard aggregation work`() {
		val auth = signup("finance@example.com", displayName = "Finance User")
		val accessToken = auth.accessToken
		val expenseCategoryId = findCategoryId(accessToken, "Food", "EXPENSE")
		val incomeCategoryId = findCategoryId(accessToken, "Salary", "INCOME")

		val incomeResponse = mockMvc.perform(
			post("/api/v1/transactions")
				.header("Authorization", bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "categoryId": "$incomeCategoryId",
					  "type": "INCOME",
					  "amount": 1000.00,
					  "note": "Salary payment",
					  "occurredAt": "2026-04-15T09:00:00Z"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.type").value("INCOME"))
			.andReturn()
			.response

		val expenseResponse = mockMvc.perform(
			post("/api/v1/transactions")
				.header("Authorization", bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "categoryId": "$expenseCategoryId",
					  "type": "EXPENSE",
					  "amount": 25.50,
					  "note": "Lunch",
					  "occurredAt": "2026-04-15T12:00:00Z"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.type").value("EXPENSE"))
			.andReturn()
			.response

		val expenseId = objectMapper.readTree(expenseResponse.contentAsString)["id"].asText()
		val incomeId = objectMapper.readTree(incomeResponse.contentAsString)["id"].asText()

		mockMvc.perform(
			get("/api/v1/transactions/$expenseId")
				.header("Authorization", bearer(accessToken)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.note").value("Lunch"))
			.andExpect(jsonPath("$.category.name").value("Food"))

		mockMvc.perform(
			put("/api/v1/transactions/$expenseId")
				.header("Authorization", bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "categoryId": "$expenseCategoryId",
					  "type": "EXPENSE",
					  "amount": 30.00,
					  "note": "Lunch and coffee",
					  "occurredAt": "2026-04-15T12:30:00Z"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.amount").value(30.0))
			.andExpect(jsonPath("$.note").value("Lunch and coffee"))

		mockMvc.perform(
			get("/api/v1/transactions")
				.header("Authorization", bearer(accessToken))
				.param("type", "EXPENSE")
				.param("from", "2026-04-01T00:00:00Z")
				.param("to", "2026-04-30T23:59:59Z")
				.param("page", "0")
				.param("size", "10")
				.param("sort", "occurredAt,desc"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(expenseId))
			.andExpect(jsonPath("$.items[0].note").value("Lunch and coffee"))

		mockMvc.perform(
			get("/api/v1/dashboard")
				.header("Authorization", bearer(accessToken))
				.param("period", "MONTH"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.summary.totalIncome").value(1000.0))
			.andExpect(jsonPath("$.summary.totalExpense").value(30.0))
			.andExpect(jsonPath("$.summary.balance").value(970.0))
			.andExpect(jsonPath("$.recentTransactions.length()").value(2))

		mockMvc.perform(
			get("/api/v1/dashboard")
				.header("Authorization", bearer(accessToken))
				.param("period", "April"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("period"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value(containsString("Allowed values: WEEK, MONTH, YEAR")))

		mockMvc.perform(
			delete("/api/v1/transactions/$expenseId")
				.header("Authorization", bearer(accessToken)),
		)
			.andExpect(status().isNoContent)

		mockMvc.perform(
			get("/api/v1/transactions/$expenseId")
				.header("Authorization", bearer(accessToken)),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code", equalTo("TRANSACTION_NOT_FOUND")))

		mockMvc.perform(
			get("/api/v1/transactions")
				.header("Authorization", bearer(accessToken))
				.param("type", "INCOME"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(incomeId))
	}

	@Test
	fun `enum request fields are validated instead of failing as malformed input`() {
		val auth = signup("finance-validation@example.com", displayName = "Finance Validation User")
		val accessToken = auth.accessToken
		val expenseCategoryId = findCategoryId(accessToken, "Food", "EXPENSE")

		mockMvc.perform(
			post("/api/v1/categories")
				.header("Authorization", bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "Travel",
					  "type": null
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("type"))

		mockMvc.perform(
			post("/api/v1/transactions")
				.header("Authorization", bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "categoryId": "$expenseCategoryId",
					  "type": null,
					  "amount": 10.00,
					  "note": "Broken enum payload",
					  "occurredAt": "2026-04-15T12:00:00Z"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("type"))
	}
}
