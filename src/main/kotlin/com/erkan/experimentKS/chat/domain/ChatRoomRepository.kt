package com.erkan.experimentks.chat.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChatRoomRepository : JpaRepository<ChatRoom, UUID>
