"""Targeted offline tests for Supabase cutover claim path, authorization, and reconciliation.

Validates:
- Migration 202611010004 presence, security definer, secure search_path, RLS, and grants.
- Authorization matrix: verified legacy Gmail, unconfirmed email, mismatched email, *.example test accounts, duplicate claims, fresh signup.
- Idempotency: repeated claims return idempotent success without duplicating records.
- Referential integrity: user -> enrollments -> attempts -> completions, awards, learning days, wrong questions.
- Reconciliation: proves 34/34 durable rows accounted for across 20 durable tables.
- Zero PII leaks: reports and test fixtures use anonymous non-production synthetic data.
"""
import hashlib
import json
from pathlib import Path
import re
import sqlite3
import uuid
import pytest
from typing import Any, Dict, List, Optional, Set, Tuple

try:
    from tools.reconcile_cutover import (
        audit_durable_inventory,
        generate_reconciliation_report,
        DURABLE_TABLES,
        PUBLIC_TARGET_MAPPING,
    )
    from tools.export_staging_archive import export_sqlite_to_staging
except ImportError:
    from backend.tools.reconcile_cutover import (
        audit_durable_inventory,
        generate_reconciliation_report,
        DURABLE_TABLES,
        PUBLIC_TARGET_MAPPING,
    )
    from backend.tools.export_staging_archive import export_sqlite_to_staging

from aimentor.database import Database


def test_migration_202611010004_presence_and_security_invariants():
    """Validates the numbered migration file for strict RLS, search_path, and least privilege."""
    repo_root = Path(__file__).resolve().parents[2]
    mig_path = repo_root / "supabase" / "migrations" / "202611010004_claim_staging_progress.sql"
    assert mig_path.is_file(), f"Migration file missing: {mig_path}"

    content = mig_path.read_text(encoding="utf-8")
    assert content.startswith("begin;"), "Migration must be wrapped in a transaction starting with begin;"
    assert content.strip().endswith("commit;"), "Migration must end with commit;"

    # Required tables present
    for tbl in [
        "mentor_questions",
        "mentor_practice_sessions",
        "mentor_wrong_questions",
        "mentor_friendships",
        "mentor_friend_streaks",
        "mentor_badge_catalog",
        "mentor_badge_awards",
        "mentor_ai_practice_sessions",
        "mentor_ai_question_reports",
    ]:
        assert f"create table if not exists public.{tbl}" in content, f"Missing table public.{tbl}"
        assert f"alter table public.{tbl} enable row level security;" in content, f"Missing RLS for public.{tbl}"

    # Verify functions and secure search_path
    assert "create or replace function public.mentor_claim_staged_progress()" in content
    assert "create or replace function staging.reconcile_staging_cutover" in content
    assert "create or replace function public.mentor_friendships_enforce_immutable_endpoints()" in content
    assert "create trigger trg_mentor_friendships_immutable_endpoints" in content

    # Strip SQL comments to verify executable statements
    executable_lines = [
        line.split("--")[0].strip()
        for line in content.splitlines()
        if line.strip() and not line.strip().startswith("--")
    ]
    executable_sql = "\n".join(executable_lines).lower()

    # All security functions must have secure search_path = '' and security definer
    assert executable_sql.count("set search_path = ''") >= 3
    assert executable_sql.count("security definer") >= 3

    # Claim function permissions: revoked from public/anon, granted ONLY to authenticated
    assert "revoke all on function public.mentor_claim_staged_progress() from public, anon;" in content
    assert "grant execute on function public.mentor_claim_staged_progress() to authenticated;" in content

    # Friendship trigger function permissions: revoked from public, anon
    assert "revoke all on function public.mentor_friendships_enforce_immutable_endpoints() from public, anon;" in content

    # Reconciliation function permissions: private to DB administrator (no public/anon/authenticated)
    assert "revoke all on function staging.reconcile_staging_cutover(int) from public, anon, authenticated;" in content
    assert "revoke all on function staging.reconcile_staging_cutover()" not in executable_sql

    # Tightened mentor_friendships permissions: column-level update, no full update grant
    assert "grant select, insert, delete on public.mentor_friendships to authenticated;" in content
    assert "grant update (state, revision, updated_at) on public.mentor_friendships to authenticated;" in content
    assert "grant update on public.mentor_friendships to authenticated" not in executable_sql

    # Friendship policies: separate safe operations, old broad policy removed
    assert "create policy mentor_friendships_select on public.mentor_friendships" in content
    assert "create policy mentor_friendships_insert on public.mentor_friendships" in content
    assert "create policy mentor_friendships_update on public.mentor_friendships" in content
    assert "create policy mentor_friendships_delete on public.mentor_friendships" in content
    assert "create policy mentor_friendships_participant" not in executable_sql

    # Zero anon grants in executable SQL
    assert "grant select on public.mentor_questions to authenticated;" in content
    assert "to anon" not in executable_sql, "Found forbidden grant to anon in migration"

    # Zero credential leaks in executable SQL
    for secret in ["password_hash", "service_role", "sb_secret", "scrypt$"]:
        assert secret not in executable_sql, f"Leaked credential/secret keyword in executable SQL: {secret}"


def create_mock_sqlite_with_34_durable_rows(tmp_path: Path) -> Path:
    """Creates a temporary isolated SQLite database with all 26 tables and exactly 34 durable rows."""
    db_obj = Database(tmp_path)
    db_obj.initialize()
    db_path = tmp_path / "aimentor.sqlite3"

    with db_obj.transaction() as db:
        # 1. Users: 6 rows (3 claimable Gmail accounts, 3 test accounts)
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (1, "student_alpha", "alpha.student@gmail.com", "scrypt$fake1", "Alpha", "STUDENT", "BRONZE", 10, 1, 1, 1000.0, 1000.0),
        )
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (2, "student_beta", "beta.student@gmail.com", "scrypt$fake2", "Beta", "STUDENT", "SILVER", 12, 1, 1, 1001.0, 1001.0),
        )
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (3, "teacher_gamma", "gamma.teacher@gmail.com", "scrypt$fake3", "Gamma", "TEACHER", "GOLD", 10, 1, 1, 1002.0, 1002.0),
        )
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (4, "test_one", "test1@school.example", "scrypt$fake4", "Test1", "STUDENT", "BRONZE", 10, 0, 1, 1003.0, 1003.0),
        )
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (5, "test_two", "test2@school.example", "scrypt$fake5", "Test2", "STUDENT", "BRONZE", 11, 0, 1, 1004.0, 1004.0),
        )
        db.execute(
            "INSERT INTO users (id, username, email, password_hash, nickname, role, rank_tier, grade, verified, revision, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (6, "test_three", "guest@qa.example", "scrypt$fake6", "Guest", "STUDENT", "BRONZE", 12, 0, 1, 1005.0, 1005.0),
        )

        # 2. Enrollments: 4 rows
        db.execute(
            "INSERT INTO enrollments (id, user_id, subject_id, grade, curriculum_version, placement, revision, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            ("550e8400-e29b-41d4-a716-446655440001", 1, "math", 10, "2026.1", '{"score": 80}', 1, 1010.0),
        )
        db.execute(
            "INSERT INTO enrollments (id, user_id, subject_id, grade, curriculum_version, placement, revision, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            ("550e8400-e29b-41d4-a716-446655440002", 1, "physics", 10, "2026.1", '{"score": 90}', 1, 1011.0),
        )
        db.execute(
            "INSERT INTO enrollments (id, user_id, subject_id, grade, curriculum_version, placement, revision, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            ("550e8400-e29b-41d4-a716-446655440003", 2, "chemistry", 12, "2026.1", '{"score": 85}', 1, 1012.0),
        )
        db.execute(
            "INSERT INTO enrollments (id, user_id, subject_id, grade, curriculum_version, placement, revision, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            ("550e8400-e29b-41d4-a716-446655440004", 4, "math", 10, "2026.1", '{}', 1, 1013.0),
        )

        # 3. Attempts: 5 rows
        for i in range(1, 6):
            uid = 1 if i <= 3 else (2 if i == 4 else 4)
            eid = f"550e8400-e29b-41d4-a716-44665544000{1 if uid == 1 else (3 if uid == 2 else 4)}"
            db.execute(
                "INSERT INTO attempts (id, user_id, enrollment_id, unit_id, kind, curriculum_version, step, answers, state, revision, updated_at) "
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (f"660e8400-e29b-41d4-a716-44665544000{i}", uid, eid, f"unit-0{i}", "LESSON", "2026.1", 3, '[{"questionId": "q1", "selected": "A"}]', "COMPLETED", 1, 1020.0 + i),
            )

        # 4. Completions: 5 rows (matches attempts)
        for i in range(1, 6):
            uid = 1 if i <= 3 else (2 if i == 4 else 4)
            att_id = f"660e8400-e29b-41d4-a716-44665544000{i}"
            db.execute(
                "INSERT INTO completions (id, attempt_id, user_id, xp, day, result, completed_at) "
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                (f"cmp-0{i}", att_id, uid, 200, "2026-11-01", '{"score": 100, "passed": true}', 1030.0 + i),
            )

        # 5. Awards: 2 rows
        db.execute(
            "INSERT INTO awards (user_id, subject_id, grade, reward_unit_id, xp, day, created_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (1, "math", 10, "math-10-01", 200, "2026-11-01", 1040.0),
        )
        db.execute(
            "INSERT INTO awards (user_id, subject_id, grade, reward_unit_id, xp, day, created_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (2, "chemistry", 12, "chem-12-01", 400, "2026-11-01", 1041.0),
        )

        # 6. Learning days: 1 row
        db.execute(
            "INSERT INTO learning_days (user_id, day) VALUES (?, ?)",
            (1, "2026-11-01"),
        )

        # 7. Questions: 5 rows (question bank authored by teacher gamma)
        for i in range(1, 6):
            db.execute(
                "INSERT INTO questions (id, code, version, subject_id, grade, chapter, topic, prompt, option_a, option_b, option_c, option_d, correct_option, explanation, points, status, created_by, created_at, updated_at) "
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (f"q-0{i}", f"CODE-0{i}", 1, "math", 10, "Chương 1", "Mệnh đề", f"Câu hỏi {i}?", "A", "B", "C", "D", "A", "Lời giải", 10, "PUBLISHED", 3, 1050.0 + i, 1050.0 + i),
            )

        # 8. Library quizzes: 1 row
        db.execute(
            "INSERT INTO library_quizzes (id, title, subject_id, grade, chapter, topic, difficulty, question_ids, status, created_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ("quiz-math-10-01", "Ôn tập chương 1 Toán 10", "math", 10, "Chương 1", "Mệnh đề", "MEDIUM", "q-01,q-02,q-03,q-04,q-05", "PUBLISHED", 1060.0),
        )

        # 9. Practice sessions: 1 row
        db.execute(
            "INSERT INTO practice_sessions (id, user_id, quiz_id, subject_id, grade, question_snapshots, step, answers, state, score, correct_count, total_count, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ("770e8400-e29b-41d4-a716-446655440001", 1, "quiz-math-10-01", "math", 10, "[]", 0, "{}", "IN_PROGRESS", 0, 0, 5, 1070.0, 1070.0),
        )

        # 10. Wrong questions notebook: 2 rows
        db.execute(
            "INSERT INTO wrong_questions_notebook (id, user_id, question_id, subject_id, grade, selected_option, correct_option, explanation, state, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ("880e8400-e29b-41d4-a716-446655440001", 1, "q-01", "math", 10, "B", "A", "Cần chú ý định nghĩa", "NEEDS_REVIEW", 1080.0, 1080.0),
        )
        db.execute(
            "INSERT INTO wrong_questions_notebook (id, user_id, question_id, subject_id, grade, selected_option, correct_option, explanation, state, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ("880e8400-e29b-41d4-a716-446655440002", 1, "q-02", "math", 10, "C", "A", "Xem lại tính chất", "RESOLVED", 1081.0, 1081.0),
        )

        # 11. Exams: 2 rows (authored by teacher gamma)
        db.execute(
            "INSERT INTO exams (id, title, subject_id, grade, exam_type, semester, duration_minutes, question_ids, total_points, question_count, status, version, created_by, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ("exam-math-10-midterm", "Đề giữa kỳ 1 Toán 10", "math", 10, "MIDTERM", 1, 45, "q-01,q-02,q-03", 30, 3, "PUBLISHED", 1, 3, 1090.0, 1090.0),
        )
        db.execute(
            "INSERT INTO exams (id, title, subject_id, grade, exam_type, semester, duration_minutes, question_ids, total_points, question_count, status, version, created_by, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ("exam-math-10-final", "Đề cuối kỳ 1 Toán 10", "math", 10, "FINAL", 1, 90, "q-01,q-02,q-03,q-04,q-05", 50, 5, "PUBLISHED", 1, 3, 1091.0, 1091.0),
        )

        # Add ephemeral rows (must be excluded from durable cutover)
        db.execute(
            "INSERT INTO sessions (id, user_id, access_hash, refresh_hash, access_expires, refresh_expires, revoked) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            ("sess-1", 1, "hash_acc1", "hash_ref1", 99999.0, 999999.0, 0),
        )
        db.execute(
            "INSERT INTO events (owner, resource_type, resource_id, revision, operation, created_at) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (1, "ATTEMPT", "att-1", 1, "CREATE", 1099.0),
        )

    return db_path


