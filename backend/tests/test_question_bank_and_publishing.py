import pytest
import sqlite3
import json
import time
from fastapi.testclient import TestClient
from aimentor.config import Settings
from aimentor.main import create_app
from aimentor.manage_roles import main as manage_roles_main
from test_api import signup, headers

def setup_client(tmp_path):
    app = create_app(Settings(tmp_path))
    client = TestClient(app)

    # 1. Student user
    student = signup(client, "StudentUser")
    student_h = headers(student)

    # 2. Admin user
    admin_u = signup(client, "AdminUser")
    admin_id = admin_u["userId"]
    db_file = tmp_path / "aimentor.sqlite3"

    # Promote to ADMIN using manage_roles
    import sys
    old_argv = sys.argv
    sys.argv = ["manage_roles", "--db-path", str(db_file), "--user", admin_id, "--role", "ADMIN", "--apply"]
    try:
        manage_roles_main()
    except SystemExit:
        pass
    finally:
        sys.argv = old_argv

    login_res = client.post("/auth/sign-in", json={"identifier": "adminuser@school.example", "password": "Mentor01!2026"})
    admin_token = login_res.json()["accessToken"]
    admin_h = {"Authorization": f"Bearer {admin_token}"}

    # 3. Teacher user
    teacher_u = signup(client, "TeacherUser")
    teacher_id = teacher_u["userId"]
    sys.argv = ["manage_roles", "--db-path", str(db_file), "--user", teacher_id, "--role", "TEACHER", "--apply"]
    try:
        manage_roles_main()
    except SystemExit:
        pass
    finally:
        sys.argv = old_argv

    login_res2 = client.post("/auth/sign-in", json={"identifier": "teacheruser@school.example", "password": "Mentor01!2026"})
    teacher_token = login_res2.json()["accessToken"]
    teacher_h = {"Authorization": f"Bearer {teacher_token}"}

    return client, admin_h, teacher_h, student_h, db_file


# =========================================================================
# GROUP 1: SESSION SNAPSHOT & HISTORICAL INTEGRITY (SCENARIO 1, 2, 3)
# =========================================================================

def test_session_grading_isolated_from_subsequent_question_edits(tmp_path):
    """
    Scenario 1:
    Question v1: correctOption='A', points=10, explanation='Lời giải v1'.
    Student starts session.
    Admin updates question to v2: correctOption='B', points=20, explanation='Lời giải v2'.
    Student answers 'A' in session v1.
    Expected: Student answer 'A' must be marked CORRECT, awarded 10 points, and explanation must be 'Lời giải v1'.
    """
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    q_payload = {
        "code": "PHY_SNAP_01",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1",
        "topic": "Động học",
        "prompt": "Phương trình chuyển động x = 5 + 6t. Vận tốc là?",
        "optionA": "6 m/s",
        "optionB": "5 m/s",
        "optionC": "-5 m/s",
        "optionD": "11 m/s",
        "correctOption": "A",
        "explanation": "Lời giải v1: v = 6 m/s.",
        "difficulty": "EASY",
        "points": 10,
        "status": "PUBLISHED"
    }
    res = client.post("/admin/questions", headers=admin_h, json=q_payload)
    assert res.status_code == 200, res.text
    qid = res.json()["id"]

    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    quiz_id = "quiz_test_snap_01"
    cur.execute("""
    INSERT INTO library_quizzes (id, title, subject_id, grade, chapter, topic, difficulty, question_ids, status, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?)
    """, (quiz_id, "Bài kiểm tra Snapshot v1", "physics", 10, "Chương 1", "Động học", "EASY", json.dumps([qid]), time.time()))
    conn.commit()
    conn.close()

    res_sess = client.post("/library/sessions", headers=student_h, json={"quizId": quiz_id})
    assert res_sess.status_code == 201
    sess_id = res_sess.json()["id"]

    # Student cannot see answer before submitting
    q_pre = res_sess.json()["questions"][0]
    assert "correctOption" not in q_pre
    assert "explanation" not in q_pre

    # Admin modifies question to v2
    q_v2 = dict(q_payload)
    q_v2["id"] = qid
    q_v2["correctOption"] = "B"
    q_v2["points"] = 20
    q_v2["explanation"] = "Lời giải v2: Đáp án sửa thành B."
    res_edit = client.post("/admin/questions", headers=admin_h, json=q_v2)
    assert res_edit.status_code == 200
    assert res_edit.json()["version"] == 2

    # Student answers 'A' in session v1
    res_ans = client.post(f"/library/sessions/{sess_id}/answers", headers=student_h, json={
        "questionId": qid,
        "selectedOption": "A"
    })
    assert res_ans.status_code == 200
    ans_data = res_ans.json()
    assert ans_data["answers"][qid]["isCorrect"] is True
    assert ans_data["score"] == 10

    # Question in session now shows v1 explanation and v1 correct option
    q_after = ans_data["questions"][0]
    assert q_after["correctOption"] == "A"
    assert q_after["explanation"] == "Lời giải v1: v = 6 m/s."


