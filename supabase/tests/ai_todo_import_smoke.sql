begin;
select set_config('test.uid', gen_random_uuid()::text, true);
insert into auth.users(id, raw_user_meta_data)
values(current_setting('test.uid')::uuid, '{}');
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('test.uid'), true);

do $$
declare
  batch jsonb := '{"tasks":[{"day":1,"title":"Review Newton laws","priority":2,"linked_subject":"Physics"},{"day":2,"title":"Read chapter 2","priority":1,"linked_subject":"Literature"}]}'::jsonb;
  result jsonb;
begin
  result := public.mentor_import_todos(batch);
  if (result->>'inserted')::int <> 2 then raise exception 'TODO_IMPORT_COUNT'; end if;
  result := public.mentor_import_todos(batch);
  if (result->>'inserted')::int <> 0 or (result->>'skipped')::int <> 2 then
    raise exception 'TODO_IMPORT_RETRY_DUPLICATED';
  end if;
  begin
    perform public.mentor_import_todos('{"tasks":[{"day":3,"title":"Valid","priority":1},{"day":8,"title":"Invalid","priority":1}]}'::jsonb);
    raise exception 'TODO_IMPORT_INVALID_ACCEPTED';
  exception when others then
    if sqlerrm = 'TODO_IMPORT_INVALID_ACCEPTED' then raise; end if;
  end;
  if (select count(*) from public.mentor_todos where user_id = current_setting('test.uid')::uuid) <> 2 then
    raise exception 'TODO_IMPORT_NOT_ATOMIC';
  end if;
end $$;

reset role;
select 'PASS: reviewed to-do import is atomic and retry-safe' as result;
rollback;
