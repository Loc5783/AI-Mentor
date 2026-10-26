import os
import sys
import json
import sqlite3
import pytest
from fastapi.testclient import TestClient

TESTS_DIR = os.path.dirname(os.path.abspath(__file__))
if TESTS_DIR not in sys.path:
    sys.path.insert(0, TESTS_DIR)

from aimentor.config import Settings
from aimentor.main import create_app
from aimentor.database import Database
from test_api import signup, headers

def setup_client(tmp_path):
    settings = Settings(tmp_path)
    app = create_app(settings)
    client = TestClient(app)
    return client, tmp_path

def create_user(client, name, role="STUDENT", tmp_path=None):
    user = signup(client, name)
    user_id = user["userId"]

    if role != "STUDENT" and tmp_path:
        db_path = tmp_path / "aimentor.sqlite3"
        conn = sqlite3.connect(db_path)
        conn.execute("UPDATE users SET role=? WHERE id=?", (role, user_id))
        conn.commit()
        conn.close()

    return {
        "id": user_id,
        "name": name,
        "token": user["accessToken"],
        "headers": headers(user)
    }

def seed_quiz_with_questions(client, admin):
    # Create 3 questions for Physics Grade 10: Q1 (B), Q2 (A), Q3 (C)
    q_ids = []
    for idx, (correct_opt, pts) in enumerate([("B", 10), ("A", 15), ("C", 20)], start=1):
        payload = {
            "code": f"PHY10_STEP4_Q0{idx}",
            "subjectId": "physics",
            "grade": 10,
            "chapter": "Chương 1",
            "topic": "Động học",
            "prompt": f"Nội dung câu hỏi kiểm chứng Step 4 số {idx}?",
            "optionA": f"Phương án A {idx}",
            "optionB": f"Phương án B {idx}",
            "optionC": f"Phương án C {idx}",
            "optionD": f"Phương án D {idx}",
            "correctOption": correct_opt,
            "explanation": f"Lời giải chi tiết câu hỏi số {idx}",
            "difficulty": "EASY",
            "points": pts
        }
        res = client.post("/admin/questions", headers=admin["headers"], json=payload)
        assert res.status_code in (200, 201), res.text
        qid = res.json()["id"]
        res_pub = client.post(f"/admin/questions/{qid}/publish?publish=true", headers=admin["headers"])
        assert res_pub.status_code == 200, res_pub.text
        q_ids.append(qid)

    quiz_payload = {
        "title": "Bộ đề kiểm chứng Step 4 Vật lý 10",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1",
        "topic": "Động học",
        "difficulty": "EASY",
        "questionIds": q_ids
    }
    res_quiz = client.post("/admin/quizzes", headers=admin["headers"], json=quiz_payload)
    assert res_quiz.status_code == 201, res_quiz.text
    quiz_id = res_quiz.json()["id"]

    res_pub_quiz = client.post(f"/admin/quizzes/{quiz_id}/publish?publish=true", headers=admin["headers"])
    assert res_pub_quiz.status_code == 200, res_pub_quiz.text

    return quiz_id, q_ids


def test_schema_migration_idempotent(tmp_path):
    db = Database(tmp_path)
    db.initialize()
    # Run initialize a second time to ensure idempotency
    db.initialize()

    with db.read() as conn:
        cols = {col["name"] for col in conn.execute("PRAGMA table_info(wrong_questions_notebook)").fetchall()}
        assert "question_snapshot" in cols
        assert "state" in cols
        assert "updated_at" in cols

        # Check indexes exist
        idx_res = conn.execute("SELECT name FROM sqlite_master WHERE type='index'").fetchall()
        idx_names = {r["name"] for r in idx_res}
        assert "idx_wrong_questions_user_subj" in idx_names
        assert "idx_practice_sessions_user_quiz" in idx_names


