-- Read-only, account-scoped history for library and AI practice.
-- Keep answer keys out of list responses; details still use their existing APIs.
create or replace function public.mentor_learning_session_history(
  p_kind text, p_subject_id text default null, p_grade integer default null,
  p_limit integer default 50
) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_uid uuid := auth.uid();
  v_items jsonb;
  v_limit integer := least(greatest(coalesce(p_limit, 50), 1), 100);
begin
  if v_uid is null then
    raise exception using errcode = 'PT401', message = 'AUTHENTICATION_REQUIRED';
  end if;
  if p_grade is not null and p_grade not between 10 and 12 then
    raise exception using errcode = 'PT400', message = 'INVALID_GRADE';
  end if;

  if p_kind = 'library' then
    select coalesce(jsonb_agg(item order by changed_at desc, session_id desc), '[]'::jsonb)
      into v_items from (
        select s.id as session_id, s.updated_at as changed_at,
          jsonb_build_object(
            'id', s.id, 'quizId', s.quiz_id, 'subjectId', s.subject_id,
            'grade', s.grade, 'state', s.state, 'score', s.score,
            'correctCount', s.correct_count,
            'totalCount', jsonb_array_length(s.question_snapshots),
            'answeredCount', (select count(*) from jsonb_object_keys(s.answers)),
            'firstUnansweredIndex', coalesce((select min(i - 1)
              from jsonb_array_elements(s.question_snapshots) with ordinality as q(value, i)
              where not (s.answers ? (q.value->>'id'))), 0),
            'updatedAt', s.updated_at) as item
        from public.mentor_practice_sessions s
        where s.user_id = v_uid and s.state in ('IN_PROGRESS', 'COMPLETED')
          and (p_subject_id is null or s.subject_id = p_subject_id)
          and (p_grade is null or s.grade = p_grade)
        order by s.updated_at desc, s.id desc limit v_limit
      ) recent;
  elsif p_kind = 'ai_practice' then
    select coalesce(jsonb_agg(item order by changed_at desc, session_id desc), '[]'::jsonb)
      into v_items from (
        select s.id as session_id, s.updated_at as changed_at,
          jsonb_build_object(
            'id', s.id, 'subjectId', s.subject_id, 'grade', s.grade,
            'topic', s.topic, 'questionCount', s.question_count,
            'answeredCount', (select count(*) from jsonb_object_keys(s.answers)),
            'state', s.state, 'updatedAt', s.updated_at) as item
        from public.mentor_ai_practice_sessions s
        where s.user_id = v_uid and s.state in ('IN_PROGRESS', 'COMPLETED')
          and (p_subject_id is null or s.subject_id = p_subject_id)
          and (p_grade is null or s.grade = p_grade)
        order by s.updated_at desc, s.id desc limit v_limit
      ) recent;
  else
    raise exception using errcode = 'PT400', message = 'INVALID_HISTORY_KIND';
  end if;
  return jsonb_build_object('items', v_items);
end;
$$;

revoke all on function public.mentor_learning_session_history(text,text,integer,integer)
  from public, anon;
grant execute on function public.mentor_learning_session_history(text,text,integer,integer)
  to authenticated;
