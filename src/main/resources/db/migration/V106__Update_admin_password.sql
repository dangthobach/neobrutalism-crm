-- Update admin password to "admin123" for easier login
-- Previous password was "Admin@123"
-- IMPORTANT: Change this password after first login in production!

UPDATE users
SET password_hash = '$2a$10$kGf2BRoXmrTpFYPTvKYwkOvgFw6.JQpDySw5SABYI0hhtKKzH8UgK',
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'system'
WHERE username = 'admin'
  AND tenant_id = 'default';
