ALTER TABLE matching_notifications
 ADD event_key VARCHAR(90) NOT NULL DEFAULT 'MATCH',
 DROP INDEX uq_match_notice,
 ADD UNIQUE KEY uq_pair_event(user_id,student_id,tutor_id,event_key);

CREATE TABLE trial_schedules (
 id VARCHAR(36) PRIMARY KEY,
 student_id VARCHAR(36) NOT NULL,
 tutor_id VARCHAR(36) NOT NULL,
 proposer_role VARCHAR(6) NOT NULL CHECK(proposer_role IN ('PARENT','TUTOR')),
 slots JSON NOT NULL,
 status VARCHAR(12) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','CONFIRMED','CANCELLED','EXPIRED')),
 version INT NOT NULL DEFAULT 1,
 chosen_at TIMESTAMP(6) NULL,
 expires_at TIMESTAMP(6) NOT NULL,
 cancellation_reason VARCHAR(500) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 active_pair TINYINT GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING','CONFIRMED') THEN 1 ELSE NULL END) PERSISTENT,
 FOREIGN KEY(student_id,tutor_id) REFERENCES matching_pairs(student_id,tutor_id),
 UNIQUE KEY uq_active_trial(student_id,tutor_id,active_pair)
);
