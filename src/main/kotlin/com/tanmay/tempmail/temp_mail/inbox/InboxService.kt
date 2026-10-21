package com.tanmay.tempmail.temp_mail.inbox

import com.tanmay.tempmail.temp_mail.config.AppProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.UUID

data class CreatedInbox(val inbox: Inbox, val accessToken: String)

@Service
class InboxService(
	private val inboxRepository: InboxRepository,
	private val properties: AppProperties,
	private val clock: Clock,
) {
	private val random = SecureRandom()

	@Transactional
	fun create(): CreatedInbox {
		val now = clock.instant()
		val token = AccessTokens.generate()
		val inbox = Inbox(
			id = UUID.randomUUID(),
			emailAddress = generateUniqueAddress(),
			accessTokenHash = AccessTokens.hash(token),
			createdAt = now,
			expiresAt = now.plus(Duration.ofMinutes(properties.inbox.defaultTtlMinutes)),
		)
		return CreatedInbox(inboxRepository.save(inbox), token)
	}

	/** Returns the inbox only if it exists, is still active, and the token matches. */
	@Transactional(readOnly = true)
	fun findAuthorized(id: UUID, token: String): Inbox? =
		inboxRepository.findById(id).orElse(null)
			?.takeIf { it.isActiveAt(clock.instant()) && AccessTokens.matches(token, it.accessTokenHash) }

	@Transactional
	fun delete(id: UUID, token: String): Boolean {
		val inbox = findAuthorized(id, token) ?: return false
		inbox.status = InboxStatus.DELETED
		return true
	}

	private fun generateUniqueAddress(): String {
		repeat(MAX_ADDRESS_ATTEMPTS) {
			val address = "${randomLocalPart()}@${properties.mailDomain}"
			if (!inboxRepository.existsByEmailAddress(address)) return address
		}
		error("Could not generate a unique address after $MAX_ADDRESS_ATTEMPTS attempts")
	}

	private fun randomLocalPart(): String =
		(1..LOCAL_PART_LENGTH).map { LOCAL_PART_ALPHABET[random.nextInt(LOCAL_PART_ALPHABET.length)] }
			.joinToString("")

	private companion object {
		const val LOCAL_PART_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
		const val LOCAL_PART_LENGTH = 10
		const val MAX_ADDRESS_ATTEMPTS = 5
	}
}
