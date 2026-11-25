begin;

-- Provide ten practice sets per Grade 10 subject without claiming to cover
-- the full school-year syllabus. These mixed sets reuse only the questions in
-- the 15–20 Grade 10 lessons currently shipped by AI Mentor. Each set has 15
-- distinct questions; reuse across different sets is intentional for review.
with subjects(subject_id) as (
  values ('math'), ('literature'), ('english'), ('history'), ('geography'),
         ('economics_law'), ('physics'), ('chemistry'), ('biology')
), existing_counts as (
  select s.subject_id, count(q.id)::int as existing_count
  from subjects s
  left join public.mentor_library_quizzes q
    on q.subject_id = s.subject_id and q.grade = 10 and q.status = 'PUBLISHED'
  group by s.subject_id
), variants as (
  select e.subject_id, n.variant
  from existing_counts e
  cross join lateral generate_series(1, greatest(0, 10 - e.existing_count)) n(variant)
), lesson_questions as (
  select c.subject_id, q.value as question
  from public.mentor_course_units c
  cross join lateral jsonb_array_elements(c.questions) q(value)
  where c.grade = 10 and c.kind = 'LESSON'
    and jsonb_typeof(c.questions) = 'array'
    and nullif(trim(q.value->>'id'), '') is not null
    and nullif(trim(q.value->>'prompt'), '') is not null
    and nullif(trim(q.value->>'explanation'), '') is not null
    and jsonb_typeof(q.value->'options') = 'array'
    and jsonb_array_length(q.value->'options') = 3
    and upper(coalesce(q.value->>'correctOptionId', q.value->>'correctOption')) in ('A','B','C')
), ranked as (
  select v.subject_id, v.variant,
    row_number() over(partition by v.subject_id, v.variant
      order by md5(v.subject_id || ':' || v.variant || ':' || (l.question->>'id'))) as question_no,
    jsonb_build_object(
      'id', l.question->>'id', 'prompt', l.question->>'prompt',
      'options', (select jsonb_agg(jsonb_build_object('id',upper(o.value->>'id'),
        'text',o.value->>'text') order by o.ordinality)
        from jsonb_array_elements(l.question->'options') with ordinality o(value, ordinality)),
      'correctOption', upper(coalesce(l.question->>'correctOptionId',l.question->>'correctOption')),
      'explanation', l.question->>'explanation',
      'difficulty', case upper(coalesce(l.question->>'difficulty','MEDIUM'))
        when 'EASY' then 'EASY' when 'HARD' then 'HARD' else 'MEDIUM' end,
      'points', 10
    ) as item
  from variants v
  join lesson_questions l on l.subject_id = v.subject_id
), sets as (
  select subject_id, variant, jsonb_agg(item order by question_no) as questions
  from ranked
  where question_no <= 15
  group by subject_id, variant
)
insert into public.mentor_library_quizzes (
  id, title, subject_id, grade, chapter, topic, difficulty, questions, status
)
select 'grade10-mixed-' || subject_id || '-' || lpad(variant::text,2,'0'),
  'Đề luyện tổng hợp ' || lpad(variant::text,2,'0'),
  subject_id, 10, 'Các bài lớp 10 hiện có',
  'Luyện tổng hợp lộ trình hiện có', 'MEDIUM', questions, 'PUBLISHED'
from sets
where jsonb_array_length(questions) = 15
on conflict (id) do nothing;

commit;
