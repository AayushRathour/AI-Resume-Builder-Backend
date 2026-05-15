-- ResumeAI: Google sign-in session remediation
-- Run against auth_db in MySQL.
-- Use the preview SELECT first, then run the scoped UPDATE only for impacted users.

USE auth_db;

-- 1) Preview candidate accounts that will be blocked by /auth/profile.
SELECT user_id, email, provider, is_active, is_deleted, deleted_at, role, updated_at
FROM users
WHERE provider = 'GOOGLE'
  AND (is_active = 0 OR is_deleted = 1)
ORDER BY updated_at DESC;

-- 2) Reactivate a single account safely (recommended).
-- Replace the email before executing.
UPDATE users
SET is_active = 1,
    is_deleted = 0,
    deleted_at = NULL,
    updated_at = NOW()
WHERE email = 'replace-me@example.com'
  AND provider = 'GOOGLE';

-- 3) Bulk repair (optional, use with care).
-- Uncomment only if many GOOGLE users were incorrectly flagged inactive/deleted.
-- UPDATE users
-- SET is_active = 1,
--     is_deleted = 0,
--     deleted_at = NULL,
--     updated_at = NOW()
-- WHERE provider = 'GOOGLE'
--   AND (is_active = 0 OR is_deleted = 1);

