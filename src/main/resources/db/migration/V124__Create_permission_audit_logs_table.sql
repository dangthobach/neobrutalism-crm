-- ================================================
-- V124: Create Permission Audit Logs Table
-- ================================================
-- Purpose: Track all permission-related changes for security and compliance
-- Created: 2025-12-09
-- Author: Phase 2 Week 9 Implementation
-- ================================================

CREATE TABLE permission_audit_logs (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT DEFAULT 0,

    -- Action Information
    action_type VARCHAR(50) NOT NULL,

    -- Changed By (Who made the change)
    changed_by_user_id UUID NOT NULL,
    changed_by_username VARCHAR(100),

    -- Target (Who/What was affected)
    target_user_id UUID,
    target_username VARCHAR(100),
    target_role_code VARCHAR(100),

    -- Permission Details
    resource VARCHAR(100),
    action VARCHAR(50),
    data_scope VARCHAR(50),
    branch_id UUID,

    -- Change Tracking
    old_value TEXT,
    new_value TEXT,
    reason TEXT,

    -- Request Context
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Operation Status
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,

    -- Multi-Tenancy
    organization_id UUID,
    tenant_id VARCHAR(50) NOT NULL,

    -- Additional Context
    metadata TEXT,
    session_id VARCHAR(100),

    -- Timestamps (inherited pattern, but no created_at/updated_at for audit logs)
    -- Audit logs are immutable once created

    -- Constraints
    CONSTRAINT chk_audit_has_target CHECK (
        target_user_id IS NOT NULL OR
        target_role_code IS NOT NULL OR
        resource IS NOT NULL
    )
);

-- ================================================
-- INDEXES
-- ================================================

-- Primary query indexes
CREATE INDEX idx_perm_audit_user ON permission_audit_logs(changed_by_user_id);
CREATE INDEX idx_perm_audit_target ON permission_audit_logs(target_user_id);
CREATE INDEX idx_perm_audit_action ON permission_audit_logs(action_type);
CREATE INDEX idx_perm_audit_timestamp ON permission_audit_logs(changed_at DESC);
CREATE INDEX idx_perm_audit_tenant ON permission_audit_logs(tenant_id);

-- Composite indexes for common queries
CREATE INDEX idx_perm_audit_target_time ON permission_audit_logs(target_user_id, changed_at DESC);
CREATE INDEX idx_perm_audit_action_time ON permission_audit_logs(action_type, changed_at DESC);
CREATE INDEX idx_perm_audit_tenant_time ON permission_audit_logs(tenant_id, changed_at DESC);

-- Security monitoring indexes
CREATE INDEX idx_perm_audit_failed ON permission_audit_logs(success, changed_at DESC) WHERE success = FALSE;
CREATE INDEX idx_perm_audit_critical ON permission_audit_logs(action_type, changed_at DESC)
    WHERE action_type IN ('UNAUTHORIZED_ACCESS_ATTEMPT', 'PERMISSION_ESCALATION_ATTEMPT', 'DATA_SCOPE_CHANGED');

-- Session correlation index
CREATE INDEX idx_perm_audit_session ON permission_audit_logs(session_id, changed_at ASC) WHERE session_id IS NOT NULL;

-- Role-based queries
CREATE INDEX idx_perm_audit_role ON permission_audit_logs(target_role_code, changed_at DESC) WHERE target_role_code IS NOT NULL;

-- Resource-based queries
CREATE INDEX idx_perm_audit_resource ON permission_audit_logs(resource, action, changed_at DESC) WHERE resource IS NOT NULL;

-- Organization-based queries
CREATE INDEX idx_perm_audit_org ON permission_audit_logs(organization_id, changed_at DESC) WHERE organization_id IS NOT NULL;

-- Search index (for text search on usernames and roles)
CREATE INDEX idx_perm_audit_search ON permission_audit_logs USING gin(
    to_tsvector('english',
        COALESCE(target_username, '') || ' ' ||
        COALESCE(changed_by_username, '') || ' ' ||
        COALESCE(target_role_code, '') || ' ' ||
        COALESCE(resource, '')
    )
);

-- ================================================
-- COMMENTS
-- ================================================

COMMENT ON TABLE permission_audit_logs IS 'Audit trail for all permission-related changes in the system';

COMMENT ON COLUMN permission_audit_logs.action_type IS 'Type of permission action (ROLE_ASSIGNED, POLICY_CREATED, etc.)';
COMMENT ON COLUMN permission_audit_logs.changed_by_user_id IS 'ID of user who made the change';
COMMENT ON COLUMN permission_audit_logs.changed_by_username IS 'Username of person who made change (denormalized for reporting)';
COMMENT ON COLUMN permission_audit_logs.target_user_id IS 'ID of user affected by the change';
COMMENT ON COLUMN permission_audit_logs.target_username IS 'Username of affected user (denormalized)';
COMMENT ON COLUMN permission_audit_logs.target_role_code IS 'Role code for role-based changes';
COMMENT ON COLUMN permission_audit_logs.resource IS 'Resource being controlled (customer, task, etc.)';
COMMENT ON COLUMN permission_audit_logs.action IS 'Action on resource (read, write, delete)';
COMMENT ON COLUMN permission_audit_logs.data_scope IS 'Data scope level (ALL_BRANCHES, CURRENT_BRANCH, SELF_ONLY)';
COMMENT ON COLUMN permission_audit_logs.branch_id IS 'Branch affected by the change';
COMMENT ON COLUMN permission_audit_logs.old_value IS 'Previous value before change (JSON format)';
COMMENT ON COLUMN permission_audit_logs.new_value IS 'New value after change (JSON format)';
COMMENT ON COLUMN permission_audit_logs.reason IS 'Explanation for the change (provided by admin)';
COMMENT ON COLUMN permission_audit_logs.ip_address IS 'IP address of user making the change';
COMMENT ON COLUMN permission_audit_logs.user_agent IS 'Browser/client information';
COMMENT ON COLUMN permission_audit_logs.changed_at IS 'Timestamp when change occurred';
COMMENT ON COLUMN permission_audit_logs.success IS 'Whether the operation succeeded';
COMMENT ON COLUMN permission_audit_logs.error_message IS 'Error details if operation failed';
COMMENT ON COLUMN permission_audit_logs.session_id IS 'Session ID for correlating related changes';
COMMENT ON COLUMN permission_audit_logs.metadata IS 'Additional context (JSON format)';

-- ================================================
-- PERFORMANCE NOTES
-- ================================================
-- 1. Full-text search index created for username/role searches
-- 2. Partial indexes used for filtered queries (failed attempts, critical events)
-- 3. Composite indexes created for common query patterns
-- 4. Consider partitioning by changed_at for tables with > 10M rows
-- 5. Implement data retention policy (delete logs older than N months)

-- ================================================
-- SECURITY NOTES
-- ================================================
-- 1. Audit logs are immutable - no UPDATE or DELETE should be allowed except by retention policy
-- 2. Create separate role with INSERT-only permissions for audit logging
-- 3. Encrypt sensitive fields (reason, old_value, new_value) if required by compliance
-- 4. Monitor for unusual patterns:
--    - High volume of failed attempts from single user
--    - Permission escalation attempts
--    - Bulk permission changes outside business hours
