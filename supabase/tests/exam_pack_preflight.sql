-- Read-only inventory. May be run against cloud to report counts; never writes fixtures.
-- This is deliberately not a *_smoke.sql test: a cloud inventory is not test evidence.
begin read only;

with subjects(subject_id) as (
  values ('biology'), ('economics_law'), ('english'), ('geography'), ('history'), ('literature')
)
select s.subject_id, count(q.id) as published_count,
  count(q.id) filter (where
    nullif(btrim(q.prompt), '') is null or nullif(btrim(q.explanation), '') is null
    or nullif(btrim(q.topic), '') is null
    or q.correct_option is null or q.correct_option not in ('A','B','C','D')
    or exists (select 1 from unnest(array[q.option_a,q.option_b,q.option_c,q.option_d]) v
               where nullif(btrim(v), '') is null)
    or (select count(distinct lower(btrim(v))) from
        unnest(array[q.option_a,q.option_b,q.option_c,q.option_d]) v) <> 4
  ) as invalid_count
from subjects s left join public.mentor_questions q
  on q.subject_id=s.subject_id and q.grade=11 and q.status='PUBLISHED'
group by s.subject_id order by s.subject_id;

-- Exact normalized duplicates to review; numbers and case are retained.
select subject_id, grade, btrim(prompt) as prompt, count(*) as duplicates
from public.mentor_questions
where grade=11 and status='PUBLISHED'
  and subject_id in ('biology','economics_law','english','geography','history','literature')
group by subject_id,grade,btrim(prompt),btrim(option_a),btrim(option_b),btrim(option_c),btrim(option_d)
having count(*)>1;

-- Establish the deployed ownership shape before designing a non-destructive migration.
select table_name,column_name,data_type
from information_schema.columns
where table_schema='public' and table_name in ('mentor_exam_packs','mentor_exam_pack_questions')
order by table_name,ordinal_position;
rollback;
