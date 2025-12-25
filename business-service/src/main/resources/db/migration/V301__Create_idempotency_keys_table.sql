-- =====================================================
-- Migration V301: Create Idempotency Keys Table
--
-- Stores idempotency keys for exactly-once operation execution.
--
-- Features:
-- - 24-hour TTL via expires_at column
-- - Request hash for deduplication
-- - Response caching for completed operations
-- - Multi-tenant support
--
-- @author Admin
-- @since Phase 1
-- =====================================================

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    operation_type VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL, -- SHA-256 hash
    status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    response_body TEXT,
    http_status_code INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),

    CONSTRAINT uk_idempotency_key UNIQUE (tenant_id, idempotency_key)
);

-- Indexes for fast lookups
CREATE INDEX idx_idempotency_tenant_key
ON idempotency_keys(tenant_id, idempotency_key);

CREATE INDEX idx_idempotency_expires_at
ON idempotency_keys(expires_at);

CREATE INDEX idx_idempotency_status
ON idempotency_keys(status);

-- Table comments
COMMENT ON TABLE idempotency_keys IS 'Stores idempotency keys for exactly-once execution (24-hour TTL)';
COMMENT ON COLUMN idempotency_keys.idempotency_key IS 'Client-provided idempotency key (unique per tenant)';
COMMENT ON COLUMN idempotency_keys.request_hash IS 'SHA-256 hash of request body for deduplication';
COMMENT ON COLUMN idempotency_keys.response_body IS 'Cached response for completed operations';
COMMENT ON COLUMN idempotency_keys.expires_at IS 'Expiration timestamp (24 hours from created_at)';
