# REST API

Base `/api`, JSON unless stated. Vietnamese errors use `{ "message": "..." }`. 400 validation, 401 authentication, 403 role/CSRF, 404 inaccessible resources, 409 duplicate/state conflict, 429 throttled, 503 delivery unavailable. IDs are UUIDs. Dates are ISO-8601 instants; frontend displays local time. Session is an opaque HttpOnly SameSite=Lax cookie valid for eight hours. Passwords are BCrypt hashes; session/challenge tokens are SHA-256 digests.

Before mutations: GET `/auth/csrf`, send returned `token` as `X-XSRF-TOKEN`, retain cookies. Login requires verified email only; phone numbers are unverified contact information. No public administrator registration.

| Method | Path | Role / payload |
|---|---|---|
| GET | /health | Public |
| GET | /auth/csrf | Public, issues CSRF cookie |
| POST | /auth/register | Public: email, phone in E.164, name, password (12–72 characters, <=72 UTF-8 bytes), role PARENT or TUTOR |
| POST | /auth/login | Public: email, password; returns id/email/name/role and session cookie |
| GET | /auth/me | Authenticated account |
| POST | /auth/logout | Authenticated; revokes current session |
| POST | /auth/verify | Public: challengeId, code; email activation challenge |
| POST | /auth/resend | Public: email; replaces an unverified email challenge |
| POST | /auth/forgot | Public: email; generic confirmation |
| POST | /auth/reset | Public: challengeId, code, password; revokes all sessions |
| GET, POST | /students | Parent list/create |
| GET, PUT | /students/{id} | Owning parent read/update |
| GET | /students/{id}/interests | Owning parent; saved interested tutors still public and eligible |
| DELETE | /students/{id}/interests/{tutorId} | Owning parent removes a saved interest; 204 |
| GET | /students/{id}/recommendations | Owning parent, <=5 eligible results; AI or RULES source |
| GET, PUT | /tutor/profile | Tutor own profile; every save returns to DRAFT |
| GET, POST | /tutor/documents | Tutor list metadata/upload multipart `file`, PDF/PNG/JPEG <=5 MB, <=5 documents |
| DELETE | /tutor/documents/{id} | Owning tutor, DRAFT or REJECTED |
| POST | /tutor/submit | Tutor, requires complete profile and identity evidence |
| GET | /tutors/{id} | Authenticated; only visible approved/activated tutors; no contact/evidence |
| GET | /admin/tutors | Admin verification queue |
| GET | /admin/tutors/{id} | Admin profile/document metadata and decision audit |
| GET | /admin/documents/{id} | Admin attachment; records evidence access for current submission |
| POST | /admin/tutors/{id}/decision | Admin: approve boolean, reason; rejection requires reason; approval requires evidence access since submission |
| PUT | /admin/tutors/{id}/visibility | Admin: visible boolean |
| POST | /swipes | Parent: studentId, tutorId, interested boolean; persists interest/skip only |
| POST | /lessons | Parent: studentId, tutorId, proposedAt, message; pending expires after seven days or proposed date, whichever first |
| GET | /lessons | Parent/tutor own requests; contact fields null before acceptance |
| PUT | /lessons/{id} | Owning tutor: status ACCEPTED or REJECTED, only from unexpired PENDING |

Student input: `nickname`, `grade` 1–12, `subjects` string array, `province`, `mode` ONLINE/IN_PERSON, `budget` VND/hour, `availability` slot array, `needs` (<=3000), `survey` map (<=15 entries, each value <=500), `consent: true`. Schedule slots: `1_AM` through `7_EVENING`, 1=Monday, 7=Sunday; each AM/PM/EVENING. Parent consent is recorded on every save. Multiple subjects require a tutor supporting **all** selected subjects. In-person location uses case-insensitive exact province matching; use consistent names.

Tutor input: `displayName`, `photoUrl` optional HTTPS string, `subjects`, `grades` integer array, `modes` string array, `province`, `hourlyRate`, `methodology`, `style`, `background`, `experience` years, `availability`. API output uses explicit response DTOs preserving snake_case for stored profile fields; computed arrays use subjects/grades/modes/availability. DTO input uses camelCase.

Recommendation response: array of `{ tutor, level: STRONG|GOOD|POSSIBLE, strengths: string[], concerns: string[], explanation, source: AI|RULES }`. Labels are qualitative references, never scientific probabilities. Previously swiped tutors are excluded for that student; interested tutors remain accessible through `/students/{id}/interests`; saving a student does not erase history. Fetch new suggestions to continue beyond five.

## Email delivery

SMTP sends a ten-minute single-use verification or password-reset link and the corresponding challengeId/code. The frontend reads link parameters from its hash route and removes secrets from the address bar. `MAIL_*` settings configure SMTP; `APP_PUBLIC_URL` controls the frontend origin. Local Compose uses the Mailpit inbox at http://localhost:8025. Mailpit captures local messages; external delivery requires an SMTP account. SMTP failures return 503. No verification secret is returned by the application API. Both activation and reset challenges are attempt-limited. SMS is not used.

## LLM adapter

LLM_URL is the full chat-completions-compatible POST endpoint, with LLM_MODEL and an optional LLM_API_KEY. Local Ollama works without a key; Compose configures its endpoint and model. Server requests JSON-object output. Only candidate pseudonyms, subjects, years of experience, grade and allowlisted learning/teaching categories are sent. Names, raw free text, addresses, budgets, calendars and contact data are excluded. At most 20 rule-eligible candidates enter ranking; at most five validated unique known IDs may return. Invented IDs, invalid labels, excessive text, percentage claims, errors, malformed or oversized output fall back. Request deadline eight seconds; orchestration deadline nine seconds; concurrent calls bounded to eight. No provider is required for deterministic matching. Evaluate recommendation quality with real consenting families before making efficacy claims.
