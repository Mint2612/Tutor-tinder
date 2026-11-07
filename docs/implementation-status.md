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
| Discovery | Strict eligibility, deterministic ranking, qualitative explanations, pointer mouse/touch swipes and equivalent buttons, public profile details and loading/empty/error states. |
| Saved interests | Parents reopen liked tutors after refresh, remove interests and send trial requests later. Ownership and current eligibility/visibility are reapplied. |
| Trials (retained existing workflow; outside current scope) | Proposed time/message, unique pending pair, tutor acceptance/rejection, lazy expiration and contact disclosure only after acceptance. Status pages refresh through REST. |
| Frontend | App/provider, reusable hooks, hash/email routing, pages, feature components and shared UI/API modules. Action locking, abortable page loads, session-expiry handling, form validation and accessible native dialogs. |
| Local integrations | Mailpit SMTP inbox in Compose. Existing AI adapter retained but disabled; experimental AI additions and setup helper removed. |
| Verification tooling | MariaDB regression suite, isolated browser-test Compose override, Playwright desktop/mobile workflows and browser checks in CI. The test image seeds Maven dependencies and preserves runtime cache in a dedicated volume. |

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
