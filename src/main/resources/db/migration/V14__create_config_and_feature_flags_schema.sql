-- =============================================================================
-- Migration: V14__create_config_and_feature_flags_schema.sql
-- Description: M15 Config & Feature Flags Engine (Dynamic Settings & Kill Switches)
-- BRD: AKH-BRD-001 Section 26, Section 29
-- =============================================================================

-- 1. Platform Configurations Table
CREATE TABLE IF NOT EXISTS platform_configs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key    VARCHAR(80) NOT NULL,
    config_value  TEXT NOT NULL,
    scope         VARCHAR(20) NOT NULL DEFAULT 'GLOBAL'
                  CONSTRAINT chk_config_scope CHECK (scope IN ('GLOBAL', 'ZONE', 'SHOP', 'CATEGORY')),
    scope_id      UUID,
    description   VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_config_key_scope UNIQUE (config_key, scope, scope_id)
);

CREATE INDEX IF NOT EXISTS idx_platform_configs_key ON platform_configs (config_key);
CREATE INDEX IF NOT EXISTS idx_platform_configs_scope ON platform_configs (scope, scope_id);

-- 2. Feature Flags Table (1-Tap Kill Switches)
CREATE TABLE IF NOT EXISTS feature_flags (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key      VARCHAR(80) NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT FALSE,
    scope         VARCHAR(20) NOT NULL DEFAULT 'GLOBAL'
                  CONSTRAINT chk_flag_scope CHECK (scope IN ('GLOBAL', 'ZONE', 'SHOP', 'CATEGORY')),
    scope_id      UUID,
    description   VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_flag_key_scope UNIQUE (flag_key, scope, scope_id)
);

CREATE INDEX IF NOT EXISTS idx_feature_flags_key ON feature_flags (flag_key);
CREATE INDEX IF NOT EXISTS idx_feature_flags_scope ON feature_flags (scope, scope_id);

-- 3. Seed BRD Default Configurations (Section 26)
INSERT INTO platform_configs (config_key, config_value, scope, description)
VALUES 
    ('hold_window_minutes', '10', 'GLOBAL', 'Time in minutes allowed for customer check-in once turn approaches'),
    ('stale_warn_minutes', '15', 'GLOBAL', 'Threshold in minutes to show subtle stale badge on shop card'),
    ('stale_flag_minutes', '45', 'GLOBAL', 'Threshold in minutes to mark shop status as STALE'),
    ('hide_after_minutes', '180', 'GLOBAL', 'Threshold in minutes to remove inactive shop from discovery'),
    ('default_radius_km', '2.0', 'GLOBAL', 'Default geographical search radius in kilometers'),
    ('token_cap_default', '25', 'GLOBAL', 'Default token capacity per shop queue session'),
    ('max_active_tokens_user', '3', 'GLOBAL', 'Anti-hoarding cap: Maximum concurrent active tokens per customer across platform'),
    ('free_tokens_per_day', '15', 'GLOBAL', 'Daily free tier token allotment per shop'),
    ('quiet_hours', '22:00-08:00', 'GLOBAL', 'Quiet hours window during which non-urgent notifications are suppressed')
ON CONFLICT (config_key, scope, scope_id) DO NOTHING;

-- 4. Seed BRD Default Feature Flags (Section 26)
INSERT INTO feature_flags (flag_key, enabled, scope, description)
VALUES 
    ('flags.map_view', TRUE, 'GLOBAL', 'Enable or disable interactive map view on public discovery screen'),
    ('flags.walk_in', TRUE, 'GLOBAL', 'Enable or disable merchant offline walk-in token issuance'),
    ('flags.priority_token', FALSE, 'GLOBAL', 'Phase 2 paid priority queue token jumping'),
    ('flags.reviews', FALSE, 'GLOBAL', 'Phase 2 post-service customer reviews and ratings')
ON CONFLICT (flag_key, scope, scope_id) DO NOTHING;

