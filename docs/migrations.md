# Database migrations

Flyway owns the MariaDB schema. Hibernate validates only. Never modify V1 on deployed databases. V2 is additive: new auth tables, additional tutor profile workflow columns, private evidence and audit tables, student/tutor swipes, and `lesson_requests`.

V1 `trial_requests` and `reviews` remain legacy tables; new APIs use `lesson_requests`, whose statuses match the current specification (PENDING, ACCEPTED, REJECTED, EXPIRED). No existing trial data is automatically migrated. Before migrating a deployment with legacy trial data, reconcile those records explicitly. V1 tutor verification_status remains synchronized with workflow_state by service operations; DRAFT/PENDING_REVIEW use legacy PENDING.

Existing users receive no default password or activation flags. Existing tutor records become DRAFT and need verified accounts and evidence review. No demo account is activated. Optional `demo` profile seeds fictional unapproved tutors only; Docker app no longer enables it by default.

The generated nullable `pending_pair` column and unique index enforce at most one pending request per student/tutor even under concurrent submissions. Requests expire lazily on list/create/respond. Expiry is not a background notification service. Foreign keys retain relationship integrity; metadata/document contents never appear in public profile queries.

Before deployment, back up MariaDB and test migrations on a restored copy. Keep database and backup access private: identity documents are BLOBs in `tutor_documents`, served only through the admin endpoint. Configure encryption at rest, TLS, encrypted backups and a retention policy at deployment. No public object URLs or static file paths expose documents.

Verification after setup: `docker compose --profile test run --rm backend-test` exercises V1/V2 against the isolated MariaDB test database. Tests create explicitly marked fixtures, never production users. Test database is tmpfs. Stop it without deleting application volumes with `docker compose stop test-db`.

Email-only activation reuses `email_verified`; `phone_verified` remains for schema compatibility and is no longer required by authentication/discovery. No migration rewrites existing verification flags, and V1/V2 checksums remain unchanged.

## V4 — Verification audit submission

Adds nullable `submission_id` and `revision` to `verification_audit`. New decisions record the reviewed submission and profile revision. Existing decisions keep null values because their historical submission cannot be reconstructed reliably. No account, evidence or decision records are removed.

## V6 — Two-way matching

Adds `matching_pairs`, daily profile quota counters and account-owned Match notifications. Existing `swipe_interactions` become parent decisions only; no historical one-sided like becomes a Match. Interest timestamps and seven-day deadlines are preserved; expired historical interests close. Historical OKs count toward their Vietnam calendar date. Existing tables/rows remain intact. V1–V5 from upstream are unchanged. Quota counters and pair decisions commit atomically; the parent compatibility endpoint still writes `swipe_interactions`.

When rerunning isolated tests after switching between incompatible historical branches, recreate only the tmpfs test database. Never repair/delete the application schema to make a test run pass.

### Local upgrade from the pre-pull Sprint 4 build

Upstream commit `3a7e3df` introduces `V5__tutor_legal_name.sql`; the local matching migration was therefore moved to V6. A local application database that already records `V5__two_way_matching.sql` needs a one-time, backed-up migration-history alignment to V6 followed by out-of-order application of upstream V5. Fresh databases use upstream V5 then local V6 normally. Do not delete application volumes or blanket-repair migration checksums to resolve this version collision.

**Completed locally on 2026-11-07:** stopped the app/backend, saved a full SQL backup plus migration history and protected-table fingerprints under the ignored `.local/backups/` directory, changed only the matching history version/script from V5 to V6, and applied upstream V5 with a temporary out-of-order override. After successful migration, removed the override and recreated backend/frontend using default Compose. Default startup passed Flyway validation. Accounts, student records, evidence metadata, choices, quotas, Match records and notifications were preserved. The current local app now uses the final merged schema; regular `docker compose --profile app up --build -d` works without additional upgrade steps.

## V7 — Profile/account management

Adds nullable deletion timestamps to users/student/tutor profiles, student `visible`, tutor
`accepting_students`, and permanent `contact_revoked` on historical lessons. Existing profiles default
to visible/accepting and no accounts are deleted by migration. Self-pausing does not write admin
visibility/approval fields. Actual deletion is an explicit authenticated action, with current-password
confirmation for account/tutor-profile deletion, identifying data removal and session revocation.
Historical foreign-key targets remain as anonymous tombstones. See [management behavior](profile-management.md).

## V8-V11 ? Connection, scheduling and shared AI

V8 adds one-time participant confirmations and a three-day deadline for post-Match decisions.
V9 adds event-keyed notifications and `trial_schedules`; a generated unique active-pair key
protects pending/confirmed schedule uniqueness. V10 stores a validated shared pair score and
an input fingerprint. V11 records the compatible learning format for each trial, backfilling it
from the student profile. Previously applied V1?V8 scripts are unchanged.

Before the local upgrade, a full SQL backup was saved to the ignored
`.local/backups/sprint56-20261114-091202/`. Original student records, matching choices/counters
and lesson history were retained. Profile/admin/evidence activity added during the work was
preserved rather than restored over. Detailed local inventory is in the ignored
`.local/sprint56-data-verification.json`.

## V13 — featured tutor

`V13__featured_tutor.sql` adds `tutor_profiles.featured BOOLEAN NOT NULL DEFAULT FALSE`. A featured tutor is
listed before every other candidate in a parent's discovery feed and in the AI suggestion list (before the
"interested in you" tag and the score). Featuring only reorders: the mandatory eligibility rules (subjects, grade,
format, schedule, budget, same province for in-person lessons) still apply, and cards the parent has already decided
on never return. No application endpoint sets the flag; demo environments set it with
`scripts/seed/featured-tutor004-seed.sql`.
