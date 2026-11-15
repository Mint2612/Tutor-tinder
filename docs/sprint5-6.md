# Sprint 5–6 delivery — 2026-11-14

Implemented on branch `test`, based on `2fec3af`. The current application is
http://localhost:3000. Sprint tracking marks technical verification separately
from the team's formal Review sign-off.

## Try the complete flow

1. A parent opens **Khám phá gia sư** for a student; a tutor opens **Tìm học sinh**.
   Both choose OK to create a Match.
2. Each opens **Đã OK & Match → Xem hồ sơ & xác nhận kết nối**. Parents see tutor
   detail; tutors see anonymous student learning detail. Contacts and private
   verification evidence are excluded. Full tutor detail cannot be opened through
   the older detail/recommendation/interest routes before Match.
3. Each chooses **Xác nhận kết nối** or **Từ chối**. Decisions cannot be changed;
   replaying the same decision is idempotent. Both confirmations reveal the other
   party's email/phone, and tutors receive only the parent's contact. Both receive
   a connection notification. Decline closes the pair and sends a neutral notice.
   An unconfirmed connection closes after three days when accessed; expiry is
   committed even when the attempted detail/confirmation request is rejected.
4. After connection, the detail dialog shows **Lịch học thử**. Either side proposes
   one to three start times and a compatible format. Times must be at least an
   hour ahead and within 90 days. The format must match the student's profile.
5. The other participant chooses one offered time and **Chốt giờ học thử**, or
   **Đề xuất giờ khác**. Counter-proposals create a new version and reverse the
   participant whose response is awaited. Both receive persistent notifications.
6. Both can also manage the pair from **Lịch học thử / Yêu cầu học thử**. Pending
   proposals expire after three days or at the earliest proposed start, whichever
   is sooner. Expiry is evaluated on access. One pending/confirmed schedule per
   pair is enforced by both transaction locking and a database unique constraint.
7. **Hủy lịch học thử** requires a nonblank reason of at most 500 characters and
   must occur before the confirmed start. After cancellation/expiry, the connection
   remains and either side can propose a fresh schedule. Closed schedules remain
   visible as history. Expired schedules, stale versions, self-acceptance and a
   second response cannot be accepted.

Dates in requests/storage are UTC instants. The interface renders times in the
device's timezone and converts the datetime input to UTC before sending it.
This MVP records trial start times, not duration, reservations or calendar sync.

## API

Paths below are relative to `/api`; writes require the current CSRF cookie/header.

| Method/path | Payload / behavior |
| --- | --- |
| GET `/matching/pairs/{studentId}/{tutorId}` | Owned, active matched pair detail; `connected`, `myDecision`, `expiresAt`, `learningMode`, role-appropriate profile and gated `contact`. |
| POST `/matching/confirm` | `{studentId,tutorId,confirm:boolean}`; fixed participant decision. |
| GET `/trials` | Current participant's schedules; expires overdue pending proposals. |
| GET `/trials/pairs/{studentId}/{tutorId}` | Connected owned pair's history/current schedule. |
| POST `/trials` | `{studentId,tutorId,slots:[UTC instant,...],mode:"ONLINE" or "IN_PERSON"}`. Format defaults to the student's format if omitted. |
| PUT `/trials/{id}` | `{action:"ACCEPT",version,slot:UTC instant}`. |
| PUT `/trials/{id}` | `{action:"COUNTER",version,slots:[UTC instant,...]}`. |
| PUT `/trials/{id}` | `{action:"CANCEL",version,reason}`. |
| GET `/students/{id}/matching/ai` | Parent-owned student's up to ten eligible cards. |
| GET `/tutor/matching/ai` | Approved visible tutor's up to ten eligible cards. |

Schedules return `id`, pair IDs, `proposerRole`, `slots`, `status`, `version`,
`chosenAt`, `expiresAt`, `cancellationReason` and `mode`. Statuses are `PENDING`,
`CONFIRMED`, `CANCELLED`, `EXPIRED`. Stale/conflicting operations return 409;
invalid input returns 400; unauthorized or unrelated participants are denied.
No original verification documents or legal-name fields enter these responses.

The old `/lessons` compatibility API requires mutual connection for both creation
and response; its accepted-contact response also checks the current connection.
It cannot create a second active schedule alongside the new flow. The new UI uses
`/trials`. Historical legacy records remain in their original table.

