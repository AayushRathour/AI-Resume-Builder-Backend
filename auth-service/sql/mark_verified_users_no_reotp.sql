-- ResumeAI OTP loop fix
-- Goal: users who already verified once should not be forced through OTP on every login.
-- Run this in MySQL against auth_db.

USE auth_db;

-- Preview users currently unverified.
SELECT user_id, email, role, provider, is_active, is_deleted, is_verified, updated_at
FROM users
WHERE is_verified = 0
ORDER BY updated_at DESC;

-- Mark active, non-deleted users as verified.
-- This is safe for existing legitimate accounts and stops repeated OTP prompts.
UPDATE users
SET is_verified = 1,
    updated_at = NOW()
WHERE is_verified = 0
  AND is_active = 1
  AND is_deleted = 0;

