package com.tanmay.tempmail.temp_mail.inbox

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InboxRepository : JpaRepository<Inbox, UUID> {
	fun existsByEmailAddress(emailAddress: String): Boolean
	fun findByEmailAddress(emailAddress: String): Inbox?
}
