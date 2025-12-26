-- V6: Fix Entity-Database Schema Mismatches
-- This migration ensures all tables match their corresponding entity definitions

-- ============================================================================
-- 1. FIX ORGANIZATIONS TABLE
-- ============================================================================
-- Add missing columns that exist in Organization entity but not in V1 schema

-- Add description column (from Organization entity line 52-53)
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS description VARCHAR(1000);

-- Add tenant_id column (from TenantAwareAggregateRoot)
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';

-- Add soft delete columns (from SoftDeletableEntity)
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);

-- Add state columns (from StatefulEntity)
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS status_changed_by VARCHAR(100);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS status_reason VARCHAR(500);

-- Fix audit columns type (entity expects VARCHAR, V1 created without proper length)
-- Note: PostgreSQL syntax for ALTER COLUMN
ALTER TABLE organizations ALTER COLUMN created_by TYPE VARCHAR(100);
ALTER TABLE organizations ALTER COLUMN updated_by TYPE VARCHAR(100);

-- ============================================================================
-- 2. FIX USERS TABLE
-- ============================================================================
-- Add missing columns that exist in User entity but not in earlier migrations

-- Add tenant_id column (from TenantAwareAggregateRoot)
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';

-- Add soft delete columns (from SoftDeletableEntity)
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);

-- Add state columns (from StatefulEntity)
ALTER TABLE users ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS status_changed_by VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS status_reason VARCHAR(500);

-- Fix audit columns type (entity expects VARCHAR)
ALTER TABLE users ALTER COLUMN created_by TYPE VARCHAR(100);
ALTER TABLE users ALTER COLUMN updated_by TYPE VARCHAR(100);

-- ============================================================================
-- 3. FIX ROLES TABLE
-- ============================================================================
-- Add missing columns that exist in Role entity but not in V1 schema

-- Add is_system column (entity has this, V1 has is_default instead)
ALTER TABLE roles ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;

-- Add tenant_id column (from TenantAwareAggregateRoot)
ALTER TABLE roles ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';

-- Add soft delete columns (from SoftDeletableEntity)
ALTER TABLE roles ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);

-- Add state columns (from StatefulEntity)
ALTER TABLE roles ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS status_changed_by VARCHAR(100);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS status_reason VARCHAR(500);

-- Fix audit columns type (entity expects VARCHAR)
ALTER TABLE roles ALTER COLUMN created_by TYPE VARCHAR(100);
ALTER TABLE roles ALTER COLUMN updated_by TYPE VARCHAR(100);

-- ============================================================================
-- 4. FIX USER_ROLES TABLE
-- ============================================================================
-- Add missing columns that exist in UserRole entity but not in V1 schema

