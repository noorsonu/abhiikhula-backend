-- =========================================================================
-- AbhiiKhula Database Migration V15: Subscriptions & Billing Engine (BRD M9, Section 20 & 29)
-- =========================================================================

-- 1. Subscriptions Table
CREATE TABLE subscriptions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id                 UUID NOT NULL UNIQUE REFERENCES shops(id) ON DELETE CASCADE,
    plan_code               VARCHAR(30) NOT NULL DEFAULT 'FREE', -- FREE, LIVE_PRO, LIVE_PRO_PLUS, LIFETIME_FREE_PRO
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, TRIAL, GRACE, SUSPENDED, CANCELLED
    trial_ends_at           TIMESTAMP WITH TIME ZONE,
    current_period_start    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_period_end      TIMESTAMP WITH TIME ZONE,
    tokens_used_today       INTEGER NOT NULL DEFAULT 0,
    last_token_reset_date   DATE NOT NULL DEFAULT CURRENT_DATE,
    auto_renew              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscriptions_shop_id ON subscriptions(shop_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_plan_code ON subscriptions(plan_code);

-- 2. Invoices Table
CREATE TABLE invoices (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id                 UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    subscription_id         UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    invoice_number          VARCHAR(50) NOT NULL UNIQUE,
    amount                  DECIMAL(10, 2) NOT NULL,
    currency                VARCHAR(10) NOT NULL DEFAULT 'INR',
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, REFUNDED, FAILED
    payment_method          VARCHAR(30) NOT NULL DEFAULT 'OFFLINE_UPI', -- OFFLINE_UPI, CASH, MANUAL_ADMIN, ONLINE
    payment_ref             VARCHAR(100), -- UPI UTR / Reference ID
    notes                   VARCHAR(500),
    paid_at                 TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoices_shop_id ON invoices(shop_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_number ON invoices(invoice_number);

-- 3. Seed Feature Flag: flags.monetization (Default: FALSE - 100% Free for startup pilot launch)
INSERT INTO feature_flags (flag_key, enabled, scope, description)
SELECT 'flags.monetization', FALSE, 'GLOBAL', 'Master switch for paid subscription enforcement and free token daily cap (BRD Section 20)'
WHERE NOT EXISTS (SELECT 1 FROM feature_flags WHERE flag_key = 'flags.monetization');

-- 4. Seed initial Lifetime Free Pro subscription for existing pilot shops
INSERT INTO subscriptions (shop_id, plan_code, status, current_period_start)
SELECT id, 'LIFETIME_FREE_PRO', 'ACTIVE', CURRENT_TIMESTAMP
FROM shops
WHERE NOT EXISTS (SELECT 1 FROM subscriptions WHERE subscriptions.shop_id = shops.id);

