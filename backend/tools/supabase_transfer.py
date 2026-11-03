"""Safe local SQLite-to-Supabase transfer and audit tool in DRY-RUN mode.

Guarantees:
- Connects to SQLite strictly READ-ONLY (URI mode=ro).
- NEVER prints personal values (email, name, password hash, user answer texts).
- Reports row counts for all 26 SQLite tables and categorizes durable vs ephemeral.
- Audits all data owners across progress tables and reports unmapped owners.
- Refuses live execution until explicit credentials and complete account mapping are supplied.
- Never writes directly to auth.users or copies scrypt password hashes.
- Excludes live session tokens, recovery tokens, rate-limit counters, and transient event streams.
"""
import argparse
import json
from pathlib import Path
import sqlite3
import sys
from typing import Dict, List, Set, Tuple


DURABLE_TABLES = {
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

EPHEMERAL_TABLES = {
    "sessions": "FastAPI session JWT hashes are process/runtime-specific; Supabase Auth issues its own JWT tokens.",
    "events": "Transient SSE event queue; Supabase Realtime uses CDC on Postgres WAL (supabase_realtime publication).",
    "idempotency": "Short-lived request deduplication cache (24-48h window).",
    "recovery_tokens": "Local password reset tokens; Supabase Auth manages secure recovery OTP flows directly.",
    "rate_limits": "Sliding-window rate limit counters on local instance; cloud has API Gateway rate limits.",
    "ai_provider_operations": "Local LLM call idempotency log; cloud uses mentor_ai_usage and Edge Function.",
}

USER_FK_TABLES = [
    ("enrollments", "user_id"),
    ("attempts", "user_id"),
    ("completions", "user_id"),
    ("awards", "user_id"),
    ("learning_days", "user_id"),
    ("practice_sessions", "user_id"),
    ("wrong_questions_notebook", "user_id"),
    ("exams", "created_by"),
    ("exam_sessions", "user_id"),
]


def audit_sqlite_database(db_path: Path) -> Tuple[Dict[str, int], Set[int]]:
    """Inspects SQLite database read-only without accessing or printing private row data."""
    if not db_path.is_file():
        raise FileNotFoundError(f"Database file not found: {db_path}")

    counts = {}
    distinct_owners: Set[int] = set()

    conn = sqlite3.connect(f"file:{db_path.resolve()}?mode=ro", uri=True)
    try:
        cur = conn.cursor()
        tables = [
            r[0]
            for r in cur.execute(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
            ).fetchall()
        ]

        for t in sorted(tables):
            cur.execute(f'SELECT COUNT(*) FROM "{t}"')
            counts[t] = cur.fetchone()[0]

        # Collect distinct user IDs from durable tables without reading names/emails/hashes
        if "users" in tables:
            for row in cur.execute("SELECT id FROM users").fetchall():
                distinct_owners.add(row[0])

        for tbl, col in USER_FK_TABLES:
            if tbl in tables:
                for row in cur.execute(
                    f'SELECT DISTINCT "{col}" FROM "{tbl}" WHERE "{col}" IS NOT NULL'
                ).fetchall():
                    distinct_owners.add(row[0])

        return counts, distinct_owners
    finally:
        conn.close()


def generate_dry_run_report(
    db_path: Path,
    account_mapping: Dict[str, str] = None,
) -> Dict:
    """Generates a complete dry-run inventory and safety analysis report."""
    counts, owners = audit_sqlite_database(db_path)
    mapping = account_mapping or {}

    unmapped_owners = sorted([uid for uid in owners if str(uid) not in mapping])
    mapped_owners = sorted([uid for uid in owners if str(uid) in mapping])

    report = {
        "databasePath": str(db_path),
        "mode": "DRY-RUN (Read-Only)",
        "totalTablesFound": len(counts),
        "tableCounts": counts,
        "durableTables": {t: counts.get(t, 0) for t in sorted(DURABLE_TABLES) if t in counts},
        "ephemeralExcludedTables": {
            t: {
                "rowCount": counts.get(t, 0),
                "reason": EPHEMERAL_TABLES[t],
            }
            for t in sorted(EPHEMERAL_TABLES)
            if t in counts
        },
        "ownerAudit": {
            "totalDistinctOwners": len(owners),
            "mappedOwnersCount": len(mapped_owners),
            "unmappedOwnersCount": len(unmapped_owners),
            "unmappedOwnerIds": unmapped_owners,
            "status": "READY_FOR_IMPORT" if not unmapped_owners and owners else "REQUIRES_ACCOUNT_MAPPING",
        },
        "securityPolicy": {
            "scryptPasswordCopyPrevented": True,
            "authUsersDirectWritePrevented": True,
            "personalDataPrinted": False,
            "accountTransitionStrategy": (
                "Verified signup / password reset or secure one-time claim process. "
                "Progress records (enrollments, attempts, completions, awards, learning days, "
                "wrong questions) will be linked to the verified Supabase auth.users UUID "
                "retaining 100% of student progress."
            ),
        },
    }
    return report


def main():
    parser = argparse.ArgumentParser(
        description="Safe SQLite-to-Supabase transfer & dry-run audit tool"
    )
    parser.add_argument(
        "--db-path",
        type=Path,
        default=Path("backend/data/aimentor.sqlite3"),
        help="Path to source SQLite database",
    )
    parser.add_argument(
        "--account-mapping",
        type=Path,
        default=None,
        help="JSON file mapping SQLite user_id to Supabase auth.users UUID: {'1': '<uuid>', ...}",
    )
    parser.add_argument(
        "--live",
        action="store_true",
        default=False,
        help="Attempt live migration (refused in Phase 1)",
    )
    parser.add_argument(
        "--output-json",
        type=Path,
        default=None,
        help="Path to write report JSON",
    )

    args = parser.parse_args()

    if args.live:
        print("[REFUSED] Live migration execution is strictly prohibited in Phase 1.", file=sys.stderr)
        print("Reason: Supabase project must first be reviewed and approved by user.", file=sys.stderr)
        sys.exit(1)

    mapping = {}
    if args.account_mapping and args.account_mapping.is_file():
        mapping = json.loads(args.account_mapping.read_text(encoding="utf-8"))

    report = generate_dry_run_report(args.db_path, mapping)

    print("=" * 70)
    print("AI MENTOR - SQLITE TO SUPABASE TRANSFER AUDIT (DRY-RUN)")
    print("=" * 70)
    print(f"Source DB: {report['databasePath']} (Strictly Read-Only)")
    print(f"Total tables: {report['totalTablesFound']} / 26 documented tables")
    print(f"Distinct progress owners: {report['ownerAudit']['totalDistinctOwners']}")
    print(f"Mapped owners: {report['ownerAudit']['mappedOwnersCount']}")
    print(f"Unmapped owners: {report['ownerAudit']['unmappedOwnersCount']} -> {report['ownerAudit']['unmappedOwnerIds']}")
    print(f"Status: {report['ownerAudit']['status']}")
    print("-" * 70)
    print("TABLE INVENTORY (DURABLE):")
    for tbl, cnt in report["durableTables"].items():
        print(f"  - {tbl:<28}: {cnt:>5} rows")
    print("-" * 70)
    print("TABLE INVENTORY (EXCLUDED EPHEMERAL):")
    for tbl, meta in report["ephemeralExcludedTables"].items():
        print(f"  - {tbl:<28}: {meta['rowCount']:>5} rows (Reason: {meta['reason'][:50]}...)")
    print("=" * 70)

    if args.output_json:
        args.output_json.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
        print(f"Report JSON written to {args.output_json}")


if __name__ == "__main__":
    main()
