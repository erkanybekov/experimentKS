package com.erkan.experimentks.chat.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ChatRoomMemberRepository : JpaRepository<ChatRoomMember, UUID> {
	fun countByRoomId(roomId: UUID): Long

	fun existsByRoomIdAndUserId(roomId: UUID, userId: UUID): Boolean

	fun findByRoomIdAndUserId(roomId: UUID, userId: UUID): ChatRoomMember?

	@Query(
		"""
		select member
		from ChatRoomMember member
		join fetch member.room room
		where member.user.id = :userId
		order by coalesce(room.lastActivityAt, room.createdAt) desc, room.createdAt desc
		""",
	)
	fun findAllForUser(userId: UUID): List<ChatRoomMember>
}
