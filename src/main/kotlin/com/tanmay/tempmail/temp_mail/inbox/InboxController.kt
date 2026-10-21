package com.tanmay.tempmail.temp_mail.inbox

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class CreateInboxResponse(
	val id: UUID,
	val address: String,
	val accessToken: String,
	val createdAt: Instant,
	val expiresAt: Instant,
)

data class InboxResponse(
	val id: UUID,
	val address: String,
	val createdAt: Instant,
	val expiresAt: Instant,
)

@RestController
@RequestMapping("/api/inboxes")
class InboxController(private val inboxService: InboxService) {

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	fun create(): CreateInboxResponse {
		val (inbox, token) = inboxService.create()
		return CreateInboxResponse(inbox.id, inbox.emailAddress, token, inbox.createdAt, inbox.expiresAt)
	}

	@GetMapping("/{id}")
	fun get(
		@PathVariable id: UUID,
		@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
	): InboxResponse {
		val inbox = inboxService.findAuthorized(id, bearerToken(authorization)) ?: throw inboxNotFound()
		return InboxResponse(inbox.id, inbox.emailAddress, inbox.createdAt, inbox.expiresAt)
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(
		@PathVariable id: UUID,
		@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
	) {
		if (!inboxService.delete(id, bearerToken(authorization))) throw inboxNotFound()
	}
}
