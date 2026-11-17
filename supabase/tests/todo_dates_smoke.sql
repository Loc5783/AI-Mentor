-- Disposable database only. The transaction leaves no user or task behind.
begin;
select set_config('test.uid',gen_random_uuid()::text,true);
insert into auth.users(id,raw_user_meta_data)
values(current_setting('test.uid')::uuid,'{}');
insert into public.mentor_todos(user_id,title,day,priority,recurrence)
values(current_setting('test.uid')::uuid,'Legacy Monday',1,1,'LEGACY_WEEKDAY');

set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('test.uid'),true);

do $$
declare
  saved jsonb;
  monday jsonb;
  next_monday jsonb;
begin
  saved:=public.mentor_schedule_action('insert_todo',
    '{"day":1,"title":"Dated Monday","priority":1,"due_date":"2026-11-16","plan_start_date":"2026-11-16"}');
  if (saved->>'id') is null then raise exception 'DATED_TODO_NOT_CREATED'; end if;
  monday:=public.mentor_schedule_action('todos','{"day":1,"date":"2026-11-16"}');
  next_monday:=public.mentor_schedule_action('todos','{"day":1,"date":"2026-11-23"}');
  if jsonb_array_length(monday->'items')<>2 then raise exception 'TODAY_TASKS_INCORRECT'; end if;
  if jsonb_array_length(next_monday->'items')<>1 or
     next_monday->'items'->0->>'title'<>'Legacy Monday' then
    raise exception 'ONE_TIME_TASK_REPEATED';
  end if;
end $$;

rollback;
