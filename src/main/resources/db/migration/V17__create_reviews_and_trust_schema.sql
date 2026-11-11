-- =============================================================================
-- Migration: V17__create_reviews_and_trust_schema.sql
-- Description: Reviews, Ratings & Trust Layer Engine (BRD Section 22, 29, FR-1101 to FR-1107)
-- =============================================================================

CREATE TABLE IF NOT EXISTS reviews (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id             UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    token_id            UUID NOT NULL UNIQUE REFERENCES tokens(id) ON DELETE RESTRICT,
    user_id             UUID REFERENCES users(id) ON DELETE SET NULL,
    customer_name       VARCHAR(100),
    stars               INT NOT NULL CONSTRAINT chk_review_stars CHECK (stars BETWEEN 1 AND 5),
    text                TEXT,
    tags                VARCHAR(255),
    verified_token      BOOLEAN NOT NULL DEFAULT TRUE,
    hidden              BOOLEAN NOT NULL DEFAULT FALSE,
    abuse_reported      BOOLEAN NOT NULL DEFAULT FALSE,
    abuse_reason        TEXT,
    reply               TEXT,
    reply_at            TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reviews_shop_hidden ON reviews (shop_id, hidden, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reviews_abuse_reported ON reviews (abuse_reported);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews (user_id);