def test_completed_historical_session_integrity_after_admin_edits(tmp_path):
    """
    Scenario 2:
    Student completes session v1.
    Admin edits question prompt, options, answer, and explanation.
    Student re-reads completed session.
    Expected: Historical session displays prompt v1, answer v1, explanation v1, score v1.
    """
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    q_payload = {
        "code": "PHY_SNAP_02",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1",
        "topic": "Động học",
        "prompt": "Vật chuyển động thẳng đều có gia tốc a bằng bao nhiêu?",
        "optionA": "0",
        "optionB": "9.8 m/s2",
        "optionC": "hằng số dương",
        "optionD": "biến thiên",
        "correctOption": "A",
        "explanation": "Lời giải gốc: Chuyển động thẳng đều có a = 0.",
        "difficulty": "EASY",
        "points": 10,
        "status": "PUBLISHED"
    }
    res = client.post("/admin/questions", headers=admin_h, json=q_payload)
    assert res.status_code == 200
    qid = res.json()["id"]

    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    quiz_id = "quiz_test_snap_02"
    cur.execute("""
    INSERT INTO library_quizzes (id, title, subject_id, grade, chapter, topic, difficulty, question_ids, status, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?)
    """, (quiz_id, "Bài kiểm tra Snapshot v2", "physics", 10, "Chương 1", "Động học", "EASY", json.dumps([qid]), time.time()))
    conn.commit()
    conn.close()

    # Student starts and completes session
    res_sess = client.post("/library/sessions", headers=student_h, json={"quizId": quiz_id})
    sess_id = res_sess.json()["id"]

    res_ans = client.post(f"/library/sessions/{sess_id}/answers", headers=student_h, json={
        "questionId": qid,
        "selectedOption": "A"
    })
    assert res_ans.status_code == 200

    res_comp = client.post(f"/library/sessions/{sess_id}/complete", headers=student_h)
    assert res_comp.status_code == 200
    assert res_comp.json()["state"] == "COMPLETED"
    assert res_comp.json()["score"] == 10

    # Admin rewrites the question completely in question bank
    q_rewritten = dict(q_payload)
    q_rewritten["id"] = qid
    q_rewritten["prompt"] = "CÂU HỎI ĐÃ BỊ THAY ĐỔI HOÀN TOÀN: 1 + 1 = ?"
    q_rewritten["optionA"] = "2"
    q_rewritten["optionB"] = "3"
    q_rewritten["optionC"] = "4"
    q_rewritten["optionD"] = "5"
    q_rewritten["correctOption"] = "B"
    q_rewritten["explanation"] = "Lời giải mới hoàn toàn."
    q_rewritten["points"] = 50
    res_edit = client.post("/admin/questions", headers=admin_h, json=q_rewritten)
    assert res_edit.status_code == 200

    # Student reads back the historical completed session
    res_get = client.get(f"/library/sessions/{sess_id}", headers=student_h)
    assert res_get.status_code == 200
    hist = res_get.json()
    assert hist["score"] == 10
    assert hist["correctCount"] == 1
    q0 = hist["questions"][0]
    assert q0["prompt"] == "Vật chuyển động thẳng đều có gia tốc a bằng bao nhiêu?"
    assert next(opt["text"] for opt in q0["options"] if opt["id"] == "A") == "0"
    assert q0["userAnswer"] == "A"
    assert q0["isCorrect"] is True
    assert q0["correctOption"] == "A"
    assert q0["explanation"] == "Lời giải gốc: Chuyển động thẳng đều có a = 0."


def test_unpublish_question_blocks_new_sessions_but_preserves_active_sessions(tmp_path):
    """
    Scenario 3:
    Question is published and added to a quiz.
    Student starts session.
    Admin unpublishes question.
    Expected:
    1. New sessions cannot use the unpublished question (get_quiz_for_student filters it out).
    2. Active session can continue, submit answer, and complete successfully.
    """
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    q_payload = {
        "code": "PHY_UNPUB_01",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1",
        "topic": "Động học",
        "prompt": "Gia tốc rơi tự do thường lấy giá trị nào?",
        "optionA": "9.8 m/s2",
        "optionB": "5 m/s2",
        "optionC": "0 m/s2",
        "optionD": "20 m/s2",
        "correctOption": "A",
        "explanation": "g xấp xỉ 9.8 m/s2",
        "difficulty": "EASY",
        "points": 10,
        "status": "PUBLISHED"
    }
    res = client.post("/admin/questions", headers=admin_h, json=q_payload)
    assert res.status_code == 200
    qid = res.json()["id"]

    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    quiz_id = "quiz_test_unpub_01"
    cur.execute("""
    INSERT INTO library_quizzes (id, title, subject_id, grade, chapter, topic, difficulty, question_ids, status, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PUBLISHED', ?)
    """, (quiz_id, "Bài kiểm tra Unpublish", "physics", 10, "Chương 1", "Động học", "EASY", json.dumps([qid]), time.time()))
    conn.commit()
    conn.close()

    # Active session starts
    res_sess = client.post("/library/sessions", headers=student_h, json={"quizId": quiz_id})
    assert res_sess.status_code == 201
    sess_id = res_sess.json()["id"]

    # Admin unpublishes the question
    res_unpub = client.post(f"/admin/questions/{qid}/publish?publish=false", headers=admin_h)
    assert res_unpub.status_code == 200
    assert res_unpub.json()["status"] == "DRAFT"

    # New session attempt: quiz now has 0 published questions, so starting a session or fetching quiz details does not leak draft
    res_quiz = client.get(f"/library/quizzes/{quiz_id}", headers=student_h)
    assert res_quiz.status_code in (200, 400, 404)
    if res_quiz.status_code == 200:
        assert len(res_quiz.json()["questions"]) == 0

    # Active session submits answer and completes
    res_ans = client.post(f"/library/sessions/{sess_id}/answers", headers=student_h, json={
        "questionId": qid,
        "selectedOption": "A"
    })
    assert res_ans.status_code == 200
    assert res_ans.json()["answers"][qid]["isCorrect"] is True
    assert res_ans.json()["score"] == 10

    res_comp = client.post(f"/library/sessions/{sess_id}/complete", headers=student_h)
    assert res_comp.status_code == 200
    assert res_comp.json()["state"] == "COMPLETED"


# =========================================================================
# GROUP 2: PERMISSIONS & ROLE AUDIT
# =========================================================================

def test_permissions_admin_teacher_student_anonymous(tmp_path):
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # 1. Anonymous access rejected (401)
    res_anon_list = client.get("/admin/questions")
    assert res_anon_list.status_code == 401

    res_anon_create = client.post("/admin/questions", json={"prompt": "test"})
    assert res_anon_create.status_code == 401

    # 2. Student access rejected (403 ADMIN_REQUIRED)
    res_stu_list = client.get("/admin/questions", headers=student_h)
    assert res_stu_list.status_code == 403
    assert res_stu_list.json()["code"] == "ADMIN_REQUIRED"

    res_stu_post = client.post("/admin/questions", headers=student_h, json={"prompt": "test"})
    assert res_stu_post.status_code == 403

    res_stu_pub = client.post("/admin/questions/q_123/publish?publish=true", headers=student_h)
    assert res_stu_pub.status_code == 403

    # 3. Admin and Teacher access allowed (200)
    res_adm = client.get("/admin/questions", headers=admin_h)
    assert res_adm.status_code == 200

    res_tch = client.get("/admin/questions", headers=teacher_h)
    assert res_tch.status_code == 200


