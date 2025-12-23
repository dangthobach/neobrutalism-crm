-- =====================================================
-- Migration V304: Create User Favorite Commands Table
--
-- Stores user's favorite commands for quick access.
--
-- @author Admin
-- @since Phase 1
-- =====================================================

CREATE TABLE IF NOT EXISTS user_favorite_commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    user_id UUID NOT NULL,
    command_id UUID NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_command
        UNIQUE (user_id, command_id),
    CONSTRAINT fk_user_favorite_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_favorite_command
        FOREIGN KEY (command_id) REFERENCES commands(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_favorite_user
ON user_favorite_commands(user_id, sort_order);

CREATE INDEX idx_favorite_tenant
ON user_favorite_commands(tenant_id);

-- Table comments
COMMENT ON TABLE user_favorite_commands IS 'User favorite commands for quick access';
COMMENT ON COLUMN user_favorite_commands.sort_order IS 'Display order in favorites list';
