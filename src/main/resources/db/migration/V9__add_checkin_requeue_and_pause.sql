-- =============================================================================
-- Migration: V9__add_checkin_requeue_and_pause.sql
-- Description: Add checkin, requeue, expired status and session pause
-- BRD: AKH-BRD-001 Section 14, 18, 29 (FR-307, FR-309, FR-707, FR-708)
-- =============================================================================

-- 1. Add checkin and requeue columns to tokens
ALTER TABLE tokens
    ADD COLUMN IF NOT EXISTS checked_in BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS checked_in_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS checkin_lat NUMERIC(9,6),
    ADD COLUMN IF NOT EXISTS checkin_lng NUMERIC(9,6),
    ADD COLUMN IF NOT EXISTS requeued BOOLEAN NOT NULL DEFAULT false;

-- 2. Update tokens status check constraint to include EXPIRED
ALTER TABLE tokens DROP CONSTRAINT IF EXISTS chk_token_status;

ALTER TABLE tokens ADD CONSTRAINT chk_token_status
    CHECK (status IN ('WAITING', 'CALLED', 'SERVING', 'COMPLETED', 'CANCELLED', 'NO_SHOW', 'SKIPPED', 'EXPIRED'));

-- 3. Add pause fields to queue_sessions
ALTER TABLE queue_sessions
    ADD COLUMN IF NOT EXISTS paused_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS pause_reason TEXT;

