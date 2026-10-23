import argparse
import os
import sqlite3
import sys
import time
import uuid
from pathlib import Path

ALLOWED_ROLES = {"STUDENT", "TEACHER", "ADMIN"}

def resolve_db_path(data_dir_arg, db_path_arg):
    if data_dir_arg and db_path_arg:
        print("Error: --data-dir and --db-path are mutually exclusive. Please specify only one parameter.", file=sys.stderr)
        sys.exit(1)

    if not data_dir_arg and not db_path_arg:
        print("Error: Must specify either --data-dir <dir_path> or --db-path <file_path>.", file=sys.stderr)
        sys.exit(1)

    if db_path_arg:
        p = Path(db_path_arg).resolve()
        if not p.exists():
            print(f"Error: Specified --db-path does not exist: {p}", file=sys.stderr)
            sys.exit(1)
        if not p.is_file():
            print(f"Error: Specified --db-path is not a file: {p}", file=sys.stderr)
            sys.exit(1)
        return p

    if data_dir_arg:
        p = Path(data_dir_arg).resolve()
        if not p.exists():
            print(f"Error: Specified --data-dir does not exist: {p}", file=sys.stderr)
            sys.exit(1)
        if not p.is_dir():
            print(f"Error: Specified --data-dir is not a directory: {p}", file=sys.stderr)
            sys.exit(1)
        db_file = p / "aimentor.sqlite3"
        if not db_file.is_file():
            print(f"Error: Database file 'aimentor.sqlite3' does not exist in specified --data-dir: {db_file}", file=sys.stderr)
            sys.exit(1)
        return db_file

    return None