def test_migration_with_pre_step4_data(tmp_path):
    db_path = tmp_path / "aimentor.sqlite3"
    conn = sqlite3.connect(db_path)
    # 1. Manually set up pre-Step-4 schema without question_snapshot, state, updated_at
    conn.executescript("""
    CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        phone TEXT UNIQUE,
        email TEXT UNIQUE,
        password_hash TEXT NOT NULL,
        full_name TEXT NOT NULL,
        nickname TEXT,
        bio TEXT DEFAULT '',
        avatar_id TEXT,
        role TEXT NOT NULL DEFAULT 'STUDENT',
        rank_tier TEXT,
        qr_code_token TEXT,
        revision INTEGER NOT NULL DEFAULT 1,
        created_at REAL NOT NULL,
        updated_at REAL NOT NULL
    );
    CREATE TABLE practice_sessions (
        id TEXT PRIMARY KEY,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        quiz_id TEXT NOT NULL,
        subject_id TEXT NOT NULL,
        grade INTEGER NOT NULL,
        question_snapshots TEXT NOT NULL,
        step INTEGER NOT NULL DEFAULT 0,
        answers TEXT NOT NULL DEFAULT '{}',
        state TEXT NOT NULL DEFAULT 'IN_PROGRESS',
        score INTEGER NOT NULL DEFAULT 0,
        correct_count INTEGER NOT NULL DEFAULT 0,
        total_count INTEGER NOT NULL DEFAULT 0,
        created_at REAL NOT NULL,
        updated_at REAL NOT NULL
    );
    CREATE TABLE wrong_questions_notebook (
        id TEXT PRIMARY KEY,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        question_id TEXT NOT NULL,
        subject_id TEXT NOT NULL,
        grade INTEGER NOT NULL,
        selected_option TEXT NOT NULL,
        correct_option TEXT NOT NULL,
        explanation TEXT NOT NULL,
        created_at REAL NOT NULL,
        UNIQUE(user_id, question_id)
    );
    """)

    now = 1000.0
    conn.execute("""
    INSERT INTO users (id, phone, password_hash, full_name, role, created_at, updated_at)
    VALUES (1, '0988776655', 'hash', 'Legacy User', 'STUDENT', ?, ?)
    """, (now, now))

    conn.execute("""
    INSERT INTO practice_sessions (id, user_id, quiz_id, subject_id, grade, question_snapshots, step, answers, state, score, correct_count, total_count, created_at, updated_at)
    VALUES ('psess_legacy_01', 1, 'quiz_legacy', 'physics', 10, '[]', 1, '{"q_old": {"selectedOption": "A", "isCorrect": false}}', 'IN_PROGRESS', 0, 0, 1, ?, ?)
    """, (now, now))

    conn.execute("""
    INSERT INTO wrong_questions_notebook (id, user_id, question_id, subject_id, grade, selected_option, correct_option, explanation, created_at)
    VALUES ('wq_legacy_entry_01', 1, 'q_old', 'physics', 10, 'A', 'B', 'Giải thích câu cũ', ?)
    """, (now,))

    conn.commit()
    conn.close()

    # 2. Run Database.initialize() - triggers Step 4 idempotent migrations
    db = Database(tmp_path)
    db.initialize()

    # Verify columns and data preserved
    with db.read() as c:
        cols = {col["name"] for col in c.execute("PRAGMA table_info(wrong_questions_notebook)").fetchall()}
        assert "question_snapshot" in cols
        assert "state" in cols
        assert "updated_at" in cols

        # Check pre-existing data is completely preserved
        row = c.execute("SELECT * FROM wrong_questions_notebook WHERE id='wq_legacy_entry_01'").fetchone()
        assert row is not None
        assert row["id"] == "wq_legacy_entry_01"
        assert row["user_id"] == 1
        assert row["question_id"] == "q_old"
        assert row["selected_option"] == "A"
        assert row["correct_option"] == "B"
        assert row["explanation"] == "Giải thích câu cũ"
        assert row["created_at"] == now
        assert row["question_snapshot"] is None, "Legacy snapshot must be NULL, not backfilled with fake data"
        assert row["state"] == "NEEDS_REVIEW"

        # Check practice_session is intact
        sess = c.execute("SELECT * FROM practice_sessions WHERE id='psess_legacy_01'").fetchone()
        assert sess is not None
        assert sess["score"] == 0

    # 3. Run Database.initialize() a second time to ensure idempotency on migrated schema
    db.initialize()
    with db.read() as c:
        count = c.execute("SELECT COUNT(*) FROM wrong_questions_notebook").fetchone()[0]
        assert count == 1, "Duplicate records created on second migration run!"

    # 4. Boot app and verify legacy entry cannot retry and hasSnapshot is False
    settings = Settings(tmp_path)
    app = create_app(settings)
    client = TestClient(app)

    with app.state.db.transaction() as trans_conn:
        tokens = app.state.auth.mint(trans_conn, 1)

    legacy_headers = {"Authorization": f"Bearer {tokens['accessToken']}"}

    res_wq = client.get("/me/wrong-questions", headers=legacy_headers)
    assert res_wq.status_code == 200
    items = res_wq.json()
    assert len(items) == 1
    assert items[0]["id"] == "wq_legacy_entry_01"
    assert items[0]["hasSnapshot"] is False
    assert items[0]["canRetry"] is False

    # Attempting to retry legacy entry must be rejected with 400 LEGACY_ENTRY_CANNOT_RETRY
    res_retry = client.post(
        "/me/wrong-questions/wq_legacy_entry_01/retry",
        headers=legacy_headers,
        json={"selectedOption": "B"}
    )
    assert res_retry.status_code == 400
    err_body = res_retry.json()
    code = err_body.get("code") or (err_body.get("detail", {}).get("code") if isinstance(err_body.get("detail"), dict) else None)
    assert code == "LEGACY_ENTRY_CANNOT_RETRY" 


