-- =============================================================================
-- Migration: V21__add_group_tokens_support.sql
-- Description: Group & Multi-Person Token Support (Family & Friends Batch Booking)
-- =============================================================================

ALTER TABLE tokens ADD COLUMN IF NOT EXISTS group_id UUID;
ALTER TABLE tokens ADD COLUMN IF NOT EXISTS group_size INT NOT NULL DEFAULT 1;
ALTER TABLE tokens ADD COLUMN IF NOT EXISTS group_member_index INT NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_tokens_group_id ON tokens (group_id);

