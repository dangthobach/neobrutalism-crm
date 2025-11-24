-- V119__Create_audit_logs_table.sql
-- Create audit logs table for comprehensive system auditing

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Multi-tenancy
    tenant_id UUID NOT NULL,
    
    -- Entity information
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    
    -- Action and user
    action VARCHAR(50) NOT NULL,
    user_id UUID,
    username VARCHAR(255),
    
    -- Description
    description VARCHAR(500),
    
    -- Change tracking (JSONB for flexible storage)
    changes JSONB,           -- Field-level changes: {"field": {"old": value, "new": value}}
    old_values JSONB,        -- Full entity snapshot before change
    new_values JSONB,        -- Full entity snapshot after change
    
    -- Request context
    request_params JSONB,    -- Method parameters
    ip_address VARCHAR(50),
    user_agent TEXT,
    method_name VARCHAR(200),
    
    -- Execution metadata
    execution_time_ms BIGINT,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    
    -- Timestamp
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for audit log queries

-- Primary lookup: tenant + entity
CREATE INDEX idx_audit_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_tenant_entity ON audit_logs(tenant_id, entity_type, entity_id);

-- User activity tracking
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_tenant_user_date ON audit_logs(tenant_id, user_id, created_at);

-- Action filtering
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_tenant_action ON audit_logs(tenant_id, action);

-- Date range queries
CREATE INDEX idx_audit_date ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_tenant_date ON audit_logs(tenant_id, created_at DESC);

-- Failed operation tracking
CREATE INDEX idx_audit_failed ON audit_logs(success, tenant_id, created_at) WHERE success = FALSE;

-- Entity type queries
CREATE INDEX idx_audit_entity_type ON audit_logs(entity_type, created_at DESC);

-- GIN index for JSONB search (optional - for advanced queries)
CREATE INDEX idx_audit_changes_gin ON audit_logs USING GIN (changes);
CREATE INDEX idx_audit_new_values_gin ON audit_logs USING GIN (new_values);

-- Comments
COMMENT ON TABLE audit_logs IS 'Comprehensive audit log for all system operations';
COMMENT ON COLUMN audit_logs.tenant_id IS 'Tenant ID for multi-tenancy isolation';
COMMENT ON COLUMN audit_logs.entity_type IS 'Entity type (e.g., Customer, Contract, User)';
COMMENT ON COLUMN audit_logs.entity_id IS 'ID of the entity that was modified';
COMMENT ON COLUMN audit_logs.action IS 'Action performed (CREATE, UPDATE, DELETE, etc.)';
COMMENT ON COLUMN audit_logs.changes IS 'Field-level changes in JSON format';
COMMENT ON COLUMN audit_logs.old_values IS 'Full entity state before change (optional)';
COMMENT ON COLUMN audit_logs.new_values IS 'Full entity state after change (optional)';
COMMENT ON COLUMN audit_logs.execution_time_ms IS 'Method execution time in milliseconds';
COMMENT ON COLUMN audit_logs.success IS 'Whether the operation was successful';

-- Partition by month for better performance (optional - for large datasets)
-- Uncomment if you expect millions of audit logs
-- CREATE TABLE audit_logs_y2025m01 PARTITION OF audit_logs
--     FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
