-- PostgreSQL init script for SQL clients (DBeaver/pgAdmin/etc.)
-- Step 1: Connect as superuser to the "postgres" database and run Section A + B.
-- Step 2: Reconnect to the "authdb" database and run Section C.

-- =========================
-- Section A: Create/Update role
-- =========================
DO
$$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authservice') THEN
        CREATE ROLE authservice LOGIN PASSWORD 'AuthService@123';
    ELSE
        ALTER ROLE authservice WITH LOGIN PASSWORD 'AuthService@123';
    END IF;
END
$$;

-- =========================
-- Section B: Create database (run once)
-- =========================
-- If authdb already exists, PostgreSQL will raise duplicate_database; you can ignore that.
CREATE DATABASE authdb OWNER authservice;

-- =========================
-- Section C: Run after connecting to authdb
-- =========================
CREATE TABLE IF NOT EXISTS public.flightapp (
    user_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Optional one-time cleanup for legacy schema (drops locally stored password hashes).
-- Review and back up before running in production.
ALTER TABLE public.flightapp DROP COLUMN IF EXISTS password_hash;

CREATE INDEX IF NOT EXISTS idx_flightapp_email ON public.flightapp (email);
CREATE UNIQUE INDEX IF NOT EXISTS uq_flightapp_username_ci ON public.flightapp (LOWER(username));
CREATE UNIQUE INDEX IF NOT EXISTS uq_flightapp_email_ci ON public.flightapp (LOWER(email));
CREATE UNIQUE INDEX IF NOT EXISTS uq_flightapp_phone ON public.flightapp (phone);
