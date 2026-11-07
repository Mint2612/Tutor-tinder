-- TutorMatch local: MariaDB, 127.0.0.1:3306, database/user tutormatch.
-- Password: DATABASE_PASSWORD in the ignored root .env file.
-- Run SET first, then rerun the SELECT you want after each action in the app.
-- These statements only read application tables. SET changes a connection-local variable.
SET @test_email = 'parent.sprint12@example.test';

-- Database clock; TIMESTAMP values follow the connection time zone.
SELECT DATABASE() AS current_database, CURRENT_TIMESTAMP AS db_now,
       UTC_TIMESTAMP() AS utc_now, @@session.time_zone AS connection_time_zone;

-- S1-01 / S1-02: registration and verified email. Password contents are not displayed.
SELECT id, email, full_name, role, email_verified, created_at,
       CASE
         WHEN password_hash IS NULL THEN 'MISSING'
         WHEN password_hash LIKE '$2%' THEN 'BCrypt'
         ELSE 'CHECK_FORMAT'
       END AS password_storage,
       CHAR_LENGTH(password_hash) AS password_hash_length
FROM users
WHERE email = @test_email;

-- S1-03: login creates a row; refresh reuses it; logout deletes that session.
-- Expired rows can remain physically present but are rejected by authentication.
SELECT u.email, LEFT(s.token_hash, 12) AS token_hash_prefix,
       CHAR_LENGTH(s.token_hash) AS token_hash_length,
       s.expires_at,
       CASE WHEN s.expires_at <= CURRENT_TIMESTAMP THEN 'EXPIRED'
            WHEN u.email_verified = 0 THEN 'ACCOUNT_NOT_VERIFIED'
            ELSE 'ACTIVE' END AS session_state,
       GREATEST(0, TIMESTAMPDIFF(MINUTE, CURRENT_TIMESTAMP, s.expires_at)) AS minutes_remaining
FROM auth_sessions s
JOIN users u ON u.id = s.user_id
WHERE u.email = @test_email
ORDER BY s.expires_at DESC;

-- S1-02 / S1-04: EMAIL or RESET challenge. Raw email/reset codes are never stored here.
-- Verification consumes its challenge; successful password reset removes all account challenges.
SELECT u.email, c.id AS challenge_id, c.purpose, c.attempts,
       LEFT(c.secret_hash, 12) AS secret_hash_prefix,
       CHAR_LENGTH(c.secret_hash) AS secret_hash_length,
       c.expires_at,
       CASE
         WHEN c.expires_at <= CURRENT_TIMESTAMP THEN 'EXPIRED'
         WHEN c.attempts >= 5 THEN 'NO_ATTEMPTS_LEFT'
         ELSE 'VALID_WINDOW'
       END AS challenge_state
FROM auth_challenges c
JOIN users u ON u.id = c.user_id
WHERE u.email = @test_email
ORDER BY c.expires_at DESC;

-- Summary: reset password should leave zero active sessions for this account.
SELECT u.email,
       COUNT(s.token_hash) AS stored_session_rows,
       COALESCE(SUM(s.expires_at > CURRENT_TIMESTAMP AND u.email_verified = 1), 0) AS active_sessions
FROM users u
LEFT JOIN auth_sessions s ON s.user_id = u.id
WHERE u.email = @test_email
GROUP BY u.id, u.email;

-- Rate limiting uses hashed bucket identifiers and a 15-minute window.
SELECT LEFT(bucket, 12) AS bucket_hash_prefix, attempts, window_start,
       TIMESTAMPADD(MINUTE, 15, window_start) AS window_ends_at,
       CASE WHEN window_start < TIMESTAMPADD(MINUTE, -15, CURRENT_TIMESTAMP)
            THEN 0 ELSE attempts END AS attempts_in_current_window
FROM auth_throttles
ORDER BY window_start DESC
LIMIT 20;
