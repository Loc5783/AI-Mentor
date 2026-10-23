import pytest
import sqlite3
import sys
import time
from pathlib import Path
from fastapi.testclient import TestClient
from aimentor.config import Settings
from aimentor.main import create_app
from aimentor.manage_roles import main as manage_roles_main
from test_api import signup, headers

def create_db_instance(tmp_path, name):
    db_dir = tmp_path / name
    db_dir.mkdir(parents=True, exist_ok=True)
    app = create_app(Settings(db_dir))
    with TestClient(app) as client:
        user = signup(client, f"User_{name}")
    db_file = db_dir / "aimentor.sqlite3"
    return db_dir, db_file, user

def test_argument_mutual_exclusion(tmp_path, monkeypatch):
    dir_a, file_a, user_a = create_db_instance(tmp_path, "DB_A")

    # Passing both --data-dir and --db-path MUST fail
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--data-dir", str(dir_a), "--db-path", str(file_a), "--user", str(user_a["userId"]), "--role", "ADMIN"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 1

def test_argument_neither_provided(tmp_path, monkeypatch):
    dir_a, file_a, user_a = create_db_instance(tmp_path, "DB_A")

    # Passing neither --data-dir nor --db-path MUST fail
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--user", str(user_a["userId"]), "--role", "ADMIN"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 1

def test_invalid_path_no_fallback_to_cwd(tmp_path, monkeypatch):
    dir_a, file_a, user_a = create_db_instance(tmp_path, "DB_A")
    invalid_file = tmp_path / "non_existent.sqlite3"

    # Passing non-existent --db-path while dir_a exists
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(invalid_file), "--user", str(user_a["userId"]), "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 1

    # Verify DB_A was NOT modified
    conn = sqlite3.connect(file_a)
    cur = conn.cursor()
    cur.execute("SELECT role FROM users WHERE id=?", (user_a["userId"],))
    assert cur.fetchone()[0] == "STUDENT"
    conn.close()

def test_two_databases_isolation(tmp_path, monkeypatch):
    dir_a, file_a, user_a = create_db_instance(tmp_path, "DB_A")
    dir_b, file_b, user_b = create_db_instance(tmp_path, "DB_B")

    # Update only DB_A
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(file_a), "--user", str(user_a["userId"]), "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 0

    # DB_A user is ADMIN, DB_B user is still STUDENT
    conn_a = sqlite3.connect(file_a)
    assert conn_a.execute("SELECT role FROM users WHERE id=?", (user_a["userId"],)).fetchone()[0] == "ADMIN"
    conn_a.close()

    conn_b = sqlite3.connect(file_b)
    assert conn_b.execute("SELECT role FROM users WHERE id=?", (user_b["userId"],)).fetchone()[0] == "STUDENT"
    conn_b.close()

def test_special_characters_in_path(tmp_path, monkeypatch):
    special_dir = tmp_path / "special # folder % name"
    special_dir.mkdir(parents=True, exist_ok=True)
    app = create_app(Settings(special_dir))
    with TestClient(app) as client:
        user = signup(client, "SpecialPathUser")
    db_file = special_dir / "aimentor.sqlite3"

    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(db_file), "--user", str(user["userId"]), "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 0

    conn = sqlite3.connect(db_file)
    assert conn.execute("SELECT role FROM users WHERE id=?", (user["userId"],)).fetchone()[0] == "ADMIN"
    conn.close()

def test_ambiguous_user_identifier(tmp_path, monkeypatch):
    dir_a, file_a, user_a = create_db_instance(tmp_path, "DB_Ambiguous")

    # Insert second user whose username is string of first user's ID
    conn = sqlite3.connect(file_a)
    conn.execute("INSERT INTO users (id, username, email, password_hash, nickname, created_at, updated_at) VALUES (999, ?, ?, 'hash', 'nick', 1, 1)",
                 (str(user_a["userId"]), "other@example.com"))
    conn.commit()
    conn.close()

    # Querying string user_a["userId"] matches user_a by ID and user 999 by username -> Ambiguous!
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(file_a), "--user", str(user_a["userId"]), "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 1

def test_non_sqlite_file_error(tmp_path, monkeypatch):
    fake_file = tmp_path / "fake.db"
    fake_file.write_text("This is not a sqlite database file.")

    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(fake_file), "--user", "1", "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 1

