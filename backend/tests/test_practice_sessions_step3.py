import contextlib
import time
import threading
import json
import sqlite3
import pytest
from fastapi.testclient import TestClient
from aimentor.config import Settings
from aimentor.main import create_app
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

def get_error_code(res):
    data = res.json()
    if isinstance(data, dict):
        if "code" in data:
            return data["code"]
        if "detail" in data:
            if isinstance(data["detail"], dict):
                return data["detail"].get("code")
            return data["detail"]
    return None

def seed_quiz_with_questions(client, admin):
    # Create 3 questions for Physics Grade 10: Q0 (+10, B), Q1 (+15, A), Q2 (+20, C)
    q_ids = []
    for idx, (correct_opt, pts) in enumerate([("B", 10), ("A", 15), ("C", 20)], start=1):
        payload = {
            "code": f"PHY10_STEP3_Q0{idx}",
            "subjectId": "physics",
            "grade": 10,
            "chapter": "Chương 1",
            "topic": "Động học",
            "prompt": f"Nội dung câu hỏi kiểm chứng Step 3 số {idx}?",
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
        "title": "Bộ đề kiểm chứng Step 3 Vật lý 10",
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

# -------------------------------------------------------------
# 1. Secrecy & Progressive Disclosure Tests
# -------------------------------------------------------------
def test_secrecy_and_progressive_disclosure(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminStep3", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentA", role="STUDENT")

    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    # 1. Quiz detail: correctOption & explanation must NOT be present
    res = client.get(f"/library/quizzes/{quiz_id}", headers=student["headers"])
    assert res.status_code == 200
    for q in res.json()["questions"]:
        assert "correctOption" not in q, "Secret correctOption leaked in quiz detail!"
        assert "explanation" not in q, "Secret explanation leaked in quiz detail!"

    # 2. Session created: correctOption & explanation must NOT be present
    res_sess = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id})
    assert res_sess.status_code == 201
    sess = res_sess.json()
    sess_id = sess["id"]
    assert sess["state"] == "IN_PROGRESS"
    assert sess["maxScore"] == 45
    for q in sess["questions"]:
        assert "correctOption" not in q, "Secret correctOption leaked in initial session!"
        assert "explanation" not in q, "Secret explanation leaked in initial session!"

    # 3. Answering Q0 reveals its solution; Q1 & Q2 remain strictly hidden
    ans_res = client.post(
        f"/library/sessions/{sess_id}/answers",
        headers=student["headers"],
        json={"questionId": q_ids[0], "selectedOption": "b "}
    )
    assert ans_res.status_code == 200
    updated_sess = ans_res.json()
    q0 = next(q for q in updated_sess["questions"] if q["id"] == q_ids[0])
    assert q0["correctOption"] == "B"
    assert "Lời giải chi tiết câu hỏi số 1" in q0["explanation"]

    q1 = next(q for q in updated_sess["questions"] if q["id"] == q_ids[1])
    assert "correctOption" not in q1, "Unanswered question leaked correctOption!"
    assert "explanation" not in q1, "Unanswered question leaked explanation!"

# -------------------------------------------------------------
# 2. Strict Input & Option Validation Tests
# -------------------------------------------------------------
def test_answer_input_validation(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminVal", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentVal", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    sess = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()
    sess_id = sess["id"]

    # 1. Option out of A-D range -> 400 INVALID_OPTION
    res = client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[0], "selectedOption": "E"})
    assert res.status_code == 400
    assert get_error_code(res) == "INVALID_OPTION"

    # 2. Empty option -> 400 INVALID_OPTION
    res = client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[0], "selectedOption": "   "})
    assert res.status_code == 400
    assert get_error_code(res) == "INVALID_OPTION"

    # 3. Question not in this quiz session -> 404 QUESTION_NOT_FOUND
    res = client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": "q_nonexistent_123", "selectedOption": "A"})
    assert res.status_code == 404
    assert get_error_code(res) == "QUESTION_NOT_FOUND"

