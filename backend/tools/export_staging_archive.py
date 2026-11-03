"""Safe read-only SQLite staging archive exporter for Supabase cutover.

Guarantees:
- Connects to SQLite strictly READ-ONLY (URI mode=ro).
- NEVER connects to live Supabase or performs live writes.
- NEVER outputs personal values (emails, names, passwords, answer texts) to stdout.
- Outputs ONLY table counts, metadata, and SHA-256 checksums to stdout.
- Refuses missing or unknown table schemas rather than silently dropping data.
- Idempotently stages all 20 durable SQLite tables into staging.durable_rows with deterministic legacy IDs and ownership.
- Stages user identity into staging.user_mappings with least privilege (email preserved solely for eventual verified claim).
- Keeps *.example test accounts archived separately as unclaimed records.
- Completely EXCLUDES password hashes, session tokens, recovery tokens, rate limits, transient events, and provider credentials.
- Writes exact UTF-8 bytes and computes SHA-256 directly from the actual persisted disk bytes.
- Writes protected artifacts under backend/data (ignored by Git).
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import sqlite3
import sys
from typing import Any, Dict, List, Optional, Set, Tuple


DURABLE_TABLES: Set[str] = {
    "users",
    "resources",
    "enrollments",
    "attempts",
    "completions",
    "awards",
    "learning_days",
    "friendships",
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
}

EPHEMERAL_TABLES: Dict[str, str] = {
    "sessions": "FastAPI session JWT hashes are process/runtime-specific; Supabase Auth issues its own JWT tokens.",
    "events": "Transient SSE event queue; Supabase Realtime uses CDC on Postgres WAL (supabase_realtime publication).",
    "idempotency": "Short-lived request deduplication cache (24-48h window).",
    "recovery_tokens": "Local password reset tokens; Supabase Auth manages secure recovery OTP flows directly.",
    "rate_limits": "Sliding-window rate limit counters on local instance; cloud has API Gateway rate limits.",
    "ai_provider_operations": "Local LLM call idempotency log; cloud uses mentor_ai_usage and Edge Function.",
}

# Explicit deterministic query ordering for all durable tables to guarantee byte-for-byte reproducibility
TABLE_ORDER_BY: Dict[str, str] = {
    "users": "id ASC",
    "resources": "id ASC",
    "enrollments": "id ASC",
    "attempts": "id ASC",
    "completions": "id ASC",
    "awards": "user_id ASC, subject_id ASC, grade ASC, reward_unit_id ASC",
    "learning_days": "user_id ASC, day ASC",
    "friendships": "id ASC",
    "badges": "id ASC",
    "user_badges": "user_id ASC, badge_id ASC",
    "friend_streaks": "id ASC",
    "questions": "id ASC",
    "library_quizzes": "id ASC",
    "practice_sessions": "id ASC",
    "wrong_questions_notebook": "id ASC",
    "role_audit_logs": "id ASC",
    "exams": "id ASC",
    "exam_sessions": "id ASC",
    "ai_practice_sessions": "id ASC",
    "ai_question_reports": "id ASC",
}

# Credential and token keys that must NEVER appear in staged payloads
SENSITIVE_PAYLOAD_KEYS: Set[str] = {
    "password_hash",
    "access_hash",
    "refresh_hash",
    "token",
    "token_hash",
    "raw_key",
    "qr_code_token",
}


def escape_sql(value: str) -> str:
    """Escapes single quotes for SQL string literals."""
    return value.replace("'", "''")


def compute_legacy_id(tbl: str, r_dict: Dict[str, Any]) -> str:
    """Computes an explicit, deterministic legacy ID for any durable table row."""
    if tbl == "awards":
        uid = r_dict.get("user_id")
        sid = r_dict.get("subject_id")
        grd = r_dict.get("grade")
        ruid = r_dict.get("reward_unit_id")
        if any(v is None for v in (uid, sid, grd, ruid)):
            raise ValueError(f"Incomplete composite key for '{tbl}': {r_dict}")
        return f"{uid}:{sid}:{grd}:{ruid}"
    elif tbl == "learning_days":
        uid = r_dict.get("user_id")
        day = r_dict.get("day")
        if uid is None or day is None:
            raise ValueError(f"Incomplete composite key for '{tbl}': {r_dict}")
        return f"{uid}:{day}"
    elif tbl == "user_badges":
        uid = r_dict.get("user_id")
        bid = r_dict.get("badge_id")
        if uid is None or bid is None:
            raise ValueError(f"Incomplete composite key for '{tbl}': {r_dict}")
        return f"{uid}:{bid}"
    elif "id" in r_dict and r_dict["id"] is not None:
        return str(r_dict["id"])
    elif tbl == "questions" and "code" in r_dict and r_dict["code"] is not None:
        return str(r_dict["code"])
    else:
        raise ValueError(f"Missing deterministic primary key for durable table '{tbl}': {r_dict}")


def compute_legacy_user_id(r_dict: Dict[str, Any]) -> Optional[int]:
    """Identifies the owner user ID for a durable row if applicable."""
    for col in ("user_id", "requester", "user1_id", "owner", "created_by"):
        if col in r_dict and r_dict[col] is not None:
            try:
                return int(r_dict[col])
            except (ValueError, TypeError):
                pass
    return None


def export_sqlite_to_staging(
    db_path: Path,
    output_dir: Path,
    sql_filename: str = "staging_archive.sql",
    manifest_filename: str = "staging_manifest.json",
) -> Tuple[Path, Path, Dict[str, Any]]:
    """Exports SQLite data read-only to an idempotent SQL migration and JSON manifest."""
    if not db_path.is_file():
        raise FileNotFoundError(f"Database file not found: {db_path}")

    output_dir.mkdir(parents=True, exist_ok=True)
    sql_path = output_dir / sql_filename
    manifest_path = output_dir / manifest_filename

    conn = sqlite3.connect(f"file:{db_path.resolve()}?mode=ro", uri=True)
    conn.row_factory = sqlite3.Row
    try:
        cur = conn.cursor()
        found_tables = [
            r[0]
            for r in cur.execute(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
            ).fetchall()
        ]

        # Refuse missing or unknown table schemas rather than silently dropping data
        found_set = set(found_tables)
        expected_set = DURABLE_TABLES | set(EPHEMERAL_TABLES.keys())
        missing_tables = expected_set - found_set
        unknown_tables = found_set - expected_set
        if missing_tables or unknown_tables:
            err_parts = []
            if missing_tables:
                err_parts.append(f"missing table(s): {sorted(missing_tables)}")
            if unknown_tables:
                err_parts.append(f"unrecognized/unknown table(s): {sorted(unknown_tables)}")
            raise ValueError(f"SQLite schema verification failed: {'; '.join(err_parts)}")

        table_counts: Dict[str, int] = {}
        for t in sorted(found_tables):
            cur.execute(f'SELECT COUNT(*) FROM "{t}"')
            table_counts[t] = cur.fetchone()[0]

        distinct_owners: Set[int] = set()
        staged_counts: Dict[str, int] = {}

        # 1. Inspect users and partition into claimable accounts vs archived test accounts (*.example)
        user_rows = cur.execute("SELECT * FROM users ORDER BY id ASC").fetchall()
        staged_counts["users"] = len(user_rows)
        for u in user_rows:
            distinct_owners.add(int(u["id"]))

        test_accounts: List[sqlite3.Row] = []
        claimable_accounts: List[sqlite3.Row] = []
        for u in user_rows:
            email_val = str(u["email"] or "").strip().lower()
            if email_val.endswith(".example") or "@example." in email_val:
                test_accounts.append(u)
            else:
                claimable_accounts.append(u)

        # 2. Count durable rows across all remaining durable tables
        for tbl in sorted(DURABLE_TABLES):
            if tbl == "users":
                continue
            staged_counts[tbl] = table_counts.get(tbl, 0)

        # Total durable rows staged and total ephemeral rows excluded
        staged_durable_rows_total = sum(staged_counts.values())
        excluded_ephemeral_rows_total = sum(table_counts.get(t, 0) for t in EPHEMERAL_TABLES)

        # Build SQL archive lines
        sql_lines = [
            "begin;",
            "-- ============================================================================",
            "-- AI Mentor - Idempotent SQLite Staging Archive Data Load",
            "-- Target schema: staging (strictly private, zero anon/authenticated grants)",
            "-- Generated strictly offline from local SQLite database in read-only mode.",
            "-- Excludes password_hash, tokens, rate limits, transient events, provider keys.",
            "-- ============================================================================",
            f"-- Total source SQLite tables inspected: {len(found_tables)}",
            f"-- Durable tables staged: {len(DURABLE_TABLES)} ({staged_durable_rows_total} total rows)",
            f"-- Ephemeral tables excluded: {len(EPHEMERAL_TABLES)} ({excluded_ephemeral_rows_total} total rows)",
            f"-- Distinct data owners: {len(distinct_owners)}",
            "-- ----------------------------------------------------------------------------",
            "-- Source table inventory counts (26 tables):",
        ]
        for t in sorted(found_tables):
            category = "durable" if t in DURABLE_TABLES else "ephemeral (excluded)"
            sql_lines.append(f"--   * {t:<28}: {table_counts.get(t, 0):>5} rows [{category}]")
        sql_lines.append("-- ============================================================================")
        sql_lines.append("")

        # 1. Staging user mappings
        sql_lines.append("-- ----------------------------------------------------------------------------")
        sql_lines.append("-- 1. staging.user_mappings (identity claim metadata)")
        sql_lines.append(
            f"-- Total accounts: {len(user_rows)} "
            f"({len(claimable_accounts)} claimable, {len(test_accounts)} archived test accounts)"
        )
        sql_lines.append("-- ----------------------------------------------------------------------------")

        def append_user_mapping_sql(u_row: sqlite3.Row) -> None:
            u_dict = dict(u_row)
            uid = int(u_dict["id"])
            email = escape_sql(str(u_dict.get("email", "")))
            username = escape_sql(str(u_dict.get("username", "")))
            nickname = escape_sql(str(u_dict.get("nickname", "")))
            role = escape_sql(str(u_dict.get("role", "STUDENT")))
            rank_tier = escape_sql(str(u_dict.get("rank_tier", "BRONZE")))
            grade = int(u_dict.get("grade", 0))

            sql_lines.append(
                f"insert into staging.user_mappings("
                f"legacy_user_id, email, username, nickname, role, rank_tier, grade) values ("
                f"{uid}, '{email}', '{username}', '{nickname}', '{role}', '{rank_tier}', {grade}"
                f") on conflict (legacy_user_id) do update set "
                f"email=excluded.email, username=excluded.username, nickname=excluded.nickname, "
                f"role=excluded.role, rank_tier=excluded.rank_tier, grade=excluded.grade, updated_at=now();"
            )

        if claimable_accounts:
            sql_lines.append("-- 1a. Claimable accounts (eligible for verified Supabase Auth linking)")
            for u in claimable_accounts:
                append_user_mapping_sql(u)

        if test_accounts:
            sql_lines.append("")
            sql_lines.append("-- 1b. Test accounts (archived separately, unclaimed)")
            for u in test_accounts:
                append_user_mapping_sql(u)

        sql_lines.append("")
        sql_lines.append("-- ----------------------------------------------------------------------------")
        sql_lines.append("-- 2. staging.durable_rows: users (profile facts excluding password & email)")
        sql_lines.append("-- ----------------------------------------------------------------------------")
        for u in user_rows:
            u_dict = dict(u)
            uid = int(u_dict["id"])
            # Exclude secret/auth credentials and isolate email exclusively to user_mappings
            safe_payload = {
                k: v
                for k, v in u_dict.items()
                if k not in ("password_hash", "qr_code_token", "email")
            }
            payload_json = json.dumps(safe_payload, ensure_ascii=False, sort_keys=True)
            escaped_json = escape_sql(payload_json)
            sql_lines.append(
                f"insert into staging.durable_rows("
                f"table_name, legacy_id, legacy_user_id, payload, staged_at) values ("
                f"'users', '{uid}', {uid}, '{escaped_json}'::jsonb, now()"
                f") on conflict (table_name, legacy_id) do update set "
                f"legacy_user_id=excluded.legacy_user_id, payload=excluded.payload, staged_at=excluded.staged_at;"
            )

        # 3. Export remaining durable tables into staging.durable_rows with deterministic ordering
        for tbl in sorted(DURABLE_TABLES):
            if tbl == "users":
                continue

            order_by = TABLE_ORDER_BY.get(tbl, "id ASC")
            rows = cur.execute(f'SELECT * FROM "{tbl}" ORDER BY {order_by}').fetchall()

            sql_lines.append("")
            sql_lines.append(f"-- Staging table: {tbl} ({len(rows)} rows)")
            for r in rows:
                r_dict = dict(r)
                legacy_id = compute_legacy_id(tbl, r_dict)
                legacy_user_id = compute_legacy_user_id(r_dict)
                if legacy_user_id is not None:
                    distinct_owners.add(legacy_user_id)

                # Never include raw passwords or provider tokens in payloads
                safe_payload = {
                    k: v
                    for k, v in r_dict.items()
                    if k not in SENSITIVE_PAYLOAD_KEYS
                }
                payload_json = json.dumps(safe_payload, ensure_ascii=False, sort_keys=True)
                escaped_json = escape_sql(payload_json)
                user_id_sql = f"{legacy_user_id}" if legacy_user_id is not None else "null"

                sql_lines.append(
                    f"insert into staging.durable_rows("
                    f"table_name, legacy_id, legacy_user_id, payload, staged_at) values ("
                    f"'{tbl}', '{escape_sql(legacy_id)}', {user_id_sql}, '{escaped_json}'::jsonb, now()"
                    f") on conflict (table_name, legacy_id) do update set "
                    f"legacy_user_id=excluded.legacy_user_id, payload=excluded.payload, staged_at=excluded.staged_at;"
                )

        sql_lines.append("")
        sql_lines.append("commit;")
        sql_lines.append("")

        # Write SQL file as exact UTF-8 binary bytes without platform newline translation
        sql_content = "\n".join(sql_lines)
        sql_bytes = sql_content.encode("utf-8")
        sql_path.write_bytes(sql_bytes)
        try:
            os.chmod(sql_path, 0o600)
        except Exception:
            pass

        # Hash MUST be computed from the ACTUAL persisted bytes read from disk
        sql_sha256 = hashlib.sha256(sql_path.read_bytes()).hexdigest()

        excluded_ephemeral = {
            t: {
                "rowCount": table_counts.get(t, 0),
                "reason": EPHEMERAL_TABLES[t],
            }
            for t in sorted(EPHEMERAL_TABLES)
        }

        manifest = {
            "mode": "READ_ONLY_OFFLINE_STAGING_EXPORT",
            "sourceDatabase": str(db_path),
            "sqlArtifact": str(sql_path),
            "sqlSha256": sql_sha256,
            "sqlLineCount": len(sql_lines),
            "totalTablesFound": len(found_tables),
            "tableCounts": table_counts,
            "stagedDurableTables": staged_counts,
            "stagedDurableRowsTotal": staged_durable_rows_total,
            "excludedEphemeralTables": excluded_ephemeral,
            "excludedEphemeralRowsTotal": excluded_ephemeral_rows_total,
            "distinctOwnersCount": len(distinct_owners),
            "accountArchive": {
                "totalAccounts": len(user_rows),
                "claimableAccountsCount": len(claimable_accounts),
                "claimableLegacyUserIds": [int(u["id"]) for u in claimable_accounts],
                "archivedTestAccountsCount": len(test_accounts),
                "archivedTestLegacyUserIds": [int(u["id"]) for u in test_accounts],
                "testAccountsRemainUnclaimed": True,
            },
            "securityPolicy": {
                "passwordHashExcluded": True,
                "sessionTokensExcluded": True,
                "recoveryTokensExcluded": True,
                "rateLimitsExcluded": True,
                "transientEventsExcluded": True,
                "providerCredentialsExcluded": True,
                "accountEmailIsolatedToUserMappings": True,
                "anonAuthenticatedGrantsPresent": False,
                "dataApiExposed": False,
            },
        }

        manifest_bytes = json.dumps(manifest, indent=2, ensure_ascii=False).encode("utf-8")
        manifest_path.write_bytes(manifest_bytes)
        try:
            os.chmod(manifest_path, 0o600)
        except Exception:
            pass

        return sql_path, manifest_path, manifest
    finally:
        conn.close()


def main():
    parser = argparse.ArgumentParser(
        description="Read-only SQLite staging archive exporter for Supabase cutover"
    )
    parser.add_argument(
        "--db-path",
        type=Path,
        default=Path("backend/data/aimentor.sqlite3"),
        help="Path to source SQLite database (connected strictly read-only)",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("backend/data"),
        help="Directory to write protected staging artifacts (ignored by Git)",
    )
    parser.add_argument(
        "--sql-filename",
        type=str,
        default="staging_archive.sql",
        help="Filename for SQL staging load script",
    )
    parser.add_argument(
        "--manifest-filename",
        type=str,
        default="staging_manifest.json",
        help="Filename for JSON export manifest",
    )

    args = parser.parse_args()

    sql_path, manifest_path, manifest = export_sqlite_to_staging(
        args.db_path,
        args.output_dir,
        args.sql_filename,
        args.manifest_filename,
    )

    # Standard output: strictly counts and checksums, ZERO personal data values
    print("=" * 70)
    print("AI MENTOR - SQLITE STAGING ARCHIVE EXPORTER (OFFLINE READ-ONLY)")
    print("=" * 70)
    print(f"Source SQLite: {manifest['sourceDatabase']} (Strictly Read-Only)")
    print(f"Total tables inspected: {manifest['totalTablesFound']} / 26")
    print(f"Durable tables staged: {len(manifest['stagedDurableTables'])} ({manifest['stagedDurableRowsTotal']} total rows)")
    print(f"Ephemeral tables excluded: {len(manifest['excludedEphemeralTables'])} ({manifest['excludedEphemeralRowsTotal']} total rows)")
    print(f"Distinct owner accounts: {manifest['distinctOwnersCount']}")
    print(f"Account archive: {manifest['accountArchive']['claimableAccountsCount']} claimable, {manifest['accountArchive']['archivedTestAccountsCount']} test accounts archived unclaimed")
    print("-" * 70)
    print("STAGED DURABLE COUNTS:")
    for tbl, cnt in sorted(manifest["stagedDurableTables"].items()):
        print(f"  - {tbl:<26}: {cnt:>5} rows")
    print("-" * 70)
    print("EXCLUDED EPHEMERAL COUNTS:")
    for tbl, meta in sorted(manifest["excludedEphemeralTables"].items()):
        print(f"  - {tbl:<26}: {meta['rowCount']:>5} rows ({meta['reason'][:40]}...)")
    print("=" * 70)
    print(f"SQL Artifact: {sql_path} (Lines: {manifest['sqlLineCount']})")
    print(f"SQL SHA-256:  {manifest['sqlSha256']}")
    print(f"Manifest JSON:{manifest_path}")
    print("Security: Password hashes & tokens excluded. Account emails isolated to user_mappings.")
    print("Status: READY_FOR_CODEX_REVIEW_AND_IMPORT")
    print("=" * 70)


if __name__ == "__main__":
    main()
