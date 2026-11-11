-- =============================================================================
-- Migration: V11__add_user_profile_and_dpdp_fields.sql
-- Description: Customer profile fields (email, preferred zone) and DPDP compliance fields (is_deleted, deleted_at)
-- BRD Section 16 & Section 24.3 (FR-502, FR-507)
-- =============================================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS preferred_zone_id UUID REFERENCES zones(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_preferred_zone ON users(preferred_zone_id);
CREATE INDEX IF NOT EXISTS idx_users_is_deleted ON users(is_deleted);

