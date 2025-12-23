-- Seed default role-menu permissions
-- Grant SUPER_ADMIN full access to all menus
-- Made idempotent to support retries without FK violations

-- SUPER_ADMIN - Dashboard (full access)
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import, tenant_id, created_by, updated_by, created_at, updated_at, version)
SELECT '018e0020-0000-0000-0000-000000000001', '018e0000-0000-0000-0000-000000000001', '018e0001-0000-0000-0000-000000000001',
     true, true, true, true, true, true, 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE id = '018e0020-0000-0000-0000-000000000001');

-- SUPER_ADMIN - Users (full access)
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import, tenant_id, created_by, updated_by, created_at, updated_at, version)
SELECT '018e0020-0000-0000-0000-000000000002', '018e0000-0000-0000-0000-000000000001', '018e0001-0000-0000-0000-000000000002',
     true, true, true, true, true, true, 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE id = '018e0020-0000-0000-0000-000000000002');

-- SUPER_ADMIN - Roles (full access)
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import, tenant_id, created_by, updated_by, created_at, updated_at, version)
SELECT '018e0020-0000-0000-0000-000000000003', '018e0000-0000-0000-0000-000000000001', '018e0001-0000-0000-0000-000000000003',
     true, true, true, true, true, true, 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE id = '018e0020-0000-0000-0000-000000000003');

-- SUPER_ADMIN - Groups (full access)
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import, tenant_id, created_by, updated_by, created_at, updated_at, version)
SELECT '018e0020-0000-0000-0000-000000000004', '018e0000-0000-0000-0000-000000000001', '018e0001-0000-0000-0000-000000000004',
     true, true, true, true, true, true, 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE id = '018e0020-0000-0000-0000-000000000004');

-- SUPER_ADMIN - Organizations (full access)
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import, tenant_id, created_by, updated_by, created_at, updated_at, version)
SELECT '018e0020-0000-0000-0000-000000000005', '018e0000-0000-0000-0000-000000000001', '018e0001-0000-0000-0000-000000000005',
     true, true, true, true, true, true, 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE id = '018e0020-0000-0000-0000-000000000005');

-- SUPER_ADMIN - Settings (full access)
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import, tenant_id, created_by, updated_by, created_at, updated_at, version)
SELECT '018e0020-0000-0000-0000-000000000006', '018e0000-0000-0000-0000-000000000001', '018e0001-0000-0000-0000-000000000006',
     true, true, true, true, true, true, 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE id = '018e0020-0000-0000-0000-000000000006');

-- ADMIN - Dashboard (view only)
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import, tenant_id, created_by, updated_by, created_at, updated_at, version)
SELECT '018e0020-0000-0000-0000-000000000007', '018e0000-0000-0000-0000-000000000002', '018e0001-0000-0000-0000-000000000001',
     true, false, false, false, false, false, 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE id = '018e0020-0000-0000-0000-000000000007');

-- ADMIN - Users (view, create, edit)
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import, tenant_id, created_by, updated_by, created_at, updated_at, version)
SELECT '018e0020-0000-0000-0000-000000000008', '018e0000-0000-0000-0000-000000000002', '018e0001-0000-0000-0000-000000000002',
     true, true, true, false, true, false, 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE id = '018e0020-0000-0000-0000-000000000008');

-- ADMIN - Roles (view only)
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import, tenant_id, created_by, updated_by, created_at, updated_at, version)
SELECT '018e0020-0000-0000-0000-000000000009', '018e0000-0000-0000-0000-000000000002', '018e0001-0000-0000-0000-000000000003',
     true, false, false, false, false, false, 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE id = '018e0020-0000-0000-0000-000000000009');

-- USER - Dashboard (view only)
INSERT INTO role_menus (id, role_id, menu_id, can_view, can_create, can_edit, can_delete, can_export, can_import, tenant_id, created_by, updated_by, created_at, updated_at, version)
SELECT '018e0020-0000-0000-0000-000000000010', '018e0000-0000-0000-0000-000000000004', '018e0001-0000-0000-0000-000000000001',
     true, false, false, false, false, false, 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM role_menus WHERE id = '018e0020-0000-0000-0000-000000000010');
