begin;

-- Student exam reads, sessions, answer revisions and grading stay server-side.
-- Do not expose raw answer snapshots through PostgREST table reads.
revoke all on public.mentor_exam_sessions from public,anon,authenticated;
revoke all on public.mentor_questions from public,anon,authenticated;
drop policy if exists mentor_questions_read on public.mentor_questions;

create or replace function public.mentor_exam_session_view(
  p_session public.mentor_exam_sessions,
  p_reveal boolean default false
) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare
  v_questions jsonb := '[]'::jsonb;
  v_answers jsonb := '{}'::jsonb;
  v_revisions jsonb := '{}'::jsonb;
  v_q jsonb;
  v_answer jsonb;
  v_choice text;
  v_reveal boolean := p_reveal or p_session.state in ('SUBMITTED','TIMED_OUT');
begin
  for v_q in select value from jsonb_array_elements(p_session.question_snapshots) loop
    v_answer := coalesce(p_session.answers -> (v_q->>'id'), '{}'::jsonb);
    v_choice := nullif(v_answer->>'selectedOption','');
    if v_choice is not null then
      v_answers := v_answers || jsonb_build_object(v_q->>'id',v_choice);
    end if;
    if v_answer ? 'revision' then
      v_revisions := v_revisions || jsonb_build_object(v_q->>'id',(v_answer->>'revision')::int);
    end if;
    v_questions := v_questions || jsonb_build_array(
      jsonb_build_object(
        'id',v_q->>'id','code',v_q->'code','prompt',v_q->>'prompt',
        'optionA',v_q->>'optionA','optionB',v_q->>'optionB',
        'optionC',v_q->>'optionC','optionD',v_q->>'optionD',
        'points',coalesce((v_q->>'points')::int,10),
        'chapter',v_q->'chapter','topic',v_q->'topic','difficulty',v_q->'difficulty',
        'selectedOption',v_choice
      ) || case when v_reveal then jsonb_build_object(
        'correctOption',v_q->>'correctOption','explanation',v_q->>'explanation',
        'isCorrect',v_choice is not null and upper(v_choice)=upper(coalesce(v_q->>'correctOption',''))
      ) else '{}'::jsonb end
    );
  end loop;

  return jsonb_build_object(
    'id',p_session.id,'examId',p_session.exam_id,'subjectId',p_session.subject_id,
    'grade',p_session.grade,'examTitle',p_session.exam_title,'examType',p_session.exam_type,
    'semester',p_session.semester,'durationMinutes',p_session.duration_minutes,
    'state',p_session.state,'serverNow',extract(epoch from clock_timestamp())::bigint,
    'startedAt',extract(epoch from p_session.started_at)::bigint,
    'deadlineAt',extract(epoch from p_session.deadline_at)::bigint,
    'submittedAt',case when p_session.submitted_at is null then null else extract(epoch from p_session.submitted_at)::bigint end,
    'remainingSeconds',case when p_session.state='IN_PROGRESS' then greatest(0,floor(extract(epoch from p_session.deadline_at-clock_timestamp()))::int) else 0 end,
    'score',p_session.score,'score10',p_session.score_10,'rawScore',p_session.raw_score,
    'totalPoints',p_session.total_points,'correctCount',p_session.correct_count,
    'totalCount',p_session.total_count,'answeredCount',jsonb_object_length(v_answers),
    'questions',v_questions,'answers',v_answers,'questionRevisions',v_revisions,
    'revision',p_session.revision,'createdAt',p_session.created_at,'updatedAt',p_session.updated_at
  );
end;
$$;
revoke all on function public.mentor_exam_session_view(public.mentor_exam_sessions,boolean) from public,anon,authenticated;

create or replace function public.mentor_exam_finalize(p_session_id uuid,p_final_state text)
returns void language plpgsql security definer set search_path = '' as $$
declare
  s public.mentor_exam_sessions;
  q jsonb;
  a jsonb;
  v_choice text;
  v_raw int := 0;
  v_correct int := 0;
  v_points int;
  v_now timestamptz := clock_timestamp();