# -------------------------------------------------------------
# 3. Strict Session Completion Policy (Lỗi A Fix)
# -------------------------------------------------------------
def test_strict_session_completion_policy(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminComp", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentComp", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    sess = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()
    sess_id = sess["id"]
    db_file = tmp_path / "aimentor.sqlite3"

    # Capture initial DB state
    conn = sqlite3.connect(db_file)
    conn.row_factory = sqlite3.Row
    row_before = dict(conn.execute("SELECT * FROM practice_sessions WHERE id=?", (sess_id,)).fetchone())
    conn.close()

    # 1. Attempt to complete immediately with 0 questions answered -> Rejected 400 SESSION_INCOMPLETE
    res_early = client.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
    assert res_early.status_code == 400
    assert get_error_code(res_early) == "SESSION_INCOMPLETE"
    assert "mới trả lời 0/3" in res_early.json().get("message", "")

    # Verify response does NOT leak any questions, solutions, or explanations
    assert "questions" not in res_early.json()
    assert "correctOption" not in res_early.text

    # Verify DB was NOT mutated on failed complete
    conn = sqlite3.connect(db_file)
    conn.row_factory = sqlite3.Row
    row_after_fail = dict(conn.execute("SELECT * FROM practice_sessions WHERE id=?", (sess_id,)).fetchone())
    conn.close()
    assert row_after_fail["state"] == row_before["state"] == "IN_PROGRESS"
    assert row_after_fail["score"] == row_before["score"] == 0
    assert row_after_fail["correct_count"] == row_before["correct_count"] == 0
    assert row_after_fail["answers"] == row_before["answers"] == "{}"
    assert row_after_fail["updated_at"] == row_before["updated_at"]

    # 2. Answer Q0 only (1 of 3) and attempt complete -> Rejected 400 SESSION_INCOMPLETE
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[0], "selectedOption": "B"})
    res_partial = client.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
    assert res_partial.status_code == 400
    assert get_error_code(res_partial) == "SESSION_INCOMPLETE"
    assert "mới trả lời 1/3" in res_partial.json().get("message", "")

    # Verify GET /library/sessions/{id} still does NOT leak solutions for unanswered Q1 & Q2
    sess_check = client.get(f"/library/sessions/{sess_id}", headers=student["headers"]).json()
    assert sess_check["state"] == "IN_PROGRESS"
    q1 = next(q for q in sess_check["questions"] if q["id"] == q_ids[1])
    q2 = next(q for q in sess_check["questions"] if q["id"] == q_ids[2])
    assert "correctOption" not in q1 and "correctOption" not in q2

    # 3. Answer remaining questions (Q1: wrong 'D', Q2: correct 'C')
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[1], "selectedOption": "D"})
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[2], "selectedOption": "C"})

    # 4. Complete after all 3 questions answered -> Succeeds 200 COMPLETED
    res_comp = client.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
    assert res_comp.status_code == 200
    comp_data = res_comp.json()
    assert comp_data["state"] == "COMPLETED"
    assert comp_data["score"] == 30  # Q0 (10) + Q2 (20)
    assert comp_data["correctCount"] == 2
    assert comp_data["totalCount"] == 3

    # All questions in completed session now expose solutions for review
    for q in comp_data["questions"]:
        assert "correctOption" in q
        assert "explanation" in q

    # 5. Repeated complete on already COMPLETED session is IDEMPOTENT (returns 200 without side effects)
    res_comp2 = client.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
    assert res_comp2.status_code == 200
    assert res_comp2.json()["state"] == "COMPLETED"
    assert res_comp2.json()["score"] == 30

    # 6. Attempting to submit answer after completion is rejected with 409
    res_after = client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[0], "selectedOption": "A"})
    assert res_after.status_code == 409
    assert get_error_code(res_after) == "SESSION_ALREADY_COMPLETED"

