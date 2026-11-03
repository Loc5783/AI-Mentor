"""Comprehensive automated test suite for Exam Prep (Ôn thi giữa kỳ & cuối kỳ).

Verifies:
1. Permissions matrix (Teacher/Admin vs Student vs Unauthenticated)
2. Draft vs Published visibility
3. Exam creation, question validation & optimistic locking (expectedVersion)
4. Session start idempotency & resuming active sessions
5. In-progress question secrecy (correctOption, explanation withheld)
6. Server-authoritative time boundaries (deadline - 1s vs deadline + 1s)
7. Answer save revision guard & clearing answers
8. Concurrent save vs submit atomicity & idempotent duplicate submits
9. Lazy finalization on session access after deadline
10. Accurate scoring math & score_10 calculation
11. Snapshot isolation from source question modifications
12. Automatic wrong questions notebook recording
13. Zero EXP / streak impact verification
"""

import os
import sqlite3
import time
import pytest
from starlette.testclient import TestClient

from aimentor.config import Settings
from aimentor.database import Database
from aimentor.main import create_app


class MockClock:
    def __init__(self, start_time: float = 1700000000.0):
        self.current_time = start_time

    def time(self) -> float:
        return self.current_time

    def advance(self, seconds: float):
        self.current_time += seconds


@pytest.fixture
def exam_test_env(tmp_path):
    data_dir = tmp_path / "data"
    data_dir.mkdir(parents=True, exist_ok=True)
    settings = Settings(data_dir)
    app = create_app(settings)
    mock_clock = MockClock(1700000000.0)
    app.state.exam.time = mock_clock.time

    db_file = data_dir / "aimentor.sqlite3"

    with TestClient(app) as client:
        yield {
            "app": app,
            "client": client,
            "clock": mock_clock,
            "db_file": db_file,
            "settings": settings
        }


def _create_user(client, db_file, email, display_name, role="STUDENT"):
    resp = client.post("/auth/sign-up", json={
        "displayName": display_name,
        "email": email,
        "password": "Mentor01!2026",
        "acceptedTermsVersion": "2026-10"
    })
    assert resp.status_code == 201, resp.text
    user_id = resp.json()["userId"]
    token = resp.json()["accessToken"]

    if role != "STUDENT":
        conn = sqlite3.connect(db_file)
        cur = conn.cursor()
        cur.execute("UPDATE users SET role=? WHERE id=?", (role, user_id))
        conn.commit()
        conn.close()

    return token, user_id


def _seed_questions(db_file):
    """Seed test questions for physics Grade 10."""
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("""
        INSERT INTO questions (
            id, code, subject_id, grade, chapter, topic, prompt,
            option_a, option_b, option_c, option_d, correct_option,
            explanation, points, difficulty, status, version, created_at, updated_at
        ) VALUES
        ('Q1', 'PHYS-10-01', 'physics', 10, 'Chương 1', 'Động học', 'Vận tốc là gì?', 'Quãng đường chia thời gian', 'Gia tốc nhân thời gian', 'Lực chia khối lượng', 'Công chia thời gian', 'A', 'v = s/t', 10, 'EASY', 'PUBLISHED', 1, 1700000000, 1700000000),
        ('Q2', 'PHYS-10-02', 'physics', 10, 'Chương 1', 'Động học', 'Gia tốc trong chuyển động thẳng đều bằng bao nhiêu?', '0', '9.8 m/s2', '1 m/s2', 'Không xác định', 'A', 'a = dv/dt = 0', 10, 'EASY', 'PUBLISHED', 1, 1700000000, 1700000000),
        ('Q3', 'PHYS-10-03', 'physics', 10, 'Chương 2', 'Động lực học', 'Định luật II Newton biểu diễn bởi công thức nào?', 'F = ma', 'F = m/a', 'F = mv', 'F = 1/2 mv2', 'A', 'Vector F = m * a', 20, 'MEDIUM', 'PUBLISHED', 1, 1700000000, 1700000000)
    """)
    conn.commit()
    conn.close()


