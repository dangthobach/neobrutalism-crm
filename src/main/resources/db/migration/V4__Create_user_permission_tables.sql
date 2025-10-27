-- =====================================================
-- User and Permission Management Tables
-- Created for complete RBAC (Role-Based Access Control)
-- =====================================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    version BIGINT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar VARCHAR(500),
    organization_id UUID NOT NULL,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    password_changed_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0 NOT NULL,
    locked_until TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    status_changed_at TIMESTAMP,
    status_changed_by VARCHAR(100),
    status_reason VARCHAR(500),
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    tenant_id VARCHAR(50) NOT NULL
);

CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_org_id ON users(organization_id);
CREATE INDEX idx_user_deleted_id ON users(deleted, id);
CREATE INDEX idx_user_status ON users(status);

-- Groups table (with hierarchy support)
CREATE TABLE IF NOT EXISTS groups (
    id UUID PRIMARY KEY,
    version BIGINT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    parent_id UUID,
    organization_id UUID NOT NULL,
    level INTEGER DEFAULT 1 NOT NULL,
    path VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    status_changed_at TIMESTAMP,
    status_changed_by VARCHAR(100),
    status_reason VARCHAR(500),
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    tenant_id VARCHAR(50) NOT NULL
);

CREATE INDEX idx_group_code ON groups(code);
CREATE INDEX idx_group_parent ON groups(parent_id);
CREATE INDEX idx_group_org ON groups(organization_id);
CREATE INDEX idx_group_deleted_id ON groups(deleted, id);
CREATE INDEX idx_group_path ON groups(path);

-- User Groups junction table
CREATE TABLE IF NOT EXISTS user_groups (
    id UUID PRIMARY KEY,
    version BIGINT,
    user_id UUID NOT NULL,
    group_id UUID NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    tenant_id VARCHAR(50) NOT NULL,
    CONSTRAINT uk_user_group UNIQUE (user_id, group_id)
);

CREATE INDEX idx_ug_user ON user_groups(user_id);
CREATE INDEX idx_ug_group ON user_groups(group_id);
CREATE INDEX idx_ug_primary ON user_groups(is_primary);

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY,
    version BIGINT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    organization_id UUID NOT NULL,
    is_system BOOLEAN DEFAULT FALSE NOT NULL,
    priority INTEGER DEFAULT 0 NOT NULL,
    status VARCHAR(50) NOT NULL,
    status_changed_at TIMESTAMP,
    status_changed_by VARCHAR(100),
    status_reason VARCHAR(500),
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    tenant_id VARCHAR(50) NOT NULL
);

CREATE INDEX idx_role_code ON roles(code);
CREATE INDEX idx_role_org ON roles(organization_id);
CREATE INDEX idx_role_deleted_id ON roles(deleted, id);

