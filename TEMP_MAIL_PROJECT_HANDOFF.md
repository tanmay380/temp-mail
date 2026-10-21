# Temp Mail Project — Handoff Guide

> Purpose: Give another AI assistant enough context to continue implementing this project without requiring the full previous conversation.
> Last updated: 2026-10-21

## 1. Project goal

Build a public temporary email service (Temp Mail website) that lets visitors generate disposable email addresses and view incoming messages.

The user wants a **simple, inexpensive hybrid hosting approach** and is implementing the backend first.

### Working style
- Give practical, sequential instructions with exact commands and file paths.
- Explain what each step accomplishes, but avoid jumping ahead.
- Prefer secure, low-cost, maintainable choices.
- Ask for command output or error logs when needed; do not assume a step succeeded.
- Keep this document updated as major decisions and implementation milestones change.

## 2. Development environment

| Item | Current setup |
|---|---|
| OS | Windows |
| IDE | IntelliJ IDEA |
| Java | Java 21 |
| Backend | Spring Boot (Java) |
| Build | Gradle |
| Database | PostgreSQL running in Docker |
| Domain/DNS | `tanmay.store`, managed in Cloudflare |
| Email DNS/routing | Cloudflare Email Routing |
| Project | Temp Mail |
| Local time zone | Asia/Kolkata |

Some details (Spring Boot version, Gradle wrapper version, database container name/credentials, repository path, and current Git status) have not been recorded here. Ask the user or inspect the project before relying on them.

### Verified repository state (2026-10-21)
- Repo also lives on a Linux machine at `~/Documents/Projects/temp-mail` (behind a corporate HTTP proxy).
- Language is **Kotlin** (2.3.21), not Java. Spring Boot **4.1.1**, Gradle wrapper **9.8.1**, package `com.tanmay.tempmail.temp_mail`.
- Gradle toolchain set to **Java 21** (was 25, which caused the toolchain error). `compileKotlin` + `compileTestKotlin` succeed.
- `compose.yaml` runs `postgres:17` as container `temp-mail-postgres`, DB/user `tempmail`, bound to `127.0.0.1:5432`.
- Root cause of the old `database "temp_mail" does not exist` error: `application.yaml` used `temp_mail` while compose creates `tempmail`. Fixed: datasource now defaults to `tempmail`.
- DB password is no longer hardcoded: both compose and Spring read `POSTGRES_PASSWORD` from the git-ignored `.env` (`spring.config.import: optional:file:.env[.properties]`).
- Flyway migration `V1__create_inboxes.sql` defines `inboxes` (UUID id, email_address, access_token_hash, created_at, expires_at, status).
- `MAIL_DOMAIN=tanmay.store` is set in `.env` (bound to `app.mail-domain`). Inbox TTL: `app.inbox.default-ttl-minutes` (10).

### Inbox API (implemented, tested)
Package `com.tanmay.tempmail.temp_mail.inbox`:
- `POST /api/inboxes` → `201` `{id, address, accessToken, createdAt, expiresAt}`. Address = 10 random `[a-z0-9]` chars @ mail domain. Token = 256-bit SecureRandom, base64url, **returned once**; DB stores SHA-256 hex only.
- `GET /api/inboxes/{id}` with `Authorization: Bearer <token>` → `200` `{id, address, createdAt, expiresAt}`.
- `DELETE /api/inboxes/{id}` with bearer token → `204`; soft delete (`status = DELETED`).
- Missing/malformed bearer → `401`. Unknown id, wrong token, expired, or deleted → `404` (indistinguishable by design).
- Expiry is checked on read; there is no background cleanup job yet.
- Tests: `InboxControllerTests` (7) + `TempMailApplicationTests` (context + `/actuator/health`). All pass against local Postgres; each test rolls back.
- Not yet built: messages table/endpoints, inbound mail ingestion, expiry cleanup job, rate limiting, CORS.
- Linux machine: dockerd proxy configured via `/etc/systemd/system/docker.service.d/http-proxy.conf`. Postgres container is up and healthy.
- `./gradlew test` passes (context loads against real Postgres). Flyway V1 applied; `inboxes` table exists.

## 3. Current architecture and decisions

