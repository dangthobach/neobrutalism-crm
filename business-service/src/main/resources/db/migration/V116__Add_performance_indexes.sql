-- =============================================
-- V116: Add Performance Optimization Indexes
-- PHASE 1: CRITICAL FIXES - Database Optimization
-- =============================================
-- Purpose: Add composite indexes to improve query performance
-- Expected impact: 10-100x faster queries, reduced database load
-- No breaking changes - transparent to application layer
-- =============================================

-- =============================================
-- BRANCH INDEXES
-- =============================================

-- Composite index for tenant-based queries (most common)
CREATE INDEX IF NOT EXISTS idx_branch_tenant 
ON branches(tenant_id) 
WHERE deleted = false;

-- Index for status filtering
CREATE INDEX IF NOT EXISTS idx_branch_status 
ON branches(status) 
WHERE deleted = false;

-- Index for manager lookups
CREATE INDEX IF NOT EXISTS idx_branch_manager 
ON branches(manager_id) 
WHERE deleted = false;

-- Composite index for tenant + organization + deleted (covers most queries)
CREATE INDEX IF NOT EXISTS idx_branch_tenant_org_deleted 
ON branches(tenant_id, organization_id, deleted);

-- Composite index for organization + status filtering
CREATE INDEX IF NOT EXISTS idx_branch_org_status 
ON branches(organization_id, status, deleted);

-- =============================================
-- CUSTOMER INDEXES
-- =============================================

-- Composite index for tenant + status filtering (very common)
CREATE INDEX IF NOT EXISTS idx_customer_tenant_status_deleted 
ON customers(tenant_id, status, deleted);

-- Composite index for tenant + type filtering
CREATE INDEX IF NOT EXISTS idx_customer_tenant_type_deleted 
ON customers(tenant_id, customer_type, deleted);

-- Composite index for VIP customer queries
CREATE INDEX IF NOT EXISTS idx_customer_tenant_vip_deleted 
ON customers(tenant_id, is_vip, deleted);

-- Composite index for organization + branch queries
CREATE INDEX IF NOT EXISTS idx_customer_org_branch 
ON customers(organization_id, branch_id, deleted);

-- Index for date range queries
CREATE INDEX IF NOT EXISTS idx_customer_acquisition_date 
ON customers(acquisition_date) 
WHERE deleted = false;

-- Index for last contact date (for follow-up queries)
CREATE INDEX IF NOT EXISTS idx_customer_last_contact 
ON customers(last_contact_date) 
WHERE deleted = false;

-- Index for company name searches (case-insensitive supported by PostgreSQL)
CREATE INDEX IF NOT EXISTS idx_customer_company_name 
ON customers(company_name) 
WHERE deleted = false;

-- =============================================
-- USER INDEXES
-- =============================================

-- Index for tenant-based user queries
CREATE INDEX IF NOT EXISTS idx_user_tenant 
ON users(tenant_id) 
WHERE deleted = false;

-- Composite index for tenant + organization queries
CREATE INDEX IF NOT EXISTS idx_user_tenant_org_deleted 
ON users(tenant_id, organization_id, deleted);

-- Composite index for tenant + status filtering
CREATE INDEX IF NOT EXISTS idx_user_tenant_status_deleted 
ON users(tenant_id, status, deleted);

-- Index for branch-based user queries
CREATE INDEX IF NOT EXISTS idx_user_branch 
ON users(branch_id) 
WHERE deleted = false;

-- Index for last login analytics
CREATE INDEX IF NOT EXISTS idx_user_last_login 
ON users(last_login_at) 
WHERE deleted = false;

-- =============================================
-- VERIFICATION QUERIES
-- =============================================
-- Run these to verify indexes are created:
-- SELECT indexname, tablename FROM pg_indexes WHERE tablename IN ('branches', 'customers', 'users') ORDER BY tablename, indexname;
-- 
-- To analyze query performance:
-- EXPLAIN ANALYZE SELECT * FROM customers WHERE tenant_id = 'xxx' AND status = 'ACTIVE' AND deleted = false;
-- =============================================
