ALTER TABLE users
 ADD password_hash VARCHAR(100) NULL,
 ADD phone VARCHAR(24) NULL,
 ADD email_verified BOOLEAN NOT NULL DEFAULT FALSE,
 ADD phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE auth_sessions (
 token_hash CHAR(64) PRIMARY KEY, user_id VARCHAR(36) NOT NULL,
 expires_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE auth_challenges (
 id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL,
 purpose VARCHAR(16) NOT NULL, secret_hash CHAR(64) NOT NULL,
 expires_at TIMESTAMP(6) NOT NULL, attempts INT NOT NULL DEFAULT 0,
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 INDEX idx_challenge_user(user_id, purpose)
);
CREATE TABLE auth_throttles (
 bucket CHAR(64) PRIMARY KEY, attempts INT NOT NULL, window_start TIMESTAMP(6) NOT NULL
);
ALTER TABLE tutor_profiles
 ADD display_name VARCHAR(120) NOT NULL DEFAULT '',
 ADD photo_url VARCHAR(500) NOT NULL DEFAULT '',
 ADD background TEXT NULL,
 ADD experience_years INT NOT NULL DEFAULT 0,
 ADD teaching_style VARCHAR(500) NOT NULL DEFAULT '',
 ADD workflow_state VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 ADD visible BOOLEAN NOT NULL DEFAULT TRUE,
 ADD rejection_reason VARCHAR(1000) NULL,
 ADD revision INT NOT NULL DEFAULT 0,
 ADD submitted_at TIMESTAMP(6) NULL,
 ADD submission_id VARCHAR(36) NULL;
CREATE TABLE tutor_documents (
 id VARCHAR(36) PRIMARY KEY, tutor_id VARCHAR(36) NOT NULL,
 filename VARCHAR(200) NOT NULL, content_type VARCHAR(50) NOT NULL,
 content MEDIUMBLOB NOT NULL, revision INT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (tutor_id) REFERENCES tutor_profiles(id) ON DELETE CASCADE
);
CREATE TABLE document_reviews (
 document_id VARCHAR(36) NOT NULL, admin_id VARCHAR(36) NOT NULL,
 submission_id VARCHAR(36) NULL,
 reviewed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY (document_id, admin_id),
 FOREIGN KEY (document_id) REFERENCES tutor_documents(id) ON DELETE CASCADE,
 FOREIGN KEY (admin_id) REFERENCES users(id)
);
CREATE TABLE verification_audit (
 id VARCHAR(36) PRIMARY KEY, tutor_id VARCHAR(36) NOT NULL,
 admin_id VARCHAR(36) NOT NULL, decision VARCHAR(20) NOT NULL,
 reason VARCHAR(1000), created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (tutor_id) REFERENCES tutor_profiles(id), FOREIGN KEY (admin_id) REFERENCES users(id)
);
CREATE TABLE swipe_interactions (
 student_id VARCHAR(36) NOT NULL, tutor_id VARCHAR(36) NOT NULL,
 interested BOOLEAN NOT NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(student_id, tutor_id),
 FOREIGN KEY(student_id) REFERENCES student_profiles(id) ON DELETE CASCADE,
 FOREIGN KEY(tutor_id) REFERENCES tutor_profiles(id) ON DELETE CASCADE
);
CREATE TABLE lesson_requests (
 id VARCHAR(36) PRIMARY KEY, student_id VARCHAR(36) NOT NULL, tutor_id VARCHAR(36) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','ACCEPTED','REJECTED','EXPIRED')),
 proposed_at TIMESTAMP(6) NOT NULL, message VARCHAR(2000) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 expires_at TIMESTAMP(6) NOT NULL, responded_at TIMESTAMP(6) NULL,
 pending_pair VARCHAR(73) GENERATED ALWAYS AS (CASE WHEN status='PENDING' THEN CONCAT(student_id,':',tutor_id) ELSE NULL END) STORED,
 UNIQUE KEY uq_pending_pair(pending_pair),
 FOREIGN KEY(student_id) REFERENCES student_profiles(id), FOREIGN KEY(tutor_id) REFERENCES tutor_profiles(id),
 INDEX idx_lesson_tutor(tutor_id,status), INDEX idx_lesson_student(student_id,status)
);