def test_durable_inventory_and_counts(tmp_path):
    """Verifies that the test fixture produces exactly 34 durable rows across 11 populated tables."""
    db_path = create_mock_sqlite_with_34_durable_rows(tmp_path)
    inventory = audit_durable_inventory(db_path)

    assert inventory["totalTables"] == 26
    assert inventory["totalDurableRows"] == 34, f"Expected 34 durable rows, got {inventory['totalDurableRows']}"

    # 11 populated tables: 6 + 4 + 5 + 5 + 2 + 1 + 5 + 1 + 1 + 2 + 2 = 34
    expected_populated = {
        "users": 6,
        "enrollments": 4,
        "attempts": 5,
        "completions": 5,
        "awards": 2,
        "learning_days": 1,
        "questions": 5,
        "library_quizzes": 1,
        "practice_sessions": 1,
        "wrong_questions_notebook": 2,
        "exams": 2,
    }
    for tbl, count in expected_populated.items():
        assert inventory["durableCounts"][tbl] == count, f"Table {tbl} expected {count}, found {inventory['durableCounts'][tbl]}"

    # Other 9 durable tables must be exactly 0
    empty_durable = set(DURABLE_TABLES) - set(expected_populated.keys())
    assert len(empty_durable) == 9
    for tbl in empty_durable:
        assert inventory["durableCounts"][tbl] == 0, f"Table {tbl} expected 0, found {inventory['durableCounts'][tbl]}"

    # Accounts: 3 claimable, 3 test accounts
    assert len(inventory["claimableAccountIds"]) == 3
    assert len(inventory["archivedTestAccountIds"]) == 3
    assert inventory["claimableAccountIds"] == [1, 2, 3]
    assert inventory["archivedTestAccountIds"] == [4, 5, 6]


def test_reconciliation_dry_run_report_invariants(tmp_path):
    """Verifies that the dry-run reconciliation report accounts for 34/34 rows without PII."""
    db_path = create_mock_sqlite_with_34_durable_rows(tmp_path)

    # 1. Unclaimed state: cutoverComplete MUST be False
    report = generate_reconciliation_report(db_path, claimed_mappings={})
    assert report["cutoverComplete"] is False
    assert report["status"] == "IN_PROGRESS_PENDING_CLAIMS"
    assert report["stagingInventory"]["accountedFor34Of34"] is True
    assert report["stagingInventory"]["totalDurableRows"] == 34
    assert report["stagingInventory"]["populatedDurableTablesCount"] == 11
    assert report["stagingInventory"]["emptyDurableTablesCount"] == 9

    # Account counts
    acct = report["accountReconciliation"]
    assert acct["totalAccounts"] == 6
    assert acct["claimableAccountsCount"] == 3
    assert acct["claimedAccountsCount"] == 0
    assert acct["unclaimedAccountsCount"] == 3
    assert acct["archivedTestAccountsCount"] == 3
    assert acct["testAccountsRemainUnclaimed"] is True

    # PII verification: zero emails, passwords or personal names in output JSON
    serialized = json.dumps(report)
    assert "@" not in serialized, "Found email character '@' in reconciliation report!"
    assert "gmail" not in serialized.lower()
    assert "alpha" not in serialized.lower()
    assert "gamma" not in serialized.lower()
    assert "scrypt$" not in serialized

    # Unresolved mappings lists legacyUserIds
    unresolved_ids = [u["legacyUserId"] for u in report["unresolvedMappings"]]
    assert unresolved_ids == [1, 2, 3]

    # Archived test accounts
    test_ids = [u["legacyUserId"] for u in report["archivedTestAccounts"]]
    assert test_ids == [4, 5, 6]

    # 2. Partially claimed state (1 claimed, 2 unclaimed): cutover STILL incomplete
    partial_claim = {1: str(uuid.uuid4())}
    partial_report = generate_reconciliation_report(db_path, partial_claim)
    assert partial_report["cutoverComplete"] is False
    assert partial_report["status"] == "IN_PROGRESS_PENDING_CLAIMS"
    assert partial_report["accountReconciliation"]["claimedAccountsCount"] == 1
    assert partial_report["accountReconciliation"]["unclaimedAccountsCount"] == 2

    # 3. All claimable accounts claimed: cutoverComplete becomes True
    full_claim = {
        1: str(uuid.uuid4()),
        2: str(uuid.uuid4()),
        3: str(uuid.uuid4()),
    }
    full_report = generate_reconciliation_report(db_path, full_claim)
    assert full_report["cutoverComplete"] is True
    assert full_report["status"] == "ALL_CLAIMABLE_ACCOUNTS_CLAIMED"
    assert full_report["accountReconciliation"]["unclaimedAccountsCount"] == 0
    assert full_report["accountReconciliation"]["claimedAccountsCount"] == 3
    assert full_report["accountReconciliation"]["archivedTestAccountsCount"] == 3


class MockClaimSimulator:
    """Offline Python simulation of the Postgres mentor_claim_staged_progress function logic."""

    def __init__(self, user_mappings: List[dict], durable_rows: List[dict]):
        self.user_mappings = {
            m.get("legacy_user_id", m.get("id")): dict(m)
            for m in user_mappings
        }
        self.durable_rows = list(durable_rows)
        # Target active public tables
        self.profiles: Dict[str, dict] = {}
        self.preferences: Dict[str, dict] = {}
        self.enrollments: List[dict] = []
        self.attempts: List[dict] = []
        self.completions: List[dict] = []
        self.awards: List[dict] = []
        self.learning_days: Set[Tuple[str, str]] = set()
        self.practice_sessions: List[dict] = []
        self.wrong_questions: List[dict] = []

    def claim(self, caller_uid: str, caller_email: str, email_confirmed: bool) -> dict:
        # Rule 1: Authenticated caller
        if not caller_uid:
            raise PermissionError("AUTHENTICATION_REQUIRED")

        # Rule 2: Unconfirmed email rejection
        if not email_confirmed:
            raise PermissionError("EMAIL_CONFIRMATION_REQUIRED")

        # Rule 3: Idempotency (has this caller already claimed an account?)
        for uid, m in self.user_mappings.items():
            if m.get("target_auth_uid") == caller_uid:
                return {
                    "claimed": True,
                    "status": "ALREADY_CLAIMED",
                    "legacyUserId": uid,
                    "replayed": True,
                }

        # Rule 4: Match legacy email
        match_legacy_id = None
        for uid, m in self.user_mappings.items():
            if m["email"].strip().lower() == caller_email.strip().lower():
                match_legacy_id = uid
                break

        if match_legacy_id is None:
            return {
                "claimed": False,
                "status": "NO_STAGED_PROGRESS",
                "message": "No staged legacy progress associated with this email.",
            }

        target_m = self.user_mappings[match_legacy_id]

        # Rule 5: Reject *.example test accounts
        if target_m["email"].endswith(".example") or "@example." in target_m["email"]:
            raise PermissionError("TEST_ACCOUNT_ARCHIVED_CANNOT_BE_CLAIMED")

        # Rule 6: Reject email mismatch
        if target_m["email"].lower() != caller_email.lower():
            raise PermissionError("EMAIL_MISMATCH")

        # Rule 7: Reject duplicate claims by different users
        if target_m.get("target_auth_uid") and target_m["target_auth_uid"] != caller_uid:
            raise ValueError("LEGACY_ACCOUNT_ALREADY_CLAIMED")

        # Migrate only rows belonging to this legacy_user_id
        migrated_count = 0
        for r in self.durable_rows:
            if r.get("legacy_user_id") != match_legacy_id:
                continue

            tbl = r["table_name"]
            payload = r["payload"]

            if tbl == "users":
                self.profiles[caller_uid] = {
                    "nickname": payload.get("nickname", ""),
                    "bio": payload.get("bio", ""),
                }
                self.preferences[caller_uid] = {"grade": payload.get("grade", 0)}
                migrated_count += 1
            elif tbl == "enrollments":
                self.enrollments.append({**payload, "user_id": caller_uid})
                migrated_count += 1
            elif tbl == "attempts":
                self.attempts.append({**payload, "user_id": caller_uid})
                migrated_count += 1
            elif tbl == "completions":
                self.completions.append({**payload, "user_id": caller_uid})
                migrated_count += 1
            elif tbl == "awards":
                self.awards.append({**payload, "user_id": caller_uid})
                migrated_count += 1
            elif tbl == "learning_days":
                self.learning_days.add((caller_uid, payload["day"]))
                migrated_count += 1
            elif tbl == "practice_sessions":
                self.practice_sessions.append({**payload, "user_id": caller_uid})
                migrated_count += 1
            elif tbl == "wrong_questions_notebook":
                self.wrong_questions.append({**payload, "user_id": caller_uid})
                migrated_count += 1

        # Record claim
        target_m["target_auth_uid"] = caller_uid
        target_m["claimed_at"] = "2026-11-01T00:00:00Z"

        return {
            "claimed": True,
            "status": "SUCCESSFULLY_CLAIMED",
            "legacyUserId": match_legacy_id,
            "claimedRowsTotal": migrated_count,
        }


