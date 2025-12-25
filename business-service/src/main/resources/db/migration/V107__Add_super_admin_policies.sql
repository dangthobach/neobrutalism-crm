-- V107: Add Casbin policies for ROLE_SUPER_ADMIN
-- ROLE_SUPER_ADMIN has full access to all APIs and resources

-- Remove any existing SUPER_ADMIN policies to avoid duplicates
DELETE FROM casbin_rule WHERE ptype = 'p' AND v0 = 'ROLE_SUPER_ADMIN';

-- Grant SUPER_ADMIN full access to all API endpoints
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3, v4) VALUES
    -- Full access to all APIs with any HTTP method
    ('p', 'ROLE_SUPER_ADMIN', 'default', '/api/.*', '(GET)|(POST)|(PUT)|(DELETE)|(PATCH)', 'allow'),

    -- Explicit access to admin endpoints
    ('p', 'ROLE_SUPER_ADMIN', 'default', '/api/admin/.*', '(GET)|(POST)|(PUT)|(DELETE)|(PATCH)', 'allow'),

    -- Access to specific resource endpoints
    ('p', 'ROLE_SUPER_ADMIN', 'default', '/api/users.*', '(GET)|(POST)|(PUT)|(DELETE)|(PATCH)', 'allow'),
    ('p', 'ROLE_SUPER_ADMIN', 'default', '/api/roles.*', '(GET)|(POST)|(PUT)|(DELETE)|(PATCH)', 'allow'),
    ('p', 'ROLE_SUPER_ADMIN', 'default', '/api/groups.*', '(GET)|(POST)|(PUT)|(DELETE)|(PATCH)', 'allow'),
    ('p', 'ROLE_SUPER_ADMIN', 'default', '/api/menus.*', '(GET)|(POST)|(PUT)|(DELETE)|(PATCH)', 'allow'),
    ('p', 'ROLE_SUPER_ADMIN', 'default', '/api/organizations.*', '(GET)|(POST)|(PUT)|(DELETE)|(PATCH)', 'allow'),
    ('p', 'ROLE_SUPER_ADMIN', 'default', '/api/branches.*', '(GET)|(POST)|(PUT)|(DELETE)|(PATCH)', 'allow'),
    ('p', 'ROLE_SUPER_ADMIN', 'default', '/api/permissions.*', '(GET)|(POST)|(PUT)|(DELETE)|(PATCH)', 'allow');

-- Verify policies were inserted
-- SELECT * FROM casbin_rule WHERE v0 = 'ROLE_SUPER_ADMIN';
