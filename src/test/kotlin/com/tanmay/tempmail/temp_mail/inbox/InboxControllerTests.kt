package com.tanmay.tempmail.temp_mail.inbox

import com.jayway.jsonpath.JsonPath
import com.tanmay.tempmail.temp_mail.config.AppProperties
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InboxControllerTests {

	@Autowired
	lateinit var mockMvc: MockMvc

	@Autowired
	lateinit var inboxRepository: InboxRepository

	@Autowired
	lateinit var properties: AppProperties

	private data class Created(val id: UUID, val token: String)

	private fun createInbox(): Created {
		val body = mockMvc.post("/api/inboxes").andReturn().response.contentAsString
		return Created(UUID.fromString(JsonPath.read(body, "$.id")), JsonPath.read(body, "$.accessToken"))
	}

	@Test
	fun `create returns a random address on the configured domain`() {
		mockMvc.post("/api/inboxes").andExpect {
			status { isCreated() }
			jsonPath("$.address") { value(matchesPattern("^[a-z0-9]{10}@.+$")) }
			jsonPath("$.address") { value(endsWith("@${properties.mailDomain}")) }
			jsonPath("$.accessToken") { isNotEmpty() }
			jsonPath("$.expiresAt") { isNotEmpty() }
		}
	}

	@Test
	fun `token is stored only as a hash`() {
		val created = createInbox()
		val stored = inboxRepository.findById(created.id).orElseThrow()
		assertNotEquals(created.token, stored.accessTokenHash)
		assertEquals(AccessTokens.hash(created.token), stored.accessTokenHash)
	}

	@Test
	fun `get returns inbox with valid token and hides the token`() {
		val created = createInbox()
		mockMvc.get("/api/inboxes/${created.id}") {
			header(HttpHeaders.AUTHORIZATION, "Bearer ${created.token}")
		}.andExpect {
			status { isOk() }
			jsonPath("$.id") { value(created.id.toString()) }
			jsonPath("$.accessToken") { doesNotExist() }
		}
	}

	@Test
	fun `get without token is unauthorized`() {
		val created = createInbox()
		mockMvc.get("/api/inboxes/${created.id}").andExpect { status { isUnauthorized() } }
	}

	@Test
	fun `wrong token and unknown id are indistinguishable`() {
		val created = createInbox()
		mockMvc.get("/api/inboxes/${created.id}") {
			header(HttpHeaders.AUTHORIZATION, "Bearer wrong-token")
		}.andExpect { status { isNotFound() } }
		mockMvc.get("/api/inboxes/${UUID.randomUUID()}") {
			header(HttpHeaders.AUTHORIZATION, "Bearer ${created.token}")
		}.andExpect { status { isNotFound() } }
	}

	@Test
	fun `expired inbox is not found`() {
		val token = AccessTokens.generate()
		val now = Instant.now()
		val expired = inboxRepository.saveAndFlush(
			Inbox(
				id = UUID.randomUUID(),
				emailAddress = "expired-${UUID.randomUUID()}@${properties.mailDomain}",
				accessTokenHash = AccessTokens.hash(token),
				createdAt = now.minusSeconds(600),
				expiresAt = now.minusSeconds(1),
			)
		)
		mockMvc.get("/api/inboxes/${expired.id}") {
			header(HttpHeaders.AUTHORIZATION, "Bearer $token")
		}.andExpect { status { isNotFound() } }
	}

	@Test
	fun `delete marks inbox deleted and it is no longer readable`() {
		val created = createInbox()
		mockMvc.delete("/api/inboxes/${created.id}") {
			header(HttpHeaders.AUTHORIZATION, "Bearer ${created.token}")
		}.andExpect { status { isNoContent() } }
		assertEquals(InboxStatus.DELETED, inboxRepository.findById(created.id).orElseThrow().status)
		mockMvc.get("/api/inboxes/${created.id}") {
			header(HttpHeaders.AUTHORIZATION, "Bearer ${created.token}")
		}.andExpect { status { isNotFound() } }
	}
}
