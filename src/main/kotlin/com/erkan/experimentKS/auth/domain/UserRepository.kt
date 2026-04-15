package com.erkan.experimentks.auth.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
	fun findByEmailAndDeletedAtIsNull(email: String): User?
	fun findByIdAndDeletedAtIsNull(id: UUID): User?
	fun existsByEmailAndDeletedAtIsNull(email: String): Boolean
}
