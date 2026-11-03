# Database migrations

Flyway owns the MariaDB schema. Hibernate validates only. Never modify V1 on deployed databases. V2 is additive: new auth tables, additional tutor profile workflow columns, private evidence and audit tables, student/tutor swipes, and `lesson_requests`.

V1 `trial_requests` and `reviews` remain legacy tables; new APIs use `lesson_requests`, whose statuses match the current specification (PENDING, ACCEPTED, REJECTED, EXPIRED). No existing trial data is automatically migrated. Before migrating a deployment with legacy trial data, reconcile those records explicitly. V1 tutor verification_status remains synchronized with workflow_state by service operations; DRAFT/PENDING_REVIEW use legacy PENDING.

Existing users receive no default password or activation flags. Existing tutor records become DRAFT and need verified accounts and evidence review. No demo account is activated. Optional `demo` profile seeds fictional unapproved tutors only; Docker app no longer enables it by default.

The generated nullable `pending_pair` column and unique index enforce at most one pending request per student/tutor even under concurrent submissions. Requests expire lazily on list/create/respond. Expiry is not a background notification service. Foreign keys retain relationship integrity; metadata/document contents never appear in public profile queries.

Before deployment, back up MariaDB and test migrations on a restored copy. Keep database and backup access private: identity documents are BLOBs in `tutor_documents`, served only through the admin endpoint. Configure encryption at rest, TLS, encrypted backups and a retention policy at deployment. No public object URLs or static file paths expose documents.

Verification after setup: `docker compose --profile test run --rm backend-test` exercises V1/V2 against the isolated MariaDB test database. Tests create explicitly marked fixtures, never production users. Test database is tmpfs. Stop it without deleting application volumes with `docker compose stop test-db`.
