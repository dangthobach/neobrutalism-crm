-- V123: Create notification_queue table for quiet hours support
-- Stores notifications that need to be sent later (during quiet hours)

CREATE TABLE notification_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,

    -- Notification details
    recipient_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    notification_type VARCHAR(50),
    priority INTEGER DEFAULT 0,

    -- Action details
    action_url VARCHAR(500),
    entity_type VARCHAR(50),
    entity_id UUID,

    -- Scheduling
    scheduled_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',

    -- Retry tracking
    attempt_count INTEGER DEFAULT 0,
    error_message TEXT,
    sent_at TIMESTAMP,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_notif_queue_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_notif_queue_recipient FOREIGN KEY (recipient_id) REFERENCES users(id),
    CONSTRAINT chk_notif_queue_status CHECK (status IN ('QUEUED', 'SENDING', 'SENT', 'FAILED'))
);

-- Indexes for efficient querying
CREATE INDEX idx_notif_queue_scheduled ON notification_queue(scheduled_at, status) WHERE deleted = false;
CREATE INDEX idx_notif_queue_recipient ON notification_queue(recipient_id) WHERE deleted = false;
CREATE INDEX idx_notif_queue_status ON notification_queue(status) WHERE deleted = false;
CREATE INDEX idx_notif_queue_org ON notification_queue(organization_id) WHERE deleted = false;

-- Index for cleanup query
CREATE INDEX idx_notif_queue_sent_at ON notification_queue(sent_at) WHERE status = 'SENT' AND deleted = false;

-- Comment
COMMENT ON TABLE notification_queue IS 'Queue for notifications sent during quiet hours or for digest mode';
COMMENT ON COLUMN notification_queue.scheduled_at IS 'When this notification should be sent';
COMMENT ON COLUMN notification_queue.attempt_count IS 'Number of send attempts (max 3)';
