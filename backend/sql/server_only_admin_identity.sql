-- Only the backend can inspect current admin roles and live Auth sessions.
create or replace function public.mentor_admin_identity(p_user_id uuid, p_session_id uuid)
returns jsonb language sql stable security definer set search_path = '' as $$
  select jsonb_build_object('id',u.id,'email',u.email,'username',u.email,
    'role',upper(coalesce(nullif(u.raw_app_meta_data->>'mentor_role',''),
                          u.raw_app_meta_data->>'role','STUDENT')))
  from auth.users u join auth.sessions s on s.user_id=u.id
  where u.id=p_user_id and s.id=p_session_id
    and u.email_confirmed_at is not null
    and not coalesce(u.is_anonymous,false)
    and (u.banned_until is null or u.banned_until<=now())
    and (s.not_after is null or s.not_after>now())
$$;
revoke all on function public.mentor_admin_identity(uuid,uuid) from public,anon,authenticated;
grant execute on function public.mentor_admin_identity(uuid,uuid) to service_role;
