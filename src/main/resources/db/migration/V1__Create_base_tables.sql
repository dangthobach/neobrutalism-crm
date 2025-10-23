-- Create base tables for the CRM system
-- This migration creates all the core tables needed for the application

-- Organizations table
CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(1000),
    email VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(500),
    website VARCHAR(200),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    status_changed_at TIMESTAMP,
    status_changed_by VARCHAR(100),
    status_reason VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    deleted_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100)
);

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
