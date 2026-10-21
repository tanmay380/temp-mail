package com.tanmay.tempmail.temp_mail.message

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "messages")
class Message(
	@Id
	val id: UUID,
	val inboxId: UUID,
	val fromHeader: String?,
	val subject: String?,
	val textBody: String?,
	val htmlBody: String?,
	val attachmentCount: Int,
	val sizeBytes: Int,
	val receivedAt: Instant,
)
