-- Seed default system menus
-- This migration creates the foundational menu structure
-- Made idempotent to support retries without FK violations

-- Insert top-level menus (idempotent)
-- Dashboard
INSERT INTO menus (id, code, name, icon, parent_id, level, path, route, display_order, is_visible, requires_auth, created_by, updated_by, created_at, updated_at, deleted, version)
SELECT '018e0001-0000-0000-0000-000000000001', 'DASHBOARD', 'Dashboard', 'dashboard', NULL, 1, '/dashboard', '/dashboard', 1, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000001');

-- Users Management
INSERT INTO menus (id, code, name, icon, parent_id, level, path, route, display_order, is_visible, requires_auth, created_by, updated_by, created_at, updated_at, deleted, version)
SELECT '018e0001-0000-0000-0000-000000000002', 'USERS', 'Users', 'users', NULL, 1, '/users', '/users', 2, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000002');

-- Roles Management
INSERT INTO menus (id, code, name, icon, parent_id, level, path, route, display_order, is_visible, requires_auth, created_by, updated_by, created_at, updated_at, deleted, version)
SELECT '018e0001-0000-0000-0000-000000000003', 'ROLES', 'Roles', 'shield', NULL, 1, '/roles', '/roles', 3, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000003');

-- Groups Management
INSERT INTO menus (id, code, name, icon, parent_id, level, path, route, display_order, is_visible, requires_auth, created_by, updated_by, created_at, updated_at, deleted, version)
SELECT '018e0001-0000-0000-0000-000000000004', 'GROUPS', 'Groups', 'users-group', NULL, 1, '/groups', '/groups', 4, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000004');

-- Organizations
INSERT INTO menus (id, code, name, icon, parent_id, level, path, route, display_order, is_visible, requires_auth, created_by, updated_by, created_at, updated_at, deleted, version)
SELECT '018e0001-0000-0000-0000-000000000005', 'ORGANIZATIONS', 'Organizations', 'building', NULL, 1, '/organizations', '/organizations', 5, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000005');

-- Settings
INSERT INTO menus (id, code, name, icon, parent_id, level, path, route, display_order, is_visible, requires_auth, created_by, updated_by, created_at, updated_at, deleted, version)
SELECT '018e0001-0000-0000-0000-000000000006', 'SETTINGS', 'Settings', 'settings', NULL, 1, '/settings', '/settings', 99, true, true, 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000006');

-- Insert second-level menus (tabs) - idempotent
-- Users tabs
INSERT INTO menu_tabs (id, menu_id, code, name, icon, display_order, is_visible, version)
SELECT '018e0002-0000-0000-0000-000000000001', '018e0001-0000-0000-0000-000000000002', 'USERS_LIST', 'User List', 'list', 1, true, 0
WHERE NOT EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000001');

INSERT INTO menu_tabs (id, menu_id, code, name, icon, display_order, is_visible, version)
SELECT '018e0002-0000-0000-0000-000000000002', '018e0001-0000-0000-0000-000000000002', 'USERS_ROLES', 'User Roles', 'shield-check', 2, true, 0
WHERE NOT EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000002');

-- Roles tabs
INSERT INTO menu_tabs (id, menu_id, code, name, icon, display_order, is_visible, version)
SELECT '018e0002-0000-0000-0000-000000000003', '018e0001-0000-0000-0000-000000000003', 'ROLES_LIST', 'Role List', 'list', 1, true, 0
WHERE NOT EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000003');

INSERT INTO menu_tabs (id, menu_id, code, name, icon, display_order, is_visible, version)
SELECT '018e0002-0000-0000-0000-000000000004', '018e0001-0000-0000-0000-000000000003', 'ROLES_PERMISSIONS', 'Permissions', 'lock', 2, true, 0
WHERE NOT EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000004');

-- Settings tabs
INSERT INTO menu_tabs (id, menu_id, code, name, icon, display_order, is_visible, version)
SELECT '018e0002-0000-0000-0000-000000000005', '018e0001-0000-0000-0000-000000000006', 'SETTINGS_SYSTEM', 'System Settings', 'cog', 1, true, 0
WHERE NOT EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000005');

INSERT INTO menu_tabs (id, menu_id, code, name, icon, display_order, is_visible, version)
SELECT '018e0002-0000-0000-0000-000000000006', '018e0001-0000-0000-0000-000000000006', 'SETTINGS_PROFILE', 'Profile Settings', 'user-cog', 2, true, 0
WHERE NOT EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000006');

-- Insert third-level menus (screens) - idempotent
-- User List screens
INSERT INTO menu_screens (id, menu_id, tab_id, code, name, route, requires_permission, version)
SELECT '018e0003-0000-0000-0000-000000000001', '018e0001-0000-0000-0000-000000000002', '018e0002-0000-0000-0000-000000000001', 'USERS_VIEW', 'View Users', '/users/list', true, 0
WHERE EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000002')
  AND EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000001')
  AND NOT EXISTS (SELECT 1 FROM menu_screens WHERE id = '018e0003-0000-0000-0000-000000000001');

INSERT INTO menu_screens (id, menu_id, tab_id, code, name, route, requires_permission, version)
SELECT '018e0003-0000-0000-0000-000000000002', '018e0001-0000-0000-0000-000000000002', '018e0002-0000-0000-0000-000000000001', 'USERS_CREATE', 'Create User', '/users/create', true, 0
WHERE EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000002')
  AND EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000001')
  AND NOT EXISTS (SELECT 1 FROM menu_screens WHERE id = '018e0003-0000-0000-0000-000000000002');

INSERT INTO menu_screens (id, menu_id, tab_id, code, name, route, requires_permission, version)
SELECT '018e0003-0000-0000-0000-000000000003', '018e0001-0000-0000-0000-000000000002', '018e0002-0000-0000-0000-000000000001', 'USERS_EDIT', 'Edit User', '/users/edit/:id', true, 0
WHERE EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000002')
  AND EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000001')
  AND NOT EXISTS (SELECT 1 FROM menu_screens WHERE id = '018e0003-0000-0000-0000-000000000003');

-- Role List screens
INSERT INTO menu_screens (id, menu_id, tab_id, code, name, route, requires_permission, version)
SELECT '018e0003-0000-0000-0000-000000000004', '018e0001-0000-0000-0000-000000000003', '018e0002-0000-0000-0000-000000000003', 'ROLES_VIEW', 'View Roles', '/roles/list', true, 0
WHERE EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000003')
  AND EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000003')
  AND NOT EXISTS (SELECT 1 FROM menu_screens WHERE id = '018e0003-0000-0000-0000-000000000004');

INSERT INTO menu_screens (id, menu_id, tab_id, code, name, route, requires_permission, version)
SELECT '018e0003-0000-0000-0000-000000000005', '018e0001-0000-0000-0000-000000000003', '018e0002-0000-0000-0000-000000000003', 'ROLES_CREATE', 'Create Role', '/roles/create', true, 0
WHERE EXISTS (SELECT 1 FROM menus WHERE id = '018e0001-0000-0000-0000-000000000003')
  AND EXISTS (SELECT 1 FROM menu_tabs WHERE id = '018e0002-0000-0000-0000-000000000003')
  AND NOT EXISTS (SELECT 1 FROM menu_screens WHERE id = '018e0003-0000-0000-0000-000000000005');