# =========================================================================
# 1. PERMISSIONS & VISIBILITY MATRIX
# =========================================================================

def test_permissions_admin_vs_student(exam_test_env):
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    _seed_questions(db_file)

    admin_token, _ = _create_user(client, db_file, "admin@school.test", "Admin User", role="ADMIN")
    student_token, _ = _create_user(client, db_file, "student@school.test", "Student User", role="STUDENT")

    # 1. Unauthenticated cannot create exam
    resp = client.post("/admin/exams", json={
        "title": "Đề thi giữa kỳ 1 Vật lí 10",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 45,
        "questionIds": ["Q1", "Q2"],
        "status": "DRAFT"
    })
    assert resp.status_code == 401

    # 2. Student cannot create exam
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {student_token}"}, json={
        "title": "Đề thi giữa kỳ 1 Vật lí 10",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 45,
        "questionIds": ["Q1", "Q2"],
        "status": "DRAFT"
    })
    assert resp.status_code == 403

    # 3. Admin can create exam (Draft)
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi giữa kỳ 1 Vật lí 10",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 45,
        "questionIds": ["Q1", "Q2"],
        "status": "DRAFT"
    })
    assert resp.status_code in (200, 201)
    created_exam = resp.json()
    assert created_exam["status"] == "DRAFT"
    exam_id = created_exam["id"]

    # 4. Draft exam is visible to Admin via /admin/exams
    resp = client.get("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"})
    assert resp.status_code == 200
    admin_exams = resp.json()
    assert any(e["id"] == exam_id for e in admin_exams)

    # 5. Draft exam is NOT visible to Student via /exams
    resp = client.get("/exams", headers={"Authorization": f"Bearer {student_token}"})
    assert resp.status_code == 200
    student_exams = resp.json().get("items", resp.json())
    assert not any(e["id"] == exam_id for e in student_exams)

    # 6. Student cannot start session on Draft exam
    resp = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    assert resp.status_code in (400, 403)

    # 7. Admin publishes the exam
    resp = client.post(f"/admin/exams/{exam_id}/publish?publish=true", headers={"Authorization": f"Bearer {admin_token}"})
    assert resp.status_code == 200
    assert resp.json()["status"] == "PUBLISHED"

    # 8. Published exam is now visible to Student
    resp = client.get("/exams", headers={"Authorization": f"Bearer {student_token}"})
    assert resp.status_code == 200
    student_exams = resp.json().get("items", resp.json())
    assert any(e["id"] == exam_id for e in student_exams)


# =========================================================================
# 2. VALIDATION & OPTIMISTIC CONCURRENCY
# =========================================================================

def test_exam_validation_and_optimistic_locking(exam_test_env):
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    _seed_questions(db_file)
    admin_token, _ = _create_user(client, db_file, "admin_v@school.test", "Admin V", role="ADMIN")

    # Invalid question IDs rejected
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi không hợp lệ",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 45,
        "questionIds": ["NON_EXISTENT_Q"],
        "status": "DRAFT"
    })
    assert resp.status_code == 400

    # Valid creation
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi kiểm tra version",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 45,
        "questionIds": ["Q1", "Q2"],
        "status": "DRAFT"
    })
    assert resp.status_code in (200, 201)
    exam = resp.json()
    exam_id = exam["id"]
    assert exam["version"] == 1

    # Update with correct version
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "id": exam_id,
        "title": "Đề thi đã đổi tên v2",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 60,
        "questionIds": ["Q1", "Q2", "Q3"],
        "status": "DRAFT",
        "expectedVersion": 1
    })
    assert resp.status_code in (200, 201)
    updated = resp.json()
    assert updated["version"] == 2
    assert updated["totalPoints"] == 40  # 10 + 10 + 20

    # Conflicting update with stale version 1
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "id": exam_id,
        "title": "Cố tình ghi đè bằng version cũ",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 60,
        "questionIds": ["Q1"],
        "status": "DRAFT",
        "expectedVersion": 1
    })
    assert resp.status_code == 409


