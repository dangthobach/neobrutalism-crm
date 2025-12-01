-- =====================================================
-- Notification Preferences Table Migration
-- Version: 116
-- Created: 2025-01-23
-- Description: Create notification_preferences table for multi-channel notification settings
-- Optimized for: 1M users, 50K CCU with proper indexing
-- =====================================================

CREATE TABLE IF NOT EXISTS notification_preferences (
    -- Primary key with UUID v7 (time-ordered for better B-tree performance)
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- User and organization context
    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,

    -- Notification type this preference applies to
    notification_type VARCHAR(50) NOT NULL CHECK (notification_type IN (
        'SYSTEM',
        'TASK_ASSIGNED',
        'TASK_UPDATED',
        'TASK_COMPLETED',
        'TASK_OVERDUE',
        'DEADLINE_APPROACHING',
        'COMMENT_ADDED',
        'MENTION',
        'CUSTOMER_CREATED',
        'CUSTOMER_UPDATED',
        'CONTACT_ADDED',
        'ACTIVITY_REMINDER',
        'TEAM_INVITATION',
        'USER_MENTION'
    )),

    -- Multi-channel notification settings
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    -- Quiet hours settings (HH:mm format)
    quiet_hours_start VARCHAR(5),
    quiet_hours_end VARCHAR(5),

    -- Digest mode settings
    digest_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    digest_time VARCHAR(5),  -- HH:mm format

    -- Audit fields
    version BIGINT DEFAULT 0,
    created_by VARCHAR(255),
    created_date TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint: one preference per user+type+organization
    CONSTRAINT uk_user_type_org UNIQUE (user_id, notification_type, organization_id)
);

-- =====================================================
-- Performance Indexes for High-Scale Operations
-- =====================================================

-- Composite index for user+organization queries (most common query pattern)
-- Used by: getUserPreferences(userId, organizationId)
CREATE INDEX IF NOT EXISTS idx_notif_pref_user_org
    ON notification_preferences(user_id, organization_id);

-- Composite index for user+type queries
-- Used by: getPreference(userId, organizationId, type)
CREATE INDEX IF NOT EXISTS idx_notif_pref_user_type
    ON notification_preferences(user_id, notification_type);

-- Organization index for admin queries and bulk operations
CREATE INDEX IF NOT EXISTS idx_notif_pref_org
    ON notification_preferences(organization_id);

-- Index for finding users with specific channel enabled (bulk notification sending)
CREATE INDEX IF NOT EXISTS idx_notif_pref_in_app_enabled
    ON notification_preferences(organization_id, notification_type)
    WHERE in_app_enabled = TRUE;

CREATE INDEX IF NOT EXISTS idx_notif_pref_email_enabled
    ON notification_preferences(organization_id, notification_type)
    WHERE email_enabled = TRUE;

CREATE INDEX IF NOT EXISTS idx_notif_pref_sms_enabled
    ON notification_preferences(organization_id, notification_type)
    WHERE sms_enabled = TRUE;

-- =====================================================
-- Comments for Documentation
-- =====================================================

COMMENT ON TABLE notification_preferences IS
    'User notification preferences for multi-channel delivery (in-app, email, SMS).
     Optimized for 1M users and 50K concurrent users with composite indexes.';

COMMENT ON COLUMN notification_preferences.user_id IS
    'Reference to user who owns this preference';

COMMENT ON COLUMN notification_preferences.organization_id IS
    'Organization context for multi-tenancy isolation';

COMMENT ON COLUMN notification_preferences.notification_type IS
    'Type of notification this preference applies to';

COMMENT ON COLUMN notification_preferences.in_app_enabled IS
    'Enable/disable in-app notifications (browser/mobile app)';

COMMENT ON COLUMN notification_preferences.email_enabled IS
    'Enable/disable email notifications';

COMMENT ON COLUMN notification_preferences.sms_enabled IS
    'Enable/disable SMS notifications (consider cost implications)';

COMMENT ON COLUMN notification_preferences.quiet_hours_start IS
    'Start time for quiet hours in HH:mm format (24-hour). Notifications queued during this period.';

COMMENT ON COLUMN notification_preferences.quiet_hours_end IS
    'End time for quiet hours in HH:mm format (24-hour)';

COMMENT ON COLUMN notification_preferences.digest_mode_enabled IS
    'Enable digest mode to receive daily summary instead of individual notifications';

COMMENT ON COLUMN notification_preferences.digest_time IS
    'Time to deliver daily digest in HH:mm format (24-hour)';

-- =====================================================
-- Sample Data for Testing (Optional)
-- =====================================================

-- Insert default preferences for system user if exists
-- INSERT INTO notification_preferences (user_id, organization_id, notification_type, in_app_enabled, email_enabled, sms_enabled)
-- SELECT
--     u.id,
--     u.organization_id,
--     'SYSTEM',
--     TRUE,
--     TRUE,
--     FALSE
-- FROM users u
-- WHERE u.username = 'admin'
-- ON CONFLICT (user_id, notification_type, organization_id) DO NOTHING;