The project is intended to use a hybrid, low-cost setup:
- **Web/backend:** Spring Boot application.
- **Database:** PostgreSQL in Docker during development; production hosting decision is not fully recorded.
- **Domain and DNS:** Cloudflare, using `tanmay.store`.
- **Email routing:** Cloudflare Email Routing has been configured and verified for `tanmay.store`.
- **Important distinction:** Cloudflare Email Routing is primarily a forwarding/routing feature. It does **not by itself provide a general-purpose inbound mailbox API** for a public disposable-email service. Before building message ingestion, verify what inbound-email mechanism will receive raw messages and deliver them to the application. Do not assume forwarded mail can automatically be read from the application.
- **Cost goal:** Keep recurring costs low. Explain unavoidable hosting, domain, email delivery/receiving, storage, and abuse-prevention costs before recommending a paid architecture.

Do not expose or commit API tokens, database passwords, Cloudflare credentials, private keys, or other secrets. Use environment variables or a local `.env` file excluded from Git.

## 4. Domain and Cloudflare status

Domain: `tanmay.store`

Cloudflare Email Routing DNS records were added and the user reported the status as:

**Verified / Configured / Ready**

The user also reported that Email Routing is working.

Earlier, the MX information shown included:
- Hostname: `tanmay.store`
- Priority: `68`
- Value: `route1.mx.cloudflare...` (the full value was not preserved in the handoff notes)

Five Email Routing DNS records were saved and subsequently verified. Do not ask the user to repeat DNS setup unless diagnosing a specific problem.

### Critical next architecture check
Before continuing, establish the intended inbound email flow:
1. A sender sends mail to a generated address such as `random@tanmay.store`.
2. The receiving system accepts mail for that address.
3. The raw message is made available to the Temp Mail backend (webhook, API, IMAP/POP mailbox, or another supported ingestion method).
4. The backend validates and stores the message and attachments safely.
5. The website displays the message to the correct temporary inbox.

Confirm whether Cloudflare Email Routing supports the exact receiving/ingestion behavior required. If not, choose an inbound email provider or another receiving setup that supports catch-all addresses and message delivery to the backend. Compare cost, setup complexity, retention, rate limits, and abuse controls before choosing. **Do not claim that Email Routing alone completes inbound message ingestion.**

## 5. Backend setup progress

The user started implementation and has already encountered and discussed these setup issues:

### Gradle / Java toolchain
- There was a Gradle Java toolchain issue.
- Current intended Java version is Java 21.
- When debugging, check `java -version`, `./gradlew -version` (or `gradlew.bat -version` on Windows), and the project's Gradle toolchain configuration.
- Keep the configured Java toolchain aligned with Java 21.

### PostgreSQL database
The application previously failed with:

```text
SQL State  : 3D000
Error Code : 0
Message    : FATAL: database "temp_mail" does not exist
```

This means PostgreSQL was reachable but the database named `temp_mail` had not been created (or the configured database name did not match). The issue should be considered resolved only if the user confirmed it; that confirmation is not preserved here. Verify the database exists and that Spring datasource configuration matches it.

### Flyway / JPA
A startup error was reported involving:
- `entityManagerFactory`
- `flywayInitializer`
- `HibernateJpaConfiguration`

The visible underlying error was the missing `temp_mail` database above. Diagnose the first/root exception in the current logs instead of assuming the old issue remains.

### First endpoint
The user requested the initial setup to include:
- Exact Step 1 setup commands
- Spring Initializr configuration
- Project structure
- First runnable endpoint

The exact endpoint path and current source files are not preserved in this handoff. Inspect the repository and continue from its actual state instead of recreating files blindly.

## 6. Suggested high-level implementation roadmap

Treat this as a roadmap, not a claim that these features are already implemented.

1. **Inspect current repository**
   - Identify current Spring Boot version, Gradle configuration, packages, endpoints, migrations, and tests.
   - Run the app and existing tests before changing anything.
2. **Make the first endpoint reliable**
   - Add a simple health/status endpoint and a smoke test.
   - Confirm app startup and database connectivity.
3. **Define mailbox lifecycle**
   - Generate cryptographically random, unguessable mailbox identifiers.
   - Set an explicit expiry/retention period.
   - Decide whether users can renew or extend mailbox lifetime.
4. **Choose inbound email ingestion**
   - Select a provider or receiving mechanism that supports catch-all addresses and forwards messages to an API/webhook or accessible mailbox.
   - Validate webhook authentication/signatures and provider retries.
