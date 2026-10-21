package com.tanmay.tempmail.temp_mail.inbox

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

private const val BEARER_PREFIX = "Bearer "

/** Extracts the token from an `Authorization: Bearer <token>` header, or responds 401. */
fun bearerToken(authorization: String?): String =
    authorization?.takeIf { it.startsWith(BEARER_PREFIX) }?.removePrefix(BEARER_PREFIX)?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token")

// Unknown, expired, and wrong-token inboxes all look the same, so IDs cannot be probed.
fun inboxNotFound() = ResponseStatusException(HttpStatus.NOT_FOUND, "Inbox not found")
