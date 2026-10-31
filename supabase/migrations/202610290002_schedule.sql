begin;
alter table public.mentor_timetable add column if not exists client_id bigint generated always as identity unique;
create table if not exists public.mentor_todos(
 id bigint generated always as identity primary key,
 user_id uuid not null references auth.users(id) on delete cascade,
 title text not null check(char_length(title) between 1 and 500),
 day smallint not null check(day between 1 and 7), priority smallint not null default 1 check(priority between 0 and 2),
 linked_subject text, completed boolean not null default false, created_at timestamptz not null default now(), completed_at timestamptz,
 updated_at timestamptz not null default now()
);
create table if not exists public.mentor_schedule_logs(
 id bigint generated always as identity primary key,
 user_id uuid not null references auth.users(id) on delete cascade,
 action text not null check(char_length(action)<=40), entity text not null check(char_length(entity)<=40),
 description text not null check(char_length(description)<=2000), created_at timestamptz not null default now()
);
alter table public.mentor_todos enable row level security;
alter table public.mentor_schedule_logs enable row level security;
revoke all on public.mentor_todos,public.mentor_schedule_logs from anon,authenticated;
grant select,insert,update,delete on public.mentor_todos,public.mentor_schedule_logs to authenticated;
grant usage,select on sequence public.mentor_todos_id_seq,public.mentor_schedule_logs_id_seq,public.mentor_timetable_client_id_seq to authenticated;
create policy mentor_todos_owner on public.mentor_todos to authenticated using((select auth.uid())=user_id) with check((select auth.uid())=user_id);
create policy mentor_schedule_logs_owner on public.mentor_schedule_logs to authenticated using((select auth.uid())=user_id) with check((select auth.uid())=user_id);
create function public.mentor_schedule_action(op text,data jsonb default '{}'::jsonb)
returns jsonb language plpgsql security invoker set search_path='' as $$
declare uid uuid:=auth.uid(); item bigint; count_added int:=0; rowdata jsonb; result jsonb;
begin
 if uid is null then raise exception 'AUTHENTICATION_REQUIRED' using errcode='28000'; end if;
 if op='entries' then
  select coalesce(jsonb_agg(to_jsonb(t) order by period),'[]') into result from public.mentor_timetable t where user_id=uid and day=(data->>'day')::int;
  return jsonb_build_object('items',result);
 elsif op='insert_entry' then
  insert into public.mentor_timetable(user_id,day,period,subject,teacher,notes) values(uid,(data->>'day')::int,(data->>'period')::int,data->>'title',data->>'teacher',data->>'notes') returning client_id into item;
 elsif op='update_entry' then
  update public.mentor_timetable set day=(data->>'day')::int,period=(data->>'period')::int,subject=data->>'title',teacher=data->>'teacher',notes=data->>'notes' where user_id=uid and client_id=(data->>'id')::bigint returning client_id into item;
  if item is null then raise exception 'NOT_FOUND'; end if;
 elsif op='delete_entry' then delete from public.mentor_timetable where user_id=uid and client_id=(data->>'id')::bigint;
 elsif op='import' then
  if jsonb_typeof(data->'entries')<>'array' or jsonb_array_length(data->'entries') not between 1 and 70 then raise exception 'INVALID_IMPORT'; end if;
  for rowdata in select value from jsonb_array_elements(data->'entries') loop
   insert into public.mentor_timetable(user_id,day,period,subject) values(uid,(rowdata->>'day')::int,(rowdata->>'period')::int,rowdata->>'title') on conflict(user_id,day,period) do nothing;
   if found then count_added:=count_added+1; end if;
  end loop;
  return jsonb_build_object('inserted',count_added);
 elsif op='todos' then
  select coalesce(jsonb_agg(to_jsonb(t) order by completed,priority desc,id),'[]') into result from public.mentor_todos t where user_id=uid and day=(data->>'day')::int;
  return jsonb_build_object('items',result);
 elsif op='insert_todo' then
  insert into public.mentor_todos(user_id,title,day,priority,linked_subject,completed) values(uid,data->>'title',(data->>'day')::int,(data->>'priority')::int,data->>'linked_subject',coalesce((data->>'completed')::boolean,false)) returning id into item;
 elsif op='update_todo' then
  update public.mentor_todos set title=data->>'title',day=(data->>'day')::int,priority=(data->>'priority')::int,linked_subject=data->>'linked_subject',updated_at=now() where user_id=uid and id=(data->>'id')::bigint returning id into item;
  if item is null then raise exception 'NOT_FOUND'; end if;
 elsif op='todo_status' then
  update public.mentor_todos set completed=(data->>'completed')::boolean,completed_at=case when (data->>'completed')::boolean then now() else null end,updated_at=now() where user_id=uid and id=(data->>'id')::bigint returning id into item;
  if item is null then raise exception 'NOT_FOUND'; end if;
 elsif op='delete_todo' then delete from public.mentor_todos where user_id=uid and id=(data->>'id')::bigint;
 elsif op='logs' then
  select coalesce(jsonb_agg(to_jsonb(t) order by id desc),'[]') into result from (select * from public.mentor_schedule_logs where user_id=uid order by id desc limit least(greatest(coalesce((data->>'limit')::int,50),1),100)) t;
  return jsonb_build_object('items',result);
 elsif op='insert_log' then
  insert into public.mentor_schedule_logs(user_id,action,entity,description) values(uid,data->>'action',data->>'entity',data->>'description') returning id into item;
 elsif op='clear_logs' then delete from public.mentor_schedule_logs where user_id=uid;
 else raise exception 'UNKNOWN_OPERATION'; end if;
 return jsonb_build_object('id',item);
end; $$;
revoke all on function public.mentor_schedule_action(text,jsonb) from public,anon;
grant execute on function public.mentor_schedule_action(text,jsonb) to authenticated;
commit;
