-- =========================================================================
-- AbhiiKhula Database Migration V16: Featured Listings & Local Ads (BRD M10, Section 21 & 29)
-- =========================================================================

CREATE TABLE featured_slots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id         UUID NOT NULL REFERENCES zones(id) ON DELETE CASCADE,
    category_id     UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    shop_id         UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    slot_date       DATE NOT NULL,
    position        INTEGER NOT NULL CHECK (position IN (1, 2)),
    impressions     INTEGER NOT NULL DEFAULT 0,
    clicks          INTEGER NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- BRD Section 29.2 Constraint: unique(zone_id, category_id, date, position)
    CONSTRAINT uk_featured_slot_pos UNIQUE (zone_id, category_id, slot_date, position),
    -- One slot per shop per zone per date
    CONSTRAINT uk_featured_slot_shop UNIQUE (zone_id, slot_date, shop_id)
);

CREATE INDEX idx_featured_slots_lookup ON featured_slots(zone_id, category_id, slot_date, active);
CREATE INDEX idx_featured_slots_shop ON featured_slots(shop_id);

