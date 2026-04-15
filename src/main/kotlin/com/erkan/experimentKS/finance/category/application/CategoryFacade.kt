package com.erkan.experimentks.finance.category.application

import com.erkan.experimentks.finance.category.api.CategoryResponse
import com.erkan.experimentks.finance.category.api.CreateCategoryRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CategoryFacade(
	private val categoryService: CategoryService,
	@Qualifier("blockingTaskDispatcher")
	private val blockingTaskDispatcher: CoroutineDispatcher,
) {

	suspend fun listCategories(userId: UUID): List<CategoryResponse> =
		withContext(blockingTaskDispatcher) { categoryService.listCategories(userId) }

	suspend fun createCategory(
		userId: UUID,
		request: CreateCategoryRequest,
	): CategoryResponse =
		withContext(blockingTaskDispatcher) { categoryService.createCategory(userId, request) }
}
