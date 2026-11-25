begin;

-- Keep tasks created before the dependency table was introduced. An invalid
-- reference aborts the transaction, so no learner's dependency is hidden.
do $$
begin
  if exists (
    select 1 from public.mentor_team_tasks task
    left join public.mentor_team_tasks prerequisite
      on prerequisite.id = task.depends_on_task_id
    where task.depends_on_task_id is not null
      and (prerequisite.id is null or prerequisite.project_id <> task.project_id
        or prerequisite.id = task.id)
  ) then
    raise exception 'TEAM_LEGACY_DEPENDENCY_INVALID';
  end if;
end $$;

insert into public.mentor_team_task_dependencies(task_id, depends_on_task_id)
select id, depends_on_task_id
from public.mentor_team_tasks
where depends_on_task_id is not null
on conflict (task_id, depends_on_task_id) do nothing;

update public.mentor_team_tasks
set confirmation_status = 'PENDING_CONFIRMATION'
where not is_confirmed and confirmation_status = 'CONFIRMED';

-- These SECURITY DEFINER helpers are exposed to authenticated users by the
-- previous migration. Their UUID argument must not permit membership probes.
create or replace function public.mentor_is_team_active_member(p_project_id text, p_user_id uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.mentor_team_members
    where project_id = p_project_id and user_id = p_user_id and status = 'ACTIVE'
      and p_user_id = auth.uid()
  );
$$;

create or replace function public.mentor_is_team_manager(p_project_id text, p_user_id uuid)
returns boolean language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.mentor_team_members
    where project_id = p_project_id and user_id = p_user_id and status = 'ACTIVE'
      and role in ('OWNER', 'ADMIN') and p_user_id = auth.uid()
  );
$$;

create or replace function public.mentor_get_team_role(p_project_id text, p_user_id uuid)
returns text language sql stable security definer set search_path = '' as $$
  select role from public.mentor_team_members
  where project_id = p_project_id and user_id = p_user_id and status = 'ACTIVE'
    and p_user_id = auth.uid()
  limit 1;
$$;

-- The original helper ignores REMOVED status. Current policies no longer
-- invoke it, but authenticated clients must not keep its execute privilege.
revoke execute on function public.mentor_is_team_member(text, uuid) from authenticated;

commit;