# =========================================================================
# GROUP 3: QUESTION VALIDATION & CODE / VERSIONING
# =========================================================================

def test_question_create_validation_checks(tmp_path):
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    base = {
        "code": "Q_VAL_01",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1",
        "topic": "Động học",
        "prompt": "Nội dung câu hỏi hợp lệ?",
        "optionA": "A1",
        "optionB": "B1",
        "optionC": "C1",
        "optionD": "D1",
        "correctOption": "A",
        "explanation": "Giải thích",
        "difficulty": "MEDIUM",
        "points": 10
    }

    # 1. Empty prompt rejected
    p1 = dict(base)
    p1["prompt"] = "   "
    res1 = client.post("/admin/questions", headers=admin_h, json=p1)
    assert res1.status_code == 400

    # 2. Prompt > 5000 chars rejected
    p2 = dict(base)
    p2["prompt"] = "A" * 5001
    res2 = client.post("/admin/questions", headers=admin_h, json=p2)
    assert res2.status_code == 400

    # 3. Missing / empty option rejected
    p3 = dict(base)
    p3["optionA"] = ""
    res3 = client.post("/admin/questions", headers=admin_h, json=p3)
    assert res3.status_code == 400

    # 4. Invalid correctOption rejected
    p4 = dict(base)
    p4["correctOption"] = "E"
    res4 = client.post("/admin/questions", headers=admin_h, json=p4)
    assert res4.status_code == 400

    # 5. Invalid points (<1 or >100) rejected
    p5 = dict(base)
    p5["points"] = 101
    res5 = client.post("/admin/questions", headers=admin_h, json=p5)
    assert res5.status_code == 400

    p5b = dict(base)
    p5b["points"] = 0
    res5b = client.post("/admin/questions", headers=admin_h, json=p5b)
    assert res5b.status_code == 400

    # 6. Invalid grade rejected
    p6 = dict(base)
    p6["grade"] = 15
    res6 = client.post("/admin/questions", headers=admin_h, json=p6)
    assert res6.status_code in (400, 422)

    # 7. Invalid subjectId rejected
    p7 = dict(base)
    p7["subjectId"] = "astrology"
    res7 = client.post("/admin/questions", headers=admin_h, json=p7)
    assert res7.status_code == 400


def test_question_update_id_code_and_version_integrity(tmp_path):
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # Create question
    payload = {
        "code": "PHY_VER_01",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1",
        "topic": "Động học",
        "prompt": "Vận tốc trong chuyển động thẳng đều có đổi không?",
        "optionA": "Không đổi",
        "optionB": "Tăng dần",
        "optionC": "Giảm dần",
        "optionD": "Bằng 0",
        "correctOption": "A",
        "explanation": "v = const",
        "difficulty": "EASY",
        "points": 10,
        "source": "Sách GK",
        "author": "Biên soạn A",
        "license": "Nội bộ",
        "status": "DRAFT"
    }
    res = client.post("/admin/questions", headers=admin_h, json=payload)
    assert res.status_code == 200
    q = res.json()
    qid = q["id"]
    assert q["version"] == 1
    created_at = q["createdAt"]

    # 1. Update with non-existent ID returns 404 (NOT silently creating a new question)
    res_fake = client.post("/admin/questions", headers=admin_h, json={**payload, "id": "q_non_existent_999"})
    assert res_fake.status_code == 404

    # 2. Update without passing 'code' preserves the existing code (does NOT generate random new code)
    res_no_code = client.post("/admin/questions", headers=admin_h, json={
        "id": qid,
        "prompt": "Vận tốc trong chuyển động thẳng đều như thế nào?",
        "optionA": "Không thay đổi theo thời gian",
        "optionB": "Tăng dần",
        "optionC": "Giảm dần",
        "optionD": "Bằng 0",
        "correctOption": "A"
    })
    assert res_no_code.status_code == 200
    updated = res_no_code.json()
    assert updated["id"] == qid
    assert updated["code"] == "PHY_VER_01", f"Code was changed to {updated['code']} instead of preserving PHY_VER_01!"
    assert updated["version"] == 2
    assert updated["createdAt"] == created_at
    # Source, author, license preserved when not overwritten
    assert updated["source"] == "Sách GK"
    assert updated["author"] == "Biên soạn A"
    assert updated["license"] == "Nội bộ"

    # 3. Create another question with code PHY_VER_02
    res2 = client.post("/admin/questions", headers=admin_h, json={
        **payload,
        "code": "PHY_VER_02",
        "id": None,
        "prompt": "Một câu hỏi khác?"
    })
    assert res2.status_code == 200
    qid2 = res2.json()["id"]

    # 4. Attempt to change PHY_VER_02 code to PHY_VER_01 returns 409 CODE_CONFLICT
    res_conflict = client.post("/admin/questions", headers=admin_h, json={
        "id": qid2,
        "code": "PHY_VER_01",
        "prompt": "Một câu hỏi khác?",
        "optionA": "A1", "optionB": "B1", "optionC": "C1", "optionD": "D1",
        "correctOption": "A"
    })
    assert res_conflict.status_code == 409


