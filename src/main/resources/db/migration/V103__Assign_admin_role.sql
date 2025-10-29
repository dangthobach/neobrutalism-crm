-- Assign SUPER_ADMIN role to system admin user
-- This migration assigns the default role to the system admin

-- Assign SUPER_ADMIN role to admin user
INSERT INTO user_roles (id, user_id, role_id, is_active, granted_at, granted_by, tenant_id, created_by, updated_by, created_at, updated_at, version)
VALUES ('018e0012-0000-0000-0000-000000000001',
        '018e0011-0000-0000-0000-000000000001',
        '018e0000-0000-0000-0000-000000000001',
        true, CURRENT_TIMESTAMP, 'system', 'default', 'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
