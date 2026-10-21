package com.tanmay.tempmail.temp_mail.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app")
data class AppProperties(
	val mailDomain: String,
	val inbox: Inbox,
	val ingest: Ingest,
) {
	data class Inbox(
		val defaultTtlMinutes: Long,
		val maxMessages: Int,
	)

	data class Ingest(
		val secret: String,
		val maxMessageBytes: Int,
	) {
		init {
			require(secret.length >= 32) { "app.ingest.secret (INGEST_SECRET) must be at least 32 characters" }
		}
	}
}