def test_publish_and_unpublish_rules(tmp_path):
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # 1. Non-existent question ID returns 404
    res_fake = client.post("/admin/questions/q_fake_123/publish?publish=true", headers=admin_h)
    assert res_fake.status_code == 404

    # 2. Cannot publish an invalid draft question
    # (Insert invalid row in DB directly to simulate corrupted/incomplete draft)
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    bad_qid = "q_corrupted_draft"
    cur.execute("""
    INSERT INTO questions (id, code, version, subject_id, grade, chapter, topic, prompt, option_a, option_b, option_c, option_d, correct_option, explanation, points, status, created_at, updated_at)
    VALUES (?, 'BAD_01', 1, 'physics', 10, 'Chương 1', 'Động học', '', 'A', 'B', 'C', 'D', 'INVALID', '', 9999, 'DRAFT', ?, ?)
    """, (bad_qid, time.time(), time.time()))
    conn.commit()
    conn.close()

    res_pub_bad = client.post(f"/admin/questions/{bad_qid}/publish?publish=true", headers=admin_h)
    assert res_pub_bad.status_code == 400

    # 3. Valid question publish & unpublish is idempotent and does not reset version
    res_create = client.post("/admin/questions", headers=admin_h, json={
        "code": "Q_PUB_01",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1",
        "topic": "Động học",
        "prompt": "Phương trình tọa độ?",
        "optionA": "x = x0 + vt", "optionB": "v = at", "optionC": "s = vt", "optionD": "a = 0",
        "correctOption": "A",
        "points": 10,
        "status": "DRAFT"
    })
    assert res_create.status_code == 200
    valid_qid = res_create.json()["id"]
    assert res_create.json()["version"] == 1

    # Publish
    res_p1 = client.post(f"/admin/questions/{valid_qid}/publish?publish=true", headers=admin_h)
    assert res_p1.status_code == 200
    assert res_p1.json()["status"] == "PUBLISHED"
    assert res_p1.json()["version"] == 1

    # Idempotent publish
    res_p2 = client.post(f"/admin/questions/{valid_qid}/publish?publish=true", headers=admin_h)
    assert res_p2.status_code == 200
    assert res_p2.json()["status"] == "PUBLISHED"
    assert res_p2.json()["version"] == 1

    # Unpublish
    res_un = client.post(f"/admin/questions/{valid_qid}/publish?publish=false", headers=admin_h)
    assert res_un.status_code == 200
    assert res_un.json()["status"] == "DRAFT"
    assert res_un.json()["version"] == 1


# =========================================================================
# GROUP 4: FILTERS & METADATA
# =========================================================================

def test_admin_filtering_and_metadata_preservation(tmp_path):
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # Seed two questions: one Physics EASY Grade 10 PUBLISHED, one Math HARD Grade 11 DRAFT
    client.post("/admin/questions", headers=admin_h, json={
        "code": "PHY_FLT_01",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1",
        "topic": "Động học",
        "prompt": "Vật lý câu 1?",
        "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
        "correctOption": "A",
        "difficulty": "EASY",
        "points": 10,
        "status": "PUBLISHED"
    })
    client.post("/admin/questions", headers=admin_h, json={
        "code": "MTH_FLT_01",
        "subjectId": "math",
        "grade": 11,
        "chapter": "Chương Lượng giác",
        "topic": "Hàm số lượng giác",
        "prompt": "Toán câu 1: sin(0) = ?",
        "optionA": "0", "optionB": "1", "optionC": "-1", "optionD": "2",
        "correctOption": "A",
        "difficulty": "HARD",
        "points": 10,
        "status": "DRAFT"
    })

    # Filter by subject alias
    res_sub = client.get("/admin/questions?subject_id=vat ly", headers=admin_h)
    assert res_sub.status_code == 200
    assert any(q["code"] == "PHY_FLT_01" for q in res_sub.json())
    assert not any(q["code"] == "MTH_FLT_01" for q in res_sub.json())

    # Filter by grade
    res_gr = client.get("/admin/questions?grade=11", headers=admin_h)
    assert res_gr.status_code == 200
    assert all(q["grade"] == 11 for q in res_gr.json())

    # Filter by status
    res_st = client.get("/admin/questions?status=DRAFT", headers=admin_h)
    assert res_st.status_code == 200
    assert all(q["status"] == "DRAFT" for q in res_st.json())

    # Filter by difficulty
    res_diff = client.get("/admin/questions?difficulty=HARD", headers=admin_h)
    assert res_diff.status_code == 200
    assert all(q["difficulty"] == "HARD" for q in res_diff.json())

    # SQL injection safety
    res_inj = client.get("/admin/questions?chapter=' OR 1=1 --", headers=admin_h)
    assert res_inj.status_code == 200
    assert len(res_inj.json()) == 0


# =========================================================================
# GROUP 5: PIPELINE FROM PUBLISHED QUESTIONS TO STUDENT LIBRARY
# =========================================================================

def test_quiz_management_connects_published_questions_to_student_library(tmp_path):
    """
    Verifies that admin can bundle published questions into a library quiz,
    and student can immediately see and practice this quiz via /library/quizzes.
    Draft quizzes must remain hidden from students until published.
    """
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # 1. Create and publish 2 questions
    q1 = client.post("/admin/questions", headers=admin_h, json={
        "code": "PIPE_Q1", "subjectId": "physics", "grade": 10,
        "chapter": "Động học", "topic": "Vận tốc",
        "prompt": "Câu hỏi nối ống 1?",
        "optionA": "A1", "optionB": "B1", "optionC": "C1", "optionD": "D1",
        "correctOption": "A", "status": "PUBLISHED"
    }).json()

    q2 = client.post("/admin/questions", headers=admin_h, json={
        "code": "PIPE_Q2", "subjectId": "physics", "grade": 10,
        "chapter": "Động học", "topic": "Gia tốc",
        "prompt": "Câu hỏi nối ống 2?",
        "optionA": "A2", "optionB": "B2", "optionC": "C2", "optionD": "D2",
        "correctOption": "B", "status": "PUBLISHED"
    }).json()

    # 2. Admin creates a quiz containing these 2 questions (status DRAFT)
    res_quiz = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề Ôn tập Động học Lớp 10 Mới",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1",
        "topic": "Động học",
        "difficulty": "MEDIUM",
        "questionIds": [q1["id"], q2["id"]],
        "status": "DRAFT"
    })
    assert res_quiz.status_code == 201, res_quiz.text
    quiz_id = res_quiz.json()["id"]

    # Student cannot see draft quiz
    res_list_stu = client.get("/library/quizzes?subject_id=physics&grade=10", headers=student_h)
    assert not any(q["id"] == quiz_id for q in res_list_stu.json())

    # Student direct lookup of draft quiz returns 404
    res_get_stu = client.get(f"/library/quizzes/{quiz_id}", headers=student_h)
    assert res_get_stu.status_code == 404

    # 3. Admin publishes the quiz
    res_pub = client.post(f"/admin/quizzes/{quiz_id}/publish?publish=true", headers=admin_h)
    assert res_pub.status_code == 200
    assert res_pub.json()["status"] == "PUBLISHED"

    # Now student can see the quiz in library list!
    res_list_stu2 = client.get("/library/quizzes?subject_id=physics&grade=10", headers=student_h)
    assert any(q["id"] == quiz_id for q in res_list_stu2.json())

    # Student can fetch quiz details (no answers leaked)
    res_quiz_detail = client.get(f"/library/quizzes/{quiz_id}", headers=student_h)
    assert res_quiz_detail.status_code == 200
    detail = res_quiz_detail.json()
    assert len(detail["questions"]) == 2
    for q_item in detail["questions"]:
        assert "correctOption" not in q_item
        assert "explanation" not in q_item

    # Student can start session and answer questions
    res_sess = client.post("/library/sessions", headers=student_h, json={"quizId": quiz_id})
    assert res_sess.status_code == 201
    sess_id = res_sess.json()["id"]

    res_ans1 = client.post(f"/library/sessions/{sess_id}/answers", headers=student_h, json={
        "questionId": q1["id"],
        "selectedOption": "A"
    })
    assert res_ans1.status_code == 200
    assert res_ans1.json()["answers"][q1["id"]]["isCorrect"] is True


