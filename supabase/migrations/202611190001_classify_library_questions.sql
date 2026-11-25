begin;

-- Quizzes made from lesson questions used to keep chapter/topic only on the quiz row.
-- Copy the metadata onto each question so future practice and notebook snapshots retain it.
update public.mentor_library_quizzes quiz
set questions = (
  select jsonb_agg(
    question.value || jsonb_build_object(
      'chapter', coalesce(nullif(question.value->>'chapter', ''), nullif(quiz.chapter, ''), 'Ôn tập tổng hợp'),
      'topic', coalesce(nullif(question.value->>'topic', ''), nullif(quiz.topic, ''), 'Ôn tập tổng hợp'))
    order by question.ordinality)
  from jsonb_array_elements(quiz.questions) with ordinality as question(value, ordinality)
)
where exists (
  select 1 from jsonb_array_elements(quiz.questions) as question(value)
  where nullif(question.value->>'chapter', '') is null
     or nullif(question.value->>'topic', '') is null
);

-- Existing sessions are immutable for answers and scoring; only enrich their question labels.
update public.mentor_practice_sessions session
set question_snapshots = (
  select jsonb_agg(
    question.value || jsonb_build_object(
      'chapter', coalesce(nullif(question.value->>'chapter', ''), nullif(quiz.chapter, ''), 'Ôn tập tổng hợp'),
      'topic', coalesce(nullif(question.value->>'topic', ''), nullif(quiz.topic, ''), 'Ôn tập tổng hợp'))
    order by question.ordinality)
  from jsonb_array_elements(session.question_snapshots) with ordinality as question(value, ordinality)
)
from public.mentor_library_quizzes quiz
where session.quiz_id = quiz.id
  and exists (
    select 1 from jsonb_array_elements(session.question_snapshots) as question(value)
    where nullif(question.value->>'chapter', '') is null
       or nullif(question.value->>'topic', '') is null
  );

-- Repair older notebook entries by their original subject/grade/question identity.
-- Entries from other sources without a matching quiz keep their original snapshot.
with labels as (
  select wrong.id,
    coalesce(nullif(wrong.question_snapshot->>'chapter', ''), nullif(quiz.chapter, ''), 'Ôn tập tổng hợp') as chapter,
    coalesce(nullif(wrong.question_snapshot->>'topic', ''), nullif(quiz.topic, ''), 'Ôn tập tổng hợp') as topic
  from public.mentor_wrong_questions wrong
  join lateral (
    select q.chapter, q.topic
    from public.mentor_library_quizzes q
    where q.subject_id = wrong.subject_id and q.grade = wrong.grade
      and exists (
        select 1 from jsonb_array_elements(q.questions) as question(value)
        where question.value->>'id' = wrong.question_id
      )
    order by q.created_at desc, q.id
    limit 1
  ) quiz on true
  where nullif(wrong.question_snapshot->>'chapter', '') is null
     or nullif(wrong.question_snapshot->>'topic', '') is null
)
update public.mentor_wrong_questions wrong
set question_snapshot = wrong.question_snapshot ||
    jsonb_build_object('chapter', labels.chapter, 'topic', labels.topic)
from labels where wrong.id = labels.id;

commit;