def test_claim_authorization_matrix_and_rejections(tmp_path):
    """Exhaustively tests the claim path security matrix: unconfirmed, test, mismatch, duplicate, fresh signup."""
    db_path = create_mock_sqlite_with_34_durable_rows(tmp_path)
    out_dir = tmp_path / "staging_export"
    _, _, manifest = export_sqlite_to_staging(db_path, out_dir)

    # Load synthetic staged rows and user mappings
    conn = sqlite3.connect(f"file:{db_path.resolve()}?mode=ro", uri=True)
    conn.row_factory = sqlite3.Row
    user_rows = [dict(r) for r in conn.execute("SELECT * FROM users").fetchall()]
    all_durable_rows = []
    for tbl in DURABLE_TABLES:
        for r in conn.execute(f"SELECT * FROM {tbl}").fetchall():
            all_durable_rows.append({
                "table_name": tbl,
                "legacy_id": str(r["id"]) if "id" in r.keys() else "key",
                "legacy_user_id": r["user_id"] if "user_id" in r.keys() else (r["created_by"] if "created_by" in r.keys() else (r["id"] if tbl == "users" else None)),
                "payload": dict(r),
            })
    conn.close()

    simulator = MockClaimSimulator(user_rows, all_durable_rows)

    # 1. Unconfirmed email rejection
    with pytest.raises(PermissionError) as exc_unconfirmed:
        simulator.claim(str(uuid.uuid4()), "alpha.student@gmail.com", email_confirmed=False)
    assert "EMAIL_CONFIRMATION_REQUIRED" in str(exc_unconfirmed.value)

    # 2. Test account (*.example) claim rejection
    with pytest.raises(PermissionError) as exc_test:
        simulator.claim(str(uuid.uuid4()), "test1@school.example", email_confirmed=True)
    assert "TEST_ACCOUNT_ARCHIVED_CANNOT_BE_CLAIMED" in str(exc_test.value)

    # 3. Fresh signup with no staged record -> Clean empty state
    fresh_uid = str(uuid.uuid4())
    fresh_res = simulator.claim(fresh_uid, "brand_new_user@gmail.com", email_confirmed=True)
    assert fresh_res["claimed"] is False
    assert fresh_res["status"] == "NO_STAGED_PROGRESS"
    assert fresh_uid not in simulator.profiles

    # 4. Legitimate verified claim by legacy Gmail account
    alpha_uid = str(uuid.uuid4())
    alpha_res = simulator.claim(alpha_uid, "alpha.student@gmail.com", email_confirmed=True)
    assert alpha_res["claimed"] is True
    assert alpha_res["status"] == "SUCCESSFULLY_CLAIMED"
    assert alpha_res["legacyUserId"] == 1
    assert alpha_res["claimedRowsTotal"] > 0

    # Verify student Alpha claimed ONLY their own progress
    assert simulator.profiles[alpha_uid]["nickname"] == "Alpha"
    assert len([e for e in simulator.enrollments if e["user_id"] == alpha_uid]) == 2
    assert len([a for a in simulator.attempts if a["user_id"] == alpha_uid]) == 3
    assert len([c for c in simulator.completions if c["user_id"] == alpha_uid]) == 3
    assert len([w for w in simulator.wrong_questions if w["user_id"] == alpha_uid]) == 2
    assert (alpha_uid, "2026-11-01") in simulator.learning_days

    # Ensure Beta's and Gamma's data was NOT claimed by Alpha
    assert not any(e["user_id"] == alpha_uid and e["subject_id"] == "chemistry" for e in simulator.enrollments)

    # 5. Idempotent re-claim by same user returns ALREADY_CLAIMED
    reclaim_res = simulator.claim(alpha_uid, "alpha.student@gmail.com", email_confirmed=True)
    assert reclaim_res["claimed"] is True
    assert reclaim_res["status"] == "ALREADY_CLAIMED"
    assert reclaim_res["replayed"] is True

    # 6. Duplicate claim attempt by a DIFFERENT UUID on the same legacy account
    imposter_uid = str(uuid.uuid4())
    with pytest.raises(ValueError) as exc_dup:
        simulator.claim(imposter_uid, "alpha.student@gmail.com", email_confirmed=True)
    assert "LEGACY_ACCOUNT_ALREADY_CLAIMED" in str(exc_dup.value)


def test_referential_integrity_between_claimed_progress_tables(tmp_path):
    """Verifies that foreign key linkages across claimed entities remain strictly intact."""
    db_path = create_mock_sqlite_with_34_durable_rows(tmp_path)
    conn = sqlite3.connect(f"file:{db_path.resolve()}?mode=ro", uri=True)
    conn.row_factory = sqlite3.Row

    user_rows = [dict(r) for r in conn.execute("SELECT * FROM users").fetchall()]
    all_durable_rows = []
    for tbl in DURABLE_TABLES:
        for r in conn.execute(f"SELECT * FROM {tbl}").fetchall():
            all_durable_rows.append({
                "table_name": tbl,
                "legacy_id": str(r["id"]) if "id" in r.keys() else "key",
                "legacy_user_id": r["user_id"] if "user_id" in r.keys() else (r["created_by"] if "created_by" in r.keys() else (r["id"] if tbl == "users" else None)),
                "payload": dict(r),
            })
    conn.close()

    simulator = MockClaimSimulator(user_rows, all_durable_rows)
    beta_uid = str(uuid.uuid4())
    res = simulator.claim(beta_uid, "beta.student@gmail.com", email_confirmed=True)
    assert res["claimed"] is True
    assert res["legacyUserId"] == 2

    # Beta has 1 enrollment (chemistry-12)
    assert len(simulator.enrollments) == 1
    enr = simulator.enrollments[0]
    assert enr["user_id"] == beta_uid
    assert enr["subject_id"] == "chemistry"
    assert enr["grade"] == 12

    # Beta has 1 attempt matching enrollment
    assert len(simulator.attempts) == 1
    att = simulator.attempts[0]
    assert att["user_id"] == beta_uid
    assert att["enrollment_id"] == enr["id"]
    assert att["kind"] == "LESSON"
    assert att["state"] == "COMPLETED"

    # Beta has 1 completion matching attempt
    assert len(simulator.completions) == 1
    cmp = simulator.completions[0]
    assert cmp["user_id"] == beta_uid
    assert cmp["attempt_id"] == att["id"]
    assert cmp["xp"] == 200

    # Beta has 1 award
    assert len(simulator.awards) == 1
    awd = simulator.awards[0]
    assert awd["user_id"] == beta_uid
    assert awd["subject_id"] == "chemistry"
    assert awd["grade"] == 12
    assert awd["xp"] == 400


