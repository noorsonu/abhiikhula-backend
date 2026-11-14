-- =============================================================================
-- Migration: V23__seed_all_categories.sql
-- Description: Seed full vertical categories for Lucknow pilot
-- =============================================================================

INSERT INTO categories (id, name, slug, icon, default_service_min, active)
VALUES 
(
    'b1b2c3d4-0000-0000-0000-000000000003',
    'Doctor & Clinic',
    'doctor-clinic',
    'stethoscope',
    20,
    true
),
(
    'b1b2c3d4-0000-0000-0000-000000000004',
    'Food & Chaat',
    'food-chaat',
    'utensils',
    10,
    true
),
(
    'b1b2c3d4-0000-0000-0000-000000000005',
    'Diagnostic & Lab',
    'diagnostic-lab',
    'activity',
    15,
    true
),
(
    'b1b2c3d4-0000-0000-0000-000000000006',
    'Automobile & Service',
    'auto-service',
    'wrench',
    30,
    true
),
(
    'b1b2c3d4-0000-0000-0000-000000000007',
    'General Retail & Store',
    'general-retail',
    'store',
    5,
    true
)
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    icon = EXCLUDED.icon,
    default_service_min = EXCLUDED.default_service_min,
    active = EXCLUDED.active;