begin
  if p_final_state not in ('SUBMITTED','TIMED_OUT') then raise exception using errcode='PT400',message='INVALID_EXAM_STATE'; end if;
  select * into s from public.mentor_exam_sessions where id=p_session_id for update;
  if s.id is null or s.state<>'IN_PROGRESS' then return; end if;
  for q in select value from jsonb_array_elements(s.question_snapshots) loop
    a:=coalesce(s.answers->(q->>'id'),'{}'::jsonb);
    v_choice:=nullif(a->>'selectedOption','');
    v_points:=coalesce((q->>'points')::int,10);
    if v_choice is not null and upper(v_choice)=upper(coalesce(q->>'correctOption','')) then
      v_raw:=v_raw+v_points; v_correct:=v_correct+1;
    else
      insert into public.mentor_wrong_questions(
        user_id,question_id,subject_id,grade,selected_option,correct_option,explanation,question_snapshot,state,created_at
      ) values(
        s.user_id,q->>'id',s.subject_id,s.grade,coalesce(v_choice,''),coalesce(q->>'correctOption',''),
        coalesce(q->>'explanation',''),q,'NEEDS_REVIEW',v_now
      ) on conflict(user_id,question_id) do update set
        selected_option=excluded.selected_option,correct_option=excluded.correct_option,
        explanation=excluded.explanation,question_snapshot=excluded.question_snapshot,
        state='NEEDS_REVIEW',updated_at=v_now;
    end if;
  end loop;
  update public.mentor_exam_sessions set state=p_final_state,score=v_raw,
    score_10=case when s.total_points>0 then round(v_raw::numeric/s.total_points*10,2) else 0 end,
    raw_score=v_raw,correct_count=v_correct,submitted_at=v_now,
    revision=revision+1,updated_at=v_now where id=s.id;
end;
$$;
revoke all on function public.mentor_exam_finalize(uuid,text) from public,anon,authenticated;

create or replace function public.mentor_exam_api(
  method text,path text,body jsonb default '{}'::jsonb,query jsonb default '{}'::jsonb
) returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  uid uuid:=auth.uid();
  e public.mentor_exams;
  s public.mentor_exam_sessions;
  qrow public.mentor_questions;
  v_id text;
  v_exam_id text;
  v_qids jsonb;
  v_snapshots jsonb:='[]'::jsonb;
  v_items jsonb:='[]'::jsonb;
  v_item jsonb;
  v_answers jsonb;
  v_existing jsonb;
  v_choice text;
  v_question_id text;
  v_revision int;
  v_old_revision int;
  v_old_choice text;
  v_index int:=0;
  v_total int:=0;
  v_raw int:=0;
  v_correct int:=0;
  v_answered int:=0;
  v_final_state text;
