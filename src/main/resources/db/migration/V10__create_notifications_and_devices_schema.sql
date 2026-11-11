-- =============================================================================
-- Migration: V10__create_notifications_and_devices_schema.sql
-- Description: M4 Notifications & Reminders Engine (Devices & Notifications)
-- BRD: AKH-BRD-001 Section 15, 29 (FR-401 to FR-410)
-- =============================================================================

-- 1. Devices Table (For Web Push FCM / Device tokens)
CREATE TABLE IF NOT EXISTS devices (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token            VARCHAR(255) NOT NULL,
    device_type          VARCHAR(30) NOT NULL DEFAULT 'WEB',
    user_agent           TEXT,
    last_seen            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_devices_user_fcm UNIQUE (user_id, fcm_token)
);

CREATE INDEX IF NOT EXISTS idx_devices_user ON devices(user_id);

-- 2. Notifications Table (In-app history & alert log)
CREATE TABLE IF NOT EXISTS notifications (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shop_id              UUID REFERENCES shops(id) ON DELETE SET NULL,
    token_id             UUID REFERENCES tokens(id) ON DELETE SET NULL,
    template             VARCHAR(50) NOT NULL,
    channel              VARCHAR(30) NOT NULL DEFAULT 'IN_APP'
                         CONSTRAINT chk_notification_channel CHECK (channel IN ('IN_APP', 'PUSH_WEB', 'SMS', 'WHATSAPP')),
    title                VARCHAR(200) NOT NULL,
    body                 TEXT NOT NULL,
    payload              TEXT,
    is_read              BOOLEAN NOT NULL DEFAULT false,
    sent_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_user_sent ON notifications(user_id, sent_at DESC);

