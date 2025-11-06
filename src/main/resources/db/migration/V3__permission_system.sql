-- =====================================================
-- ✅ PHASE 1.2: FLYWAY MIGRATION V3
-- Permission System: Menus, API Endpoints, Permissions
-- =====================================================

-- =====================================================
-- MENUS
-- =====================================================
CREATE TABLE menus (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    title VARCHAR(200),
    description TEXT,
    menu_type VARCHAR(50) NOT NULL,
    
    -- Hierarchy
    parent_id UUID,
    level INTEGER DEFAULT 0,
    path VARCHAR(500),
    
    -- Display
    icon VARCHAR(100),
    display_order INTEGER DEFAULT 0,
    
    -- Routing
    route VARCHAR(500),
    component_path VARCHAR(500),
    
    -- Permissions
    required_permission VARCHAR(200),
    
    -- Visibility
    is_visible BOOLEAN DEFAULT true,
    is_enabled BOOLEAN DEFAULT true,
    is_external BOOLEAN DEFAULT false,
    
    -- Target
    target VARCHAR(20), -- _self, _blank, etc.
    
    -- Metadata
    metadata TEXT, -- JSON
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_menu_parent FOREIGN KEY (parent_id) REFERENCES menus(id)
);

-- =====================================================
-- MENU_TABS
-- =====================================================
CREATE TABLE menu_tabs (
    id UUID PRIMARY KEY,
    menu_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    title VARCHAR(200),
    description TEXT,
    
    -- Display
    icon VARCHAR(100),
    display_order INTEGER DEFAULT 0,
    
    -- Routing
    route VARCHAR(500),
    
    -- Visibility
    is_visible BOOLEAN DEFAULT true,
    is_enabled BOOLEAN DEFAULT true,
    
    -- Metadata
    metadata TEXT, -- JSON
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_tab_menu FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE,
    CONSTRAINT uk_tab_code_menu UNIQUE (code, menu_id)
);

-- =====================================================
-- MENU_SCREENS
-- =====================================================
CREATE TABLE menu_screens (
    id UUID PRIMARY KEY,
    menu_id UUID NOT NULL,
    tab_id UUID,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    title VARCHAR(200),
    description TEXT,
    screen_type VARCHAR(50) NOT NULL,
    
    -- Display
    icon VARCHAR(100),
    display_order INTEGER DEFAULT 0,
    
    -- Component
    component_path VARCHAR(500),
    
    -- Permissions
    required_permission VARCHAR(200),
    
    -- Visibility
    is_visible BOOLEAN DEFAULT true,
    is_enabled BOOLEAN DEFAULT true,
    
    -- Metadata
    metadata TEXT, -- JSON
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT fk_screen_menu FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE,
    CONSTRAINT fk_screen_tab FOREIGN KEY (tab_id) REFERENCES menu_tabs(id) ON DELETE CASCADE,
    CONSTRAINT uk_screen_code_menu UNIQUE (code, menu_id)
);

