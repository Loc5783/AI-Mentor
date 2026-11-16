begin;

-- Keep generated answer keys and scoring on the server. Mobile clients receive
-- only the fields needed for the current learning state.
revoke all on public.mentor_ai_practice_sessions, public.mentor_ai_question_reports
  from public, anon, authenticated;
revoke all on function public.mentor_ai_record_wrong(uuid, text) from public, anon, authenticated;

create or replace function public.mentor_ai_practice_session_view(p_session public.mentor_ai_practice_sessions)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_questions jsonb := '[]'::jsonb;
  v_question jsonb;
  v_answer jsonb;
  v_key text;
  v_revealed boolean;
begin
  for v_question in select value from jsonb_array_elements(p_session.questions_snapshot) loop
    v_answer := coalesce(p_session.answers->(v_question->>'id'), '{}'::jsonb);
    v_revealed := p_session.state = 'COMPLETED' or coalesce((v_answer->>'revealed')::boolean, false);
    if v_revealed then
      v_questions := v_questions || jsonb_build_array(v_question);
    else
      v_questions := v_questions || jsonb_build_array(v_question - 'correctOption' - 'explanation');
    end if;
  end loop;
  return jsonb_build_object(
    'id', p_session.id,
    'subject_id', p_session.subject_id,
    'grade', p_session.grade,
    'topic', p_session.topic,
    'difficulty', p_session.difficulty,
    'question_count', p_session.question_count,
    'questions_snapshot', v_questions,
    'answers', p_session.answers,
    'current_index', p_session.current_index,
    'state', p_session.state,
    'summary', p_session.summary,
    'created_at', p_session.created_at,
    'updated_at', p_session.updated_at
  );
end;
$$;

create or replace function public.mentor_ai_practice_api(
  p_action text,
  p_session_id uuid default null,
  p_subject_id text default null,
  p_grade int default null,
  p_topic text default null,
  p_difficulty text default null,
  p_question_count int default null,
  p_questions jsonb default null,
  p_question_id text default null,
  p_option text default null,
  p_submission_id uuid default null,
  p_reason text default null,
  p_details text default null
) returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_session public.mentor_ai_practice_sessions%rowtype;
  v_question jsonb;
  v_answer jsonb;
  v_previous jsonb;
  v_record jsonb;
  v_summary jsonb;
  v_id text;
  v_correct boolean;
  v_first boolean;
  v_attempts int;
  v_skipped boolean;
  v_questions jsonb;
  v_notebook_question jsonb;
