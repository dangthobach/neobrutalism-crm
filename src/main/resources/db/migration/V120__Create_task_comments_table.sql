-- Create task_comments table for task comment system
CREATE TABLE IF NOT EXISTS task_comments (
    id UUID PRIMARY KEY DEFAULT RANDOM_UUID(),
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    parent_id UUID,
    content TEXT NOT NULL CHECK (char_length(content) <= 5000),
    edited BOOLEAN DEFAULT FALSE,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0,
    organization_id UUID NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_task_comment_task FOREIGN KEY (task_id) 
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_comment_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_comment_parent FOREIGN KEY (parent_id) 
        REFERENCES task_comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_comment_organization FOREIGN KEY (organization_id) 
        REFERENCES organizations(id) ON DELETE CASCADE
);

-- Indexes for performance (H2 compatible - no WHERE clauses)
CREATE INDEX IF NOT EXISTS idx_task_comments_task_id 
    ON task_comments(task_id);

CREATE INDEX IF NOT EXISTS idx_task_comments_user_id 
    ON task_comments(user_id);

CREATE INDEX IF NOT EXISTS idx_task_comments_parent_id 
    ON task_comments(parent_id);

CREATE INDEX IF NOT EXISTS idx_task_comments_created_at 
    ON task_comments(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_task_comments_organization_id 
    ON task_comments(organization_id);

-- Composite index for common query patterns
CREATE INDEX IF NOT EXISTS idx_task_comments_task_deleted_created 
    ON task_comments(task_id, deleted, created_at DESC);

-- Comments: H2 doesn't support COMMENT ON statements
-- See code documentation for table/column descriptions
