begin;

-- ==========================================
-- 1. XƯỞNG PHÁ MỆNH ĐỀ (MISCONCEPTION BUSTER)
-- ==========================================
create table if not exists public.mentor_misconception_verifications (
  id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  question_id text not null,
  subject_id text not null,
  grade int not null,
  misconception_reason text not null,
  reason_type text not null check (reason_type in ('PRE_AUTHORED', 'CUSTOM')),
  ai_hint text,
  ai_counterexample text,
  prop1_prompt text not null,
  prop1_correct boolean not null,
  prop1_user_ans boolean,
  prop2_prompt text not null,
  prop2_correct boolean not null,
  prop2_user_ans boolean,
  verification_status text not null default 'NEEDS_REVIEW' check (verification_status in ('NEEDS_REVIEW', 'RESOLVED')),
  verified_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.mentor_misconception_verifications enable row level security;
revoke all on public.mentor_misconception_verifications from public, anon;
grant select, insert, update on public.mentor_misconception_verifications to authenticated;

drop policy if exists misconception_verifications_owner on public.mentor_misconception_verifications;
create policy misconception_verifications_owner on public.mentor_misconception_verifications
  for all to authenticated using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- ==========================================
-- 2. AI EXAMPACK FOUNDRY
-- ==========================================
create table if not exists public.mentor_exam_packs (
  id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  subject_id text not null,
  grade int not null,
  source_files jsonb not null default '[]'::jsonb,
  status text not null default 'DRAFT' check (status in ('DRAFT', 'APPROVED')),
  question_count int not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.mentor_exam_packs enable row level security;
revoke all on public.mentor_exam_packs from public, anon;
grant select, insert, update, delete on public.mentor_exam_packs to authenticated;

drop policy if exists exam_packs_owner on public.mentor_exam_packs;
create policy exam_packs_owner on public.mentor_exam_packs
  for all to authenticated using (user_id = auth.uid())
  with check (user_id = auth.uid());

create table if not exists public.mentor_exam_pack_questions (
  id text primary key,
  pack_id text not null references public.mentor_exam_packs(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  prompt text not null,
  options jsonb not null,
  correct_option text,
  explanation text,
  topic text,
  difficulty text not null default 'MEDIUM',
  source_file_name text not null,
  source_page_index int not null default 1,
  is_approved boolean not null default false,
  is_answer_verified boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.mentor_exam_pack_questions enable row level security;
revoke all on public.mentor_exam_pack_questions from public, anon;
grant select, insert, update, delete on public.mentor_exam_pack_questions to authenticated;

drop policy if exists exam_pack_questions_owner on public.mentor_exam_pack_questions;
create policy exam_pack_questions_owner on public.mentor_exam_pack_questions
  for all to authenticated using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- ==========================================
-- 3. AI TEAMOS (DỰ ÁN NHÓM AI)
-- ==========================================
create table if not exists public.mentor_team_projects (
  id text primary key,
  title text not null,
  description text,
  owner_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.mentor_team_members (
  project_id text not null references public.mentor_team_projects(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'MEMBER' check (role in ('OWNER', 'MEMBER')),
  joined_at timestamptz not null default now(),
  primary key (project_id, user_id)
);

create table if not exists public.mentor_team_requirements (
  id text primary key,
  project_id text not null references public.mentor_team_projects(id) on delete cascade,
  title text not null,
  description text not null,
  added_by uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create table if not exists public.mentor_team_documents (
  id text primary key,
  project_id text not null references public.mentor_team_projects(id) on delete cascade,
  title text not null,
  content text not null,
  uploaded_by uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create table if not exists public.mentor_team_tasks (
  id text primary key,
  project_id text not null references public.mentor_team_projects(id) on delete cascade,
  title text not null,
  description text,
  assignee_id uuid references auth.users(id) on delete set null,
  depends_on_task_id text,
  deadline timestamptz,
  status text not null default 'TODO' check (status in ('TODO', 'IN_PROGRESS', 'DONE')),
  is_confirmed boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.mentor_team_warnings (
  id text primary key,
  project_id text not null references public.mentor_team_projects(id) on delete cascade,
  warning_type text not null check (warning_type in ('MISSING_CONTENT', 'DUPLICATION', 'CONTRADICTION', 'UNGROUNDED_CLAIM')),
  message text not null,
  doc_id text references public.mentor_team_documents(id) on delete cascade,
  location_info text not null,
  status text not null default 'NEEDS_CHECK' check (status in ('NEEDS_CHECK', 'RESOLVED')),
  created_at timestamptz not null default now()
);

-- Security RLS for TeamOS (Strict Server-Side Member Isolation)
alter table public.mentor_team_projects enable row level security;
alter table public.mentor_team_members enable row level security;
alter table public.mentor_team_requirements enable row level security;
alter table public.mentor_team_documents enable row level security;
alter table public.mentor_team_tasks enable row level security;
alter table public.mentor_team_warnings enable row level security;

revoke all on public.mentor_team_projects, public.mentor_team_members, public.mentor_team_requirements,
  public.mentor_team_documents, public.mentor_team_tasks, public.mentor_team_warnings from public, anon;

grant select, insert, update, delete on public.mentor_team_projects, public.mentor_team_members, public.mentor_team_requirements,
  public.mentor_team_documents, public.mentor_team_tasks, public.mentor_team_warnings to authenticated;

-- Helper function to check team membership
create or replace function public.mentor_is_team_member(p_project_id text, p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.mentor_team_members
    where project_id = p_project_id and user_id = p_user_id
  );
$$;

revoke all on function public.mentor_is_team_member(text, uuid) from public, anon;
grant execute on function public.mentor_is_team_member(text, uuid) to authenticated;

-- RLS Policies
drop policy if exists team_projects_member on public.mentor_team_projects;
create policy team_projects_member on public.mentor_team_projects
  for all to authenticated using (public.mentor_is_team_member(id, auth.uid()))
  with check (owner_id = auth.uid());

drop policy if exists team_members_member on public.mentor_team_members;
create policy team_members_member on public.mentor_team_members
  for all to authenticated using (public.mentor_is_team_member(project_id, auth.uid()))
  with check (public.mentor_is_team_member(project_id, auth.uid()) or user_id = auth.uid());

drop policy if exists team_reqs_member on public.mentor_team_requirements;
create policy team_reqs_member on public.mentor_team_requirements
  for all to authenticated using (public.mentor_is_team_member(project_id, auth.uid()))
  with check (public.mentor_is_team_member(project_id, auth.uid()));

drop policy if exists team_docs_member on public.mentor_team_documents;
create policy team_docs_member on public.mentor_team_documents
  for all to authenticated using (public.mentor_is_team_member(project_id, auth.uid()))
  with check (public.mentor_is_team_member(project_id, auth.uid()));

drop policy if exists team_tasks_member on public.mentor_team_tasks;
create policy team_tasks_member on public.mentor_team_tasks
  for all to authenticated using (public.mentor_is_team_member(project_id, auth.uid()))
  with check (public.mentor_is_team_member(project_id, auth.uid()));

drop policy if exists team_warnings_member on public.mentor_team_warnings;
create policy team_warnings_member on public.mentor_team_warnings
  for all to authenticated using (public.mentor_is_team_member(project_id, auth.uid()))
  with check (public.mentor_is_team_member(project_id, auth.uid()));

commit;