# =========================================================================
# GROUP 6: DRAFT ISOLATION & ERROR RESPONSES
# =========================================================================

def test_draft_questions_not_leaked_in_library_or_errors(tmp_path):
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # 1. Create one PUBLISHED and one DRAFT question
    res_pub = client.post("/admin/questions", headers=admin_h, json={
        "code": "LEAK_PUB_01", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi công khai?", "optionA": "A1", "optionB": "B1", "optionC": "C1", "optionD": "D1",
        "correctOption": "A", "status": "PUBLISHED"
    })
    qid_pub = res_pub.json()["id"]

    res_draft = client.post("/admin/questions", headers=admin_h, json={
        "code": "LEAK_SECRET_DRAFT", "subjectId": "physics", "grade": 10,
        "prompt": "BÍ MẬT ĐỀ THI KHÔNG ĐƯỢC LỘ?", "optionA": "A2", "optionB": "B2", "optionC": "C2", "optionD": "D2",
        "correctOption": "B", "status": "DRAFT"
    })
    qid_draft = res_draft.json()["id"]

    # 2. Put both questions in a library quiz
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    quiz_id = "quiz_test_leak_01"
    cur.execute("""
    INSERT INTO library_quizzes (id, title, subject_id, grade, chapter, topic, difficulty, question_ids, status, created_at)
    VALUES (?, ?, 'physics', 10, 'Chương 1', 'Động học', 'MEDIUM', ?, 'PUBLISHED', ?)
    """, (quiz_id, "Bộ đề kiểm tra rò rỉ", json.dumps([qid_pub, qid_draft]), time.time()))
    conn.commit()
    conn.close()

    # 3. Student fetches quiz containing a draft question:
    # Under strict integrity rules (Lỗi A), a quiz with any draft/missing/mismatched question
    # is ineligible for students and returns 404 QUIZ_NOT_FOUND, never leaking draft content!
    res_stu = client.get(f"/library/quizzes/{quiz_id}", headers=student_h)
    assert res_stu.status_code == 404
    assert "BÍ MẬT ĐỀ THI" not in res_stu.text

    # Student listing quizzes must not see this contaminated quiz either
    res_list = client.get("/library/quizzes?grade=10&subjectId=physics", headers=student_h)
    assert res_list.status_code == 200
    assert not any(q["id"] == quiz_id for q in res_list.json())

    # 4. Student queries invalid session or non-existent quiz; error response must not leak draft data
    res_err = client.get(f"/library/quizzes/quiz_non_existent", headers=student_h)
    assert res_err.status_code == 404
    assert "BÍ MẬT" not in res_err.text

    # 5. Legitimate quiz with only PUBLISHED questions: works normally and student never sees draft questions
    res_legit_quiz = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề chuẩn vật lý 10",
        "subjectId": "physics",
        "grade": 10,
        "questionIds": [qid_pub],
        "status": "PUBLISHED"
    })
    assert res_legit_quiz.status_code in (200, 201)
    legit_quiz_id = res_legit_quiz.json()["id"]

    res_legit_stu = client.get(f"/library/quizzes/{legit_quiz_id}", headers=student_h)
    assert res_legit_stu.status_code == 200
    legit_view = res_legit_stu.json()
    assert len(legit_view["questions"]) == 1
    assert legit_view["questions"][0]["id"] == qid_pub
    assert "BÍ MẬT ĐỀ THI" not in json.dumps(legit_view, ensure_ascii=False)


# =========================================================================
# GROUP 7: QUIZ MANAGEMENT EDGE CASES & VALIDATION
# =========================================================================

def test_quiz_creation_and_publish_validation(tmp_path):
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # Create 1 published and 1 draft question
    q_pub = client.post("/admin/questions", headers=admin_h, json={
        "code": "Q_PUB_VAL", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi đã duyệt?", "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
        "correctOption": "A", "status": "PUBLISHED"
    }).json()["id"]

    q_draft = client.post("/admin/questions", headers=admin_h, json={
        "code": "Q_DRAFT_VAL", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi nháp?", "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
        "correctOption": "A", "status": "DRAFT"
    }).json()["id"]

    # 1. Empty title rejected
    res1 = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "   ", "subjectId": "physics", "grade": 10, "questionIds": [q_pub]
    })
    assert res1.status_code in (400, 422)

    # 2. Empty questionIds rejected
    res2 = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề không có câu", "subjectId": "physics", "grade": 10, "questionIds": []
    })
    assert res2.status_code in (400, 422)

    # 3. Non-existent question ID rejected
    res3 = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề câu ảo", "subjectId": "physics", "grade": 10, "questionIds": ["q_ghost_question"]
    })
    assert res3.status_code == 404

    # 4. Cannot create a PUBLISHED quiz containing DRAFT questions
    res4 = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề chứa nháp", "subjectId": "physics", "grade": 10,
        "questionIds": [q_pub, q_draft], "status": "PUBLISHED"
    })
    assert res4.status_code == 400
    assert "DRAFT" in res4.json().get("message", "")

    # 5. Can create a DRAFT quiz containing draft and published questions
    res5 = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề nháp hợp lệ", "subjectId": "physics", "grade": 10,
        "questionIds": [q_pub, q_draft], "status": "DRAFT"
    })
    assert res5.status_code == 201
    draft_quiz_id = res5.json()["id"]

    # 6. Cannot publish a quiz if all questions are DRAFT
    res_draft_only = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề chỉ có nháp", "subjectId": "physics", "grade": 10,
        "questionIds": [q_draft], "status": "DRAFT"
    })
    assert res_draft_only.status_code == 201
    draft_only_quiz_id = res_draft_only.json()["id"]

    res_pub_draft_only = client.post(f"/admin/quizzes/{draft_only_quiz_id}/publish?publish=true", headers=admin_h)
    assert res_pub_draft_only.status_code == 400

    # 7. Non-existent quiz ID publish returns 404
    res_fake_pub = client.post("/admin/quizzes/quiz_fake_id_999/publish?publish=true", headers=admin_h)
    assert res_fake_pub.status_code == 404