-- =====================================================
-- ROLE_MENUS (Many-to-Many)
-- =====================================================
CREATE TABLE role_menus (
    id UUID PRIMARY KEY,
    role_id UUID NOT NULL,
    menu_id UUID NOT NULL,
    
    -- Access Control
    can_view BOOLEAN DEFAULT true,
    can_create BOOLEAN DEFAULT false,
    can_update BOOLEAN DEFAULT false,
    can_delete BOOLEAN DEFAULT false,
    can_export BOOLEAN DEFAULT false,
    can_import BOOLEAN DEFAULT false,
    
    -- Custom Permissions (JSON array)
    custom_permissions TEXT,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_rm_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rm_menu FOREIGN KEY (menu_id) REFERENCES menus(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

-- =====================================================
-- API_ENDPOINTS
-- =====================================================
CREATE TABLE api_endpoints (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    
    -- HTTP Details
    http_method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL,
    controller_name VARCHAR(200),
    method_name VARCHAR(200),
    
    -- Category
    category VARCHAR(100),
    tags TEXT, -- JSON array
    
    -- Security
    requires_auth BOOLEAN DEFAULT true,
    required_permission VARCHAR(200),
    
    -- Rate Limiting
    rate_limit_enabled BOOLEAN DEFAULT false,
    rate_limit_requests INTEGER,
    rate_limit_window_seconds INTEGER,
    
    -- Monitoring
    is_monitored BOOLEAN DEFAULT true,
    sla_response_time_ms INTEGER,
    
    -- Status
    is_enabled BOOLEAN DEFAULT true,
    is_deprecated BOOLEAN DEFAULT false,
    deprecation_message TEXT,
    
    -- Documentation
    request_example TEXT,
    response_example TEXT,
    
    -- Metadata
    metadata TEXT, -- JSON
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    version INTEGER DEFAULT 0,
    
    CONSTRAINT uk_endpoint_method_path UNIQUE (http_method, path)
);

-- =====================================================
-- ROLE_API_ENDPOINTS (Many-to-Many)
-- =====================================================
CREATE TABLE role_api_endpoints (
    id UUID PRIMARY KEY,
    role_id UUID NOT NULL,
    api_endpoint_id UUID NOT NULL,
    
    -- Access Control
    is_allowed BOOLEAN DEFAULT true,
    
    -- Rate Limiting Override
    rate_limit_override INTEGER,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_rae_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rae_endpoint FOREIGN KEY (api_endpoint_id) REFERENCES api_endpoints(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_endpoint UNIQUE (role_id, api_endpoint_id)
);

-- =====================================================
-- PERMISSIONS (Casbin-style)
-- =====================================================
CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    
    -- Casbin Model: sub, obj, act
    subject VARCHAR(200) NOT NULL, -- role:ADMIN, user:uuid, group:uuid
    object VARCHAR(200) NOT NULL,  -- resource name
    action VARCHAR(100) NOT NULL,  -- read, write, delete, *
    
    -- Effect
    effect VARCHAR(10) NOT NULL DEFAULT 'ALLOW', -- ALLOW or DENY
    
    -- Conditions (JSON)
    conditions TEXT,
    
    -- Organization Scope
    organization_id UUID,
    
    -- Audit
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_perm_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT uk_permission UNIQUE (subject, object, action, organization_id)
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Menus
CREATE INDEX idx_menu_code ON menus(code);
CREATE INDEX idx_menu_parent ON menus(parent_id);
CREATE INDEX idx_menu_type ON menus(menu_type);
CREATE INDEX idx_menu_visible ON menus(is_visible);
CREATE INDEX idx_menu_order ON menus(display_order);

-- Menu Tabs
CREATE INDEX idx_tab_menu ON menu_tabs(menu_id);
CREATE INDEX idx_tab_code ON menu_tabs(code);
CREATE INDEX idx_tab_order ON menu_tabs(display_order);

-- Menu Screens
CREATE INDEX idx_screen_menu ON menu_screens(menu_id);
CREATE INDEX idx_screen_tab ON menu_screens(tab_id);
CREATE INDEX idx_screen_code ON menu_screens(code);
CREATE INDEX idx_screen_type ON menu_screens(screen_type);

-- Role Menus
CREATE INDEX idx_rm_role ON role_menus(role_id);
CREATE INDEX idx_rm_menu ON role_menus(menu_id);

-- API Endpoints
CREATE INDEX idx_endpoint_code ON api_endpoints(code);
CREATE INDEX idx_endpoint_method ON api_endpoints(http_method);
CREATE INDEX idx_endpoint_path ON api_endpoints(path);
CREATE INDEX idx_endpoint_category ON api_endpoints(category);
CREATE INDEX idx_endpoint_enabled ON api_endpoints(is_enabled);

-- Role API Endpoints
CREATE INDEX idx_rae_role ON role_api_endpoints(role_id);
CREATE INDEX idx_rae_endpoint ON role_api_endpoints(api_endpoint_id);

-- Permissions
CREATE INDEX idx_perm_subject ON permissions(subject);
CREATE INDEX idx_perm_object ON permissions(object);
CREATE INDEX idx_perm_action ON permissions(action);
CREATE INDEX idx_perm_org ON permissions(organization_id);
CREATE INDEX idx_perm_effect ON permissions(effect);

-- Composite indexes for permission checks
CREATE INDEX idx_perm_sub_obj_act ON permissions(subject, object, action);
CREATE INDEX idx_perm_org_sub ON permissions(organization_id, subject);

-- =====================================================
-- INITIAL PERMISSION DATA
-- =====================================================

-- SUPER_ADMIN gets all permissions
INSERT INTO permissions (id, subject, object, action, effect)
VALUES (gen_random_uuid(), 'role:SUPER_ADMIN', '*', '*', 'ALLOW');

-- ADMIN gets most permissions except system config
INSERT INTO permissions (id, subject, object, action, effect)
VALUES 
    (gen_random_uuid(), 'role:ADMIN', 'user', '*', 'ALLOW'),
    (gen_random_uuid(), 'role:ADMIN', 'role', '*', 'ALLOW'),
    (gen_random_uuid(), 'role:ADMIN', 'group', '*', 'ALLOW'),
    (gen_random_uuid(), 'role:ADMIN', 'customer', '*', 'ALLOW'),
    (gen_random_uuid(), 'role:ADMIN', 'contact', '*', 'ALLOW'),
    (gen_random_uuid(), 'role:ADMIN', 'branch', '*', 'ALLOW');

-- MANAGER gets read/write for business entities
INSERT INTO permissions (id, subject, object, action, effect)
VALUES 
    (gen_random_uuid(), 'role:MANAGER', 'customer', 'read', 'ALLOW'),
    (gen_random_uuid(), 'role:MANAGER', 'customer', 'write', 'ALLOW'),
    (gen_random_uuid(), 'role:MANAGER', 'contact', '*', 'ALLOW'),
    (gen_random_uuid(), 'role:MANAGER', 'user', 'read', 'ALLOW');

-- USER gets read-only access
INSERT INTO permissions (id, subject, object, action, effect)
VALUES 
    (gen_random_uuid(), 'role:USER', 'customer', 'read', 'ALLOW'),
    (gen_random_uuid(), 'role:USER', 'contact', 'read', 'ALLOW'),
    (gen_random_uuid(), 'role:USER', 'user', 'read', 'ALLOW');

-- GUEST gets minimal read access
INSERT INTO permissions (id, subject, object, action, effect)
VALUES 
    (gen_random_uuid(), 'role:GUEST', 'customer', 'read', 'ALLOW');

-- =====================================================
-- END OF V3 MIGRATION
-- =====================================================
