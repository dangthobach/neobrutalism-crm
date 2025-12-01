-- =====================================================
-- ✅ PHASE 1.2: FLYWAY MIGRATION V4
-- JWT Blacklist & Refresh Tokens for Security
-- =====================================================

-- =====================================================
-- TOKEN_BLACKLIST
-- For invalidated JWT access tokens
-- =====================================================
CREATE TABLE token_blacklist (
    id UUID PRIMARY KEY,
    
    -- Token identification
    token_hash VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 hash of JWT
    jti VARCHAR(36), -- JWT ID claim
    
    -- User information
    user_id UUID NOT NULL,
    username VARCHAR(50),
    
    -- Blacklist reason
    reason VARCHAR(100) NOT NULL, -- LOGOUT, REFRESH, SECURITY_BREACH, PASSWORD_CHANGE, etc.
    
    -- Timestamps
    blacklisted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL, -- Original token expiry (for cleanup)
    
    -- Device/Session info
    user_agent TEXT,
    ip_address VARCHAR(45),
    
    -- Audit
    created_by UUID,
    
    CONSTRAINT fk_blacklist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================
-- REFRESH_TOKENS
-- For JWT refresh token rotation
-- =====================================================
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    
    -- Token identification
    token_hash VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 hash of refresh token
    jti VARCHAR(36) UNIQUE, -- JWT ID claim
    
    -- User information
    user_id UUID NOT NULL,
    username VARCHAR(50),
    
    -- Token lifecycle
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    
    -- Revocation
    revoked BOOLEAN DEFAULT false,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR(200),
    
    -- Token rotation tracking
    replaced_by_token UUID, -- Points to new token after rotation
    rotation_count INTEGER DEFAULT 0,
    
    -- Device/Session information
    device_id VARCHAR(100),
    device_name VARCHAR(200),
    device_type VARCHAR(50), -- WEB, MOBILE_IOS, MOBILE_ANDROID, DESKTOP
    user_agent TEXT,
    ip_address VARCHAR(45),
    location VARCHAR(200), -- City, Country
    
    -- Security flags
    is_suspicious BOOLEAN DEFAULT false,
    suspicious_reason TEXT,
    
    -- Audit
    created_by UUID,
    
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_replaced FOREIGN KEY (replaced_by_token) REFERENCES refresh_tokens(id)
);

-- =====================================================
-- USER_SESSIONS
-- Track active user sessions
-- =====================================================
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    
    -- User information
    user_id UUID NOT NULL,
    username VARCHAR(50),
    
    -- Session identification
    session_id VARCHAR(100) NOT NULL UNIQUE,
    refresh_token_id UUID,
    
    -- Session lifecycle
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    
    -- Termination
    terminated BOOLEAN DEFAULT false,
    terminated_at TIMESTAMP,
    termination_reason VARCHAR(100), -- LOGOUT, TIMEOUT, FORCED, SECURITY
    
    -- Device information
    device_id VARCHAR(100),
    device_name VARCHAR(200),
    device_type VARCHAR(50),
    user_agent TEXT,
    browser VARCHAR(100),
    os VARCHAR(100),
    
    -- Location
    ip_address VARCHAR(45),
    country VARCHAR(100),
    city VARCHAR(100),
    
    -- Activity tracking
    page_views INTEGER DEFAULT 0,
    api_calls INTEGER DEFAULT 0,
    
    -- Security
    is_suspicious BOOLEAN DEFAULT false,
    risk_score INTEGER DEFAULT 0, -- 0-100
    
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_refresh_token FOREIGN KEY (refresh_token_id) REFERENCES refresh_tokens(id)
);

