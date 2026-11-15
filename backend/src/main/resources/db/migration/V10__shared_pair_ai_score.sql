CREATE TABLE pair_ai_scores (
 student_id VARCHAR(36) NOT NULL,
 tutor_id VARCHAR(36) NOT NULL,
 fingerprint CHAR(64) NOT NULL,
 score DOUBLE NOT NULL,
 level VARCHAR(8) NOT NULL,
 PRIMARY KEY(student_id,tutor_id),
 FOREIGN KEY(student_id) REFERENCES student_profiles(id),
 FOREIGN KEY(tutor_id) REFERENCES tutor_profiles(id)
);