Profile/account deletion closes active schedules, removes cancellation narratives
and proposed times from retained trial records, and deletes stored AI pair scores.
It does not refund matching quotas or reset previous pair decisions.

## Sprint 6 and AI bonus

- Desktop and 360px mobile workflows cover signup/email activation, login,
  tutor evidence/reapproval, student consent/survey, swipe/buttons, mutual Match,
  connection, trial negotiation, cancellation, password reset and profile/account
  management. Native dialogs scroll; trial inputs/buttons wrap within the viewport.
- Matching, detail and scheduling provide loading/empty/error states. Detail and
  schedule refresh actions allow recovery. A failed trial request retains its form
  values. Local action locks and disabled controls prevent duplicate submission;
  server locks, versions and uniqueness protect against retries/concurrent clients.
- **Gợi ý hồ sơ bằng AI** is available to either role. Hard matching constraints
  and previous decisions filter candidates first; at most 20 eligible cards are
  scored and at most ten return in a boxed horizontal carousel showing three
  compact cards (name, subjects/grades, rate and experience, area and format) on desktop, two on tablet and one on mobile, plus a partial next card.
  The list uses native horizontal overflow and CSS scroll snapping, with mouse
  grab-to-scroll and native touch/trackpad gestures. Arrow buttons advance one card.
  Next/previous buttons,
  horizontal swipes and arrow keys navigate without making matching decisions.
  Moving past the last card loops back to the first; moving before the first
  loops to the last. Periodic refresh pauses while browsing AI results.
  A single bounded Gemini embedding batch compares
  structured learning/teaching categories. No identity, raw narrative, contact,
  address, calendar, budget, photo, achievements or real database IDs are sent.
- Both sides reuse a persisted per-pair cosine value/label and explanation. Its
  fingerprint changes with allowed inputs or model changes. Labels use fixed
  thresholds rather than a candidate's position in a list. Raw percentages are
  not presented as scientific probabilities.
- Provider count, dimensions, numeric finiteness, nonzero vectors and response
  size are validated. The orchestration deadline is 8.5 seconds; errors/invalid
  output/timeout fall back to rule-ranked eligible cards within ten seconds.
  Concurrency is bounded to eight provider calls. A provider is optional.
- Set `GEMINI_API_KEY` and optionally `GEMINI_EMBED_MODEL` in the ignored `.env`.
  The UI uses configured credentials only on the server. Live ranking was verified
  with fictional test profiles; no recommendation-quality or efficacy claim is made.

## Verification evidence

Saved Match cards now refresh in the background every three seconds and on window
focus/visibility changes. Existing cards stay mounted during background reads and
transient errors. Four focused desktop/mobile checks passed for an incoming mutual
Match appearing without reload and recovery after a failed refresh
(`e2e/matching-refresh.spec.js`). The real workflow also checks the parent's Match
after returning focus, without explicitly reloading the page.

- **110 backend checks passed**; the additional opt-in live-provider test is skipped
  during default regression. The live test was also explicitly run and passed.
- The final focused AI/provider run passed **7 checks**, including the live call,
  bilateral shared value, strict candidate filtering, invalid-vector fallback,
  timeout fallback and HTTP adapter response validation.
- **6 Playwright cases passed** on desktop Chromium and mobile emulation. The main
  workflow verifies two initial time proposals, a counterproposal, confirmation,
  required cancellation reason and persisted history. It aborts one proposal's
  network request and then succeeds through the unchanged form.
- Frontend lint/build and whitespace checks passed. Mobile/desktop trial screenshots
  were inspected. Browser coverage is emulated; physical devices and every browser
  have not been certified.

Backend tests use isolated `test-db`; browser tests use `tutormatch-e2e` with
temporary database/storage and Mailpit. Tests never reset application users,
matching decisions or quotas. The opt-in `LiveAiIntegrationTest` requires
`RUN_LIVE_AI=true` and the configured key in the test container environment.

The application database was backed up before upgrade. Original record keys were
retained, and student data, matching choices/counters and lesson history matched
the snapshot. Additional admin/profile/evidence activity during verification was
preserved rather than overwritten by restoring the backup. No `.env`, account
fixture, database dump or API credential is included in source changes.

Local ignored evidence: `.local/sprint56-backend-tests.log`,
`.local/sprint56-live-ai-tests.log`, `.local/sprint56-browser-tests.log`,
`.local/sprint56-data-verification.json`, plus screenshots under
`frontend/test-results/`. See `docs/verification.md` for isolated test commands.
