-- =============================================================================
-- Migration: V6__add_shop_owner_and_status_events.sql
-- Description: M2 Real-Time Status Engine (Audit Trail & Shop Ownership)
-- BRD: AKH-BRD-001 Section 13, 29 (Live Availability & Status Updates)
-- =============================================================================

-- 1. Add owner_user_id to shops
ALTER TABLE shops ADD COLUMN IF NOT EXISTS owner_user_id UUID REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_shops_owner_user_id ON shops (owner_user_id);

-- 2. Seed pilot shop owner (Salim Bhai - Royal Barber)
INSERT INTO users (id, phone, name, role, language, blocked)
VALUES ('b1b2c3d4-0000-0000-0000-000000000001', '9876543210', 'Salim Ahmad', 'SHOP_OWNER', 'hi', FALSE)
ON CONFLICT (phone) DO UPDATE 
SET role = 'SHOP_OWNER', name = 'Salim Ahmad', id = 'b1b2c3d4-0000-0000-0000-000000000001';

-- Link pilot shop (Royal Barber) to Salim Bhai
UPDATE shops
SET owner_user_id = 'b1b2c3d4-0000-0000-0000-000000000001'
WHERE id = 'd1b2c3d4-0000-0000-0000-000000000001';

-- 3. Create shop_status_events table (Immutable audit trail)
CREATE TABLE IF NOT EXISTS shop_status_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id         UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    from_status     VARCHAR(20) NOT NULL,
    to_status       VARCHAR(20) NOT NULL,
    crowd_level     INT NOT NULL CONSTRAINT chk_event_crowd_level CHECK (crowd_level BETWEEN 1 AND 4),
    source          VARCHAR(30) NOT NULL DEFAULT 'OWNER_APP',
    lat             NUMERIC(9, 6),
    lng             NUMERIC(9, 6),
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shop_status_events_shop_id_created ON shop_status_events (shop_id, created_at DESC);

