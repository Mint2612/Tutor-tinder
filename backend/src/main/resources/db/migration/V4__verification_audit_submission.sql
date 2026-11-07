-- Historical submission/revision values are unknown for existing audit rows.
ALTER TABLE verification_audit
 ADD submission_id VARCHAR(36) NULL,
 ADD revision INT NULL;
