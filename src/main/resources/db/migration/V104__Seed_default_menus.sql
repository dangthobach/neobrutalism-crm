-- Seed default system menus
-- This migration creates the foundational menu structure

-- Insert top-level menus
INSERT INTO menus (id, code, name, icon, parent_id, level, path, route, display_order, is_visible, requires_auth, created_by, updated_by, created_at, updated_at, deleted, version)
VALUES
    -- Dashboard
    ('018e0001-0000-0000-0000-000000000001', 'DASHBOARD', 'Dashboard', 'dashboard', NULL, 1, '/dashboard', '/dashboard', 1, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0),

    -- Users Management
    ('018e0001-0000-0000-0000-000000000002', 'USERS', 'Users', 'users', NULL, 1, '/users', '/users', 2, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0),

    -- Roles Management
    ('018e0001-0000-0000-0000-000000000003', 'ROLES', 'Roles', 'shield', NULL, 1, '/roles', '/roles', 3, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0),

    -- Groups Management
    ('018e0001-0000-0000-0000-000000000004', 'GROUPS', 'Groups', 'users-group', NULL, 1, '/groups', '/groups', 4, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0),

    -- Organizations
    ('018e0001-0000-0000-0000-000000000005', 'ORGANIZATIONS', 'Organizations', 'building', NULL, 1, '/organizations', '/organizations', 5, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0),

    -- Settings
    ('018e0001-0000-0000-0000-000000000006', 'SETTINGS', 'Settings', 'settings', NULL, 1, '/settings', '/settings', 99, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0);

-- Insert second-level menus (tabs)
INSERT INTO menu_tabs (id, menu_id, code, name, icon, display_order, is_visible, version)
VALUES
    -- Users tabs
    ('018e0002-0000-0000-0000-000000000001', '018e0001-0000-0000-0000-000000000002', 'USERS_LIST', 'User List', 'list', 1, true, 0),
    ('018e0002-0000-0000-0000-000000000002', '018e0001-0000-0000-0000-000000000002', 'USERS_ROLES', 'User Roles', 'shield-check', 2, true, 0),

    -- Roles tabs
    ('018e0002-0000-0000-0000-000000000003', '018e0001-0000-0000-0000-000000000003', 'ROLES_LIST', 'Role List', 'list', 1, true, 0),
    ('018e0002-0000-0000-0000-000000000004', '018e0001-0000-0000-0000-000000000003', 'ROLES_PERMISSIONS', 'Permissions', 'lock', 2, true, 0),

    -- Settings tabs
    ('018e0002-0000-0000-0000-000000000005', '018e0001-0000-0000-0000-000000000006', 'SETTINGS_SYSTEM', 'System Settings', 'cog', 1, true, 0),
    ('018e0002-0000-0000-0000-000000000006', '018e0001-0000-0000-0000-000000000006', 'SETTINGS_PROFILE', 'Profile Settings', 'user-cog', 2, true, 0);

-- Insert third-level menus (screens)
-- Note: MenuScreen doesn't have display_order column
INSERT INTO menu_screens (id, menu_id, tab_id, code, name, route, requires_permission, version)
VALUES
    -- User List screens
    ('018e0003-0000-0000-0000-000000000001', '018e0001-0000-0000-0000-000000000002', '018e0002-0000-0000-0000-000000000001', 'USERS_VIEW', 'View Users', '/users/list', true, 0),
    ('018e0003-0000-0000-0000-000000000002', '018e0001-0000-0000-0000-000000000002', '018e0002-0000-0000-0000-000000000001', 'USERS_CREATE', 'Create User', '/users/create', true, 0),
    ('018e0003-0000-0000-0000-000000000003', '018e0001-0000-0000-0000-000000000002', '018e0002-0000-0000-0000-000000000001', 'USERS_EDIT', 'Edit User', '/users/edit/:id', true, 0),

    -- Role List screens
    ('018e0003-0000-0000-0000-000000000004', '018e0001-0000-0000-0000-000000000003', '018e0002-0000-0000-0000-000000000003', 'ROLES_VIEW', 'View Roles', '/roles/list', true, 0),
    ('018e0003-0000-0000-0000-000000000005', '018e0001-0000-0000-0000-000000000003', '018e0002-0000-0000-0000-000000000003', 'ROLES_CREATE', 'Create Role', '/roles/create', true, 0);
