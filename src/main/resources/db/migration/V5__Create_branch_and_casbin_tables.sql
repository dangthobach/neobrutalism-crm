-- V5: Create Branch table and Casbin policy tables

-- ============================================================================
-- 1. CREATE BRANCHES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS branches (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    organization_id UUID NOT NULL,
    parent_id UUID,
    level INTEGER NOT NULL DEFAULT 0,
    path VARCHAR(500),
    branch_type VARCHAR(20) DEFAULT 'LOCAL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    manager_id UUID,
    email VARCHAR(255),
    phone VARCHAR(50),
    address VARCHAR(500),
    display_order INTEGER DEFAULT 0,
    tenant_id VARCHAR(255) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for branches
CREATE INDEX idx_branch_org_id ON branches(organization_id);
CREATE INDEX idx_branch_parent_id ON branches(parent_id);
CREATE INDEX idx_branch_code ON branches(code);
CREATE INDEX idx_branch_deleted_id ON branches(deleted, id);
CREATE INDEX idx_branch_status ON branches(status);
CREATE INDEX idx_branch_tenant_id ON branches(tenant_id);

-- Foreign key constraints
ALTER TABLE branches ADD CONSTRAINT fk_branch_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
ALTER TABLE branches ADD CONSTRAINT fk_branch_parent FOREIGN KEY (parent_id) REFERENCES branches(id);

-- ============================================================================
-- 2. ADD BRANCH_ID AND DATA_SCOPE TO USERS TABLE
-- ============================================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS branch_id UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS data_scope VARCHAR(20) NOT NULL DEFAULT 'SELF_ONLY';

CREATE INDEX IF NOT EXISTS idx_user_branch_id ON users(branch_id);
CREATE INDEX IF NOT EXISTS idx_user_data_scope ON users(data_scope);

ALTER TABLE users ADD CONSTRAINT fk_user_branch
    FOREIGN KEY (branch_id) REFERENCES branches(id);

-- ============================================================================
-- 3. CREATE CASBIN POLICY TABLE
-- ============================================================================
-- Casbin policy table theo cấu trúc của jCasbin JDBC Adapter
CREATE TABLE IF NOT EXISTS casbin_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ptype VARCHAR(100) NOT NULL,  -- p (policy), g (grouping), g2 (role hierarchy)
    v0 VARCHAR(100),              -- subject (user/role)
    v1 VARCHAR(100),              -- domain (tenant)
    v2 VARCHAR(100),              -- object (resource)
    v3 VARCHAR(100),              -- action
    v4 VARCHAR(100),              -- effect (allow/deny)
    v5 VARCHAR(100)               -- scope (optional)
);

-- Indexes for casbin_rule
CREATE INDEX idx_casbin_rule_ptype ON casbin_rule(ptype);
CREATE INDEX idx_casbin_rule_v0 ON casbin_rule(v0);
CREATE INDEX idx_casbin_rule_v1 ON casbin_rule(v1);
CREATE INDEX idx_casbin_rule_v0_v1 ON casbin_rule(v0, v1);

-- ============================================================================
-- 4. INSERT DEFAULT DATA
-- ============================================================================

-- Insert default branch cho các organizations đã có
INSERT INTO branches (id, code, name, organization_id, level, path, branch_type, status, tenant_id, created_by)
SELECT
    RANDOM_UUID(),
    'HQ',
    org.name || ' - Head Quarter',
    org.id,
    0,
    '/HQ',
    'HQ',
    'ACTIVE',
    CAST(org.id AS VARCHAR),
    'system'
FROM organizations org
WHERE NOT EXISTS (
    SELECT 1 FROM branches b WHERE b.organization_id = org.id
);

-- Insert default Casbin policies for ADMIN role
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3, v4) VALUES
    -- ROLE_ADMIN có quyền truy cập tất cả APIs
    ('p', 'ROLE_ADMIN', 'default', '/api/.*', '(GET)|(POST)|(PUT)|(DELETE)', 'allow'),

    -- ROLE_MANAGER có quyền đọc và tạo
    ('p', 'ROLE_MANAGER', 'default', '/api/.*', '(GET)|(POST)', 'allow'),

    -- ROLE_ORC có quyền đọc trong phạm vi branch
    ('p', 'ROLE_ORC', 'default', '/api/.*', 'GET', 'allow'),

    -- ROLE_MAKER có quyền tạo và đọc bản ghi của mình
    ('p', 'ROLE_MAKER', 'default', '/api/.*/create', 'POST', 'allow'),
    ('p', 'ROLE_MAKER', 'default', '/api/.*/mine', 'GET', 'allow'),

    -- ROLE_CHECKER có quyền xem và approve
    ('p', 'ROLE_CHECKER', 'default', '/api/.*/approve', 'POST', 'allow'),
    ('p', 'ROLE_CHECKER', 'default', '/api/.*/pending', 'GET', 'allow'),

    -- ROLE_USER có quyền đọc cơ bản
    ('p', 'ROLE_USER', 'default', '/api/users/me', 'GET', 'allow'),
    ('p', 'ROLE_USER', 'default', '/api/organizations', 'GET', 'allow');
