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

The current browser workflow verifies registration/email-link activation, tutor form/evidence upload and renewed approval, parent survey/consent, two-way anonymous discovery, swipe/buttons, incoming tags, mutual Match and notifications, saved choices across refresh, per-student isolation, exhausted quota with Skip still enabled, password-reset email links and sign-in with the changed password. Existing trial/contact logic remains covered by backend regression tests; it is outside the new Sprint 4 UI workflow. A separate scenario checks anonymous role-route protection and recovery from a failed network request. It runs on Chromium desktop and a 360px mobile/touch configuration. These are emulated device checks, not proof of every physical device or browser.

The suite retains screenshots only on failure. Network traces are disabled because verification/reset emails contain single-use secrets. Generated artifacts are ignored by Git.

Stop the isolated stack when finished:

```sh
docker compose -p tutormatch-e2e --env-file .env.example \
  -f compose.yaml -f compose.e2e.yaml --profile app stop
```

## Deferred work

AI installation, downloads and live inference are deferred. Current recommendations use deterministic rules. Existing trial/AI regression cases remain in the suite to protect code already present, while current delivery focuses on Sprints 1–4.

External SMTP delivery to real recipient inboxes needs configured SMTP credentials. Mailpit acceptance verifies SMTP transport and local email flows.

## Sprint 1: session review and DBeaver tracking

Focused review on 2026-11-07: **7 authentication/session tests passed** against isolated MariaDB, including hashed session storage, cookie flags, CSRF, expired-session logout, email activation, role restrictions, expiry and reset revocation. The tracking SQL was also executed read-only against the local application database.

The application uses an opaque server-side session. The browser stores `TM_SESSION` as an HttpOnly, SameSite=Lax cookie; JavaScript does not store the authentication token in localStorage/sessionStorage. A new 32-byte cryptographically random token is generated at login. MariaDB stores its SHA-256 hash in `auth_sessions`; the raw token is not returned in the account JSON. Passwords use BCrypt with cost 12. Email/reset secrets are also hashed, expire after ten minutes and are single-use.

Each request hashes the cookie and checks the database session, expiration and activated account before restoring its role. `SessionCreationPolicy.STATELESS` disables Spring's separate HttpSession storage; authentication is restored by the custom filter from `auth_sessions`. Logout removes the current database session and expires its cookie, including when the old session is already expired. Logout remains CSRF-protected. Reset password revokes every session belonging to the account.

Current session lifetime is an **absolute eight hours** from login. Idle/inactivity expiration, device metadata and automatic cleanup of expired database rows are not implemented. An expired row is rejected even if still present in the table. `Secure` is enabled by default and configured false for local HTTP only. These are specific implementation checks, not a claim of a complete production security audit. Reference: [OWASP session management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).

DBeaver local connection:

| Setting | Value |
|---|---|
| Database type | MariaDB |
| Host / port | 127.0.0.1 / 3306 |
| Database | tutormatch |
| Username | tutormatch |
| Password | DATABASE_PASSWORD from the ignored root .env file |

Choose Database → New Database Connection → MariaDB, enter the values and use Test Connection → Finish. Download the driver if DBeaver requests it. Open [sql/sprint1-tracking.sql](sql/sprint1-tracking.sql) in an SQL editor attached to this connection, run its initial SET for your test email, then refresh/re-execute the SELECT statements after each app action. Keep Auto-commit enabled for this tracking connection to read current committed changes.

| App action | Expected database change |
|---|---|
| Register | users row with email_verified=0; EMAIL challenge with secret_hash |
| Verify email | email_verified=1; consumed EMAIL challenge removed |
| Log in | New auth_sessions row; only token_hash and expiry are stored |
| Refresh page | Same cookie/session remains valid until expiry |
| Log out | Current session removed and browser cookie expired |
| Forgot password | New RESET challenge with hashed secret |
| Reset password | Password hash replaced; all account sessions/challenges removed |

