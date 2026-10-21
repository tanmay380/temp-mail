package com.tanmay.tempmail.temp_mail.message

import com.tanmay.tempmail.temp_mail.config.AppProperties
import com.tanmay.tempmail.temp_mail.inbox.Inbox
import com.tanmay.tempmail.temp_mail.inbox.InboxRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

enum class IngestResult { STORED, UNKNOWN_RECIPIENT, INBOX_FULL }

@Service
class MessageService(
	private val inboxRepository: InboxRepository,
	private val messageRepository: MessageRepository,
	private val properties: AppProperties,
	private val clock: Clock,
) {

	@Transactional
	fun ingest(recipient: String, raw: ByteArray): IngestResult {
		val now = clock.instant()
		val inbox = inboxRepository.findByEmailAddress(recipient.trim().lowercase())
			?.takeIf { it.isActiveAt(now) }
			?: return IngestResult.UNKNOWN_RECIPIENT
		if (messageRepository.countByInboxId(inbox.id) >= properties.inbox.maxMessages) {
			return IngestResult.INBOX_FULL
		}

		val parsed = EmailParser.parse(raw)
		messageRepository.save(
			Message(
				id = UUID.randomUUID(),
				inboxId = inbox.id,
				fromHeader = parsed.from,
				subject = parsed.subject,
				textBody = parsed.textBody,
				htmlBody = parsed.htmlBody?.let(HtmlSanitizer::sanitize),
				attachmentCount = parsed.attachmentCount,
				sizeBytes = raw.size,
				receivedAt = now,
			)
		)
		return IngestResult.STORED
	}

	@Transactional(readOnly = true)
	fun list(inbox: Inbox): List<Message> = messageRepository.findByInboxIdOrderByReceivedAtDesc(inbox.id)

	@Transactional(readOnly = true)
	fun find(inbox: Inbox, messageId: UUID): Message? = messageRepository.findByIdAndInboxId(messageId, inbox.id)
}
