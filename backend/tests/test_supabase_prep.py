import hashlib
import io
import json
from pathlib import Path
import re
import sqlite3
import pytest
from fastapi.testclient import TestClient

from aimentor.config import Settings
from aimentor.database import Database
from aimentor.main import create_app
from test_api import signup, headers, key

EXPECTED_TABLES = {
    "users",
    "sessions",
    "resources",
    "enrollments",
    "attempts",
    "completions",
    "awards",
    "learning_days",
    "friendships",
    "events",
    "idempotency",
    "recovery_tokens",
    "rate_limits",
    "badges",
    "user_badges",
    "friend_streaks",
    "questions",
    "library_quizzes",
    "practice_sessions",
    "wrong_questions_notebook",
    "role_audit_logs",
    "exams",
    "exam_sessions",
    "ai_practice_sessions",
    "ai_question_reports",
    "ai_provider_operations",
}

EXPECTED_CUSTOM_INDEXES = {
    "resource_owner_kind",
    "idx_wrong_questions_user_subj",
    "idx_practice_sessions_user_quiz",
    "idx_exams_filter",
    "idx_exam_sessions_user",
    "idx_ai_provider_usage",
    "idx_ai_practice_user_state",
    "idx_ai_reports_user",
}


def test_sqlite_schema_matches_documented_inventory(tmp_path):
    """Verifies that SQLite database initialization creates all 26 documented tables and 8 indexes."""
    db_obj = Database(tmp_path)
    db_obj.initialize()

    with db_obj.read() as db:
        table_rows = db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").fetchall()
        actual_tables = {r[0] for r in table_rows}

        assert EXPECTED_TABLES.issubset(actual_tables), f"Missing tables: {EXPECTED_TABLES - actual_tables}"
        assert len(actual_tables) == 26, f"Expected exactly 26 tables, found {len(actual_tables)}: {actual_tables}"

        index_rows = db.execute("SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'sqlite_autoindex_%'").fetchall()
        actual_indexes = {r[0] for r in index_rows}
        assert EXPECTED_CUSTOM_INDEXES.issubset(actual_indexes), f"Missing indexes: {EXPECTED_CUSTOM_INDEXES - actual_indexes}"


def test_uploads_filesystem_isolation(tmp_path):
    """Verifies that binary uploads reside on the filesystem under data_dir/uploads, separate from DB."""
    settings = Settings(tmp_path)
    app = create_app(settings)

    with TestClient(app) as client:
        session = signup(client, "FileTester")

        # Upload a test text file
        file_content = b"This is offline test attachment payload content."
        resp = client.post(
            "/attachments",
            headers=headers(session, **{"Idempotency-Key": key()}),
            files={"file": ("test_doc.txt", io.BytesIO(file_content), "text/plain")},
            data={"purpose": "CHAT"},
        )
        assert resp.status_code == 201, resp.text
        attachment_id = resp.json()["id"]

        # Check DB metadata in resources table
        with app.state.db.read() as db:
            row = db.execute("SELECT * FROM resources WHERE id=?", (attachment_id,)).fetchone()
            assert row is not None
            assert row["kind"] == "ATTACHMENT"
            body = json.loads(row["body"])
            assert body["name"] == "test_doc.txt"
            assert body["mimeType"] == "text/plain"
            assert body["byteSize"] == len(file_content)
            # Binary payload MUST NOT be stored directly in SQLite table
            assert "This is offline test attachment" not in row["body"]

        # Check filesystem storage
        expected_disk_path = tmp_path / "uploads" / session["userId"] / (attachment_id + ".bin")
        assert expected_disk_path.is_file(), f"Binary file not found at {expected_disk_path}"
        assert expected_disk_path.read_bytes() == file_content


def test_runtime_remains_unswitched_sqlite(tmp_path):
    """Verifies that runtime persistence remains strictly SQLite with no bogus connection switches."""
    settings = Settings(tmp_path)
    app = create_app(settings)

    assert isinstance(app.state.db, Database)
    assert app.state.db.path == tmp_path / "aimentor.sqlite3"
    assert app.state.db.path.is_file()


