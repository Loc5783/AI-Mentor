begin;

-- ============================================================================
-- AI Mentor - Private Staging Schema for Legacy SQLite Cutover Archive
-- Versioned migration: 202611010003_private_staging_schema.sql
-- Idempotent, private staging schema for all durable SQLite rows.
-- Preserves original legacy IDs/ownership and JSONB payload.
-- NO anon/authenticated grants or Data API exposure.
-- Preserves account email only in private user mapping for eventual verified identity claim.
-- Excludes raw credentials, transient sessions, recovery codes, rate counters, transient events.
-- ============================================================================

create schema if not exists staging;

-- 1. Private user mapping for eventual verified identity claim (least privilege)
-- Preserves account email, username, and role; never stages raw credentials or active sessions.
create table if not exists staging.user_mappings (
  legacy_user_id bigint primary key,
  email text not null,
  username text not null,
  nickname text not null default '',
  role text not null default 'STUDENT',
  rank_tier text not null default 'BRONZE',
  grade int not null default 0,
  target_auth_uid uuid references auth.users(id) on delete set null,
  claimed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_staging_user_mappings_email on staging.user_mappings(email);
create index if not exists idx_staging_user_mappings_target on staging.user_mappings(target_auth_uid);

-- 2. Durable SQLite rows archive
-- Stores all durable progress and catalog records with original legacy IDs, ownership, and JSONB payload.
-- Excludes ephemeral runtime state (sessions, events, idempotency, ephemeral recovery, rate limits, AI provider usage).
create table if not exists staging.durable_rows (
  table_name text not null,
  legacy_id text not null,
  legacy_user_id bigint,
  payload jsonb not null check (jsonb_typeof(payload) = 'object'),
  staged_at timestamptz not null default now(),
  primary key (table_name, legacy_id)
);

create index if not exists idx_staging_durable_rows_lookup on staging.durable_rows(table_name, legacy_user_id);

-- 3. Strict access control: NO anon/authenticated grants or Data API exposure.
-- Staging schema is completely private to database administrators.
revoke all on schema staging from public, anon, authenticated;
revoke all on all tables in schema staging from public, anon, authenticated;
revoke all on all sequences in schema staging from public, anon, authenticated;
revoke all on all routines in schema staging from public, anon, authenticated;

alter default privileges in schema staging revoke all on tables from public, anon, authenticated;
alter default privileges in schema staging revoke all on sequences from public, anon, authenticated;
alter default privileges in schema staging revoke all on routines from public, anon, authenticated;

commit;