# =========================================================================
# GROUP 8: LEGACY SESSIONS & UNICODE / FORMULA PRESERVATION
# =========================================================================

def test_legacy_session_snapshot_graceful_fallback(tmp_path):
    """
    LỖI B Verification:
    Ensures that legacy sessions lacking snapshot data:
    1. For completed legacy sessions:
       - Retains historical score, isCorrect, and selectedOption.
       - NEVER assigns current/mutated question answers or explanations as historical truth!
       - Returns correctOption=None and informative explanation.
       - Even if question in questions table was deleted or mutated to answer B, score remains 10, isCorrect remains True.
    2. For in-progress legacy sessions:
       - Refuses to grade against mutated questions table with 400 LEGACY_SESSION_REQUIRES_RESTART.
       - Keeps the session record intact without corrupting it.
    """
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # 1. Question in DB created with answer A
    q = client.post("/admin/questions", headers=admin_h, json={
        "code": "PHY_LEGACY_01", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi lịch sử?", "optionA": "A1", "optionB": "B1", "optionC": "C1", "optionD": "D1",
        "correctOption": "A", "points": 10, "status": "PUBLISHED", "explanation": "Giải thích v1"
    }).json()
    qid = q["id"]

    # 2. Insert a COMPLETED legacy session manually into practice_sessions where snapshot lacks correctOption
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    legacy_sess_id = "psess_legacy_completed_01"
    legacy_snapshot = [{
        "id": qid,
        "code": "PHY_LEGACY_01",
        "chapter": "Chương 1",
        "topic": "Động học",
        "prompt": "Câu hỏi lịch sử?",
        "options": [{"id": "A", "text": "A1"}, {"id": "B", "text": "B1"}, {"id": "C", "text": "C1"}, {"id": "D", "text": "D1"}],
        "difficulty": "EASY",
        "points": 10
    }]
    completed_answers = {
        qid: {
            "selectedOption": "A",
            "isCorrect": True,
            "answeredAt": time.time()
        }
    }
    cur.execute("""
    INSERT INTO practice_sessions (id, user_id, quiz_id, subject_id, grade, question_snapshots, step, answers, state, score, correct_count, total_count, created_at, updated_at)
    VALUES (?, 1, 'quiz_legacy', 'physics', 10, ?, 1, ?, 'COMPLETED', 10, 1, 1, ?, ?)
    """, (legacy_sess_id, json.dumps(legacy_snapshot), json.dumps(completed_answers), time.time(), time.time()))
    conn.commit()
    conn.close()

    # 3. Admin modifies question in DB to answer B, points 50, explanation v2
    client.post("/admin/questions", headers=admin_h, json={
        "id": qid,
        "prompt": "Câu hỏi lịch sử đã bị đổi sang v2?",
        "optionA": "A1", "optionB": "B1", "optionC": "C1", "optionD": "D1",
        "correctOption": "B",
        "points": 50,
        "explanation": "Giải thích mới v2 hoàn toàn khác"
    })

    # 4. Read completed legacy session:
    # Must NOT display current answer 'B' or 'Giải thích mới v2' as historical truth!
    # Must preserve score=10 and isCorrect=True!
    res_get = client.get(f"/library/sessions/{legacy_sess_id}", headers=student_h)
    assert res_get.status_code == 200
    sess_hist = res_get.json()
    assert sess_hist["score"] == 10
    assert sess_hist["correctCount"] == 1
    q0 = sess_hist["questions"][0]
    assert q0["userAnswer"] == "A"
    assert q0["isCorrect"] is True
    # Crucial: correctOption is None (not 'B'!), explanation is not v2
    assert q0["correctOption"] is None
    assert "v2" not in (q0.get("explanation") or "")
    assert "không khả dụng" in q0["explanation"]

    # 5. In-progress legacy session: submitting answer returns 400 LEGACY_SESSION_REQUIRES_RESTART
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    legacy_prog_id = "psess_legacy_inprogress_01"
    cur.execute("""
    INSERT INTO practice_sessions (id, user_id, quiz_id, subject_id, grade, question_snapshots, step, answers, state, score, correct_count, total_count, created_at, updated_at)
    VALUES (?, 1, 'quiz_legacy', 'physics', 10, ?, 0, '{}', 'IN_PROGRESS', 0, 0, 1, ?, ?)
    """, (legacy_prog_id, json.dumps(legacy_snapshot), time.time(), time.time()))
    conn.commit()
    conn.close()

    res_sub_legacy = client.post(f"/library/sessions/{legacy_prog_id}/answers", headers=student_h, json={
        "questionId": qid,
        "selectedOption": "A"
    })
    assert res_sub_legacy.status_code == 400
    assert res_sub_legacy.json()["code"] == "LEGACY_SESSION_REQUIRES_RESTART"


