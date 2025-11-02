-- ===================================
-- Add Member Tier to Users
-- Version: V114
-- Description: Add member_tier column for LMS/CMS tier-based access control
-- ===================================

ALTER TABLE users
ADD COLUMN member_tier VARCHAR(20) NOT NULL DEFAULT 'FREE';

ALTER TABLE users
ADD CONSTRAINT chk_user_member_tier CHECK (member_tier IN ('FREE', 'SILVER', 'GOLD', 'VIP'));

CREATE INDEX idx_users_member_tier ON users(member_tier);
