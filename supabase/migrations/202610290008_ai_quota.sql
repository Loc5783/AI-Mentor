begin;
create table public.mentor_ai_usage(user_id uuid not null references auth.users(id) on delete cascade,day date not null,requests int not null default 0,primary key(user_id,day));
alter table public.mentor_ai_usage enable row level security;
revoke all on public.mentor_ai_usage from anon,authenticated;
create function public.mentor_reserve_ai_request() returns jsonb language plpgsql security definer set search_path='' as $$
declare uid uuid:=auth.uid(); d date:=(now() at time zone 'Asia/Ho_Chi_Minh')::date; count_requests int;
begin
 if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
 insert into public.mentor_ai_usage values(uid,d,1) on conflict(user_id,day) do update set requests=public.mentor_ai_usage.requests+1 where public.mentor_ai_usage.requests<60 returning requests into count_requests;
 if count_requests is null then raise sqlstate 'PT429' using message='RATE_LIMITED'; end if;
 return jsonb_build_object('remaining',60-count_requests);
end; $$;
revoke all on function public.mentor_reserve_ai_request() from public,anon;
grant execute on function public.mentor_reserve_ai_request() to authenticated;
commit;
