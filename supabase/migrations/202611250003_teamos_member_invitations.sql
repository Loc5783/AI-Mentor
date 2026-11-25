-- Membership mutations are explicit RPCs; invited users cannot read project data.
revoke insert, update, delete on public.mentor_team_members from authenticated;

create or replace function public.mentor_team_invite(p_project_id text, p_user_id uuid, p_role text)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null or not public.mentor_is_team_manager(p_project_id, auth.uid()) then raise exception 'TEAM_MANAGER_REQUIRED'; end if;
  if p_user_id = auth.uid() or p_role not in ('ADMIN','MEMBER','VIEWER') then raise exception 'INVALID_INVITE'; end if;
  if exists (select 1 from public.mentor_team_members where project_id=p_project_id and user_id=p_user_id and status='ACTIVE') then raise exception 'ALREADY_MEMBER'; end if;
  insert into public.mentor_team_members(project_id,user_id,role,status,removed_at)
  values(p_project_id,p_user_id,p_role,'INVITED',null)
  on conflict(project_id,user_id) do update set role=excluded.role,status='INVITED',removed_at=null;
end; $$;

create or replace function public.mentor_team_respond_invite(p_project_id text, p_accept boolean)
returns void language plpgsql security definer set search_path = '' as $$
begin
  update public.mentor_team_members set status=case when p_accept then 'ACTIVE' else 'REMOVED' end,
    removed_at=case when p_accept then null else now() end,
    joined_at=case when p_accept then now() else joined_at end
  where project_id=p_project_id and user_id=auth.uid() and status='INVITED';
  if not found then raise exception 'INVITE_NOT_FOUND'; end if;
end; $$;

create or replace function public.mentor_team_change_role(p_project_id text, p_user_id uuid, p_role text)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null or not public.mentor_is_team_manager(p_project_id, auth.uid()) then raise exception 'TEAM_MANAGER_REQUIRED'; end if;
  if p_role not in ('ADMIN','MEMBER','VIEWER') or p_user_id=auth.uid() then raise exception 'INVALID_ROLE'; end if;
  update public.mentor_team_members set role=p_role
  where project_id=p_project_id and user_id=p_user_id and status='ACTIVE' and role<>'OWNER';
  if not found then raise exception 'MEMBER_NOT_FOUND'; end if;
end; $$;

create or replace function public.mentor_team_remove_member(p_project_id text, p_user_id uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if auth.uid() is null or not public.mentor_is_team_manager(p_project_id, auth.uid()) then raise exception 'TEAM_MANAGER_REQUIRED'; end if;
  if p_user_id=auth.uid() then raise exception 'CANNOT_REMOVE_SELF'; end if;
  update public.mentor_team_members set status='REMOVED',removed_at=now()
  where project_id=p_project_id and user_id=p_user_id and status in ('ACTIVE','INVITED') and role<>'OWNER';
  if not found then raise exception 'MEMBER_NOT_FOUND'; end if;
end; $$;

create or replace function public.mentor_team_my_invites()
returns table(project_id text,title text,role text) language sql stable security definer set search_path = '' as $$
  select m.project_id,p.title,m.role from public.mentor_team_members m
  join public.mentor_team_projects p on p.id=m.project_id
  where m.user_id=auth.uid() and m.status='INVITED';
$$;

revoke all on function public.mentor_team_invite(text,uuid,text) from public, anon;
revoke all on function public.mentor_team_respond_invite(text,boolean) from public, anon;
revoke all on function public.mentor_team_change_role(text,uuid,text) from public, anon;
revoke all on function public.mentor_team_remove_member(text,uuid) from public, anon;
revoke all on function public.mentor_team_my_invites() from public, anon;
grant execute on function public.mentor_team_invite(text,uuid,text) to authenticated;
grant execute on function public.mentor_team_respond_invite(text,boolean) to authenticated;
grant execute on function public.mentor_team_change_role(text,uuid,text) to authenticated;
grant execute on function public.mentor_team_remove_member(text,uuid) to authenticated;
grant execute on function public.mentor_team_my_invites() to authenticated;
