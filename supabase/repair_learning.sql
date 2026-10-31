create or replace function public.mentor_learning_api(method text,path text,body jsonb default '{}',request_key text default null,expected_revision int default null,query jsonb default '{}') returns jsonb
language plpgsql security definer set search_path='' as $$
declare uid uuid:=auth.uid(); e public.mentor_enrollments; a public.mentor_attempts; u public.mentor_course_units; units jsonb:='[]'; x record; unit_state text; done bool; ready bool; result jsonb; q jsonb; ans jsonb; old jsonb; prev jsonb; sid text; chosen text; correct bool; wrong int; submissions jsonb; score int; xp int:=0; first_reward bool:=false; lit bool:=false; day date:=(now() at time zone 'Asia/Ho_Chi_Minh')::date; daily bigint; item uuid; fingerprint text:=md5(method||path||body::text); cached public.mentor_requests;
begin
 if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
 perform pg_advisory_xact_lock(hashtextextended(uid::text,0));
 if method<>'GET' and request_key is not null then
  select * into cached from public.mentor_requests where user_id=uid and key=request_key;
  if cached.key is not null then
   if cached.fingerprint<>fingerprint then raise sqlstate 'PT409' using message='IDEMPOTENCY_CONFLICT'; end if;
   return cached.result;
  end if;
 end if;
 if path='/subjects' then
  select jsonb_build_object('items',jsonb_agg(jsonb_build_object('id',s,'implementation',case when s in ('physics','chemistry','biology') then 'ACTIVE' else 'FRAMEWORK' end,'availableGrades',coalesce((select jsonb_agg(distinct grade) from public.mentor_course_units where subject_id=s),'[]')))) into result from unnest(array['math','literature','english','history','geography','physics','chemistry','biology','economics_law']) s;
 elsif path='/enrollments' and method='POST' then
  if not exists(select 1 from public.mentor_course_units where subject_id=body->>'subjectId' and grade=(body->>'grade')::int) then raise sqlstate 'PT409' using message='CURRICULUM_NOT_PUBLISHED'; end if;
  insert into public.mentor_enrollments(user_id,subject_id,grade) values(uid,body->>'subjectId',(body->>'grade')::int) on conflict(user_id,subject_id,grade) do update set updated_at=public.mentor_enrollments.updated_at returning * into e;
  result:=jsonb_build_object('id',e.id,'subjectId',e.subject_id,'grade',e.grade,'placement',e.placement,'revision',e.revision,'updatedAt',e.updated_at);
 elsif path='/enrollments' and method='GET' then
  select jsonb_build_object('items',coalesce(jsonb_agg(jsonb_build_object('id',id,'subjectId',subject_id,'grade',grade,'placement',placement,'revision',revision,'updatedAt',updated_at)),'[]')) into result from public.mentor_enrollments where user_id=uid;
 elsif path ~ '^/enrollments/[^/]+/path$' then
  select * into e from public.mentor_enrollments where id=split_part(path,'/',3)::uuid and user_id=uid;
  if e.id is null then raise sqlstate 'PT404' using message='ENROLLMENT_NOT_FOUND'; end if;
  ready:=e.placement is not null;
  for u in select * from public.mentor_course_units where subject_id=e.subject_id and grade=e.grade and kind<>'PLACEMENT' order by position loop
   done:=exists(select 1 from public.mentor_attempts where user_id=uid and enrollment_id=e.id and unit_id=u.unit_id and state='COMPLETED');
   unit_state:=case when done then 'COMPLETED' when ready then 'AVAILABLE' else 'LOCKED' end;
   units:=units||jsonb_build_array(jsonb_build_object('id',u.unit_id,'title',u.title,'kind',u.kind,'xp',case when u.kind='LESSON' then 200 else 400 end,'state',unit_state,'questionCount',jsonb_array_length(u.questions),'sourceLessonIds',u.sources));
   ready:=ready and done;
  end loop;
  result:=jsonb_build_object('enrollment',jsonb_build_object('id',e.id,'subjectId',e.subject_id,'grade',e.grade,'placement',e.placement,'revision',e.revision),'title',u.course_title,'curriculumVersion',u.course_version,'sourceNote',u.source_note,'units',units,'pendingAttempts',coalesce((select jsonb_agg(jsonb_build_object('id',id,'unit_id',unit_id)) from public.mentor_attempts where user_id=uid and enrollment_id=e.id and state='IN_PROGRESS'),'[]'));
 elsif path in ('/placements','/attempts') and method='POST' then
  select * into e from public.mentor_enrollments where id=(body->>'enrollmentId')::uuid and user_id=uid;
  if e.id is null then raise sqlstate 'PT404' using message='ENROLLMENT_NOT_FOUND'; end if;
  select * into u from public.mentor_course_units where subject_id=e.subject_id and grade=e.grade and unit_id=case when path='/placements' then 'placement' else body->>'lessonId' end;
  if u.unit_id is null then raise sqlstate 'PT404' using message='LESSON_NOT_FOUND'; end if;
  if path='/placements' then
   if e.placement is not null then raise sqlstate 'PT409' using message='PLACEMENT_ALREADY_COMPLETED'; end if;
  else
   if u.kind='PLACEMENT' then raise sqlstate 'PT409' using message='INVALID_LESSON'; end if;
   if e.placement is null then raise sqlstate 'PT409' using message='PLACEMENT_REQUIRED'; end if;
   if u.course_version<>body->>'curriculumVersion' then raise sqlstate 'PT409' using message='CURRICULUM_VERSION_CONFLICT'; end if;
   if exists(select 1 from public.mentor_course_units prior where prior.subject_id=e.subject_id and prior.grade=e.grade and prior.kind<>'PLACEMENT' and prior.position<u.position and not exists(select 1 from public.mentor_attempts t where t.user_id=uid and t.enrollment_id=e.id and t.unit_id=prior.unit_id and t.state='COMPLETED')) then raise sqlstate 'PT409' using message='PREVIOUS_LESSON_REQUIRED'; end if;
  end if;
  select * into a from public.mentor_attempts where user_id=uid and enrollment_id=e.id and unit_id=u.unit_id and course_version=u.course_version and state='IN_PROGRESS';
  if a.id is null then insert into public.mentor_attempts(user_id,enrollment_id,unit_id,course_version,kind) values(uid,e.id,u.unit_id,u.course_version,u.kind) returning * into a; end if;
  result:=public.mentor_attempt_view(a.id);
 elsif path ~ '^/attempts/[^/]+$' and method='GET' then result:=public.mentor_attempt_view(split_part(path,'/',3)::uuid);
 elsif path ~ '^/attempts/[^/]+$' and method='PATCH' then
  select * into a from public.mentor_attempts where id=split_part(path,'/',3)::uuid and user_id=uid for update;
  if a.id is null then raise sqlstate 'PT404' using message='ATTEMPT_NOT_FOUND'; end if;
  if expected_revision is null or expected_revision<>a.revision then raise sqlstate 'PT409' using message='REVISION_CONFLICT'; end if;
  if a.state<>'IN_PROGRESS' then raise sqlstate 'PT409' using message='ATTEMPT_ALREADY_COMPLETED'; end if;
  select * into e from public.mentor_enrollments where id=a.enrollment_id;
  select * into u from public.mentor_course_units where subject_id=e.subject_id and grade=e.grade and unit_id=a.unit_id;
  if u.course_version<>a.course_version then raise sqlstate 'PT409' using message='CURRICULUM_VERSION_CONFLICT'; end if;
  if (body->>'step')::int not between 0 and jsonb_array_length(u.questions)+2 or jsonb_typeof(body->'answers')<>'array' then raise sqlstate 'PT400' using message='INVALID_STEP'; end if;
  prev:=a.answers;
  for ans in select value from jsonb_array_elements(body->'answers') loop
   select value into q from jsonb_array_elements(u.questions) where value->>'id'=ans->>'questionId';
   if q is null then raise sqlstate 'PT400' using message='INVALID_QUESTION'; end if;
   chosen:=ans->>'selectedOptionId'; sid:=ans->>'submissionId';
   if sid is null or char_length(sid) not between 1 and 100 or not exists(select 1 from jsonb_array_elements(q->'options') where value->>'id'=chosen) then raise sqlstate 'PT400' using message='INVALID_ANSWER'; end if;
   old:=coalesce(prev->(q->>'id'),'{}'); submissions:=coalesce(old->'submissions','{}');
   if submissions ? sid then
    if submissions->>sid<>chosen then raise sqlstate 'PT409' using message='SUBMISSION_CONFLICT'; end if;
    continue;
   end if;
   if a.kind='PLACEMENT' and old<>'{}' then raise sqlstate 'PT409' using message='PLACEMENT_ANSWER_FINAL'; end if;
   if (select count(*) from jsonb_object_keys(submissions))>=100 then raise sqlstate 'PT409' using message='TOO_MANY_ANSWER_ATTEMPTS'; end if;
   correct:=chosen=q->>'correctOptionId'; wrong:=coalesce((old->>'wrongCount')::int,0)+case when correct then 0 else 1 end;
   ans:=jsonb_build_object('questionId',q->>'id','selectedOptionId',chosen,'correct',correct,'wrongCount',wrong,'assisted',coalesce((old->>'assisted')::boolean,false) or wrong>=2,'firstTryCorrect',coalesce((old->>'firstTryCorrect')::boolean,correct),'submissions',submissions||jsonb_build_object(sid,chosen));
   prev:=jsonb_set(prev,array[q->>'id'],ans,true);
  end loop;
  update public.mentor_attempts set step=(body->>'step')::int,answers=prev,revision=revision+1,updated_at=now() where id=a.id;
  result:=public.mentor_attempt_view(a.id);
 elsif path='/completions' and method='POST' then
  select * into a from public.mentor_attempts where id=(body->>'attemptId')::uuid and user_id=uid for update;
  if a.id is null then raise sqlstate 'PT404' using message='ATTEMPT_NOT_FOUND'; end if;
  select c.result||jsonb_build_object('replayed',true) into result from public.mentor_completions c where c.attempt_id=a.id;
  if result is not null then return result; end if;
  select * into e from public.mentor_enrollments where id=a.enrollment_id;
  select * into u from public.mentor_course_units where subject_id=e.subject_id and grade=e.grade and unit_id=a.unit_id;
  if u.course_version<>a.course_version then raise sqlstate 'PT409' using message='CURRICULUM_VERSION_CONFLICT'; end if;
  if exists(select 1 from jsonb_array_elements(u.questions) question_row where not(a.answers ? (question_row->>'id')) or (a.kind<>'PLACEMENT' and not coalesce((a.answers->(question_row->>'id')->>'correct')::boolean,false))) then raise sqlstate 'PT409' using message='PRACTICE_NOT_FINISHED'; end if;
  if a.kind='PLACEMENT' then
   select count(*) filter(where (value->>'correct')::boolean) into score from jsonb_each(a.answers);
   ans:=jsonb_build_object('correct',score,'total',jsonb_array_length(u.questions),'recommendation',case when score<jsonb_array_length(u.questions)*.6 then 'GUIDED' else 'STANDARD' end,'scope','INTRODUCTORY_BLOCK','completedAt',now());
   update public.mentor_enrollments set placement=ans,revision=revision+1,updated_at=now() where id=e.id;
  else
   xp:=case when a.kind='LESSON' then 200 else 400 end;
   insert into public.mentor_awards(user_id,subject_id,grade,unit_id,xp,learning_day) values(uid,e.subject_id,e.grade,a.unit_id,xp,day) on conflict do nothing;
   first_reward:=found; if not first_reward then xp:=0; end if;
   insert into public.mentor_learning_days values(uid,day) on conflict do nothing; lit:=found;
  end if;
  select coalesce(sum(w.xp),0) into daily from public.mentor_awards w where user_id=uid and learning_day=day;
  result:=jsonb_build_object('id',gen_random_uuid(),'attemptId',a.id,'rewardUnitId',a.unit_id,'kind',a.kind,'xpAwarded',xp,'firstRewardForUnit',first_reward,'streakLit',lit,'streakDays',public.mentor_streak(uid),'dailyXp',daily,'learningDay',day,'timezone','Asia/Ho_Chi_Minh','completedAt',now(),'replayed',false,'revision',1);
  if a.kind='PLACEMENT' then result:=result||jsonb_build_object('assessment',ans); end if;
  insert into public.mentor_completions values(a.id,uid,result);
  update public.mentor_attempts set state='COMPLETED',revision=revision+1,updated_at=now() where id=a.id;
 else raise sqlstate 'PT404' using message='API_NOT_IMPLEMENTED'; end if;
 if method<>'GET' and request_key is not null then insert into public.mentor_requests values(uid,request_key,fingerprint,result); end if;
 return result;
end; $$;
revoke all on function public.mentor_learning_api(text,text,jsonb,text,int,jsonb) from public,anon;
grant execute on function public.mentor_learning_api(text,text,jsonb,text,int,jsonb) to authenticated;