5. **Data model and migrations**
   - Mailbox/address, message metadata and body, attachment metadata, expiration/cleanup state.
   - Use Flyway migrations; avoid relying on Hibernate auto-DDL for production.
6. **Message processing**
   - Parse MIME safely, sanitize HTML, limit message and attachment sizes, and handle malformed input.
   - Do not render raw email HTML without sanitization.
7. **Public API**
   - Create mailbox, fetch mailbox details, list messages, read a message, delete/expire mailbox.
   - Ensure each request is authorized by a hard-to-guess mailbox token or equivalent capability.
8. **Frontend**
   - Public landing page, generated address with copy action, inbox refresh/polling, message detail view, expiration indicator.
9. **Abuse prevention and privacy**
   - Rate limits, request throttling, abuse reporting, CAPTCHA or equivalent only where warranted, and operational monitoring.
   - Define a clear retention policy; minimize stored personal data.
   - Avoid becoming an open relay. The service should receive mail only; sending mail is not required unless explicitly decided later.
10. **Deployment**
    - Pick an inexpensive hosting option compatible with the inbound email provider.
    - Configure HTTPS, environment variables/secrets, logs, database backups, health checks, and migrations.
11. **Testing and launch**
    - Unit/integration tests, malformed email tests, expiration cleanup tests, rate-limit tests, and end-to-end delivery tests.
    - Test with real external email senders before public launch.

## 7. Security and privacy requirements

A public temporary-email service can receive spam, phishing, malware, and sensitive content. Design for safe handling from the beginning.

- Do not execute attachments or automatically fetch remote resources referenced by email HTML.
- Sanitize HTML and escape plain text appropriately.
- Restrict attachment size and total mailbox storage.
- Store only what is needed; expire messages and attachments according to a documented retention policy.
- Use unguessable mailbox IDs/tokens; do not rely on the email address alone as a secret.
- Apply per-IP and per-mailbox rate limits, with proxy-aware client IP handling configured safely.
- Validate provider webhooks, use TLS, and avoid logging message bodies or secrets.
- Configure CORS narrowly if a separate frontend is used.
- Keep database and management endpoints private.
- Do not expose private inbox contents through predictable URLs or search engines.
- Add a clear acceptable-use and privacy notice before public launch.

## 8. Questions / decisions still open

Do not silently assume answers to these:
- Which inbound receiving provider or mechanism will be used?
- Which low-cost production hosting platform will host Spring Boot and PostgreSQL?
- Will attachments be supported in the first release?
- How long will mailboxes and messages live (for example, a short fixed retention period)?
- Will mailbox addresses be generated randomly only, or will custom names be allowed?
- What traffic, message-size, and storage limits should apply?
- Is the first frontend in scope now, or should backend and email ingestion be completed first?

When possible, recommend a simple default and explain tradeoffs instead of asking many questions at once.

## 9. Instructions for the next AI assistant

Start by acknowledging this handoff file and then:
1. Ask the user to share the current repository structure or relevant files **only if necessary**; otherwise work from the project files already available.
2. Check the actual state of the implementation before prescribing changes.
3. Prioritize resolving the inbound email ingestion architecture, because DNS verification alone does not prove that messages can be ingested by the app.
4. Continue in small, testable steps with exact Windows/PowerShell commands where appropriate.
5. Do not repeat completed Cloudflare Email Routing DNS instructions unless a concrete issue requires it.
6. Do not invent successful setup results, endpoint names, credentials, or hosting choices.
7. Update this handoff file when key decisions or milestones change.

## 10. Prompt to paste into another AI

You are helping me continue my **Temp Mail** project. Read `TEMP_MAIL_PROJECT_HANDOFF.md` first and treat it as the project context. I am using Windows, IntelliJ IDEA, Java 21, Spring Boot with Gradle, and PostgreSQL in Docker. My domain is `tanmay.store`, managed through Cloudflare. Cloudflare Email Routing is verified/configured and reported working, but we still need to ensure there is a real inbound-email ingestion path that can deliver messages to the application; DNS verification alone is not enough. Continue from the actual repository state, give exact step-by-step instructions and Windows commands, explain the purpose of each change, and ask for logs when a result needs verification. Keep costs low, prioritize security and privacy, and update the handoff file as the project evolves.
