begin;

-- ============================================================================
-- AI TEAMOS PRODUCTION-GRADE SCHEMA & RLS AUTHORIZATION
-- Enforces strict UUID identity, role-based access control, task dependencies,
-- cycle prevention, and idempotency.
-- ============================================================================

-- Extend the existing tables from 202611200001. Never drop team data.

-- 1. Projects
create table if not exists public.mentor_team_projects (
  id text primary key,
  title text not null check (char_length(trim(title)) between 1 and 200),
  description text check (description is null or char_length(description) <= 2000),
  owner_id uuid not null references auth.users(id) on delete cascade,
  version int not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 2. Members
create table if not exists public.mentor_team_members (
  project_id text not null references public.mentor_team_projects(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'MEMBER' check (role in ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER')),
  status text not null default 'ACTIVE' check (status in ('INVITED', 'ACTIVE', 'REMOVED')),
  joined_at timestamptz not null default now(),
  removed_at timestamptz,
  primary key (project_id, user_id)
);

-- 3. Teacher Requirements
create table if not exists public.mentor_team_requirements (
  id text primary key,
  project_id text not null references public.mentor_team_projects(id) on delete cascade,
  title text not null check (char_length(trim(title)) between 1 and 200),
  description text not null check (char_length(description) <= 4000),
  status text not null default 'OPEN' check (status in ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'DISMISSED')),
  priority text not null default 'MEDIUM' check (priority in ('LOW', 'MEDIUM', 'HIGH')),
  added_by uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 4. Documents
create table if not exists public.mentor_team_documents (
  id text primary key,
  project_id text not null references public.mentor_team_projects(id) on delete cascade,
  title text not null check (char_length(trim(title)) between 1 and 200),
  content text not null,
  uploaded_by uuid not null references auth.users(id) on delete cascade,
  updated_by uuid references auth.users(id) on delete set null,
  version int not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 5. Tasks
create table if not exists public.mentor_team_tasks (
  id text primary key,
  project_id text not null references public.mentor_team_projects(id) on delete cascade,
  title text not null check (char_length(trim(title)) between 1 and 200),
  description text check (description is null or char_length(description) <= 4000),
  assignee_id uuid references auth.users(id) on delete set null,
  deadline timestamptz,
  status text not null default 'TODO' check (status in ('TODO', 'IN_PROGRESS', 'DONE')),
  priority text not null default 'MEDIUM' check (priority in ('LOW', 'MEDIUM', 'HIGH')),
  source text not null default 'HUMAN' check (source in ('HUMAN', 'AI_SUGGESTED')),
  confirmation_status text not null default 'CONFIRMED' check (confirmation_status in ('PENDING_CONFIRMATION', 'CONFIRMED', 'DISMISSED')),
  confirmed_by uuid references auth.users(id) on delete set null,
  confirmed_at timestamptz,
  client_request_id text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (project_id, client_request_id)
);

-- 6. Task Dependencies
create table if not exists public.mentor_team_task_dependencies (
  task_id text not null references public.mentor_team_tasks(id) on delete cascade,
  depends_on_task_id text not null references public.mentor_team_tasks(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (task_id, depends_on_task_id),
  check (task_id <> depends_on_task_id)
);

-- 7. AI Content Warnings
create table if not exists public.mentor_team_warnings (
  id text primary key,
  project_id text not null references public.mentor_team_projects(id) on delete cascade,
  warning_type text not null check (warning_type in ('MISSING_CONTENT', 'DUPLICATION', 'CONTRADICTION', 'UNGROUNDED_CLAIM')),
  message text not null,
  doc_id text references public.mentor_team_documents(id) on delete cascade,
  location_info text not null,
  excerpt text,
  evidence_ref text,
  explanation text,
  status text not null default 'NEEDS_CHECK' check (status in ('NEEDS_CHECK', 'RESOLVED', 'DISMISSED')),
  created_by_ai boolean not null default true,
  reviewed_by uuid references auth.users(id) on delete set null,
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.mentor_team_projects
  add column if not exists version int not null default 1;
alter table public.mentor_team_members
  add column if not exists status text not null default 'ACTIVE',
  add column if not exists removed_at timestamptz;
alter table public.mentor_team_members
  drop constraint if exists mentor_team_members_role_check;
alter table public.mentor_team_members
  add constraint mentor_team_members_role_check check (role in ('OWNER','ADMIN','MEMBER','VIEWER'));
alter table public.mentor_team_requirements
  add column if not exists status text not null default 'OPEN',
  add column if not exists priority text not null default 'MEDIUM',
  add column if not exists updated_at timestamptz not null default now();
alter table public.mentor_team_documents
  add column if not exists updated_by uuid references auth.users(id) on delete set null,
  add column if not exists version int not null default 1,
  add column if not exists updated_at timestamptz not null default now();
alter table public.mentor_team_tasks
  add column if not exists priority text not null default 'MEDIUM',
  add column if not exists source text not null default 'HUMAN',
  add column if not exists confirmation_status text not null default 'CONFIRMED',
  add column if not exists confirmed_by uuid references auth.users(id) on delete set null,
  add column if not exists confirmed_at timestamptz,
  add column if not exists client_request_id text;
create unique index if not exists mentor_team_tasks_request_unique
  on public.mentor_team_tasks(project_id, client_request_id);
alter table public.mentor_team_warnings
  add column if not exists excerpt text,
  add column if not exists evidence_ref text,
  add column if not exists explanation text,
  add column if not exists created_by_ai boolean not null default true,
  add column if not exists reviewed_by uuid references auth.users(id) on delete set null,
  add column if not exists reviewed_at timestamptz,
  add column if not exists updated_at timestamptz not null default now();
alter table public.mentor_team_warnings
  drop constraint if exists mentor_team_warnings_status_check;
alter table public.mentor_team_warnings
  add constraint mentor_team_warnings_status_check check (status in ('NEEDS_CHECK','RESOLVED','DISMISSED'));

-- Indexes for foreign keys and RLS evaluation performance
create index if not exists nontrivial_idx_team_members_user on public.mentor_team_members(user_id, status);
create index if not exists nontrivial_idx_team_projects_owner on public.mentor_team_projects(owner_id);
create index if not exists nontrivial_idx_team_requirements_proj on public.mentor_team_requirements(project_id);
create index if not exists nontrivial_idx_team_documents_proj on public.mentor_team_documents(project_id);
create index if not exists nontrivial_idx_team_tasks_proj on public.mentor_team_tasks(project_id, status);
create index if not exists nontrivial_idx_team_warnings_proj on public.mentor_team_warnings(project_id, status);

-- Trigger: Cycle Prevention & Project Isolation on Task Dependencies
create or replace function public.mentor_check_task_dependency()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
  v_task_proj text;
  v_dep_proj text;
  v_cycle_detected boolean;
begin
  -- Validate same project
  select project_id into v_task_proj from public.mentor_team_tasks where id = new.task_id;
  select project_id into v_dep_proj from public.mentor_team_tasks where id = new.depends_on_task_id;
  if v_task_proj is null or v_dep_proj is null or v_task_proj <> v_dep_proj then
    raise exception 'CROSS_PROJECT_DEPENDENCY_FORBIDDEN';
  end if;

  -- Validate cycle prevention (new.depends_on_task_id must not already depend on new.task_id)
  with recursive chain as (
    select depends_on_task_id from public.mentor_team_task_dependencies where task_id = new.depends_on_task_id
    union
    select d.depends_on_task_id from public.mentor_team_task_dependencies d
    join chain c on d.task_id = c.depends_on_task_id
  )
  select exists (select 1 from chain where depends_on_task_id = new.task_id) into v_cycle_detected;

  if v_cycle_detected then
    raise exception 'CYCLIC_DEPENDENCY_DETECTED';
  end if;

  return new;
end;
$$;

drop trigger if exists trg_mentor_check_task_dependency on public.mentor_team_task_dependencies;
create trigger trg_mentor_check_task_dependency
  before insert or update on public.mentor_team_task_dependencies
  for each row execute function public.mentor_check_task_dependency();

-- Helper Security Functions (SECURITY DEFINER with strict search_path = '')
create or replace function public.mentor_is_team_active_member(p_project_id text, p_user_id uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.mentor_team_members
    where project_id = p_project_id and user_id = p_user_id and status = 'ACTIVE'
  );
$$;

create or replace function public.mentor_is_team_manager(p_project_id text, p_user_id uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.mentor_team_members
    where project_id = p_project_id and user_id = p_user_id and status = 'ACTIVE' and role in ('OWNER', 'ADMIN')
  );
$$;

create or replace function public.mentor_get_team_role(p_project_id text, p_user_id uuid)
returns text language sql stable security definer set search_path = '' as $$
  select role from public.mentor_team_members
  where project_id = p_project_id and user_id = p_user_id and status = 'ACTIVE'
  limit 1;
$$;

revoke all on function public.mentor_is_team_active_member(text, uuid) from public, anon;
grant execute on function public.mentor_is_team_active_member(text, uuid) to authenticated;

revoke all on function public.mentor_is_team_manager(text, uuid) from public, anon;
grant execute on function public.mentor_is_team_manager(text, uuid) to authenticated;

revoke all on function public.mentor_get_team_role(text, uuid) from public, anon;
grant execute on function public.mentor_get_team_role(text, uuid) to authenticated;

-- Automatic project creator becomes OWNER member
create or replace function public.mentor_add_team_owner()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.mentor_team_members(project_id, user_id, role, status)
  values (new.id, new.owner_id, 'OWNER', 'ACTIVE')
  on conflict (project_id, user_id) do update set role = 'OWNER', status = 'ACTIVE', removed_at = null;
  return new;
end;
$$;

drop trigger if exists mentor_team_owner_created on public.mentor_team_projects;
drop trigger if exists trg_mentor_team_owner_created on public.mentor_team_projects;
create trigger trg_mentor_team_owner_created
  after insert on public.mentor_team_projects
  for each row execute function public.mentor_add_team_owner();

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================
-- The project owner is authoritative; an ADMIN cannot transfer ownership by
-- changing owner_id or the owner's membership row.
create or replace function public.mentor_protect_team_owner()
returns trigger language plpgsql set search_path = '' as $$
begin
  if tg_table_name = 'mentor_team_projects' and old.owner_id is distinct from new.owner_id then
    raise exception 'TEAM_OWNER_IMMUTABLE';
  end if;
  return new;
end;
$$;
drop trigger if exists trg_mentor_protect_team_owner on public.mentor_team_projects;
create trigger trg_mentor_protect_team_owner
  before update on public.mentor_team_projects
  for each row execute function public.mentor_protect_team_owner();

create or replace function public.mentor_protect_owner_membership()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if old.user_id = (select owner_id from public.mentor_team_projects where id = old.project_id)
    and (new.role <> 'OWNER' or new.status <> 'ACTIVE'
      or new.user_id is distinct from old.user_id
      or new.project_id is distinct from old.project_id) then
    raise exception 'TEAM_OWNER_MEMBERSHIP_REQUIRED';
  end if;
  return new;
end;
$$;
drop trigger if exists trg_mentor_protect_owner_membership on public.mentor_team_members;
create trigger trg_mentor_protect_owner_membership
  before update on public.mentor_team_members
  for each row execute function public.mentor_protect_owner_membership();

-- Remove the broad policies from the original schema before adding role-aware
-- policies. PostgreSQL ORs permissive policies, so leaving them would bypass
-- the VIEWER and manager restrictions below.
drop policy if exists team_projects_member on public.mentor_team_projects;
drop policy if exists team_members_member on public.mentor_team_members;
drop policy if exists team_reqs_member on public.mentor_team_requirements;
drop policy if exists team_docs_member on public.mentor_team_documents;
drop policy if exists team_tasks_member on public.mentor_team_tasks;
drop policy if exists team_warnings_member on public.mentor_team_warnings;
alter table public.mentor_team_projects enable row level security;
alter table public.mentor_team_members enable row level security;
alter table public.mentor_team_requirements enable row level security;
alter table public.mentor_team_documents enable row level security;
alter table public.mentor_team_tasks enable row level security;
alter table public.mentor_team_task_dependencies enable row level security;
alter table public.mentor_team_warnings enable row level security;

revoke all on public.mentor_team_projects, public.mentor_team_members, public.mentor_team_requirements,
  public.mentor_team_documents, public.mentor_team_tasks, public.mentor_team_task_dependencies,
  public.mentor_team_warnings from public, anon;

grant select, insert, update, delete on public.mentor_team_projects, public.mentor_team_members,
  public.mentor_team_requirements, public.mentor_team_documents, public.mentor_team_tasks,
  public.mentor_team_task_dependencies, public.mentor_team_warnings to authenticated;

-- 1. Projects
create policy team_projects_select on public.mentor_team_projects
  for select to authenticated
  using (owner_id = auth.uid() or public.mentor_is_team_active_member(id, auth.uid()));

create policy team_projects_insert on public.mentor_team_projects
  for insert to authenticated
  with check (owner_id = auth.uid());

create policy team_projects_update on public.mentor_team_projects
  for update to authenticated
  using (public.mentor_is_team_manager(id, auth.uid()))
  with check (public.mentor_is_team_manager(id, auth.uid()));

create policy team_projects_delete on public.mentor_team_projects
  for delete to authenticated
  using (owner_id = auth.uid());

-- 2. Members
-- Self-join strictly eliminated: only OWNER or ADMIN can insert members
create policy team_members_select on public.mentor_team_members
  for select to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()) or user_id = auth.uid());

create policy team_members_insert on public.mentor_team_members
  for insert to authenticated
  with check (public.mentor_is_team_manager(project_id, auth.uid())
    and (role <> 'OWNER' or exists (
      select 1 from public.mentor_team_projects p
      where p.id = project_id and p.owner_id = auth.uid()
    )));

create policy team_members_update on public.mentor_team_members
  for update to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid())
    and (role <> 'OWNER' or user_id = auth.uid()))
  with check (public.mentor_is_team_manager(project_id, auth.uid())
    and (role <> 'OWNER' or exists (
      select 1 from public.mentor_team_projects p
      where p.id = project_id and p.owner_id = auth.uid()
    )));

create policy team_members_delete on public.mentor_team_members
  for delete to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()) and role <> 'OWNER');

