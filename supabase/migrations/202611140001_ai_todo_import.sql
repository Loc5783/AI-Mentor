begin;

create or replace function public.mentor_import_todos(data jsonb)
returns jsonb language plpgsql security invoker set search_path = '' as $$
declare
  uid uuid := auth.uid();
  rowdata jsonb;
  task_title text;
  task_subject text;
  task_day integer;
  task_priority integer;
  inserted_id bigint;
  added integer := 0;
  skipped integer := 0;
begin
  if uid is null then raise exception 'AUTHENTICATION_REQUIRED' using errcode = '28000'; end if;
  if data is null or jsonb_typeof(data->'tasks') is distinct from 'array' then
    raise exception 'INVALID_TODO_IMPORT';
  end if;
  if jsonb_array_length(data->'tasks') not between 1 and 21 then
    raise exception 'INVALID_TODO_IMPORT';
  end if;

  -- The per-user lock makes duplicate checks reliable across simultaneous requests.
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(uid::text, 4242));
  for rowdata in select value from jsonb_array_elements(data->'tasks') loop
    if jsonb_typeof(rowdata) is distinct from 'object'
       or jsonb_typeof(rowdata->'day') is distinct from 'number'
       or jsonb_typeof(rowdata->'priority') is distinct from 'number'
       or jsonb_typeof(rowdata->'title') is distinct from 'string'
       or (rowdata ? 'linked_subject' and jsonb_typeof(rowdata->'linked_subject') not in ('string','null')) then
      raise exception 'INVALID_TODO_IMPORT';
    end if;
    task_day := (rowdata->>'day')::integer;
    task_priority := (rowdata->>'priority')::integer;
    task_title := pg_catalog.btrim(rowdata->>'title');
    task_subject := nullif(pg_catalog.btrim(rowdata->>'linked_subject'), '');
    if task_day not between 1 and 7 or task_priority not between 0 and 2
       or pg_catalog.char_length(task_title) not between 1 and 160
       or pg_catalog.char_length(coalesce(task_subject,'')) > 80
       or task_title ~ '[[:cntrl:]]'
       or coalesce(task_subject,'') ~ '[[:cntrl:]]' then
      raise exception 'INVALID_TODO_IMPORT';
    end if;

    inserted_id := null;
    insert into public.mentor_todos(user_id,title,day,priority,linked_subject,completed)
      select uid,task_title,task_day,task_priority,task_subject,false
      where not exists (
        select 1 from public.mentor_todos existing
        where existing.user_id = uid and existing.day = task_day
          and pg_catalog.lower(pg_catalog.btrim(existing.title)) = pg_catalog.lower(task_title)
          and coalesce(pg_catalog.lower(pg_catalog.btrim(existing.linked_subject)),'') =
              coalesce(pg_catalog.lower(task_subject),'')
      )
      returning id into inserted_id;
    if inserted_id is null then skipped := skipped + 1; else added := added + 1; end if;
  end loop;
  return jsonb_build_object('inserted',added,'skipped',skipped);
end;
$$;

revoke all on function public.mentor_import_todos(jsonb) from public, anon;
grant execute on function public.mentor_import_todos(jsonb) to authenticated;

commit;