# =========================================================================
# 3. SESSION START, QUESTION SECRECY & IDEMPOTENCY
# =========================================================================

def test_session_start_idempotency_and_question_secrecy(exam_test_env):
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    _seed_questions(db_file)
    admin_token, _ = _create_user(client, db_file, "admin_s@school.test", "Admin S", role="ADMIN")
    student_token, student_id = _create_user(client, db_file, "student_s@school.test", "Student S", role="STUDENT")

    # Admin creates and publishes exam (45 mins)
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi Vật lí 10 HK1",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 45,
        "questionIds": ["Q1", "Q2", "Q3"],
        "status": "PUBLISHED"
    })
    exam_id = resp.json()["id"]

    # Student starts session
    resp = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    assert resp.status_code in (200, 201)
    session = resp.json()
    session_id = session["id"]
    assert session["state"] == "IN_PROGRESS"
    assert session["startedAt"] == 1700000000
    assert session["deadlineAt"] == 1700000000 + 45 * 60

    # Verify QUESTION SECRECY: correctOption, explanation, isCorrect MUST NOT be leaked in progress
    questions = session["questions"]
    assert len(questions) == 3
    for q in questions:
        assert q.get("correctOption") is None
        assert q.get("explanation") is None
        assert q.get("isCorrect") is None

    # Idempotency: Starting the same exam again while in progress returns the active session
    resp2 = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    assert resp2.status_code in (200, 201)
    assert resp2.json()["id"] == session_id

    # Another student cannot see this student's session
    other_token, _ = _create_user(client, db_file, "other_student@school.test", "Other Student", role="STUDENT")
    resp_other = client.get(f"/exams/sessions/{session_id}", headers={"Authorization": f"Bearer {other_token}"})
    assert resp_other.status_code == 403


# =========================================================================
# 4. TIME BOUNDARY, REVISION GUARD & DEADLINE ENFORCEMENT
# =========================================================================

def test_time_boundary_and_revision_protection(exam_test_env):
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    clock = exam_test_env["clock"]
    _seed_questions(db_file)

    admin_token, _ = _create_user(client, db_file, "admin_t@school.test", "Admin T", role="ADMIN")
    student_token, _ = _create_user(client, db_file, "student_t@school.test", "Student T", role="STUDENT")

    # 10-minute exam
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi 10 phút",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 10,
        "questionIds": ["Q1", "Q2"],
        "status": "PUBLISHED"
    })
    exam_id = resp.json()["id"]

    # Start session at t = 1700000000 (deadline = 1700000600)
    resp = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    session_id = resp.json()["id"]

    # 1. Answer Q1 at t = 1700000100 (Revision 1)
    clock.advance(100)
    resp = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "A",
        "revision": 1
    })
    assert resp.status_code == 200
    assert resp.json()["answers"]["Q1"] == "A"

    # 2. Update Q1 to B with Revision 2
    resp = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "B",
        "revision": 2
    })
    assert resp.status_code == 200
    assert resp.json()["answers"]["Q1"] == "B"

    # 3. Stale Revision 1 arrives late -> MUST BE SAFELY DISCARDED (answers remain 'B')
    resp = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "A",
        "revision": 1
    })
    assert resp.status_code == 200
    assert resp.json()["answers"]["Q1"] == "B"

    # 4. Unselect/clear Q1 (selectedOption = null) with Revision 3
    resp = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": None,
        "revision": 3
    })
    assert resp.status_code == 200
    assert "Q1" not in resp.json()["answers"]

    # Reselect Q1 as A with Revision 4
    resp = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "A",
        "revision": 4
    })
    assert resp.status_code == 200
    assert resp.json()["answers"]["Q1"] == "A"

    # 5. Boundary: deadline - 1s (t = 1700000599) -> Answering is ALLOWED
    clock.current_time = 1700000599
    resp = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q2",
        "selectedOption": "A",
        "revision": 1
    })
    assert resp.status_code == 200
    assert resp.json()["answers"]["Q2"] == "A"

    # 6. Boundary: deadline + 1s (t = 1700000601) -> Answering is REJECTED
    clock.current_time = 1700000601
    resp = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q2",
        "selectedOption": "B",
        "revision": 2
    })
    assert resp.status_code == 409


