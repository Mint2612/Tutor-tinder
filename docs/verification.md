# Verification

## Backend regression

The backend suite uses isolated MariaDB:

```sh
docker compose --profile test up -d test-db
docker compose --profile test build backend-test
docker compose --profile test run --rm backend-test
```

Coverage includes email-only activation, expiring sessions, real CSRF cookie/header exchange, roles and ownership, consent, private evidence/current-submission review, public DTO privacy, all mandatory matching constraints, AI output validation/deadline fallback, saved-interest ownership, concurrent pending-request uniqueness, request expiration, contact gating and reset attempt limits and concurrent email verification without connection-pool exhaustion.

The workflow tests replace SMTP and AI integrations with controlled boundaries. Real SMTP and UI interactions are exercised separately below. The default application never enables fake activation or fabricated recommended tutors.

## Frontend checks

```sh
cd frontend
npm ci
npm run lint
npm run build
```

## Browser acceptance with local SMTP

Use the isolated test project, not the application database. The override removes exposed database ports/persistent DB storage, uses tmpfs, and provisions a test-only admin in that isolated schema.

```sh
# From the repository root:
docker compose -p tutormatch-e2e --env-file .env.example \
  -f compose.yaml -f compose.e2e.yaml --profile app up --build -d

# Wait for http://127.0.0.1:33000/api/health, then:
cd frontend
npm ci
npx playwright install chromium
npm run test:e2e
```

Test endpoints: app http://127.0.0.1:33000, backend http://127.0.0.1:38080, Mailpit http://127.0.0.1:38025. Tests create unique fictional parent/tutor accounts and consume their actual SMTP emails from the local inbox. The test-only admin is defined in `compose.e2e.yaml`; do not use that override for application hosting.

The browser workflow verifies registration/email-link activation, tutor form/evidence upload, admin evidence access before approval, parent survey/consent, discovery, saved interests across refresh, trial requests and acceptance, contact gating, switching between students with different subjects, password-reset email links and sign-in with the changed password. A separate scenario checks anonymous role-route protection and recovery from a failed network request. It runs on Chromium desktop and a 360px mobile/touch configuration. These are emulated device checks, not proof of every physical device or browser.

The suite retains screenshots only on failure. Network traces are disabled because verification/reset emails contain single-use secrets. Generated artifacts are ignored by Git.

Stop the isolated stack when finished:

```sh
docker compose -p tutormatch-e2e --env-file .env.example \
  -f compose.yaml -f compose.e2e.yaml --profile app stop
```

## Deferred work

AI installation, downloads and live inference are deferred. Current recommendations use deterministic rules. Existing trial/AI regression cases remain in the suite to protect code already present, while current delivery focuses on Sprints 1–4.

External SMTP delivery to real recipient inboxes needs configured SMTP credentials. Mailpit acceptance verifies SMTP transport and local email flows.
