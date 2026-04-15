package com.erkan.experimentks.finance.category.domain

import com.erkan.experimentks.finance.TransactionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CategoryRepository : JpaRepository<Category, UUID> {

	@Query(
		"""
		select c
		from Category c
		where c.user.id = :userId
		  and c.deletedAt is null
		order by c.type asc, lower(c.name) asc
		""",
	)
	fun findActiveByUserId(userId: UUID): List<Category>

	@Query(
		"""
		select count(c) > 0
		from Category c
		where c.user.id = :userId
		  and c.type = :type
		  and lower(c.name) = lower(:name)
		  and c.deletedAt is null
		""",
	)
	fun existsActiveByUserIdAndTypeAndName(
		userId: UUID,
		type: TransactionType,
		name: String,
	): Boolean

	fun findByIdAndUserIdAndDeletedAtIsNull(
		id: UUID,
		userId: UUID,
	): Category?
}