# =========================================================================
# 5. LAZY FINALIZATION, SCORING MATH & WRONG NOTEBOOK INTEGRATION
# =========================================================================

def test_lazy_finalization_scoring_and_wrong_notebook(exam_test_env):
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    clock = exam_test_env["clock"]
    _seed_questions(db_file)

    admin_token, _ = _create_user(client, db_file, "admin_l@school.test", "Admin L", role="ADMIN")
    student_token, student_id = _create_user(client, db_file, "student_l@school.test", "Student L", role="STUDENT")

    # Exam with Q1 (10 pts), Q2 (10 pts), Q3 (20 pts). Total points = 40.
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi chấm điểm chuẩn",
        "subjectId": "physics",
        "grade": 10,
        "examType": "FINAL",
        "semester": 1,
        "durationMinutes": 15,
        "questionIds": ["Q1", "Q2", "Q3"],
        "status": "PUBLISHED"
    })
    exam_id = resp.json()["id"]

    resp = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    session_id = resp.json()["id"]

    # Student answers:
    # Q1: A (Correct, +10)
    # Q2: B (Wrong, 0 - correct is A)
    # Q3: Unanswered (0)
    client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "A",
        "revision": 1
    })
    client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q2",
        "selectedOption": "B",
        "revision": 1
    })

    # Advance clock past deadline (15 mins = 900s)
    clock.advance(950)

    # 1. Accessing session triggers LAZY FINALIZATION to TIMED_OUT
    resp = client.get(f"/exams/sessions/{session_id}", headers={"Authorization": f"Bearer {student_token}"})
    assert resp.status_code == 200
    final_session = resp.json()
    assert final_session["state"] == "TIMED_OUT"

    # 2. Verify score calculation:
    # rawScore = 10 (Q1)
    # totalPoints = 40
    # score10 = round(10 / 40 * 10, 2) = 2.5
    # correctCount = 1, totalCount = 3
    assert final_session["rawScore"] == 10
    assert final_session["totalPoints"] == 40
    assert final_session["score10"] == 2.5
    assert final_session["correctCount"] == 1
    assert final_session["totalCount"] == 3

    # 3. Post-completion question data MUST now reveal correctOption and explanation
    q_map = {q["id"]: q for q in final_session["questions"]}
    assert q_map["Q1"]["isCorrect"] is True
    assert q_map["Q1"]["correctOption"] == "A"
    assert q_map["Q2"]["isCorrect"] is False
    assert q_map["Q2"]["selectedOption"] == "B"
    assert q_map["Q2"]["correctOption"] == "A"
    assert q_map["Q3"]["isCorrect"] is False
    assert q_map["Q3"]["selectedOption"] is None

    # 4. Verify Wrong Questions Notebook: Q2 (wrong) and Q3 (unanswered) must be added with NEEDS_REVIEW
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT question_id, state FROM wrong_questions_notebook WHERE user_id = ? ORDER BY question_id", (student_id,))
    rows = cur.fetchall()
    assert len(rows) == 2
    assert rows[0][0] == "Q2"
    assert rows[0][1] == "NEEDS_REVIEW"
    assert rows[1][0] == "Q3"
    assert rows[1][1] == "NEEDS_REVIEW"

    # 5. Verify Zero EXP / streak impact: Exam prep does NOT modify learning progress
    cur.execute("SELECT * FROM awards WHERE user_id = ?", (student_id,))
    assert cur.fetchone() is None
    cur.execute("SELECT * FROM learning_days WHERE user_id = ?", (student_id,))
    assert cur.fetchone() is None
    conn.close()