-- 3. Requirements
create policy team_reqs_select on public.mentor_team_requirements
  for select to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()));

create policy team_reqs_insert on public.mentor_team_requirements
  for insert to authenticated
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

create policy team_reqs_update on public.mentor_team_requirements
  for update to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER')
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

create policy team_reqs_delete on public.mentor_team_requirements
  for delete to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()) or (added_by = auth.uid() and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER'));

-- 4. Documents
create policy team_docs_select on public.mentor_team_documents
  for select to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()));

create policy team_docs_insert on public.mentor_team_documents
  for insert to authenticated
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

create policy team_docs_update on public.mentor_team_documents
  for update to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER')
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

create policy team_docs_delete on public.mentor_team_documents
  for delete to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()) or (uploaded_by = auth.uid() and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER'));

-- 5. Tasks
create policy team_tasks_select on public.mentor_team_tasks
  for select to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()));

create policy team_tasks_insert on public.mentor_team_tasks
  for insert to authenticated
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

create policy team_tasks_update on public.mentor_team_tasks
  for update to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER')
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

create policy team_tasks_delete on public.mentor_team_tasks
  for delete to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()));

-- 6. Dependencies
create policy team_task_deps_select on public.mentor_team_task_dependencies
  for select to authenticated
  using (exists (
    select 1 from public.mentor_team_tasks t
    where t.id = task_id and public.mentor_is_team_active_member(t.project_id, auth.uid())
  ));

create policy team_task_deps_insert on public.mentor_team_task_dependencies
  for insert to authenticated
  with check (exists (
    select 1 from public.mentor_team_tasks t
    where t.id = task_id and public.mentor_is_team_active_member(t.project_id, auth.uid())
    and public.mentor_get_team_role(t.project_id, auth.uid()) <> 'VIEWER'
  ));

create policy team_task_deps_delete on public.mentor_team_task_dependencies
  for delete to authenticated
  using (exists (
    select 1 from public.mentor_team_tasks t
    where t.id = task_id and public.mentor_is_team_active_member(t.project_id, auth.uid())
    and public.mentor_get_team_role(t.project_id, auth.uid()) <> 'VIEWER'
  ));

-- 7. Warnings
create policy team_warnings_select on public.mentor_team_warnings
  for select to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()));

create policy team_warnings_insert on public.mentor_team_warnings
  for insert to authenticated
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

create policy team_warnings_update on public.mentor_team_warnings
  for update to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER')
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

create policy team_warnings_delete on public.mentor_team_warnings
  for delete to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()));

commit;
