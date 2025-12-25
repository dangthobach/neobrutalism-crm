-- =====================================================
-- ✅ PHASE 1.2: FLYWAY MIGRATION V1
-- Initial Schema: Organizations, Users, Roles, Groups
-- =====================================================

-- =====================================================
-- ORGANIZATIONS
-- =====================================================
CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(200),
    organization_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    parent_id UUID,
    level INTEGER DEFAULT 0,
    path VARCHAR(500),
    
    -- Contact Information
    email VARCHAR(255),
    phone VARCHAR(20),
    website VARCHAR(500),
    
    -- Address
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    
    -- Business Details
    tax_id VARCHAR(50),
    business_registration_number VARCHAR(50),
    industry VARCHAR(100),
    employee_count INTEGER,
    annual_revenue DECIMAL(15, 2),
    
    -- Settings
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en-US',
    currency VARCHAR(3) DEFAULT 'USD',
    date_format VARCHAR(20),
    
    -- Metadata
    is_active BOOLEAN DEFAULT true,
    is_headquarters BOOLEAN DEFAULT false,
    notes TEXT,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_org_parent FOREIGN KEY (parent_id) REFERENCES organizations(id)
);

-- =====================================================
-- USERS
-- =====================================================
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    
    -- Personal Information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    display_name VARCHAR(200),
    
    -- Contact
    phone VARCHAR(20),
    mobile VARCHAR(20),
    avatar VARCHAR(500),
    
    -- Job Information
    job_title VARCHAR(100),
    department VARCHAR(100),
    date_of_birth DATE,
    hire_date DATE,
    
    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    
    -- Security
    password_expiry_date TIMESTAMP,
    last_login_at TIMESTAMP,
    last_password_change_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    account_locked_until TIMESTAMP,
    
    -- Organization
    organization_id UUID NOT NULL,
    
    -- Preferences
    locale VARCHAR(10) DEFAULT 'en-US',
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    -- Flags
    is_system_user BOOLEAN DEFAULT false,
    is_two_factor_enabled BOOLEAN DEFAULT false,
    is_email_verified BOOLEAN DEFAULT false,
    is_phone_verified BOOLEAN DEFAULT false,
    
    -- Notes
    notes TEXT,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_user_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- =====================================================
-- ROLES
-- =====================================================
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    role_type VARCHAR(50) NOT NULL DEFAULT 'CUSTOM',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    
    -- Organization (NULL for system roles)
    organization_id UUID,
    
    -- Hierarchy
    parent_id UUID,
    level INTEGER DEFAULT 0,
    path VARCHAR(500),
    
    -- Metadata
    priority INTEGER DEFAULT 0,
    is_default BOOLEAN DEFAULT false,
    notes TEXT,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_role_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_role_parent FOREIGN KEY (parent_id) REFERENCES roles(id),
    CONSTRAINT uk_role_code_org UNIQUE (code, organization_id)
);

-- =====================================================
-- GROUPS
-- =====================================================
CREATE TABLE groups (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    group_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    
    -- Organization
    organization_id UUID NOT NULL,
    
    -- Hierarchy
    parent_id UUID,
    level INTEGER DEFAULT 0,
    path VARCHAR(500),
    
    -- Manager
    manager_id UUID,
    
    -- Contact
    email VARCHAR(255),
    location VARCHAR(200),
    
    -- Metadata
    max_members INTEGER,
    current_member_count INTEGER DEFAULT 0,
    priority INTEGER DEFAULT 0,
    is_default BOOLEAN DEFAULT false,
    allow_auto_join BOOLEAN DEFAULT false,
    require_approval BOOLEAN DEFAULT true,
    is_virtual BOOLEAN DEFAULT false,
    
    -- Tags (stored as JSON array)
    tags TEXT,
    
    -- Notes
    notes TEXT,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_group_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_group_parent FOREIGN KEY (parent_id) REFERENCES groups(id),
    CONSTRAINT fk_group_manager FOREIGN KEY (manager_id) REFERENCES users(id),
    CONSTRAINT uk_group_code_org UNIQUE (code, organization_id)
);

