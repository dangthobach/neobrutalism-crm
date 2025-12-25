-- =====================================================
-- Migration V303: Create User Command History Table
--
-- Tracks command executions per user for:
-- - Recent commands list
-- - Usage analytics
-- - Personalized suggestions
--
-- @author Admin
-- @since Phase 1
-- =====================================================

CREATE TABLE IF NOT EXISTS user_command_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    user_id UUID NOT NULL,
    command_id UUID NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time_ms BIGINT NOT NULL,
    context_data TEXT, -- JSON

    CONSTRAINT fk_user_command_history_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_command_history_command
        FOREIGN KEY (command_id) REFERENCES commands(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_command_history_user
ON user_command_history(user_id, executed_at DESC);

CREATE INDEX idx_command_history_tenant_user
ON user_command_history(tenant_id, user_id);

CREATE INDEX idx_command_history_command
ON user_command_history(command_id);

-- Table comments
COMMENT ON TABLE user_command_history IS 'User command execution history for analytics and suggestions';
COMMENT ON COLUMN user_command_history.execution_time_ms IS 'Time taken to execute command (client-side measurement)';
COMMENT ON COLUMN user_command_history.context_data IS 'JSON context where command was executed (e.g., current page, entity)';
