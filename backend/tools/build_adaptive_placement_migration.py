"""Build the Supabase RPC migration for topic-level placement and skipped path units."""
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "supabase" / "migrations" / "202610290003_learning.sql"
TARGET = ROOT / "supabase" / "migrations" / "202610300002_adaptive_placement.sql"


def replace_once(source, old, new):
    if source.count(old) != 1:
        raise ValueError(f"Expected one migration anchor, found {source.count(old)}: {old[:80]}")
    return source.replace(old, new, 1)


def build():
    original = SOURCE.read_text(encoding="utf-8")
    start = original.index("create function public.mentor_learning_api(")
    end = original.index("\nrevoke all on function public.mentor_learning_api", start)
    function = original[start:end].strip()
    function = function.replace("create function public.mentor_learning_api(",
                                "create or replace function public.mentor_learning_api(", 1)

    function = replace_once(
        function,
        "daily bigint; item uuid; fingerprint text:=md5(method||path||body::text); cached public.mentor_requests;",
        "daily bigint; item uuid; fingerprint text:=md5(method||path||body::text); cached public.mentor_requests; "
        "skipped_units jsonb:='[]'; skipped_unit bool:=false; mastered_ids jsonb:='[]'; "
        "skipped_review_ids jsonb:='[]'; mastered_count int:=0; lesson_total int:=0;",
    )
    function = replace_once(
        function,
        "select jsonb_build_object('items',jsonb_agg(jsonb_build_object('id',s,'implementation',case when s in ('physics','chemistry','biology') then 'ACTIVE' else 'FRAMEWORK' end,'availableGrades',coalesce((select jsonb_agg(distinct grade) from public.mentor_course_units where subject_id=s),'[]')))) into result from unnest(array['math','literature','english','history','geography','physics','chemistry','biology','economics_law']) s;",
        "select jsonb_build_object('items',jsonb_agg(jsonb_build_object('id',s,'implementation',case when exists(select 1 from public.mentor_course_units where subject_id=s and kind='LESSON') then 'ACTIVE' else 'FRAMEWORK' end,'availableGrades',coalesce((select jsonb_agg(distinct grade) from public.mentor_course_units where subject_id=s),'[]')))) into result from unnest(array['math','literature','english','history','geography','physics','chemistry','biology','economics_law']) s;",
    )
    function = replace_once(
        function,
        "  ready:=e.placement is not null;\n"
        "  for u in select * from public.mentor_course_units where subject_id=e.subject_id and grade=e.grade and kind<>'PLACEMENT' order by position loop\n"
        "   done:=exists(select 1 from public.mentor_attempts where user_id=uid and enrollment_id=e.id and unit_id=u.unit_id and state='COMPLETED');\n"
        "   unit_state:=case when done then 'COMPLETED' when ready then 'AVAILABLE' else 'LOCKED' end;\n"
        "   units:=units||jsonb_build_array(jsonb_build_object('id',u.unit_id,'title',u.title,'kind',u.kind,'xp',case when u.kind='LESSON' then 200 else 400 end,'state',unit_state,'questionCount',jsonb_array_length(u.questions),'sourceLessonIds',u.sources));\n"
        "   ready:=ready and done;\n"
        "  end loop;",
        "  ready:=e.placement is not null;\n"
        "  skipped_units:=coalesce(e.placement->'skippedUnitIds','[]'::jsonb);\n"
        "  for u in select * from public.mentor_course_units where subject_id=e.subject_id and grade=e.grade and kind<>'PLACEMENT' order by position loop\n"
        "   done:=exists(select 1 from public.mentor_attempts where user_id=uid and enrollment_id=e.id and unit_id=u.unit_id and state='COMPLETED');\n"
        "   skipped_unit:=skipped_units ? u.unit_id;\n"
        "   unit_state:=case when done then 'COMPLETED' when skipped_unit then 'SKIPPED' when ready then 'AVAILABLE' else 'LOCKED' end;\n"
        "   units:=units||jsonb_build_array(jsonb_build_object('id',u.unit_id,'title',u.title,'kind',u.kind,'xp',case when u.kind='LESSON' then 200 else 400 end,'state',unit_state,'canOpen',ready,'questionCount',jsonb_array_length(u.questions),'sourceLessonIds',u.sources));\n"
        "   ready:=ready and (done or skipped_unit);\n"
        "  end loop;",
    )
    function = replace_once(
        function,
        "and not exists(select 1 from public.mentor_attempts t where t.user_id=uid and t.enrollment_id=e.id and t.unit_id=prior.unit_id and t.state='COMPLETED')) then raise sqlstate 'PT409' using message='PREVIOUS_LESSON_REQUIRED'; end if;",
        "and not exists(select 1 from public.mentor_attempts t where t.user_id=uid and t.enrollment_id=e.id and t.unit_id=prior.unit_id and t.state='COMPLETED') and not (skipped_units ? prior.unit_id)) then raise sqlstate 'PT409' using message='PREVIOUS_LESSON_REQUIRED'; end if;",
    )
    function = replace_once(
        function,
        "   if e.placement is null then raise sqlstate 'PT409' using message='PLACEMENT_REQUIRED'; end if;\n"
        "   if u.course_version<>body->>'curriculumVersion' then raise sqlstate 'PT409' using message='CURRICULUM_VERSION_CONFLICT'; end if;",
        "   if e.placement is null then raise sqlstate 'PT409' using message='PLACEMENT_REQUIRED'; end if;\n"
        "   skipped_units:=coalesce(e.placement->'skippedUnitIds','[]'::jsonb);\n"
        "   if u.course_version<>body->>'curriculumVersion' then raise sqlstate 'PT409' using message='CURRICULUM_VERSION_CONFLICT'; end if;",
    )
    function = replace_once(
        function,
        "  if a.kind='PLACEMENT' then\n"
        "   select count(*) filter(where (value->>'correct')::boolean) into score from jsonb_each(a.answers);\n"
        "   ans:=jsonb_build_object('correct',score,'total',jsonb_array_length(u.questions),'recommendation',case when score<jsonb_array_length(u.questions)*.6 then 'GUIDED' else 'STANDARD' end,'scope','INTRODUCTORY_BLOCK','completedAt',now());\n"
        "   update public.mentor_enrollments set placement=ans,revision=revision+1,updated_at=now() where id=e.id;",
        "  if a.kind='PLACEMENT' then\n"
        "   select count(*) filter(where coalesce((a.answers->(value->>'id')->>'correct')::boolean,false))::int into score\n"
        "     from jsonb_array_elements(u.questions);\n"
        "   with ordered_lessons as (\n"
        "     select l.unit_id,l.position,row_number() over(order by l.position,l.unit_id)::int as lesson_no\n"
        "     from public.mentor_course_units l\n"
        "     where l.subject_id=e.subject_id and l.grade=e.grade and l.kind='LESSON'\n"
        "   ), lesson_scores as (\n"
        "     select l.unit_id,l.position,l.lesson_no,count(*)::int as question_count,\n"
        "       count(*) filter(where coalesce((a.answers->(q.value->>'id')->>'correct')::boolean,false))::int as correct_count\n"
        "     from ordered_lessons l\n"
        "     join public.mentor_course_units u2 on u2.subject_id=e.subject_id and u2.grade=e.grade and u2.unit_id=l.unit_id\n"
        "     cross join lateral jsonb_array_elements(u2.questions) with ordinality q(value,ordinality)\n"
        "     where q.ordinality<=2 group by l.unit_id,l.position,l.lesson_no\n"
        "   ), mastered as (\n"
        "     select * from lesson_scores where question_count=2 and correct_count=2\n"
        "   ), full_blocks as (\n"
        "     select ((lesson_no-1)/5+1)::int as block_no from lesson_scores\n"
        "     group by ((lesson_no-1)/5+1)::int\n"
        "     having count(*)=5 and count(*) filter(where question_count=2 and correct_count=2)=5\n"
        "   )\n"
        "   select coalesce((select jsonb_agg(unit_id order by lesson_no) from mastered),'[]'::jsonb),\n"
        "     (select count(*)::int from mastered),(select count(*)::int from ordered_lessons),\n"
        "     coalesce((select jsonb_agg(e.subject_id||'-'||e.grade||'-review-'||block_no order by block_no) from full_blocks),'[]'::jsonb)\n"
        "     into mastered_ids,mastered_count,lesson_total,skipped_review_ids;\n"
        "   skipped_units:=mastered_ids||skipped_review_ids;\n"
        "   ans:=jsonb_build_object('correct',score,'total',jsonb_array_length(u.questions),\n"
        "     'scorePercent',round(score*100.0/greatest(jsonb_array_length(u.questions),1)),\n"
        "     'proficiencyBand',case when score::numeric/greatest(jsonb_array_length(u.questions),1)<.5 then 'FOUNDATION' when score::numeric/greatest(jsonb_array_length(u.questions),1)<.75 then 'DEVELOPING' when score::numeric/greatest(jsonb_array_length(u.questions),1)<.9 then 'PROFICIENT' else 'STRONG' end,\n"
        "     'recommendation','PERSONALIZED_PATH','scope','PUBLISHED_LESSONS','masteredLessonIds',mastered_ids,\n"
        "     'masteredLessonCount',mastered_count,'totalLessonCount',lesson_total,'skippedUnitIds',skipped_units,\n"
        "     'skippedLessonCount',mastered_count,'skippedReviewCount',jsonb_array_length(skipped_review_ids),'completedAt',now());\n"
        "   update public.mentor_enrollments set placement=ans,revision=revision+1,updated_at=now() where id=e.id;",
    )

    prelude = """begin;
-- Extend each entry assessment to two questions per published lesson, retaining the original
-- first-ten question order so an assessment already in progress can resume safely.
with ordered_lessons as (
  select subject_id,grade,course_version,unit_id,position,questions,
    row_number() over(partition by subject_id,grade order by position,unit_id) as lesson_no
  from public.mentor_course_units where kind='LESSON'
), placement_questions as (
  select subject_id,grade,course_version,
    jsonb_agg(q.value order by lesson_no,q.ordinality) as questions
  from ordered_lessons
  cross join lateral jsonb_array_elements(questions) with ordinality q(value,ordinality)
  where q.ordinality<=2
  group by subject_id,grade,course_version
)
update public.mentor_course_units p
set questions=pq.questions,
    source_note=case when p.source_note like '%Bài đầu vào dùng hai câu mỗi bài đã xuất bản%'
      then p.source_note
      else p.source_note||' Bài đầu vào dùng hai câu mỗi bài đã xuất bản để ước lượng nền tảng và đề xuất bài có thể bỏ qua; kết quả không đánh giá toàn bộ chương trình năm học.'
    end
from placement_questions pq
where p.subject_id=pq.subject_id and p.grade=pq.grade and p.course_version=pq.course_version
  and p.unit_id='placement' and p.kind='PLACEMENT';

-- Keep the curriculum scope shown in the path consistent for existing course rows too.
update public.mentor_course_units
set source_note=source_note||' Bài đầu vào dùng hai câu mỗi bài đã xuất bản để ước lượng nền tảng và đề xuất bài có thể bỏ qua; kết quả không đánh giá toàn bộ chương trình năm học.'
where source_note not like '%Bài đầu vào dùng hai câu mỗi bài đã xuất bản%';

"""
    ending = """
revoke all on function public.mentor_learning_api(text,text,jsonb,text,int,jsonb) from public,anon;
grant execute on function public.mentor_learning_api(text,text,jsonb,text,int,jsonb) to authenticated;
commit;
"""
    TARGET.write_text(prelude + function + ending, encoding="utf-8")


if __name__ == "__main__":
    build()
