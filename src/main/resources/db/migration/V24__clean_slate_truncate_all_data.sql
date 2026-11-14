-- =============================================================================
-- Migration: V24__clean_slate_truncate_all_data.sql
-- Description: Complete clean slate for fresh production testing
-- =============================================================================

TRUNCATE TABLE analytics_events, disputes, featured_slots, invoices, notifications, 
               queue_sessions, reviews, shop_claims, shop_hours, shop_status_events, 
               subscriptions, tokens, shops, users CASCADE;
