-- Create indexes for better query performance

-- Organizations indexes
CREATE INDEX idx_org_tenant_id ON organizations(tenant_id);
CREATE INDEX idx_org_name ON organizations(name);
CREATE INDEX idx_org_code ON organizations(code);
CREATE INDEX idx_org_status ON organizations(status);
CREATE INDEX idx_org_deleted_id ON organizations(deleted, id);
CREATE INDEX idx_org_deleted_created_at ON organizations(deleted, created_at);
CREATE INDEX idx_org_deleted_status ON organizations(deleted, status);
CREATE INDEX idx_org_tenant_status ON organizations(tenant_id, status);

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
