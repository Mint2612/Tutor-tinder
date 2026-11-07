CREATE TABLE matching_pairs (
 student_id VARCHAR(36) NOT NULL, tutor_id VARCHAR(36) NOT NULL,
 parent_choice VARCHAR(4) NULL CHECK (parent_choice IN ('LIKE','SKIP')),
 tutor_choice VARCHAR(4) NULL CHECK (tutor_choice IN ('LIKE','SKIP')),
 status VARCHAR(12) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','MATCHED','SKIPPED','EXPIRED')),
 first_liked_at TIMESTAMP(6) NULL, expires_at TIMESTAMP(6) NULL, matched_at TIMESTAMP(6) NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(student_id,tutor_id),
 FOREIGN KEY(student_id) REFERENCES student_profiles(id) ON DELETE CASCADE,
 FOREIGN KEY(tutor_id) REFERENCES tutor_profiles(id) ON DELETE CASCADE,
 INDEX idx_matching_tutor(tutor_id,status), INDEX idx_matching_expiry(status,expires_at)
);
CREATE TABLE matching_daily_usage (
 actor VARCHAR(6) NOT NULL CHECK(actor IN ('PARENT','TUTOR')),
 profile_id VARCHAR(36) NOT NULL, usage_day DATE NOT NULL,
 total_used INT NOT NULL DEFAULT 0, incoming_used INT NOT NULL DEFAULT 0,
 PRIMARY KEY(actor,profile_id,usage_day)
);
CREATE TABLE matching_notifications (
 id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL,
 student_id VARCHAR(36) NOT NULL, tutor_id VARCHAR(36) NOT NULL,
 message VARCHAR(300) NOT NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 read_at TIMESTAMP(6) NULL,
 FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 FOREIGN KEY(student_id,tutor_id) REFERENCES matching_pairs(student_id,tutor_id) ON DELETE CASCADE,
 UNIQUE KEY uq_match_notice(user_id,student_id,tutor_id),
 INDEX idx_unread_matches(user_id,read_at)
);
-- Preserve existing choices. A historical one-sided interest never becomes a mutual Match.
INSERT INTO matching_pairs(student_id,tutor_id,parent_choice,status,first_liked_at,expires_at,created_at)
 SELECT student_id,tutor_id,IF(interested,'LIKE','SKIP'),
 IF(NOT interested,'SKIPPED',IF(TIMESTAMPADD(DAY,7,created_at)<=CURRENT_TIMESTAMP,'EXPIRED','OPEN')),
 IF(interested,created_at,NULL),IF(interested,TIMESTAMPADD(DAY,7,created_at),NULL),created_at
 FROM swipe_interactions;
INSERT INTO matching_daily_usage(actor,profile_id,usage_day,total_used,incoming_used)
 SELECT 'PARENT',student_id,DATE(CONVERT_TZ(created_at,'+00:00','+07:00')),COUNT(*),0
 FROM swipe_interactions WHERE interested=TRUE
 GROUP BY student_id,DATE(CONVERT_TZ(created_at,'+00:00','+07:00'));
