-- Seed system admin user
-- Default password is "Admin@123" (BCrypt encrypted)
-- IMPORTANT: Change this password after first login in production!

-- Insert default organization (idempotent - only if not exists)
INSERT INTO organizations (id, name, code, organization_type, description, email, status, tenant_id, created_by, updated_by, created_at, updated_at, deleted, version)
SELECT '018e0010-0000-0000-0000-000000000001', 'System Organization', 'SYSTEM_ORG', 'CORPORATE',
        'Default system organization', 'admin@system.com', 'ACTIVE', 'default',
        'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE id = '018e0010-0000-0000-0000-000000000001');

-- Insert system admin user (idempotent - only if not exists)
-- Password: Admin@123 (BCrypt hash)
INSERT INTO users (id, username, email, password_hash, first_name, last_name, organization_id, status, tenant_id, created_by, updated_by, created_at, updated_at, deleted, version, failed_login_attempts)
SELECT '018e0011-0000-0000-0000-000000000001', 'admin', 'admin@system.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'System', 'Administrator', '018e0010-0000-0000-0000-000000000001', 'ACTIVE', 'default',
        'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
