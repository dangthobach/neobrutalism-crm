-- Seed system admin user
-- Default password is "Admin@123" (BCrypt encrypted)
-- IMPORTANT: Change this password after first login in production!

-- Insert default organization
INSERT INTO organizations (id, name, code, description, email, status, tenant_id, created_by, updated_by, created_at, updated_at, deleted, version)
VALUES ('018e0010-0000-0000-0000-000000000001', 'System Organization', 'SYSTEM_ORG',
        'Default system organization', 'admin@system.com', 'ACTIVE', 'default',
        'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0);

-- Insert system admin user
-- Password: Admin@123 (BCrypt hash)
INSERT INTO users (id, username, email, password_hash, first_name, last_name, organization_id, status, tenant_id, created_by, updated_by, created_at, updated_at, deleted, version, failed_login_attempts)
VALUES ('018e0011-0000-0000-0000-000000000001', 'admin', 'admin@system.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'System', 'Administrator', '018e0010-0000-0000-0000-000000000001', 'ACTIVE', 'default',
        'system', 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false, 0, 0);
