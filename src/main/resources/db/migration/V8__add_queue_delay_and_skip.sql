-- =============================================================================
-- Migration: V8__add_queue_delay_and_skip.sql
-- Description: Add delay broadcast to queue_sessions and SKIPPED to token_status
-- BRD: AKH-BRD-001 Section 14, 15 (Merchant Queue Controls & Delay Announcement)
-- =============================================================================

-- 1. Add delay fields to queue_sessions
ALTER TABLE queue_sessions
    ADD COLUMN IF NOT EXISTS delay_minutes INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS delay_reason TEXT;

-- 2. Update token status check constraint to include SKIPPED
ALTER TABLE tokens DROP CONSTRAINT IF EXISTS chk_token_status;

ALTER TABLE tokens ADD CONSTRAINT chk_token_status
    CHECK (status IN ('WAITING', 'CALLED', 'SERVING', 'COMPLETED', 'CANCELLED', 'NO_SHOW', 'SKIPPED'));

