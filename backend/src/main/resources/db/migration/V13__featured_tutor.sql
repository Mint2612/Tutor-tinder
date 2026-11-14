-- A featured tutor is listed first in every parent's discovery and AI suggestion list (after the
-- eligibility rules; it never bypasses them). Used by demo environments to make one profile easy to find.
ALTER TABLE tutor_profiles ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE;