try:
    from tools.build_grade12_sql_migration import build_migration_sql
    from tools.supabase_transfer import generate_dry_run_report, audit_sqlite_database
    from tools.export_staging_archive import export_sqlite_to_staging
except ImportError:
    from backend.tools.build_grade12_sql_migration import build_migration_sql
    from backend.tools.supabase_transfer import generate_dry_run_report, audit_sqlite_database
    from backend.tools.export_staging_archive import export_sqlite_to_staging


def test_grade12_supabase_migration_presence_and_invariants():
    """Validates that Grade 12 curriculum is seeded idempotently into Supabase migrations."""
    repo_root = Path(__file__).resolve().parents[2]
    curriculum_dir = repo_root / "backend" / "curriculum"
    output_mig = repo_root / "supabase" / "migrations" / "202611010001_grade12_curriculum.sql"

    sql = build_migration_sql(curriculum_dir)
    assert output_mig.is_file(), f"Migration file missing: {output_mig}"
    content = output_mig.read_text(encoding="utf-8")
    assert content == sql, "Generated migration on disk differs from build_migration_sql()"

    assert content.startswith("begin;")
    assert content.strip().endswith("commit;")

    # Verify all 9 Grade 12 subjects are seeded
    expected_subjects = [
        "math", "physics", "chemistry", "biology", "literature",
        "english", "history", "geography", "economics_law"
    ]
    for subj in expected_subjects:
        assert f"'{subj}',12,'intro-2026.1','placement'" in content, f"Missing placement for {subj} 12"
        assert f"review-{subj}-12-" in content, f"Missing review quizzes for {subj} 12"

    # Verify idempotent seed rules
    assert "on conflict(subject_id,grade,unit_id) do update" in content
    assert "on conflict(id) do update" in content

    # Verify no server secrets or private keys leaked in SQL
    assert "service_role" not in content
    assert "sb_secret" not in content
    assert "password_hash" not in content

    # Rigorous literal boundaries and JSON syntax validation for all ::jsonb literals
    # Catches Postgres syntax error 42601 (e.g. malformed outer quoting like ''[...]''::jsonb)
    assert "''[" not in content, "Found malformed double-single-quote literal start ''[ in generated SQL"
    assert "]''::jsonb" not in content, "Found malformed double-single-quote literal end ]''::jsonb in generated SQL"
    assert "''''::jsonb" not in content, "Found malformed empty string cast ''''::jsonb in generated SQL"
    assert "::jsonb,''" not in content, "Found malformed literal start ::jsonb,'' in generated SQL"

    # Extract all JSON literals cast to ::jsonb and verify valid JSON structure
    # Matches: '...'::jsonb (with SQL-escaped quotes inside)
    jsonb_pattern = re.compile(r"'((?:''|[^'])*)'::jsonb")
    matches = jsonb_pattern.findall(content)
    assert len(matches) > 0, "No ::jsonb literals found in migration"

    # Count total occurrences of ::jsonb to ensure 100% of casts were parsed by the regex
    total_jsonb_casts = content.count("::jsonb")
    assert len(matches) == total_jsonb_casts, (
        f"Regex matched {len(matches)} valid string literals, but found {total_jsonb_casts} '::jsonb' occurrences. "
        "Malformed SQL string literal boundaries detected!"
    )

    for idx, raw_json_str in enumerate(matches):
        unescaped_json = raw_json_str.replace("''", "'")
        try:
            parsed = json.loads(unescaped_json)
            assert isinstance(parsed, (list, dict)), f"JSON at index {idx} parsed to unexpected type: {type(parsed)}"
        except json.JSONDecodeError as exc:
            pytest.fail(f"Invalid JSON in migration SQL literal #{idx}: {exc}")


