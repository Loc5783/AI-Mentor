-- Run only on a disposable database after migrations. All fixtures roll back.
begin;
select set_config('test.history_uid1', gen_random_uuid()::text, true),
       set_config('test.history_uid2', gen_random_uuid()::text, true);
insert into auth.users(id, raw_user_meta_data)
values (current_setting('test.history_uid1')::uuid, '{}'),
       (current_setting('test.history_uid2')::uuid, '{}');
insert into public.mentor_library_quizzes(id,title,subject_id,grade,questions,status)
values ('history-smoke-quiz','History smoke','math',10,
  '[{"id":"q1","prompt":"2 + 2?","correctOption":"A","options":[{"id":"A","text":"4"}]}]',
  'PUBLISHED');
insert into public.mentor_practice_sessions(user_id,quiz_id,subject_id,grade,question_snapshots,answers,state)
values
 (current_setting('test.history_uid1')::uuid,'history-smoke-quiz','math',10,
  '[{"id":"q1","correctOption":"A"}]','{}','IN_PROGRESS'),
 (current_setting('test.history_uid1')::uuid,'history-smoke-quiz','math',10,
  '[{"id":"q1","correctOption":"A"}]','{"q1":"A"}','COMPLETED'),
 (current_setting('test.history_uid2')::uuid,'history-smoke-quiz','math',10,
  '[{"id":"q1","correctOption":"A"}]','{}','IN_PROGRESS');
insert into public.mentor_ai_practice_sessions(user_id,subject_id,grade,topic,difficulty,
  question_count,questions_snapshot,answers,state)
values
 (current_setting('test.history_uid1')::uuid,'math',10,'Đạo hàm','MEDIUM',1,
  '[{"id":"q1","correctOption":"A"}]','{}','IN_PROGRESS'),
 (current_setting('test.history_uid2')::uuid,'math',10,'Bài của người khác','MEDIUM',1,
  '[{"id":"q1","correctOption":"A"}]','{}','COMPLETED');

set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('test.history_uid1'), true);
do $$
declare r jsonb;
begin
  r := public.mentor_learning_session_history('library','math',10,50);
  if jsonb_array_length(r->'items') <> 2 then raise exception 'LIBRARY_HISTORY_OWNER_OR_STATE'; end if;
  if (r::text like '%correctOption%') then raise exception 'LIBRARY_HISTORY_ANSWER_LEAK'; end if;
  r := public.mentor_learning_session_history('ai_practice','math',10,50);
  if jsonb_array_length(r->'items') <> 1 or r::text like '%Bài của người khác%'
      or r::text like '%correctOption%' then raise exception 'AI_HISTORY_OWNER_OR_ANSWER_LEAK'; end if;
  r := public.mentor_learning_session_history('library','physics',10,50);
  if jsonb_array_length(r->'items') <> 0 then raise exception 'HISTORY_SUBJECT_FILTER'; end if;
  perform set_config('request.jwt.claim.sub', current_setting('test.history_uid2'), true);
  r := public.mentor_learning_session_history('library','math',10,50);
  if jsonb_array_length(r->'items') <> 1 then raise exception 'HISTORY_CROSS_ACCOUNT'; end if;
end $$;
rollback;
