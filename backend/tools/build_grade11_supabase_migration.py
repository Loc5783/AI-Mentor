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

                sql = f"""INSERT INTO curricula (id, subject_id, grade, version, title, data)
VALUES (
    {escape_sql_string(record_id)},
    {escape_sql_string(subject_id)},
    {grade},
    {escape_sql_string(version)},
    {escape_sql_string(title)},
    {escape_sql_string(json_str)}::jsonb
)
ON CONFLICT (id) DO UPDATE SET
    version = EXCLUDED.version,
    title = EXCLUDED.title,
    data = EXCLUDED.data,
    updated_at = NOW();\n\n"""
                out.write(sql)
                print(f"Generated SQL for {record_id}")
            except Exception as e:
                print(f"Error processing {file_path}: {e}")

    print(f"Migration script created at: {migration_file}")

if __name__ == "__main__":
    main()