# -------------------------------------------------------------
# 4. Controlled Concurrency Tests (Khoảng trống C Fix)
# -------------------------------------------------------------
def test_concurrency_same_question_same_answer(tmp_path):
    """Scenario A1: Two threads submit the same answer for the same question concurrently."""
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminConcA1", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentConcA1", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)
    sess_id = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()["id"]

    barrier = threading.Barrier(2)
    results = [None, None]

    def worker(idx):
        barrier.wait()  # Both release at the exact same moment
        res = client.post(
            f"/library/sessions/{sess_id}/answers",
            headers=student["headers"],
            json={"questionId": q_ids[0], "selectedOption": "B"}
        )
        results[idx] = res

    t0 = threading.Thread(target=worker, args=(0,))
    t1 = threading.Thread(target=worker, args=(1,))
    t0.start(); t1.start()
    t0.join(); t1.join()

    assert results[0].status_code == 200
    assert results[1].status_code == 200

    # Score must be counted once (+10), not doubled (+20)
    final_sess = client.get(f"/library/sessions/{sess_id}", headers=student["headers"]).json()
    assert final_sess["score"] == 10
    assert final_sess["correctCount"] == 1

def test_concurrency_same_question_different_answers(tmp_path):
    """Scenario A2: Two threads submit different answers for the same question concurrently."""
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminConcA2", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentConcA2", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)
    sess_id = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()["id"]

    barrier = threading.Barrier(2)
    results = [None, None]

    def worker(idx, opt):
        barrier.wait()
        res = client.post(
            f"/library/sessions/{sess_id}/answers",
            headers=student["headers"],
            json={"questionId": q_ids[0], "selectedOption": opt}
        )
        results[idx] = res

    t0 = threading.Thread(target=worker, args=(0, "B"))  # Correct
    t1 = threading.Thread(target=worker, args=(1, "C"))  # Wrong
    t0.start(); t1.start()
    t0.join(); t1.join()

    assert results[0].status_code == 200
    assert results[1].status_code == 200

    # The winning answer must be preserved; the other request must NOT overwrite it
    final_sess = client.get(f"/library/sessions/{sess_id}", headers=student["headers"]).json()
    winning_opt = final_sess["answers"][q_ids[0]]["selectedOption"]
    assert winning_opt in ("B", "C")
    expected_score = 10 if winning_opt == "B" else 0
    assert final_sess["score"] == expected_score
    assert final_sess["correctCount"] == (1 if winning_opt == "B" else 0)

def test_concurrency_different_questions_overlap(tmp_path):
    """Scenario B: Two threads submit answers for different questions concurrently."""
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminConcB", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentConcB", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)
    sess_id = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()["id"]

    barrier = threading.Barrier(2)
    results = [None, None]

    def worker(idx, qid, opt):
        barrier.wait()
        res = client.post(
            f"/library/sessions/{sess_id}/answers",
            headers=student["headers"],
            json={"questionId": qid, "selectedOption": opt}
        )
        results[idx] = res

    t0 = threading.Thread(target=worker, args=(0, q_ids[0], "B"))  # +10
    t1 = threading.Thread(target=worker, args=(1, q_ids[1], "A"))  # +15
    t0.start(); t1.start()
    t0.join(); t1.join()

    assert results[0].status_code == 200
    assert results[1].status_code == 200

    # Both answers must be retained, no lost updates, mathematically exact total: 25
    final_sess = client.get(f"/library/sessions/{sess_id}", headers=student["headers"]).json()
    assert final_sess["score"] == 25
    assert final_sess["correctCount"] == 2
    assert q_ids[0] in final_sess["answers"]
    assert q_ids[1] in final_sess["answers"]