# =========================================================================
# 6. MANUAL SUBMIT IDEMPOTENCY & SNAPSHOT ISOLATION
# =========================================================================

def test_manual_submit_and_snapshot_isolation(exam_test_env):
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    _seed_questions(db_file)

    admin_token, _ = _create_user(client, db_file, "admin_m@school.test", "Admin M", role="ADMIN")
    student_token, student_id = _create_user(client, db_file, "student_m@school.test", "Student M", role="STUDENT")

    # Exam with Q1
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi kiểm tra snapshot",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 30,
        "questionIds": ["Q1"],
        "status": "PUBLISHED"
    })
    exam_id = resp.json()["id"]

    # Start session
    resp = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    session_id = resp.json()["id"]

    # Student answers Q1 = A
    client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "A",
        "revision": 1
    })

    # NOW: Source question Q1 is modified in the bank (prompt changed, correct_option changed to B)
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("""
        UPDATE questions 
        SET prompt = 'Nội dung câu hỏi đã bị sửa hoàn toàn!', correct_option = 'B', points = 50
        WHERE id = 'Q1'
    """)
    conn.commit()
    conn.close()

    # Student submits session manually
    resp = client.post(f"/exams/sessions/{session_id}/submit", headers={"Authorization": f"Bearer {student_token}"}, json={})
    assert resp.status_code == 200
    res1 = resp.json()
    assert res1["state"] == "SUBMITTED"
    assert res1["rawScore"] == 10  # graded against snapshot points (10), not modified question points (50)
    assert res1["questions"][0]["prompt"] == "Vận tốc là gì?"  # original snapshot preserved
    assert res1["questions"][0]["isCorrect"] is True  # graded against snapshot correct_option ('A')

    # Duplicate submit is IDEMPOTENT: returns the already submitted session
    resp_dup = client.post(f"/exams/sessions/{session_id}/submit", headers={"Authorization": f"Bearer {student_token}"}, json={})
    assert resp_dup.status_code == 200
    assert resp_dup.json()["id"] == session_id
    assert resp_dup.json()["state"] == "SUBMITTED"


# =========================================================================
# 7. REGRESSION TESTS: ANSWER REVISION RULES (BUGS 1A & 1B)
# =========================================================================

def test_regression_answer_revision_rules(exam_test_env):
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    _seed_questions(db_file)

    admin_token, _ = _create_user(client, db_file, "admin_rev@school.test", "Admin Rev", role="ADMIN")
    student_token, _ = _create_user(client, db_file, "student_rev@school.test", "Student Rev", role="STUDENT")

    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi kiểm tra Revision",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 30,
        "questionIds": ["Q1", "Q2"],
        "status": "PUBLISHED"
    })
    exam_id = resp.json()["id"]

    resp = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    session_id = resp.json()["id"]

    # Step 1: Save Q1 = B, revision 3
    resp1 = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "B",
        "revision": 3
    })
    assert resp1.status_code == 200
    assert resp1.json()["answers"].get("Q1") == "B"

    # Step 2: Cùng revision/cùng payload -> Safe retry (idempotent, 200)
    resp_retry = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "B",
        "revision": 3
    })
    assert resp_retry.status_code == 200
    assert resp_retry.json()["answers"].get("Q1") == "B"

    # Step 3: Cùng revision 3 nhưng khác payload (A) -> BUG 1A: Phải bị từ chối 409 Conflict, không được âm thầm ghi đè!
    resp_conflict = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "A",
        "revision": 3
    })
    assert resp_conflict.status_code == 409, f"Cùng revision khác payload phải bị từ chối 409, nhưng nhận {resp_conflict.status_code}: {resp_conflict.text}"

    # Step 4: Bỏ chọn Q1 ở revision 5
    resp_clear = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": None,
        "revision": 5
    })
    assert resp_clear.status_code == 200
    assert "Q1" not in resp_clear.json()["answers"]

    # Step 5: BUG 1B: Request cũ chọn D, revision 2 đến sau thao tác bỏ chọn ở revision 5
    resp_stale = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "D",
        "revision": 2
    })
    # Request cũ đến sau không được ghi đè thao tác bỏ chọn mới hơn
    assert resp_stale.status_code == 200
    assert resp_stale.json().get("status") == "IGNORED_STALE"
    # Q1 vẫn phải là bỏ chọn (không có trong answers)
    assert "Q1" not in resp_stale.json()["answers"], "Lỗi: Request cũ rev 2 đã ghi đè lên thao tác bỏ chọn rev 5!"

    # Step 6: Chọn lại sau khi bỏ chọn: Chọn C ở revision 6
    resp_reselect = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "C",
        "revision": 6
    })
    assert resp_reselect.status_code == 200
    assert resp_reselect.json()["answers"].get("Q1") == "C"

    # Step 7: Request mới đến trước (Q2 = A, rev 10), request cũ đến sau (Q2 = D, rev 8)
    resp_q2_new = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q2",
        "selectedOption": "A",
        "revision": 10
    })
    assert resp_q2_new.status_code == 200
    assert resp_q2_new.json()["answers"].get("Q2") == "A"

    resp_q2_old = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q2",
        "selectedOption": "D",
        "revision": 8
    })
    assert resp_q2_old.status_code == 200
    assert resp_q2_old.json().get("status") == "IGNORED_STALE"
    assert resp_q2_old.json()["answers"].get("Q2") == "A"