-- =====================================================
-- AUDIT_LOGS
-- Comprehensive audit trail
-- =====================================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    
    -- Event information
    event_type VARCHAR(100) NOT NULL, -- LOGIN, LOGOUT, CREATE, UPDATE, DELETE, etc.
    event_category VARCHAR(50) NOT NULL, -- AUTH, USER, CUSTOMER, CONTACT, etc.
    event_action VARCHAR(50) NOT NULL, -- READ, WRITE, DELETE, etc.
    
    -- Actor
    user_id UUID,
    username VARCHAR(50),
    
    -- Target
    entity_type VARCHAR(100), -- User, Customer, Contact, etc.
    entity_id UUID,
    entity_name VARCHAR(200),
    
    -- Details
    description TEXT,
    old_value TEXT, -- JSON
    new_value TEXT, -- JSON
    changes TEXT, -- JSON diff
    
    -- Result
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILURE, ERROR
    error_message TEXT,
    
    -- Context
    organization_id UUID,
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_id VARCHAR(100),
    session_id VARCHAR(100),
    
    -- Performance
    execution_time_ms INTEGER,
    
    -- Timestamp
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE SET NULL
);

-- =====================================================
-- SECURITY_EVENTS
-- Security-specific events
-- =====================================================
CREATE TABLE security_events (
    id UUID PRIMARY KEY,
    
    -- Event classification
    event_type VARCHAR(100) NOT NULL, -- FAILED_LOGIN, SUSPICIOUS_ACTIVITY, TOKEN_THEFT, etc.
    severity VARCHAR(20) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    
    -- User
    user_id UUID,
    username VARCHAR(50),
    
    -- Details
    description TEXT NOT NULL,
    details TEXT, -- JSON
    
    -- Context
    ip_address VARCHAR(45),
    user_agent TEXT,
    location VARCHAR(200),
    
    -- Response
    action_taken VARCHAR(100), -- BLOCKED, LOGGED, ALERTED, AUTO_LOCKED
    requires_investigation BOOLEAN DEFAULT false,
    investigated BOOLEAN DEFAULT false,
    investigated_at TIMESTAMP,
    investigated_by UUID,
    
    -- Timestamp
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_security_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_security_investigator FOREIGN KEY (investigated_by) REFERENCES users(id)
);

-- =====================================================
-- INDEXES FOR PERFORMANCE
-- =====================================================

-- Token Blacklist
CREATE INDEX idx_blacklist_token_hash ON token_blacklist(token_hash);
CREATE INDEX idx_blacklist_user ON token_blacklist(user_id);
CREATE INDEX idx_blacklist_expires_at ON token_blacklist(expires_at);
CREATE INDEX idx_blacklist_reason ON token_blacklist(reason);

-- Refresh Tokens
CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_jti ON refresh_tokens(jti);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_revoked ON refresh_tokens(revoked);
CREATE INDEX idx_refresh_device_id ON refresh_tokens(device_id);
CREATE INDEX idx_refresh_suspicious ON refresh_tokens(is_suspicious);

-- User Sessions
CREATE INDEX idx_session_user ON user_sessions(user_id);
CREATE INDEX idx_session_id ON user_sessions(session_id);
CREATE INDEX idx_session_refresh_token ON user_sessions(refresh_token_id);
CREATE INDEX idx_session_expires_at ON user_sessions(expires_at);
CREATE INDEX idx_session_terminated ON user_sessions(terminated);
CREATE INDEX idx_session_last_activity ON user_sessions(last_activity_at);
CREATE INDEX idx_session_device_id ON user_sessions(device_id);

-- Audit Logs
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_organization ON audit_logs(organization_id);
CREATE INDEX idx_audit_status ON audit_logs(status);

-- Composite indexes
CREATE INDEX idx_audit_user_timestamp ON audit_logs(user_id, timestamp DESC);
CREATE INDEX idx_audit_entity_timestamp ON audit_logs(entity_type, entity_id, timestamp DESC);

-- Security Events
CREATE INDEX idx_security_user ON security_events(user_id);
CREATE INDEX idx_security_type ON security_events(event_type);
CREATE INDEX idx_security_severity ON security_events(severity);
CREATE INDEX idx_security_timestamp ON security_events(timestamp);
CREATE INDEX idx_security_investigated ON security_events(investigated);
CREATE INDEX idx_security_requires_investigation ON security_events(requires_investigation);

