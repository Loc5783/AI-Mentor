begin;

-- Derive review quizzes from the approved lesson question bank. This keeps the
-- practice library populated without maintaining a second copy of questions.
do $$
declare
  group_row record;
  question_set jsonb;
  question_total int;
  quiz_id text;
begin
  for group_row in
    with numbered as (
      select subject_id, grade, unit_id, position, title,
        row_number() over(partition by subject_id, grade order by position, unit_id) as lesson_no
      from public.mentor_course_units
      where kind='LESSON' and subject_id in ('physics','chemistry','biology')
    )
    select subject_id, grade,
      ((lesson_no - 1) / 5 + 1)::int as group_no,
      min(lesson_no)::int as first_lesson,
      max(lesson_no)::int as last_lesson,
      count(*)::int as lesson_count
    from numbered
    group by subject_id, grade, ((lesson_no - 1) / 5 + 1)::int
    having count(*)=5
  loop
    with numbered as (
      select subject_id, grade, unit_id, position, questions,
        row_number() over(partition by subject_id, grade order by position, unit_id) as lesson_no
      from public.mentor_course_units
      where kind='LESSON' and subject_id=group_row.subject_id and grade=group_row.grade
    ), selected as (
      select n.lesson_no, q.ordinality as question_no,
        jsonb_build_object(
          'id',q.value->>'id',
          'prompt',q.value->>'prompt',
          'options',(select coalesce(jsonb_agg(jsonb_build_object('id',upper(o.value->>'id'),'text',o.value->>'text') order by o.ordinality),'[]')
            from jsonb_array_elements(case when jsonb_typeof(q.value->'options')='array' then q.value->'options' else '[]'::jsonb end) with ordinality o(value,ordinality)),
          'correctOption',upper(coalesce(q.value->>'correctOption',q.value->>'correctOptionId')),
          'explanation',coalesce(q.value->>'explanation',''),
          'difficulty',case upper(coalesce(q.value->>'difficulty','MEDIUM'))
            when 'EASY' then 'EASY' when 'HARD' then 'HARD' else 'MEDIUM' end,
          'points',10
        ) as item
      from numbered n
      cross join lateral jsonb_array_elements(case when jsonb_typeof(n.questions)='array' then n.questions else '[]'::jsonb end) with ordinality q(value,ordinality)
      where ((n.lesson_no - 1) / 5 + 1)::int=group_row.group_no
        and jsonb_typeof(n.questions)='array'
        and q.value ? 'id' and q.value ? 'prompt'
        and coalesce(q.value->>'correctOption',q.value->>'correctOptionId') is not null
        and jsonb_typeof(q.value->'options')='array'
        and jsonb_array_length(q.value->'options')>=2
      order by n.lesson_no,q.ordinality
      limit 15
    )
    select coalesce(jsonb_agg(item order by lesson_no,question_no),'[]') into question_set from selected;

    question_total:=jsonb_array_length(question_set);
    if question_total between 10 and 15 then
      quiz_id:='review-'||group_row.subject_id||'-'||group_row.grade||'-'||group_row.group_no;
      insert into public.mentor_library_quizzes(
        id,title,subject_id,grade,chapter,topic,difficulty,questions,status
      ) values (
        quiz_id,
        'Ôn tập bài '||group_row.first_lesson||'–'||group_row.last_lesson,
        group_row.subject_id,
        group_row.grade,
        'Bài '||group_row.first_lesson||'–'||group_row.last_lesson,
        'Ôn tập 5 bài gần nhất',
        'MEDIUM',
        question_set,
        'PUBLISHED'
      ) on conflict(id) do nothing;
    end if;
  end loop;
end $$;

commit;
