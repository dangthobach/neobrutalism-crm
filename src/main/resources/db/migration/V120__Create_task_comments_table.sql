-- Create task_comments table for task comment system
CREATE TABLE IF NOT EXISTS task_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    parent_id UUID,
    content TEXT NOT NULL CHECK (char_length(content) <= 5000),
    edited BOOLEAN DEFAULT FALSE,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
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

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_task_comments_task_id 
    ON task_comments(task_id) WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_task_comments_user_id 
    ON task_comments(user_id) WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_task_comments_parent_id 
    ON task_comments(parent_id) WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_task_comments_created_at 
    ON task_comments(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_task_comments_organization_id 
    ON task_comments(organization_id) WHERE deleted = FALSE;

-- Composite index for common query patterns
CREATE INDEX IF NOT EXISTS idx_task_comments_task_deleted_created 
    ON task_comments(task_id, deleted, created_at DESC);

-- Comment for documentation
COMMENT ON TABLE task_comments IS 'Stores comments on tasks with support for threaded replies';
COMMENT ON COLUMN task_comments.parent_id IS 'References parent comment for threaded replies (NULL for top-level comments)';
COMMENT ON COLUMN task_comments.edited IS 'Indicates if comment has been edited after creation';
COMMENT ON COLUMN task_comments.deleted IS 'Soft delete flag to preserve comment history';