# =========================================================================
# 8. REGRESSION TESTS: TIMEOUT ROLLBACK BUG (BUG 2) & BOUNDARY
# =========================================================================

def test_regression_timeout_rollback_and_atomic_finalization(exam_test_env):
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    clock = exam_test_env["clock"]
    _seed_questions(db_file)

    admin_token, _ = _create_user(client, db_file, "admin_to@school.test", "Admin TO", role="ADMIN")
    student_token, student_id = _create_user(client, db_file, "student_to@school.test", "Student TO", role="STUDENT")

    # Exam duration 10 minutes (600s)
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi kiểm tra Timeout Rollback",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 10,
        "questionIds": ["Q1", "Q2"],
        "status": "PUBLISHED"
    })
    exam_id = resp.json()["id"]

    # Start session at T0 (1700000000.0) -> deadline is T0 + 600.0
    resp = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    session_id = resp.json()["id"]
    deadline = resp.json()["deadlineAt"]

    # Save answer on time (T0 + 100s)
    clock.advance(100.0)
    client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "A",
        "revision": 1
    })

    # Advance clock past deadline: T0 + 605s (5 seconds late)
    clock.current_time = deadline + 5.0

    # Call save_answer with late answer
    resp_late = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q2",
        "selectedOption": "A",
        "revision": 1
    })
    assert resp_late.status_code == 409
    assert resp_late.json()["code"] == "EXAM_SESSION_EXPIRED"

    # DIRECT DATABASE CHECK: In BUG 2, this transaction was rolled back, so state was still IN_PROGRESS!
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT state, score, correct_count, raw_score FROM exam_sessions WHERE id = ?", (session_id,))
    row = cur.fetchone()
    conn.close()

    assert row is not None
    db_state, db_score, db_correct, db_raw = row
    assert db_state == "TIMED_OUT", f"BUG 2 REPRODUCED: Transaction rolled back! DB state is still '{db_state}' instead of TIMED_OUT!"
    assert db_correct == 1  # Q1 correct, Q2 not answered on time
    assert db_raw == 10

    # Reading session via GET /exams/sessions/{id} returns committed TIMED_OUT state
    resp_get = client.post(f"/exams/sessions/{session_id}/submit", headers={"Authorization": f"Bearer {student_token}"}, json={})
    assert resp_get.status_code == 200
    assert resp_get.json()["state"] == "TIMED_OUT"
    assert resp_get.json()["rawScore"] == 10

    # Verify late answer Q2 is NOT in answers or score
    assert "Q2" not in resp_get.json()["answers"]

    # Re-creating service and reading session shows committed final result
    fresh_db = Database(exam_test_env["settings"].data_dir)
    from aimentor.exam import ExamService
    fresh_exam_service = ExamService(fresh_db, time_provider=clock.time)
    reloaded_session = fresh_exam_service.get_session(int(student_id), session_id)
    assert reloaded_session["state"] == "TIMED_OUT"
    assert reloaded_session["rawScore"] == 10
    assert "Q2" not in reloaded_session["answers"]


