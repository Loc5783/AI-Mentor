begin;
select set_config('test.uid',gen_random_uuid()::text,true);
insert into auth.users(id,raw_user_meta_data) values(current_setting('test.uid')::uuid,'{}');
insert into public.mentor_library_quizzes(id,title,subject_id,grade,chapter,topic,difficulty,questions,status)
values('smoke-physics-10','Smoke test','physics',10,'Chương thử','Định luật','EASY',
  '[{"id":"q1","code":"q1","chapter":"Chương thử","topic":"Định luật","prompt":"2 + 2 = ?","options":[{"id":"A","text":"4"},{"id":"B","text":"5"}],"correctOption":"A","explanation":"Cộng hai số.","difficulty":"EASY","points":10}]','PUBLISHED');
do $$ begin
  if not exists(select 1 from public.mentor_library_quizzes
      where id like 'review-%' and status='PUBLISHED'
        and jsonb_array_length(questions) between 10 and 15) then
    raise exception 'REVIEW_QUIZ_SEED_MISSING';
  end if;
end $$;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('test.uid'),true);
do $$
declare enrollment jsonb; session jsonb; result jsonb; sid uuid; study_id uuid; key_id uuid; badges jsonb;
begin
  enrollment:=public.mentor_learning_api('POST','/enrollments','{"subjectId":"physics","grade":10}','cloud-smoke-enroll');
  session:=public.mentor_library_api('POST','/library/sessions',jsonb_build_object('quizId','smoke-physics-10'));
  sid:=(session->>'id')::uuid;
  if (session->'questions'->0) ? 'correctOption' then raise exception 'PRACTICE_ANSWER_LEAK'; end if;
  session:=public.mentor_library_api('POST','/library/sessions/'||sid||'/answers','{"questionId":"q1","selectedOption":"B"}');
  if (session->'questions'->0->>'isCorrect')::boolean then raise exception 'PRACTICE_GRADING_ERROR'; end if;
  if not ((session->'questions'->0) ? 'explanation') then raise exception 'ANSWER_EXPLANATION_MISSING'; end if;
  result:=public.mentor_library_api('GET','/me/wrong-questions');
  if jsonb_array_length(result->'items')<>1 then raise exception 'WRONG_NOTEBOOK_NOT_UPDATED'; end if;
  result:=public.mentor_library_api('POST','/me/wrong-questions/'||(result->'items'->0->>'id')||'/retry','{"selectedOption":"A"}');
  if not (result->>'isCorrect')::boolean or result->>'state'<>'RESOLVED' then raise exception 'WRONG_RETRY_NOT_RESOLVED'; end if;
  result:=public.mentor_library_api('POST','/library/sessions/'||sid||'/complete','{}');
  if result->>'state'<>'COMPLETED' then raise exception 'PRACTICE_COMPLETION_FAILED'; end if;
  if exists(select 1 from public.mentor_awards where user_id=current_setting('test.uid')::uuid) then raise exception 'PRACTICE_GRANTED_XP'; end if;

  study_id:=gen_random_uuid(); key_id:=gen_random_uuid();
  result:=public.mentor_study_action('start',jsonb_build_object('sessionId',study_id,'subjectId','physics','grade',10));
  result:=public.mentor_study_action('heartbeat',jsonb_build_object('sessionId',study_id,'eventKey',key_id));
  if (result->>'studySeconds')::int<0 or (result->>'studySeconds')::int>120 then raise exception 'STUDY_TIME_NOT_BOUNDED'; end if;
  result:=public.mentor_study_action('heartbeat',jsonb_build_object('sessionId',study_id,'eventKey',key_id));
  if not (result->>'replayed')::boolean then raise exception 'STUDY_HEARTBEAT_NOT_IDEMPOTENT'; end if;
  result:=public.mentor_study_action('stop',jsonb_build_object('sessionId',study_id,'eventKey',gen_random_uuid()));
  if result->>'state'<>'CLOSED' then raise exception 'STUDY_SESSION_NOT_CLOSED'; end if;

  badges:=public.mentor_badges();
  if jsonb_array_length(badges->'badges')<>4 then raise exception 'BADGE_CATALOG_MISSING'; end if;
end $$;
reset role;
select 'PASS: practice privacy, grading, wrong notebook/retry, no practice XP, bounded study time, idempotency and badge catalog' as result;
rollback;
