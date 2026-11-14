-- =============================================================================
-- Migration: V22__remove_dummy_pilot_shops.sql
-- Description: Permanently remove dummy pilot shops (Royal Barber & Gupta Medical)
-- =============================================================================

DELETE FROM shop_hours WHERE shop_id IN ('d1b2c3d4-0000-0000-0000-000000000001', 'd1b2c3d4-0000-0000-0000-000000000002');
DELETE FROM tokens WHERE shop_id IN ('d1b2c3d4-0000-0000-0000-000000000001', 'd1b2c3d4-0000-0000-0000-000000000002');
DELETE FROM queue_sessions WHERE shop_id IN ('d1b2c3d4-0000-0000-0000-000000000001', 'd1b2c3d4-0000-0000-0000-000000000002');
DELETE FROM shop_status_events WHERE shop_id IN ('d1b2c3d4-0000-0000-0000-000000000001', 'd1b2c3d4-0000-0000-0000-000000000002');
DELETE FROM reviews WHERE shop_id IN ('d1b2c3d4-0000-0000-0000-000000000001', 'd1b2c3d4-0000-0000-0000-000000000002');
DELETE FROM shop_claims WHERE shop_id IN ('d1b2c3d4-0000-0000-0000-000000000001', 'd1b2c3d4-0000-0000-0000-000000000002');
DELETE FROM subscriptions WHERE shop_id IN ('d1b2c3d4-0000-0000-0000-000000000001', 'd1b2c3d4-0000-0000-0000-000000000002');
DELETE FROM featured_slots WHERE shop_id IN ('d1b2c3d4-0000-0000-0000-000000000001', 'd1b2c3d4-0000-0000-0000-000000000002');
DELETE FROM disputes WHERE shop_id IN ('d1b2c3d4-0000-0000-0000-000000000001', 'd1b2c3d4-0000-0000-0000-000000000002');
DELETE FROM shops WHERE id IN ('d1b2c3d4-0000-0000-0000-000000000001', 'd1b2c3d4-0000-0000-0000-000000000002')
   OR slug IN ('royal-barber-indira-nagar', 'gupta-medical-indira-nagar');

