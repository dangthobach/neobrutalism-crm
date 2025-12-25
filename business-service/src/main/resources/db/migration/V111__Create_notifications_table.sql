-- Create notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    recipient_id UUID NOT NULL,
    sender_id UUID,
    entity_type VARCHAR(100),
    entity_id UUID,
    action_url VARCHAR(1000),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    priority INTEGER NOT NULL DEFAULT 0,
    metadata TEXT,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent_at TIMESTAMP,
    push_sent BOOLEAN NOT NULL DEFAULT FALSE,
    push_sent_at TIMESTAMP,

    -- Audit fields
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP,
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT,
    tenant_id VARCHAR(255)
);

-- Create indexes
CREATE INDEX idx_notification_recipient ON notifications(recipient_id);
CREATE INDEX idx_notification_type ON notifications(notification_type);
CREATE INDEX idx_notification_status ON notifications(status);
CREATE INDEX idx_notification_read ON notifications(is_read);
CREATE INDEX idx_notification_created_at ON notifications(created_at);
CREATE INDEX idx_notification_deleted ON notifications(deleted);
CREATE INDEX idx_notification_tenant ON notifications(tenant_id);
CREATE INDEX idx_notification_entity ON notifications(entity_type, entity_id);
CREATE INDEX idx_notification_priority ON notifications(priority);
CREATE INDEX idx_notification_unread ON notifications(recipient_id, is_read);
CREATE INDEX idx_notification_high_priority ON notifications(recipient_id, priority, created_at);