class MockFriendshipSecurityEngine:
    """Exact offline simulation of PostgreSQL RLS policies, column grants, and trigger for mentor_friendships."""

    def __init__(self):
        # Rows: id -> dict(id, requester, recipient, state, revision, updated_at)
        self.rows: Dict[str, dict] = {}
        # Columns granted to authenticated for UPDATE:
        self.authenticated_updatable_columns = {"state", "revision", "updated_at"}

    def insert(self, caller_uid: str, requester: str, recipient: str, state: str = "PENDING") -> dict:
        # Table check constraint: requester <> recipient
        if requester == recipient:
            raise ValueError("requester and recipient must be distinct (requester <> recipient)")

        # RLS INSERT with check: (auth.uid() = requester and auth.uid() <> recipient and state = 'PENDING')
        if caller_uid != requester:
            raise PermissionError("RLS VIOLATION: Cannot impersonate another user as friendship requester")
        if caller_uid == recipient:
            raise PermissionError("RLS VIOLATION: Cannot send friend request to self")
        if state != "PENDING":
            raise PermissionError(f"RLS VIOLATION: Initial friendship status must be PENDING, got {state}")

        # Unique constraint on (requester, recipient)
        for r in self.rows.values():
            if r["requester"] == requester and r["recipient"] == recipient:
                raise ValueError("Unique constraint violation: friendship already exists")

        row_id = str(uuid.uuid4())
        record = {
            "id": row_id,
            "requester": requester,
            "recipient": recipient,
            "state": state,
            "revision": 1,
            "updated_at": 1000.0,
        }
        self.rows[row_id] = record
        return dict(record)

    def select(self, caller_uid: str, row_id: str) -> Optional[dict]:
        # RLS SELECT using: auth.uid() in (requester, recipient)
        row = self.rows.get(row_id)
        if not row:
            return None
        if caller_uid not in (row["requester"], row["recipient"]):
            # RLS silently filters out rows caller cannot select
            return None
        return dict(row)

    def trigger_check(self, old_row: dict, new_row: dict, caller_uid: Optional[str] = None):
        """Simulates trg_mentor_friendships_immutable_endpoints."""
        if new_row["id"] != old_row["id"] or new_row["requester"] != old_row["requester"] or new_row["recipient"] != old_row["recipient"]:
            raise PermissionError("TRIGGER VIOLATION: Friendship endpoints (requester, recipient) and ID are immutable")

        if caller_uid is not None:
            if old_row["state"] == "BLOCKED" and new_row["state"] != "BLOCKED":
                raise PermissionError("TRIGGER VIOLATION: Cannot change status of a blocked relationship")
            if new_row["state"] == "ACCEPTED" and (old_row["state"] != "PENDING" or caller_uid != old_row["recipient"]):
                raise PermissionError("TRIGGER VIOLATION: Only the recipient can accept a pending friendship request")
            if new_row["state"] == "PENDING" and old_row["state"] != "PENDING":
                raise PermissionError("TRIGGER VIOLATION: Cannot transition status back to PENDING")

    def update(self, caller_uid: str, row_id: str, updated_columns: dict) -> dict:
        row = self.rows.get(row_id)
        if not row:
            raise KeyError("Row not found")

        # 1. Column Grants Check: Authenticated role only has grant update on (state, revision, updated_at)
        disallowed_columns = set(updated_columns.keys()) - self.authenticated_updatable_columns
        if disallowed_columns:
            raise PermissionError(f"COLUMN GRANT VIOLATION: Permission denied for column(s) {disallowed_columns} of relation mentor_friendships")

        # 2. RLS UPDATE USING: auth.uid() in (requester, recipient)
        if caller_uid not in (row["requester"], row["recipient"]):
            raise PermissionError("RLS VIOLATION: Caller is not a participant in this friendship")

        # Simulate proposed new row
        new_row = dict(row)
        new_row.update(updated_columns)

        # 3. Trigger check
        self.trigger_check(row, new_row, caller_uid=caller_uid)

        # 4. RLS UPDATE WITH CHECK:
        # auth.uid() in (requester, recipient) and
        # (((auth.uid() = recipient and state in ('ACCEPTED', 'BLOCKED')) or (auth.uid() = requester and state = 'BLOCKED')))
        is_recipient = (caller_uid == new_row["recipient"])
        is_requester = (caller_uid == new_row["requester"])
        new_state = new_row["state"]

        valid_check = (
            caller_uid in (new_row["requester"], new_row["recipient"]) and (
                (is_recipient and new_state in ("ACCEPTED", "BLOCKED")) or
                (is_requester and new_state == "BLOCKED")
            )
        )
        if not valid_check:
            raise PermissionError(f"RLS WITH CHECK VIOLATION: Disallowed state transition to {new_state} by caller")

        new_row["revision"] = row["revision"] + 1
        new_row["updated_at"] = row["updated_at"] + 1.0
        self.rows[row_id] = new_row
        return dict(new_row)

    def delete(self, caller_uid: str, row_id: str) -> bool:
        row = self.rows.get(row_id)
        if not row:
            return False

        # RLS DELETE USING: state in ('PENDING', 'ACCEPTED') and auth.uid() in (requester, recipient)
        if caller_uid not in (row["requester"], row["recipient"]):
            raise PermissionError("RLS VIOLATION: Caller is not a participant in this friendship")
        if row["state"] == "BLOCKED":
            raise PermissionError("RLS VIOLATION: Blocked relationships cannot be deleted by participants")
        if row["state"] not in ("PENDING", "ACCEPTED"):
            raise PermissionError(f"RLS VIOLATION: Cannot delete relationship in state {row['state']}")

        del self.rows[row_id]
        return True


def test_friendship_endpoint_immutability_and_role_isolation():
    """Validates that friendship endpoints (requester/recipient) are immutable and actors cannot impersonate pairs."""
    engine = MockFriendshipSecurityEngine()
    alice_uid = str(uuid.uuid4())
    bob_uid = str(uuid.uuid4())
    eve_uid = str(uuid.uuid4())

    # 1. Legitimate friend request: Alice requests Bob
    conn = engine.insert(caller_uid=alice_uid, requester=alice_uid, recipient=bob_uid, state="PENDING")
    row_id = conn["id"]
    assert conn["state"] == "PENDING"
    assert conn["requester"] == alice_uid
    assert conn["recipient"] == bob_uid

    # 2. Impersonating requester blocked: Eve attempts to create request as Alice
    with pytest.raises(PermissionError) as exc_imp:
        engine.insert(caller_uid=eve_uid, requester=alice_uid, recipient=bob_uid, state="PENDING")
    assert "Cannot impersonate" in str(exc_imp.value)

    # 3. Self-friending blocked
    with pytest.raises((ValueError, PermissionError)):
        engine.insert(caller_uid=alice_uid, requester=alice_uid, recipient=alice_uid)

    # 4. Sane status required on insert (cannot insert directly as ACCEPTED or BLOCKED)
    with pytest.raises(PermissionError) as exc_stat:
        engine.insert(caller_uid=alice_uid, requester=alice_uid, recipient=eve_uid, state="ACCEPTED")
    assert "Initial friendship status must be PENDING" in str(exc_stat.value)

    # 5. Safe SELECT for participants only
    assert engine.select(caller_uid=alice_uid, row_id=row_id)["id"] == row_id
    assert engine.select(caller_uid=bob_uid, row_id=row_id)["id"] == row_id
    assert engine.select(caller_uid=eve_uid, row_id=row_id) is None, "Unrelated user Eve must not see friendship"

    # 6. Immutable endpoint columns: neither participant can change requester or recipient
    with pytest.raises(PermissionError) as exc_recip:
        engine.update(caller_uid=alice_uid, row_id=row_id, updated_columns={"recipient": eve_uid})
    assert "COLUMN GRANT VIOLATION" in str(exc_recip.value)

    with pytest.raises(PermissionError) as exc_req:
        engine.update(caller_uid=bob_uid, row_id=row_id, updated_columns={"requester": eve_uid})
    assert "COLUMN GRANT VIOLATION" in str(exc_req.value)

    # Trigger-level check proving endpoint immutability cannot be bypassed
    old_row = {"id": row_id, "requester": alice_uid, "recipient": bob_uid, "state": "PENDING"}
    forged_row = {"id": row_id, "requester": eve_uid, "recipient": bob_uid, "state": "PENDING"}
    with pytest.raises(PermissionError) as exc_trg:
        engine.trigger_check(old_row, forged_row, caller_uid=alice_uid)
    assert "immutable" in str(exc_trg.value).lower()

    # 7. Requester cannot accept their own request
    with pytest.raises(PermissionError) as exc_own_acc:
        engine.update(caller_uid=alice_uid, row_id=row_id, updated_columns={"state": "ACCEPTED"})
    assert "Only the recipient can accept" in str(exc_own_acc.value)

    # 8. Unrelated user Eve cannot update Alice & Bob's friendship
    with pytest.raises(PermissionError) as exc_eve_upd:
        engine.update(caller_uid=eve_uid, row_id=row_id, updated_columns={"state": "ACCEPTED"})
    assert "not a participant" in str(exc_eve_upd.value).lower()

    # 9. Recipient Bob accepts the request
    acc = engine.update(caller_uid=bob_uid, row_id=row_id, updated_columns={"state": "ACCEPTED"})
    assert acc["state"] == "ACCEPTED"
    assert acc["revision"] == 2

    # 10. Cannot transition status back to PENDING
    with pytest.raises(PermissionError) as exc_pending:
        engine.update(caller_uid=bob_uid, row_id=row_id, updated_columns={"state": "PENDING"})
    assert "Cannot transition status back to PENDING" in str(exc_pending.value)

    # 11. Alice blocks Bob
    blk = engine.update(caller_uid=alice_uid, row_id=row_id, updated_columns={"state": "BLOCKED"})
    assert blk["state"] == "BLOCKED"

    # 12. Bob cannot transition out of BLOCKED
    with pytest.raises(PermissionError) as exc_unblk:
        engine.update(caller_uid=bob_uid, row_id=row_id, updated_columns={"state": "ACCEPTED"})
    assert "Cannot change status of a blocked relationship" in str(exc_unblk.value)

    # 13. Blocked relationship CANNOT be deleted by either party
    with pytest.raises(PermissionError) as exc_del_bob:
        engine.delete(caller_uid=bob_uid, row_id=row_id)
    assert "Blocked relationships cannot be deleted" in str(exc_del_bob.value)

    with pytest.raises(PermissionError) as exc_del_alice:
        engine.delete(caller_uid=alice_uid, row_id=row_id)
    assert "Blocked relationships cannot be deleted" in str(exc_del_alice.value)

    # 14. Non-blocked relationships can be explicitly deleted (cancel, decline, unfriend)
    # Cancel pending:
    c1 = engine.insert(caller_uid=alice_uid, requester=alice_uid, recipient=eve_uid, state="PENDING")
    assert engine.delete(caller_uid=alice_uid, row_id=c1["id"]) is True

    # Decline pending:
    c2 = engine.insert(caller_uid=eve_uid, requester=eve_uid, recipient=alice_uid, state="PENDING")
    assert engine.delete(caller_uid=alice_uid, row_id=c2["id"]) is True

    # Unfriend accepted:
    c3 = engine.insert(caller_uid=alice_uid, requester=alice_uid, recipient=eve_uid, state="PENDING")
    engine.update(caller_uid=eve_uid, row_id=c3["id"], updated_columns={"state": "ACCEPTED"})
    assert engine.delete(caller_uid=alice_uid, row_id=c3["id"]) is True


