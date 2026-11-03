# Implementation delivery — 2026-11-03 (Asia/Ho_Chi_Minh)

This is the repository MVP implementation, retaining React, Spring Boot and MariaDB. External delivery/model credentials and browser acceptance testing are separate from the checks below. The new user specification supersedes conflicting older Specs.

## Implemented

| Area | Behavior | Verification |
|---|---|---|
| Authentication | Parent/tutor signup, independent email/phone activation, BCrypt, generic invalid-credential response, expiring opaque sessions, logout/reset revocation, CSRF and throttling; no public admin signup | MariaDB + Spring MVC integration tests, including real CSRF cookie/header exchange |
| Students | Multiple owned profiles; consent timestamp; subject/grade/budget/schedule validation; six-question survey plus free-text needs; at most 15 survey answers in API | Integration tests cover ownership, consent and role boundaries; React compiled |
| Tutors | Teaching profile editor, subjects/grades/formats/schedule/rate/methodology/experience; edits reset approval | Service/API integration tests; React compiled |
| Verification | Private bounded uploads, metadata, human admin review tied to current submission, rejection reasons, audit, visibility control | Integration tests cover missing evidence, unauthorized reads, approval-before-review and reapproval after edits |
| Recommendation | Strict eligibility; up to 20 anonymized candidates; replaceable chat-completions LLM adapter; <=5 validated qualitative results; deterministic fallback | Unit tests cover every mandatory constraint and invalid AI output; integration tests cover valid AI, fallback, privacy and a stalled provider returning fallback before ten seconds |
| Discovery | Vietnamese tutor cards, pointer drag left/right, equivalent accessible buttons, detail dialog, persisted swipe; interest explicitly separate from acceptance | API persistence/eligibility tests; frontend build/lint; browser gestures not yet exercised |
| Trials | Proposed time/message; unique pending pair; accepted/rejected/expired states; ownership; contact access only on acceptance | Integration tests cover duplicates, concurrent duplicate submissions, expiration, unauthorized responses, invalid transitions and contact gating |
| Operations | Additive V2 migration, Docker Compose, CI-compatible tests, env template, API and setup docs | MariaDB migration/startup verified; production frontend/backend Docker builds; HTTP checks via nginx |

## Files delivered

- Backend `auth/`: security configuration, account APIs/service, delivery gateway and admin bootstrap.
- Backend `platform/`: profile DTO/service/API, verification workflow, LLM interface/adapter, recommendations, trial/swipe APIs and structured errors.
- `V2__mvp_workflows.sql`; existing V1 preserved. Existing demo assertion now checks only demo fixtures so legitimate approved fixtures do not invalidate it.
- Tests: `MvpIntegrationTest`, `BrowserSecurityIntegrationTest`, `RecommendationServiceTest`; existing cosine and migration/demo tests retained.
- React `App.jsx`, feature components under `components/`, shared API/CSRF client and constants, responsive Vietnamese styles; nginx upload/cache/forwarding configuration.
- `.env.example`, Compose and backend Docker build caching; README, architecture, plan, environment, API, migrations and this status document.

## Checks and acceptance evidence

Backend suite: 17 tests in total (including retained tests), run with Maven verify in Docker against isolated MariaDB 11.4. Provider delivery and ranking are mocked only in workflow tests; the default app has no mock users, fake activation or sample recommended cards. Fresh V1/V2 migration and subsequent migration validation have both been exercised.

Frontend: Vite production build and existing Oxlint CI check. Real HTTP smoke checks through nginx: `/` 200, `/api/health` 200, `/api/auth/csrf` 200 with CSRF cookie, anonymous `/api/students` 401, mutation without CSRF 403, correctly protected invalid login 401. These checks do not constitute browser interaction tests.

## Remaining configuration and limits

- **Real email/SMS delivery is not configured or live-tested.** Supply a trusted delivery webhook implementing the API contract. Signup fails closed with 503 until this is configured; no secret is returned to frontend. Delivery during registration is synchronous; a partial external delivery failure can send a now-invalid code before the DB transaction rolls back. Retry signup/resend after resolving delivery. A transactional outbox is a future reliability improvement.
- **Live LLM integration/model quality is not verified.** Supply endpoint, key and model if desired. The deterministic path remains operational without these. The application uses broad allowlisted teaching/learning categories; it does not send the student's detailed free-text narrative. It makes no scientific efficacy claim.
- **No browser/device acceptance test has been run.** Mouse/touch gestures, layout across devices, screen readers and full UI journeys still need browser QA. Build and HTTP checks passed; they do not prove these interactions.
- Trial status updates use refresh; no push/email notification system. Expiration is enforced on access and a proposed time is a request, not a reserved calendar slot.
- Weekly availability is coarse AM/PM/EVENING; location matching requires consistent province names; no geocoding or date-specific booking/conflict engine. The UI offers seven canonical subjects and six survey questions.
- Active view is held in React memory; page refresh opens the landing page after session restoration. Deep links are not implemented.
- Identity review is manual. File signatures/size/type are checked, but malware scanning/OCR/fraud detection are not included. Deployment must configure encrypted DB/backups, restricted access and evidence retention. Expired session/challenge/throttle rows currently need operational cleanup.
- SQL candidate hydration is intended for MVP scale; paginate/batch relation loading and profile queries before a large public rollout. Auth sessions are stored in MariaDB and checked per authenticated request.
- Configure a private admin via bootstrap, HTTPS/secure cookies and deployment secrets before public use. No hosted deployment, provider subscription or production security audit is included.
- Legacy V1 trial/review records require an explicit data reconciliation if deploying over a populated pre-MVP database; no automatic historical data conversion is performed.

## Local account handoff follow-up

On the user's request, three local demo accounts (PARENT, TUTOR, ADMIN) were manually provisioned and activated in the local database. Their randomly generated password is supplied in chat and stored only as BCrypt in the database; credentials are not seeded by application startup. These fixtures do not establish real email/phone verification or tutor identity approval. No approved tutor profile or student data was added.

Live client checks exposed CSRF token rotation after authentication. The frontend API client now obtains the current CSRF token before each mutation. A Node-based check using the actual API client against nginx verified parent login, two consecutive consent-rejected mutations and logout; no student information was stored. All three account logins and roles were verified through HTTP. Browser interaction QA remains outstanding.
