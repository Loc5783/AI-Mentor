import json
import os
import glob

def escape_sql_string(s):
    if s is None:
        return 'NULL'
    return "'" + s.replace("'", "''") + "'"

def main():
    backend_dir = os.path.join(os.path.dirname(__file__), "..")
    curriculum_dir = os.path.join(backend_dir, "curriculum")
    migrations_dir = os.path.join(backend_dir, "..", "supabase", "migrations")
    os.makedirs(migrations_dir, exist_ok=True)

    migration_file = os.path.join(migrations_dir, "202610310001_grade11_curricula.sql")

    json_files = glob.glob(os.path.join(curriculum_dir, "*-11.json"))

    if not json_files:
        print("No grade 11 JSON files found in backend/curriculum/")
        return

    with open(migration_file, "w", encoding="utf-8") as out:
        out.write("-- Migration: Grade 11 Curriculum Seeding\n")
        out.write("-- Generated automatically by AI Mentor tools\n\n")

        for file_path in json_files:
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    data = json.load(f)

                subject_id = data['subjectId']
                grade = data['grade']
                version = data['version']
                title = data['title']

                # The primary key usually combines subject, grade and version, or we just insert raw.
                # Assuming the table structure:
                # id (text, e.g. "math-11"), subject_id, grade, version, title, data (jsonb)
                record_id = f"{subject_id}-{grade}"

                json_str = json.dumps(data, ensure_ascii=False)

                sql = ""
                for index, lesson in enumerate(data['lessons']):
                    position = index + 1
                    kind = "LESSON"
                    if index % 5 == 4:
                        kind = "FIVE_LESSON_REVIEW"

                    unit_id = lesson['id']
                    lesson_title = lesson['title']
                    guidance = json.dumps(lesson.get('guidance', []), ensure_ascii=False)
                    examples = json.dumps(lesson.get('examples', []), ensure_ascii=False)
                    questions = json.dumps(lesson.get('questions', []), ensure_ascii=False)

                    sql += f"""insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    {escape_sql_string(subject_id)},
    {grade},
    {escape_sql_string(version)},
    {escape_sql_string(unit_id)},
    {position},
    {escape_sql_string(kind)},
    {escape_sql_string(lesson_title)},
    {escape_sql_string(title)},
    {escape_sql_string(data.get('sourceNote', ''))},
    {escape_sql_string(guidance)}::jsonb,
    {escape_sql_string(examples)}::jsonb,
    {escape_sql_string(questions)}::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;\n"""
                out.write(sql)
                print(f"Generated SQL for {record_id}")
            except Exception as e:
                print(f"Error processing {file_path}: {e}")

    print(f"Migration script created at: {migration_file}")

if __name__ == "__main__":
    main()