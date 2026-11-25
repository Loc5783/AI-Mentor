begin;

-- ============================================================================
-- AI TEAMOS PRODUCTION-GRADE SCHEMA & RLS AUTHORIZATION (MIGRATION 202611220002)
-- Enforces strict UUID identity, role-based access control (OWNER/ADMIN/MEMBER/VIEWER),
-- task dependencies, cycle prevention, idempotency, and anti-privilege escalation.
-- ============================================================================

-- 0. PRECONDITION CHECK: Fail-closed on legacy integer tables with data
do $$
declare
  v_col_type text;
  v_legacy_count int;
begin
  if exists (
    select 1 from information_schema.tables
    where table_schema = 'public' and table_name = 'mentor_team_projects'
  ) then
    select data_type into v_col_type
    from information_schema.columns
    where table_schema = 'public' and table_name = 'mentor_team_projects' and column_name = 'owner_id';

    if v_col_type in ('integer', 'bigint', 'smallint') then
      select count(*) into v_legacy_count from public.mentor_team_projects;
      if v_legacy_count > 0 then
        raise exception 'PRECONDITION_FAILED: Legacy mentor_team_projects contains % records with integer IDs that cannot be automatically mapped to auth.users UUIDs without manual migration.', v_legacy_count;
      else
        -- Empty legacy tables can be safely replaced
        drop table if exists public.mentor_team_task_dependencies cascade;
        drop table if exists public.mentor_team_warnings cascade;
        drop table if exists public.mentor_team_tasks cascade;
        drop table if exists public.mentor_team_documents cascade;
        drop table if exists public.mentor_team_requirements cascade;
        drop table if exists public.mentor_team_members cascade;
        drop table if exists public.mentor_team_projects cascade;
      end if;
    end if;
  end if;
end $$;

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
  updated_at timestamptz not null default now()
);

create unique index if not exists idx_team_tasks_client_req on public.mentor_team_tasks(project_id, client_request_id)
  where client_request_id is not null;

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

-- Indexes for performance
create index if not exists nontrivial_idx_team_members_user on public.mentor_team_members(user_id, status);
create index if not exists nontrivial_idx_team_projects_owner on public.mentor_team_projects(owner_id);
create index if not exists nontrivial_idx_team_requirements_proj on public.mentor_team_requirements(project_id);
create index if not exists nontrivial_idx_team_documents_proj on public.mentor_team_documents(project_id);
create index if not exists nontrivial_idx_team_tasks_proj on public.mentor_team_tasks(project_id, status);
create index if not exists nontrivial_idx_team_warnings_proj on public.mentor_team_warnings(project_id, status);

-- Backfill dependencies from legacy column if exists
do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'mentor_team_tasks' and column_name = 'depends_on_task_id'
  ) then
    execute '
      insert into public.mentor_team_task_dependencies (task_id, depends_on_task_id)
      select id, depends_on_task_id from public.mentor_team_tasks
      where depends_on_task_id is not null and depends_on_task_id <> id
      on conflict (task_id, depends_on_task_id) do nothing;
    ';
  end if;
end $$;

-- ============================================================================
-- HELPER SECURITY DEFINER FUNCTIONS
-- ============================================================================
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

-- ============================================================================
-- ACTOR IDENTITY & PRIVILEGE ESCALATION ENFORCEMENT TRIGGERS
-- ============================================================================

-- Trigger: Projects Actor & Ownership Guard
create or replace function public.mentor_guard_team_project()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if tg_op = 'INSERT' then
    if auth.uid() is not null then
      new.owner_id := auth.uid();
    end if;
  elsif tg_op = 'UPDATE' then
    if new.owner_id <> old.owner_id then
      raise exception 'CANNOT_CHANGE_OWNER_DIRECTLY: Use mentor_transfer_team_ownership RPC';
    end if;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_mentor_guard_team_project on public.mentor_team_projects;
create trigger trg_mentor_guard_team_project
  before insert or update on public.mentor_team_projects
  for each row execute function public.mentor_guard_team_project();

