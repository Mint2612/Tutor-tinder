# Architecture

The current implementation follows the November 2026 specification: React JavaScript → Spring Boot REST API → Spring Security/business services → MariaDB; the optional external LLM sits behind the backend `LlmRanker` interface. Older embedding-first proposals in Specs are superseded. No embedding is required for eligibility.

Frontend uses relative `/api` paths (Vite proxy locally, nginx in Docker), reusable forms/cards/dialogs, Vietnamese labels, pointer gestures with equivalent buttons, native accessible dialogs, responsive CSS and reduced-motion support. No secrets, sample tutor cards or client-only business decisions stand in for backend state. React maintains the active view in memory; URLs/deep links are not implemented.

Backend packages:

- `auth`: BCrypt, opaque hashed eight-hour sessions, CSRF double-submit cookie/header, independent email/phone challenges, reset/session revocation, database throttling, delivery gateway and deployment-only admin provisioning.
- `platform`: validated DTOs, profile ownership, private evidence and current-submission audit, trial state transitions, deterministic filtering/scoring, provider output validation.
- `health`, `user`, `matching`: existing health, JPA user scaffold and cosine utility retained. Business queries use parameterized Spring JDBC on the same transaction manager/database.

Services own profile persistence; controllers expose JSON DTO inputs and deliberately selected public outputs. Tutor edits reset approval. Evidence endpoints require ADMIN, record which submission was reviewed and return attachment/no-store headers; documents never have public URLs. Actual identity verification remains a human responsibility, not a claim inferred from a file upload.

Only activated, approved and visible tutors can be publicly listed or recommended. Filters require all requested subjects, grade, format, shared weekly slot, budget, and same province for in-person learning. Unseen eligible profiles are ordered deterministically before the top 20 enter the ranking stage. The fallback uses shared schedule, experience and budget headroom. LLM sees candidate aliases and allowlisted learning/teaching categories, not raw student narratives or identity. Output is bounded and validated against candidate IDs, qualitative labels and explanation sizes. Up to eight concurrent AI calls; an eight-second provider timeout and nine-second orchestration limit preserve fallback availability. An unhealthy provider cannot override eligibility.

Trial interest, request creation and acceptance are separate records/actions. MariaDB generated unique pending pairs protect concurrent duplicate submissions. Only the owning tutor can transition an unexpired PENDING request. Contact fields are selected with CASE only for ACCEPTED lessons; they are absent from discovery/public tutor data. Expiration is checked lazily on request operations. Parent/tutor status pages refresh through REST; no realtime messaging/notification delivery is implied.

Flyway owns migrations; Hibernate validates the retained User entity. V1 remains unchanged; V2 extends profiles and introduces auth, evidence, swipes and lesson requests. See migrations.md for legacy-table handling and deployment/backups. Admin audit and evidence remain private DB data.
