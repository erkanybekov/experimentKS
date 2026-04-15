package com.erkan.experimentks.finance.category.api

import com.erkan.experimentks.finance.category.application.CategoryFacade
import com.erkan.experimentks.shared.security.AuthenticatedUser
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/categories")
@SecurityRequirement(name = "bearerAuth")
class CategoryController(
	private val categoryFacade: CategoryFacade,
) {

	@GetMapping
	suspend fun listCategories(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
	): List<CategoryResponse> = categoryFacade.listCategories(currentUser.id)

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	suspend fun createCategory(
		@AuthenticationPrincipal currentUser: AuthenticatedUser,
		@Valid @RequestBody request: CreateCategoryRequest,
	): CategoryResponse = categoryFacade.createCategory(currentUser.id, request)
}
