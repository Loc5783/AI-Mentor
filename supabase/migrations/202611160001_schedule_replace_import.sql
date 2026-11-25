begin;

-- Replace entries atomically for only the days represented by a reviewed import.
create or replace function public.mentor_replace_schedule_import(data jsonb)
returns jsonb
language plpgsql
security invoker
set search_path=''
as $$
declare
  uid uuid := auth.uid();
  entry_count integer;
  unique_slot_count integer;
  removed_count integer := 0;
  inserted_count integer := 0;
begin
  if uid is null then
    raise exception 'AUTHENTICATION_REQUIRED' using errcode='28000';
  end if;

  if coalesce(jsonb_typeof(data->'entries'), 'null') <> 'array' then
    raise exception 'INVALID_IMPORT';
  end if;

  entry_count := jsonb_array_length(data->'entries');
  if entry_count not between 1 and 70 then
    raise exception 'INVALID_IMPORT';
  end if;

  if exists (
    select 1
    from jsonb_array_elements(data->'entries') as row_data(value)
    where coalesce(jsonb_typeof(value->'day'), 'null') <> 'number'
       or coalesce(jsonb_typeof(value->'period'), 'null') <> 'number'
       or coalesce(jsonb_typeof(value->'title'), 'null') <> 'string'
       or (value->>'day')::integer not between 1 and 7
       or (value->>'period')::integer not between 1 and 10
       or char_length(btrim(value->>'title')) not between 1 and 120
  ) then
    raise exception 'INVALID_IMPORT';
  end if;

  select count(*) into unique_slot_count
  from (
    select (value->>'day')::integer, (value->>'period')::integer
    from jsonb_array_elements(data->'entries') as row_data(value)
    group by (value->>'day')::integer, (value->>'period')::integer
  ) slots;

  if unique_slot_count <> entry_count then
    raise exception 'DUPLICATE_IMPORT_SLOT';
  end if;

  delete from public.mentor_timetable
  where user_id=uid
    and day in (
      select distinct (value->>'day')::smallint
      from jsonb_array_elements(data->'entries') as row_data(value)
    );
  get diagnostics removed_count = row_count;

  insert into public.mentor_timetable(user_id,day,period,subject)
  select uid, (value->>'day')::smallint, (value->>'period')::smallint, btrim(value->>'title')
  from jsonb_array_elements(data->'entries') as row_data(value);
  get diagnostics inserted_count = row_count;

  return jsonb_build_object('removed', removed_count, 'inserted', inserted_count);
end;
$$;

revoke all on function public.mentor_replace_schedule_import(jsonb) from public, anon;
grant execute on function public.mentor_replace_schedule_import(jsonb) to authenticated;

commit;
