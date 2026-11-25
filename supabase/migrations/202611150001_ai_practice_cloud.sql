-- AI practice sessions generated through the authenticated mentor-ai function.
-- Keep this migration independent of the older staging migration, which some
-- production projects have not applied yet.
create table if not exists public.mentor_ai_practice_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  subject_id text not null,
  grade int not null check (grade between 10 and 12),
  topic text not null,
  difficulty text not null check (difficulty in ('EASY', 'MEDIUM', 'HARD')),
  question_count int not null check (question_count between 1 and 10),
  questions_snapshot jsonb not null check (jsonb_typeof(questions_snapshot) = 'array'),
  answers jsonb not null default '{}' check (jsonb_typeof(answers) = 'object'),
  current_index int not null default 0,
  state text not null default 'IN_PROGRESS' check (state in ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
  summary text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists mentor_ai_practice_sessions_owner_recent
  on public.mentor_ai_practice_sessions(user_id, state, updated_at desc);

create table if not exists public.mentor_ai_question_reports (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  session_id uuid not null references public.mentor_ai_practice_sessions(id) on delete cascade,
  question_id text not null,
  reason text not null,
  details text,
  created_at timestamptz not null default now()
);

alter table public.mentor_ai_practice_sessions enable row level security;
alter table public.mentor_ai_question_reports enable row level security;

revoke all on public.mentor_ai_practice_sessions, public.mentor_ai_question_reports
  from public, anon, authenticated;
grant select, insert, update on public.mentor_ai_practice_sessions to authenticated;
grant select, insert on public.mentor_ai_question_reports to authenticated;

drop policy if exists mentor_ai_practice_sessions_owner on public.mentor_ai_practice_sessions;
create policy mentor_ai_practice_sessions_owner on public.mentor_ai_practice_sessions
  for all to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

drop policy if exists mentor_ai_question_reports_owner on public.mentor_ai_question_reports;
create policy mentor_ai_question_reports_owner on public.mentor_ai_question_reports
  for all to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
