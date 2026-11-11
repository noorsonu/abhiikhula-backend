-- =============================================================================
-- Migration: V13__allow_null_customer_phone_for_walkin.sql
-- Description: Allow NULL customer_phone in tokens table for offline walk-in customers
-- BRD: AKH-BRD-001 Section 14, 18 (FR-704 Walk-in Customer Queue Support)
-- =============================================================================

ALTER TABLE tokens ALTER COLUMN customer_phone DROP NOT NULL;