def test_durable_app_structures_migration_rls_and_invariants():
    """Validates that missing durable app structures (exams, sessions, audit) have strict RLS."""
    repo_root = Path(__file__).resolve().parents[2]
    exams_mig = repo_root / "supabase" / "migrations" / "202611010002_exams_and_durable_schema.sql"
    assert exams_mig.is_file(), f"Migration file missing: {exams_mig}"

    content = exams_mig.read_text(encoding="utf-8")
    assert "create table if not exists public.mentor_exams" in content
    assert "create table if not exists public.mentor_exam_sessions" in content
    assert "create table if not exists public.mentor_role_audit_logs" in content

    # RLS checks
    assert "alter table public.mentor_exams enable row level security;" in content
    assert "alter table public.mentor_exam_sessions enable row level security;" in content
    assert "alter table public.mentor_role_audit_logs enable row level security;" in content

    # Revoke default permissions
    assert "revoke all on public.mentor_exams, public.mentor_exam_sessions, public.mentor_role_audit_logs from public, anon, authenticated;" in content

    # Minimal table grants consistent with RLS
    assert "grant select on public.mentor_exams to authenticated;" in content
    assert "grant select, insert, update on public.mentor_exam_sessions to authenticated;" in content
    # Role audit logs must remain private to superuser/service_role
    assert "grant select on public.mentor_role_audit_logs" not in content
    assert "grant all on public.mentor_role_audit_logs" not in content

    # Privileged functions must not have default PUBLIC execute
    assert "alter default privileges in schema public revoke execute on functions from public;" in content

    # Ownership isolation policies
    assert "create policy mentor_exams_read on public.mentor_exams" in content
    assert "status = 'PUBLISHED'" in content
    assert "create policy mentor_exam_sessions_owner on public.mentor_exam_sessions" in content
    assert "(select auth.uid()) = user_id" in content

    # No server secrets
    assert "service_role" not in content
    assert "sb_secret" not in content


def test_private_staging_schema_migration_rls_and_invariants():
    """Validates that the private staging schema is idempotent, private, and exposes zero Data API."""
    repo_root = Path(__file__).resolve().parents[2]
    staging_mig = repo_root / "supabase" / "migrations" / "202611010003_private_staging_schema.sql"
    assert staging_mig.is_file(), f"Migration file missing: {staging_mig}"

    content = staging_mig.read_text(encoding="utf-8")
    assert content.startswith("begin;")
    assert content.strip().endswith("commit;")

    assert "create schema if not exists staging;" in content
    assert "create table if not exists staging.user_mappings" in content
    assert "create table if not exists staging.durable_rows" in content

    # Check strict permission revocation
    assert "revoke all on schema staging from public, anon, authenticated;" in content
    assert "revoke all on all tables in schema staging from public, anon, authenticated;" in content
    assert "alter default privileges in schema staging revoke all on tables from public, anon, authenticated;" in content

    # Strip comments to inspect executable SQL statements
    # Allows comments to describe excluded credentials without false-positive failures
    executable_lines = [
        line.split("--")[0].strip()
        for line in content.splitlines()
        if line.strip() and not line.strip().startswith("--")
    ]
    executable_sql = "\n".join(executable_lines).lower()

    # Zero anon or authenticated grants in executable SQL
    assert "grant select on" not in executable_sql
    assert "grant usage on schema staging to anon" not in executable_sql
    assert "grant usage on schema staging to authenticated" not in executable_sql

    # Secrets and passwords must never be staged in executable SQL statements
    assert "password_hash" not in executable_sql
    assert "session_token" not in executable_sql
    assert "recovery_token" not in executable_sql
    assert "service_role" not in executable_sql
    assert "sb_secret" not in executable_sql