def test_session_resume_and_first_unanswered_index(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminStep4A", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentResumeA", role="STUDENT")

    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    # 1. Start session
    res_start = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id})
    assert res_start.status_code == 201
    sess_data = res_start.json()
    sess_id = sess_data["id"]
    assert sess_data["state"] == "IN_PROGRESS"
    assert sess_data["answeredCount"] == 0
    assert sess_data["firstUnansweredIndex"] == 0

    # 2. Answer question 1
    res_ans1 = client.post(
        f"/library/sessions/{sess_id}/answers",
        headers=student["headers"],
        json={"questionId": q_ids[0], "selectedOption": "B"}
    )
    assert res_ans1.status_code == 200
    ans1_data = res_ans1.json()
    assert ans1_data["answeredCount"] == 1
    assert ans1_data["firstUnansweredIndex"] == 1

    # 3. Simulate leaving and resuming: GET /library/sessions?quiz_id=...&state=IN_PROGRESS
    res_list = client.get(
        f"/library/sessions?quiz_id={quiz_id}&state=IN_PROGRESS",
        headers=student["headers"]
    )
    assert res_list.status_code == 200
    list_data = res_list.json()
    assert list_data["count"] == 1
    summary = list_data["items"][0]
    assert summary["id"] == sess_id
    assert summary["state"] == "IN_PROGRESS"
    assert summary["answeredCount"] == 1
    assert summary["firstUnansweredIndex"] == 1

    # Resume directly via GET /library/sessions/{session_id}
    res_resume = client.get(f"/library/sessions/{sess_id}", headers=student["headers"])
    assert res_resume.status_code == 200
    resumed_data = res_resume.json()
    assert resumed_data["id"] == sess_id
    assert resumed_data["firstUnansweredIndex"] == 1
    # Check question 1 already has userAnswer
    q1 = next(q for q in resumed_data["questions"] if q["id"] == q_ids[0])
    assert q1["userAnswer"] == "B"
    assert q1["isCorrect"] is True

    # 4. Answer question 2 and 3, then complete session
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[1], "selectedOption": "A"})
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[2], "selectedOption": "C"})

    res_comp = client.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
    assert res_comp.status_code == 200
    assert res_comp.json()["state"] == "COMPLETED"

    # Completed session must NOT appear as IN_PROGRESS
    res_in_prog = client.get(f"/library/sessions?quiz_id={quiz_id}&state=IN_PROGRESS", headers=student["headers"])
    assert res_in_prog.json()["count"] == 0

    # Completed session CANNOT accept further answers
    res_ans_after_comp = client.post(
        f"/library/sessions/{sess_id}/answers",
        headers=student["headers"],
        json={"questionId": q_ids[0], "selectedOption": "A"}
    )
    assert res_ans_after_comp.status_code == 409


