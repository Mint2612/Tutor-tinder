ALTER TABLE users ADD deleted_at TIMESTAMP(6) NULL;
ALTER TABLE student_profiles
 ADD visible BOOLEAN NOT NULL DEFAULT TRUE,
 ADD deleted_at TIMESTAMP(6) NULL;
ALTER TABLE tutor_profiles
 ADD accepting_students BOOLEAN NOT NULL DEFAULT TRUE,
 ADD deleted_at TIMESTAMP(6) NULL;
ALTER TABLE lesson_requests ADD contact_revoked BOOLEAN NOT NULL DEFAULT FALSE;
