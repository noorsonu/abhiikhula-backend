-- =============================================================================
-- Migration: V19__create_disputes_and_support_schema.sql
-- Description: Support & Disputes Desk schema (BRD Section 24, 29, Annexure C)
-- =============================================================================

-- Add strikes column to shops for governance tracking (BRD Section 22/24)
ALTER TABLE shops ADD COLUMN IF NOT EXISTS strikes INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS disputes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id             UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    token_id            UUID REFERENCES tokens(id) ON DELETE SET NULL,
    user_id             UUID REFERENCES users(id) ON DELETE SET NULL,
    customer_phone      VARCHAR(20),
    reason              VARCHAR(50) NOT NULL,
    description         TEXT,
    evidence_json       TEXT,
    state               VARCHAR(20) NOT NULL DEFAULT 'NEW',
    merchant_reply      TEXT,
    merchant_reply_at   TIMESTAMPTZ,
    resolution_action   VARCHAR(30),
    resolution_notes    TEXT,
    resolved_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_disputes_shop_state ON disputes (shop_id, state);
CREATE INDEX IF NOT EXISTS idx_disputes_state_created ON disputes (state, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_disputes_token_id ON disputes (token_id);
CREATE INDEX IF NOT EXISTS idx_disputes_user_id ON disputes (user_id);