begin
  if uid is null then raise exception using errcode='PT401',message='AUTHENTICATION_REQUIRED'; end if;

  if path='/exams' and method='GET' then
    select coalesce(jsonb_agg(jsonb_build_object(
      'id',x.id,'title',x.title,'subjectId',x.subject_id,'grade',x.grade,'examType',x.exam_type,
      'semester',x.semester,'durationMinutes',x.duration_minutes,'totalPoints',x.total_points,
      'questionCount',x.question_count,'status',x.status,'version',x.version,'createdAt',x.created_at
    ) order by x.created_at desc),'[]'::jsonb) into v_items
    from public.mentor_exams x where x.status='PUBLISHED'
      and (coalesce(query->>'subject_id',query->>'subjectId','')='' or x.subject_id=coalesce(query->>'subject_id',query->>'subjectId'))
      and (coalesce(query->>'grade','')='' or x.grade=(query->>'grade')::int)
      and (coalesce(query->>'exam_type',query->>'examType','')='' or x.exam_type=upper(coalesce(query->>'exam_type',query->>'examType')))
      and (coalesce(query->>'semester','')='' or x.semester=(query->>'semester')::int);
    return jsonb_build_object('items',v_items,'total',jsonb_array_length(v_items));
  elsif path ~ '^/exams/[^/]+$' and method='GET' then
    v_exam_id:=split_part(path,'/',3);
    select * into e from public.mentor_exams where id=v_exam_id and status='PUBLISHED';
    if e.id is null then raise exception using errcode='PT404',message='EXAM_NOT_FOUND'; end if;
    return jsonb_build_object('id',e.id,'title',e.title,'subjectId',e.subject_id,'grade',e.grade,
      'examType',e.exam_type,'semester',e.semester,'durationMinutes',e.duration_minutes,
      'totalPoints',e.total_points,'questionCount',e.question_count,'status',e.status,'version',e.version,'createdAt',e.created_at);
  elsif path='/exams/sessions' and method='GET' then
    for s in select * from public.mentor_exam_sessions x where x.user_id=uid
      and (coalesce(query->>'subject_id',query->>'subjectId','')='' or x.subject_id=coalesce(query->>'subject_id',query->>'subjectId'))
      and (coalesce(query->>'grade','')='' or x.grade=(query->>'grade')::int)
      order by x.started_at desc limit 100 loop
      if s.state='IN_PROGRESS' and clock_timestamp()>=s.deadline_at then
        perform public.mentor_exam_finalize(s.id,'TIMED_OUT');
        select * into s from public.mentor_exam_sessions where id=s.id;
      end if;
      v_items:=v_items||jsonb_build_array(public.mentor_exam_session_view(s,s.state in ('SUBMITTED','TIMED_OUT')));
    end loop;
    return jsonb_build_object('items',v_items,'total',jsonb_array_length(v_items));
  elsif path='/exams/sessions' and method='POST' then
    v_exam_id:=body->>'examId';
    select * into e from public.mentor_exams where id=v_exam_id and status='PUBLISHED';
    if e.id is null then raise exception using errcode='PT404',message='EXAM_NOT_FOUND'; end if;
    select * into s from public.mentor_exam_sessions x where x.user_id=uid and x.exam_id=e.id
      and x.state='IN_PROGRESS' and x.deadline_at>clock_timestamp() order by x.started_at desc limit 1;
    if s.id is not null then return public.mentor_exam_session_view(s,false); end if;
    begin v_qids:=e.question_ids::jsonb; exception when others then raise exception using errcode='PT500',message='EXAM_QUESTION_LIST_INVALID'; end;
    if jsonb_typeof(v_qids)='string' then begin v_qids:=(v_qids#>>'{}')::jsonb; exception when others then raise exception using errcode='PT500',message='EXAM_QUESTION_LIST_INVALID'; end; end if;
    if jsonb_typeof(v_qids)<>'array' or jsonb_array_length(v_qids)=0 then raise exception using errcode='PT400',message='EXAM_EMPTY'; end if;
    if jsonb_array_length(v_qids)>500 then raise exception using errcode='PT400',message='EXAM_TOO_LARGE'; end if;
    for v_item in select value from jsonb_array_elements(v_qids) loop
      v_id:=case when jsonb_typeof(v_item)='string' then v_item#>>'{}' else '' end;
      if v_id='' then raise exception using errcode='PT400',message='EXAM_QUESTION_LIST_INVALID'; end if;
      if (select count(*) from jsonb_array_elements(v_qids) as item(value) where item.value=v_item)>1 then raise exception using errcode='PT400',message='EXAM_DUPLICATE_QUESTION'; end if;
      select * into qrow from public.mentor_questions where id=v_id and status='PUBLISHED';
      if qrow.id is null then raise exception using errcode='PT400',message='EXAM_QUESTION_UNAVAILABLE'; end if;
      if qrow.subject_id<>e.subject_id or qrow.grade<>e.grade then raise exception using errcode='PT400',message='EXAM_QUESTION_SCOPE_MISMATCH'; end if;
      v_total:=v_total+coalesce(qrow.points,10);
      v_snapshots:=v_snapshots||jsonb_build_array(jsonb_build_object(
        'id',qrow.id,'code',qrow.code,'prompt',qrow.prompt,'optionA',qrow.option_a,
        'optionB',qrow.option_b,'optionC',qrow.option_c,'optionD',qrow.option_d,
        'correctOption',qrow.correct_option,'explanation',qrow.explanation,
        'difficulty',qrow.difficulty,'chapter',qrow.chapter,'topic',qrow.topic,'points',coalesce(qrow.points,10)));
    end loop;
    insert into public.mentor_exam_sessions(user_id,exam_id,subject_id,grade,exam_title,exam_type,semester,
      duration_minutes,question_snapshots,answers,state,total_points,total_count,started_at,deadline_at)
    values(uid,e.id,e.subject_id,e.grade,e.title,e.exam_type,e.semester,e.duration_minutes,v_snapshots,'{}','IN_PROGRESS',
      v_total,jsonb_array_length(v_snapshots),clock_timestamp(),clock_timestamp()+make_interval(mins=>e.duration_minutes)) returning * into s;
    return public.mentor_exam_session_view(s,false);
  elsif path ~ '^/exams/sessions/[^/]+/answers$' and method='POST' then
    v_id:=split_part(path,'/',4); v_question_id:=body->>'questionId';
    v_choice:=nullif(upper(trim(body->>'selectedOption')),'NULL');
    if v_choice is not null and v_choice not in ('A','B','C','D') then raise exception using errcode='PT400',message='INVALID_ANSWER'; end if;
    v_revision:=coalesce((body->>'revision')::int,0);
    select * into s from public.mentor_exam_sessions where id=v_id::uuid and user_id=uid for update;
    if s.id is null then raise exception using errcode='PT404',message='SESSION_NOT_FOUND'; end if;
    if s.state<>'IN_PROGRESS' then raise exception using errcode='PT409',message='EXAM_SESSION_CLOSED'; end if;
    if clock_timestamp()>=s.deadline_at then
      perform public.mentor_exam_finalize(s.id,'TIMED_OUT');
      select * into s from public.mentor_exam_sessions where id=s.id;
      return public.mentor_exam_session_view(s,true);
    end if;
    if not exists(select 1 from jsonb_array_elements(s.question_snapshots) q where q->>'id'=v_question_id) then raise exception using errcode='PT400',message='QUESTION_NOT_IN_SESSION'; end if;
    v_existing:=coalesce(s.answers->v_question_id,'{}'::jsonb);
    v_old_revision:=coalesce((v_existing->>'revision')::int,0);
    v_old_choice:=nullif(v_existing->>'selectedOption','');
    if v_revision<v_old_revision then return public.mentor_exam_session_view(s,false)||jsonb_build_object('status','IGNORED_STALE','questionId',v_question_id); end if;
    if v_revision=v_old_revision then
      if v_choice is distinct from v_old_choice then raise exception using errcode='PT409',message='REVISION_CONFLICT'; end if;
      return public.mentor_exam_session_view(s,false)||jsonb_build_object('status','SAVED','questionId',v_question_id);
    end if;
    v_answers:=jsonb_set(s.answers,array[v_question_id],jsonb_build_object('selectedOption',v_choice,'revision',v_revision,'savedAt',clock_timestamp()),true);
    update public.mentor_exam_sessions set answers=v_answers,revision=revision+1,updated_at=clock_timestamp() where id=s.id returning * into s;
    return public.mentor_exam_session_view(s,false)||jsonb_build_object('status','SAVED','questionId',v_question_id);
  elsif path ~ '^/exams/sessions/[^/]+/submit$' and method='POST' then
    v_id:=split_part(path,'/',4);
    select * into s from public.mentor_exam_sessions where id=v_id::uuid and user_id=uid for update;
    if s.id is null then raise exception using errcode='PT404',message='SESSION_NOT_FOUND'; end if;
    if s.state='IN_PROGRESS' then
      v_final_state:=case when clock_timestamp()<s.deadline_at then 'SUBMITTED' else 'TIMED_OUT' end;
      perform public.mentor_exam_finalize(s.id,v_final_state);
      select * into s from public.mentor_exam_sessions where id=s.id;
    end if;
    return public.mentor_exam_session_view(s,true);
  elsif path ~ '^/exams/sessions/[^/]+$' and method='GET' then
    v_id:=split_part(path,'/',4);
    select * into s from public.mentor_exam_sessions where id=v_id::uuid and user_id=uid for update;
    if s.id is null then raise exception using errcode='PT404',message='SESSION_NOT_FOUND'; end if;
    if s.state='IN_PROGRESS' and clock_timestamp()>=s.deadline_at then
      perform public.mentor_exam_finalize(s.id,'TIMED_OUT');
      select * into s from public.mentor_exam_sessions where id=s.id;
    end if;
    return public.mentor_exam_session_view(s,s.state in ('SUBMITTED','TIMED_OUT'));
  end if;
  raise exception using errcode='PT404',message='API_NOT_IMPLEMENTED';
end;
$$;
revoke all on function public.mentor_exam_api(text,text,jsonb,jsonb) from public,anon;
grant execute on function public.mentor_exam_api(text,text,jsonb,jsonb) to authenticated;

commit;
