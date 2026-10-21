package com.tanmay.tempmail.temp_mail.message

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

/**
 * Email HTML is untrusted. Scripts, styles, and event handlers are stripped, and images are
 * removed entirely so opening a message never contacts a sender's tracking server.
 */
object HtmlSanitizer {
	private val safelist = Safelist.relaxed()
		.removeTags("img")
		.addEnforcedAttribute("a", "rel", "noopener noreferrer nofollow")

	fun sanitize(html: String): String = Jsoup.clean(html, safelist)
}