-- =====================================================
-- USER_ROLES (Many-to-Many)
-- =====================================================
CREATE TABLE user_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_by UUID,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_primary BOOLEAN DEFAULT false,
    notes TEXT,
    
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- =====================================================
-- USER_GROUPS (Many-to-Many)
-- =====================================================
CREATE TABLE user_groups (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    group_id UUID NOT NULL,
    is_primary BOOLEAN DEFAULT false,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_by UUID,
    approved_at TIMESTAMP,
    group_role VARCHAR(50),
    notes TEXT,
    
    CONSTRAINT fk_ug_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ug_group FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_ug_approved_by FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT uk_user_group UNIQUE (user_id, group_id)
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Organizations
CREATE INDEX idx_org_code ON organizations(code);
CREATE INDEX idx_org_parent ON organizations(parent_id);
CREATE INDEX idx_org_type ON organizations(organization_type);
CREATE INDEX idx_org_status ON organizations(status);

-- Users
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_org ON users(organization_id);
CREATE INDEX idx_user_status ON users(status);
CREATE INDEX idx_user_created_at ON users(created_at);

-- Roles
CREATE INDEX idx_role_code ON roles(code);
CREATE INDEX idx_role_org ON roles(organization_id);
CREATE INDEX idx_role_type ON roles(role_type);
CREATE INDEX idx_role_status ON roles(status);
CREATE INDEX idx_role_parent ON roles(parent_id);

-- Groups
CREATE INDEX idx_group_code ON groups(code);
CREATE INDEX idx_group_org ON groups(organization_id);
CREATE INDEX idx_group_type ON groups(group_type);
CREATE INDEX idx_group_status ON groups(status);
CREATE INDEX idx_group_parent ON groups(parent_id);
CREATE INDEX idx_group_manager ON groups(manager_id);

-- User Roles
CREATE INDEX idx_ur_user ON user_roles(user_id);
CREATE INDEX idx_ur_role ON user_roles(role_id);
CREATE INDEX idx_ur_assigned_at ON user_roles(assigned_at);

-- User Groups
CREATE INDEX idx_ug_user ON user_groups(user_id);
CREATE INDEX idx_ug_group ON user_groups(group_id);
CREATE INDEX idx_ug_joined_at ON user_groups(joined_at);

-- =====================================================
-- INITIAL DATA (System Roles & Admin User)
-- =====================================================

-- System Roles
INSERT INTO roles (id, code, name, description, role_type, status, organization_id)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Super Administrator', 'Full system access', 'SYSTEM', 'ACTIVE', NULL),
    ('00000000-0000-0000-0000-000000000002', 'ADMIN', 'Administrator', 'Organization admin access', 'SYSTEM', 'ACTIVE', NULL),
    ('00000000-0000-0000-0000-000000000003', 'MANAGER', 'Manager', 'Management access', 'SYSTEM', 'ACTIVE', NULL),
    ('00000000-0000-0000-0000-000000000004', 'USER', 'User', 'Standard user access', 'SYSTEM', 'ACTIVE', NULL),
    ('00000000-0000-0000-0000-000000000005', 'GUEST', 'Guest', 'Read-only access', 'SYSTEM', 'ACTIVE', NULL);

-- Default Organization
INSERT INTO organizations (id, code, name, organization_type, status, is_headquarters)
VALUES ('00000000-0000-0000-0000-000000000001', 'DEFAULT', 'Default Organization', 'ENTERPRISE', 'ACTIVE', true);

-- System Admin User (password: Admin@123)
INSERT INTO users (id, username, email, password_hash, first_name, last_name, status, organization_id, is_system_user)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@neobrutalism-crm.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye1VZJIvJTHKPl0./zROXfVD1u2LoQT.i', -- bcrypt hash of "Admin@123"
    'System',
    'Administrator',
    'ACTIVE',
    '00000000-0000-0000-0000-000000000001',
    true
);

-- Assign SUPER_ADMIN role to admin user
INSERT INTO user_roles (id, user_id, role_id, is_primary)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    true
);

-- =====================================================
-- END OF V1 MIGRATION
-- =====================================================
