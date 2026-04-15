package com.erkan.experimentks.finance.category.application

import com.erkan.experimentks.auth.domain.User
import com.erkan.experimentks.auth.domain.UserRepository
import com.erkan.experimentks.finance.TransactionType
import com.erkan.experimentks.finance.category.api.CategoryResponse
import com.erkan.experimentks.finance.category.api.CreateCategoryRequest
import com.erkan.experimentks.finance.category.api.toResponse
import com.erkan.experimentks.finance.category.domain.Category
import com.erkan.experimentks.finance.category.domain.CategoryRepository
import com.erkan.experimentks.shared.api.ConflictException
import com.erkan.experimentks.shared.api.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategoryService(
	private val categoryRepository: CategoryRepository,
	private val userRepository: UserRepository,
) {

	@Transactional(readOnly = true)
	fun listCategories(userId: UUID): List<CategoryResponse> =
		categoryRepository.findActiveByUserId(userId).map { it.toResponse() }

	@Transactional
	fun createCategory(
		userId: UUID,
		request: CreateCategoryRequest,
	): CategoryResponse {
		val normalizedName = request.name.trim()
		if (categoryRepository.existsActiveByUserIdAndTypeAndName(userId, request.type, normalizedName)) {
			throw ConflictException("CATEGORY_ALREADY_EXISTS", "A category with this name already exists.")
		}

		val user = userRepository.getReferenceById(userId)
		return categoryRepository.saveAndFlush(
			Category(
				user = user,
				type = request.type,
				name = normalizedName,
			),
		).toResponse()
	}

	@Transactional
	fun createDefaultCategories(user: User) {
		val defaults = listOf(
			TransactionType.INCOME to "Salary",
			TransactionType.INCOME to "Freelance",
			TransactionType.INCOME to "Investments",
			TransactionType.EXPENSE to "Food",
			TransactionType.EXPENSE to "Transport",
			TransactionType.EXPENSE to "Housing",
			TransactionType.EXPENSE to "Shopping",
			TransactionType.EXPENSE to "Health",
			TransactionType.EXPENSE to "Entertainment",
		)

		val categories = defaults.map { (type, name) ->
			Category(
				user = user,
				type = type,
				name = name,
			)
		}

		categoryRepository.saveAll(categories)
	}

	@Transactional(readOnly = true)
	fun findOwnedActiveCategory(
		userId: UUID,
		categoryId: UUID,
	): Category =
		categoryRepository.findByIdAndUserIdAndDeletedAtIsNull(categoryId, userId)
			?: throw NotFoundException("CATEGORY_NOT_FOUND", "Category $categoryId was not found.")
}