def test_missing_schema_error(tmp_path, monkeypatch):
    empty_db = tmp_path / "empty.sqlite3"
    conn = sqlite3.connect(empty_db)
    conn.execute("CREATE TABLE dummy (id INT)")
    conn.commit()
    conn.close()

    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(empty_db), "--user", "1", "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 1

def test_missing_role_audit_logs_schema_error(tmp_path, monkeypatch):
    partial_db = tmp_path / "partial.sqlite3"
    conn = sqlite3.connect(partial_db)
    conn.execute("CREATE TABLE users (id INT, username TEXT, email TEXT, role TEXT)")
    conn.execute("CREATE TABLE sessions (id INT)")
    conn.execute("CREATE TABLE events (id INT)")
    conn.commit()
    conn.close()

    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(partial_db), "--user", "1", "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 1

def test_role_audit_logs_created_and_no_op(tmp_path, monkeypatch):
    dir_a, file_a, user_a = create_db_instance(tmp_path, "DB_Audit")
    user_id = user_a["userId"]

    # 1. STUDENT -> ADMIN role change with --apply
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(file_a), "--user", str(user_id), "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 0

    # Verify audit log entry
    conn = sqlite3.connect(file_a)
    conn.row_factory = sqlite3.Row
    audit = conn.execute("SELECT * FROM role_audit_logs WHERE user_id=?", (user_id,)).fetchall()
    assert len(audit) == 1
    assert audit[0]["previous_role"] == "STUDENT"
    assert audit[0]["new_role"] == "ADMIN"
    assert audit[0]["source"] == "CLI_MANAGE_ROLES"
    conn.close()

    # 2. No-Op check: re-running with same role (ADMIN)
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(file_a), "--user", str(user_id), "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 0

    # Verify NO new audit log was added on No-Op
    conn = sqlite3.connect(file_a)
    audit2 = conn.execute("SELECT * FROM role_audit_logs WHERE user_id=?", (user_id,)).fetchall()
    assert len(audit2) == 1 # Still 1 entry
    conn.close()

    # 3. ADMIN -> STUDENT demotion
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(file_a), "--user", str(user_id), "--role", "STUDENT", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 0

    # Verify second audit log entry
    conn = sqlite3.connect(file_a)
    conn.row_factory = sqlite3.Row
    audit3 = conn.execute("SELECT * FROM role_audit_logs WHERE user_id=? ORDER BY changed_at ASC", (user_id,)).fetchall()
    assert len(audit3) == 2
    assert audit3[1]["previous_role"] == "ADMIN"
    assert audit3[1]["new_role"] == "STUDENT"
    conn.close()

def test_admin_api_authorization_matrix(tmp_path, monkeypatch):
    db_dir, db_file, user_student = create_db_instance(tmp_path, "DB_Matrix")
    app = create_app(Settings(db_dir))
    client = TestClient(app)

    # 1. Anonymous Access (No Token) -> 401
    assert client.get("/admin/questions").status_code == 401
    assert client.post("/admin/questions", json={"prompt": "Test"}).status_code == 401
    assert client.post("/admin/questions/q1/publish").status_code == 401
    assert client.post("/admin/import/preview", content=b"code,subject").status_code == 401

    # 2. STUDENT Access -> 403 ADMIN_REQUIRED
    h_student = headers(user_student)
    assert client.get("/admin/questions", headers=h_student).status_code == 403
    assert client.post("/admin/questions", headers=h_student, json={"prompt": "Test"}).status_code == 403
    assert client.post("/admin/questions/q1/publish", headers=h_student).status_code == 403
    assert client.post("/admin/import/preview", headers=h_student, content=b"code,subject").status_code == 403

    # 3. Elevate user to ADMIN via CLI
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(db_file), "--user", str(user_student["userId"]), "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 0

    # 4. Re-login as ADMIN
    user_email = "user_db_matrix@school.example"
    login_res = client.post("/auth/sign-in", json={"identifier": user_email, "password": "Mentor01!2026"})
    assert login_res.status_code == 200
    admin_token = login_res.json()["accessToken"]
    h_admin = {"Authorization": f"Bearer {admin_token}"}

    # ADMIN Access -> 200 OK
    assert client.get("/admin/questions", headers=h_admin).status_code == 200
    assert client.post("/admin/import/preview", headers=h_admin, content=b"code,subject\nQ1,physics").status_code == 200