-- Trigger: Automatic Project Creator becomes OWNER
create or replace function public.mentor_add_team_owner()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.mentor_team_members(project_id, user_id, role, status)
  values (new.id, new.owner_id, 'OWNER', 'ACTIVE')
  on conflict (project_id, user_id) do update set role = 'OWNER', status = 'ACTIVE', removed_at = null;
  return new;
end;
$$;

drop trigger if exists trg_mentor_team_owner_created on public.mentor_team_projects;
create trigger trg_mentor_team_owner_created
  after insert on public.mentor_team_projects
  for each row execute function public.mentor_add_team_owner();

-- Trigger: Anti-Privilege Escalation on Members
create or replace function public.mentor_guard_team_members()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
  v_caller_role text;
  v_proj_owner uuid;
begin
  select owner_id into v_proj_owner from public.mentor_team_projects where id = coalesce(new.project_id, old.project_id);

  if tg_op = 'INSERT' then
    -- Only allow OWNER role if caller is project owner or through project creation trigger
    if new.role = 'OWNER' and new.user_id <> v_proj_owner then
      raise exception 'CANNOT_ASSIGN_OWNER_ROLE: Only one owner allowed via transfer';
    end if;
  elsif tg_op = 'UPDATE' then
    -- Cannot demote or promote to OWNER directly
    if old.role = 'OWNER' and new.role <> 'OWNER' then
      raise exception 'CANNOT_DEMOTE_OWNER: Only owner transfer RPC can demote owner';
    end if;
    if new.role = 'OWNER' and old.role <> 'OWNER' then
      raise exception 'CANNOT_ESCALATE_TO_OWNER: Only owner transfer RPC can assign owner';
    end if;
    -- Users cannot change their own role (self-promotion)
    if auth.uid() is not null and old.user_id = auth.uid() and new.role <> old.role then
      raise exception 'CANNOT_SELF_PROMOTE: Users cannot change their own role';
    end if;
    -- ADMIN cannot modify OWNER
    if old.role = 'OWNER' and auth.uid() is not null and auth.uid() <> old.user_id then
      raise exception 'ADMIN_CANNOT_MODIFY_OWNER: Admin cannot modify project owner';
    end if;
  elsif tg_op = 'DELETE' then
    if old.role = 'OWNER' then
      raise exception 'CANNOT_DELETE_OWNER: Project owner cannot be removed';
    end if;
    if auth.uid() is not null and auth.uid() <> old.user_id then
      select role into v_caller_role from public.mentor_team_members
      where project_id = old.project_id and user_id = auth.uid() and status = 'ACTIVE';
      if v_caller_role = 'ADMIN' and old.role = 'OWNER' then
        raise exception 'ADMIN_CANNOT_DELETE_OWNER';
      end if;
    end if;
  end if;

  return coalesce(new, old);
end;
$$;

drop trigger if exists trg_mentor_guard_team_members on public.mentor_team_members;
create trigger trg_mentor_guard_team_members
  before insert or update or delete on public.mentor_team_members
  for each row execute function public.mentor_guard_team_members();

-- Trigger: Requirements Actor Guard
create or replace function public.mentor_guard_team_reqs()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if tg_op = 'INSERT' then
    if auth.uid() is not null then
      new.added_by := auth.uid();
    end if;
  elsif tg_op = 'UPDATE' then
    new.added_by := old.added_by;
    new.updated_at := now();
  end if;
  return new;
end;
$$;

drop trigger if exists trg_mentor_guard_team_reqs on public.mentor_team_requirements;
create trigger trg_mentor_guard_team_reqs
  before insert or update on public.mentor_team_requirements
  for each row execute function public.mentor_guard_team_reqs();

-- Trigger: Documents Actor Guard
create or replace function public.mentor_guard_team_docs()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if tg_op = 'INSERT' then
    if auth.uid() is not null then
      new.uploaded_by := auth.uid();
    end if;
  elsif tg_op = 'UPDATE' then
    new.uploaded_by := old.uploaded_by;
    if auth.uid() is not null then
      new.updated_by := auth.uid();
    end if;
    new.version := old.version + 1;
    new.updated_at := now();
  end if;
  return new;
