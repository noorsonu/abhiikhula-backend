-- =============================================================================
-- Migration: V2__seed_pilot_zones_and_categories.sql
-- Description: Seed Pilot Zone & Initial Categories (BRD Section 6.1 & 6.5)
-- =============================================================================

-- 1. Pilot Hyperlocal Zone (Indira Nagar Sector 14, Lucknow)
INSERT INTO zones (id, name, city, lat, lng, radius_km, active)
VALUES (
    'a1b2c3d4-0000-0000-0000-000000000001',
    'Indira Nagar (Sector 14)',
    'Lucknow',
    26.885400,
    80.994200,
    2.0,
    true
)
ON CONFLICT (name, city) DO UPDATE
SET lat = EXCLUDED.lat,
    lng = EXCLUDED.lng,
    radius_km = EXCLUDED.radius_km,
    active = EXCLUDED.active;

-- 2. Phase 1 Pilot Categories
INSERT INTO categories (id, name, slug, icon, default_service_min, active)
VALUES 
(
    'b1b2c3d4-0000-0000-0000-000000000001',
    'Salon & Barber',
    'salon-barber',
    'scissors',
    15,
    true
),
(
    'b1b2c3d4-0000-0000-0000-000000000002',
    'Medical & Pharmacy',
    'medical-pharmacy',
    'pill',
    5,
    true
)
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    icon = EXCLUDED.icon,
    default_service_min = EXCLUDED.default_service_min,
    active = EXCLUDED.active;

