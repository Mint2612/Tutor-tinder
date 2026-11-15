# Trial reviews

Parents open `/#/reviews` from **Đánh giá** or the trial schedule page.
The page lists their confirmed trial schedules whose chosen start time has passed,
with pending/reviewed/all filters. Submitting requires 1–5 stars, an optional
comment up to 2,000 characters, and explicit confirmation that the child attended.
This confirmation records the parent's report of attendance; elapsed time alone
does not verify that the lesson occurred.

- `GET /api/trial-reviews`: parent-only list with `trialId`, `studentName`,
  `tutorName`, `lessonAt`, `mode`, `rating`, `comment`, `reviewedAt`.
- `POST /api/trial-reviews/{trialId}`: `{rating, comment, attended: true}`.
  Requires authentication, CSRF and ownership. Returns 400 for invalid input,
  403 for other roles, 404 for unknown/other-parent trials, and 409 for a duplicate
  review or a trial that is cancelled, pending, expired or in the future.

Migration `V12__trial_reviews.sql` stores one review per current `trial_schedules`
record. Row locks and a unique trial key prevent concurrent duplicate submissions.
Legacy `reviews` continues to reference legacy `trial_requests`; the new UI uses
the current schedule records and does not change legacy reviews.

Local demo: parent `seed.parent004@tutormatch.local`, child **Học sinh Demo 4**,
tutor `seed.tutor004@tutormatch.local`, trial
`ed440004-1113-4000-8000-000000000001` on 2026-11-13 at 19:00 Vietnam time.