def validate_schema(conn, db_path):
    try:
        cur = conn.cursor()
        cur.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = {row[0] for row in cur.fetchall()}

        required_tables = {"users", "sessions", "events", "role_audit_logs"}
        missing_tables = required_tables - tables
        if missing_tables:
            print(f"Error: Database schema in '{db_path}' is missing required table(s): {', '.join(sorted(missing_tables))}. Please run application migrations before using manage_roles.", file=sys.stderr)
            sys.exit(1)

        # Check required columns
        required_columns = {
            "users": {"id", "username", "email", "role", "updated_at"},
            "sessions": {"id", "user_id", "revoked"},
            "events": {"owner", "resource_type", "resource_id", "revision", "operation", "created_at"},
            "role_audit_logs": {"id", "user_id", "previous_role", "new_role", "source", "changed_at"}
        }

        for table, cols in required_columns.items():
            cur.execute(f"PRAGMA table_info({table})")
            existing_cols = {row[1] for row in cur.fetchall()}
            missing_cols = cols - existing_cols
            if missing_cols:
                print(f"Error: Table '{table}' in '{db_path}' is missing required column(s): {', '.join(sorted(missing_cols))}. Please run application migrations.", file=sys.stderr)
                sys.exit(1)

    except sqlite3.DatabaseError as e:
        print(f"Error: File is not a valid SQLite database or is corrupted: {db_path}", file=sys.stderr)
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description="AI Mentor Local Role Management Tool")
    parser.add_argument("--data-dir", help="Path to data directory containing aimentor.sqlite3")
    parser.add_argument("--db-path", help="Direct path to aimentor.sqlite3 database file")
    parser.add_argument("--user", required=True, help="Exact user ID, username, or email address")
    parser.add_argument("--role", required=True, help="Role to set: STUDENT, TEACHER, or ADMIN")
    parser.add_argument("--apply", action="store_true", help="Commit the role change to the database")

    args = parser.parse_args()

    role = args.role.strip().upper()
    if role not in ALLOWED_ROLES:
        print(f"Error: Invalid role '{args.role}'. Allowed roles: {', '.join(sorted(ALLOWED_ROLES))}", file=sys.stderr)
        sys.exit(1)

    db_path = resolve_db_path(args.data_dir, args.db_path)

    user_identifier = args.user.strip()
    if not user_identifier:
        print("Error: --user identifier cannot be empty.", file=sys.stderr)
        sys.exit(1)

    conn = None
    try:
        uri_path = db_path.as_uri()
        conn = sqlite3.connect(f"{uri_path}?mode=rw", uri=True, timeout=10)
        conn.row_factory = sqlite3.Row

        validate_schema(conn, db_path)

        if args.apply:
            # Transactional Apply Mode: Acquire lock BEFORE reading role or making decisions
            conn.execute("BEGIN IMMEDIATE")

            cur = conn.cursor()
            query = "SELECT id, username, email, role FROM users WHERE "
            if user_identifier.isdigit():
                query += "id = ? OR username = ? COLLATE NOCASE OR email = ? COLLATE NOCASE"
                params = (int(user_identifier), user_identifier, user_identifier)
            else:
                query += "username = ? COLLATE NOCASE OR email = ? COLLATE NOCASE"
                params = (user_identifier, user_identifier)

            cur.execute(query, params)
            rows = cur.fetchall()

            if not rows:
                conn.rollback()
                print(f"Error: User '{user_identifier}' not found in database {db_path}", file=sys.stderr)
                sys.exit(1)

            if len(rows) > 1:
                conn.rollback()
                print(f"Error: Ambiguous query, multiple users matched '{user_identifier}'", file=sys.stderr)
                sys.exit(1)

            user = rows[0]
            user_id = user["id"]
            username = user["username"]
            email = user["email"]
            current_role = user["role"] or "STUDENT"

            print(f"Database Path : {db_path}")
            print(f"Target User ID : {user_id}")
            print(f"Username      : {username}")
            print(f"Email         : {email}")
            print(f"Current Role  : {current_role}")
            print(f"Proposed Role : {role}")

            if current_role == role:
                conn.rollback()
                print(f"\n[NO-OP] User '{username}' already has role '{role}'. No changes were made to the database.")
                sys.exit(0)

            now = time.time()
            audit_id = f"audit_{uuid.uuid4().hex[:12]}"

            upd_cur = conn.execute("UPDATE users SET role=?, updated_at=? WHERE id=?", (role, now, user_id))
            if upd_cur.rowcount != 1:
                conn.rollback()
                print(f"Error: Expected 1 user update but modified {upd_cur.rowcount} rows.", file=sys.stderr)
                sys.exit(1)

            sess_cur = conn.execute("UPDATE sessions SET revoked=1 WHERE user_id=? AND revoked=0", (user_id,))
            revoked_count = sess_cur.rowcount

            conn.execute(
                "INSERT INTO role_audit_logs (id, user_id, previous_role, new_role, source, changed_at) VALUES (?, ?, ?, ?, 'CLI_MANAGE_ROLES', ?)",
                (audit_id, user_id, current_role, role, now)
            )

            conn.execute(
                "INSERT INTO events(owner, resource_type, resource_id, revision, operation, created_at) VALUES(?, 'ROLE_CHANGE', ?, 1, 'UPDATE', ?)",
                (user_id, str(user_id), now)
            )

            conn.commit()

            print(f"\n[SUCCESS] Role updated for user '{username}' (ID: {user_id}): {current_role} -> {role}")
            print(f"Audit Log ID    : {audit_id}")
            print(f"Sessions Revoked: {revoked_count} active session(s) revoked. User must sign in again.")
            sys.exit(0)

        else:
            # Read-Only Dry-Run Mode
            cur = conn.cursor()
            query = "SELECT id, username, email, role FROM users WHERE "
            if user_identifier.isdigit():
                query += "id = ? OR username = ? COLLATE NOCASE OR email = ? COLLATE NOCASE"
                params = (int(user_identifier), user_identifier, user_identifier)
            else:
                query += "username = ? COLLATE NOCASE OR email = ? COLLATE NOCASE"
                params = (user_identifier, user_identifier)

            cur.execute(query, params)
            rows = cur.fetchall()

            if not rows:
                print(f"Error: User '{user_identifier}' not found in database {db_path}", file=sys.stderr)
                sys.exit(1)

            if len(rows) > 1:
                print(f"Error: Ambiguous query, multiple users matched '{user_identifier}'", file=sys.stderr)
                sys.exit(1)

            user = rows[0]
            user_id = user["id"]
            username = user["username"]
            email = user["email"]
            current_role = user["role"] or "STUDENT"

            print(f"Database Path : {db_path}")
            print(f"Target User ID : {user_id}")
            print(f"Username      : {username}")
            print(f"Email         : {email}")
            print(f"Current Role  : {current_role}")
            print(f"Proposed Role : {role}")

            if current_role == role:
                print(f"\n[NO-OP] User '{username}' already has role '{role}'. No changes were made.")
            else:
                print("\n[DRY-RUN] Snapshot at time of check. No changes were committed.")
                print("To commit this role change, re-run with the --apply flag.")
            sys.exit(0)

    except Exception as e:
        if conn:
            try:
                conn.rollback()
            except Exception:
                pass
        print(f"Error executing role change: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass

if __name__ == "__main__":
    main()
