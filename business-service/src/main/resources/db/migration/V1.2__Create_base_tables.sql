-- Create Event Sourcing and CQRS infrastructure tables
-- These tables support event sourcing, outbox pattern, and read models

-- Event store table for Event Sourcing
CREATE TABLE event_store (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data TEXT NOT NULL,
    event_metadata TEXT,
    version BIGINT NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id VARCHAR(50) NOT NULL
);

-- Outbox events table for Transactional Outbox Pattern
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    occurred_at TIMESTAMP NOT NULL,
    occurred_by VARCHAR(100),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    last_error TEXT,
    max_retries INTEGER DEFAULT 5,
    tenant_id VARCHAR(50) NOT NULL
);

-- Organization read model table for CQRS
CREATE TABLE organization_read_model (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(1000),
    email VARCHAR(100),
    phone VARCHAR(20),
    website VARCHAR(200),
    status VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL,
    is_deleted BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    search_text VARCHAR(2000),
    has_contact_info BOOLEAN NOT NULL,
    days_since_created INTEGER NOT NULL,
    tenant_id VARCHAR(50) NOT NULL
);

-- Audit log table
CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(100),
    reason VARCHAR(500)
);

-- State transition history table
CREATE TABLE state_transitions (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    changed_by VARCHAR(100),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id VARCHAR(50) NOT NULL
);

-- =====================================================
-- INDEXES FOR EVENT SOURCING TABLES
-- =====================================================

-- Event store indexes
CREATE INDEX idx_event_store_aggregate_id ON event_store(aggregate_id);
CREATE INDEX idx_event_store_aggregate_type ON event_store(aggregate_type);
CREATE INDEX idx_event_store_occurred_at ON event_store(occurred_at);
CREATE INDEX idx_event_store_tenant_id ON event_store(tenant_id);
CREATE INDEX idx_event_store_aggregate_version ON event_store(aggregate_id, version);

-- Outbox events indexes
CREATE INDEX idx_outbox_published ON outbox_events(published, occurred_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_event_type ON outbox_events(event_type);
CREATE INDEX idx_outbox_retry ON outbox_events(published, retry_count, next_retry_at);
CREATE INDEX idx_outbox_tenant_id ON outbox_events(tenant_id);

-- Organization read model indexes
CREATE INDEX idx_org_rm_name ON organization_read_model(name);
CREATE INDEX idx_org_rm_code ON organization_read_model(code);
CREATE INDEX idx_org_rm_status ON organization_read_model(status);
CREATE INDEX idx_org_rm_active ON organization_read_model(is_active);
CREATE INDEX idx_org_rm_created ON organization_read_model(created_at);
CREATE INDEX idx_org_rm_search ON organization_read_model(search_text);
CREATE INDEX idx_org_rm_tenant_id ON organization_read_model(tenant_id);

-- Audit log indexes
CREATE INDEX idx_audit_entity_type_id ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_changed_at ON audit_log(changed_at);

-- State transitions indexes
CREATE INDEX idx_state_entity_type_id ON state_transitions(entity_type, entity_id);
CREATE INDEX idx_state_changed_at ON state_transitions(changed_at);
CREATE INDEX idx_state_tenant_id ON state_transitions(tenant_id);
