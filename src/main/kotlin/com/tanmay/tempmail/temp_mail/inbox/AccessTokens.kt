package com.tanmay.tempmail.temp_mail.inbox

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.HexFormat

/**
 * Inbox access tokens are random 256-bit secrets. Only their SHA-256 hash is stored,
 * so a database leak does not expose working tokens.
 */
object AccessTokens {
	private val random = SecureRandom()
	private val encoder = Base64.getUrlEncoder().withoutPadding()

	fun generate(): String = ByteArray(32).also(random::nextBytes).let(encoder::encodeToString)

	fun hash(token: String): String =
		HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))

	fun matches(token: String, expectedHash: String): Boolean =
		MessageDigest.isEqual(hash(token).toByteArray(), expectedHash.toByteArray())
}
