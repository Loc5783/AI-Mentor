begin;

alter table public.mentor_todos add column if not exists due_date date;
alter table public.mentor_todos add column if not exists plan_start_date date;
alter table public.mentor_todos add column if not exists recurrence text not null default 'NONE';

-- The previous schema only knew a weekday. Preserve that exact behavior for
-- existing rows rather than inventing a historical or future due date.
update public.mentor_todos set recurrence='LEGACY_WEEKDAY'
where due_date is null and recurrence='NONE';

alter table public.mentor_todos add constraint mentor_todos_date_scope_check
  check (
    (recurrence='NONE' and due_date is not null and
      extract(isodow from due_date)::int=day and
      (plan_start_date is null or due_date>=plan_start_date))
    or (recurrence='LEGACY_WEEKDAY' and due_date is null)
  );
create index if not exists mentor_todos_owner_due_date
  on public.mentor_todos(user_id,due_date);

-- Retire the old entry point without deleting its implementation. Non-to-do
-- actions still use it, while all to-do reads and writes use dated semantics.
alter function public.mentor_schedule_action(text,jsonb)
  rename to mentor_schedule_action_weekday_legacy;
revoke all on function public.mentor_schedule_action_weekday_legacy(text,jsonb)
  from public,anon,authenticated;

create function public.mentor_schedule_action(op text,data jsonb default '{}'::jsonb)
returns jsonb language plpgsql security definer set search_path='' as $$
declare
  uid uuid:=auth.uid();
  task_id bigint;
  task_day int;
  task_due date;
  task_start date;
  result jsonb;
begin
  if uid is null then raise exception 'AUTHENTICATION_REQUIRED' using errcode='28000'; end if;
  if op='todos' then
    task_day:=(data->>'day')::int;
    if task_day not between 1 and 7 then raise exception 'INVALID_TODO_DAY'; end if;
    task_due:=coalesce((data->>'date')::date,
      current_date+((task_day-extract(isodow from current_date)::int+7)%7));
    if extract(isodow from task_due)::int<>task_day then
      raise exception 'INVALID_TODO_DATE';
    end if;
    select coalesce(jsonb_agg(to_jsonb(t) order by completed,priority desc,id),'[]')
      into result from public.mentor_todos t
      where t.user_id=uid and
        (t.due_date=task_due or (t.due_date is null and t.recurrence='LEGACY_WEEKDAY' and t.day=task_day));
    return jsonb_build_object('items',result);
  elsif op='insert_todo' then
    task_day:=(data->>'day')::int;
    task_due:=case when coalesce((data->>'legacy_weekday')::boolean,false) then null
      else coalesce((data->>'due_date')::date,
        current_date+((task_day-extract(isodow from current_date)::int+7)%7)) end;
    task_start:=coalesce((data->>'plan_start_date')::date,current_date);
    if task_day not between 1 and 7 or
      (task_due is not null and extract(isodow from task_due)::int<>task_day) or
      (task_due is not null and task_due<task_start) or
      char_length(btrim(coalesce(data->>'title',''))) not between 1 and 160
      or (data->>'priority')::int not between 0 and 2 then
      raise exception 'INVALID_TODO';
    end if;
    insert into public.mentor_todos(user_id,title,day,priority,linked_subject,completed,due_date,plan_start_date,recurrence)
      values(uid,btrim(data->>'title'),task_day,(data->>'priority')::int,
        data->>'linked_subject',coalesce((data->>'completed')::boolean,false),task_due,task_start,
        case when task_due is null then 'LEGACY_WEEKDAY' else 'NONE' end)
      returning id into task_id;
    return jsonb_build_object('id',task_id);
  elsif op='update_todo' then
    task_id:=(data->>'id')::bigint;
    select due_date,plan_start_date into task_due,task_start
      from public.mentor_todos where user_id=uid and id=task_id for update;
    if not found then raise exception 'NOT_FOUND'; end if;
    task_day:=(data->>'day')::int;
    task_due:=coalesce((data->>'due_date')::date,task_due);
    task_start:=coalesce((data->>'plan_start_date')::date,task_start);
    if task_day not between 1 and 7 or
      (task_due is not null and extract(isodow from task_due)::int<>task_day) or
      (task_due is not null and task_start is not null and task_due<task_start) or
      char_length(btrim(coalesce(data->>'title',''))) not between 1 and 160 or
      (data->>'priority')::int not between 0 and 2 then
      raise exception 'INVALID_TODO';
    end if;
    update public.mentor_todos set title=btrim(data->>'title'),day=task_day,
      priority=(data->>'priority')::int,linked_subject=data->>'linked_subject',
      due_date=task_due,plan_start_date=task_start,
      recurrence=case when task_due is null then 'LEGACY_WEEKDAY' else 'NONE' end,
      updated_at=now() where user_id=uid and id=task_id;
    return jsonb_build_object('id',task_id);
  end if;
  return public.mentor_schedule_action_weekday_legacy(op,data);