def test_safe_sqlite_to_supabase_transfer_dry_run(tmp_path):
    """Verifies that the migration transfer tool operates read-only and safely on an isolated fixture."""
    # 1. Initialize temporary isolated SQLite database with all 26 tables
    db_obj = Database(tmp_path)
    db_obj.initialize()
    db_path = tmp_path / "aimentor.sqlite3"
    assert db_path.is_file()

    # 2. Insert test fixture records across durable and ephemeral tables
    with db_obj.transaction() as db:
        # Create 3 test users (simulating 1 admin, 1 student, 1 unverified)
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (1, "admin_user", "admin@domain.example", "scrypt$dummyhash1", "Admin", "ADMIN", "DIAMOND", 12, 1, 1, 1000.0, 1000.0)
        )
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (2, "student_user", "student@domain.example", "scrypt$dummyhash2", "Student", "STUDENT", "BRONZE", 10, 1, 1, 1001.0, 1001.0)
        )
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (3, "test_account", "guest@school.example", "scrypt$dummyhash3", "Guest", "STUDENT", "BRONZE", 10, 0, 1, 1002.0, 1002.0)
        )
        # Add sample durable rows
        db.execute(
            "INSERT INTO enrollments (id, user_id, subject_id, grade, curriculum_version, placement, revision, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            ("enr-1", 2, "math", 10, "2026.1", "{}", 1, 1003.0)
        )
        db.execute(
            "INSERT INTO attempts (id, user_id, enrollment_id, unit_id, kind, curriculum_version, step, answers, state, revision, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ("att-1", 2, "enr-1", "math-10-01", "LESSON", "2026.1", 0, "[]", "IN_PROGRESS", 1, 1004.0)
        )
        # Add sample ephemeral rows (which must be excluded)
        db.execute(
            "INSERT INTO sessions (id, user_id, access_hash, refresh_hash, access_expires, refresh_expires, revoked) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            ("sess-1", 2, "hash_acc", "hash_ref", 9999.0, 99999.0, 0)
        )
        db.execute(
            "INSERT INTO events (owner, resource_type, resource_id, revision, operation, created_at) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (2, "ATTEMPT", "att-1", 1, "CREATE", 1005.0)
        )

    # 3. Audit SQLite database read-only
    counts, owners = audit_sqlite_database(db_path)
    assert len(counts) == 26, f"Expected 26 tables, found {len(counts)}: {counts}"
    assert owners == {1, 2, 3}, f"Expected distinct owners {1, 2, 3}, got {owners}"
    assert counts["users"] == 3
    assert counts["enrollments"] == 1
    assert counts["attempts"] == 1
    assert counts["sessions"] == 1
    assert counts["events"] == 1

    # 4. Dry-run report without account mapping
    report = generate_dry_run_report(db_path, account_mapping=None)
    assert report["mode"] == "DRY-RUN (Read-Only)"
    assert report["totalTablesFound"] == 26
    assert report["ownerAudit"]["status"] == "REQUIRES_ACCOUNT_MAPPING"
    assert report["ownerAudit"]["unmappedOwnerIds"] == [1, 2, 3]
    assert report["ownerAudit"]["mappedOwnersCount"] == 0

    # 5. Security policies enforced
    sec = report["securityPolicy"]
    assert sec["scryptPasswordCopyPrevented"] is True
    assert sec["authUsersDirectWritePrevented"] is True
    assert sec["personalDataPrinted"] is False

    # 6. Verified with mock account mapping
    mock_mapping = {
        "1": "00000000-0000-0000-0000-000000000001",
        "2": "00000000-0000-0000-0000-000000000002",
        "3": "00000000-0000-0000-0000-000000000003",
    }
    mapped_report = generate_dry_run_report(db_path, account_mapping=mock_mapping)
    assert mapped_report["ownerAudit"]["status"] == "READY_FOR_IMPORT"
    assert mapped_report["ownerAudit"]["unmappedOwnersCount"] == 0
    assert mapped_report["ownerAudit"]["mappedOwnersCount"] == 3

    # 7. Ephemeral exclusions properly reasoned
    eph = mapped_report["ephemeralExcludedTables"]
    for required in ["sessions", "events", "recovery_tokens", "rate_limits", "idempotency"]:
        assert required in eph
        assert len(eph[required]["reason"]) > 10


