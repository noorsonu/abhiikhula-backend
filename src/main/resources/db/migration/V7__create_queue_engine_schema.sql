-- =============================================================================
-- Migration: V7__create_queue_engine_schema.sql
-- Description: M3 Virtual Queue Engine (Sessions, Tokens, Events)
-- BRD: AKH-BRD-001 Section 14, 29 (Virtual Queue, Token Issuance, Lifecycle)
-- =============================================================================

-- 1. Queue Sessions Table
CREATE TABLE IF NOT EXISTS queue_sessions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id              UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    session_date         DATE NOT NULL DEFAULT CURRENT_DATE,
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                         CONSTRAINT chk_session_status CHECK (status IN ('ACTIVE', 'PAUSED', 'CLOSED')),
    current_token_number INT NOT NULL DEFAULT 0,
    total_tokens_issued  INT NOT NULL DEFAULT 0,
    opened_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at            TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Partial unique index: A shop can only have ONE active/paused queue session at a time
CREATE UNIQUE INDEX IF NOT EXISTS uk_shop_active_session 
    ON queue_sessions (shop_id) 
    WHERE status IN ('ACTIVE', 'PAUSED');

CREATE INDEX IF NOT EXISTS idx_queue_sessions_shop_date 
    ON queue_sessions (shop_id, session_date);

-- 2. Tokens Table
CREATE TABLE IF NOT EXISTS tokens (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id           UUID NOT NULL REFERENCES queue_sessions(id) ON DELETE CASCADE,
    shop_id              UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id              UUID REFERENCES users(id) ON DELETE SET NULL,
    token_number         INT NOT NULL,
    display_token        VARCHAR(20) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'WAITING'
                         CONSTRAINT chk_token_status CHECK (status IN ('WAITING', 'CALLED', 'SERVING', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    source               VARCHAR(30) NOT NULL DEFAULT 'APP',
    customer_name        VARCHAR(100),
    customer_phone       VARCHAR(20) NOT NULL,
    estimated_wait_min   INT NOT NULL DEFAULT 15,
    joined_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    called_at            TIMESTAMPTZ,
    served_at            TIMESTAMPTZ,
    completed_at         TIMESTAMPTZ,
    cancelled_at         TIMESTAMPTZ,
    cancellation_reason  TEXT,
    CONSTRAINT uk_tokens_session_number UNIQUE (session_id, token_number)
);

CREATE INDEX IF NOT EXISTS idx_tokens_shop_status ON tokens (shop_id, status);
CREATE INDEX IF NOT EXISTS idx_tokens_user_status ON tokens (user_id, status);
CREATE INDEX IF NOT EXISTS idx_tokens_session_status ON tokens (session_id, status);

-- 3. Token Events Table (Immutable Audit)
CREATE TABLE IF NOT EXISTS token_events (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id             UUID NOT NULL REFERENCES tokens(id) ON DELETE CASCADE,
    from_status          VARCHAR(20) NOT NULL,
    to_status            VARCHAR(20) NOT NULL,
    actor_user_id        UUID REFERENCES users(id) ON DELETE SET NULL,
    note                 TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_token_events_token_id ON token_events (token_id, created_at DESC);

