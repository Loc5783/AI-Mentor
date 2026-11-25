-- Count answer keys using a PostgreSQL-supported JSONB set-returning function.
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
    'totalCount',p_session.total_count,'answeredCount',(select count(*) from jsonb_object_keys(v_answers)),
    'questions',v_questions,'answers',v_answers,'questionRevisions',v_revisions,
    'revision',p_session.revision,'createdAt',p_session.created_at,'updatedAt',p_session.updated_at
  );
end;
$$;
commit;