begin
  if v_uid is null then raise exception using errcode='PT401', message='AUTHENTICATION_REQUIRED'; end if;

  if p_action='create' then
    if jsonb_typeof(p_questions)<>'array' then
      raise exception using errcode='PT400',message='INVALID_PRACTICE_SESSION';
    end if;
    if p_session_id is null or p_subject_id is null or p_subject_id not in
      ('math','physics','chemistry','biology','literature','history','geography','english','economics_law')
      or p_grade is null or p_grade not between 10 and 12
      or p_difficulty is null or p_difficulty not in ('EASY','MEDIUM','HARD')
      or length(trim(coalesce(p_topic,''))) not between 1 and 100
      or p_question_count is null or p_question_count not between 1 and 10
      or jsonb_array_length(p_questions)<>p_question_count then
      raise exception using errcode='PT400',message='INVALID_PRACTICE_SESSION';
    end if;
    insert into public.mentor_ai_practice_sessions(id,user_id,subject_id,grade,topic,difficulty,
      question_count,questions_snapshot,answers,current_index,state)
    values(p_session_id,v_uid,p_subject_id,p_grade,trim(p_topic),p_difficulty,p_question_count,
      p_questions,'{}'::jsonb,0,'IN_PROGRESS')
    on conflict(id) do nothing;
    select * into v_session from public.mentor_ai_practice_sessions
      where id=p_session_id and user_id=v_uid;
    if v_session.id is null then raise exception using errcode='PT409',message='SESSION_ID_CONFLICT'; end if;
    if v_session.subject_id<>p_subject_id or v_session.grade<>p_grade or v_session.topic<>trim(p_topic)
      or v_session.difficulty<>p_difficulty or v_session.question_count<>p_question_count then
      raise exception using errcode='PT409',message='SESSION_ID_CONFLICT';
    end if;
    return public.mentor_ai_practice_session_view(v_session);
  elsif p_action='get' then
    select * into v_session from public.mentor_ai_practice_sessions
      where id=p_session_id and user_id=v_uid;
    if v_session.id is null then raise exception using errcode='PT404',message='SESSION_NOT_FOUND'; end if;
    return public.mentor_ai_practice_session_view(v_session);
  elsif p_action='resumable' then
    select * into v_session from public.mentor_ai_practice_sessions
      where user_id=v_uid and state='IN_PROGRESS' order by updated_at desc limit 1;
    if v_session.id is null then return null; end if;
    return public.mentor_ai_practice_session_view(v_session);
  elsif p_action in ('answer','skip') then
    select * into v_session from public.mentor_ai_practice_sessions
      where id=p_session_id and user_id=v_uid for update;
    if v_session.id is null then raise exception using errcode='PT404',message='SESSION_NOT_FOUND'; end if;
    if v_session.state<>'IN_PROGRESS' then raise exception using errcode='PT409',message='SESSION_CLOSED'; end if;
    select value into v_question from jsonb_array_elements(v_session.questions_snapshot)
      where value->>'id'=p_question_id;
    if v_question is null then raise exception using errcode='PT404',message='QUESTION_NOT_FOUND'; end if;
    v_previous:=v_session.answers->p_question_id;
    if p_action='answer' then
      if p_option not in ('A','B','C','D') or p_submission_id is null then
        raise exception using errcode='PT400',message='INVALID_ANSWER';
      end if;
      if v_previous->>'submissionId'=p_submission_id::text then
        v_record:=v_previous;
      else
        v_correct:=p_option=v_question->>'correctOption';
        v_first:=case when v_previous is null then v_correct else coalesce((v_previous->>'firstTryCorrect')::boolean,false) end;
        v_attempts:=coalesce((v_previous->>'attemptsCount')::int,0)+1;
        v_record:=jsonb_build_object('selectedOption',p_option,'isCorrect',v_correct,
          'firstTryCorrect',v_first,'attemptsCount',v_attempts,'revealed',true,'skipped',false,
          'submissionId',p_submission_id);
      end if;
    else
      if v_previous is not null and coalesce((v_previous->>'skipped')::boolean,false)=false then
        raise exception using errcode='PT409',message='ANSWER_ALREADY_RECORDED';
      end if;
      v_record:=coalesce(v_previous,jsonb_build_object('selectedOption',null,'isCorrect',false,
        'firstTryCorrect',false,'attemptsCount',0,'revealed',true,'skipped',true));
    end if;
    update public.mentor_ai_practice_sessions set
      answers=jsonb_set(answers,array[p_question_id],v_record,true),
      current_index=greatest(current_index,coalesce((v_question->>'order')::int,1)),updated_at=now()
      where id=v_session.id returning * into v_session;
    v_correct:=coalesce((v_record->>'isCorrect')::boolean,false);
    v_skipped:=coalesce((v_record->>'skipped')::boolean,false);
    if not v_correct or v_skipped then
      v_notebook_question:=jsonb_build_object('id',p_question_id,'prompt',v_question->>'prompt',
        'options',jsonb_build_array(
          jsonb_build_object('id','A','text',v_question->'options'->>'A'),
          jsonb_build_object('id','B','text',v_question->'options'->>'B'),
          jsonb_build_object('id','C','text',v_question->'options'->>'C'),
          jsonb_build_object('id','D','text',v_question->'options'->>'D')),
        'correctOption',v_question->>'correctOption','explanation',v_question->>'explanation',
        'chapter','Gia sư AI','topic',v_session.topic,'difficulty',v_session.difficulty);
      insert into public.mentor_wrong_questions(user_id,question_id,subject_id,grade,selected_option,
        correct_option,explanation,question_snapshot,state,updated_at)
      values(v_uid,p_question_id,v_session.subject_id,v_session.grade,coalesce(v_record->>'selectedOption',''),
        v_question->>'correctOption',coalesce(v_question->>'explanation',''),v_notebook_question,'NEEDS_REVIEW',now())
      on conflict(user_id,question_id) do update set selected_option=excluded.selected_option,
        correct_option=excluded.correct_option,explanation=excluded.explanation,
        question_snapshot=excluded.question_snapshot,state='NEEDS_REVIEW',updated_at=now();
    end if;
    return jsonb_build_object('questionId',p_question_id,'isCorrect',v_correct,
      'selectedOption',v_record->'selectedOption','correctOption',v_question->>'correctOption',
      'explanation',v_question->>'explanation','hint',coalesce(v_question->>'hint',''),
      'firstTryCorrect',coalesce((v_record->>'firstTryCorrect')::boolean,false),
      'attemptsCount',coalesce((v_record->>'attemptsCount')::int,0),'isSkipped',v_skipped);
  elsif p_action='complete' then
    select * into v_session from public.mentor_ai_practice_sessions
      where id=p_session_id and user_id=v_uid for update;
    if v_session.id is null then raise exception using errcode='PT404',message='SESSION_NOT_FOUND'; end if;
    if v_session.state='IN_PROGRESS' then
      v_questions:=v_session.questions_snapshot;
      v_summary:=jsonb_build_object('totalQuestions',jsonb_array_length(v_questions),
        'firstTryCorrectCount',0,'assistedCount',0,'incorrectCount',0,'skippedCount',0,'accuracyFirstTry',0);
      for v_question in select value from jsonb_array_elements(v_questions) loop
        v_answer:=v_session.answers->(v_question->>'id');
        if v_answer is null or coalesce((v_answer->>'skipped')::boolean,false) then
          v_summary:=jsonb_set(v_summary,'{skippedCount}',to_jsonb((v_summary->>'skippedCount')::int+1));
        elsif coalesce((v_answer->>'firstTryCorrect')::boolean,false) then
          v_summary:=jsonb_set(v_summary,'{firstTryCorrectCount}',to_jsonb((v_summary->>'firstTryCorrectCount')::int+1));
        elsif coalesce((v_answer->>'isCorrect')::boolean,false) then
          v_summary:=jsonb_set(v_summary,'{assistedCount}',to_jsonb((v_summary->>'assistedCount')::int+1));
        else
          v_summary:=jsonb_set(v_summary,'{incorrectCount}',to_jsonb((v_summary->>'incorrectCount')::int+1));
        end if;
      end loop;
      v_summary:=jsonb_set(v_summary,'{accuracyFirstTry}',to_jsonb(
        (v_summary->>'firstTryCorrectCount')::numeric*100/jsonb_array_length(v_questions)));
      update public.mentor_ai_practice_sessions set state='COMPLETED',summary=v_summary::text,
        current_index=question_count,updated_at=now() where id=v_session.id returning * into v_session;
    end if;
    return public.mentor_ai_practice_session_view(v_session);
  elsif p_action='report' then
    select * into v_session from public.mentor_ai_practice_sessions
      where id=p_session_id and user_id=v_uid;
    if v_session.id is null then raise exception using errcode='PT404',message='SESSION_NOT_FOUND'; end if;
    if not exists(select 1 from jsonb_array_elements(v_session.questions_snapshot) q where q->>'id'=p_question_id) then
      raise exception using errcode='PT404',message='QUESTION_NOT_FOUND';
    end if;
    if length(trim(coalesce(p_reason,''))) not between 1 and 200 or length(coalesce(p_details,''))>2000 then
      raise exception using errcode='PT400',message='INVALID_REPORT';
    end if;
    insert into public.mentor_ai_question_reports(user_id,session_id,question_id,reason,details)
      values(v_uid,p_session_id,p_question_id,trim(p_reason),trim(coalesce(p_details,'')));
    return jsonb_build_object('message','Cảm ơn bạn đã phản hồi chất lượng câu hỏi.');
  elsif p_action='explain_context' then
    select * into v_session from public.mentor_ai_practice_sessions
      where id=p_session_id and user_id=v_uid;
    if v_session.id is null then raise exception using errcode='PT404',message='SESSION_NOT_FOUND'; end if;
    select value into v_question from jsonb_array_elements(v_session.questions_snapshot)
      where value->>'id'=p_question_id;
    v_answer:=v_session.answers->p_question_id;
    if v_question is null then raise exception using errcode='PT404',message='QUESTION_NOT_FOUND'; end if;
    if v_session.state<>'COMPLETED' and coalesce((v_answer->>'revealed')::boolean,false)=false then
      raise exception using errcode='PT409',message='ANSWER_REQUIRED';
    end if;
    return jsonb_build_object('grade',v_session.grade,'prompt',v_question->>'prompt',
      'correctOption',v_question->>'correctOption','explanation',v_question->>'explanation');
  end if;
  raise exception using errcode='PT404',message='API_NOT_IMPLEMENTED';
end;
$$;

revoke all on function public.mentor_ai_practice_session_view(public.mentor_ai_practice_sessions) from public,anon,authenticated;
revoke all on function public.mentor_ai_practice_api(text,uuid,text,int,text,text,int,jsonb,text,text,uuid,text,text) from public,anon;
grant execute on function public.mentor_ai_practice_api(text,uuid,text,int,text,text,int,jsonb,text,text,uuid,text,text) to authenticated;

commit;