def test_quiz_publish_endpoint_rejects_draft_questions_and_demotes_on_unpublish(tmp_path):
    """
    LỖI A Verification:
    1. Create Quiz in DRAFT with 1 PUBLISHED question and 1 DRAFT question.
    2. Call POST /admin/quizzes/{id}/publish?publish=true.
    3. MUST reject with 400 (not 200!).
    4. Quiz remains in DRAFT.
    5. After publishing both questions, publishing quiz succeeds.
    6. When one question is subsequently unpublished by admin, quiz is automatically demoted to DRAFT
       and students cannot start an incomplete / truncated quiz.
    """
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # Question A (PUBLISHED)
    qA = client.post("/admin/questions", headers=admin_h, json={
        "code": "QUIZ_ERR_A", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi A?", "optionA": "A1", "optionB": "B1", "optionC": "C1", "optionD": "D1",
        "correctOption": "A", "status": "PUBLISHED"
    }).json()["id"]

    # Question B (DRAFT)
    qB = client.post("/admin/questions", headers=admin_h, json={
        "code": "QUIZ_ERR_B", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi B nháp?", "optionA": "A2", "optionB": "B2", "optionC": "C2", "optionD": "D2",
        "correctOption": "B", "status": "DRAFT"
    }).json()["id"]

    # Create DRAFT quiz with [qA, qB]
    res_create = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề kiểm tra Lỗi A", "subjectId": "physics", "grade": 10,
        "questionIds": [qA, qB], "status": "DRAFT"
    })
    assert res_create.status_code == 201
    quiz_id = res_create.json()["id"]

    # Attempt to publish the quiz via POST /admin/quizzes/{quiz_id}/publish?publish=true
    # In buggy code: this returned 200 PUBLISHED because it only checked len(q_rows) > 0!
    res_pub = client.post(f"/admin/quizzes/{quiz_id}/publish?publish=true", headers=admin_h)
    assert res_pub.status_code == 400
    assert "DRAFT" in res_pub.json().get("message", "")

    # Verify quiz is still DRAFT
    res_chk = client.get("/admin/quizzes", headers=admin_h)
    quiz_row = next(q for q in res_chk.json() if q["id"] == quiz_id)
    assert quiz_row["status"] == "DRAFT"

    # Now publish question B so all questions are PUBLISHED
    client.post(f"/admin/questions/{qB}/publish?publish=true", headers=admin_h)

    # Now publishing quiz succeeds
    res_pub2 = client.post(f"/admin/quizzes/{quiz_id}/publish?publish=true", headers=admin_h)
    assert res_pub2.status_code == 200
    assert res_pub2.json()["status"] == "PUBLISHED"

    # Student can see the quiz
    res_stu_quizzes = client.get("/library/quizzes?subject_id=physics&grade=10", headers=student_h)
    assert any(q["id"] == quiz_id for q in res_stu_quizzes.json())

    # Admin unpublishes question B
    client.post(f"/admin/questions/{qB}/publish?publish=false", headers=admin_h)

    # Quiz is automatically demoted to DRAFT and hidden from students
    res_stu_after = client.get("/library/quizzes?subject_id=physics&grade=10", headers=student_h)
    assert not any(q["id"] == quiz_id for q in res_stu_after.json())

    # Student direct lookup or start_session fails with 404/400 (never truncated session)
    res_stu_detail = client.get(f"/library/quizzes/{quiz_id}", headers=student_h)
    assert res_stu_detail.status_code == 404

    res_stu_start = client.post("/library/sessions", headers=student_h, json={"quizId": quiz_id})
    assert res_stu_start.status_code in (400, 404)


def test_quiz_and_question_subject_grade_mismatch_and_conflict_guard(tmp_path):
    """
    LỖI C Verification:
    1. Reject quiz containing questions with mismatched subject (e.g. physics in math quiz).
    2. Reject quiz containing questions with mismatched grade (e.g. grade 10 in grade 12 quiz).
    3. Reject duplicate question IDs in quiz payload.
    4. Guard question edit: changing subject/grade of a question that belongs to a PUBLISHED quiz is blocked (409).
    """
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # 1. Physics Grade 10 question
    q_phy10 = client.post("/admin/questions", headers=admin_h, json={
        "code": "MISMATCH_PHY10", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi Lý 10?", "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
        "correctOption": "A", "status": "PUBLISHED"
    }).json()["id"]

    # 2. Attempt to create Math Grade 12 quiz with the Physics Grade 10 question -> 400
    res_bad_sub = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề Toán nhưng chứa Lý", "subjectId": "math", "grade": 12,
        "questionIds": [q_phy10], "status": "DRAFT"
    })
    assert res_bad_sub.status_code == 400
    assert "không khớp với môn" in res_bad_sub.json().get("message", "")

    # 3. Attempt to create Physics Grade 12 quiz with the Physics Grade 10 question -> 400
    res_bad_gr = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề Lý 12 nhưng chứa Lý 10", "subjectId": "physics", "grade": 12,
        "questionIds": [q_phy10], "status": "DRAFT"
    })
    assert res_bad_gr.status_code == 400
    assert "không khớp với môn" in res_bad_gr.json().get("message", "")

    # 4. Attempt to create quiz with duplicate IDs -> 400
    res_dup = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề trùng ID", "subjectId": "physics", "grade": 10,
        "questionIds": [q_phy10, q_phy10], "status": "DRAFT"
    })
    assert res_dup.status_code == 400
    assert "trùng lặp" in res_dup.json().get("message", "")

    # 5. Create valid published quiz with q_phy10
    res_ok = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề Lý 10 Chuẩn", "subjectId": "physics", "grade": 10,
        "questionIds": [q_phy10], "status": "PUBLISHED"
    })
    assert res_ok.status_code == 201

    # 6. Admin attempts to edit q_phy10 to subjectId="chemistry" -> 409 CONFLICT_QUIZ_MEMBERSHIP
    res_edit_sub = client.post("/admin/questions", headers=admin_h, json={
        "id": q_phy10,
        "subjectId": "chemistry",
        "grade": 10,
        "prompt": "Câu hỏi Lý 10 sửa sang Hóa?",
        "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
        "correctOption": "A"
    })
    assert res_edit_sub.status_code == 409
    assert res_edit_sub.json()["code"] == "CONFLICT_QUIZ_MEMBERSHIP"


