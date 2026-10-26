import pytest
import sys
import sqlite3
from fastapi.testclient import TestClient
from aimentor.config import Settings
from aimentor.main import create_app
from aimentor.manage_roles import main as manage_roles_main
from test_api import signup, headers

def create_client(tmp_path):
    app = create_app(Settings(tmp_path))
    return TestClient(app), app

def test_self_grant_admin_prevented(tmp_path):
    client, app = create_client(tmp_path)
    user = signup(client, "StudentUser")
    h = headers(user)

    # Calling removed grant-local-admin route should return 404 or 405, NOT 200
    res = client.post("/admin/grant-local-admin", headers=h)
    assert res.status_code in (404, 405)

    # Confirm user is still STUDENT and admin endpoints return 403
    res_admin = client.get("/admin/questions", headers=h)
    assert res_admin.status_code == 403
    assert res_admin.json()["code"] == "ADMIN_REQUIRED"

def test_signup_role_injection_prevented(tmp_path):
    client, app = create_client(tmp_path)

    # Attempting to inject extra field "role": "ADMIN" into signup payload
    res = client.post("/auth/sign-up", json={
        "displayName": "HackerUser",
        "email": "hacker@example.com",
        "password": "Password123!",
        "acceptedTermsVersion": "2026-10",
        "role": "ADMIN"
    })
    # Pydantic extra="forbid" rejects payload with 422 Unprocessable Entity
    assert res.status_code == 422

def test_library_and_practice_flow(tmp_path):
    client, app = create_client(tmp_path)
    user = signup(client, "StudentLib")
    h = headers(user)

    # List library quizzes for Physics Grade 10
    res = client.get("/library/quizzes?subject_id=physics&grade=10", headers=h)
    assert res.status_code == 200
    quizzes = res.json()
    assert len(quizzes) >= 1
    quiz = quizzes[0]
    assert quiz["subjectId"] == "physics"
    assert quiz["grade"] == 10

    # Get quiz details for student (ensure correct_option is NOT exposed in questions)
    res = client.get(f"/library/quizzes/{quiz['id']}", headers=h)
    assert res.status_code == 200
    details = res.json()
    assert len(details["questions"]) > 0
    q0 = details["questions"][0]
    assert "correctOption" not in q0
    assert "explanation" not in q0

    # Start practice session
    res = client.post("/library/sessions", headers=h, json={"quizId": quiz["id"]})
    assert res.status_code == 201
    session = res.json()
    session_id = session["id"]
    assert session["state"] == "IN_PROGRESS"
    assert len(session["questions"]) > 0

    # Submit an answer to question 1
    qid = session["questions"][0]["id"]
    res = client.post(f"/library/sessions/{session_id}/answers", headers=h, json={"questionId": qid, "selectedOption": "B"})
    assert res.status_code == 200
    updated = res.json()
    ans = updated["answers"].get(qid)
    assert ans is not None
    assert ans["selectedOption"] == "B"

    # Idempotent answer submission (submitting again returns same state)
    res2 = client.post(f"/library/sessions/{session_id}/answers", headers=h, json={"questionId": qid, "selectedOption": "A"})
    assert res2.status_code == 200
    assert res2.json()["answers"][qid]["selectedOption"] == "B"

    # Early complete attempt must be rejected because questions 1..4 are unanswered
    res_early = client.post(f"/library/sessions/{session_id}/complete", headers=h)
    assert res_early.status_code == 400
    assert res_early.json()["code"] == "SESSION_INCOMPLETE"

    # Answer remaining questions (1 to 4)
    for q in session["questions"][1:]:
        client.post(f"/library/sessions/{session_id}/answers", headers=h, json={"questionId": q["id"], "selectedOption": "A"})

    # Complete practice session after all questions are answered
    res = client.post(f"/library/sessions/{session_id}/complete", headers=h)
    assert res.status_code == 200
    completed = res.json()
    assert completed["state"] == "COMPLETED"

    # Check wrong questions notebook
    res = client.get("/me/wrong-questions?subject_id=physics&grade=10", headers=h)
    assert res.status_code == 200