-- Add is_active column (entity has this, V1 doesn't have it)
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Add granted_at and granted_by columns (entity has these)
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS granted_by VARCHAR(100);

-- Add tenant_id column (from TenantAwareEntity)
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';

-- Add audit columns (from BaseEntity via TenantAwareEntity)
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS created_by VARCHAR(100);
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;

-- ============================================================================
-- 5. FIX MENUS TABLE
-- ============================================================================
-- Add missing columns that exist in Menu entity but not in V3 schema

-- Add requires_auth column (Menu entity line 64-65)
ALTER TABLE menus ADD COLUMN IF NOT EXISTS requires_auth BOOLEAN NOT NULL DEFAULT TRUE;

-- Add soft delete columns (from SoftDeletableEntity)
ALTER TABLE menus ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE menus ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(100);

-- Fix audit columns type (entity expects VARCHAR, V3 created UUID)
ALTER TABLE menus ALTER COLUMN created_by TYPE VARCHAR(100);
ALTER TABLE menus ALTER COLUMN updated_by TYPE VARCHAR(100);

-- Make menu_type nullable (V3 has this as NOT NULL, but Menu entity doesn't have this field)
-- This allows inserts without menu_type for now
ALTER TABLE menus ALTER COLUMN menu_type DROP NOT NULL;

-- ============================================================================
-- 6. FIX MENU_SCREENS TABLE
-- ============================================================================
-- Add missing columns that exist in MenuScreen entity but not in V3 schema

-- Add route column (MenuScreen entity line 45-46)
ALTER TABLE menu_screens ADD COLUMN IF NOT EXISTS route VARCHAR(500);

-- Rename component_path to component (entity has component, V3 has component_path)
ALTER TABLE menu_screens ADD COLUMN IF NOT EXISTS component VARCHAR(500);

-- Add requires_permission column (entity has requires_permission, V3 has required_permission)
ALTER TABLE menu_screens ADD COLUMN IF NOT EXISTS requires_permission BOOLEAN NOT NULL DEFAULT TRUE;

-- Make screen_type nullable (V3 has this as NOT NULL, but MenuScreen entity doesn't have this field)
ALTER TABLE menu_screens ALTER COLUMN screen_type DROP NOT NULL;

-- ============================================================================
-- 7. FIX ROLE_MENUS TABLE
-- ============================================================================
-- Add missing columns that exist in RoleMenu entity but not in V3 schema

-- Add can_edit column (entity has can_edit, V3 has can_update)
ALTER TABLE role_menus ADD COLUMN IF NOT EXISTS can_edit BOOLEAN NOT NULL DEFAULT FALSE;

-- Add tenant_id column (from TenantAwareEntity)
ALTER TABLE role_menus ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';

-- Fix audit columns type (entity expects VARCHAR, V3 created UUID)
ALTER TABLE role_menus ALTER COLUMN created_by TYPE VARCHAR(100);
ALTER TABLE role_menus ALTER COLUMN updated_by TYPE VARCHAR(100);

-- Add version column (from BaseEntity)
ALTER TABLE role_menus ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;

-- ============================================================================
-- 8. ADD INDEXES FOR NEW COLUMNS
-- ============================================================================

-- Organization indexes
CREATE INDEX IF NOT EXISTS idx_org_tenant_id ON organizations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_org_tenant_deleted ON organizations(tenant_id, deleted);
CREATE INDEX IF NOT EXISTS idx_org_deleted_at ON organizations(deleted_at);

-- User indexes
CREATE INDEX IF NOT EXISTS idx_user_tenant_id ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_tenant_deleted ON users(tenant_id, deleted);
CREATE INDEX IF NOT EXISTS idx_user_deleted_at ON users(deleted_at);

-- Role indexes
CREATE INDEX IF NOT EXISTS idx_role_tenant_id ON roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_role_tenant_deleted ON roles(tenant_id, deleted);
CREATE INDEX IF NOT EXISTS idx_role_deleted_at ON roles(deleted_at);
CREATE INDEX IF NOT EXISTS idx_role_is_system ON roles(is_system);

-- UserRole indexes
CREATE INDEX IF NOT EXISTS idx_ur_tenant_id ON user_roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ur_is_active ON user_roles(is_active);
CREATE INDEX IF NOT EXISTS idx_ur_granted_at ON user_roles(granted_at);

-- Menu indexes
CREATE INDEX IF NOT EXISTS idx_menu_deleted ON menus(deleted);
CREATE INDEX IF NOT EXISTS idx_menu_requires_auth ON menus(requires_auth);

-- RoleMenu indexes
CREATE INDEX IF NOT EXISTS idx_rm_tenant_id ON role_menus(tenant_id);

-- ============================================================================
-- NOTES
-- ============================================================================
-- This migration fixes the following schema mismatches:
-- 1. Organizations table missing: description, tenant_id, deleted*, status_changed*
-- 2. Users table missing: tenant_id, deleted*, status_changed*
-- 3. Roles table missing: is_system, tenant_id, deleted*, status_changed*
-- 4. User_roles table missing: is_active, granted_at/by, tenant_id, audit columns
-- 5. Menus table missing: requires_auth, deleted, deleted_by
-- 6. Audit columns (created_by/updated_by) had wrong type (UUID vs VARCHAR(100))
--
-- These mismatches caused:
-- - V100 INSERT to fail (missing description column)
-- - V5 INSERT to fail (trying to insert 'system' string into UUID column)
-- - V102 INSERT to fail (missing is_system column)
-- - V104 INSERT to fail (missing requires_auth column)
-- - Future entity operations to fail due to missing tenant_id, deleted columns
