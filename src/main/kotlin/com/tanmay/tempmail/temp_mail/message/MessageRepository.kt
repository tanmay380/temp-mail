package com.tanmay.tempmail.temp_mail.message

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MessageRepository : JpaRepository<Message, UUID> {
	fun findByInboxIdOrderByReceivedAtDesc(inboxId: UUID): List<Message>

	fun findByIdAndInboxId(id: UUID, inboxId: UUID): Message?

	fun countByInboxId(inboxId: UUID): Long
}