def test_deadline_boundaries_exact_and_duplicate_finalization(exam_test_env):
    """Verifies strict boundary at deadline-1, exact deadline, deadline+1, and idempotent finalization."""
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    clock = exam_test_env["clock"]
    _seed_questions(db_file)

    admin_token, _ = _create_user(client, db_file, "admin_bnd@school.test", "Admin Bnd", role="ADMIN")
    student_token, student_id = _create_user(client, db_file, "student_bnd@school.test", "Student Bnd", role="STUDENT")

    # Exam duration 10 minutes (600s)
    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi kiểm tra Ranh giới Deadline",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 10,
        "questionIds": ["Q1", "Q2", "Q3"],
        "status": "PUBLISHED"
    })
    exam_id = resp.json()["id"]

    # Start session at T0 (1700000000.0) -> deadline is T0 + 600.0
    resp = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    session_id = resp.json()["id"]
    deadline = resp.json()["deadlineAt"]

    # Boundary 1: Ngay trước deadline (deadline - 1.0s) -> Save Q1 succeeds
    clock.current_time = deadline - 1.0
    resp_before = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "A",
        "revision": 1
    })
    assert resp_before.status_code == 200
    assert resp_before.json()["answers"].get("Q1") == "A"

    # Boundary 2: Đúng deadline (deadline) -> Rejected as EXAM_SESSION_EXPIRED & committed as TIMED_OUT
    clock.current_time = deadline
    resp_exact = client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q2",
        "selectedOption": "A",
        "revision": 1
    })
    assert resp_exact.status_code == 409
    assert resp_exact.json()["code"] == "EXAM_SESSION_EXPIRED"

    # Verify directly in DB that exact deadline finalized as TIMED_OUT
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT state FROM exam_sessions WHERE id = ?", (session_id,))
    assert cur.fetchone()[0] == "TIMED_OUT"

    # Count wrong questions notebook entries for this student
    cur.execute("SELECT COUNT(*) FROM wrong_questions_notebook WHERE user_id = ?", (student_id,))
    initial_wrong_cnt = cur.fetchone()[0]
    # Q1 was answered correctly ('A'). Q2 and Q3 were not answered, so 2 wrong entries.
    assert initial_wrong_cnt == 2

    # Duplicate finalization: calling submit or get on already expired session must NOT duplicate wrong notebook
    client.post(f"/exams/sessions/{session_id}/submit", headers={"Authorization": f"Bearer {student_token}"}, json={})
    client.get(f"/exams/sessions/{session_id}", headers={"Authorization": f"Bearer {student_token}"})

    cur.execute("SELECT COUNT(*) FROM wrong_questions_notebook WHERE user_id = ?", (student_id,))
    assert cur.fetchone()[0] == initial_wrong_cnt
    conn.close()


