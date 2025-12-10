-- =====================================================
-- Migration V302: Create Commands Table
--
-- Stores command palette commands with:
-- - Keyboard shortcuts
-- - Permission requirements
-- - Usage statistics
-- - Search metadata
--
-- @author Admin
-- @since Phase 1
-- =====================================================

CREATE TABLE IF NOT EXISTS commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    command_id VARCHAR(100) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL CHECK (category IN (
        'CUSTOMER', 'CONTACT', 'TASK', 'ACTIVITY', 'USER',
        'ORGANIZATION', 'REPORT', 'SETTINGS', 'NAVIGATION', 'SEARCH'
    )),
    icon VARCHAR(100),
    shortcut_key VARCHAR(50),
    action_type VARCHAR(50) NOT NULL,
    action_payload TEXT, -- JSON
    required_permission VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    execution_count BIGINT NOT NULL DEFAULT 0,
    avg_execution_time_ms BIGINT,
    search_keywords TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

-- Indexes
CREATE INDEX idx_command_tenant_category
ON commands(tenant_id, category);

CREATE INDEX idx_command_shortcut
ON commands(shortcut_key);

CREATE INDEX idx_command_active
ON commands(is_active);

CREATE INDEX idx_command_execution_count
ON commands(execution_count DESC);

-- Full-text search index
CREATE INDEX idx_command_search
ON commands USING GIN (to_tsvector('english', label || ' ' || COALESCE(description, '') || ' ' || COALESCE(search_keywords, '')));

-- Table comments
COMMENT ON TABLE commands IS 'Command palette commands with shortcuts and permissions';
COMMENT ON COLUMN commands.command_id IS 'Unique command identifier (e.g., customer.create)';
COMMENT ON COLUMN commands.action_type IS 'Action type: NAVIGATION, API_CALL, MODAL, EXTERNAL';
COMMENT ON COLUMN commands.action_payload IS 'JSON payload for action execution';
COMMENT ON COLUMN commands.execution_count IS 'Total times command has been executed';
COMMENT ON COLUMN commands.avg_execution_time_ms IS 'Average execution time in milliseconds';
