 package com.tanmay.tempmail.temp_mail.message

import jakarta.mail.MessagingException
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeUtility
import java.io.ByteArrayInputStream
import java.util.Properties

data class ParsedEmail(
	val from: String?,
	val subject: String?,
	val textBody: String?,
	val htmlBody: String?,
	val attachmentCount: Int,
)

class MalformedEmailException(cause: Throwable) : RuntimeException("Malformed email", cause)

/** Extracts headers and the first text/HTML bodies from a raw RFC 822 message. Attachments are counted, not kept. */
object EmailParser {
	private const val MAX_HEADER_LENGTH = 998
	private val session = Session.getInstance(Properties())

	fun parse(raw: ByteArray): ParsedEmail {
		try {
			val message = MimeMessage(session, ByteArrayInputStream(raw))
			val bodies = Bodies()
			collect(message, bodies)
			return ParsedEmail(
				from = message.getHeader("From", ", ")?.let(::decodeHeader),
				subject = message.getHeader("Subject", " ")?.let(::decodeHeader),
				textBody = bodies.text,
				htmlBody = bodies.html,
				attachmentCount = bodies.attachments,
			)
		} catch (e: MessagingException) {
			throw MalformedEmailException(e)
		}
	}

	private class Bodies {
		var text: String? = null
		var html: String? = null
		var attachments = 0
	}

	private fun collect(part: Part, bodies: Bodies) {
		when {
			Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) || part.fileName != null ->
				bodies.attachments++
			part.isMimeType("multipart/*") -> {
				val multipart = part.content as? Multipart ?: return
				for (i in 0 until multipart.count) collect(multipart.getBodyPart(i), bodies)
			}
			part.isMimeType("text/plain") -> if (bodies.text == null) bodies.text = textContent(part)
			part.isMimeType("text/html") -> if (bodies.html == null) bodies.html = textContent(part)
			else -> bodies.attachments++
		}
	}

	// Unknown charsets throw from getContent(); skip that body rather than rejecting the whole email.
	private fun textContent(part: Part): String? = runCatching { part.content as? String }.getOrNull()

	private fun decodeHeader(value: String): String =
		runCatching { MimeUtility.decodeText(MimeUtility.unfold(value)) }.getOrDefault(value)
			.take(MAX_HEADER_LENGTH)
}