def test_resume_session_accurate_revisions_and_concurrent_saves(exam_test_env):
    """Verifies that resuming session returns accurate questionRevisions and handles concurrent/interleaved writes."""
    import concurrent.futures
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    _seed_questions(db_file)

    admin_token, _ = _create_user(client, db_file, "admin_conc@school.test", "Admin Conc", role="ADMIN")
    student_token, _ = _create_user(client, db_file, "student_conc@school.test", "Student Conc", role="STUDENT")

    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi kiểm tra Concurrent & Resume",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 30,
        "questionIds": ["Q1", "Q2", "Q3"],
        "status": "PUBLISHED"
    })
    exam_id = resp.json()["id"]

    resp = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    session_id = resp.json()["id"]

    # Save Q1 at rev 3, Q2 at rev 7
    client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": "B",
        "revision": 3
    })
    client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q2",
        "selectedOption": "C",
        "revision": 7
    })

    # Clear Q1 at rev 4 (tombstone)
    client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
        "questionId": "Q1",
        "selectedOption": None,
        "revision": 4
    })

    # Resume session (GET /exams/sessions/{id})
    resumed = client.get(f"/exams/sessions/{session_id}", headers={"Authorization": f"Bearer {student_token}"}).json()
    assert resumed["serverNow"] > 0
    assert "Q1" not in resumed["answers"]
    assert resumed["answers"]["Q2"] == "C"

    # Accurate per-question revisions from server!
    q_revs = resumed.get("questionRevisions", {})
    assert q_revs.get("Q1") == 4, f"Q1 tombstone revision should be 4, got {q_revs.get('Q1')}"
    assert q_revs.get("Q2") == 7, f"Q2 revision should be 7, got {q_revs.get('Q2')}"

    # Concurrent answer saves on different questions
    def save_worker(qid, opt, rev):
        return client.post(f"/exams/sessions/{session_id}/answers", headers={"Authorization": f"Bearer {student_token}"}, json={
            "questionId": qid,
            "selectedOption": opt,
            "revision": rev
        }).status_code

    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as executor:
        f1 = executor.submit(save_worker, "Q1", "A", 5)
        f2 = executor.submit(save_worker, "Q2", "A", 8)
        f3 = executor.submit(save_worker, "Q3", "A", 2)
        results = [f1.result(), f2.result(), f3.result()]

    assert all(code == 200 for code in results)

    # Verify final answers
    final_session = client.get(f"/exams/sessions/{session_id}", headers={"Authorization": f"Bearer {student_token}"}).json()
    assert final_session["answers"]["Q1"] == "A"
    assert final_session["answers"]["Q2"] == "A"
    assert final_session["answers"]["Q3"] == "A"


def test_transaction_error_midway_rolls_back_cleanly(exam_test_env):
    """Verifies that unexpected errors occurring mid-transaction trigger a full clean rollback."""
    from unittest.mock import patch
    client = exam_test_env["client"]
    db_file = exam_test_env["db_file"]
    _seed_questions(db_file)

    admin_token, _ = _create_user(client, db_file, "admin_rb@school.test", "Admin RB", role="ADMIN")
    student_token, _ = _create_user(client, db_file, "student_rb@school.test", "Student RB", role="STUDENT")

    resp = client.post("/admin/exams", headers={"Authorization": f"Bearer {admin_token}"}, json={
        "title": "Đề thi kiểm tra Rollback Transaction",
        "subjectId": "physics",
        "grade": 10,
        "examType": "MIDTERM",
        "semester": 1,
        "durationMinutes": 30,
        "questionIds": ["Q1"],
        "status": "PUBLISHED"
    })
    exam_id = resp.json()["id"]

    resp = client.post("/exams/sessions", headers={"Authorization": f"Bearer {student_token}"}, json={
        "examId": exam_id
    })
    session_id = resp.json()["id"]

    # Initial state
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT answers, revision FROM exam_sessions WHERE id = ?", (session_id,))
    init_answers, init_rev = cur.fetchone()
    conn.close()

    # Simulate an unexpected operational failure in DB execute during update
    with patch("aimentor.exam.dump", side_effect=RuntimeError("Disk failure or serialization crash")):
        with pytest.raises(RuntimeError):
            exam_test_env["app"].state.exam.save_answer(
                user_id=2,
                session_id=session_id,
                question_id="Q1",
                selected_option="B",
                revision=1
            )

    # Verify database was NOT partially written: answers and revision remain intact
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT answers, revision FROM exam_sessions WHERE id = ?", (session_id,))
    after_answers, after_rev = cur.fetchone()
    conn.close()

    assert after_answers == init_answers
    assert after_rev == init_rev



