-- Record a wrong AI-generated question without granting direct notebook writes.
create or replace function public.mentor_ai_record_wrong(
  p_session_id uuid,
  p_question_id text
) returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
  v_session public.mentor_ai_practice_sessions%rowtype;
  v_question jsonb;
  v_answer jsonb;
  v_notebook_question jsonb;
begin
  if auth.uid() is null then
    raise exception 'Authentication required' using errcode = '28000';
  end if;

  select * into v_session
  from public.mentor_ai_practice_sessions
  where id = p_session_id and user_id = auth.uid();
  if not found then
    raise exception 'Session not found' using errcode = 'P0002';
  end if;

  select item.value into v_question
  from jsonb_array_elements(v_session.questions_snapshot) as item(value)
  where item.value->>'id' = p_question_id;
  v_answer := v_session.answers->p_question_id;
  if v_question is null or v_answer is null
      or (v_answer->>'revealed')::boolean is not true
      or (v_answer->>'isCorrect')::boolean is true then
    raise exception 'Wrong answer not recorded' using errcode = '22023';
  end if;

  -- The shared wrong-answer notebook uses an array of {id,text} options.
  v_notebook_question := jsonb_build_object(
    'id', p_question_id,
    'prompt', v_question->>'prompt',
    'options', jsonb_build_array(
      jsonb_build_object('id', 'A', 'text', v_question->'options'->>'A'),
      jsonb_build_object('id', 'B', 'text', v_question->'options'->>'B'),
      jsonb_build_object('id', 'C', 'text', v_question->'options'->>'C'),
      jsonb_build_object('id', 'D', 'text', v_question->'options'->>'D')
    ),
    'correctOption', v_question->>'correctOption',
    'explanation', v_question->>'explanation',
    'chapter', 'Gia sư AI',
    'topic', v_session.topic,
    'difficulty', v_session.difficulty
  );

  insert into public.mentor_wrong_questions (
    user_id, question_id, subject_id, grade, selected_option,
    correct_option, explanation, question_snapshot, state, updated_at
  ) values (
    auth.uid(), p_question_id, v_session.subject_id, v_session.grade,
    coalesce(v_answer->>'selectedOption', ''), v_question->>'correctOption',
    coalesce(v_question->>'explanation', ''), v_notebook_question, 'NEEDS_REVIEW', now()
  )
  on conflict (user_id, question_id) do update set
    selected_option = excluded.selected_option,
    correct_option = excluded.correct_option,
    explanation = excluded.explanation,
    question_snapshot = excluded.question_snapshot,
    state = 'NEEDS_REVIEW',
    updated_at = now();
end;
$$;

revoke all on function public.mentor_ai_record_wrong(uuid, text) from public, anon;
grant execute on function public.mentor_ai_record_wrong(uuid, text) to authenticated;
