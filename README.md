# TutorMatch

Vietnamese tutor discovery: React + Vite, Java 21 + Spring Boot, MariaDB, Spring Security and Flyway. Parents create student profiles, discover verified tutors, save interests and request trial lessons. Tutors manage profiles and requests; admins review private identity evidence.

Accounts activate through **email only**. Phone numbers are used for contact after trial acceptance. Email uses SMTP; local development includes a Mailpit inbox. The current delivery covers Sprints 1–4 with deterministic filtering/ranking. AI is deferred.

## Run locally

Requirements: Docker with Compose. Native development additionally uses Java 21 and Node 24.

```sh
cp .env.example .env
# Set database passwords. Optionally configure private ADMIN_EMAIL / ADMIN_PASSWORD.
docker compose --profile app up --build -d
```

- App: http://localhost:3000
- Local development inbox: http://localhost:8025
- API health: http://localhost:8080/api/health

Register a parent/tutor, open the verification email in Mailpit, follow its link and confirm. Mailpit captures local emails; to send to actual recipients, configure your SMTP host, credentials, sender and TLS in `.env`. No activation secrets are returned by the application API.

Admin accounts are created only through `ADMIN_EMAIL` / `ADMIN_PASSWORD` on first startup (16–72 characters, at most 72 UTF-8 bytes). Remove bootstrap credentials after provisioning. Existing accounts are never upgraded to admin or given a new password by startup. Local HTTP uses `COOKIE_SECURE=false`; public deployment requires HTTPS and secure cookies.

## Product flow

Current scope: steps 1–6 (Sprints 1–4). The existing trial-request workflow is retained for a later sprint.

1. Register, verify email and log in as a parent or tutor.
2. Parent records consent, creates one or more student profiles and completes the six-question survey.
3. Tutor creates a teaching profile, uploads private identity evidence and submits it for review.
4. Admin opens current-submission evidence, approves or rejects with a reason and controls visibility. Tutor edits require renewed approval.
5. Parent discovers tutors satisfying subject, grade, format, province, schedule and budget constraints. Recommendations use rules; AI is deferred.
6. Swipe/use buttons to like or skip. Reopen liked tutors in **Đã quan tâm**, including after refresh.
7. Send a proposed trial time and message. The owning tutor accepts/rejects; contact details appear only after acceptance. Duplicate pending requests are blocked, and expired requests cannot be accepted.

The scope excludes payment, realtime chat, video, calendar reservations and reviews after lessons.

## Verification

```sh
docker compose --profile test up -d test-db
docker compose --profile test build backend-test
docker compose --profile test run --rm backend-test

cd frontend
npm ci
npm run lint
npm run build
```

For real browser workflows against isolated MariaDB and SMTP, follow [verification.md](docs/verification.md). Tests never need application database credentials or production users.

## Source structure

Backend layers: `controller`, `service`, `repository`, `mapper`, `dto`, `model`, `config`, `security`, `exception`, and replaceable integrations. Controllers/services contain no SQL. API responses use explicit DTOs.

Frontend: `app`, `hooks`, `pages`, `features`, `components`, `services` and shared presentation constants. Only relative `/api` calls enter the frontend bundle.

Details: [architecture](docs/architecture.md), [API](docs/api.md), [environment](docs/environment.md), [migrations](docs/migrations.md), [implementation status](docs/implementation-status.md), [sprint tracking](docs/sprint-tracking.md). Current instructions and docs supersede conflicting older proposals under `Specs/`.