def test_concurrency_scenario_a1_complete_lock_contention(tmp_path):
    """Scenario A1: Complete acquires transaction lock (BEGIN IMMEDIATE) first.
    Answer for remaining question arrives and contends, physically blocking at SQLite BEGIN IMMEDIATE.
    Complete finishes first, rejected with 400 SESSION_INCOMPLETE without mutating data.
    Answer unblocks and commits 200 OK. Final Complete succeeds 200 COMPLETED."""
    settings = Settings(tmp_path)
    app = create_app(settings)
    client = TestClient(app)
    admin = create_user(client, "AdminConcA1", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentConcA1", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)
    sess_id = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()["id"]

    # Pre-answer Q0 and Q1; Q2 remains unanswered
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[0], "selectedOption": "B"})
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[1], "selectedOption": "A"})

    orig_transaction = app.state.db.transaction
    complete_holding_lock = threading.Event()
    allow_complete_to_proceed = threading.Event()
    answer_entered_tx = threading.Event()

    @contextlib.contextmanager
    def hooked_transaction():
        import inspect
        caller = ""
        for frame in inspect.stack():
            if frame.function in ("complete_session", "submit_answer"):
                caller = frame.function
                break

        db = app.state.db.connect()
        try:
            if caller == "submit_answer":
                answer_entered_tx.set()
            db.execute("BEGIN IMMEDIATE")
            if caller == "complete_session":
                complete_holding_lock.set()
                # Hold the transaction lock until Answer is blocked and we signal release
                allow_complete_to_proceed.wait(timeout=5)
            yield db
            db.commit()
        except BaseException:
            db.rollback()
            raise
        finally:
            db.close()

    app.state.db.transaction = hooked_transaction

    results = {}
    def run_complete():
        c = TestClient(app)
        res = c.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
        results["complete"] = res

    def run_answer():
        c = TestClient(app)
        res = c.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[2], "selectedOption": "C"})
        results["answer"] = res

    t_complete = threading.Thread(target=run_complete)
    t_answer = threading.Thread(target=run_answer)

    # 1. Complete thread starts and acquires lock
    t_complete.start()
    assert complete_holding_lock.wait(timeout=3), "Complete thread did not acquire transaction lock in time"

    # 2. Answer thread starts while Complete holds BEGIN IMMEDIATE lock
    t_answer.start()
    time.sleep(0.2)
    assert answer_entered_tx.is_set(), "Answer thread should have attempted transaction"
    assert t_answer.is_alive(), "Answer thread should be physically blocked at BEGIN IMMEDIATE waiting for Complete"

    # 3. Allow Complete to finish: sees missing Q2, raises 400 SESSION_INCOMPLETE, rolls back, releases lock
    allow_complete_to_proceed.set()

    t_complete.join(timeout=5)
    t_answer.join(timeout=5)

    app.state.db.transaction = orig_transaction

    assert not t_complete.is_alive()
    assert not t_answer.is_alive()

    # Complete failed 400 without corrupting data
    assert results["complete"].status_code == 400
    assert get_error_code(results["complete"]) == "SESSION_INCOMPLETE"

    # Answer succeeded 200 after acquiring released lock
    assert results["answer"].status_code == 200
    assert results["answer"].json()["answers"][q_ids[2]]["selectedOption"] == "C"

    # Session is still IN_PROGRESS in DB with all 3 answers
    sess_check = client.get(f"/library/sessions/{sess_id}", headers=student["headers"]).json()
    assert sess_check["state"] == "IN_PROGRESS"
    assert len(sess_check["answers"]) == 3

    # 4. Final Complete succeeds 200 COMPLETED
    res_final = client.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
    assert res_final.status_code == 200
    assert res_final.json()["state"] == "COMPLETED"
    assert res_final.json()["score"] == 45
    assert res_final.json()["correctCount"] == 3