def test_export_staging_archive_offline_idempotent(tmp_path):
    """Verifies that the read-only exporter dumps durable SQLite data safely and idempotently."""
    db_obj = Database(tmp_path)
    db_obj.initialize()
    db_path = tmp_path / "aimentor.sqlite3"

    with db_obj.transaction() as db:
        # 3 users: 1 gmail, 2 test-account (*.example)
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (1, "student_gmail", "student@gmail.com", "scrypt$dummyhash1", "GmailStudent", "STUDENT", "BRONZE", 10, 1, 1, 1000.0, 1000.0)
        )
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (2, "test_acc_one", "test1@school.example", "scrypt$dummyhash2", "TestUser1", "STUDENT", "BRONZE", 10, 0, 1, 1001.0, 1001.0)
        )
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (3, "test_acc_two", "test2@school.example", "scrypt$dummyhash3", "TestUser2", "STUDENT", "BRONZE", 12, 0, 1, 1002.0, 1002.0)
        )
        # Durable rows across single-key and composite-key tables
        db.execute(
            "INSERT INTO enrollments (id, user_id, subject_id, grade, curriculum_version, placement, revision, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            ("enr-1", 1, "math", 10, "2026.1", "{}", 1, 1003.0)
        )
        db.execute(
            "INSERT INTO attempts (id, user_id, enrollment_id, unit_id, kind, curriculum_version, step, answers, state, revision, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ("att-1", 1, "enr-1", "math-10-01", "LESSON", "2026.1", 0, "[]", "IN_PROGRESS", 1, 1004.0)
        )
        db.execute(
            "INSERT INTO awards (user_id, subject_id, grade, reward_unit_id, xp, day, created_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (1, "math", 10, "unit-1", 200, "2026-11-01", 1005.0)
        )
        db.execute(
            "INSERT INTO learning_days (user_id, day) VALUES (?, ?)",
            (1, "2026-11-01")
        )
        # Ephemeral rows (must be excluded from staging)
        db.execute(
            "INSERT INTO sessions (id, user_id, access_hash, refresh_hash, access_expires, refresh_expires, revoked) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            ("sess-1", 1, "hash_acc", "hash_ref", 9999.0, 99999.0, 0)
        )
        db.execute(
            "INSERT INTO recovery_tokens (token_hash, user_id, purpose, expires_at, consumed) "
            "VALUES (?, ?, ?, ?, ?)",
            ("token_hash_abc", 1, "RESET_PASSWORD", 9999.0, 0)
        )

    out_dir = tmp_path / "export_out"
    sql_path, manifest_path, manifest = export_sqlite_to_staging(db_path, out_dir)

    assert sql_path.is_file()
    assert manifest_path.is_file()

    # Exact SHA-256 computed from disk matches manifest
    actual_disk_sha256 = hashlib.sha256(sql_path.read_bytes()).hexdigest()
    assert manifest["sqlSha256"] == actual_disk_sha256, (
        f"Manifest SHA-256 {manifest['sqlSha256']} does not match disk SHA-256 {actual_disk_sha256}"
    )

    # Verify table counting: records all 26 source tables, 20 durable, 6 ephemeral
    assert manifest["totalTablesFound"] == 26
    assert len(manifest["tableCounts"]) == 26
    assert len(manifest["stagedDurableTables"]) == 20
    assert len(manifest["excludedEphemeralTables"]) == 6
    assert manifest["distinctOwnersCount"] == 3
    assert manifest["stagedDurableRowsTotal"] == sum(manifest["stagedDurableTables"].values())

    # Verify SQL text and header record source table counts
    raw_sql_bytes = sql_path.read_bytes()
    sql_text = raw_sql_bytes.decode("utf-8")
    assert "-- Source table inventory counts (26 tables):" in sql_text
    assert "-- Durable tables staged: 20" in sql_text
    assert "-- Ephemeral tables excluded: 6" in sql_text

    # Account archiving verification: test accounts archived separately unclaimed
    assert manifest["accountArchive"]["totalAccounts"] == 3
    assert manifest["accountArchive"]["claimableAccountsCount"] == 1
    assert manifest["accountArchive"]["archivedTestAccountsCount"] == 2
    assert manifest["accountArchive"]["testAccountsRemainUnclaimed"] is True
    assert "-- 1a. Claimable accounts" in sql_text
    assert "-- 1b. Test accounts" in sql_text

    # Check manifest security policy flags
    assert manifest["securityPolicy"]["passwordHashExcluded"] is True
    assert manifest["securityPolicy"]["sessionTokensExcluded"] is True
    assert manifest["securityPolicy"]["recoveryTokensExcluded"] is True
    assert manifest["securityPolicy"]["accountEmailIsolatedToUserMappings"] is True

    assert sql_text.startswith("begin;")
    assert sql_text.strip().endswith("commit;")

    # Password hashes and tokens must NEVER be anywhere in SQL statements
    assert "scrypt$" not in sql_text
    assert "dummyhash" not in sql_text
    assert "token_hash_abc" not in sql_text
    assert "hash_acc" not in sql_text
    assert "hash_ref" not in sql_text

    # Email is ONLY present in staging.user_mappings, NEVER in staging.durable_rows
    assert "insert into staging.user_mappings" in sql_text
    assert "student@gmail.com" in sql_text
    assert "test1@school.example" in sql_text
    assert "test2@school.example" in sql_text

    durable_users_lines = [
        line for line in sql_text.splitlines()
        if "insert into staging.durable_rows" in line and "'users'" in line
    ]
    for line in durable_users_lines:
        assert "@" not in line, f"Email leaked into staging.durable_rows users payload: {line}"
        assert "password_hash" not in line

    # Deterministic composite legacy IDs
    assert "'awards', '1:math:10:unit-1'" in sql_text
    assert "'learning_days', '1:2026-11-01'" in sql_text

    # Duplicate export byte-for-byte reproducibility
    out_dir_2 = tmp_path / "export_out_dup"
    sql_path_2, manifest_path_2, manifest_2 = export_sqlite_to_staging(db_path, out_dir_2)
    assert sql_path_2.read_bytes() == sql_path.read_bytes(), "Duplicate export SQL differs byte-for-byte!"
    assert manifest_2["sqlSha256"] == manifest["sqlSha256"]
    assert hashlib.sha256(sql_path_2.read_bytes()).hexdigest() == manifest_2["sqlSha256"]


