-- Seed default system roles
-- This migration creates the foundational roles for the system
-- NOTE: Must run after V100 which creates the default organization

-- Insert default roles (using organization from V100)
INSERT INTO roles (id, code, name, description, organization_id, status, is_system, priority, tenant_id, created_by, updated_by, created_at, updated_at, deleted, version)
VALUES
    -- Super Admin - Full system access
    ('018e0000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Super Administrator',
     'Full system access with all permissions', '018e0010-0000-0000-0000-000000000001',
     'ACTIVE', true, 100, 'default',
     'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0),

    -- Admin - Administrative access
    ('018e0000-0000-0000-0000-000000000002', 'ADMIN', 'Administrator',
     'Administrative access to manage users and settings', '018e0010-0000-0000-0000-000000000001',
     'ACTIVE', true, 80, 'default',
     'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0),

    -- Manager - Management access
    ('018e0000-0000-0000-0000-000000000003', 'MANAGER', 'Manager',
     'Manager access with view and edit permissions', '018e0010-0000-0000-0000-000000000001',
     'ACTIVE', true, 60, 'default',
     'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0),

    -- User - Regular user access
    ('018e0000-0000-0000-0000-000000000004', 'USER', 'Regular User',
     'Standard user access with basic permissions', '018e0010-0000-0000-0000-000000000001',
     'ACTIVE', true, 40, 'default',
     'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0),

    -- Guest - Read-only access
    ('018e0000-0000-0000-0000-000000000005', 'GUEST', 'Guest',
     'Guest access with read-only permissions', '018e0010-0000-0000-0000-000000000001',
     'ACTIVE', true, 20, 'default',
     'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0);
