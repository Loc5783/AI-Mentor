create or replace function public.mentor_update_profile(data jsonb)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare uid uuid:=auth.uid(); profile_row public.mentor_profiles;
begin
  if uid is null then raise sqlstate 'PT401' using message='UNAUTHORIZED'; end if;
  if char_length(coalesce(data->>'nickname','')) not between 2 and 40
     or char_length(coalesce(data->>'bio',''))>500
     or char_length(coalesce(data->>'address',''))>240
     or char_length(coalesce(data->>'phone',''))>32
     or char_length(coalesce(data->>'avatar_path',''))>96 then
    raise sqlstate 'PT400' using message='INVALID_PROFILE';
  end if;
  update public.mentor_profiles
    set nickname=data->>'nickname',
        bio=coalesce(data->>'bio',bio),
        avatar_path=case when data ? 'avatar_path' then nullif(data->>'avatar_path','') else avatar_path end
    where id=uid returning * into profile_row;
  update public.mentor_preferences
    set address=data->>'address',phone=data->>'phone'
    where user_id=uid;
  return jsonb_build_object('nickname',profile_row.nickname,'bio',profile_row.bio,
    'avatarPath',profile_row.avatar_path,
    'address',(select address from public.mentor_preferences where user_id=uid),
    'phone',(select phone from public.mentor_preferences where user_id=uid));
end $$;

revoke all on function public.mentor_update_profile(jsonb) from public,anon;
grant execute on function public.mentor_update_profile(jsonb) to authenticated;
