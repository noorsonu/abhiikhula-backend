-- =============================================================================
-- Migration: V18__create_analytics_and_reporting_schema.sql
-- Description: Analytics events & tracking engine (BRD Section 23, 29, FR-1201 to FR-1204)
-- =============================================================================

CREATE TABLE IF NOT EXISTS analytics_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id       VARCHAR(100),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    shop_id         UUID REFERENCES shops(id) ON DELETE CASCADE,
    zone_id         UUID REFERENCES zones(id) ON DELETE SET NULL,
    name            VARCHAR(50) NOT NULL,
    props           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_analytics_events_name_created ON analytics_events (name, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_events_zone_name ON analytics_events (zone_id, name, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_events_shop_created ON analytics_events (shop_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_events_device ON analytics_events (device_id);
