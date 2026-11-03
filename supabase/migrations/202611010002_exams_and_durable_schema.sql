begin;

-- ============================================================================
-- AI Mentor - Durable Assessment & Exam Schema Migration
-- Versioned migration: 202611010002_exams_and_durable_schema.sql
-- Adds missing durable app structures required by existing product data:
-- - public.mentor_exams: Midterm and final exam paper definitions
-- - public.mentor_exam_sessions: Student examination attempts and scoring
-- - public.mentor_role_audit_logs: Administrative role modification audit trail
-- Follows strict RLS, private secrets, and idempotent creation rules.
-- ============================================================================

create table if not exists public.mentor_exams (
  id text primary key,
  title text not null,
  subject_id text not null,
  grade int not null check (grade between 10 and 12),
  exam_type text not null check (exam_type in ('MIDTERM', 'FINAL')),
  semester int not null check (semester in (1, 2)),
  duration_minutes int not null check (duration_minutes between 5 and 300),
  question_ids text not null,
  total_points int not null default 0,
  question_count int not null default 0,
  status text not null default 'DRAFT' check (status in ('DRAFT', 'PUBLISHED')),
  version int not null default 1,
  created_by uuid references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_mentor_exams_filter on public.mentor_exams(subject_id, grade, exam_type, semester, status);

create table if not exists public.mentor_exam_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  exam_id text not null references public.mentor_exams(id),
  subject_id text not null,
  grade int not null check (grade between 10 and 12),
  exam_title text not null,
  exam_type text not null,
  semester int not null,
  duration_minutes int not null,
  question_snapshots jsonb not null check (jsonb_typeof(question_snapshots) = 'array'),
  answers jsonb not null default '{}',
  state text not null default 'IN_PROGRESS' check (state in ('IN_PROGRESS', 'SUBMITTED', 'TIMED_OUT')),
  score numeric not null default 0,
  score_10 numeric not null default 0,
  raw_score int not null default 0,
  total_points int not null default 0,
  correct_count int not null default 0,
  total_count int not null default 0,
  started_at timestamptz not null default now(),
  deadline_at timestamptz not null,
  submitted_at timestamptz,
  revision int not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_mentor_exam_sessions_user on public.mentor_exam_sessions(user_id, exam_id, state, started_at);

create table if not exists public.mentor_role_audit_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  previous_role text not null,
  new_role text not null,
  source text not null default 'CLI_MANAGE_ROLES',
  changed_at timestamptz not null default now()
);

-- Row Level Security (RLS)
alter table public.mentor_exams enable row level security;
alter table public.mentor_exam_sessions enable row level security;
alter table public.mentor_role_audit_logs enable row level security;

revoke all on public.mentor_exams, public.mentor_exam_sessions, public.mentor_role_audit_logs from public, anon, authenticated;

-- Minimal table grants consistent with RLS
grant select on public.mentor_exams to authenticated;
grant select, insert, update on public.mentor_exam_sessions to authenticated;
-- public.mentor_role_audit_logs remains strictly private: no grants to anon or authenticated

-- Ensure no default PUBLIC execute on privileged functions in public schema
alter default privileges in schema public revoke execute on functions from public;

-- Students may view published exams
create policy mentor_exams_read on public.mentor_exams
  for select to authenticated
  using (status = 'PUBLISHED');

-- Students strictly access only their own exam sessions
create policy mentor_exam_sessions_owner on public.mentor_exam_sessions
  for all to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

commit;
