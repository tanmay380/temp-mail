package com.tanmay.tempmail.temp_mail.inbox

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class InboxStatus { ACTIVE, EXPIRED, DELETED }

@Entity
@Table(name = "inboxes")
class Inbox(
	@Id
	val id: UUID,
	val emailAddress: String,
	val accessTokenHash: String,
	val createdAt: Instant,
	var expiresAt: Instant,
	@Enumerated(EnumType.STRING)
	var status: InboxStatus = InboxStatus.ACTIVE,
) {
	fun isActiveAt(now: Instant): Boolean =
		status == InboxStatus.ACTIVE && expiresAt.isAfter(now)
}