def test_cli_role_management_tool_and_session_revocation(tmp_path, monkeypatch):
    client, app = create_client(tmp_path)
    user = signup(client, "EditorUser")
    user_id = user["userId"]
    user_email = "editoruser@school.example"
    h = headers(user)
    db_file = tmp_path / "aimentor.sqlite3"

    # 1. Non-admin access should return 403
    res = client.get("/admin/questions", headers=h)
    assert res.status_code == 403

    # 2. Dry-run role change: role should remain STUDENT
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(db_file), "--user", user_id, "--role", "ADMIN"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 0

    # Verify role in DB is still STUDENT
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT role FROM users WHERE id=?", (user_id,))
    assert cur.fetchone()[0] == "STUDENT"
    conn.close()

    # 3. Apply mode role change: role becomes ADMIN and active session is revoked
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(db_file), "--user", user_id, "--role", "ADMIN", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 0

    # Old token should now be revoked (401 INVALID_SESSION)
    res_old = client.get("/admin/questions", headers=h)
    assert res_old.status_code == 401

    # User signs in again to get fresh session token with ADMIN role
    login_res = client.post("/auth/sign-in", json={"identifier": user_email, "password": "Mentor01!2026"})
    assert login_res.status_code == 200
    admin_token = login_res.json()["accessToken"]
    admin_h = {"Authorization": f"Bearer {admin_token}"}

    # Now admin endpoints succeed
    res_admin = client.get("/admin/questions", headers=admin_h)
    assert res_admin.status_code == 200

    # Admin Excel/CSV import preview & confirm
    csv_content = """Mã,Môn,Lớp,Chương,Chủ đề,Bộ sách,Câu hỏi,A,B,C,D,Đáp án đúng,Lời giải,Độ khó,Điểm,Nguồn,Tác giả,Quyền
Q_TEST_01,physics,10,Chương 1,Động học,KNTT,Câu hỏi thử nghiệm?,Option 1,Option 2,Option 3,Option 4,INVALID_ANSWER,Lời giải thử nghiệm,EASY,10,Source,Author,License
Q_TEST_02,physics,10,Chương 1,Động học,KNTT,Câu hỏi 2 hợp lệ?,A1,A2,A3,A4,A,Lời giải 2,EASY,10,Source,Author,License
"""
    res_imp = client.post("/admin/import/preview", headers=admin_h, content=csv_content.encode("utf-8"))
    assert res_imp.status_code == 200
    preview = res_imp.json()
    assert preview["totalRows"] == 2
    assert preview["validCount"] == 1
    assert preview["errorCount"] == 1

    # Confirm import
    res_conf = client.post("/admin/import/confirm", headers=admin_h, json={"validRows": preview["validRows"], "publish": True})
    assert res_conf.status_code == 200
    assert res_conf.json()["importedCount"] == 1

    # 4. Demote role back to STUDENT
    monkeypatch.setattr(sys, "argv", [
        "manage_roles", "--db-path", str(db_file), "--user", user_id, "--role", "STUDENT", "--apply"
    ])
    with pytest.raises(SystemExit) as exit_info:
        manage_roles_main()
    assert exit_info.value.code == 0

    # Former admin token should be revoked (401)
    res_demoted = client.get("/admin/questions", headers=admin_h)
    assert res_demoted.status_code == 401

    # Re-login receives token with STUDENT role, accessing /admin/questions returns 403
    student_login = client.post("/auth/sign-in", json={"identifier": user_email, "password": "Mentor01!2026"})
    student_h = {"Authorization": f"Bearer {student_login.json()['accessToken']}"}
    res_denied = client.get("/admin/questions", headers=student_h)
    assert res_denied.status_code == 403
