-- =============================================================================
-- Migration: V12__create_shop_claims_schema.sql
-- Description: Shop Claim and Merchant Verification Schema (BRD Section 17, FR-601 to FR-607)
-- =============================================================================

CREATE TABLE IF NOT EXISTS shop_claims (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id             UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_type       VARCHAR(50) NOT NULL,
    document_url        VARCHAR(500) NOT NULL,
    applicant_phone     VARCHAR(20) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason    VARCHAR(500),
    admin_notes         VARCHAR(500),
    reviewed_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shop_claims_shop_id ON shop_claims(shop_id);
CREATE INDEX IF NOT EXISTS idx_shop_claims_user_id ON shop_claims(user_id);
CREATE INDEX IF NOT EXISTS idx_shop_claims_status ON shop_claims(status);

-- Only one PENDING claim per user per shop
CREATE UNIQUE INDEX IF NOT EXISTS uk_shop_claim_pending ON shop_claims (shop_id, user_id) WHERE status = 'PENDING';