def test_session_isolation(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminStep4B", role="ADMIN", tmp_path=tmp_path)
    student_a = create_user(client, "StudentUserA", role="STUDENT")
    student_b = create_user(client, "StudentUserB", role="STUDENT")

    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    # Student A starts a session
    res_a = client.post("/library/sessions", headers=student_a["headers"], json={"quizId": quiz_id})
    sess_a_id = res_a.json()["id"]

    # Student B queries active sessions: must NOT see Student A's session
    res_b_list = client.get(f"/library/sessions?quiz_id={quiz_id}", headers=student_b["headers"])
    assert res_b_list.status_code == 200
    assert res_b_list.json()["count"] == 0

    # Student B attempts to access Student A's session: must be 404
    res_b_get = client.get(f"/library/sessions/{sess_a_id}", headers=student_b["headers"])
    assert res_b_get.status_code == 404

    # Student B attempts to submit answer to Student A's session: must be 404
    res_b_sub = client.post(
        f"/library/sessions/{sess_a_id}/answers",
        headers=student_b["headers"],
        json={"questionId": q_ids[0], "selectedOption": "A"}
    )
    assert res_b_sub.status_code == 404


def test_wrong_question_snapshot_isolation(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminStep4C", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentMistakeA", role="STUDENT")

    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    # Student starts session and answers Q1 incorrectly: Q1 correct is B, student answers A
    res_start = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id})
    sess_id = res_start.json()["id"]

    res_ans = client.post(
        f"/library/sessions/{sess_id}/answers",
        headers=student["headers"],
        json={"questionId": q_ids[0], "selectedOption": "A"}
    )
    assert res_ans.status_code == 200

    # Check wrong question in student's notebook
    res_wq = client.get("/me/wrong-questions", headers=student["headers"])
    assert res_wq.status_code == 200
    items = res_wq.json()
    assert len(items) == 1
    wq_entry = items[0]
    assert wq_entry["questionId"] == q_ids[0]
    assert wq_entry["selectedOption"] == "A"
    assert wq_entry["correctOption"] == "B"
    assert wq_entry["hasSnapshot"] is True
    assert wq_entry["canRetry"] is True
    assert wq_entry["state"] == "NEEDS_REVIEW"
    assert "Nội dung câu hỏi kiểm chứng Step 4 số 1?" in wq_entry["prompt"]

    # Now, ADMIN MUTATES the question in database: changes prompt, changes correct option to D, unpublishes
    db_path = tmp_path / "aimentor.sqlite3"
    conn = sqlite3.connect(db_path)
    conn.execute("""
    UPDATE questions
    SET prompt='PROMPT ĐÃ BỊ ADMIN THAY ĐỔI HOÀN TOÀN',
        option_a='Phương án mới A',
        correct_option='D',
        status='DRAFT'
    WHERE id=?
    """, (q_ids[0],))
    conn.commit()
    conn.close()

    # Student checks wrong questions again: MUST PRESERVE FROZEN SNAPSHOT!
    res_wq2 = client.get("/me/wrong-questions", headers=student["headers"])
    assert res_wq2.status_code == 200
    items2 = res_wq2.json()
    assert len(items2) == 1
    wq_preserved = items2[0]
    assert "PROMPT ĐÃ BỊ ADMIN THAY ĐỔI" not in wq_preserved["prompt"]
    assert "Nội dung câu hỏi kiểm chứng Step 4 số 1?" in wq_preserved["prompt"]
    assert wq_preserved["correctOption"] == "B"


def test_wrong_question_retry_behavior(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminStep4D", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentRetryA", role="STUDENT")

    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    # Student makes mistake on Q1 (correct is B)
    res_start = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id})
    sess_id = res_start.json()["id"]
    client.post(
        f"/library/sessions/{sess_id}/answers",
        headers=student["headers"],
        json={"questionId": q_ids[0], "selectedOption": "D"}
    )

    wq_list = client.get("/me/wrong-questions", headers=student["headers"]).json()
    entry_id = wq_list[0]["id"]
    assert wq_list[0]["state"] == "NEEDS_REVIEW"

    # 1. Retry with incorrect answer "C": must stay NEEDS_REVIEW
    res_retry_fail = client.post(
        f"/me/wrong-questions/{entry_id}/retry",
        headers=student["headers"],
        json={"selectedOption": "C"}
    )
    assert res_retry_fail.status_code == 200
    fail_data = res_retry_fail.json()
    assert fail_data["isCorrect"] is False
    assert fail_data["state"] == "NEEDS_REVIEW"
    assert fail_data["correctOption"] == "B"

    # 2. Retry with correct answer "B": state must become RESOLVED
    res_retry_succ = client.post(
        f"/me/wrong-questions/{entry_id}/retry",
        headers=student["headers"],
        json={"selectedOption": "B"}
    )
    assert res_retry_succ.status_code == 200
    succ_data = res_retry_succ.json()
    assert succ_data["isCorrect"] is True
    assert succ_data["state"] == "RESOLVED"

    # 3. Check filters: ?state=RESOLVED vs ?state=NEEDS_REVIEW
    resolved_list = client.get("/me/wrong-questions?state=RESOLVED", headers=student["headers"]).json()
    assert len(resolved_list) == 1
    assert resolved_list[0]["state"] == "RESOLVED"

    needs_review_list = client.get("/me/wrong-questions?state=NEEDS_REVIEW", headers=student["headers"]).json()
    assert len(needs_review_list) == 0

    # 4. Verify practice session answers and score were NOT mutated by notebook retry
    sess_check = client.get(f"/library/sessions/{sess_id}", headers=student["headers"]).json()
    assert sess_check["answers"][q_ids[0]]["selectedOption"] == "D"
    assert sess_check["answers"][q_ids[0]]["isCorrect"] is False
    assert sess_check["score"] == 0