def test_unicode_and_latex_math_formulas_preserved(tmp_path):
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    formula_prompt = "Phương trình chuyển động thẳng biến đổi đều: $s = v_0 t + \\frac{1}{2}at^2$ (với $v_0 = 2\\text{ m/s}$, $a = 1\\text{ m/s}^2$). Quãng đường sau $t = 4\\text{ s}$ là:"
    explanation_latex = "Thay số: $s = 2 \\times 4 + \\frac{1}{2} \\times 1 \\times 4^2 = 8 + 8 = 16\\text{ m}$."

    q_res = client.post("/admin/questions", headers=admin_h, json={
        "code": "PHY_LATEX_01",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1: Động học",
        "topic": "Chuyển động thẳng biến đổi đều",
        "prompt": formula_prompt,
        "optionA": "16 m",
        "optionB": "8 m",
        "optionC": "24 m",
        "optionD": "12 m",
        "correctOption": "A",
        "explanation": explanation_latex,
        "difficulty": "MEDIUM",
        "points": 10,
        "status": "PUBLISHED"
    })
    assert q_res.status_code == 200
    qid = q_res.json()["id"]

    # Verify admin get preserves formulas
    res_get_adm = client.get(f"/admin/questions?subject_id=physics", headers=admin_h)
    assert res_get_adm.status_code == 200
    adm_q = next(item for item in res_get_adm.json() if item["id"] == qid)
    assert adm_q["prompt"] == formula_prompt
    assert adm_q["explanation"] == explanation_latex

    # Create quiz and start session
    res_quiz = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề Công thức LaTeX Vật lý 10",
        "subjectId": "physics",
        "grade": 10,
        "questionIds": [qid],
        "status": "PUBLISHED"
    })
    assert res_quiz.status_code == 201
    quiz_id = res_quiz.json()["id"]

    res_sess = client.post("/library/sessions", headers=student_h, json={"quizId": quiz_id})
    assert res_sess.status_code == 201
    sess_id = res_sess.json()["id"]
    assert res_sess.json()["questions"][0]["prompt"] == formula_prompt

    # Submit answer
    res_ans = client.post(f"/library/sessions/{sess_id}/answers", headers=student_h, json={
        "questionId": qid,
        "selectedOption": "A"
    })
    assert res_ans.status_code == 200
    q_answered = res_ans.json()["questions"][0]
    assert q_answered["prompt"] == formula_prompt
    assert q_answered["explanation"] == explanation_latex


def test_e2e_full_admin_to_student_library_and_session_lifecycle(tmp_path):
    """
    End-to-End Product Flow:
    1. Admin creates questions and publishes them.
    2. Admin creates and publishes quiz via API.
    3. Student discovers quiz on /library/quizzes.
    4. Student starts session, verifies answers/explanations not leaked.
    5. Admin updates a question to v2 (points 20, answer B) and unpublishes another question.
    6. Student submits answer A in the existing session: graded against v1 (score 10, correct).
    7. Student completes session: score is 10.
    8. New sessions cannot be started on the quiz with unpublished question (prevent truncation).
    """
    client, admin_h, teacher_h, student_h, db_file = setup_client(tmp_path)

    # 1. Admin creates 2 published questions
    q1 = client.post("/admin/questions", headers=admin_h, json={
        "code": "E2E_Q01", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi E2E 1: Gia tốc a = ?", "optionA": "0", "optionB": "1", "optionC": "2", "optionD": "3",
        "correctOption": "A", "points": 10, "status": "PUBLISHED", "explanation": "Giải thích 1 v1"
    }).json()["id"]

    q2 = client.post("/admin/questions", headers=admin_h, json={
        "code": "E2E_Q02", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi E2E 2: Vận tốc v = ?", "optionA": "10 m/s", "optionB": "20 m/s", "optionC": "30 m/s", "optionD": "40 m/s",
        "correctOption": "B", "points": 10, "status": "PUBLISHED", "explanation": "Giải thích 2 v1"
    }).json()["id"]

    # 2. Admin creates and publishes quiz
    res_quiz = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề Luyện tập E2E Toàn luồng",
        "subjectId": "physics", "grade": 10,
        "chapter": "Động học", "topic": "Chuyển động thẳng",
        "difficulty": "EASY",
        "questionIds": [q1, q2],
        "status": "PUBLISHED"
    })
    assert res_quiz.status_code == 201
    quiz_id = res_quiz.json()["id"]

    # 3. Student views library quizzes
    res_lib = client.get("/library/quizzes?subject_id=physics&grade=10", headers=student_h)
    assert res_lib.status_code == 200
    assert any(q["id"] == quiz_id for q in res_lib.json())

    # 4. Student starts session
    res_sess = client.post("/library/sessions", headers=student_h, json={"quizId": quiz_id})
    assert res_sess.status_code == 201
    sess_id = res_sess.json()["id"]

    for q_item in res_sess.json()["questions"]:
        assert "correctOption" not in q_item
        assert "explanation" not in q_item

    # 5. Admin updates q1 to answer B, points 20, explanation v2
    client.post("/admin/questions", headers=admin_h, json={
        "id": q1,
        "prompt": "Câu hỏi E2E 1: Gia tốc a = ?",
        "optionA": "0", "optionB": "1", "optionC": "2", "optionD": "3",
        "correctOption": "B", "points": 20, "explanation": "Giải thích 1 v2"
    })

    # Admin unpublishes q2
    client.post(f"/admin/questions/{q2}/publish?publish=false", headers=admin_h)

    # 6. Student answers q1 with 'A' in existing session: graded against v1!
    res_ans1 = client.post(f"/library/sessions/{sess_id}/answers", headers=student_h, json={
        "questionId": q1, "selectedOption": "A"
    })
    assert res_ans1.status_code == 200
    assert res_ans1.json()["answers"][q1]["isCorrect"] is True
    assert res_ans1.json()["score"] == 10
    assert res_ans1.json()["questions"][0]["correctOption"] == "A"
    assert res_ans1.json()["questions"][0]["explanation"] == "Giải thích 1 v1"

    # Student answers q2 with 'B' in existing session (uses snapshot even though q2 was unpublished!)
    res_ans2 = client.post(f"/library/sessions/{sess_id}/answers", headers=student_h, json={
        "questionId": q2, "selectedOption": "B"
    })
    assert res_ans2.status_code == 200
    assert res_ans2.json()["answers"][q2]["isCorrect"] is True
    assert res_ans2.json()["score"] == 20

    # Student completes session
    res_comp = client.post(f"/library/sessions/{sess_id}/complete", headers=student_h)
    assert res_comp.status_code == 200
    assert res_comp.json()["state"] == "COMPLETED"
    assert res_comp.json()["score"] == 20

    # 7. New sessions cannot be opened on the quiz because q2 was unpublished (prevents silent truncation!)
    res_new_sess = client.post("/library/sessions", headers=student_h, json={"quizId": quiz_id})
    assert res_new_sess.status_code in (400, 404)
