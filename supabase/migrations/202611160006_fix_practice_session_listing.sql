-- Avoid a PL/pgSQL record named q shadowing the JSON array alias q(value,i).
begin;
create or replace function public.mentor_library_api(method text,path text,body jsonb default '{}',query jsonb default '{}') returns jsonb
language plpgsql security definer set search_path='' as $$
declare
  uid uuid:=auth.uid(); quiz_row public.mentor_library_quizzes; s public.mentor_practice_sessions;
  sid uuid; qid text; question jsonb; answer text; correct text; items jsonb; state_value text;
  entry public.mentor_wrong_questions;
begin
  if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
  if path='/library/quizzes' and method='GET' then
    select coalesce(jsonb_agg(jsonb_build_object('id',id,'title',title,'subjectId',subject_id,'grade',grade,
      'chapter',chapter,'topic',topic,'difficulty',difficulty,'questionCount',jsonb_array_length(questions)) order by created_at desc,id),'[]')
      into items from public.mentor_library_quizzes where status='PUBLISHED'
        and (coalesce(query->>'subject_id',query->>'subjectId','')='' or subject_id=coalesce(query->>'subject_id',query->>'subjectId'))
        and (coalesce(query->>'grade','')='' or grade=(query->>'grade')::int);
    return jsonb_build_object('items',items);
  elsif path='/library/sessions' and method='GET' then
    select coalesce(jsonb_agg(jsonb_build_object('id',id,'quizId',quiz_id,'subjectId',subject_id,'grade',grade,
      'state',state,'score',score,'correctCount',correct_count,'totalCount',jsonb_array_length(question_snapshots),
      'answeredCount',(select count(*) from jsonb_object_keys(answers)),'firstUnansweredIndex',coalesce((select min(i-1) from jsonb_array_elements(question_snapshots) with ordinality as q(value,i) where not(answers ? (q.value->>'id'))),0),
      'updatedAt',updated_at) order by updated_at desc),'[]') into items
      from public.mentor_practice_sessions where user_id=uid and state='IN_PROGRESS'
        and (coalesce(query->>'subject_id',query->>'subjectId','')='' or subject_id=coalesce(query->>'subject_id',query->>'subjectId'))
        and (coalesce(query->>'grade','')='' or grade=(query->>'grade')::int);
    return jsonb_build_object('items',items);
  elsif path='/library/sessions' and method='POST' then
    select * into quiz_row from public.mentor_library_quizzes where id=body->>'quizId' and status='PUBLISHED';
    if quiz_row.id is null then raise sqlstate 'PT404' using message='QUIZ_NOT_FOUND'; end if;
    if not exists(select 1 from public.mentor_enrollments where user_id=uid and subject_id=quiz_row.subject_id and grade=quiz_row.grade and selected) then
      raise sqlstate 'PT409' using message='SUBJECT_NOT_SELECTED';
    end if;
    insert into public.mentor_practice_sessions(user_id,quiz_id,subject_id,grade,question_snapshots)
      values(uid,quiz_row.id,quiz_row.subject_id,quiz_row.grade,quiz_row.questions) returning * into s;
    return public.mentor_library_view(s);
  elsif path ~ '^/library/sessions/[^/]+/answers$' and method='POST' then
    sid:=split_part(path,'/',4)::uuid; qid:=body->>'questionId'; answer:=upper(trim(body->>'selectedOption'));
    select * into s from public.mentor_practice_sessions where id=sid and user_id=uid for update;
    if s.id is null then raise sqlstate 'PT404' using message='SESSION_NOT_FOUND'; end if;
    if s.state<>'IN_PROGRESS' then raise sqlstate 'PT409' using message='SESSION_COMPLETED'; end if;
    select value into question from jsonb_array_elements(s.question_snapshots) where value->>'id'=qid;
    if question is null or not exists(select 1 from jsonb_array_elements(question->'options') o where o->>'id'=answer) then
      raise sqlstate 'PT400' using message='INVALID_ANSWER';
    end if;
    if s.answers ? qid then
      if s.answers->>qid<>answer then raise sqlstate 'PT409' using message='ANSWER_ALREADY_SUBMITTED'; end if;
      return public.mentor_library_view(s);
    end if;
    correct:=question->>'correctOption';
    update public.mentor_practice_sessions set answers=answers||jsonb_build_object(qid,answer),
      correct_count=correct_count+case when answer=correct then 1 else 0 end,
      score=score+case when answer=correct then coalesce((question->>'points')::int,10) else 0 end,
      updated_at=now() where id=sid returning * into s;
    if answer<>correct then
      insert into public.mentor_wrong_questions(user_id,question_id,subject_id,grade,selected_option,correct_option,explanation,question_snapshot)
        values(uid,qid,s.subject_id,s.grade,answer,correct,coalesce(question->>'explanation',''),question)
      on conflict(user_id,question_id) do update set selected_option=excluded.selected_option,
        correct_option=excluded.correct_option,explanation=excluded.explanation,question_snapshot=excluded.question_snapshot,
        state='NEEDS_REVIEW',updated_at=now();
    end if;
    return public.mentor_library_view(s);
  elsif path ~ '^/library/sessions/[^/]+/complete$' and method='POST' then
    sid:=split_part(path,'/',4)::uuid;
    select * into s from public.mentor_practice_sessions where id=sid and user_id=uid for update;
    if s.id is null then raise sqlstate 'PT404' using message='SESSION_NOT_FOUND'; end if;
    if (select count(*) from jsonb_object_keys(s.answers))<jsonb_array_length(s.question_snapshots) then raise sqlstate 'PT409' using message='PRACTICE_NOT_FINISHED'; end if;
    update public.mentor_practice_sessions set state='COMPLETED',updated_at=now() where id=sid returning * into s;
    return public.mentor_library_view(s);
  elsif path ~ '^/library/sessions/[^/]+$' and method='GET' then
    sid:=split_part(path,'/',4)::uuid;
    select * into s from public.mentor_practice_sessions where id=sid and user_id=uid;
    if s.id is null then raise sqlstate 'PT404' using message='SESSION_NOT_FOUND'; end if;
    return public.mentor_library_view(s);
  elsif path='/me/wrong-questions' and method='GET' then
    select coalesce(jsonb_agg(jsonb_build_object('id',w.id,'questionId',w.question_id,'subjectId',w.subject_id,
      'grade',w.grade,'chapter',coalesce(w.question_snapshot->>'chapter','Chưa phân loại'),
      'topic',coalesce(w.question_snapshot->>'topic','Chủ đề chung'),'prompt',w.question_snapshot->>'prompt',
      'options',w.question_snapshot->'options','selectedOption',w.selected_option,'correctOption',w.correct_option,
      'explanation',w.explanation,'difficulty',coalesce(w.question_snapshot->>'difficulty','MEDIUM'),
      'state',w.state,'hasSnapshot',true,'canRetry',true,'createdAt',w.created_at,'updatedAt',w.updated_at)
      order by w.updated_at desc nulls last,w.created_at desc),'[]') into items
      from public.mentor_wrong_questions w where w.user_id=uid
        and (coalesce(query->>'subject_id',query->>'subjectId','')='' or w.subject_id=coalesce(query->>'subject_id',query->>'subjectId'))
        and (coalesce(query->>'grade','')='' or w.grade=(query->>'grade')::int)
        and (coalesce(query->>'state','')='' or query->>'state'='ALL' or w.state=query->>'state');
    return jsonb_build_object('items',items);
  elsif path ~ '^/me/wrong-questions/[^/]+/retry$' and method='POST' then
    sid:=split_part(path,'/',4)::uuid; answer:=upper(trim(body->>'selectedOption'));
    select * into entry from public.mentor_wrong_questions where id=sid and user_id=uid for update;
    if entry.id is null then raise sqlstate 'PT404' using message='WRONG_QUESTION_NOT_FOUND'; end if;
    question:=entry.question_snapshot;
    if not exists(select 1 from jsonb_array_elements(question->'options') o where o->>'id'=answer) then raise sqlstate 'PT400' using message='INVALID_ANSWER'; end if;
    update public.mentor_wrong_questions set selected_option=answer,
      state=case when answer=entry.correct_option then 'RESOLVED' else 'NEEDS_REVIEW' end,updated_at=now()
      where id=sid returning * into entry;
    return jsonb_build_object('entryId',entry.id,'questionId',entry.question_id,'selectedOption',answer,
      'correctOption',entry.correct_option,'isCorrect',answer=entry.correct_option,'state',entry.state,
      'explanation',entry.explanation,'updatedAt',entry.updated_at);
  end if;
  raise sqlstate 'PT404' using message='API_NOT_IMPLEMENTED';
end; $$;
commit;
