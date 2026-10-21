package com.tanmay.tempmail.temp_mail.message

import com.tanmay.tempmail.temp_mail.inbox.Inbox
import com.tanmay.tempmail.temp_mail.inbox.InboxService
import com.tanmay.tempmail.temp_mail.inbox.bearerToken
import com.tanmay.tempmail.temp_mail.inbox.inboxNotFound
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

data class MessageSummary(
	val id: UUID,
	val from: String?,
	val subject: String?,
	val receivedAt: Instant,
	val attachmentCount: Int,
)

data class MessageDetail(
	val id: UUID,
	val from: String?,
	val subject: String?,
	val receivedAt: Instant,
	val textBody: String?,
	val htmlBody: String?,
	val attachmentCount: Int,
)

@RestController
@RequestMapping("/api/inboxes/{inboxId}/messages")
class MessageController(
	private val inboxService: InboxService,
	private val messageService: MessageService,
) {

	@GetMapping
	fun list(
		@PathVariable inboxId: UUID,
		@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
	): List<MessageSummary> =
		messageService.list(authorizedInbox(inboxId, authorization)).map {
			MessageSummary(it.id, it.fromHeader, it.subject, it.receivedAt, it.attachmentCount)
		}

	@GetMapping("/{messageId}")
	fun get(
		@PathVariable inboxId: UUID,
		@PathVariable messageId: UUID,
		@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
	): MessageDetail {
		val message = messageService.find(authorizedInbox(inboxId, authorization), messageId)
			?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found")
		return MessageDetail(
			message.id, message.fromHeader, message.subject, message.receivedAt,
			message.textBody, message.htmlBody, message.attachmentCount,
		)
	}

	private fun authorizedInbox(inboxId: UUID, authorization: String?): Inbox =
		inboxService.findAuthorized(inboxId, bearerToken(authorization)) ?: throw inboxNotFound()
}