def test_concurrency_scenario_a2_answer_lock_contention(tmp_path):
    """Scenario A2: Answer acquires transaction lock (BEGIN IMMEDIATE) first.
    Complete arrives and contends, physically blocking at SQLite BEGIN IMMEDIATE.
    Answer finishes and commits 200 OK, releasing lock.
    Complete unblocks, reads updated state, and succeeds 200 COMPLETED with score 45."""
    settings = Settings(tmp_path)
    app = create_app(settings)
    client = TestClient(app)
    admin = create_user(client, "AdminConcA2", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentConcA2", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)
    sess_id = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()["id"]

    # Pre-answer Q0 and Q1
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[0], "selectedOption": "B"})
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[1], "selectedOption": "A"})

    orig_transaction = app.state.db.transaction
    answer_holding_lock = threading.Event()
    allow_answer_to_proceed = threading.Event()
    complete_entered_tx = threading.Event()

    @contextlib.contextmanager
    def hooked_transaction():
        import inspect
        caller = ""
        for frame in inspect.stack():
            if frame.function in ("complete_session", "submit_answer"):
                caller = frame.function
                break

        db = app.state.db.connect()
        try:
            if caller == "complete_session":
                complete_entered_tx.set()
            db.execute("BEGIN IMMEDIATE")
            if caller == "submit_answer":
                answer_holding_lock.set()
                allow_answer_to_proceed.wait(timeout=5)
            yield db
            db.commit()
        except BaseException:
            db.rollback()
            raise
        finally:
            db.close()

    app.state.db.transaction = hooked_transaction

    results = {}
    def run_answer():
        c = TestClient(app)
        res = c.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[2], "selectedOption": "C"})
        results["answer"] = res

    def run_complete():
        c = TestClient(app)
        res = c.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
        results["complete"] = res

    t_answer = threading.Thread(target=run_answer)
    t_complete = threading.Thread(target=run_complete)

    # 1. Answer thread starts and acquires lock
    t_answer.start()
    assert answer_holding_lock.wait(timeout=3), "Answer thread did not acquire transaction lock in time"

    # 2. Complete thread starts while Answer holds BEGIN IMMEDIATE lock
    t_complete.start()
    time.sleep(0.2)
    assert complete_entered_tx.is_set(), "Complete thread should have attempted transaction"
    assert t_complete.is_alive(), "Complete thread should be physically blocked at BEGIN IMMEDIATE waiting for Answer"

    # 3. Allow Answer to write, commit and release lock
    allow_answer_to_proceed.set()

    t_answer.join(timeout=5)
    t_complete.join(timeout=5)

    app.state.db.transaction = orig_transaction

    assert not t_answer.is_alive()
    assert not t_complete.is_alive()

    assert results["answer"].status_code == 200
    assert results["complete"].status_code == 200
    assert results["complete"].json()["state"] == "COMPLETED"
    assert results["complete"].json()["score"] == 45
    assert results["complete"].json()["correctCount"] == 3


def test_concurrency_sensitivity_checks(tmp_path):
    """Sensitivity checks proving assertions catch data corruption, premature completion, and points loss."""
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminSens", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentSens", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)
    sess_id = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()["id"]

    # 1. Complete with 0 answers -> 400
    res_empty = client.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
    assert res_empty.status_code == 400
    assert get_error_code(res_empty) == "SESSION_INCOMPLETE"

    # 2. Answer 2 out of 3 questions
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[0], "selectedOption": "B"})
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[1], "selectedOption": "A"})

    # 3. Complete with 2/3 answers -> 400
    res_partial = client.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
    assert res_partial.status_code == 400
    assert get_error_code(res_partial) == "SESSION_INCOMPLETE"

    # Verify session is still IN_PROGRESS and score is NOT committed prematurely
    sess = client.get(f"/library/sessions/{sess_id}", headers=student["headers"]).json()
    assert sess["state"] == "IN_PROGRESS"
    assert sess["score"] == 25  # Running score reflects Q0 (10) + Q1 (15)

    # 4. Answer last question with WRONG answer (D instead of C)
    client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[2], "selectedOption": "D"})

    # 5. Complete with 3/3 answers -> 200 COMPLETED
    res_comp = client.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])
    assert res_comp.status_code == 200
    # Q0: 10 (correct), Q1: 15 (correct), Q2: 0 (wrong) -> Score must be exactly 25, NOT 45!
    assert res_comp.json()["score"] == 25
    assert res_comp.json()["correctCount"] == 2
    assert res_comp.json()["state"] == "COMPLETED"

