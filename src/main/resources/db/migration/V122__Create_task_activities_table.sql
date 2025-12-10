-- Create task_activities table for activity timeline/audit log
CREATE TABLE IF NOT EXISTS task_activities (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    task_id UUID NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    user_id UUID NOT NULL,
    username VARCHAR(255),
    metadata TEXT,
    organization_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    
    -- Foreign key constraints
    CONSTRAINT fk_task_activity_task FOREIGN KEY (task_id) 
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_activity_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_activity_organization FOREIGN KEY (organization_id) 
        REFERENCES organizations(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_task_activity_task_id 
    ON task_activities(task_id);

CREATE INDEX IF NOT EXISTS idx_task_activity_user_id 
    ON task_activities(user_id);

CREATE INDEX IF NOT EXISTS idx_task_activity_created_at 
    ON task_activities(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_task_activity_type 
    ON task_activities(activity_type);

CREATE INDEX IF NOT EXISTS idx_task_activity_organization_id 
    ON task_activities(organization_id);

-- Composite index for common query patterns
CREATE INDEX IF NOT EXISTS idx_task_activity_task_created 
    ON task_activities(task_id, created_at DESC);

-- Comments: H2 doesn't support COMMENT ON statements
-- See code documentation for table/column descriptions
