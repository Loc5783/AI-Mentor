begin;

-- Publish 35 existing, explained Physics 10 lesson questions in the standalone
-- question bank. The lesson catalog already contains one verified answer per
-- question; the fourth choice is deliberately false for that format.
with lesson_questions as (
  select q.value as item,
    row_number() over (order by c.position, q.ordinality) as position
  from public.mentor_course_units c
  cross join lateral jsonb_array_elements(c.questions) with ordinality q(value, ordinality)
  where c.subject_id = 'physics' and c.grade = 10 and c.kind = 'LESSON'
    and jsonb_typeof(c.questions) = 'array'
    and jsonb_typeof(q.value->'options') = 'array'
    and jsonb_array_length(q.value->'options') = 3
    and upper(q.value->>'correctOptionId') in ('A', 'B', 'C')
    and nullif(trim(q.value->>'prompt'), '') is not null
    and nullif(trim(q.value->>'explanation'), '') is not null
), selected as (
  select item, position,
    (select o.value->>'text' from jsonb_array_elements(item->'options') o(value)
      where upper(o.value->>'id') = 'A' limit 1) as option_a,
    (select o.value->>'text' from jsonb_array_elements(item->'options') o(value)
      where upper(o.value->>'id') = 'B' limit 1) as option_b,
    (select o.value->>'text' from jsonb_array_elements(item->'options') o(value)
      where upper(o.value->>'id') = 'C' limit 1) as option_c
  from lesson_questions where position <= 35
)
insert into public.mentor_questions (
  id, code, subject_id, grade, chapter, topic, textbook, prompt,
  option_a, option_b, option_c, option_d, correct_option, explanation,
  difficulty, points, source, author, license, status
)
select 'g10-physics-lesson-bank-' || lpad(position::text, 3, '0'),
  'G10_PHYSICS_LESSON_BANK_' || lpad(position::text, 3, '0'),
  'physics', 10, 'Bài học Vật lí 10', 'Luyện kiến thức trong lộ trình',
  'Kết nối tri thức', item->>'prompt', option_a, option_b, option_c,
  case when position % 2 = 0 then 'Không có phương án nào đúng'
       else 'Cả ba phương án A, B, C đều đúng' end,
  upper(item->>'correctOptionId'), item->>'explanation',
  case upper(coalesce(item->>'difficulty', 'MEDIUM'))
    when 'EASY' then 'EASY' when 'HARD' then 'HARD' else 'MEDIUM' end,
  10, 'Câu hỏi bài học Vật lí 10 đã xuất bản', 'AI Mentor', 'Nội bộ', 'PUBLISHED'
from selected
where option_a is not null and option_b is not null and option_c is not null
  and option_a <> option_b and option_a <> option_c and option_b <> option_c
on conflict (id) do nothing;

do $$
declare published_count int;
begin
  select count(*) into published_count from public.mentor_questions
  where subject_id = 'physics' and grade = 10 and status = 'PUBLISHED';
  if published_count < 50 then
    raise exception 'Physics 10 requires 50 published questions; found %', published_count;
  end if;
end $$;

commit;
