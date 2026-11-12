-- The name given at registration is the public display name (users.full_name, mirrored into
-- tutor_profiles.display_name). The name a tutor types in their profile is their legal full name:
-- only the owner and admins see it, so admins can check it against the identity documents.
-- Written to be safe to run on a database where an earlier draft of this change (applied as V4)
-- already added the column and moved the names.
ALTER TABLE tutor_profiles
 ADD COLUMN IF NOT EXISTS legal_name VARCHAR(120) NOT NULL DEFAULT '';

-- Existing profiles: whatever the tutor had typed as their name becomes the legal name (nothing is
-- lost) unless it was already the account name, and the public name follows the account's display
-- name. Two statements, because the order of assignments inside a multi-table UPDATE is not
-- guaranteed.
UPDATE tutor_profiles t JOIN users u ON u.id = t.user_id
   SET t.legal_name = t.display_name
 WHERE t.legal_name = '' AND t.display_name <> '' AND t.display_name <> u.full_name;
UPDATE tutor_profiles t JOIN users u ON u.id = t.user_id SET t.display_name = u.full_name;