def test_reconciliation_dynamic_source_sizes_and_manifest(tmp_path):
    """Proves reconciliation works truthfully for dynamic source sizes and never claims completeness merely because row count is 34."""
    # 1. Create a custom SQLite database with 40 durable rows (34 baseline + 6 additional questions)
    db_path = create_mock_sqlite_with_34_durable_rows(tmp_path / "dynamic_db")
    conn = sqlite3.connect(db_path)
    for i in range(6, 12):
        conn.execute(
            "INSERT INTO questions (id, code, version, subject_id, grade, chapter, topic, prompt, option_a, option_b, option_c, option_d, correct_option, explanation, points, status, created_by, created_at, updated_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (f"q-dyn-{i}", f"CODE-DYN-{i}", 1, "math", 10, "C1", "T1", f"Q{i}", "A", "B", "C", "D", "A", "exp", 10, "PUBLISHED", 3, 1050.0 + i, 1050.0 + i),
        )
    conn.commit()
    conn.close()

    full_claim = {1: str(uuid.uuid4()), 2: str(uuid.uuid4()), 3: str(uuid.uuid4())}

    # Verify audit inventory counts 40 durable rows
    inv = audit_durable_inventory(db_path)
    assert inv["totalDurableRows"] == 40

    # 1. When expected_durable_count is 40: cutoverComplete succeeds despite not being 34!
    report_40 = generate_reconciliation_report(db_path, full_claim, expected_durable_count=40)
    assert report_40["cutoverComplete"] is True
    assert report_40["status"] == "ALL_CLAIMABLE_ACCOUNTS_CLAIMED"
    assert report_40["stagingInventory"]["totalDurableRows"] == 40
    assert report_40["stagingInventory"]["accountedFor34Of34"] is False, "Truthfully reports 40 != 34 baseline"
    assert report_40["stagingInventory"]["expectedDurableRows"] == 40
    assert report_40["stagingInventory"]["rowsMatchExpected"] is True

    # 2. When expected_durable_count is 50 (mismatch): cutover is NOT marked complete!
    report_mismatch = generate_reconciliation_report(db_path, full_claim, expected_durable_count=50)
    assert report_mismatch["cutoverComplete"] is False
    assert report_mismatch["status"] == "ROW_COUNT_MISMATCH_EXPECTED"
    assert report_mismatch["stagingInventory"]["rowsMatchExpected"] is False

    # 3. Deriving expected count from staging_manifest.json
    manifest_file = tmp_path / "dynamic_db" / "staging_manifest.json"
    manifest_file.write_text(json.dumps({"stagedDurableRowsTotal": 40}), encoding="utf-8")
    report_manifest = generate_reconciliation_report(db_path, full_claim, manifest_path=manifest_file)
    assert report_manifest["cutoverComplete"] is True
    assert report_manifest["stagingInventory"]["expectedDurableRows"] == 40
    assert report_manifest["stagingInventory"]["rowsMatchExpected"] is True

    # 4. Never claim completeness merely because row count happens to equal 34:
    # If a database has 34 rows, but expected source count was 38:
    db_34_path = create_mock_sqlite_with_34_durable_rows(tmp_path / "db_34")
    report_34_expected_38 = generate_reconciliation_report(db_34_path, full_claim, expected_durable_count=38)
    assert report_34_expected_38["stagingInventory"]["accountedFor34Of34"] is True
    assert report_34_expected_38["stagingInventory"]["totalDurableRows"] == 34
    assert report_34_expected_38["cutoverComplete"] is False, "Must not claim completeness merely because count is 34 when 38 was expected"
    assert report_34_expected_38["status"] == "ROW_COUNT_MISMATCH_EXPECTED"


def translate_legacy_quiz_questions_py(payload: dict, questions_bank: dict) -> list:
    """Exact offline Python reference of staging.translate_legacy_quiz_questions PL/pgSQL function."""
    if not payload:
        return []

    # 1. Non-empty array of question objects pass-through
    raw_q = payload.get("questions")
    if isinstance(raw_q, list) and len(raw_q) > 0 and isinstance(raw_q[0], dict) and ("prompt" in raw_q[0] or "options" in raw_q[0]):
        return raw_q

    # 2. Extract ordered question IDs
    q_ids = []
    raw_ids = payload.get("question_ids")
    if isinstance(raw_ids, list):
        q_ids = [str(x).strip() for x in raw_ids if str(x).strip()]
    elif isinstance(raw_q, list) and len(raw_q) > 0 and isinstance(raw_q[0], str):
        q_ids = [str(x).strip() for x in raw_q if str(x).strip()]
    elif isinstance(raw_ids, str) and raw_ids.strip():
        s = raw_ids.strip()
        if s.startswith("[") and s.endswith("]"):
            try:
                parsed = json.loads(s)
                if isinstance(parsed, list):
                    q_ids = [str(x).strip() for x in parsed if str(x).strip()]
            except Exception:
                pass
        if not q_ids:
            q_ids = [x.strip() for x in s.split(",") if x.strip()]
    elif isinstance(raw_q, str) and raw_q.strip():
        s = raw_q.strip()
        if s.startswith("[") and s.endswith("]"):
            try:
                parsed = json.loads(s)
                if isinstance(parsed, list):
                    q_ids = [str(x).strip() for x in parsed if str(x).strip()]
            except Exception:
                pass
        if not q_ids:
            q_ids = [x.strip() for x in s.split(",") if x.strip()]

    if not q_ids:
        return []

    # 3. Resolve questions preserving order
    res = []
    for qid in q_ids:
        q_data = questions_bank.get(qid)
        if not q_data:
            # Preservation of integrity: missing question yields empty list (prevents corrupt quiz)
            return []

        options = q_data.get("options")
        if not (isinstance(options, list) and len(options) > 0):
            options = [
                {"id": "A", "text": q_data.get("option_a", "")},
                {"id": "B", "text": q_data.get("option_b", "")},
                {"id": "C", "text": q_data.get("option_c", "")},
                {"id": "D", "text": q_data.get("option_d", "")},
            ]

        res.append({
            "id": q_data["id"],
            "code": q_data.get("code"),
            "chapter": q_data.get("chapter", ""),
            "topic": q_data.get("topic", ""),
            "prompt": q_data["prompt"],
            "options": options,
            "correctOption": (q_data.get("correct_option") or q_data.get("correctOption") or "A").upper(),
            "explanation": q_data.get("explanation", ""),
            "difficulty": q_data.get("difficulty", "MEDIUM"),
            "points": int(q_data.get("points", 10)),
            "imageUrl": q_data.get("image_url") or q_data.get("imageUrl"),
        })

    return res


def test_translate_legacy_quiz_phy10_basic_shape():
    """Sanitized fixture matching failing legacy quiz_phy10_basic proves accepted target shape."""
    # Sanitized legacy questions as staged in staging.durable_rows where table_name = 'questions'
    questions_bank = {
        "q_phy10_01": {
            "id": "q_phy10_01",
            "code": "PHY10_CH1_Q01",
            "chapter": "Chương 1: Mở đầu & Động học",
            "topic": "Chuyển động thẳng đều",
            "prompt": "Phương trình chuyển động x = 5 + 6t. Vận tốc là:",
            "option_a": "5 m/s",
            "option_b": "6 m/s",
            "option_c": "-5 m/s",
            "option_d": "11 m/s",
            "correct_option": "B",
            "explanation": "Vận tốc v = 6 m/s.",
            "difficulty": "EASY",
            "points": 10,
            "image_url": None,
        },
        "q_phy10_02": {
            "id": "q_phy10_02",
            "code": "PHY10_CH1_Q02",
            "chapter": "Chương 1: Mở đầu & Động học",
            "topic": "Chuyển động thẳng biến đổi đều",
            "prompt": "Công thức độc lập thời gian:",
            "option_a": "v^2 - v0^2 = 2as",
            "option_b": "v - v0 = 2as",
            "option_c": "v^2 + v0^2 = 2as",
            "option_d": "v^2 - v0^2 = as",
            "correct_option": "A",
            "explanation": "Công thức là v^2 - v0^2 = 2as.",
            "difficulty": "EASY",
            "points": 10,
            "image_url": None,
        },
        "q_phy10_03": {
            "id": "q_phy10_03",
            "code": "PHY10_CH2_Q03",
            "chapter": "Chương 2: Động lực học",
            "topic": "Định luật II Newton",
            "prompt": "F = 10 N, m = 2 kg. Gia tốc là:",
            "option_a": "5 m/s^2",
            "option_b": "20 m/s^2",
            "option_c": "0.2 m/s^2",
            "option_d": "8 m/s^2",
            "correct_option": "A",
            "explanation": "a = F/m = 10/2 = 5 m/s^2.",
            "difficulty": "MEDIUM",
            "points": 10,
            "image_url": None,
        },
        "q_phy10_04": {
            "id": "q_phy10_04",
            "code": "PHY10_CH1_Q04",
            "chapter": "Chương 1: Mở đầu & Động học",
            "topic": "Sự rơi tự do",
            "prompt": "Rơi tự do là chuyển động:",
            "option_a": "Thẳng đều",
            "option_b": "Thẳng nhanh dần đều với gia tốc g",
            "option_c": "Thẳng chậm dần đều",
            "option_d": "Cong đều",
            "correct_option": "B",
            "explanation": "Rơi tự do có gia tốc g.",
            "difficulty": "EASY",
            "points": 10,
            "image_url": None,
        },
        "q_phy10_05": {
            "id": "q_phy10_05",
            "code": "PHY10_CH2_Q05",
            "chapter": "Chương 2: Động lực học",
            "topic": "Tổng hợp lực",
            "prompt": "F1 = 3 N, F2 = 4 N vuông góc. Hợp lực là:",
            "option_a": "1 N",
            "option_b": "7 N",
            "option_c": "5 N",
            "option_d": "12 N",
            "correct_option": "C",
            "explanation": "F = sqrt(3^2 + 4^2) = 5 N.",
            "difficulty": "MEDIUM",
            "points": 10,
            "image_url": None,
        },
    }

    # Staged legacy quiz payload matching the live failure
    # Notice payload has stringified JSON array in question_ids, questions is None/empty
    legacy_payload = {
        "id": "quiz_phy10_basic",
        "title": "Bộ đề ôn tập Vật lý 10 - Động học & Động lực học",
        "subject_id": "physics",
        "grade": 10,
        "chapter": "Tổng hợp",
        "topic": "Tổng hợp Lớp 10",
        "difficulty": "MEDIUM",
        "question_ids": '["q_phy10_01", "q_phy10_02", "q_phy10_03", "q_phy10_04", "q_phy10_05"]',
        "status": "PUBLISHED",
    }

    translated = translate_legacy_quiz_questions_py(legacy_payload, questions_bank)

    # 1. Non-empty array satisfying check(jsonb_typeof(questions)='array' and jsonb_array_length(questions)>0)
    assert isinstance(translated, list)
    assert len(translated) == 5, f"Expected 5 questions, got {len(translated)}"

    # 2. Exact preservation of IDs and deterministic ordering
    expected_order = ["q_phy10_01", "q_phy10_02", "q_phy10_03", "q_phy10_04", "q_phy10_05"]
    assert [q["id"] for q in translated] == expected_order

    # 3. Shape validation matching target mentor_library_quizzes / mentor_library_view contracts
    for idx, q in enumerate(translated):
        orig = questions_bank[expected_order[idx]]
        assert q["id"] == orig["id"]
        assert q["code"] == orig["code"]
        assert q["prompt"] == orig["prompt"]
        assert q["explanation"] == orig["explanation"]
        assert q["correctOption"] == orig["correct_option"]
        assert q["points"] == orig["points"]
        assert q["difficulty"] == orig["difficulty"]
        assert q["chapter"] == orig["chapter"]
        assert q["topic"] == orig["topic"]

        # Options must be a 4-element array of {id, text}
        assert isinstance(q["options"], list)
        assert len(q["options"]) == 4
        assert [o["id"] for o in q["options"]] == ["A", "B", "C", "D"]
        assert q["options"][0]["text"] == orig["option_a"]
        assert q["options"][1]["text"] == orig["option_b"]
        assert q["options"][2]["text"] == orig["option_c"]
        assert q["options"][3]["text"] == orig["option_d"]


