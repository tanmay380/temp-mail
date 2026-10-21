package com.tanmay.tempmail.temp_mail.message

import com.tanmay.tempmail.temp_mail.config.AppProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest

/**
 * Receives raw emails from the Cloudflare Email Worker. The body is the full RFC 822 message;
 * the envelope recipient comes in a header because the To: header can differ (Bcc, aliases).
 */
@RestController
@RequestMapping("/internal/messages")
class IngestController(
	private val messageService: MessageService,
	private val properties: AppProperties,
) {

	@PostMapping(consumes = [MESSAGE_RFC822, MediaType.APPLICATION_OCTET_STREAM_VALUE])
	fun ingest(
		@RequestHeader(SECRET_HEADER, required = false) secret: String?,
		@RequestHeader(RECIPIENT_HEADER) recipient: String,
		request: HttpServletRequest,
	): ResponseEntity<Void> {
		if (secret == null || !MessageDigest.isEqual(secret.toByteArray(), properties.ingest.secret.toByteArray())) {
			throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		}

		// Read at most one byte past the limit so oversized bodies are never fully buffered.
		val maxBytes = properties.ingest.maxMessageBytes
		val raw = request.inputStream.readNBytes(maxBytes + 1)
		if (raw.size > maxBytes) throw ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE)

		return when (messageService.ingest(recipient, raw)) {
			IngestResult.STORED -> ResponseEntity.noContent().build()
			IngestResult.UNKNOWN_RECIPIENT -> ResponseEntity.notFound().build()
			IngestResult.INBOX_FULL -> ResponseEntity.status(HttpStatus.CONFLICT).build()
		}
	}

	@ExceptionHandler(MalformedEmailException::class)
	fun malformed(): ResponseEntity<Void> = ResponseEntity.badRequest().build()

	companion object {
		const val SECRET_HEADER = "X-Ingest-Secret"
		const val RECIPIENT_HEADER = "X-Envelope-To"
		const val MESSAGE_RFC822 = "message/rfc822"
	}
}