end;
$$;

drop trigger if exists trg_mentor_guard_team_docs on public.mentor_team_documents;
create trigger trg_mentor_guard_team_docs
  before insert or update on public.mentor_team_documents
  for each row execute function public.mentor_guard_team_docs();

-- Trigger: Tasks Actor Guard & AI Confirmation Lifecycle
create or replace function public.mentor_guard_team_tasks()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if tg_op = 'INSERT' then
    -- Rule 6: AI suggestion must be PENDING_CONFIRMATION; cannot bypass directly
    if new.source = 'AI_SUGGESTED' then
      new.confirmation_status := 'PENDING_CONFIRMATION';
      new.confirmed_by := null;
      new.confirmed_at := null;
    elsif new.source = 'HUMAN' and new.confirmation_status = 'CONFIRMED' then
      if auth.uid() is not null then
        new.confirmed_by := auth.uid();
        new.confirmed_at := now();
      end if;
    end if;
  elsif tg_op = 'UPDATE' then
    -- Transitioning from PENDING_CONFIRMATION to CONFIRMED requires authorized human confirmation
    if old.confirmation_status = 'PENDING_CONFIRMATION' and new.confirmation_status = 'CONFIRMED' then
      if auth.uid() is not null then
        new.confirmed_by := auth.uid();
        new.confirmed_at := now();
      end if;
    else
      -- Cannot forge confirmed_by
      new.confirmed_by := old.confirmed_by;
      new.confirmed_at := old.confirmed_at;
    end if;
    new.updated_at := now();
  end if;
  return new;
end;
$$;

drop trigger if exists trg_mentor_guard_team_tasks on public.mentor_team_tasks;
create trigger trg_mentor_guard_team_tasks
  before insert or update on public.mentor_team_tasks
  for each row execute function public.mentor_guard_team_tasks();

-- Trigger: Warnings Actor Guard
create or replace function public.mentor_guard_team_warnings()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if tg_op = 'UPDATE' then
    if old.status = 'NEEDS_CHECK' and new.status in ('RESOLVED', 'DISMISSED') then
      if auth.uid() is not null then
        new.reviewed_by := auth.uid();
        new.reviewed_at := now();
      end if;
    end if;
    new.updated_at := now();
  end if;
  return new;
end;
$$;

drop trigger if exists trg_mentor_guard_team_warnings on public.mentor_team_warnings;
create trigger trg_mentor_guard_team_warnings
  before insert or update on public.mentor_team_warnings
  for each row execute function public.mentor_guard_team_warnings();

-- Trigger: Cycle Prevention & Project Isolation on Task Dependencies
create or replace function public.mentor_check_task_dependency()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
  v_task_proj text;
  v_dep_proj text;
  v_cycle_detected boolean;
begin
  -- Self dependency check
  if new.task_id = new.depends_on_task_id then
    raise exception 'SELF_DEPENDENCY_FORBIDDEN';
  end if;

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