def test_wrong_question_legacy_entry_handling(tmp_path):
    client, _ = setup_client(tmp_path)
    student = create_user(client, "StudentLegacy", role="STUDENT")

    # Insert a legacy notebook entry directly into SQLite without question_snapshot
    db_path = tmp_path / "aimentor.sqlite3"
    conn = sqlite3.connect(db_path)
    conn.execute("""
    INSERT INTO wrong_questions_notebook (
        id, user_id, question_id, subject_id, grade, selected_option, correct_option, explanation,
        question_snapshot, state, created_at, updated_at
    ) VALUES (
        'wq_legacy_01', ?, 'q_legacy_old', 'physics', 10, 'A', 'B', 'Lời giải cũ',
        NULL, 'NEEDS_REVIEW', 1000, 1000
    )
    """, (student["id"],))
    conn.commit()
    conn.close()

    # GET /me/wrong-questions: must show hasSnapshot=False, canRetry=False
    res_list = client.get("/me/wrong-questions", headers=student["headers"])
    assert res_list.status_code == 200
    items = res_list.json()
    legacy_item = next((i for i in items if i["id"] == "wq_legacy_01"), None)
    assert legacy_item is not None
    assert legacy_item["hasSnapshot"] is False
    assert legacy_item["canRetry"] is False

    # POST retry on legacy item: must return 400 LEGACY_ENTRY_CANNOT_RETRY
    res_retry = client.post(
        "/me/wrong-questions/wq_legacy_01/retry",
        headers=student["headers"],
        json={"selectedOption": "B"}
    )
    assert res_retry.status_code == 400
    err_data = res_retry.json()
    code = err_data.get("code")
    if not code and isinstance(err_data.get("detail"), dict):
        code = err_data["detail"].get("code")
    assert code == "LEGACY_ENTRY_CANNOT_RETRY"


def test_repeated_wrong_answer_resets_resolved(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminStep4E", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentRepeatA", role="STUDENT")

    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    # Session 1: Student makes mistake on Q1 (correct is B), answers A
    sess1 = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()
    client.post(f"/library/sessions/{sess1['id']}/answers", headers=student["headers"], json={"questionId": q_ids[0], "selectedOption": "A"})

    wq_list = client.get("/me/wrong-questions", headers=student["headers"]).json()
    entry_id = wq_list[0]["id"]

    # Student retries correctly -> state is RESOLVED
    res_retry = client.post(f"/me/wrong-questions/{entry_id}/retry", headers=student["headers"], json={"selectedOption": "B"})
    assert res_retry.json()["state"] == "RESOLVED"

    # Session 2: Later, in a new session, student answers Q1 WRONG AGAIN (answers C)
    sess2 = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()
    client.post(f"/library/sessions/{sess2['id']}/answers", headers=student["headers"], json={"questionId": q_ids[0], "selectedOption": "C"})

    # Notebook entry must have reset back to NEEDS_REVIEW with the new selectedOption
    wq_list2 = client.get("/me/wrong-questions", headers=student["headers"]).json()
    assert len(wq_list2) == 1
    updated_entry = wq_list2[0]
    assert updated_entry["id"] == entry_id
    assert updated_entry["state"] == "NEEDS_REVIEW"
    assert updated_entry["selectedOption"] == "C"
