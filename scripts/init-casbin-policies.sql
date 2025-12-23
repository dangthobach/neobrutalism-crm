-- Initial jCasbin Policies Import Script
-- Run this after IAM service is up and database is initialized
-- This script imports default roles and permissions for the CRM system

-- Connect to iam_db
\c iam_db;

-- Clear existing policies (for fresh import)
-- TRUNCATE casbin_rule;

-- ====================
-- System-wide Roles (domain = *)
-- ====================

-- Super Admin: Full access to all resources in all tenants
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
('p', 'role:super_admin', '*', '/api/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:super_admin', '*', '/actuator/**', 'GET');

-- ====================
-- Tenant-specific Roles
-- ====================

-- Tenant Admin: Full access to all resources in their tenant
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
-- Customers
('p', 'role:tenant_admin', '*', '/api/customers/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/contacts/**', '(GET|POST|PUT|DELETE)'),
-- Content Management System (CMS)
('p', 'role:tenant_admin', '*', '/api/content/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/categories/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/tags/**', '(GET|POST|PUT|DELETE)'),
-- Learning Management System (LMS)
('p', 'role:tenant_admin', '*', '/api/courses/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/modules/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/lessons/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/enrollments/**', '(GET|POST|PUT|DELETE)'),
-- User Management
('p', 'role:tenant_admin', '*', '/api/users/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:tenant_admin', '*', '/api/v1/permissions/**', '(GET|POST|PUT|DELETE)');

-- Content Manager: Full CMS access
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
('p', 'role:content_manager', '*', '/api/content/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:content_manager', '*', '/api/categories/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:content_manager', '*', '/api/tags/**', '(GET|POST|PUT|DELETE)'),
-- Read-only for other areas
('p', 'role:content_manager', '*', '/api/customers/**', 'GET'),
('p', 'role:content_manager', '*', '/api/users/**', 'GET');

-- Instructor: Full LMS access
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
('p', 'role:instructor', '*', '/api/courses/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:instructor', '*', '/api/modules/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:instructor', '*', '/api/lessons/**', '(GET|POST|PUT|DELETE)'),
('p', 'role:instructor', '*', '/api/enrollments/**', 'GET'),
('p', 'role:instructor', '*', '/api/progress/**', 'GET'),
-- Read-only for content
('p', 'role:instructor', '*', '/api/content/**', 'GET');

-- Student: Course enrollment and progress tracking
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
('p', 'role:student', '*', '/api/courses/**', 'GET'),
('p', 'role:student', '*', '/api/modules/**', 'GET'),
('p', 'role:student', '*', '/api/lessons/**', 'GET'),
('p', 'role:student', '*', '/api/enrollments/my/**', '(GET|POST)'),
('p', 'role:student', '*', '/api/progress/**', '(GET|POST)'),
('p', 'role:student', '*', '/api/certificates/my/**', 'GET'),
-- Read-only for content
('p', 'role:student', '*', '/api/content/**', 'GET');

-- Sales Representative: Customer and contact management
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
('p', 'role:sales_rep', '*', '/api/customers/**', '(GET|POST|PUT)'),
('p', 'role:sales_rep', '*', '/api/contacts/**', '(GET|POST|PUT)'),
('p', 'role:sales_rep', '*', '/api/users/**', 'GET');

-- Regular User: Read-only access
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
('p', 'role:user', '*', '/api/customers/**', 'GET'),
('p', 'role:user', '*', '/api/contacts/**', 'GET'),
('p', 'role:user', '*', '/api/content/**', 'GET'),
('p', 'role:user', '*', '/api/courses/**', 'GET');

-- ====================
-- User-Role Assignments (Examples)
-- ====================

-- Map Keycloak users to Casbin roles
-- Format: (ptype='g', v0=userId, v1=roleId, v2=tenantId)

-- System Admin
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES
('g', 'admin@crm.local', 'role:super_admin', 'system');

-- Tenant1 Admin
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES
('g', 'tenant1-admin@crm.local', 'role:tenant_admin', 'tenant1');

-- Tenant1 Users
INSERT INTO casbin_rule (ptype, v0, v1, v2) VALUES
('g', 'user1@tenant1.com', 'role:user', 'tenant1'),
('g', 'instructor1@tenant1.com', 'role:instructor', 'tenant1'),
('g', 'student1@tenant1.com', 'role:student', 'tenant1');

-- ====================
-- Advanced Permissions (ABAC - Attribute-Based)
-- ====================

-- Resource-specific permissions
-- Users can only edit their own profile
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
('p', 'role:user', '*', '/api/users/me', '(GET|PUT)'),
('p', 'role:user', '*', '/api/users/me/password', 'PUT');

-- Students can only view their own enrollments and progress
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
('p', 'role:student', '*', '/api/enrollments/my', 'GET'),
('p', 'role:student', '*', '/api/progress/my', 'GET');

-- Instructors can view enrollments for their courses
INSERT INTO casbin_rule (ptype, v0, v1, v2, v3) VALUES
('p', 'role:instructor', '*', '/api/courses/my/enrollments', 'GET'),
('p', 'role:instructor', '*', '/api/courses/my/students', 'GET');

-- ====================
-- Refresh Materialized View
-- ====================

-- Refresh the materialized view for fast permission lookups
REFRESH MATERIALIZED VIEW CONCURRENTLY user_permissions_mv;

-- ====================
-- Audit Log Entry
-- ====================

INSERT INTO permission_audit_log (action, entity_type, user_id, tenant_id, new_value) VALUES
('INIT', 'SYSTEM', 'system', 'system',
 '{"message": "Initial policies imported", "policies_count": (SELECT COUNT(*) FROM casbin_rule WHERE ptype = ''p''), "roles_count": (SELECT COUNT(DISTINCT v0) FROM casbin_rule WHERE ptype = ''g'')}');

-- ====================
-- Verification Queries
-- ====================

-- Count policies
SELECT ptype, COUNT(*) as count FROM casbin_rule GROUP BY ptype;

-- Show all role assignments
SELECT v0 as user_id, v1 as role_id, v2 as tenant_id
FROM casbin_rule
WHERE ptype = 'g'
ORDER BY v2, v0;

-- Show all permissions for a role
SELECT v0 as role, v1 as domain, v2 as resource, v3 as action
FROM casbin_rule
WHERE ptype = 'p' AND v0 = 'role:tenant_admin';

-- Show user permissions through materialized view
SELECT user_id, tenant_id, resource, action
FROM user_permissions_mv
WHERE user_id = 'tenant1-admin@crm.local'
LIMIT 10;

-- ====================
-- Success Message
-- ====================

DO $$
BEGIN
  RAISE NOTICE 'jCasbin policies imported successfully!';
  RAISE NOTICE 'Total policies: %', (SELECT COUNT(*) FROM casbin_rule WHERE ptype = 'p');
  RAISE NOTICE 'Total role assignments: %', (SELECT COUNT(*) FROM casbin_rule WHERE ptype = 'g');
END $$;
