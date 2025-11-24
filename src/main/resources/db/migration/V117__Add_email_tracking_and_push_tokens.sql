-- =====================================================
-- Email Tracking and Push Notification Enhancement
-- Version: 117
-- Created: 2025-01-23
-- Description: Add email tracking, push tokens, and WebSocket session management
-- Optimized for: Email delivery tracking and WebSocket real-time notifications for 50K CCU
-- =====================================================

-- =====================================================
-- 1. Push Notification Tokens Table
-- =====================================================
CREATE TABLE IF NOT EXISTS push_notification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- User context
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    
    -- Token information
    token VARCHAR(500) NOT NULL UNIQUE,
    device_type VARCHAR(50) NOT NULL CHECK (device_type IN ('WEB', 'IOS', 'ANDROID', 'OTHER')),
    device_id VARCHAR(255),
    
    -- Browser/device information
    user_agent TEXT,
    ip_address VARCHAR(45),
    
    -- Token status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP WITHOUT TIME ZONE,
    
    -- Audit fields
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    
    -- Indexes for performance
    CONSTRAINT fk_push_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for push tokens
CREATE INDEX IF NOT EXISTS idx_push_token_user ON push_notification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_push_token_user_active ON push_notification_tokens(user_id, is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_push_token_org ON push_notification_tokens(organization_id);
CREATE INDEX IF NOT EXISTS idx_push_token_expires ON push_notification_tokens(expires_at) WHERE is_active = TRUE;

-- =====================================================
-- 2. Email Delivery Tracking Table
-- =====================================================
CREATE TABLE IF NOT EXISTS email_delivery_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Notification reference
    notification_id UUID NOT NULL,
    
    -- Recipient information
    recipient_email VARCHAR(255) NOT NULL,
    recipient_user_id UUID,
    
    -- Email details
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(100),
    
    -- Delivery status
    status VARCHAR(50) NOT NULL CHECK (status IN (
        'QUEUED',
        'SENDING',
        'SENT',
        'DELIVERED',
        'OPENED',
        'CLICKED',
        'BOUNCED',
        'FAILED',
        'SPAM'
    )) DEFAULT 'QUEUED',
    
    -- Tracking information
    sent_at TIMESTAMP WITHOUT TIME ZONE,
    delivered_at TIMESTAMP WITHOUT TIME ZONE,
    opened_at TIMESTAMP WITHOUT TIME ZONE,
    clicked_at TIMESTAMP WITHOUT TIME ZONE,
    failed_at TIMESTAMP WITHOUT TIME ZONE,
    
    -- Error handling
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    
    -- Email provider information
    provider_message_id VARCHAR(255),
    provider_name VARCHAR(50),
    
    -- Metadata
    metadata JSONB,
    
    -- Audit fields
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key
    CONSTRAINT fk_email_log_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- Indexes for email delivery logs
CREATE INDEX IF NOT EXISTS idx_email_log_notification ON email_delivery_logs(notification_id);
CREATE INDEX IF NOT EXISTS idx_email_log_recipient ON email_delivery_logs(recipient_email);
CREATE INDEX IF NOT EXISTS idx_email_log_status ON email_delivery_logs(status);
CREATE INDEX IF NOT EXISTS idx_email_log_created ON email_delivery_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_email_log_user ON email_delivery_logs(recipient_user_id);

-- Composite index for retry logic
CREATE INDEX IF NOT EXISTS idx_email_log_retry 
    ON email_delivery_logs(status, retry_count, created_at)
    WHERE status = 'FAILED' AND retry_count < max_retries;

-- =====================================================
-- 3. WebSocket Session Management (Optional - for connection tracking)
-- =====================================================
CREATE TABLE IF NOT EXISTS websocket_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- User context
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    
    -- Session information
    session_id VARCHAR(255) NOT NULL UNIQUE,
    subscription_id VARCHAR(255),
    
    -- Connection details
    connected_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP WITHOUT TIME ZONE,
    last_heartbeat_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Client information
    user_agent TEXT,
    ip_address VARCHAR(45),
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Metadata
    metadata JSONB,
    
    -- Indexes for performance
    CONSTRAINT fk_ws_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for WebSocket sessions
CREATE INDEX IF NOT EXISTS idx_ws_session_user ON websocket_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_ws_session_active ON websocket_sessions(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_ws_session_heartbeat ON websocket_sessions(last_heartbeat_at) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_ws_session_org ON websocket_sessions(organization_id);

-- =====================================================
-- 4. Notification Queue Table (for digest and delayed delivery)
-- =====================================================
CREATE TABLE IF NOT EXISTS notification_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Notification reference
    notification_id UUID NOT NULL,
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    
    -- Queue information
    delivery_channel VARCHAR(50) NOT NULL CHECK (delivery_channel IN ('EMAIL', 'PUSH', 'SMS', 'IN_APP')),
    scheduled_for TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    
    -- Status
    status VARCHAR(50) NOT NULL CHECK (status IN (
        'PENDING',
        'PROCESSING',
        'COMPLETED',
        'FAILED',
        'CANCELLED'
    )) DEFAULT 'PENDING',
    
    -- Processing
    processed_at TIMESTAMP WITHOUT TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    
    -- Error handling
    last_error TEXT,
    
    -- Metadata
    metadata JSONB,
    
    -- Audit fields
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_queue_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_queue_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for notification queue
CREATE INDEX IF NOT EXISTS idx_queue_scheduled ON notification_queue(scheduled_for, status) 
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_queue_user ON notification_queue(user_id, status);
CREATE INDEX IF NOT EXISTS idx_queue_notification ON notification_queue(notification_id);
CREATE INDEX IF NOT EXISTS idx_queue_channel ON notification_queue(delivery_channel, status);

-- Composite index for queue processing
CREATE INDEX IF NOT EXISTS idx_queue_processing 
    ON notification_queue(status, scheduled_for, attempts)
    WHERE status IN ('PENDING', 'FAILED') AND attempts < max_attempts;

-- =====================================================
-- Comments for Documentation
-- =====================================================

COMMENT ON TABLE push_notification_tokens IS 
    'Storage for push notification tokens (FCM, APNs) for real-time mobile/web push notifications';

COMMENT ON TABLE email_delivery_logs IS 
    'Tracks email delivery status with open/click tracking for monitoring email campaign effectiveness';

COMMENT ON TABLE websocket_sessions IS 
    'Manages active WebSocket connections for real-time notifications. Used for connection health monitoring.';

COMMENT ON TABLE notification_queue IS 
    'Queue for delayed/scheduled notification delivery and digest mode. Enables retry logic and rate limiting.';

-- =====================================================
-- Cleanup Job Hints (to be implemented in application)
-- =====================================================

-- Cleanup old email logs (older than 90 days):
-- DELETE FROM email_delivery_logs WHERE created_at < NOW() - INTERVAL '90 days';

-- Cleanup expired push tokens:
-- DELETE FROM push_notification_tokens WHERE expires_at < NOW() AND is_active = FALSE;

-- Cleanup stale WebSocket sessions (no heartbeat for > 5 minutes):
-- UPDATE websocket_sessions SET is_active = FALSE, disconnected_at = NOW() 
-- WHERE last_heartbeat_at < NOW() - INTERVAL '5 minutes' AND is_active = TRUE;

-- Cleanup processed queue items (older than 7 days):
-- DELETE FROM notification_queue WHERE status = 'COMPLETED' AND processed_at < NOW() - INTERVAL '7 days';
