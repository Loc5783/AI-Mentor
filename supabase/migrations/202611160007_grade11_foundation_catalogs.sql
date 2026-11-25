begin;

-- Grade 11 already has five reviewed, published foundation questions per subject.
-- Expose them in practice and exam catalogs without publishing lesson drafts.
with selected as (
  select subject_id,
    jsonb_agg(jsonb_build_object(
      'id', id, 'prompt', prompt,
      'options', jsonb_build_array(
        jsonb_build_object('id','A','text',option_a),
        jsonb_build_object('id','B','text',option_b),
        jsonb_build_object('id','C','text',option_c),
        jsonb_build_object('id','D','text',option_d)),
      'correctOption', correct_option,
      'explanation', explanation,
      'difficulty', difficulty, 'points', points) order by id) questions
  from public.mentor_questions
  where grade=11 and status='PUBLISHED' and id like 'g11-found-%'
    and correct_option in ('A','B','C','D')
    and nullif(trim(explanation),'') is not null
  group by subject_id
  having count(*)=5
)
insert into public.mentor_library_quizzes(id,title,subject_id,grade,chapter,topic,difficulty,questions,status)
select 'grade11-foundation-'||subject_id,'Ôn tập nền tảng lớp 11',subject_id,11,
  'Kiến thức nền tảng','Bài luyện ngắn theo môn','EASY',questions,'PUBLISHED'
from selected
on conflict(id) do nothing;

with selected as (
  select subject_id, jsonb_agg(id order by id) as question_ids,
    count(*)::int as question_count, sum(points)::int as total_points
  from public.mentor_questions
  where grade=11 and status='PUBLISHED' and id like 'g11-found-%'
    and correct_option in ('A','B','C','D')
    and nullif(trim(explanation),'') is not null
  group by subject_id
  having count(*)=5
)
insert into public.mentor_exams(id,title,subject_id,grade,exam_type,semester,duration_minutes,
  question_ids,total_points,question_count,status)
select 'grade11-foundation-'||subject_id,'Đề ôn tập nền tảng lớp 11',subject_id,11,
  'MIDTERM',1,10,question_ids,total_points,question_count,'PUBLISHED'
from selected
on conflict(id) do nothing;

commit;
