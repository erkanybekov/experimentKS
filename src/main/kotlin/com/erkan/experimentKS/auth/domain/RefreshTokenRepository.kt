package com.erkan.experimentks.auth.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
	fun findByIdAndUserId(id: UUID, userId: UUID): RefreshToken?
}
