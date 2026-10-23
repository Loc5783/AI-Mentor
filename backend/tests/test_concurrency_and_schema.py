import pytest
import sqlite3
import sys
import time
import threading
import importlib.util
from pathlib import Path
from fastapi.testclient import TestClient
from aimentor.config import Settings
from aimentor.main import create_app
from aimentor.manage_roles import main as manage_roles_main
from test_api import signup

def create_db_instance(tmp_path, name):
    db_dir = tmp_path / name
    db_dir.mkdir(parents=True, exist_ok=True)
    app = create_app(Settings(db_dir))
    with TestClient(app) as client:
        user = signup(client, f"User_{name}")
    db_file = db_dir / "aimentor.sqlite3"
    return db_dir, db_file, user

def test_missing_column_schema_validation(tmp_path, monkeypatch):
    missing_col_db = tmp_path / "missing_col.sqlite3"
    conn = sqlite3.connect(missing_col_db)
    conn.execute("CREATE TABLE users (id INT PRIMARY KEY, username TEXT, email TEXT, role TEXT)")
    conn.execute("CREATE TABLE sessions (id TEXT PRIMARY KEY, user_id INT, revoked INT)")
    conn.execute("CREATE TABLE events (seq INT PRIMARY KEY, owner INT, resource_type TEXT, resource_id TEXT, created_at REAL)")
    conn.execute("CREATE TABLE role_audit_logs (id TEXT PRIMARY KEY, user_id INT, previous_role TEXT, new_role TEXT, source TEXT, changed_at REAL)")
    conn.commit()
    conn.close()

    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(missing_col_db), "--user", "1", "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 1


class InstrumentedConnection:
    def __init__(self, real_conn, on_execute):
        object.__setattr__(self, 'real_conn', real_conn)
        object.__setattr__(self, 'on_execute', on_execute)

    def cursor(self):
        real_cur = self.real_conn.cursor()
        return InstrumentedCursor(real_cur, self.on_execute)

    def execute(self, sql, parameters=()):
        self.on_execute(sql)
        return self.real_conn.execute(sql, parameters)

    def commit(self):
        self.real_conn.commit()

    def rollback(self):
        self.real_conn.rollback()

    def close(self):
        self.real_conn.close()

    def __getattr__(self, name):
        return getattr(self.real_conn, name)

    def __setattr__(self, name, value):
        if name in ('real_conn', 'on_execute'):
            object.__setattr__(self, name, value)
        else:
            setattr(self.real_conn, name, value)

class InstrumentedCursor:
    def __init__(self, real_cur, on_execute):
        object.__setattr__(self, 'real_cur', real_cur)
        object.__setattr__(self, 'on_execute', on_execute)

    def execute(self, sql, parameters=()):
        self.on_execute(sql)
        return self.real_cur.execute(sql, parameters)

    def fetchall(self):
        return self.real_cur.fetchall()

    def fetchone(self):
        return self.real_cur.fetchone()

    def __getattr__(self, name):
        return getattr(self.real_cur, name)

    def __setattr__(self, name, value):
        if name in ('real_cur', 'on_execute'):
            object.__setattr__(self, name, value)
        else:
            setattr(self.real_cur, name, value)

def run_concurrency_test(db_dir, db_file, user_id, monkeypatch, module_main_func, is_buggy=False):
    # Prepare Connection B
    conn_b = sqlite3.connect(db_file)
    conn_b.execute("BEGIN IMMEDIATE")
    conn_b.execute("UPDATE users SET role='TEACHER' WHERE id=?", (user_id,))
    # Do NOT commit yet. B holds the lock.

    cli_event_about_to_lock = threading.Event()
    cli_event_about_to_read = threading.Event()
    cli_exception = []
    sql_execution_order = []

    original_connect = sqlite3.connect

    def mock_connect(dsn, **kwargs):
        real_conn = original_connect(dsn, **kwargs)
        if "mode=rw" not in str(dsn):
            return real_conn # Don't wrap internal test checks

        def on_execute(sql):
            sql_upper = sql.upper().strip()
            # Only record the first SELECT users and BEGIN IMMEDIATE
            if sql_upper == "BEGIN IMMEDIATE" and "BEGIN IMMEDIATE" not in sql_execution_order:
                sql_execution_order.append("BEGIN IMMEDIATE")
                cli_event_about_to_lock.set()
            elif sql_upper.startswith("SELECT ID, USERNAME, EMAIL, ROLE FROM USERS") and "SELECT USERS" not in sql_execution_order:
                sql_execution_order.append("SELECT USERS")
                cli_event_about_to_read.set()
        return InstrumentedConnection(real_conn, on_execute)

    monkeypatch.setattr(sqlite3, "connect", mock_connect)

    def run_cli():
        try:
            monkeypatch.setattr(sys, "argv", [
                "manage_roles", "--db-path", str(db_file), "--user", str(user_id), "--role", "ADMIN", "--apply"
            ])
            try:
                module_main_func()
            except SystemExit as exit_err:
                if exit_err.code != 0:
                    cli_exception.append(f"CLI exited with {exit_err.code}")
        except Exception as err:
            cli_exception.append(err)

    cli_thread = threading.Thread(target=run_cli)
    cli_thread.start()

    # Wait for CLI to hit BEGIN IMMEDIATE
    while not cli_event_about_to_lock.is_set():
        time.sleep(0.01)
        if not cli_thread.is_alive():
            break

    # Now that CLI is definitely blocked on BEGIN IMMEDIATE, B commits.
    # In the buggy version, SELECT USERS has already occurred and been recorded in sql_execution_order.
    conn_b.commit()
    conn_b.close()

    cli_thread.join(timeout=5)
    assert not cli_thread.is_alive(), "CLI thread hung"
    assert not cli_exception, f"CLI exception: {cli_exception}"

    if is_buggy:
        # In buggy version, SELECT happens before BEGIN IMMEDIATE.
        assert sql_execution_order.index("SELECT USERS") < sql_execution_order.index("BEGIN IMMEDIATE")
    else:
        # In fixed version, BEGIN IMMEDIATE happens before SELECT.
        assert sql_execution_order.index("BEGIN IMMEDIATE") < sql_execution_order.index("SELECT USERS")

    # Verify audit
    conn_check = sqlite3.connect(db_file)
    conn_check.row_factory = sqlite3.Row
    audit = conn_check.execute("SELECT * FROM role_audit_logs WHERE user_id=? ORDER BY changed_at DESC", (user_id,)).fetchone()

    if is_buggy:
        # Buggy version reads STUDENT, but B changed it to TEACHER. So audit records STUDENT -> ADMIN.
        assert audit["previous_role"] == "STUDENT", "Buggy variant failed to reproduce the bug"
    else:
        # Fixed version waits for B, then reads TEACHER. So audit records TEACHER -> ADMIN.
        assert audit["previous_role"] == "TEACHER", "Fixed variant failed to record correct previous role"

    assert audit["new_role"] == "ADMIN"
    conn_check.close()

