"""Build Grade 12 curriculum seeding SQL migration for Supabase.

Reads Grade 12 packs from backend/curriculum/*.json and generates:
- supabase/migrations/202611010001_grade12_curriculum.sql:
  Seeds public.mentor_course_units (placement, lessons, 5-lesson reviews) for all 9 Grade 12 subjects.
  Seeds public.mentor_library_quizzes with corresponding 5-lesson review quizzes.
"""
import json
from pathlib import Path


def escape_sql(value: str) -> str:
    return value.replace("'", "''")


def build_migration_sql(curriculum_dir: Path) -> str:
    lines = [
        "begin;",
        "-- ============================================================================",
        "-- AI Mentor - Grade 12 Curriculum & Practice Library Seeding",
        "-- Versioned migration: 202611010001_grade12_curriculum.sql",
        "-- Idempotent seeding for all 9 subjects: Math, Physics, Chemistry, Biology,",
        "-- Literature, English, History, Geography, Economics & Law.",
        "-- ============================================================================",
        "",
    ]

    subjects = [
        "math",
        "physics",
        "chemistry",
        "biology",
        "literature",
        "english",
        "history",
        "geography",
        "economics_law",
    ]

    all_courses = []
    for subj in subjects:
        file_path = curriculum_dir / f"{subj}-12.json"
        if not file_path.exists():
            raise FileNotFoundError(f"Missing curriculum pack: {file_path}")
        course = json.loads(file_path.read_text(encoding="utf-8"))
        all_courses.append(course)

    # 1. Seed mentor_course_units
    for course in all_courses:
        subject = course["subjectId"]
        grade = course["grade"]
        version = course["version"]
        title = course["title"]
        source_note = course["sourceNote"]
        lessons = course["lessons"]

        lines.append(f"-- Subject: {subject} Grade: {grade} ({len(lessons)} lessons)")

        # Placement diagnostic unit (position 0)
        placement_questions = []
        for l in lessons:
            for q in l["questions"][:2]:
                placement_questions.append(q)

        placement_sql = (
            f"insert into public.mentor_course_units("
            f"subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values ("
            f"'{subject}',{grade},'{version}','placement',0,'PLACEMENT',"
            f"'Khảo sát đầu vào','{escape_sql(title)}','{escape_sql(source_note)}',"
            f"'[]'::jsonb,'[]'::jsonb,'{escape_sql(json.dumps(placement_questions, ensure_ascii=False))}'::jsonb,'[]'::jsonb"
            f") on conflict(subject_id,grade,unit_id) do update set "
            f"course_version=excluded.course_version,title=excluded.title,course_title=excluded.course_title,"
            f"source_note=excluded.source_note,questions=excluded.questions;"
        )
        lines.append(placement_sql)

        # Lessons and 5-lesson reviews
        pos = 1
        for idx, lesson in enumerate(lessons):
            lid = lesson["id"]
            ltitle = lesson["title"]
            guidance = json.dumps(lesson.get("guidance", []), ensure_ascii=False)
            examples = json.dumps(lesson.get("examples", []), ensure_ascii=False)
            questions = json.dumps(lesson.get("questions", []), ensure_ascii=False)
            sources = json.dumps([lid], ensure_ascii=False)

            lesson_sql = (
                f"insert into public.mentor_course_units("
                f"subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values ("
                f"'{subject}',{grade},'{version}','{lid}',{pos},'LESSON',"
                f"'{escape_sql(ltitle)}','{escape_sql(title)}','{escape_sql(source_note)}',"
                f"'{escape_sql(guidance)}'::jsonb,'{escape_sql(examples)}'::jsonb,'{escape_sql(questions)}'::jsonb,'{escape_sql(sources)}'::jsonb"
                f") on conflict(subject_id,grade,unit_id) do update set "
                f"course_version=excluded.course_version,position=excluded.position,title=excluded.title,"
                f"course_title=excluded.course_title,source_note=excluded.source_note,guidance=excluded.guidance,"
                f"examples=excluded.examples,questions=excluded.questions,sources=excluded.sources;"
            )
            lines.append(lesson_sql)
            pos += 1

            # After every 5 lessons, insert FIVE_LESSON_REVIEW
            if (idx + 1) % 5 == 0:
                block_no = (idx + 1) // 5
                rid = f"{subject}-{grade}-review-{block_no}"
                block = lessons[idx - 4:idx + 1]
                rev_questions = [q for bl in block for q in bl["questions"][:2]]
                rev_sources = [bl["id"] for bl in block]

                rev_sql = (
                    f"insert into public.mentor_course_units("
                    f"subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values ("
                    f"'{subject}',{grade},'{version}','{rid}',{pos},'FIVE_LESSON_REVIEW',"
                    f"'Ôn tập {block_no}','{escape_sql(title)}','{escape_sql(source_note)}',"
                    f"'[]'::jsonb,'[]'::jsonb,'{escape_sql(json.dumps(rev_questions, ensure_ascii=False))}'::jsonb,'{escape_sql(json.dumps(rev_sources, ensure_ascii=False))}'::jsonb"
                    f") on conflict(subject_id,grade,unit_id) do update set "
                    f"course_version=excluded.course_version,position=excluded.position,title=excluded.title,"
                    f"course_title=excluded.course_title,source_note=excluded.source_note,questions=excluded.questions,sources=excluded.sources;"
                )
                lines.append(rev_sql)
                pos += 1
        lines.append("")

    # 2. Seed mentor_library_quizzes for Grade 12 review quizzes
    lines.append("-- ----------------------------------------------------------------------------")
    lines.append("-- Seed Grade 12 practice library review quizzes")
    lines.append("-- ----------------------------------------------------------------------------")
    for course in all_courses:
        subject = course["subjectId"]
        grade = course["grade"]
        lessons = course["lessons"]

        for b_idx in range(len(lessons) // 5):
            block_no = b_idx + 1
            first_l = b_idx * 5 + 1
            last_l = (b_idx + 1) * 5
            block = lessons[first_l - 1:last_l]

            quiz_id = f"review-{subject}-{grade}-{block_no}"
            quiz_title = f"Ôn tập bài {first_l}–{last_l}"
            chapter_name = f"Bài {first_l}–{last_l}"
            topic_name = "Ôn tập 5 bài gần nhất"

            quiz_questions = []
            for bl in block:
                for q in bl["questions"][:2]:
                    formatted_q = {
                        "id": q["id"],
                        "prompt": q["prompt"],
                        "options": [{"id": o["id"].upper(), "text": o["text"]} for o in q["options"]],
                        "correctOption": q.get("correctOptionId", "a").upper(),
                        "explanation": q.get("explanation", ""),
                        "difficulty": q.get("difficulty", "MEDIUM"),
                        "points": 10,
                    }
                    quiz_questions.append(formatted_q)

            quiz_sql = (
                f"insert into public.mentor_library_quizzes("
                f"id,title,subject_id,grade,chapter,topic,difficulty,questions,status) values ("
                f"'{quiz_id}','{escape_sql(quiz_title)}','{subject}',{grade},"
                f"'{escape_sql(chapter_name)}','{escape_sql(topic_name)}','MEDIUM',"
                f"'{escape_sql(json.dumps(quiz_questions, ensure_ascii=False))}'::jsonb,'PUBLISHED'"
                f") on conflict(id) do update set "
                f"title=excluded.title,subject_id=excluded.subject_id,grade=excluded.grade,"
                f"chapter=excluded.chapter,topic=excluded.topic,difficulty=excluded.difficulty,"
                f"questions=excluded.questions,status=excluded.status;"
            )
            lines.append(quiz_sql)

    lines.append("")
    lines.append("commit;")
    lines.append("")
    return "\n".join(lines)


def main():
    repo_root = Path(__file__).resolve().parents[2]
    curriculum_dir = repo_root / "backend" / "curriculum"
    output_mig = repo_root / "supabase" / "migrations" / "202611010001_grade12_curriculum.sql"
    sql = build_migration_sql(curriculum_dir)
    output_mig.write_text(sql, encoding="utf-8")
    print(f"Generated {output_mig} ({len(sql.splitlines())} lines)")


if __name__ == "__main__":
    main()
