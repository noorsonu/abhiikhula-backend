-- =============================================================================
-- Migration: V3__create_shops_schema.sql
-- Description: Shops & Shop Hours tables (BRD: AKH-BRD-001 Section 29)
-- =============================================================================

CREATE TABLE IF NOT EXISTS shops (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id         UUID NOT NULL REFERENCES zones(id) ON DELETE RESTRICT,
    category_id     UUID NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    name            VARCHAR(150) NOT NULL,
    slug            VARCHAR(150) NOT NULL UNIQUE,
    address         TEXT NOT NULL,
    lat             NUMERIC(9, 6) NOT NULL,
    lng             NUMERIC(9, 6) NOT NULL,
    phone           VARCHAR(20),
    status          VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
    crowd_level     INT NOT NULL DEFAULT 1
                    CONSTRAINT chk_crowd_level CHECK (crowd_level BETWEEN 1 AND 4),
    last_status_update_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    token_cap       INT NOT NULL DEFAULT 100,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_shops_zone_category_status ON shops (zone_id, category_id, status);
CREATE INDEX IF NOT EXISTS idx_shops_zone_status          ON shops (zone_id, status);
CREATE INDEX IF NOT EXISTS idx_shops_active               ON shops (active);

-- Shop Operating Hours (Mon=1 … Sun=7)
CREATE TABLE IF NOT EXISTS shop_hours (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id         UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    weekday         SMALLINT NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    open_time       TIME NOT NULL,
    close_time      TIME NOT NULL,
    closed_all_day  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_shop_hours_shop_weekday UNIQUE (shop_id, weekday)
);

