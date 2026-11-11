-- =============================================================================
-- Migration: V5__create_users_schema.sql
-- Description: Users table for OTP-based Passwordless Auth (BRD Section 24)
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(20) UNIQUE,
    name            VARCHAR(100),
    role            VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    language        VARCHAR(10) NOT NULL DEFAULT 'hi',
    blocked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_phone ON users (phone);