-- =====================================================
-- AUTOMATIC CLEANUP FUNCTIONS (PostgreSQL-specific, disabled for H2)
-- =====================================================

-- Function to cleanup expired blacklisted tokens
-- CREATE OR REPLACE FUNCTION cleanup_expired_blacklist()
-- RETURNS void AS $$
-- BEGIN
--     DELETE FROM token_blacklist
--     WHERE expires_at < NOW() - INTERVAL '7 days'; -- Keep 7 days after expiry for audit
-- END;
-- $$ LANGUAGE plpgsql;

-- Function to cleanup expired refresh tokens
-- CREATE OR REPLACE FUNCTION cleanup_expired_refresh_tokens()
-- RETURNS void AS $$
-- BEGIN
--     DELETE FROM refresh_tokens
--     WHERE expires_at < NOW() - INTERVAL '30 days' -- Keep 30 days after expiry
--     AND revoked = true;
-- END;
-- $$ LANGUAGE plpgsql;

-- Function to cleanup old sessions
-- CREATE OR REPLACE FUNCTION cleanup_old_sessions()
-- RETURNS void AS $$
-- BEGIN
--     DELETE FROM user_sessions
--     WHERE terminated = true
--     AND terminated_at < NOW() - INTERVAL '90 days'; -- Keep 90 days of history
-- END;
-- $$ LANGUAGE plpgsql;

-- Function to cleanup old audit logs (keep 1 year)
-- CREATE OR REPLACE FUNCTION cleanup_old_audit_logs()
-- RETURNS void AS $$
-- BEGIN
--     DELETE FROM audit_logs
--     WHERE timestamp < NOW() - INTERVAL '1 year';
-- END;
-- $$ LANGUAGE plpgsql;

-- =====================================================
-- VIEWS FOR MONITORING
-- =====================================================

-- Active sessions view
CREATE VIEW v_active_sessions AS
SELECT
    s.id,
    s.user_id,
    s.username,
    s.session_id,
    s.device_type,
    s.device_name,
    s.ip_address,
    s.started_at,
    s.last_activity_at,
    s.expires_at,
    s.page_views,
    s.api_calls,
    EXTRACT(EPOCH FROM (NOW() - s.last_activity_at)) / 60 AS idle_minutes,
    s.is_suspicious,
    s.risk_score
FROM user_sessions s
WHERE s.terminated = false
AND s.expires_at > NOW();

-- Recent security events view
CREATE VIEW v_recent_security_events AS
SELECT
    e.id,
    e.event_type,
    e.severity,
    e.user_id,
    e.username,
    e.description,
    e.ip_address,
    e.timestamp,
    e.action_taken,
    e.requires_investigation
FROM security_events e
WHERE e.timestamp > NOW() - INTERVAL '7 days'
ORDER BY e.timestamp DESC;

-- Suspicious activity view
CREATE VIEW v_suspicious_activity AS
SELECT
    u.id AS user_id,
    u.username,
    u.email,
    COUNT(DISTINCT s.id) AS suspicious_sessions,
    COUNT(DISTINCT rt.id) AS suspicious_tokens,
    MAX(se.timestamp) AS last_suspicious_event
FROM users u
LEFT JOIN user_sessions s ON u.id = s.user_id AND s.is_suspicious = true
LEFT JOIN refresh_tokens rt ON u.id = rt.user_id AND rt.is_suspicious = true
LEFT JOIN security_events se ON u.id = se.user_id
WHERE s.is_suspicious = true
   OR rt.is_suspicious = true
   OR se.severity IN ('HIGH', 'CRITICAL')
GROUP BY u.id, u.username, u.email
HAVING COUNT(DISTINCT s.id) > 0 OR COUNT(DISTINCT rt.id) > 0;

-- =====================================================
-- END OF V4 MIGRATION
-- =====================================================