-- ============================================================================
-- RPCs: SAFE OWNERSHIP TRANSFER & TASK CONFIRMATION
-- ============================================================================
create or replace function public.mentor_transfer_team_ownership(
  p_project_id text,
  p_new_owner_id uuid
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
  v_current_owner uuid;
  v_is_active_member boolean;
begin
  select owner_id into v_current_owner from public.mentor_team_projects where id = p_project_id;
  if v_current_owner is null or v_current_owner <> auth.uid() then
    raise exception 'PERMISSION_DENIED: Only the current project owner can transfer ownership';
  end if;

  if p_new_owner_id = auth.uid() then
    return true; -- No-op
  end if;

  select exists (
    select 1 from public.mentor_team_members
    where project_id = p_project_id and user_id = p_new_owner_id and status = 'ACTIVE'
  ) into v_is_active_member;

  if not v_is_active_member then
    raise exception 'INVALID_NEW_OWNER: New owner must be an active member of the project';
  end if;

  -- Temporarily disable guard trigger during atomic ownership transfer
  alter table public.mentor_team_members disable trigger trg_mentor_guard_team_members;
  alter table public.mentor_team_projects disable trigger trg_mentor_guard_team_project;

  update public.mentor_team_members set role = 'ADMIN' where project_id = p_project_id and user_id = auth.uid();
  update public.mentor_team_members set role = 'OWNER' where project_id = p_project_id and user_id = p_new_owner_id;
  update public.mentor_team_projects set owner_id = p_new_owner_id, updated_at = now() where id = p_project_id;

  alter table public.mentor_team_members enable trigger trg_mentor_guard_team_members;
  alter table public.mentor_team_projects enable trigger trg_mentor_guard_team_project;

  return true;
end;
$$;

revoke all on function public.mentor_transfer_team_ownership(text, uuid) from public, anon;
grant execute on function public.mentor_transfer_team_ownership(text, uuid) to authenticated;

create or replace function public.mentor_confirm_task(
  p_task_id text
)
returns public.mentor_team_tasks language plpgsql security definer set search_path = '' as $$
declare
  v_task public.mentor_team_tasks%rowtype;
  v_proj_id text;
begin
  select project_id into v_proj_id from public.mentor_team_tasks where id = p_task_id;
  if v_proj_id is null then
    raise exception 'TASK_NOT_FOUND';
  end if;

  if not public.mentor_is_team_active_member(v_proj_id, auth.uid()) or
     public.mentor_get_team_role(v_proj_id, auth.uid()) = 'VIEWER' then
    raise exception 'PERMISSION_DENIED';
  end if;

  update public.mentor_team_tasks
  set confirmation_status = 'CONFIRMED',
      confirmed_by = auth.uid(),
      confirmed_at = now(),
      updated_at = now()
  where id = p_task_id
  returning * into v_task;

  return v_task;
end;
$$;

revoke all on function public.mentor_confirm_task(text) from public, anon;
grant execute on function public.mentor_confirm_task(text) to authenticated;

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================
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
drop policy if exists team_projects_select on public.mentor_team_projects;
create policy team_projects_select on public.mentor_team_projects
  for select to authenticated
  using (owner_id = auth.uid() or public.mentor_is_team_active_member(id, auth.uid()));

drop policy if exists team_projects_insert on public.mentor_team_projects;
create policy team_projects_insert on public.mentor_team_projects
  for insert to authenticated
  with check (owner_id = auth.uid());

drop policy if exists team_projects_update on public.mentor_team_projects;
create policy team_projects_update on public.mentor_team_projects
  for update to authenticated
  using (public.mentor_is_team_manager(id, auth.uid()))
  with check (public.mentor_is_team_manager(id, auth.uid()));

drop policy if exists team_projects_delete on public.mentor_team_projects;
create policy team_projects_delete on public.mentor_team_projects
  for delete to authenticated
  using (owner_id = auth.uid());

-- 2. Members
drop policy if exists team_members_select on public.mentor_team_members;
create policy team_members_select on public.mentor_team_members
  for select to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()) or (user_id = auth.uid() and status = 'ACTIVE'));

drop policy if exists team_members_insert on public.mentor_team_members;
create policy team_members_insert on public.mentor_team_members
  for insert to authenticated
  with check (public.mentor_is_team_manager(project_id, auth.uid()));

drop policy if exists team_members_update on public.mentor_team_members;
create policy team_members_update on public.mentor_team_members
  for update to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()))
  with check (public.mentor_is_team_manager(project_id, auth.uid()));

drop policy if exists team_members_delete on public.mentor_team_members;
create policy team_members_delete on public.mentor_team_members
  for delete to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()) and role <> 'OWNER');

-- 3. Requirements
drop policy if exists team_reqs_select on public.mentor_team_requirements;
create policy team_reqs_select on public.mentor_team_requirements
  for select to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()));