The SQL script reads application tables and masks hash values. Its SET only changes a connection-local variable. Verification/reset links are delivered through the configured SMTP provider (Mailpit locally, or Gmail with the setup in [environment.md](environment.md#gmail-smtp)), not stored as raw secrets in MariaDB. DBeaver's own database Session Manager concerns database connections; application login sessions are tracked through auth_sessions.

Gmail SMTP connection check on 2026-11-07: STARTTLS and authentication succeeded with local credentials, and the backend health check passed after configuration reload. No email was sent by this connection check; actual recipient inbox delivery still requires the registration/reset flow. Credentials remain in the ignored `.env` file.

## Sprint 3 — reapproval acceptance (2026-11-07)

- Backend: `./mvnw -B -o test` against isolated MariaDB: **56 tests passed**, zero failures/errors/skips. Includes fresh evidence reads per resubmission, submission/revision audit fields, rejection reasons, file type/size/count limits, ownership, and profile/photo changes withdrawing approval and public access. Storage in these backend tests is mocked.
- Browser: **6 Playwright tests passed**, Chromium desktop and 360px mobile emulation, against separate MariaDB, Mailpit and **real MinIO**. The main workflow uploads and downloads identity evidence, approves, edits the tutor profile, resubmits, confirms the approval button remains disabled until another download, then approves again and continues discovery.
- Frontend Oxlint and Docker production builds passed. These checks do not claim testing on a physical phone or all browsers.

Manual Sprint 3 review:

1. Tutor: complete profile, upload PDF/PNG/JPEG (at most 5 MB each, five files), submit; reload and confirm pending status. Invalid/empty/oversized files must fail.
2. Admin: open the submission; approval must fail until current evidence has been downloaded. Reject with a reason, verify the tutor sees it, then resubmit and approve after a fresh read. History displays decision, reason, submission ID and revision.
3. Parent: use matching student conditions; approved/visible/activated tutor appears with a verification badge. Admin hiding the tutor removes public access and suggestions.
4. Tutor: save an approved profile or replace/remove its photo; status becomes DRAFT and public profile/suggestions disappear. Resubmit; the earlier evidence read cannot authorize the new submission.
5. Parent/another tutor: accessing `/api/admin/documents/{id}` must return 403; anonymous access must return 401. Evidence remains in a private storage bucket.

Saving a profile, including saving unchanged form values, conservatively requires reapproval for an approved/pending profile. Rejected profiles retain the rejection reason until resubmission. Old audit rows have unknown submission/revision and are not backfilled. Team review checkboxes remain pending in the sprint sheet.

## Latest Sprint 4 verification — 2026-11-07

66 backend tests passed (including 10 two-way matching integration tests). Six Playwright cases passed across desktop Chromium and Pixel 7 emulation at 360px, with actual local SMTP and object storage. Frontend lint/build passed. See [Sprint 4 scope, migration, API and verification details](sprint4-matching.md). Repeat browser workflows against a clean tmpfs test DB to avoid cumulative registration throttles from prior runs. Formal Product Owner review remains separate from these technical results.

### After pulling upstream `3a7e3df` — 2026-11-07

The local Sprint 4 changes were reapplied and the browser-test conflict resolved while preserving the new legal-name privacy assertion. Matching moves to migration V6 to keep upstream V5 intact. Fresh isolated MariaDB verification passed **79 backend tests**, including the matching tests and the newly added contact/province/name tests. Frontend lint and production build also passed.

Final merged delivery: **all six browser tests reran and passed** on desktop/mobile against this merged source, including the legal-name privacy assertion and current province/time-range controls. The local application database was backed up, its earlier matching migration aligned to V6, and upstream V5 applied successfully. The final app was then restarted using ordinary Compose with no temporary migration override. Parent/tutor/admin logins, the 23-profile sample parent, approved sample tutor, anonymous matching feeds and the new private legal-name field were checked on `localhost:3000`. Protected table row counts and fingerprints matched the pre-upgrade backup; no accounts, student profiles, choices, quota counters, notifications or Match records were lost. See [migration upgrade details](migrations.md#local-upgrade-from-the-pre-pull-sprint-4-build).
