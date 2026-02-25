-- Auth Service PostgreSQL bootstrap script
-- Safe execution order:
--   1) Connect to postgres/system database as superuser.
--   2) Run role creation block.
--   3) Create database (manual statement below, once).
--   4) Connect to authdb and run schema/tables block.

-- 1) Role creation (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'auth_user') THEN
        CREATE ROLE auth_user LOGIN PASSWORD 'auth_password';
    END IF;
END$$;

-- 2) Database creation
-- PostgreSQL restriction: CREATE DATABASE cannot run inside DO/function/transaction blocks.
-- Check first:
--   SELECT datname FROM pg_database WHERE datname = 'authdb';
-- If not found, run once:
--   CREATE DATABASE authdb OWNER auth_user;

-- Optional psql-only one-liner (not supported by many GUI SQL clients):
-- SELECT 'CREATE DATABASE authdb OWNER auth_user'
-- WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'authdb')\gexec

-- 3) Connect to authdb before running below section
-- \connect authdb

-- 4) Schema + tables
CREATE SCHEMA IF NOT EXISTS auth AUTHORIZATION auth_user;
SET search_path TO auth, public;

CREATE TABLE IF NOT EXISTS auth.user_profile (
    user_id VARCHAR(64) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    mobile VARCHAR(20),
    mobile_verified BOOLEAN NOT NULL DEFAULT FALSE,
    realm VARCHAR(32) NOT NULL,
    profile_status VARCHAR(32) NOT NULL DEFAULT 'INCOMPLETE',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS auth.user_session (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    device VARCHAR(255),
    ip VARCHAR(64),
    risk_level VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_session_user
        FOREIGN KEY (user_id) REFERENCES auth.user_profile(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS auth.audit_event (
    event_id BIGSERIAL PRIMARY KEY,
    actor_user_id VARCHAR(64),
    event_type VARCHAR(100) NOT NULL,
    event_payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_session_user_id
    ON auth.user_session(user_id);

CREATE INDEX IF NOT EXISTS idx_audit_event_type_created_at
    ON auth.audit_event(event_type, created_at);

CREATE INDEX IF NOT EXISTS idx_audit_event_payload_gin
    ON auth.audit_event USING GIN(event_payload);
