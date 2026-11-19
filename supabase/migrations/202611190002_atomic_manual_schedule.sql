begin;

-- One request inserts all manually selected slots or none of them.
create function public.mentor_save_schedule_entries(data jsonb)
returns jsonb language plpgsql security invoker set search_path='' as $$
declare
  uid uuid := auth.uid();
  item_count integer;
  inserted_count integer;
begin
  if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
  if jsonb_typeof(data->'entries') <> 'array' then
    raise sqlstate 'PT400' using message='INVALID_SCHEDULE';
  end if;
  item_count := jsonb_array_length(data->'entries');
  if item_count not between 1 and 70 or exists (
    select 1 from jsonb_array_elements(data->'entries') as row_data(value)
    where coalesce(jsonb_typeof(value->'day'),'null') <> 'number'
       or coalesce(jsonb_typeof(value->'period'),'null') <> 'number'
       or coalesce(jsonb_typeof(value->'title'),'null') <> 'string'
       or (value->>'day')::integer not between 1 and 7
       or (value->>'period')::integer not between 1 and 10
       or char_length(btrim(value->>'title')) not between 1 and 120
       or char_length(coalesce(value->>'teacher','')) > 120
       or char_length(coalesce(value->>'notes','')) > 2000
  ) then
    raise sqlstate 'PT400' using message='INVALID_SCHEDULE';
  end if;
  if (select count(*) from (
    select value->>'day', value->>'period'
    from jsonb_array_elements(data->'entries') as row_data(value)
    group by value->>'day', value->>'period'
  ) slots) <> item_count then
    raise sqlstate 'PT409' using message='SCHEDULE_CONFLICT';
  end if;
  insert into public.mentor_timetable(user_id,day,period,subject,teacher,notes)
    select uid,(value->>'day')::smallint,(value->>'period')::smallint,
      btrim(value->>'title'),nullif(value->>'teacher',''),nullif(value->>'notes','')
    from jsonb_array_elements(data->'entries') as row_data(value);
  get diagnostics inserted_count = row_count;
  return jsonb_build_object('inserted',inserted_count);
exception when unique_violation then
  raise sqlstate 'PT409' using message='SCHEDULE_CONFLICT';
end; $$;

revoke all on function public.mentor_save_schedule_entries(jsonb) from public,anon;
grant execute on function public.mentor_save_schedule_entries(jsonb) to authenticated;

commit;
