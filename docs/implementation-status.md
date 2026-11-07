# Implementation delivery — 2026-11-07 (Asia/Ho_Chi_Minh)

The source has been reorganized into explicit backend layers and frontend screens/features. The latest user scope is **Sprints 1–4 only**, with email-only activation and deterministic discovery. AI and new work on Sprints 5–6 are deferred. React, Spring Boot, MariaDB and the original V1/V2 migration files are retained.

## Delivered behavior

| Area | Implementation |
|---|---|
| Backend structure | Controller → service → repository; typed domain models, validated request/response DTOs, dedicated mappers, business exceptions, security/configuration and integration packages. No SQL or JDBC handles in controllers/services. |
| Accounts | Parent/tutor registration, SMTP email links and manual-code fallback, resend, login/logout, password reset and eight-hour opaque sessions. Email verification is sufficient; phone is contact information. No public admin registration. |
| Security | BCrypt, hashed session/challenge secrets, CSRF, throttling, atomic single-use challenge consumption and reset session revocation. Attempt counters commit before consumption transactions so concurrent verification does not hold two database connections per request. |
| Students | Multiple owned profiles, consent timestamps, six-question survey, needs, subjects, grade, format, province, budget and weekly availability. Switching students clears recommendations, saved interests and open tutor/trial context. |
| Tutors | Full teaching-profile editor; edits reset approval/current submission. Subject/grade/mode relations are hydrated in batches. |
| Verification | Private bounded evidence upload/deletion, submission, current-submission evidence access before admin approval, rejection reasons, visibility control and decision history in the admin UI. |
| Discovery | Two-way rules-ranked preview cards for each student and approved visible tutor; anonymous student DTOs, incoming-interest priority, pointer swipes/buttons and loading/empty/error states. |
| Matching | Durable decisions per student/tutor pair, mutual Match, persistent notices for both accounts, seven-day interest expiry, atomic 3 incoming / 10 total OK daily quotas per profile (Vietnam time), idempotent retries and saved OK/Match views. Legacy swipe writes use the same rules. See [Sprint 4 verification](sprint4-matching.md). |
| Trials (retained existing workflow; outside current scope) | Proposed time/message, unique pending pair, tutor acceptance/rejection, lazy expiration and contact disclosure only after acceptance. Status pages refresh through REST. |
| Frontend | App/provider, reusable hooks, hash/email routing, pages, feature components and shared UI/API modules. Action locking, abortable page loads, session-expiry handling, form validation and accessible native dialogs. |
| Local integrations | Mailpit SMTP inbox in Compose. Existing AI adapter retained but disabled; experimental AI additions and setup helper removed. |
| Verification tooling | MariaDB regression suite, isolated browser-test Compose override, Playwright desktop/mobile workflows and browser checks in CI. The test image seeds Maven dependencies and preserves runtime cache in a dedicated volume. |

## Latest Sprint 3 verification — 2026-11-07

Profile saves and photo changes withdraw approval/pending submission; evidence changes do likewise. Resubmission requires a fresh admin evidence read. Decisions include submission ID and profile revision via additive migration V4. Latest checks: **56 backend tests** (`mvn test`), **6 Playwright desktop/mobile tests** with real MinIO in an isolated stack, frontend lint and production builds passed. See [verification.md](verification.md#sprint-3--reapproval-acceptance-2026-11-07) for acceptance steps. The counts below describe the earlier delivery run.

## Executed checks

| Check | Result |
|---|---|
| Backend Maven verify against isolated MariaDB | **24 tests passed**, 0 failures/errors/skips. Includes 12 concurrent email verifications, reset guess limits, saved interests, public DTO privacy, all mandatory matching constraints, concurrent duplicate trial requests, contact gating and AI deadline/HTTP body limits. |
| Frontend | Production build and Oxlint passed. |
| Browser acceptance | **4 tests passed** on Chromium desktop and 360px mobile emulation. Includes mouse/touch gestures, email registration, private admin review, survey/consent, saved interests after refresh, changing students, removal/re-saving, trial acceptance, contact disclosure, email password reset and network-error recovery. |
| Local SMTP | Real SMTP messages were captured and their verification/reset links consumed through Mailpit in the isolated browser stack. |
| AI | **Deferred by user request.** Model downloads and the task-owned native server were stopped. No live AI completion is claimed. |
| Migrations | V1/V2 source files unchanged; fresh schema, retained JPA validation and fixture idempotence exercised by backend/browser stacks. |

Commands and scope are documented in [verification.md](verification.md). Browser emulation does not establish compatibility with every physical device or browser.

## Configuration and limits

- Local SMTP uses Mailpit at http://localhost:8025. Delivering to actual recipient inboxes requires external SMTP host/credentials/sender/TLS in `.env`; no external SMTP account has been provisioned.
- AI is disabled with empty provider settings. Local runtime/download cache is ignored by Git and is not required to run Sprints 1–4.
- Authentication checks verified email only. The legacy `phone_verified` column is retained without pretending a phone has been verified.
- Availability is coarse weekly slots; provinces use consistent case-insensitive names. Proposed trial times do not reserve calendar slots. There is no realtime chat, push notification, payment, video, calendar conflict engine or lesson review workflow in this scope.
- Identity verification remains manual; malware scanning, OCR and fraud detection are not included. Public hosting requires HTTPS/secure cookies, private database access and appropriate backup/evidence retention configuration. No public deployment was performed.
- Legacy V1 trial/review records are not converted automatically. Expired authentication rows still need operational cleanup; SMTP delivery is synchronous rather than an outbox workflow.
- Existing application data and manually provisioned local accounts were retained. Browser fixtures live in their separate temporary Compose project; no fictional approved tutors were seeded into the application database.

## Sprint 4 — two-way matching

Implemented against the updated product tracking sheet under `Specs/SPRINT`: 79 backend tests and 6 desktop/mobile browser tests passed on the final merged source. Migration V6 preserves old one-sided decisions without fabricating matches. New matching endpoints use rules only. Post-Match detail/connection confirmation and the revised trial workflow remain Sprint 5; old detail/lesson APIs have not yet been migrated to those rules. See [scope and evidence](sprint4-matching.md).

## Profile/account lifecycle

Added per-student hide/restore/delete, independent tutor pause/restore, teaching-profile deletion
and self-service account deletion for parent/tutor roles. Admin visibility/approval cannot be
overridden by a tutor. Temporarily hiding keeps existing compatible Matches, while deletion
closes related pairs, removes identifying information/files, permanently revokes historical
lesson contact sharing and revokes all account sessions/challenges. Recreating a teaching profile
keeps its original ID/quota and requires a fresh review. Migration V7 preserves existing profiles
and decisions by default. Final verification: 87 backend tests, 6 expanded desktop/mobile browser
cases, lint/build and main-app visual smoke passed. See [details](profile-management.md).