def test_translate_legacy_quiz_comma_separated_and_edge_shapes():
    """Validates comma-separated IDs, pass-through objects, and unresolvable quiz handling."""
    questions_bank = {
        "q-01": {"id": "q-01", "prompt": "P1", "option_a": "A1", "option_b": "B1", "option_c": "C1", "option_d": "D1", "correct_option": "A"},
        "q-02": {"id": "q-02", "prompt": "P2", "option_a": "A2", "option_b": "B2", "option_c": "C2", "option_d": "D2", "correct_option": "B"},
    }

    # Shape 1: Comma-separated question_ids (as in mock test fixtures)
    payload_csv = {"id": "quiz-math-10-01", "question_ids": "q-01, q-02"}
    trans_csv = translate_legacy_quiz_questions_py(payload_csv, questions_bank)
    assert len(trans_csv) == 2
    assert [q["id"] for q in trans_csv] == ["q-01", "q-02"]
    assert trans_csv[0]["options"][0]["text"] == "A1"

    # Shape 2: Already structured questions array pass-through
    already_structured = [
        {"id": "q-custom-1", "prompt": "Custom?", "options": [{"id": "A", "text": "OptA"}], "correctOption": "A"}
    ]
    payload_structured = {"id": "quiz-custom", "questions": already_structured}
    trans_struct = translate_legacy_quiz_questions_py(payload_structured, questions_bank)
    assert trans_struct == already_structured

    # Shape 3: Native JSON array in question_ids
    payload_native = {"id": "quiz-arr", "question_ids": ["q-02", "q-01"]}
    trans_native = translate_legacy_quiz_questions_py(payload_native, questions_bank)
    assert len(trans_native) == 2
    assert [q["id"] for q in trans_native] == ["q-02", "q-01"], "Must preserve exact order"

    # Shape 4: Unresolvable question ID returns empty list (preventing corrupt quiz insertion)
    payload_missing = {"id": "quiz-bad", "question_ids": "q-01,q-missing-99"}
    trans_missing = translate_legacy_quiz_questions_py(payload_missing, questions_bank)
    assert trans_missing == [], "Unresolvable question ID must return empty array to block corrupt quiz"

    # Shape 5: Empty payload returns empty list
    assert translate_legacy_quiz_questions_py({}, questions_bank) == []
    assert translate_legacy_quiz_questions_py({"id": "empty", "question_ids": ""}, questions_bank) == []


def test_migration_202611010004_quiz_translation_and_constraint_guards():
    """Validates that migration 0004 incorporates deterministic quiz translation, search_path, revokes, and CHECK guards."""
    repo_root = Path(__file__).resolve().parents[2]
    mig_path = repo_root / "supabase" / "migrations" / "202611010004_claim_staging_progress.sql"
    content = mig_path.read_text(encoding="utf-8")

    # 1. Translation function definition & security invariants
    assert "create or replace function staging.translate_legacy_quiz_questions" in content
    assert "revoke all on function staging.translate_legacy_quiz_questions(jsonb) from public, anon, authenticated;" in content

    # 2. mentor_library_quizzes seeding uses translation and enforces non-empty array
    assert "staging.translate_legacy_quiz_questions(payload)" in content
    assert "jsonb_typeof(staging.translate_legacy_quiz_questions(payload)) = 'array'" in content
    assert "jsonb_array_length(staging.translate_legacy_quiz_questions(payload)) > 0" in content
    assert "questions = excluded.questions" in content

    # 3. No unsafe '[]'::jsonb fallback for mentor_library_quizzes questions
    lines = content.splitlines()
    for idx, l in enumerate(lines):
        if "into public.mentor_library_quizzes" in l:
            # Check the next 30 lines for '[]'::jsonb
            block = "\n".join(lines[idx:idx + 30])
            assert "'[]'::jsonb" not in block, "Found unsafe '[]'::jsonb fallback in mentor_library_quizzes insert"

    # 4. mentor_exams has range and enum guards
    assert "(payload->>'grade')::int between 10 and 12" in content
    assert "upper(coalesce(payload->>'exam_type', '')) = 'FINAL'" in content
    assert "'MIDTERM' end" in content
    assert "(payload->>'semester')::int in (1, 2)" in content
    assert "(payload->>'duration_minutes')::int between 5 and 300" in content

    # 5. mentor_practice_sessions has step, total_count alter guards and fallback
    assert "alter table public.mentor_practice_sessions add column if not exists step int not null default 0;" in content
    assert "alter table public.mentor_practice_sessions add column if not exists total_count int not null default 0;" in content
    assert "exists (select 1 from public.mentor_library_quizzes where id = v_row.payload->>'quiz_id')" in content

    # 6. mentor_wrong_questions has snapshot resolution so question_snapshot is never null
    assert "v_wq_snapshot" in content
    assert "v_wq_state" in content
    assert "v_wq_grade" in content

    # 7. staging.reconcile_staging_cutover checks unresolved quizzes
    assert "v_unresolved_quizzes" in content
    assert "'unresolvedQuizzes'" in content


def find_invalid_key_colon_syntax(sql_text: str) -> List[Tuple[int, str, str]]:
    """Scans SQL/PLpgSQL source for invalid key-colon syntax (comments excluded).

    Catches defects like:
      'key': value
      'key' : value
      key: value (outside array subscript [...])

    Correctly excludes:
      - Line comments (-- ...)
      - Block comments (/* ... */)
      - Colons inside string literals ('foo:bar', 'http://...')
      - Valid typecasts (::)
      - Valid assignments (:=)
      - Valid array slice subscripts ([lower:upper])
      - Valid bind parameters (:param)

    Returns a list of tuples: (line_number, offending_token, description)
    """
    errors: List[Tuple[int, str, str]] = []
    i = 0
    n = len(sql_text)
    line = 1
    bracket_depth = 0

    last_token_type = None  # 'STRING', 'IDENT', 'NUMBER', 'OPERATOR', 'PUNCT'
    last_token_text = ""
    last_token_line = 1

    while i < n:
        ch = sql_text[i]

        # Newline
        if ch == '\n':
            line += 1
            i += 1
            continue

        # Single-line comment: -- ...
        if ch == '-' and i + 1 < n and sql_text[i + 1] == '-':
            i += 2
            while i < n and sql_text[i] != '\n':
                i += 1
            continue

        # Multi-line comment: /* ... */
        if ch == '/' and i + 1 < n and sql_text[i + 1] == '*':
            i += 2
            while i + 1 < n and not (sql_text[i] == '*' and sql_text[i + 1] == '/'):
                if sql_text[i] == '\n':
                    line += 1
                i += 1
            if i + 1 < n:
                i += 2
            continue

        # Whitespace
        if ch in (' ', '\t', '\r'):
            i += 1
            continue

        # String literal: '...' with '' escape
        if ch == "'":
            start_line = line
            i += 1
            str_content = []
            while i < n:
                if sql_text[i] == '\n':
                    line += 1
                if sql_text[i] == "'":
                    if i + 1 < n and sql_text[i + 1] == "'":
                        str_content.append("'")
                        i += 2
                        continue
                    else:
                        i += 1
                        break
                else:
                    str_content.append(sql_text[i])
                    i += 1
            last_token_type = 'STRING'
            last_token_text = "'" + "".join(str_content) + "'"
            last_token_line = start_line
            continue

        # Brackets for array indexing [ ... ]
        if ch == '[':
            bracket_depth += 1
            last_token_type = 'PUNCT'
            last_token_text = '['
            last_token_line = line
            i += 1
            continue
        elif ch == ']':
            bracket_depth = max(0, bracket_depth - 1)
            last_token_type = 'PUNCT'
            last_token_text = ']'
            last_token_line = line
            i += 1
            continue

        # Colon handling
        if ch == ':':
            # Check :: (typecast)
            if i + 1 < n and sql_text[i + 1] == ':':
                i += 2
                last_token_type = 'OPERATOR'
                last_token_text = '::'
                last_token_line = line
                continue
            # Check := (assignment)
            if i + 1 < n and sql_text[i + 1] == '=':
                i += 2
                last_token_type = 'OPERATOR'
                last_token_text = ':='
                last_token_line = line
                continue
            # Check array slice [start:end]
            if bracket_depth > 0:
                i += 1
                last_token_type = 'OPERATOR'
                last_token_text = ':'
                last_token_line = line
                continue

            # If we reach here, single colon ':' outside brackets and not '::' or ':='
            colon_line = line
            snippet_start = max(0, i - 25)
            snippet_end = min(n, i + 25)
            snippet = sql_text[snippet_start:snippet_end].replace('\n', ' ')

            if last_token_type == 'STRING':
                errors.append((
                    colon_line,
                    f"{last_token_text}:",
                    f"Invalid key-colon syntax after string literal at line {colon_line}: {snippet.strip()}"
                ))
            elif last_token_type == 'IDENT':
                errors.append((
                    colon_line,
                    f"{last_token_text}:",
                    f"Invalid key-colon syntax after identifier at line {colon_line}: {snippet.strip()}"
                ))
            else:
                # Check for bind parameter :identifier
                if i + 1 < n and (sql_text[i + 1].isalpha() or sql_text[i + 1] == '_'):
                    # bind parameter like :param, skip identifier
                    pass
                else:
                    errors.append((
                        colon_line,
                        ":",
                        f"Unexpected stray colon syntax at line {colon_line}: {snippet.strip()}"
                    ))

            i += 1
            last_token_type = 'OPERATOR'
            last_token_text = ':'
            last_token_line = colon_line
            continue

        # Identifier or keyword
        if ch.isalpha() or ch == '_':
            start_i = i
            start_line = line
            while i < n and (sql_text[i].isalnum() or sql_text[i] == '_'):
                i += 1
            ident = sql_text[start_i:i]
            last_token_type = 'IDENT'
            last_token_text = ident
            last_token_line = start_line
            continue

        # Number
        if ch.isdigit():
            start_i = i
            start_line = line
            while i < n and (sql_text[i].isdigit() or sql_text[i] == '.'):
                i += 1
            last_token_type = 'NUMBER'
            last_token_text = sql_text[start_i:i]
            last_token_line = start_line
            continue

        # Other punctuation ($ , ( ) ; + - * / = < > etc.)
        last_token_type = 'PUNCT'
        last_token_text = ch
        last_token_line = line
        i += 1

    return errors


