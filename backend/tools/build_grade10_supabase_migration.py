"""Create an idempotent Supabase seed migration from the published Grade 10 packs."""
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PACKS = ROOT / "backend" / "curriculum"
MIGRATION = ROOT / "supabase" / "migrations" / "202610300001_remaining_grade10_curricula.sql"
SUBJECTS = ("math", "literature", "english", "history", "geography", "economics_law")


def sql_text(value):
    return "'" + value.replace("'", "''") + "'"


def sql_json(value):
    return sql_text(json.dumps(value, ensure_ascii=False, separators=(",", ":"))) + "::jsonb"


def build():
    statements = [
        "begin;",
        "-- Seed the six newly authored Grade 10 introductory courses. Content is partial, not a full-year claim.",
    ]
    for subject in SUBJECTS:
        course = json.loads((PACKS / f"{subject}-10.json").read_text(encoding="utf-8"))
        def insert_unit(unit_id, position, kind, title, guidance, examples, questions, sources):
            columns = (
                "subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,"
                "guidance,examples,questions,sources"
            )
            values = [
                sql_text(subject), "10", sql_text(course["version"]), sql_text(unit_id), str(position),
                sql_text(kind), sql_text(title), sql_text(course["title"]), sql_text(course["sourceNote"]),
                sql_json(guidance), sql_json(examples), sql_json(questions), sql_json(sources),
            ]
            statements.append(
                f"insert into public.mentor_course_units({columns})\nvalues ({', '.join(values)})\n"
                "on conflict(subject_id,grade,unit_id) do nothing;"
            )

        lessons = course["lessons"]
        placement_questions = [question for lesson in lessons[:5] for question in lesson["questions"][:2]]
        insert_unit("placement", 0, "PLACEMENT", "Khảo sát đầu vào", [], [], placement_questions, [])
        for index, lesson in enumerate(lessons):
            position = index + 1 + index // 5
            insert_unit(lesson["id"], position, "LESSON", lesson["title"], lesson["guidance"],
                        lesson["examples"], lesson["questions"], [lesson["id"]])
            if (index + 1) % 5 == 0:
                block = lessons[index - 4:index + 1]
                block_no = (index + 1) // 5
                review_id = f"{subject}-10-review-{block_no}"
                review_questions = [question for source in block for question in source["questions"][:2]]
                review_position = position + 1
                insert_unit(review_id, review_position, "FIVE_LESSON_REVIEW", f"Ôn tập {block_no}", [], [],
                            review_questions, [source["id"] for source in block])

    statements.extend([
        "",
        "-- Create the same 10-question reviews used by the app after each five-lesson block.",
        "do $$",
        "declare group_row record; question_set jsonb; quiz_id text;",
        "begin",
        "  for group_row in",
        "    with numbered as (",
        "      select subject_id, grade, unit_id, position, title,",
        "        row_number() over(partition by subject_id, grade order by position, unit_id) as lesson_no",
        "      from public.mentor_course_units",
        "      where kind='LESSON' and grade=10",
        "        and subject_id in ('math','literature','english','history','geography','economics_law')",
        "    )",
        "    select subject_id, grade, ((lesson_no - 1) / 5 + 1)::int as group_no,",
        "      min(lesson_no)::int as first_lesson, max(lesson_no)::int as last_lesson, count(*)::int as lesson_count",
        "    from numbered group by subject_id, grade, ((lesson_no - 1) / 5 + 1)::int having count(*)=5",
        "  loop",
        "    with numbered as (",
        "      select subject_id, grade, unit_id, position, questions,",
        "        row_number() over(partition by subject_id, grade order by position, unit_id) as lesson_no",
        "      from public.mentor_course_units",
        "      where kind='LESSON' and subject_id=group_row.subject_id and grade=group_row.grade",
        "    ), selected as (",
        "      select n.lesson_no, q.ordinality as question_no,",
        "        jsonb_build_object(",
        "          'id',q.value->>'id','prompt',q.value->>'prompt',",
        "          'options',(select coalesce(jsonb_agg(jsonb_build_object('id',upper(o.value->>'id'),'text',o.value->>'text') order by o.ordinality),'[]')",
        "            from jsonb_array_elements(case when jsonb_typeof(q.value->'options')='array' then q.value->'options' else '[]'::jsonb end) with ordinality o(value,ordinality)),",
        "          'correctOption',upper(coalesce(q.value->>'correctOption',q.value->>'correctOptionId')),",
        "          'explanation',coalesce(q.value->>'explanation',''),",
        "          'difficulty',case upper(coalesce(q.value->>'difficulty','MEDIUM')) when 'EASY' then 'EASY' else 'MEDIUM' end,",
        "          'points',10",
        "        ) as item",
        "      from numbered n",
        "      cross join lateral jsonb_array_elements(case when jsonb_typeof(n.questions)='array' then n.questions else '[]'::jsonb end) with ordinality q(value,ordinality)",
        "      where ((n.lesson_no - 1) / 5 + 1)::int=group_row.group_no and q.ordinality<=2",
        "        and jsonb_typeof(n.questions)='array' and q.value ? 'id' and q.value ? 'prompt'",
        "        and coalesce(q.value->>'correctOption',q.value->>'correctOptionId') is not null",
        "        and jsonb_typeof(q.value->'options')='array' and jsonb_array_length(q.value->'options')>=2",
        "      order by n.lesson_no,q.ordinality limit 15",
        "    )",
        "    select coalesce(jsonb_agg(item order by lesson_no,question_no),'[]') into question_set from selected;",
        "    if jsonb_array_length(question_set)=10 then",
        "      quiz_id:='review-'||group_row.subject_id||'-'||group_row.grade||'-'||group_row.group_no;",
        "      insert into public.mentor_library_quizzes(id,title,subject_id,grade,chapter,topic,difficulty,questions,status)",
        "      values (quiz_id,'Ôn tập bài '||group_row.first_lesson||'–'||group_row.last_lesson,group_row.subject_id,group_row.grade,",
        "        'Bài '||group_row.first_lesson||'–'||group_row.last_lesson,'Ôn tập 5 bài gần nhất','MEDIUM',question_set,'PUBLISHED')",
        "      on conflict(id) do nothing;",
        "    end if;",
        "  end loop;",
        "end $$;",
        "commit;",
        "",
    ])
    MIGRATION.write_text("\n".join(statements), encoding="utf-8")


if __name__ == "__main__":
    build()
