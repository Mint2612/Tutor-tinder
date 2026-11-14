# TutorMatch

Vietnamese tutor discovery: React + Vite, Java 21 + Spring Boot, MariaDB, Spring Security and Flyway. Parents and approved tutors discover compatible anonymous preview cards, choose OK/Skip and create a mutual Match. Parents manage student profiles; tutors manage teaching profiles; admins review private identity evidence.

Accounts activate through **email only**. Contacts are shared after both participants confirm connection. Email uses SMTP; local development includes a Mailpit inbox. Delivery covers Sprints 1–6, including trial scheduling and optional AI ranking with rule fallback. See [Sprint 5–6](docs/sprint5-6.md).

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

Current scope: the complete discovery → Match → connection → trial flow (Sprints 1–6).

1. Register, verify email and log in as a parent or tutor.
2. Parent records consent, creates one or more student profiles and completes the six-question survey.
3. Tutor creates a teaching profile, uploads private identity evidence and submits it for review.
4. Admin opens current-submission evidence, approves or rejects with a reason and controls visibility. Tutor edits require renewed approval. The admin **Giám sát** page shows aggregate counts (accounts, verification queue, Match funnel, trials, 7-day activity, server health) with no personal data.
5. Parents open **Khám phá gia sư** for each student; approved visible tutors open **Tìm học sinh**. Both feeds apply subject, grade, format, schedule and budget constraints, plus the same province for in-person lessons. Ranking uses rules by default; either side can request up to ten AI-ranked eligible cards in a looping horizontal carousel.
6. Swipe or use OK/Skip buttons. One OK prioritizes the card for the other side; two OKs create a Match and persistent notifications. **Đã OK & Match** survives refresh. Each profile has 10 OK/day, including at most 3 replies to incoming interest; pending interest expires after 7 days. See [Sprint 4](docs/sprint4-matching.md).
7. Open a matched profile, then confirm or decline connection. Contacts remain hidden until both sides confirm. Pending confirmation expires after three days, with neutral closure notifications.
8. Either connected participant proposes 1-3 trial start times and the compatible learning format. The other accepts a time or counter-proposes. Each pair has one active schedule; responses expire after three days or before the earliest offered time. Cancelling before the confirmed start requires a reason. Both sides receive durable notifications.

The scope excludes payment, realtime chat, video, calendar reservations and reviews after lessons.

## Profile management

Parents can edit, hide/show and delete student profiles. Tutors can pause receiving new students
independently of admin visibility, or delete their teaching profile and create a new draft later.
Parents/tutors can delete their account with a current-password confirmation; sessions are revoked
and identifying profile data is removed while historical references stay anonymous. See
[profile/account management](docs/profile-management.md) for behavior and endpoints.

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
