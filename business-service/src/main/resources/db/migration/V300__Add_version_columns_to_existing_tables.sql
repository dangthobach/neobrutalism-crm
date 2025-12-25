-- =====================================================
-- Migration V300: Add Version Columns for Optimistic Locking
--
-- Adds version column to existing critical tables to enable
-- optimistic locking for transaction integrity (Phase 1).
--
-- Tables affected:
-- - customers
-- - contacts
-- - tasks
-- - users
-- - organizations
-- - activities
--
-- @author Admin
-- @since Phase 1
-- =====================================================

-- Customers table
ALTER TABLE customers
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_customers_version
ON customers(version);

COMMENT ON COLUMN customers.version IS 'Optimistic locking version';

-- Contacts table
ALTER TABLE contacts
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_contacts_version
ON contacts(version);

COMMENT ON COLUMN contacts.version IS 'Optimistic locking version';

-- Tasks table
ALTER TABLE tasks
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_tasks_version
ON tasks(version);

COMMENT ON COLUMN tasks.version IS 'Optimistic locking version';

-- Users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_users_version
ON users(version);

COMMENT ON COLUMN users.version IS 'Optimistic locking version';

-- Organizations table
ALTER TABLE organizations
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_organizations_version
ON organizations(version);

COMMENT ON COLUMN organizations.version IS 'Optimistic locking version';

-- Activities table
ALTER TABLE activities
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_activities_version
ON activities(version);

COMMENT ON COLUMN activities.version IS 'Optimistic locking version';
