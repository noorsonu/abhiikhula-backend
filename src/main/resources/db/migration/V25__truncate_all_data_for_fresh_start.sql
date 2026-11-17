-- =============================================================================
-- Migration: V25__truncate_all_data_for_fresh_start.sql
-- Description: Complete clean slate for fresh production restart
-- =============================================================================

TRUNCATE TABLE analytics_events, disputes, featured_slots, invoices, notifications, 
               queue_sessions, reviews, shop_claims, shop_hours, shop_status_events, 
               subscriptions, tokens, shops, users CASCADE;