end;
$$;
revoke all on function public.mentor_schedule_action(text,jsonb) from public,anon;
grant execute on function public.mentor_schedule_action(text,jsonb) to authenticated;

alter function public.mentor_import_todos(jsonb)
  rename to mentor_import_todos_weekday_legacy;
revoke all on function public.mentor_import_todos_weekday_legacy(jsonb)
  from public,anon,authenticated;

create function public.mentor_import_todos(data jsonb)
returns jsonb language plpgsql security invoker set search_path='' as $$
declare
  uid uuid:=auth.uid();
  rowdata jsonb;
  task_day int;
  task_due date;
  task_start date;
  task_priority int;
  task_title text;
  task_subject text;
  inserted_id bigint;
  added int:=0;
  skipped int:=0;
begin
  if uid is null then raise exception 'AUTHENTICATION_REQUIRED' using errcode='28000'; end if;
  if jsonb_typeof(data->'tasks') is distinct from 'array' or
     jsonb_array_length(data->'tasks') not between 1 and 21 then
    raise exception 'INVALID_TODO_IMPORT';
  end if;
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(uid::text,4242));
  for rowdata in select value from jsonb_array_elements(data->'tasks') loop
    if jsonb_typeof(rowdata) is distinct from 'object' or
       jsonb_typeof(rowdata->'day') is distinct from 'number' or
       jsonb_typeof(rowdata->'priority') is distinct from 'number' or
       jsonb_typeof(rowdata->'title') is distinct from 'string' then
      raise exception 'INVALID_TODO_IMPORT';
    end if;
    task_day:=(rowdata->>'day')::int;
    task_priority:=(rowdata->>'priority')::int;
    task_title:=pg_catalog.btrim(rowdata->>'title');
    task_subject:=nullif(pg_catalog.btrim(rowdata->>'linked_subject'),'');
    task_due:=coalesce((rowdata->>'due_date')::date,
      current_date+((task_day-extract(isodow from current_date)::int+7)%7));
    task_start:=coalesce((rowdata->>'plan_start_date')::date,current_date);
    if task_day not between 1 and 7 or task_priority not between 0 and 2 or
       extract(isodow from task_due)::int<>task_day or task_due<task_start or
       pg_catalog.char_length(task_title) not between 1 and 160 or
       pg_catalog.char_length(coalesce(task_subject,''))>80 or
       task_title~'[[:cntrl:]]' or coalesce(task_subject,'')~'[[:cntrl:]]' then
      raise exception 'INVALID_TODO_IMPORT';
    end if;
    inserted_id:=null;
    insert into public.mentor_todos(user_id,title,day,priority,linked_subject,completed,due_date,plan_start_date)
      select uid,task_title,task_day,task_priority,task_subject,false,task_due,task_start
      where not exists (
        select 1 from public.mentor_todos existing
        where existing.user_id=uid and existing.due_date=task_due
          and pg_catalog.lower(pg_catalog.btrim(existing.title))=pg_catalog.lower(task_title)
          and coalesce(pg_catalog.lower(pg_catalog.btrim(existing.linked_subject)),'')=
              coalesce(pg_catalog.lower(task_subject),'')
      ) returning id into inserted_id;
    if inserted_id is null then skipped:=skipped+1; else added:=added+1; end if;
  end loop;
  return jsonb_build_object('inserted',added,'skipped',skipped);
end;
$$;
revoke all on function public.mentor_import_todos(jsonb) from public,anon;
grant execute on function public.mentor_import_todos(jsonb) to authenticated;

commit;
