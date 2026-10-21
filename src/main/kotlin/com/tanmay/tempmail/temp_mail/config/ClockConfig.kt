package com.tanmay.tempmail.temp_mail.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {

	// Injected instead of calling Instant.now() directly, so tests can control time.
	@Bean
	fun clock(): Clock = Clock.systemDefaultZone()
}
