# Sprint 5 verification — 2026-11-14

Source: branch `test`, base commit `2fec3af`. Requirements: S5-01 through S5-04 in
`Specs/SPRINT/sprint-tracking.md`. **Sprint 5 is not ready for acceptance.**

## Executed checks

- Existing backend regression: **87 passed**, no failures or errors.
- Added Sprint 5 acceptance suite: **10 executed, 4 passed, 6 failed**, no errors.
- Playwright: **6 passed** across desktop Chromium and a 360px mobile configuration.
  The main workflow now also opens both post-Match detail dialogs, verifies hidden
  contacts, confirms each side, and checks the opposite participant's email appears
  only after mutual confirmation.
- Frontend lint/build and `git diff --check`: passed.

Backend tests use `test-db`; browser tests use the separate `tutormatch-e2e`
project with temporary MariaDB/object storage and Mailpit. Application users,
profiles, decisions and quotas were not used by these tests. The app remains at
http://localhost:3000. Browser results use emulation, not physical device testing.

## Requirement findings

| Story | Result | Evidence |
| --- | --- | --- |
| S5-01: detail only after Match | Incomplete | The new pair-detail endpoint rejects unmatched/unrelated participants and omits private evidence/contact data. However, `GET /api/tutors/{id}` still returns full public tutor detail to an unmatched parent (HTTP 200). |
| S5-02: connection confirmation | Partial | Mutual confirmation, cross-contact gating, ownership, CSRF, decision immutability, idempotent retries, refusal closure, and retaining an established connection past the deadline passed. Closure notifications and durable expiry on rejected detail/confirmation requests failed. |
| S5-03: trial scheduling | Incomplete | A matched but unconfirmed pair can still create a trial via `POST /api/lessons` (HTTP 200). Inspection shows the legacy single `proposedAt` request and tutor-only response: proposing 1–3 slots, proposals from either side, counter-proposals and final scheduling notifications are not implemented. |
| S5-04: active schedule lifecycle | Incomplete | Existing regression covers pending-request uniqueness, expiry and duplicate-response rejection in the legacy flow. The required cancellation with a reason and the revised scheduling lifecycle are not implemented; these cannot be accepted based on the old regression. |

## Reproduced failures

`Sprint5AcceptanceIntegrationTest` retains six failing acceptance checks:

1. `legacyTutorDetailCannotBypassMutualMatchRequirement`: expected client error,
   received HTTP 200 before any Match.
2. `declineNotifiesOtherSideWithNeutralClosure`: after clearing the original Match
   notices and declining, the other participant has no new notification.
3. `expiredDetailPersistsClosedStatusAndNotifiesOtherSide`: moving the deadline
   into the past and requesting detail returns 404, but DB status stays `MATCHED`.
4. `expiredConfirmationRejectsAndPersistsExpiryWithoutContact`: expired confirmation
   returns 409, but DB status also stays `MATCHED`.
5. `expiryViaFeedClosesPairAndNotifiesOtherSide`: feed access does persist `EXPIRED`,
   but no closure notification is created for the other participant.
6. `unconnectedPairCannotCreateTrialRequest`: trial creation succeeds before both
   connection confirmations.

The expiry persistence failures occur because `ConnectionService` updates expiry
inside a transaction and then throws `BusinessException`, rolling back that update.
The notification repository only creates Match notifications; there is no closure
notification path. Production code was not changed during this verification.

## Reproduce

From the repository root, against an isolated database:

```sh
docker compose --profile test build backend-test
docker compose --profile test up -d --wait test-db
docker compose --profile test run --rm backend-test ./mvnw -B -ntp -Dtest=Sprint5AcceptanceIntegrationTest test
```

The acceptance command intentionally fails until the listed requirements are fixed.
To run all backend tests, omit `-Dtest=Sprint5AcceptanceIntegrationTest`.
For browser stack setup, follow `docs/verification.md`, then run `npm run test:e2e`
in `frontend`. Test-only ports are 33000 (web), 38080 (API), and 38025 (Mailpit).

Detailed logs are local and ignored by Git:
`.local/sprint5-backend-tests.log`, `.local/sprint5-acceptance-tests.log`, and
`.local/sprint5-browser-tests.log`. AI embedding/provider behavior was not part of
this S5-01–04 verification and no live AI acceptance is claimed.
