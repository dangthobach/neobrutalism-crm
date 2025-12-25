-- Create checklist_items table for task checklist functionality
CREATE TABLE IF NOT EXISTS checklist_items (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    task_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    position INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    organization_id UUID NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_checklist_item_task FOREIGN KEY (task_id) 
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_item_organization FOREIGN KEY (organization_id) 
        REFERENCES organizations(id) ON DELETE CASCADE,
    
    -- Constraint to ensure position is non-negative
    CONSTRAINT chk_checklist_item_position CHECK (position >= 0)
);

-- Indexes for performance (H2 compatible - no WHERE clauses)
CREATE INDEX IF NOT EXISTS idx_checklist_items_task_id 
    ON checklist_items(task_id);

CREATE INDEX IF NOT EXISTS idx_checklist_items_task_position 
    ON checklist_items(task_id, position);

CREATE INDEX IF NOT EXISTS idx_checklist_items_organization_id 
    ON checklist_items(organization_id);

CREATE INDEX IF NOT EXISTS idx_checklist_items_completed 
    ON checklist_items(task_id, completed);

-- Unique constraint to prevent duplicate positions within a task
CREATE UNIQUE INDEX IF NOT EXISTS idx_checklist_items_task_position_unique 
    ON checklist_items(task_id, position);

-- Comments: H2 doesn't support COMMENT ON statements
-- See code documentation for table/column descriptions
