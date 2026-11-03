# TutorMatch

**Find the Right Tutor, Not Just Any Tutor.**

Vietnamese tutor discovery MVP: React 19 + Vite, Java 21 + Spring Boot 4.1.2, MariaDB 11.4, Spring Security and Flyway. Parents manage students, answer a short learning survey, browse eligible verified tutors with swipe gestures, and request a trial lesson. Tutors manage their profiles and requests. Administrators review private identity evidence before approving visibility.

## Run locally

Requirements: Docker with Compose, or Java 21, Node 24 and MariaDB 11.4 for native development.

```sh
cp .env.example .env
# Set database passwords. Configure DELIVERY_URL / DELIVERY_KEY for real email and SMS.
# Optionally set ADMIN_EMAIL and ADMIN_PASSWORD (16–72 characters) for first startup.
docker compose --profile app up --build -d
```

Open `http://localhost:3000`; API health: `http://localhost:8080/api/health`. Docker binds ports to loopback. `COOKIE_SECURE=false` supports local HTTP only; use `true` with HTTPS in deployment. Remove bootstrap admin credentials after provisioning; startup never changes an existing user's role or password. No administrator role is available in public registration.

**Registration requires a real delivery adapter.** It fails closed with a clear 503 if delivery is missing. Connect a trusted email/SMS webhook implementing the [delivery contract](docs/api.md); both independent messages must be verified before login. The UI accepts the challenge identifier and secret from each message. There are no default user passwords or frontend verification bypasses. An optional LLM endpoint enriches rankings; without it recommendations still work through deterministic scoring.

For native development, start `docker compose up -d db`, run `./mvnw spring-boot:run` in `backend/`, and `npm ci && npm run dev` in `frontend/`. Vite proxies relative `/api` requests. See [environment setup](docs/environment.md).

## Product flow

1. Parent registers, verifies email and phone, logs in, and creates a student with recorded consent.
2. Complete the six-question survey, select subjects, grade, location, schedule, format and budget.
3. Tutor registers and verifies both channels, saves a teaching profile, uploads identity evidence and submits for review.
4. Administrator opens the evidence for the current submission and approves or rejects with a reason. Profile changes require renewed approval.
5. Parent requests recommendations. Mandatory constraints are applied first. At most 20 anonymized candidates enter LLM ranking; up to five profiles appear. LLM errors, invalid output, overload and timeout use deterministic fallback.
6. Swipe right or use the interest button; swipe left or skip. Interest is persisted but is **not** acceptance.
7. Parent sends a proposed trial time and message. Tutor accepts or rejects. Contact details are disclosed only on accepted requests. Duplicate pending requests are blocked by the database. Statuses update when the request list is refreshed.

No payments, chat, video, digital contracts, student logins or custom model training.

## Verification

```sh
# Isolated MariaDB test database; does not use application data.
docker compose --profile test up -d test-db
docker compose --profile test build backend-test
docker compose --profile test run --rm backend-test

# Native frontend checks, also used in CI
cd frontend
npm ci
npm run lint
npm run build
```

Tests include registration and channel activation, session expiry/reset revocation, CSRF, roles and ownership, consent, private evidence, review before approval, strict matching filters, fallback, request uniqueness, state transitions and contact gating. Fixtures are test-only; no invented records appear in the production UI. Optional `demo` Spring profile seeds fictional **unapproved**, non-login tutor records and is not enabled in the default app Compose service.

## Repository

- `frontend/src/components/`: Vietnamese auth, profile forms, discovery, dialogs and shared UI.
- `frontend/src/lib/`: API/CSRF client and common presentation constants.
- `backend/src/main/java/com/tutormatch/auth/`: sessions, password hashing, verification, delivery and admin provisioning.
- `backend/src/main/java/com/tutormatch/platform/`: profile services, verification, recommendations and trial workflows.
- `backend/src/main/resources/db/migration/`: immutable V1 foundation and additive V2 MVP schema.
- `docs/`: [architecture](docs/architecture.md), [plan](docs/implementation-plan.md), [REST API](docs/api.md), [migration guide](docs/migrations.md), [implementation status and limits](docs/implementation-status.md).

The user-provided November 2026 specification takes precedence over older documents under `Specs/`. Production deployment and external delivery/LLM credentials are separate configuration work; build/test results do not verify actual SMS/email delivery, external model quality or browser gesture behavior.
