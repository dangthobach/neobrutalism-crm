-- Flyway Migration V1: Create jCasbin policy tables
-- Used for storing RBAC policies and role assignments

-- Casbin rule table for storing policies and role assignments
CREATE TABLE IF NOT EXISTS casbin_rule (
    id BIGSERIAL PRIMARY KEY,
    ptype VARCHAR(100) NOT NULL,  -- Policy type: 'p' for policy, 'g' for role
    v0 VARCHAR(255),               -- Subject (userId or roleId)
    v1 VARCHAR(255),               -- Domain (tenantId)
    v2 VARCHAR(255),               -- Object/Resource (API path)
    v3 VARCHAR(255),               -- Action (HTTP method or operation)
    v4 VARCHAR(255),
    v5 VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for fast policy lookup
CREATE INDEX idx_casbin_rule_ptype ON casbin_rule(ptype);
CREATE INDEX idx_casbin_rule_v0 ON casbin_rule(v0);
CREATE INDEX idx_casbin_rule_v1 ON casbin_rule(v1);
CREATE INDEX idx_casbin_rule_v0_v1 ON casbin_rule(v0, v1);
CREATE INDEX idx_casbin_rule_ptype_v0_v1 ON casbin_rule(ptype, v0, v1);

-- Materialized view for fast permission lookups
-- Pre-joins policies and role assignments for common queries
CREATE MATERIALIZED VIEW user_permissions_mv AS
SELECT
    g.v0 as user_id,           -- User ID
    g.v1 as tenant_id,         -- Tenant ID
    p.v2 as resource,          -- Resource/Object
    p.v3 as action,            -- Action
    p.ptype as policy_type
FROM casbin_rule g
INNER JOIN casbin_rule p ON g.v1 = p.v1 AND g.v2 = p.v0
WHERE g.ptype = 'g' AND p.ptype = 'p';

-- Index on materialized view for fast lookups
CREATE INDEX idx_user_permissions_mv_user_tenant ON user_permissions_mv(user_id, tenant_id);
CREATE INDEX idx_user_permissions_mv_resource ON user_permissions_mv(resource);

-- Function to refresh materialized view
CREATE OR REPLACE FUNCTION refresh_user_permissions_mv()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY user_permissions_mv;
END;
$$ LANGUAGE plpgsql;

-- User roles cache table (for L2 Redis fallback)
CREATE TABLE IF NOT EXISTS user_roles_cache (
    user_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    roles JSONB NOT NULL,
    permissions JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, tenant_id)
);

-- Index for cache expiration cleanup
CREATE INDEX idx_user_roles_cache_expires_at ON user_roles_cache(expires_at);

-- Audit log table for permission changes
CREATE TABLE IF NOT EXISTS permission_audit_log (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL,      -- INSERT, UPDATE, DELETE
    entity_type VARCHAR(50) NOT NULL, -- POLICY, ROLE
    entity_id VARCHAR(255),
    user_id VARCHAR(255),
    tenant_id VARCHAR(255),
    old_value JSONB,
    new_value JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for audit log queries
CREATE INDEX idx_permission_audit_log_tenant ON permission_audit_log(tenant_id);
CREATE INDEX idx_permission_audit_log_created_at ON permission_audit_log(created_at);

-- Insert default policies for system roles
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
-- Super Admin: Full access to all resources in all tenants
('p', 'role:super_admin', '*', '/api/**', '(GET|POST|PUT|DELETE)'),

-- Tenant Admin: Full access to all resources in their tenant
('p', 'role:tenant_admin', '*', '/api/customers/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/contacts/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/content/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/courses/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/users/**', '(GET|POST|PUT|DELETE)'),

-- User: Read-only access to basic resources
('p', 'role:user', '*', '/api/customers/**', 'GET'),
('p', 'role:user', '*', '/api/contacts/**', 'GET'),
('p', 'role:user', '*', '/api/content/**', 'GET'),

-- Content Manager: Full access to content
('p', 'role:content_manager', '*', '/api/content/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:content_manager', '*', '/api/categories/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:content_manager', '*', '/api/tags/**', '(GET|POST|PUT|DELETE)'),

-- Instructor: Full access to courses
('p', 'role:instructor', '*', '/api/courses/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:instructor', '*', '/api/modules/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:instructor', '*', '/api/lessons/**', '(GET|POST|PUT|DELETE)'),

-- Student: Read courses and manage own enrollments
('p', 'role:student', '*', '/api/courses/**', 'GET'),
('p', 'role:student', '*', '/api/enrollments/my/**', '(GET|POST)'),
('p', 'role:student', '*', '/api/progress/**', '(GET|POST)');

COMMENT ON TABLE casbin_rule IS 'jCasbin policy storage table';
COMMENT ON TABLE user_permissions_mv IS 'Materialized view for fast permission lookups (refreshed every 60s)';
COMMENT ON TABLE user_roles_cache IS 'User roles and permissions cache for L2 Redis fallback';
COMMENT ON TABLE permission_audit_log IS 'Audit log for all permission changes';
