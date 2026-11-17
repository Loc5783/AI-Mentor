-- Grade 12 question payloads and mixed question scoring.
begin;
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
        'points',coalesce((v_q->>'points')::int,10),'pointScale',coalesce((v_q->>'pointScale')::int,1),
        'questionType',coalesce(v_q->>'questionType','SINGLE_CHOICE'),
        'contentData',case when v_reveal then coalesce(v_q->'contentData','{}'::jsonb)
          when v_q->>'questionType'='TRUE_FALSE_4' then jsonb_build_object('subItems',
            coalesce((select jsonb_agg(jsonb_build_object(
                'key',item.value->>'key','statement',item.value->>'statement') order by item.ordinality)
              from jsonb_array_elements(coalesce(v_q->'contentData'->'subItems','[]'::jsonb)) with ordinality as item(value,ordinality)),'[]'::jsonb))
          else '{}'::jsonb end,
        'chapter',v_q->'chapter','topic',v_q->'topic','difficulty',v_q->'difficulty',
        'selectedOption',v_choice
      ) || case when v_reveal then jsonb_build_object(
        'correctOption',v_q->>'correctOption','explanation',v_q->>'explanation',
        'isCorrect',case
          when v_q->>'questionType'='ESSAY' then case when v_answer ? 'essayGrade' then (v_answer->'essayGrade'->>'points')::int=(v_q->>'points')::int else null end
          when v_q->>'questionType'='TRUE_FALSE_4' then v_choice is not null and v_choice::jsonb=(v_q->>'correctOption')::jsonb
          when v_q->>'questionType'='SHORT_ANSWER' then exists(select 1 from jsonb_array_elements_text(coalesce(v_q->'contentData'->'acceptedAnswers',jsonb_build_array(v_q->>'correctOption'))) as a(value) where lower(trim(a.value))=lower(trim(coalesce(v_choice,''))) and v_choice is not null)
          else v_choice is not null and upper(v_choice)=upper(coalesce(v_q->>'correctOption','')) end,
        'essayPoints',v_answer->'essayGrade'->'points','essayFeedback',v_answer->'essayGrade'->>'feedback'
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
    'gradingStatus',case when p_session.state='IN_PROGRESS' then 'NOT_STARTED' when exists (select 1 from jsonb_array_elements(p_session.question_snapshots) q where q->>'questionType'='ESSAY' and nullif(p_session.answers->(q->>'id')->>'selectedOption','') is not null and not (p_session.answers->(q->>'id') ? 'essayGrade')) then 'PENDING' else 'COMPLETE' end,
    'totalPoints',p_session.total_points,'correctCount',p_session.correct_count,
    'totalCount',p_session.total_count,'answeredCount',(select count(*) from jsonb_object_keys(v_answers)),
    'questions',v_questions,'answers',v_answers,'questionRevisions',v_revisions,
    'revision',p_session.revision,'createdAt',p_session.created_at,'updatedAt',p_session.updated_at
  );
end;
$$;

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
  v_type text;
  v_expected boolean;
  v_matches int;
  v_partial int;
  v_now timestamptz := clock_timestamp();
