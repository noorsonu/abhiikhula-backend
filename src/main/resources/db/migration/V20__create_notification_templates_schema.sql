-- =============================================================================
-- Migration: V20__create_notification_templates_schema.sql
-- Description: M14 Dynamic Comms & Notification Templates Engine (BRD Section 25, Annexure A)
-- =============================================================================

CREATE TABLE IF NOT EXISTS notification_templates (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug                VARCHAR(60) NOT NULL,
    channel             VARCHAR(20) NOT NULL, -- PUSH, SMS, EMAIL
    locale              VARCHAR(10) NOT NULL DEFAULT 'hi', -- hi, en
    title_template      VARCHAR(255) NOT NULL,
    body_template       TEXT NOT NULL,
    variables           TEXT, -- comma-separated or json of placeholders, e.g. "shop,number,eta"
    version             INT NOT NULL DEFAULT 1,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_template_slug_channel_locale UNIQUE (slug, channel, locale)
);

CREATE INDEX IF NOT EXISTS idx_templates_slug_active ON notification_templates (slug, active);

-- Broadcast announcement log for rate-limit rule: max 1 broadcast per week (BRD FR-1405)
CREATE TABLE IF NOT EXISTS broadcast_announcements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title               VARCHAR(255) NOT NULL,
    body                TEXT NOT NULL,
    target_role         VARCHAR(30) NOT NULL, -- ALL, CUSTOMER, SHOP_OWNER
    sent_by             UUID REFERENCES users(id) ON DELETE SET NULL,
    sent_count          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_broadcast_created ON broadcast_announcements (created_at DESC);

-- Seed Standard Templates from BRD Annexure A
INSERT INTO notification_templates (slug, channel, locale, title_template, body_template, variables, active)
VALUES
-- 1. two_ahead (Token approaching)
('two_ahead', 'PUSH', 'hi', 'Aapka number aane wala hai!', 'Aapke aage sirf 2 log hain {{shop}} par. Kripya dukaan pahuchein. Token #{{number}}, ETA {{eta}} min.', 'shop,number,eta', TRUE),
('two_ahead', 'PUSH', 'en', 'Your turn is almost here!', 'Only 2 people ahead of you at {{shop}}. Please reach the shop. Token #{{number}}, ETA {{eta}} mins.', 'shop,number,eta', TRUE),
('two_ahead', 'SMS', 'hi', 'AbhiiKhula Alert', 'AbhiiKhula: Aapke aage 2 log hain {{shop}} par. Token #{{number}}. Kripya pahuchein.', 'shop,number', TRUE),

-- 2. called (Now serving)
('called', 'PUSH', 'hi', 'Aapka number aa gaya!', 'Token #{{number}} ko {{shop}} par bulaya jaa raha hai. Kripya counter par jaayein.', 'shop,number', TRUE),
('called', 'PUSH', 'en', 'Your turn is now!', 'Token #{{number}} is being called at {{shop}}. Please proceed to the counter.', 'shop,number', TRUE),
('called', 'SMS', 'hi', 'AbhiiKhula Alert', 'AbhiiKhula: Token #{{number}} is NOW SERVING at {{shop}}. Kripya counter par pahuchein.', 'shop,number', TRUE),

-- 3. hold_warning (10 min hold before auto-cancel)
('hold_warning', 'PUSH', 'hi', 'Token Hold Warning', 'Aapka token #{{number}} {{shop}} par hold kar diya gaya hai. 10 minute me cancel ho jayega.', 'shop,number', TRUE),
('hold_warning', 'PUSH', 'en', 'Token Hold Warning', 'Your token #{{number}} at {{shop}} has been held. It will cancel in 10 minutes if not present.', 'shop,number', TRUE),

-- 4. shop_closed (Shop closed while token active)
('shop_closed', 'PUSH', 'hi', 'Dukaan Band Ho Gayi', 'Dukaan {{shop}} emergency me band ho gayi hai. Aapka token cancel kar diya gaya hai.', 'shop', TRUE),
('shop_closed', 'PUSH', 'en', 'Shop Closed', '{{shop}} has closed for the day. Your token has been safely cancelled.', 'shop', TRUE),

-- 5. owner_new_token (Owner alert)
('owner_new_token', 'PUSH', 'hi', 'Naya Token Aaya!', 'Customer ne naya token liya hai. Token #{{number}}.', 'number', TRUE),

-- 6. owner_stale (4hr stale alert)
('owner_stale', 'PUSH', 'hi', 'Status Update Karein', 'Aapki dukaan ka status 4 ghante se update nahi hua hai. Kripya live status refresh karein.', '', TRUE)
ON CONFLICT (slug, channel, locale) DO NOTHING;

