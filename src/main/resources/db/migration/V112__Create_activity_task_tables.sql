-- V112: Create Activity and Task tables

-- Activities table
CREATE TABLE activities (
    id UUID PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    description TEXT,
    activity_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) DEFAULT 'MEDIUM',

    -- Ownership
    owner_id UUID NOT NULL,

    -- Related entity (polymorphic)
    related_to_type VARCHAR(50),
    related_to_id UUID,

    -- Scheduling
    scheduled_start_at TIMESTAMP,
    scheduled_end_at TIMESTAMP,
    actual_start_at TIMESTAMP,
    actual_end_at TIMESTAMP,
    duration_minutes INTEGER,
    location VARCHAR(255),

    -- Outcome
    outcome TEXT,
    next_steps TEXT,

    -- Recurrence
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_pattern VARCHAR(100),
    parent_activity_id UUID,

    -- Organization context
    organization_id UUID,
    branch_id UUID,

    -- Status tracking (from StatefulEntity)
    status_changed_at TIMESTAMP,
    status_changed_by VARCHAR(100),
    status_reason VARCHAR(500),

    -- Soft delete (from SoftDeletableEntity)
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),

    -- Audit fields (from AuditableEntity)
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),

    -- Multi-tenancy (from TenantAwareEntity)
    tenant_id VARCHAR(50) NOT NULL,

    -- Optimistic locking (from BaseEntity)
    version BIGINT DEFAULT 0,

    -- Foreign key constraints
    CONSTRAINT fk_activity_parent FOREIGN KEY (parent_activity_id)
        REFERENCES activities(id) ON DELETE SET NULL
);

-- Tasks table
CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) DEFAULT 'MEDIUM',

    -- Assignment
    assigned_to_id UUID,
    assigned_by_id UUID,

    -- Related entity (polymorphic)
    related_to_type VARCHAR(50),
    related_to_id UUID,

    -- Scheduling
    due_date TIMESTAMP,
    completed_at TIMESTAMP,
    estimated_hours INTEGER,
    actual_hours INTEGER,

    -- Progress
    progress_percentage INTEGER DEFAULT 0 CHECK (progress_percentage >= 0 AND progress_percentage <= 100),

    -- Checklist (JSONB for PostgreSQL, TEXT for H2)
    checklist TEXT,

    -- Organization context
    organization_id UUID,
    branch_id UUID,

    -- Status tracking (from StatefulEntity)
    status_changed_at TIMESTAMP,
    status_changed_by VARCHAR(100),
    status_reason VARCHAR(500),

    -- Soft delete (from SoftDeletableEntity)
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),

    -- Audit fields (from AuditableEntity)
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),

    -- Multi-tenancy (from TenantAwareEntity)
    tenant_id VARCHAR(50) NOT NULL,

    -- Optimistic locking (from BaseEntity)
    version BIGINT DEFAULT 0
);

-- Indexes for activities table
-- Note: H2 does not support partial indexes (WHERE clause), so we create full indexes
CREATE INDEX idx_activity_owner ON activities(owner_id);
CREATE INDEX idx_activity_tenant ON activities(tenant_id);
CREATE INDEX idx_activity_related ON activities(related_to_type, related_to_id);
CREATE INDEX idx_activity_scheduled_start ON activities(scheduled_start_at);
CREATE INDEX idx_activity_scheduled_end ON activities(scheduled_end_at);
CREATE INDEX idx_activity_status ON activities(status);
CREATE INDEX idx_activity_type ON activities(activity_type);
CREATE INDEX idx_activity_organization ON activities(organization_id);
CREATE INDEX idx_activity_branch ON activities(branch_id);
CREATE INDEX idx_activity_priority ON activities(priority);
CREATE INDEX idx_activity_deleted ON activities(deleted);

-- Indexes for tasks table
CREATE INDEX idx_task_assigned_to ON tasks(assigned_to_id);
CREATE INDEX idx_task_assigned_by ON tasks(assigned_by_id);
CREATE INDEX idx_task_tenant ON tasks(tenant_id);
CREATE INDEX idx_task_related ON tasks(related_to_type, related_to_id);
CREATE INDEX idx_task_due_date ON tasks(due_date);
CREATE INDEX idx_task_status ON tasks(status);
CREATE INDEX idx_task_organization ON tasks(organization_id);
CREATE INDEX idx_task_branch ON tasks(branch_id);
CREATE INDEX idx_task_priority ON tasks(priority);
CREATE INDEX idx_task_deleted ON tasks(deleted);

-- Comments for documentation
COMMENT ON TABLE activities IS 'CRM activities including calls, meetings, emails, demos, etc.';
COMMENT ON TABLE tasks IS 'Task management for CRM with assignment and progress tracking';

COMMENT ON COLUMN activities.activity_type IS 'Type: CALL, MEETING, EMAIL, TASK, NOTE, DEMO, PRESENTATION, TRAINING, SITE_VISIT, FOLLOW_UP';
COMMENT ON COLUMN activities.status IS 'Status: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED, RESCHEDULED';
COMMENT ON COLUMN activities.priority IS 'Priority: LOW, MEDIUM, HIGH, URGENT';
COMMENT ON COLUMN activities.related_to_type IS 'Related entity type: CUSTOMER, CONTACT, OPPORTUNITY';
COMMENT ON COLUMN activities.is_recurring IS 'Whether this is a recurring activity';
COMMENT ON COLUMN activities.recurrence_pattern IS 'Recurrence pattern: DAILY, WEEKLY, MONTHLY';
COMMENT ON COLUMN activities.parent_activity_id IS 'Parent activity for recurring instances';

COMMENT ON COLUMN tasks.status IS 'Status: TODO, IN_PROGRESS, IN_REVIEW, DONE, CANCELLED';
COMMENT ON COLUMN tasks.priority IS 'Priority: LOW, MEDIUM, HIGH, URGENT';
COMMENT ON COLUMN tasks.related_to_type IS 'Related entity type: CUSTOMER, CONTACT, OPPORTUNITY, ACTIVITY';
COMMENT ON COLUMN tasks.progress_percentage IS 'Task completion progress: 0-100';
COMMENT ON COLUMN tasks.checklist IS 'JSON array of checklist items with description and completed status';
