-- =============================================================================
-- Migration: V4__seed_pilot_shops.sql
-- Description: Seed 2 pilot shops for Indira Nagar zone (BRD Section 6.1)
-- =============================================================================

INSERT INTO shops (id, zone_id, category_id, name, slug, address, lat, lng, phone, status, crowd_level, verified, active, token_cap)
VALUES
(
    'd1b2c3d4-0000-0000-0000-000000000001',
    'a1b2c3d4-0000-0000-0000-000000000001',
    'b1b2c3d4-0000-0000-0000-000000000001',
    'Royal Barber & Hair Style',
    'royal-barber-indira-nagar',
    'Shop No. 12, Sector 14 Market, Indira Nagar, Lucknow',
    26.886100, 80.995100,
    '9876543210',
    'OPEN', 2, true, true, 50
),
(
    'd1b2c3d4-0000-0000-0000-000000000002',
    'a1b2c3d4-0000-0000-0000-000000000001',
    'b1b2c3d4-0000-0000-0000-000000000002',
    'Gupta Medical Store',
    'gupta-medical-indira-nagar',
    'Plot 7, Near C-Block, Indira Nagar, Lucknow',
    26.884500, 80.993500,
    '9876543211',
    'OPEN', 1, true, true, 100
)
ON CONFLICT (slug) DO NOTHING;

-- Shop hours for Royal Barber (Mon–Sun 09:00–21:30, Tuesday off)
INSERT INTO shop_hours (shop_id, weekday, open_time, close_time, closed_all_day)
SELECT 'd1b2c3d4-0000-0000-0000-000000000001',
       day_num,
       '09:00:00'::TIME,
       '21:30:00'::TIME,
       CASE WHEN day_num = 2 THEN TRUE ELSE FALSE END
FROM generate_series(1, 7) AS day_num
ON CONFLICT (shop_id, weekday) DO NOTHING;

-- Shop hours for Gupta Medical (Mon–Sun 08:00–22:00, no weekly off)
INSERT INTO shop_hours (shop_id, weekday, open_time, close_time, closed_all_day)
SELECT 'd1b2c3d4-0000-0000-0000-000000000002',
       day_num,
       '08:00:00'::TIME,
       '22:00:00'::TIME,
       FALSE
FROM generate_series(1, 7) AS day_num
ON CONFLICT (shop_id, weekday) DO NOTHING;