drop policy if exists team_reqs_insert on public.mentor_team_requirements;
create policy team_reqs_insert on public.mentor_team_requirements
  for insert to authenticated
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

drop policy if exists team_reqs_update on public.mentor_team_requirements;
create policy team_reqs_update on public.mentor_team_requirements
  for update to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER')
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

drop policy if exists team_reqs_delete on public.mentor_team_requirements;
create policy team_reqs_delete on public.mentor_team_requirements
  for delete to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()) or (added_by = auth.uid() and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER'));

-- 4. Documents
drop policy if exists team_docs_select on public.mentor_team_documents;
create policy team_docs_select on public.mentor_team_documents
  for select to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()));

drop policy if exists team_docs_insert on public.mentor_team_documents;
create policy team_docs_insert on public.mentor_team_documents
  for insert to authenticated
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

drop policy if exists team_docs_update on public.mentor_team_documents;
create policy team_docs_update on public.mentor_team_documents
  for update to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER')
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

drop policy if exists team_docs_delete on public.mentor_team_documents;
create policy team_docs_delete on public.mentor_team_documents
  for delete to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()) or (uploaded_by = auth.uid() and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER'));

-- 5. Tasks
drop policy if exists team_tasks_select on public.mentor_team_tasks;
create policy team_tasks_select on public.mentor_team_tasks
  for select to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()));

drop policy if exists team_tasks_insert on public.mentor_team_tasks;
create policy team_tasks_insert on public.mentor_team_tasks
  for insert to authenticated
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

drop policy if exists team_tasks_update on public.mentor_team_tasks;
create policy team_tasks_update on public.mentor_team_tasks
  for update to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER')
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

drop policy if exists team_tasks_delete on public.mentor_team_tasks;
create policy team_tasks_delete on public.mentor_team_tasks
  for delete to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()));

-- 6. Dependencies
drop policy if exists team_task_deps_select on public.mentor_team_task_dependencies;
create policy team_task_deps_select on public.mentor_team_task_dependencies
  for select to authenticated
  using (exists (
    select 1 from public.mentor_team_tasks t
    where t.id = task_id and public.mentor_is_team_active_member(t.project_id, auth.uid())
  ));

drop policy if exists team_task_deps_insert on public.mentor_team_task_dependencies;
create policy team_task_deps_insert on public.mentor_team_task_dependencies
  for insert to authenticated
  with check (exists (
    select 1 from public.mentor_team_tasks t
    where t.id = task_id and public.mentor_is_team_active_member(t.project_id, auth.uid())
    and public.mentor_get_team_role(t.project_id, auth.uid()) <> 'VIEWER'
  ));

drop policy if exists team_task_deps_delete on public.mentor_team_task_dependencies;
create policy team_task_deps_delete on public.mentor_team_task_dependencies
  for delete to authenticated
  using (exists (
    select 1 from public.mentor_team_tasks t
    where t.id = task_id and public.mentor_is_team_active_member(t.project_id, auth.uid())
    and public.mentor_get_team_role(t.project_id, auth.uid()) <> 'VIEWER'
  ));

-- 7. Warnings
drop policy if exists team_warnings_select on public.mentor_team_warnings;
create policy team_warnings_select on public.mentor_team_warnings
  for select to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()));

drop policy if exists team_warnings_insert on public.mentor_team_warnings;
create policy team_warnings_insert on public.mentor_team_warnings
  for insert to authenticated
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

drop policy if exists team_warnings_update on public.mentor_team_warnings;
create policy team_warnings_update on public.mentor_team_warnings
  for update to authenticated
  using (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER')
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and public.mentor_get_team_role(project_id, auth.uid()) <> 'VIEWER');

drop policy if exists team_warnings_delete on public.mentor_team_warnings;
create policy team_warnings_delete on public.mentor_team_warnings
  for delete to authenticated
  using (public.mentor_is_team_manager(project_id, auth.uid()));

commit;