def test_export_staging_archive_refuses_missing_or_unknown_tables(tmp_path):
    """Exporter must refuse databases with missing or unknown tables to prevent silent data drop."""
    # Test 1: Missing table schema
    partial_dir = tmp_path / "partial_db"
    partial_dir.mkdir()
    partial_db = partial_dir / "partial.sqlite3"
    conn = sqlite3.connect(partial_db)
    conn.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, email TEXT, username TEXT)")
    conn.commit()
    conn.close()

    with pytest.raises(ValueError) as exc_info:
        export_sqlite_to_staging(partial_db, partial_dir / "out")
    assert "missing table(s)" in str(exc_info.value).lower()

    # Test 2: Unrecognized/unknown table schema
    full_dir = tmp_path / "full_db"
    full_dir.mkdir()
    db_obj = Database(full_dir)
    db_obj.initialize()
    full_db = full_dir / "aimentor.sqlite3"

    with db_obj.transaction() as db:
        db.execute("CREATE TABLE rogue_table (id INT, secret TEXT)")

    with pytest.raises(ValueError) as exc_info2:
        export_sqlite_to_staging(full_db, full_dir / "out")
    assert "unrecognized/unknown table(s)" in str(exc_info2.value).lower()
    assert "rogue_table" in str(exc_info2.value)