def test_migration_sql_no_invalid_key_colon_syntax():
    """Validates that all numbered SQL migration files have zero invalid key-colon syntax in executable SQL."""
    repo_root = Path(__file__).resolve().parents[2]
    migrations_dir = repo_root / "supabase" / "migrations"
    migration_files = sorted(migrations_dir.glob("*.sql"))
    assert len(migration_files) >= 4, f"Expected at least 4 migration files in {migrations_dir}"

    for mig in migration_files:
        content = mig.read_text(encoding="utf-8")
        errors = find_invalid_key_colon_syntax(content)
        assert not errors, (
            f"Found {len(errors)} invalid key-colon syntax errors in {mig.name}:\n"
            + "\n".join(f"  Line {err[0]}: {err[2]}" for err in errors)
        )


def test_sql_key_colon_detector_catches_regression():
    """Regression test ensuring the scanner reliably catches the rejected 004 syntax defect and permits valid syntax."""
    # 1. Exact buggy snippet rejected by Postgres with ERROR 42601 syntax error at or near ':'
    buggy_snippet = """
    return jsonb_build_object(
      'mode', 'ADMIN_DRY_RUN_RECONCILIATION',
      'cutoverComplete', v_cutover_complete,
      'status', v_status,
      'stagingInventory', jsonb_build_object(
        'durableRowsTotal', v_total_rows,
        'tableCounts', v_table_counts
      ),
      'accountReconciliation': jsonb_build_object(
        'totalAccounts', v_claimable_count + v_test_count,
        'claimableAccountsTotal', v_claimable_count
      ),
      'unresolvedMappings': v_unresolved_claimable,
      'unresolvedQuizzes': v_unresolved_quizzes,
      'archivedTestAccounts': v_archived_test,
      'securityPolicy': jsonb_build_object(
        'piiPrinted', false
      )
    );
    """
    buggy_errors = find_invalid_key_colon_syntax(buggy_snippet)
    assert len(buggy_errors) == 5, f"Expected exactly 5 errors in buggy snippet, found {len(buggy_errors)}: {buggy_errors}"
    detected_keys = [err[1] for err in buggy_errors]
    assert "'accountReconciliation':" in detected_keys
    assert "'unresolvedMappings':" in detected_keys
    assert "'unresolvedQuizzes':" in detected_keys
    assert "'archivedTestAccounts':" in detected_keys
    assert "'securityPolicy':" in detected_keys

    # 2. Fixed snippet with comma separators
    fixed_snippet = """
    return jsonb_build_object(
      'mode', 'ADMIN_DRY_RUN_RECONCILIATION',
      'cutoverComplete', v_cutover_complete,
      'status', v_status,
      'stagingInventory', jsonb_build_object(
        'durableRowsTotal', v_total_rows,
        'tableCounts', v_table_counts
      ),
      'accountReconciliation', jsonb_build_object(
        'totalAccounts', v_claimable_count + v_test_count,
        'claimableAccountsTotal', v_claimable_count
      ),
      'unresolvedMappings', v_unresolved_claimable,
      'unresolvedQuizzes', v_unresolved_quizzes,
      'archivedTestAccounts', v_archived_test,
      'securityPolicy', jsonb_build_object(
        'piiPrinted', false
      )
    );
    """
    assert find_invalid_key_colon_syntax(fixed_snippet) == []

    # 3. Unquoted key: val
    unquoted_buggy = "return jsonb_build_object(accountReconciliation: jsonb_build_object('a', 1));"
    unquoted_errors = find_invalid_key_colon_syntax(unquoted_buggy)
    assert len(unquoted_errors) == 1
    assert "accountReconciliation:" in unquoted_errors[0][1]

    # 4. Valid colons: comments, strings, typecasts, assignments, array slices
    valid_sql = """
    -- This is a comment with key: value and 'accountReconciliation': jsonb_build_object()
    /* Multi-line comment:
       'unresolvedMappings': v_unresolved_claimable
    */
    declare
      v_uid uuid := auth.uid();
      v_text text := 'https://example.com:8080/path';
      v_arr int[] := array[1, 2, 3];
      v_slice int := v_arr[1:2];
      v_json jsonb := '{"key": "value"}'::jsonb;
      v_hash text := md5('enr:' || v_uid::text);
    begin
      select count(*) into v_uid from public.mentor_profiles where id = v_uid;
      return jsonb_build_object('status', 'OK', 'id', v_uid);
    end;
    """
    assert find_invalid_key_colon_syntax(valid_sql) == []


def test_reconcile_staging_cutover_exact_syntax_and_json_structure():
    """Validates lines in 202611010004 for staging.reconcile_staging_cutover specifically."""
    repo_root = Path(__file__).resolve().parents[2]
    mig_path = repo_root / "supabase" / "migrations" / "202611010004_claim_staging_progress.sql"
    content = mig_path.read_text(encoding="utf-8")

    # Assert comma separators are used in reconcile_staging_cutover return object
    assert "'accountReconciliation', jsonb_build_object(" in content
    assert "'unresolvedMappings', v_unresolved_claimable," in content
    assert "'unresolvedQuizzes', v_unresolved_quizzes," in content
    assert "'archivedTestAccounts', v_archived_test," in content
    assert "'securityPolicy', jsonb_build_object(" in content

    # Assert no colon separators after keys
    assert "'accountReconciliation':" not in content
    assert "'unresolvedMappings':" not in content
    assert "'unresolvedQuizzes':" not in content
    assert "'archivedTestAccounts':" not in content
    assert "'securityPolicy':" not in content

    # Assert exact function definition signature and exact revoke signature
    assert "create or replace function staging.reconcile_staging_cutover(p_expected_durable_rows int default null)" in content
    assert "revoke all on function staging.reconcile_staging_cutover(int) from public, anon, authenticated;" in content
    assert "revoke all on function staging.reconcile_staging_cutover()" not in content


def normalize_pg_type(t: str) -> str:
    """Normalizes PostgreSQL data types to canonical form for signature comparison."""
    t = t.strip().lower()
    mapping = {
        "int": "int",
        "integer": "int",
        "int4": "int",
        "bigint": "bigint",
        "int8": "bigint",
        "smallint": "smallint",
        "int2": "smallint",
        "boolean": "boolean",
        "bool": "boolean",
        "jsonb": "jsonb",
        "json": "json",
        "text": "text",
        "varchar": "text",
        "character varying": "text",
        "timestamptz": "timestamptz",
        "timestamp with time zone": "timestamptz",
        "uuid": "uuid",
        "numeric": "numeric",
    }
    return mapping.get(t, t)


def parse_function_parameter_types(params_str: str) -> List[str]:
    """Parses parameter list string from CREATE FUNCTION or GRANT/REVOKE into normalized type names."""
    params_str = params_str.strip()
    if not params_str:
        return []

    types = []
    raw_params = [p.strip() for p in params_str.split(",") if p.strip()]
    for p in raw_params:
        # Strip default clause if present: e.g. "p_expected_durable_rows int default null"
        p_no_default = re.split(r"\s+default\s+", p, flags=re.IGNORECASE)[0].strip()
        lower_p = p_no_default.lower()
        if "timestamp with time zone" in lower_p:
            types.append("timestamptz")
        elif "character varying" in lower_p:
            types.append("text")
        else:
            tokens = p_no_default.split()
            if tokens:
                types.append(normalize_pg_type(tokens[-1]))
    return types


def extract_sql_function_declarations(sql_text: str) -> Dict[str, List[List[str]]]:
    """Extracts all CREATE [OR REPLACE] FUNCTION declarations and returns:
    { "schema.func_name": [ [param_type1, ...], ... ] }
    """
    pattern = re.compile(
        r"create\s+(?:or\s+replace\s+)?function\s+([a-zA-Z0-9_.]+)\s*\((.*?)\)",
        re.IGNORECASE | re.DOTALL,
    )
    funcs: Dict[str, List[List[str]]] = {}
    for m in pattern.finditer(sql_text):
        func_name = m.group(1).lower()
        args_str = m.group(2)
        param_types = parse_function_parameter_types(args_str)
        funcs.setdefault(func_name, []).append(param_types)
    return funcs


