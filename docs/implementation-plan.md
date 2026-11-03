# TutorMatch MVP implementation plan

The November 2026 user specification is authoritative over older Specs. Preserve React, Spring Boot, MariaDB, V1 and the existing health API. Work locally in this repository; hosting is outside this implementation request.

1. Security foundation: Spring Security, opaque expiring server sessions, CSRF, BCrypt, parent/tutor registration, independent email/SMS challenges, reset, private admin bootstrap, structured errors. Delivery uses a backend-only configurable HTTP adapter; never expose verification codes in API responses.
2. Data: additive Flyway V2; extend student/tutor profiles; private evidence storage, verification audit, sessions/challenges, swipes, lesson requests with a database-enforced unique pending pair. Existing V1 trials remain legacy.
3. Business APIs: ownership and role checks on every operation; consent and at most 15 survey answers; tutor changes require renewed approval; administrators must open evidence before approving. Contact data only on accepted lessons.
4. Matching: strict subject/grade/format/location/budget/schedule filtering, at most 20 anonymized candidates; replaceable LLM interface, validated output and deterministic fallback, total AI timeout below 10 seconds, at most five results.
5. React: Vietnamese landing/auth, verification/reset, parent students and survey, discovery with pointer gestures and equivalent buttons, tutor detail and interest confirmation, trial tracking, tutor editing/evidence, admin review. No invented tutors in the product UI.
6. Verification: compile/build; MariaDB-backed integration tests for authentication, CSRF, ownership, verification and trial invariants; matching unit tests and frontend build. Report unexecuted checks explicitly.

REST groups: `/api/auth/*`, `/api/students`, `/api/tutor/profile`, `/api/tutor/documents`, `/api/admin/*`, `/api/students/{id}/recommendations`, `/api/tutors/{id}`, `/api/swipes`, `/api/lessons`.

External dependencies: MariaDB; delivery webhook for real email/SMS; optional LLM endpoint/key/model. No keys or student identity in frontend or AI requests. Delivery adapter contracts and remaining deployment requirements are documented in README and API docs.