def test_real_cli_concurrency_lock_order(tmp_path, monkeypatch):
    db_dir, db_file, user = create_db_instance(tmp_path, "DB_Real_Concurrency")
    run_concurrency_test(db_dir, db_file, user["userId"], monkeypatch, manage_roles_main, is_buggy=False)

def test_sensitivity_proof_prelock_buggy_variant_fails(tmp_path, monkeypatch):
    # 1. Create buggy version of manage_roles.py dynamically
    original_code = (Path(__file__).parent.parent / "aimentor" / "manage_roles.py").read_text(encoding="utf-8")

    # Replace the fixed order with the buggy order
    buggy_code = original_code.replace(
        """        if args.apply:
            # Transactional Apply Mode: Acquire lock BEFORE reading role or making decisions
            conn.execute("BEGIN IMMEDIATE")

            cur = conn.cursor()
            query = "SELECT id, username, email, role FROM users WHERE \"""",
        """        if args.apply:
            cur = conn.cursor()
            query = "SELECT id, username, email, role FROM users WHERE \""""
    )

    buggy_code = buggy_code.replace(
        """            if current_role == role:
                conn.rollback()
                print(f"\\n[NO-OP] User '{username}' already has role '{role}'. No changes were made to the database.")
                sys.exit(0)

            now = time.time()
            audit_id = f"audit_{uuid.uuid4().hex[:12]}\"""",
        """            if current_role == role:
                conn.rollback()
                print(f"\\n[NO-OP] User '{username}' already has role '{role}'. No changes were made to the database.")
                sys.exit(0)

            conn.execute("BEGIN IMMEDIATE")

            now = time.time()
            audit_id = f"audit_{uuid.uuid4().hex[:12]}\""""
    )

    buggy_file = tmp_path / "manage_roles_buggy.py"
    buggy_file.write_text(buggy_code, encoding="utf-8")

    # 2. Import the buggy module
    spec = importlib.util.spec_from_file_location("manage_roles_buggy", str(buggy_file))
    buggy_module = importlib.util.module_from_spec(spec)
    sys.modules["manage_roles_buggy"] = buggy_module
    spec.loader.exec_module(buggy_module)

    # 3. Run the concurrency test against the buggy variant
    db_dir, db_file, user = create_db_instance(tmp_path, "DB_Buggy_Proof")
    run_concurrency_test(db_dir, db_file, user["userId"], monkeypatch, buggy_module.main, is_buggy=True)

def test_locked_database_handles_error_cleanly(tmp_path, monkeypatch):
    db_dir, db_file, user = create_db_instance(tmp_path, "DB_Locked")
    user_id = user["userId"]

    conn_lock = sqlite3.connect(db_file, timeout=1)
    conn_lock.execute("BEGIN IMMEDIATE")
    conn_lock.execute("UPDATE users SET nickname='Locking' WHERE id=?", (user_id,))

    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(db_file), "--user", str(user_id), "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 1

    conn_lock.rollback()
    conn_lock.close()

    conn = sqlite3.connect(db_file)
    assert conn.execute("SELECT role FROM users WHERE id=?", (user_id,)).fetchone()[0] == "STUDENT"
    conn.close()

def test_file_handles_released_on_exit(tmp_path, monkeypatch):
    db_dir, db_file, user = create_db_instance(tmp_path, "DB_Handles")
    user_id = user["userId"]

    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(db_file), "--user", str(user_id), "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 0

    new_path = db_dir / "renamed_aimentor.sqlite3"
    db_file.rename(new_path)
    assert new_path.is_file()
    assert not db_file.exists()
