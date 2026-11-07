-- Evidence now lives in object storage (storage_key); rows written before this migration keep their
-- bytes in `content` and are still readable.
ALTER TABLE tutor_documents
 MODIFY content MEDIUMBLOB NULL,
 ADD storage_key VARCHAR(160) NULL;
-- Verification level 3: self-declared academic results/awards, shown as "Tự khai" (unverified).
ALTER TABLE tutor_profiles
 ADD achievements TEXT NULL;