-- User Roles junction table
CREATE TABLE IF NOT EXISTS user_roles (
    id UUID PRIMARY KEY,
    version BIGINT,
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    granted_by VARCHAR(100),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    tenant_id VARCHAR(50) NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_ur_user ON user_roles(user_id);
CREATE INDEX idx_ur_role ON user_roles(role_id);
CREATE INDEX idx_ur_active ON user_roles(is_active);

-- Group Roles junction table
CREATE TABLE IF NOT EXISTS group_roles (
    id UUID PRIMARY KEY,
    version BIGINT,
    group_id UUID NOT NULL,
    role_id UUID NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    granted_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    tenant_id VARCHAR(50) NOT NULL,
    CONSTRAINT uk_group_role UNIQUE (group_id, role_id)
);

CREATE INDEX idx_gr_group ON group_roles(group_id);
CREATE INDEX idx_gr_role ON group_roles(role_id);

-- Menus table (with hierarchy support)
CREATE TABLE IF NOT EXISTS menus (
    id UUID PRIMARY KEY,
    version BIGINT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    icon VARCHAR(100),
    parent_id UUID,
    level INTEGER DEFAULT 1 NOT NULL,
    path VARCHAR(500),
    route VARCHAR(500),
    display_order INTEGER DEFAULT 0 NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE NOT NULL,
    requires_auth BOOLEAN DEFAULT TRUE NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE INDEX idx_menu_code ON menus(code);
CREATE INDEX idx_menu_parent ON menus(parent_id);
CREATE INDEX idx_menu_order ON menus(display_order);
CREATE INDEX idx_menu_deleted_id ON menus(deleted, id);
CREATE INDEX idx_menu_path ON menus(path);

-- Menu Tabs table
CREATE TABLE IF NOT EXISTS menu_tabs (
    id UUID PRIMARY KEY,
    version BIGINT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    menu_id UUID NOT NULL,
    icon VARCHAR(100),
    display_order INTEGER DEFAULT 0 NOT NULL,
    is_visible BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE INDEX idx_tab_menu ON menu_tabs(menu_id);
CREATE INDEX idx_tab_code ON menu_tabs(code);
CREATE INDEX idx_tab_order ON menu_tabs(display_order);

-- Menu Screens table
CREATE TABLE IF NOT EXISTS menu_screens (
    id UUID PRIMARY KEY,
    version BIGINT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    menu_id UUID,
    tab_id UUID,
    route VARCHAR(500),
    component VARCHAR(500),
    requires_permission BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE INDEX idx_screen_menu ON menu_screens(menu_id);
CREATE INDEX idx_screen_tab ON menu_screens(tab_id);
CREATE INDEX idx_screen_code ON menu_screens(code);

-- API Endpoints table
CREATE TABLE IF NOT EXISTS api_endpoints (
    id UUID PRIMARY KEY,
    version BIGINT,
    method VARCHAR(20) NOT NULL,
    path VARCHAR(500) NOT NULL,
    tag VARCHAR(100),
    description VARCHAR(500),
    requires_auth BOOLEAN DEFAULT TRUE NOT NULL,
    is_public BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT uk_method_path UNIQUE (method, path)
);

CREATE INDEX idx_api_method_path ON api_endpoints(method, path);
CREATE INDEX idx_api_tag ON api_endpoints(tag);

-- Screen API Endpoints junction table
CREATE TABLE IF NOT EXISTS screen_api_endpoints (
    id UUID PRIMARY KEY,
    version BIGINT,
    screen_id UUID NOT NULL,
    endpoint_id UUID NOT NULL,
    required_permission VARCHAR(20) NOT NULL,
    CONSTRAINT uk_screen_endpoint UNIQUE (screen_id, endpoint_id)
);

CREATE INDEX idx_sae_screen ON screen_api_endpoints(screen_id);
CREATE INDEX idx_sae_endpoint ON screen_api_endpoints(endpoint_id);

-- Role Menu Permissions table
CREATE TABLE IF NOT EXISTS role_menus (
    id UUID PRIMARY KEY,
    version BIGINT,
    role_id UUID NOT NULL,
    menu_id UUID NOT NULL,
    can_view BOOLEAN DEFAULT TRUE NOT NULL,
    can_create BOOLEAN DEFAULT FALSE NOT NULL,
    can_edit BOOLEAN DEFAULT FALSE NOT NULL,
    can_delete BOOLEAN DEFAULT FALSE NOT NULL,
    can_export BOOLEAN DEFAULT FALSE NOT NULL,
    can_import BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    tenant_id VARCHAR(50) NOT NULL,
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

CREATE INDEX idx_rm_role ON role_menus(role_id);
CREATE INDEX idx_rm_menu ON role_menus(menu_id);

-- Comments
COMMENT ON TABLE users IS 'System users with authentication and profile information';
COMMENT ON TABLE groups IS 'Hierarchical user groups/teams';
COMMENT ON TABLE user_groups IS 'User to group assignments';
COMMENT ON TABLE roles IS 'Role definitions for RBAC';
COMMENT ON TABLE user_roles IS 'User to role assignments with optional expiration';
COMMENT ON TABLE group_roles IS 'Group to role assignments';
COMMENT ON TABLE menus IS 'Hierarchical menu structure';
COMMENT ON TABLE menu_tabs IS 'Tabs within menus';
COMMENT ON TABLE menu_screens IS 'Screens/pages accessible from menus or tabs';
COMMENT ON TABLE api_endpoints IS 'API endpoints for permission control';
COMMENT ON TABLE screen_api_endpoints IS 'Screen to API endpoint mappings';
COMMENT ON TABLE role_menus IS 'Role to menu permission mappings';