# -------------------------------------------------------------
# 5. Snapshot Resilience & WAL Mode Verification
# -------------------------------------------------------------
def test_snapshot_resilience_and_wal_mode(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminSnap", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentSnap", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    db_path = tmp_path / "aimentor.sqlite3"
    conn = sqlite3.connect(db_path)
    journal_mode = conn.execute("PRAGMA journal_mode;").fetchone()[0]
    conn.close()
    assert journal_mode.lower() == "wal", f"Expected WAL mode, got {journal_mode}"

    sess = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()
    sess_id = sess["id"]

    # Admin un-publishes Q0 in question bank
    res_unpub = client.post(f"/admin/questions/{q_ids[0]}/publish?publish=false", headers=admin["headers"])
    assert res_unpub.status_code == 200

    # Student submits answer to Q0: must still grade against frozen snapshot
    ans_res = client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": q_ids[0], "selectedOption": "B"})
    assert ans_res.status_code == 200
    assert ans_res.json()["answers"][q_ids[0]]["isCorrect"] is True

# -------------------------------------------------------------
# 6. User Isolation A/B Security
# -------------------------------------------------------------
def test_session_ownership_isolation(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminIso", role="ADMIN", tmp_path=tmp_path)
    studentA = create_user(client, "StudentIsoA", role="STUDENT")
    studentB = create_user(client, "StudentIsoB", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    sessA = client.post("/library/sessions", headers=studentA["headers"], json={"quizId": quiz_id}).json()
    sess_id_A = sessA["id"]

    # Student B cannot get Student A's session
    assert client.get(f"/library/sessions/{sess_id_A}", headers=studentB["headers"]).status_code == 404
    # Student B cannot answer Student A's session
    assert client.post(f"/library/sessions/{sess_id_A}/answers", headers=studentB["headers"], json={"questionId": q_ids[0], "selectedOption": "B"}).status_code == 404
    # Student B cannot complete Student A's session
    assert client.post(f"/library/sessions/{sess_id_A}/complete", headers=studentB["headers"]).status_code == 404

# -------------------------------------------------------------
# 7. No EXP or Streak Mutation
# -------------------------------------------------------------
def test_library_practice_does_not_mutate_exp_or_streak(tmp_path):
    client, _ = setup_client(tmp_path)
    admin = create_user(client, "AdminExp", role="ADMIN", tmp_path=tmp_path)
    student = create_user(client, "StudentExp", role="STUDENT")
    quiz_id, q_ids = seed_quiz_with_questions(client, admin)

    user_id = student["id"]
    db_path = tmp_path / "aimentor.sqlite3"
    conn = sqlite3.connect(db_path)
    awards_before = conn.execute("SELECT COUNT(*) FROM awards WHERE user_id=?", (user_id,)).fetchone()[0]
    days_before = conn.execute("SELECT COUNT(*) FROM learning_days WHERE user_id=?", (user_id,)).fetchone()[0]
    conn.close()

    # Answer all questions and complete session
    sess_id = client.post("/library/sessions", headers=student["headers"], json={"quizId": quiz_id}).json()["id"]
    for qid, opt in zip(q_ids, ["B", "A", "C"]):
        client.post(f"/library/sessions/{sess_id}/answers", headers=student["headers"], json={"questionId": qid, "selectedOption": opt})
    client.post(f"/library/sessions/{sess_id}/complete", headers=student["headers"])

    conn2 = sqlite3.connect(db_path)
    awards_after = conn2.execute("SELECT COUNT(*) FROM awards WHERE user_id=?", (user_id,)).fetchone()[0]
    days_after = conn2.execute("SELECT COUNT(*) FROM learning_days WHERE user_id=?", (user_id,)).fetchone()[0]
    conn2.close()

    assert awards_after == awards_before == 0, "Awards (XP) were created by Library practice!"
    assert days_after == days_before == 0, "Learning days (streak) were mutated by Library practice!"
