# TutorMatch

Vietnamese tutor discovery: React + Vite, Java 21 + Spring Boot, MariaDB, Spring Security and Flyway. Parents and approved tutors discover compatible anonymous preview cards, choose OK/Skip and create a mutual Match. Parents manage student profiles; tutors manage teaching profiles; admins review private identity evidence.

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
5. Parents open **Khám phá gia sư** for each student; approved visible tutors open **Tìm học sinh**. Both feeds apply subject, grade, format, schedule and budget constraints, plus the same province for in-person lessons. Ranking uses rules.
6. Swipe or use OK/Skip buttons. One OK prioritizes the card for the other side; two OKs create a Match and persistent notifications. **Đã OK & Match** survives refresh. Each profile has 10 OK/day, including at most 3 replies to incoming interest; pending interest expires after 7 days. See [Sprint 4](docs/sprint4-matching.md).
7. Detailed post-Match profiles, mutual connection confirmation and the new trial scheduling flow are Sprint 5 backlog. Earlier detail/trial APIs remain for compatibility; the new Sprint 4 interface does not send a trial request on OK.

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