begin
  if p_final_state not in ('SUBMITTED','TIMED_OUT') then raise exception using errcode='PT400',message='INVALID_EXAM_STATE'; end if;
  select * into s from public.mentor_exam_sessions where id=p_session_id for update;
  if s.id is null or s.state<>'IN_PROGRESS' then return; end if;
  for q in select value from jsonb_array_elements(s.question_snapshots) loop
    a:=coalesce(s.answers->(q->>'id'),'{}'::jsonb);
    v_choice:=nullif(a->>'selectedOption','');
    v_points:=coalesce((q->>'points')::int,10);
    v_type:=coalesce(q->>'questionType','SINGLE_CHOICE');
    if v_type='ESSAY' then continue; end if;
    if v_type='SHORT_ANSWER' then
      select exists(select 1 from jsonb_array_elements_text(coalesce(q->'contentData'->'acceptedAnswers',jsonb_build_array(q->>'correctOption'))) as a(value) where lower(trim(a.value))=lower(trim(coalesce(v_choice,'')))) into v_expected;
    end if;
    if v_type='TRUE_FALSE_4' and v_choice is not null then
      select count(*) into v_matches
      from jsonb_each_text((q->>'correctOption')::jsonb) expected(key,value)
      where (v_choice::jsonb->>expected.key)=expected.value;
      v_partial:=round(coalesce((q->'contentData'->'scoringScale'->>(v_matches::text))::numeric,
        case v_matches when 1 then 0.1 when 2 then 0.25 when 3 then 0.5 when 4 then 1 else 0 end)
        *coalesce((q->>'pointScale')::int,1))::int;
      v_raw:=v_raw+least(v_points,greatest(0,v_partial));
    end if;
    if (v_type='SINGLE_CHOICE' and v_choice is not null and upper(v_choice)=upper(coalesce(q->>'correctOption','')))
      or (v_type='SHORT_ANSWER' and coalesce(v_expected,false))
      or (v_type='TRUE_FALSE_4' and v_choice is not null and v_choice::jsonb=(q->>'correctOption')::jsonb) then
      if v_type<>'TRUE_FALSE_4' then v_raw:=v_raw+v_points; end if;
      v_correct:=v_correct+1;
    else
      insert into public.mentor_wrong_questions(
        user_id,question_id,subject_id,grade,selected_option,correct_option,explanation,question_snapshot,state,created_at
      ) values(
        s.user_id,q->>'id',s.subject_id,s.grade,coalesce(v_choice,''),coalesce(q->>'correctOption',''),
        coalesce(q->>'explanation',''),
        case when v_type='SINGLE_CHOICE' then q || jsonb_build_object('options',jsonb_build_array(
          jsonb_build_object('id','A','text',q->>'optionA'),
          jsonb_build_object('id','B','text',q->>'optionB'),
          jsonb_build_object('id','C','text',q->>'optionC'),
          jsonb_build_object('id','D','text',q->>'optionD')))
        else q end,'NEEDS_REVIEW',v_now
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

create or replace function public.mentor_exam_api(
  method text,path text,body jsonb default '{}'::jsonb,query jsonb default '{}'::jsonb
) returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  uid uuid:=auth.uid();
  e public.mentor_exams;
  s public.mentor_exam_sessions;
  qrow public.mentor_questions;
  v_payload jsonb;
  v_answer_json jsonb;
  v_type text;
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
      -- The list only needs metadata; full snapshots are loaded by the detail endpoint.
      v_items:=v_items||jsonb_build_array(jsonb_build_object(
        'id',s.id,'examId',s.exam_id,'examTitle',s.exam_title,'subjectId',s.subject_id,
        'grade',s.grade,'examType',s.exam_type,'semester',s.semester,
        'durationMinutes',s.duration_minutes,'state',s.state,'score',s.score,
        'score10',s.score_10,'rawScore',s.raw_score,'totalPoints',s.total_points,
        'correctCount',s.correct_count,'totalCount',s.total_count,
        'startedAt',extract(epoch from s.started_at)::bigint,
        'deadlineAt',extract(epoch from s.deadline_at)::bigint,
        'submittedAt',case when s.submitted_at is null then null else extract(epoch from s.submitted_at)::bigint end,
        'revision',s.revision,
        'gradingStatus',case when s.state='IN_PROGRESS' then 'NOT_STARTED'
          when exists(select 1 from jsonb_array_elements(s.question_snapshots) as q(value)
            where q.value->>'questionType'='ESSAY'
              and nullif(s.answers->(q.value->>'id')->>'selectedOption','') is not null
              and not (s.answers->(q.value->>'id') ? 'essayGrade')) then 'PENDING'
          else 'COMPLETE' end));
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
      if qrow.id is null then
        select p.payload into v_payload from public.mentor_exam_question_payloads p where p.id=v_id and p.exam_id=e.id;
        if v_payload is null then
          raise exception using errcode='PT400',message='EXAM_QUESTION_UNAVAILABLE';
        end if;
        if v_payload->>'subject_id'<>e.subject_id or (v_payload->>'grade')::int<>e.grade then raise exception using errcode='PT400',message='EXAM_QUESTION_SCOPE_MISMATCH'; end if;
        v_type:=coalesce(v_payload->>'question_type','SINGLE_CHOICE');
        if v_type not in ('SINGLE_CHOICE','TRUE_FALSE_4','SHORT_ANSWER','ESSAY') then raise exception using errcode='PT400',message='INVALID_QUESTION_TYPE'; end if;
        v_total:=v_total+round(coalesce((v_payload->>'points')::numeric,0)*10000)::int;
        v_snapshots:=v_snapshots||jsonb_build_array(jsonb_build_object(
          'id',v_id,'code',v_payload->>'code','prompt',v_payload->>'prompt',
          'optionA',v_payload->>'option_a','optionB',v_payload->>'option_b',
          'optionC',v_payload->>'option_c','optionD',v_payload->>'option_d',
          'correctOption',v_payload->>'correct_option','explanation',v_payload->>'explanation',
          'difficulty',v_payload->>'difficulty','chapter',v_payload->>'chapter','topic',v_payload->>'topic',
          'questionType',v_type,'contentData',coalesce(v_payload->'content_data','{}'::jsonb),
          'points',round(coalesce((v_payload->>'points')::numeric,0)*10000)::int,'pointScale',10000));
        continue;
      end if;
      if qrow.subject_id<>e.subject_id or qrow.grade<>e.grade then raise exception using errcode='PT400',message='EXAM_QUESTION_SCOPE_MISMATCH'; end if;
      v_total:=v_total+coalesce(qrow.points,10);
      v_snapshots:=v_snapshots||jsonb_build_array(jsonb_build_object(
        'id',qrow.id,'code',qrow.code,'prompt',qrow.prompt,'optionA',qrow.option_a,
        'optionB',qrow.option_b,'optionC',qrow.option_c,'optionD',qrow.option_d,
        'correctOption',qrow.correct_option,'explanation',qrow.explanation,
        'difficulty',qrow.difficulty,'chapter',qrow.chapter,'topic',qrow.topic,'points',coalesce(qrow.points,10),'pointScale',1,'questionType','SINGLE_CHOICE','contentData','{}'::jsonb));
    end loop;
    insert into public.mentor_exam_sessions(user_id,exam_id,subject_id,grade,exam_title,exam_type,semester,
      duration_minutes,question_snapshots,answers,state,total_points,total_count,started_at,deadline_at)
    values(uid,e.id,e.subject_id,e.grade,e.title,e.exam_type,e.semester,e.duration_minutes,v_snapshots,'{}','IN_PROGRESS',
      v_total,jsonb_array_length(v_snapshots),clock_timestamp(),clock_timestamp()+make_interval(mins=>e.duration_minutes)) returning * into s;
    return public.mentor_exam_session_view(s,false);
  elsif path ~ '^/exams/sessions/[^/]+/answers$' and method='POST' then
    v_id:=split_part(path,'/',4); v_question_id:=body->>'questionId';
    v_choice:=nullif(trim(body->>'selectedOption'),'');
    v_revision:=coalesce((body->>'revision')::int,0);
    select * into s from public.mentor_exam_sessions where id=v_id::uuid and user_id=uid for update;
    if s.id is null then raise exception using errcode='PT404',message='SESSION_NOT_FOUND'; end if;
    if s.state<>'IN_PROGRESS' then raise exception using errcode='PT409',message='EXAM_SESSION_CLOSED'; end if;
    if clock_timestamp()>=s.deadline_at then
      perform public.mentor_exam_finalize(s.id,'TIMED_OUT');
      select * into s from public.mentor_exam_sessions where id=s.id;
      return public.mentor_exam_session_view(s,true);
    end if;
    select q.value into v_payload from jsonb_array_elements(s.question_snapshots) q where q.value->>'id'=v_question_id;
    if v_payload is null then raise exception using errcode='PT400',message='QUESTION_NOT_IN_SESSION'; end if;
    v_type:=coalesce(v_payload->>'questionType','SINGLE_CHOICE');
    if v_type='SINGLE_CHOICE' then v_choice:=upper(v_choice); end if;
    if v_choice is not null then
      if v_type='SINGLE_CHOICE' and v_choice not in ('A','B','C','D') then raise exception using errcode='PT400',message='INVALID_ANSWER'; end if;
      if v_type='TRUE_FALSE_4' then
        if length(v_choice)>100 then raise exception using errcode='PT400',message='INVALID_ANSWER'; end if;
        begin v_answer_json:=v_choice::jsonb;
        exception when others then raise exception using errcode='PT400',message='INVALID_ANSWER'; end;
        if jsonb_typeof(v_answer_json)<>'object' then
          raise exception using errcode='PT400',message='INVALID_ANSWER';
        end if;
        if (select count(*) from jsonb_object_keys(v_answer_json))>4 or
          exists(select 1 from jsonb_each(v_answer_json) as entry(key,value)
            where entry.key not in ('a','b','c','d') or jsonb_typeof(entry.value)<>'boolean') then
          raise exception using errcode='PT400',message='INVALID_ANSWER';
        end if;
      end if;
      if v_type='SHORT_ANSWER' and length(v_choice)>200 then raise exception using errcode='PT400',message='INVALID_ANSWER'; end if;
      if v_type='ESSAY' and length(v_choice)>4000 then raise exception using errcode='PT400',message='INVALID_ANSWER'; end if;
    end if;
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

-- Only the authenticated Edge Function's service role can apply AI marks.
create or replace function public.mentor_apply_exam_essay_grades(p_session_id uuid,p_results jsonb)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  s public.mentor_exam_sessions;
  item jsonb;
  q jsonb;
  a jsonb;
  v_answers jsonb;
  v_points int;
  v_added int := 0;
  v_correct int := 0;
begin
  if auth.role() <> 'service_role' then raise exception using errcode='PT403',message='FORBIDDEN'; end if;
  if jsonb_typeof(p_results)<>'array' or jsonb_array_length(p_results)>10 then
    raise exception using errcode='PT400',message='INVALID_GRADES';
  end if;
  select * into s from public.mentor_exam_sessions where id=p_session_id for update;
  if s.id is null or s.state not in ('SUBMITTED','TIMED_OUT') then
    raise exception using errcode='PT409',message='EXAM_NOT_SUBMITTED';
  end if;
  v_answers:=s.answers;
  for item in select value from jsonb_array_elements(p_results) loop
    select value into q from jsonb_array_elements(s.question_snapshots) where value->>'id'=item->>'id';
    a:=v_answers->(item->>'id');
    if q is null or q->>'questionType'<>'ESSAY' or nullif(a->>'selectedOption','') is null then
      raise exception using errcode='PT400',message='INVALID_ESSAY_GRADE';
    end if;
    if a ? 'essayGrade' then continue; end if;
    if jsonb_typeof(item->'points')<>'number' or jsonb_typeof(item->'feedback')<>'string' then
      raise exception using errcode='PT400',message='INVALID_ESSAY_GRADE';
    end if;
    v_points:=(item->>'points')::int;
    if v_points<0 or v_points>(q->>'points')::int or length(trim(item->>'feedback')) not between 1 and 1000 then
      raise exception using errcode='PT400',message='INVALID_ESSAY_GRADE';
    end if;
    v_answers:=jsonb_set(v_answers,array[item->>'id','essayGrade'],
      jsonb_build_object('points',v_points,'feedback',trim(item->>'feedback'),'gradedAt',clock_timestamp()),true);
    v_added:=v_added+v_points;
    if v_points=(q->>'points')::int then v_correct:=v_correct+1; end if;
    if v_points<(q->>'points')::int then
      insert into public.mentor_wrong_questions(
        user_id,question_id,subject_id,grade,selected_option,correct_option,
        explanation,question_snapshot,state,created_at
      ) values(
        s.user_id,item->>'id',s.subject_id,s.grade,a->>'selectedOption','',
        coalesce(q->>'explanation','') || E'\nNhận xét AI: ' || trim(item->>'feedback'),
        q,'NEEDS_REVIEW',clock_timestamp()
      ) on conflict(user_id,question_id) do update set
        selected_option=excluded.selected_option,explanation=excluded.explanation,
        question_snapshot=excluded.question_snapshot,state='NEEDS_REVIEW',updated_at=clock_timestamp();
    end if;
  end loop;
  if v_answers is distinct from s.answers then
    update public.mentor_exam_sessions set answers=v_answers,raw_score=raw_score+v_added,
      score=score+v_added,score_10=case when total_points>0 then round((raw_score+v_added)::numeric/total_points*10,2) else 0 end,
      correct_count=correct_count+v_correct,revision=revision+1,updated_at=clock_timestamp()
      where id=s.id;
  end if;
  return jsonb_build_object('status','COMPLETE','awardedPoints',v_added);
end;
$$;
revoke all on function public.mentor_apply_exam_essay_grades(uuid,jsonb) from public,anon,authenticated;
grant execute on function public.mentor_apply_exam_essay_grades(uuid,jsonb) to service_role;

commit;
