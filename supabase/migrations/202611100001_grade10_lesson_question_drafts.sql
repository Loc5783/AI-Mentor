begin;

-- Keep the existing published lesson and review quizzes intact. Grade 10 lesson
-- questions currently have three choices; the admin bank requires four. Copy
-- them as drafts so an editor can write and verify choice D before publishing.
with lesson_questions as (
  select c.subject_id, c.unit_id, c.title, q.value as question
  from public.mentor_course_units c
  cross join lateral jsonb_array_elements(c.questions) q(value)
  where c.grade = 10 and c.kind = 'LESSON'
), checked as (
  select *,
    (select chr(64 + o.ordinality::int)
     from jsonb_array_elements(question->'options') with ordinality o(value, ordinality)
     where lower(o.value->>'id') = lower(coalesce(question->>'correctOptionId', question->>'correctOption'))
     limit 1) as answer
  from lesson_questions
  where case when jsonb_typeof(question->'options') = 'array'
      then jsonb_array_length(question->'options') = 3 else false end
    and nullif(trim(question->>'id'), '') is not null
    and nullif(trim(question->>'prompt'), '') is not null
    and nullif(trim(question->>'explanation'), '') is not null
)
insert into public.mentor_questions (
  id, code, subject_id, grade, chapter, topic, textbook, prompt,
  option_a, option_b, option_c, option_d, correct_option, explanation,
  difficulty, points, source, author, license, status
)
select
  'g10-lesson-' || (question->>'id'),
  'AIM_G10_LESSON_' || upper(replace(question->>'id', '-', '_')),
  subject_id, 10, title, title, null, question->>'prompt',
  question->'options'->0->>'text',
  question->'options'->1->>'text',
  question->'options'->2->>'text',
  '[Cần biên tập phương án D trước khi xuất bản]',
  answer, question->>'explanation',
  case upper(coalesce(question->>'difficulty', 'MEDIUM'))
    when 'EASY' then 'EASY' when 'HARD' then 'HARD' else 'MEDIUM' end,
  10, 'course_unit:' || unit_id, 'AI Mentor', 'Nội bộ', 'DRAFT'
from checked
where answer is not null
on conflict (id) do nothing;

commit;
