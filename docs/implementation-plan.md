# TutorMatch implementation plan

Current user scope: organize source into explicit layers, complete **Sprints 1–4** and use **email-only** activation. AI and new work on Sprints 5–6 are deferred. Preserve React, Spring Boot, MariaDB and existing V1/V2 migration checksums.

1. Separate HTTP controllers, business services, repositories, mapping, request/response DTOs, domain models, security configuration and integrations. Remove service JDBC accessors and SQL in controllers/services.
2. Preserve ownership, role/CSRF protection, consent, private evidence, current-submission audit, strict matching filters, request uniqueness and contact gating through regression tests.
3. Deliver verification/reset emails over SMTP with a Mailpit inbox in local Compose. Remove phone activation requirements while retaining phone contact fields and the legacy database column.
4. Deliver discovery through strict eligibility, deterministic rule ranking, tutor details and persisted interest/skip decisions. Retain the existing AI interface for later; keep its configuration empty.
5. Split the React shell into screens/features/hooks/services; support hash/email routes, session expiry, loading/errors/retry, action locking and saved interests after refresh.
6. Verify backend with isolated MariaDB; verify the browser journey against an isolated app/database/Mailpit stack on desktop/mobile emulation; document actual results and external SMTP/device limitations.

API groups remain `/api/auth/*`, `/api/students`, `/api/tutor/*`, `/api/admin/*`, `/api/swipes`, `/api/lessons`. Saved-interest APIs are added under `/api/students/{id}/interests`. No hosting, payment, realtime chat or calendar-reservation service is added.
