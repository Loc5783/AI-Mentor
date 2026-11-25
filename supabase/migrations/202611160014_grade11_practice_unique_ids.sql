begin;

-- Some Literature lessons share a question ID. Select one source row per ID
-- before ranking so each published set still contains fifteen unique items.
with variants as (
  select generate_series(2,10) as variant
), unique_questions as (
  select distinct on (q.value->>'id') q.value as question
  from public.mentor_course_units c
  cross join lateral jsonb_array_elements(c.questions) q(value)
  where c.grade=11 and c.subject_id='literature' and c.kind='LESSON'
    and jsonb_typeof(c.questions)='array'
    and nullif(trim(q.value->>'id'),'') is not null
    and nullif(trim(q.value->>'prompt'),'') is not null
    and nullif(trim(q.value->>'explanation'),'') is not null
    and jsonb_typeof(q.value->'options')='array'
    and jsonb_array_length(q.value->'options')=3
    and upper(coalesce(q.value->>'correctOptionId',q.value->>'correctOption')) in ('A','B','C')
  order by q.value->>'id',c.position,c.unit_id
), ranked as (
  select v.variant,row_number() over(partition by v.variant
    order by md5(v.variant::text||':'||(q.question->>'id'))) as question_no,
    jsonb_build_object(
      'id',q.question->>'id','prompt',q.question->>'prompt',
      'options',(select jsonb_agg(jsonb_build_object('id',upper(o.value->>'id'),
        'text',o.value->>'text') order by o.ordinality)
        from jsonb_array_elements(q.question->'options') with ordinality o(value,ordinality)),
      'correctOption',upper(coalesce(q.question->>'correctOptionId',q.question->>'correctOption')),
      'explanation',q.question->>'explanation',
      'difficulty',case upper(coalesce(q.question->>'difficulty','MEDIUM'))
        when 'EASY' then 'EASY' when 'HARD' then 'HARD' else 'MEDIUM' end,
      'points',10
    ) as item
  from variants v cross join unique_questions q
), sets as (
  select variant,jsonb_agg(item order by question_no) as questions
  from ranked where question_no<=15 group by variant
)
insert into public.mentor_library_quizzes(
  id,title,subject_id,grade,chapter,topic,difficulty,questions,status
)
select 'grade11-mixed-literature-'||lpad(variant::text,2,'0'),
  'Đề luyện tổng hợp '||lpad(variant::text,2,'0'),
  'literature',11,'Các bài lớp 11 hiện có','Luyện tổng hợp lộ trình hiện có',
  'MEDIUM',questions,'PUBLISHED'
from sets where jsonb_array_length(questions)=15
on conflict(id) do nothing;

commit;
