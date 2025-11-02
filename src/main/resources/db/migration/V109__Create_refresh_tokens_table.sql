-- =====================================================
-- Migration V109: Create Refresh Tokens Table
-- Description: Add refresh tokens for token rotation
-- Author: System
-- Date: 2025-11-01
-- =====================================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    replaced_by_token VARCHAR(500),
    created_by_ip VARCHAR(50),
    revoked_by_ip VARCHAR(50),
    user_agent VARCHAR(500),

    -- Auditing fields
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100)
);

-- Create indexes
CREATE INDEX idx_refresh_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_token_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_token_revoked ON refresh_tokens(revoked);