def extract_sql_function_permissions(sql_text: str) -> List[Dict[str, Any]]:
    """Extracts all GRANT/REVOKE statements targeting functions."""
    pattern = re.compile(
        r"(grant|revoke)\s+(.*?)\s+on\s+function\s+([a-zA-Z0-9_.]+)\s*\((.*?)\)\s+(to|from)\s+([^;]+);",
        re.IGNORECASE | re.DOTALL,
    )
    perms = []
    for m in pattern.finditer(sql_text):
        action = m.group(1).upper()
        privilege = m.group(2).strip()
        func_name = m.group(3).lower()
        args_str = m.group(4)
        direction = m.group(5).lower()
        grantees = [g.strip().lower() for g in m.group(6).split(",") if g.strip()]
        param_types = parse_function_parameter_types(args_str)
        perms.append({
            "action": action,
            "privilege": privilege,
            "function": func_name,
            "args_str": args_str.strip(),
            "param_types": param_types,
            "direction": direction,
            "grantees": grantees,
        })
    return perms


def validate_migration_function_signatures(
    sql_text: str,
    declared_catalog: Optional[Dict[str, List[List[str]]]] = None,
    require_all_declared_have_perms: bool = False,
) -> List[str]:
    """Validates that:
    1. For every function declared in this migration (or catalog), every GRANT/REVOKE targets an EXACT defined signature.
    2. No GRANT/REVOKE refers to a nonexistent parameter overload (e.g. () when only (int) was created).
    3. If require_all_declared_have_perms is True, every declared function has its permissions explicitly managed.
    Returns a list of error descriptions (empty if valid).
    """
    errors = []
    declared_funcs = dict(declared_catalog or {})
    local_declared = extract_sql_function_declarations(sql_text)
    for fname, overloads in local_declared.items():
        declared_funcs.setdefault(fname, []).extend(overloads)
    perms = extract_sql_function_permissions(sql_text)

    # 1. Verify every GRANT/REVOKE on a declared function targets an actual declared signature
    for p in perms:
        fname = p["function"]
        ptypes = p["param_types"]
        if fname in declared_funcs:
            overloads = declared_funcs[fname]
            if ptypes not in overloads:
                errors.append(
                    f"{p['action']} on function '{fname}({p['args_str']})' refers to nonexistent overload {ptypes}! "
                    f"Declared overloads for '{fname}': {overloads}"
                )

    # 2. Optionally verify every declared function has explicit permissions
    if require_all_declared_have_perms:
        for fname, overloads in local_declared.items():
            for ov in overloads:
                matching_perms = [
                    p for p in perms
                    if p["function"] == fname and p["param_types"] == ov
                ]
                if not matching_perms:
                    errors.append(
                        f"Declared function '{fname}' with signature {ov} has no explicit GRANT or REVOKE statement matching exact identity"
                    )

    return errors


def test_migration_sql_function_signature_pairs_exact_identity():
    """Validates that all migration files have 100% exact matches between CREATE FUNCTION and GRANT/REVOKE signatures."""
    repo_root = Path(__file__).resolve().parents[2]
    migrations_dir = repo_root / "supabase" / "migrations"
    migration_files = sorted(migrations_dir.glob("*.sql"))
    assert len(migration_files) >= 4, f"Expected at least 4 migration files in {migrations_dir}"

    for mig in migration_files:
        content = mig.read_text(encoding="utf-8")
        errors = validate_migration_function_signatures(content)
        assert not errors, (
            f"Found {len(errors)} function signature mismatch errors in {mig.name}:\n"
            + "\n".join(f"  - {err}" for err in errors)
        )

    # Targeted check for 202611010004 requiring all functions to have exact permissions
    mig_004 = (migrations_dir / "202611010004_claim_staging_progress.sql").read_text(encoding="utf-8")
    errors_004 = validate_migration_function_signatures(mig_004, require_all_declared_have_perms=True)
    assert not errors_004, (
        f"Found {len(errors_004)} permission errors in 202611010004_claim_staging_progress.sql:\n"
        + "\n".join(f"  - {err}" for err in errors_004)
    )

    funcs_004 = extract_sql_function_declarations(mig_004)
    perms_004 = extract_sql_function_permissions(mig_004)

    # Assert exactly 4 functions declared in 004
    assert set(funcs_004.keys()) == {
        "public.mentor_friendships_enforce_immutable_endpoints",
        "staging.translate_legacy_quiz_questions",
        "public.mentor_claim_staged_progress",
        "staging.reconcile_staging_cutover",
    }
    assert funcs_004["staging.reconcile_staging_cutover"] == [["int"]]
    assert funcs_004["staging.translate_legacy_quiz_questions"] == [["jsonb"]]
    assert funcs_004["public.mentor_claim_staged_progress"] == [[]]
    assert funcs_004["public.mentor_friendships_enforce_immutable_endpoints"] == [[]]

    # Assert reconcile_staging_cutover has exact (int) revoke and strict denial for public, anon, authenticated
    reconcile_perms = [p for p in perms_004 if p["function"] == "staging.reconcile_staging_cutover"]
    assert len(reconcile_perms) == 1
    rec_perm = reconcile_perms[0]
    assert rec_perm["action"] == "REVOKE"
    assert rec_perm["param_types"] == ["int"]
    assert "public" in rec_perm["grantees"]
    assert "anon" in rec_perm["grantees"]
    assert "authenticated" in rec_perm["grantees"]

    # Assert no bogus () overload revoke exists anywhere
    assert not any(
        p["function"] == "staging.reconcile_staging_cutover" and p["param_types"] == []
        for p in perms_004
    )


def test_function_signature_validator_catches_invalid_overload_regression():
    """Regression test ensuring the signature validator catches the exact rejected Attempt 7 defect."""
    # 1. Exact buggy snippet rejected by Postgres with ERROR 42883 function staging.reconcile_staging_cutover() does not exist
    buggy_snippet = """
    create or replace function staging.reconcile_staging_cutover(p_expected_durable_rows int default null)
    returns jsonb language plpgsql as $$ begin return '{}'::jsonb; end; $$;

    revoke all on function staging.reconcile_staging_cutover(int) from public, anon, authenticated;
    revoke all on function staging.reconcile_staging_cutover() from public, anon, authenticated;
    """
    errors = validate_migration_function_signatures(buggy_snippet)
    assert len(errors) == 1, f"Expected exactly 1 error, got {len(errors)}: {errors}"
    assert "staging.reconcile_staging_cutover()" in errors[0]
    assert "nonexistent overload []" in errors[0]

    # 2. Wrong parameter type (e.g. text instead of int)
    type_mismatch_snippet = """
    create or replace function staging.reconcile_staging_cutover(p_expected_durable_rows int default null)
    returns jsonb language plpgsql as $$ begin return '{}'::jsonb; end; $$;

    revoke all on function staging.reconcile_staging_cutover(text) from public, anon, authenticated;
    """
    errs_type = validate_migration_function_signatures(type_mismatch_snippet)
    assert any("nonexistent overload ['text']" in e for e in errs_type)

    # 3. Nonexistent overload on declared function
    overload_mismatch_snippet = """
    create or replace function public.example_func(x text) returns void language plpgsql as $$ begin end; $$;
    grant execute on function public.example_func() to authenticated;
    """
    errs_overload = validate_migration_function_signatures(overload_mismatch_snippet)
    assert any("refers to nonexistent overload []" in e for e in errs_overload)

    # 4. Declared function with missing permissions
    missing_perm_snippet = """
    create or replace function public.forgotten_function() returns void language plpgsql as $$ begin end; $$;
    """
    errs_missing = validate_migration_function_signatures(missing_perm_snippet, require_all_declared_have_perms=True)
    assert any("has no explicit GRANT or REVOKE" in e for e in errs_missing)

    # 5. Fixed snippet passes with zero errors
    fixed_snippet = """
    create or replace function staging.reconcile_staging_cutover(p_expected_durable_rows int default null)
    returns jsonb language plpgsql as $$ begin return '{}'::jsonb; end; $$;

    revoke all on function staging.reconcile_staging_cutover(int) from public, anon, authenticated;
    """
    assert validate_migration_function_signatures(fixed_snippet, require_all_declared_have_perms=True) == []


def test_sql_parser_syntax_and_protections_verification():
    """Verifies SQL parser syntax across migrations and protects quiz mapping and RLS corrections."""
    repo_root = Path(__file__).resolve().parents[2]
    migrations_dir = repo_root / "supabase" / "migrations"
    migration_files = sorted(migrations_dir.glob("*.sql"))

    # If sqlparse is available, parse every migration file to ensure clean AST generation
    try:
        import sqlparse
        for mig in migration_files:
            content = mig.read_text(encoding="utf-8")
            parsed = sqlparse.parse(content)
            assert len(parsed) > 0, f"sqlparse produced empty AST for {mig.name}"
    except ImportError:
        pass

    # Protect all previous quiz mapping and RLS corrections in 004
    mig_004 = (migrations_dir / "202611010004_claim_staging_progress.sql").read_text(encoding="utf-8")

    # 1. Deterministic quiz translation preserved
    assert "create or replace function staging.translate_legacy_quiz_questions" in mig_004
    assert "staging.translate_legacy_quiz_questions(payload)" in mig_004
    assert "jsonb_typeof(staging.translate_legacy_quiz_questions(payload)) = 'array'" in mig_004

    # 2. Friendship RLS protections preserved
    assert "grant update (state, revision, updated_at) on public.mentor_friendships to authenticated;" in mig_004
    assert "create policy mentor_friendships_select" in mig_004
    assert "create policy mentor_friendships_insert" in mig_004
    assert "create policy mentor_friendships_update" in mig_004
    assert "create policy mentor_friendships_delete" in mig_004
    assert "create trigger trg_mentor_friendships_immutable_endpoints" in mig_004

    # 3. Practice session alters and guards preserved
    assert "alter table public.mentor_practice_sessions add column if not exists step" in mig_004
    assert "alter table public.mentor_practice_sessions add column if not exists total_count" in mig_004

    # 4. Wrong questions snapshot resolution preserved
    assert "v_wq_snapshot" in mig_004
    assert "unique(user_id, question_id)" in mig_004

    # 5. Clean transaction wrapping
    assert mig_004.startswith("begin;")
    assert mig_004.strip().endswith("commit;")


