begin;
select set_config('test.uid1',gen_random_uuid()::text,true),set_config('test.uid2',gen_random_uuid()::text,true);
insert into auth.users(id,raw_user_meta_data) values(current_setting('test.uid1')::uuid,'{}'),(current_setting('test.uid2')::uuid,'{}');
create temporary table expected_units as select * from public.mentor_course_units where subject_id='physics' and grade=10;
alter table expected_units enable row level security;
create policy test_fixture_read on expected_units for select to authenticated using (true);
grant select on expected_units to authenticated;
set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('test.uid1'),true);
do $$
declare e jsonb; a jsonb; done jsonb; answers jsonb; first_lesson text; oldid uuid; eid uuid; r jsonb;
begin
 e:=public.mentor_learning_api('POST','/enrollments','{"subjectId":"physics","grade":10}','enroll-test'); eid:=(e->>'id')::uuid;
 a:=public.mentor_learning_api('POST','/placements',jsonb_build_object('enrollmentId',eid),'placement-test');
 if (a->'questions'->0) ? 'correctOptionId' then raise exception 'ANSWER_LEAK'; end if;
 select jsonb_agg(jsonb_build_object('questionId',q->>'id','selectedOptionId',q->>'correctOptionId','submissionId',gen_random_uuid())) into answers from expected_units u,jsonb_array_elements(u.questions) q where unit_id='placement';
 a:=public.mentor_learning_api('PATCH','/attempts/'||(a->>'id'),jsonb_build_object('step',2,'answers',answers),null,(a->>'revision')::int);
 done:=public.mentor_learning_api('POST','/completions',jsonb_build_object('attemptId',a->>'id'),'placement-complete-test');
 if (done->>'xpAwarded')::int<>0 or (done->>'streakLit')::boolean then raise exception 'PLACEMENT_REWARD_BUG'; end if;
 select unit_id into first_lesson from expected_units where kind='LESSON' order by position limit 1;
 a:=public.mentor_learning_api('POST','/attempts',jsonb_build_object('enrollmentId',eid,'lessonId',first_lesson,'curriculumVersion','intro-2026.1'),'lesson-start-test');
 oldid:=(a->>'id')::uuid;
 if (a->'questions'->0) ? 'explanation' then raise exception 'EARLY_EXPLANATION'; end if;
 select jsonb_agg(jsonb_build_object('questionId',q->>'id','selectedOptionId',q->>'correctOptionId','submissionId',gen_random_uuid())) into answers from expected_units u,jsonb_array_elements(u.questions) q where unit_id=first_lesson;
 a:=public.mentor_learning_api('PATCH','/attempts/'||oldid,jsonb_build_object('step',2,'answers',answers),null,(a->>'revision')::int);
 done:=public.mentor_learning_api('POST','/completions',jsonb_build_object('attemptId',oldid),'lesson-complete-test');
 if (done->>'xpAwarded')::int<>200 or not (done->>'streakLit')::boolean then raise exception 'FIRST_REWARD_BUG'; end if;
 r:=public.mentor_learning_api('POST','/completions',jsonb_build_object('attemptId',oldid),'lesson-complete-again-test');
 if r->>'id'<>done->>'id' or not (r->>'replayed')::boolean then raise exception 'REPLAY_BUG'; end if;
 a:=public.mentor_learning_api('POST','/attempts',jsonb_build_object('enrollmentId',eid,'lessonId',first_lesson,'curriculumVersion','intro-2026.1'),'lesson-retake-test');
 a:=public.mentor_learning_api('PATCH','/attempts/'||(a->>'id'),jsonb_build_object('step',2,'answers',answers),null,(a->>'revision')::int);
 r:=public.mentor_learning_api('POST','/completions',jsonb_build_object('attemptId',a->>'id'),'lesson-retake-complete-test');
 if (r->>'xpAwarded')::int<>0 or (r->>'streakLit')::boolean then raise exception 'DUPLICATE_REWARD'; end if;
 perform set_config('request.jwt.claim.sub',current_setting('test.uid2'),true);
 begin
 perform public.mentor_learning_api('GET','/attempts/'||oldid);
 raise exception 'ISOLATION_FAILED';
 exception when sqlstate 'PT404' then null;
 end;
 begin
 perform xp from public.mentor_awards;
 raise exception 'POINT_TABLE_EXPOSED';
 exception when insufficient_privilege then null;
 end;
end $$;
reset role;
select 'PASS: placement, answer privacy, first reward, replay, retake, identity isolation, protected ledger' as result;
rollback;
