import json
import pytest
from starlette.testclient import TestClient

from aimentor.config import Settings
from aimentor.main import create_app
from aimentor.ai_practice import FakeAiProvider, UnconfiguredAiProvider


@pytest.fixture(scope="module")
def env(tmp_path_factory):
    data_dir = tmp_path_factory.mktemp("ai_practice_data")
    settings = Settings(data_dir=data_dir, demo_accounts=True)
    app = create_app(settings)
    fake_provider = FakeAiProvider(demo_mode=True)
    app.state.ai_practice.set_provider(fake_provider)

    with TestClient(app) as client:
        # Register user 1
        r1 = client.post("/auth/sign-up", json={
            "displayName": "HocSinh1",
            "email": "student1@school.test",
            "password": "Password123!",
            "acceptedTermsVersion": "2026-10"
        })
        assert r1.status_code == 201
        token1 = r1.json()["accessToken"]

        # Register user 2
        r2 = client.post("/auth/sign-up", json={
            "displayName": "HocSinh2",
            "email": "student2@school.test",
            "password": "Password123!",
            "acceptedTermsVersion": "2026-10"
        })
        assert r2.status_code == 201
        token2 = r2.json()["accessToken"]

        yield {
            "app": app,
            "client": client,
            "provider": fake_provider,
            "token1": token1,
            "token2": token2,
            "user1_id": int(r1.json()["userId"]),
            "user2_id": int(r2.json()["userId"]),
        }


def auth(token):
    return {"Authorization": f"Bearer {token}"}


def code_of(res):
    data = res.json()
    if isinstance(data, dict):
        if "code" in data:
            return data["code"]
        if "detail" in data and isinstance(data["detail"], dict):
            return data["detail"].get("code")
    return None


def test_config_endpoint(env):
    c = env["client"]
    res = c.get("/ai-practice/config")
    assert res.status_code == 200
    data = res.json()
    assert data["configured"] is True
    assert "physics" in data["subjects"]
    assert "math" in data["subjects"]
    assert data["defaultQuestions"] == 5


def test_unauthenticated_and_ownership_isolation(env):
    c = env["client"]
    t1 = env["token1"]
    t2 = env["token2"]

    # 1. Unauthenticated create rejected
    r = c.post("/ai-practice/sessions", json={
        "subjectId": "physics",
        "grade": 10,
        "topic": "Động học chất điểm",
        "difficulty": "MEDIUM",
        "questionCount": 5
    })
    assert r.status_code == 401

    # 2. User 1 creates session
    sess_res = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "physics",
        "grade": 10,
        "topic": "Động học chất điểm",
        "difficulty": "MEDIUM",
        "questionCount": 5
    })
    assert sess_res.status_code == 201
    sess_id = sess_res.json()["id"]

    # 3. User 2 cannot read User 1's session
    r_u2 = c.get(f"/ai-practice/sessions/{sess_id}", headers=auth(t2))
    assert r_u2.status_code in (403, 404)

    # 4. User 2 cannot answer User 1's session
    r_ans2 = c.post(f"/ai-practice/sessions/{sess_id}/answer", headers=auth(t2), json={
        "questionId": sess_res.json()["questions"][0]["id"],
        "selectedOption": "A"
    })
    assert r_ans2.status_code in (403, 404)


def test_input_validation(env):
    c = env["client"]
    t1 = env["token1"]

    # Invalid subject
    r = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "astronomy", "grade": 10, "topic": "Sao Hỏa", "questionCount": 5
    })
    assert r.status_code == 400
    assert code_of(r) == "INVALID_SUBJECT"

    # Invalid grade
    r = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "math", "grade": 9, "topic": "Hàm số", "questionCount": 5
    })
    assert r.status_code in (400, 422)

    # Empty topic
    r = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "math", "grade": 10, "topic": "   ", "questionCount": 5
    })
    assert r.status_code in (400, 422)

    # Topic too long (> 100 chars)
    r = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "math", "grade": 10, "topic": "A" * 101, "questionCount": 5
    })
    assert r.status_code in (400, 422)

    # Question count out of bounds (< 1 or > 10)
    r = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "math", "grade": 10, "topic": "Hàm số", "questionCount": 0
    })
    assert r.status_code in (400, 422)
    r2 = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "math", "grade": 10, "topic": "Hàm số", "questionCount": 15
    })
    assert r2.status_code in (400, 422)


def test_provider_error_handling(env):
    c = env["client"]
    t1 = env["token1"]
    app = env["app"]

    payload = {"subjectId": "biology", "grade": 11, "topic": "Quang hợp", "questionCount": 3}

    # 1. Unconfigured provider -> 503
    app.state.ai_practice.set_provider(UnconfiguredAiProvider())
    r = c.post("/ai-practice/sessions", headers=auth(t1), json=payload)
    assert r.status_code == 503
    assert code_of(r) == "AI_PROVIDER_NOT_CONFIGURED"

    # 2. Timeout -> 504
    app.state.ai_practice.set_provider(FakeAiProvider(failure_mode="TIMEOUT"))
    r = c.post("/ai-practice/sessions", headers=auth(t1), json=payload)
    assert r.status_code == 504
    assert code_of(r) == "AI_TIMEOUT"

    # 3. Rate limit -> 429
    app.state.ai_practice.set_provider(FakeAiProvider(failure_mode="RATE_LIMIT"))
    r = c.post("/ai-practice/sessions", headers=auth(t1), json=payload)
    assert r.status_code == 429
    assert code_of(r) == "AI_RATE_LIMIT"

    # 4. Malformed output -> 502
    app.state.ai_practice.set_provider(FakeAiProvider(failure_mode="MALFORMED_JSON"))
    r = c.post("/ai-practice/sessions", headers=auth(t1), json=payload)
    assert r.status_code == 502
    assert code_of(r) == "AI_MALFORMED_OUTPUT"

    # Restore provider for subsequent tests
    app.state.ai_practice.set_provider(env["provider"])


def test_question_secrecy_and_sanitization(env):
    c = env["client"]
    t1 = env["token1"]
    env["app"].state.ai_practice.set_provider(FakeAiProvider())

    # Create session
    r = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "chemistry", "grade": 10, "topic": "Nguyên tử", "difficulty": "MEDIUM", "questionCount": 5
    })
    assert r.status_code == 201
    data = r.json()
    questions = data["questions"]
    assert len(questions) == 5

    # VERIFY SECRECY: correctOption and explanation are WITHHELD!
    for q in questions:
        assert "correctOption" not in q, f"Leaked correctOption: {q}"
        assert "explanation" not in q, f"Leaked explanation: {q}"
        assert "prompt" in q
        assert "options" in q
        assert set(q["options"].keys()) == {"A", "B", "C", "D"}

    # Also verifying reading active session via GET /ai-practice/sessions/{id} withholds answers
    sess_get = c.get(f"/ai-practice/sessions/{data['id']}", headers=auth(t1)).json()
    for q in sess_get["questions"]:
        assert "correctOption" not in q
        assert "explanation" not in q


def test_idempotency_retry_and_conflict(env):
    c = env["client"]
    t1 = env["token1"]
    env["app"].state.ai_practice.set_provider(FakeAiProvider())

    headers = {**auth(t1), "Idempotency-Key": "idem-ai-key-001"}
    payload = {
        "subjectId": "physics", "grade": 10, "topic": "Lực ma sát", "difficulty": "EASY", "questionCount": 3
    }

    # First call
    r1 = c.post("/ai-practice/sessions", headers=headers, json=payload)
    assert r1.status_code == 201
    sess1 = r1.json()

    # Retry call with exact same key and payload -> returns cached session
    r2 = c.post("/ai-practice/sessions", headers=headers, json=payload)
    assert r2.status_code == 201
    sess2 = r2.json()
    assert sess1["id"] == sess2["id"]

    # Same key with different payload -> 409 IDEMPOTENCY_CONFLICT
    r3 = c.post("/ai-practice/sessions", headers=headers, json={
        "subjectId": "physics", "grade": 10, "topic": "Lực đàn hồi", "difficulty": "EASY", "questionCount": 3
    })
    assert r3.status_code == 409
    assert code_of(r3) == "IDEMPOTENCY_CONFLICT"


def test_answer_evaluation_first_try_vs_retry_and_wrong_notebook(env):
    c = env["client"]
    t1 = env["token1"]
    u1_id = env["user1_id"]
    app = env["app"]
    app.state.ai_practice.set_provider(FakeAiProvider())

    # Create session with 2 questions
    r = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "math", "grade": 11, "topic": "Cấp số cộng", "difficulty": "MEDIUM", "questionCount": 2
    })
    sess = r.json()
    sess_id = sess["id"]
    q1_id = sess["questions"][0]["id"]
    q2_id = sess["questions"][1]["id"]

    # Inspect DB snapshot to know correct options for testing
    with app.state.db.read() as db:
        row = db.execute("SELECT questions_snapshot FROM ai_practice_sessions WHERE id=?", (sess_id,)).fetchone()
        snap = json.loads(row["questions_snapshot"])
        q1_correct = snap[0]["correctOption"]  # 'A'
        q2_correct = snap[1]["correctOption"]  # 'B'

    q2_wrong = "C" if q2_correct != "C" else "D"

    # 1. Answer Q1 correctly on first attempt
    ans1_res = c.post(f"/ai-practice/sessions/{sess_id}/answer", headers=auth(t1), json={
        "questionId": q1_id, "selectedOption": q1_correct
    })
    assert ans1_res.status_code == 200
    ans1 = ans1_res.json()
    assert ans1["isCorrect"] is True
    assert ans1["firstTryCorrect"] is True
    assert ans1["attemptsCount"] == 1
    assert ans1["correctOption"] == q1_correct
    assert "explanation" in ans1

    # 2. Answer Q2 incorrectly on first attempt
    ans2_res = c.post(f"/ai-practice/sessions/{sess_id}/answer", headers=auth(t1), json={
        "questionId": q2_id, "selectedOption": q2_wrong
    })
    assert ans2_res.status_code == 200
    ans2 = ans2_res.json()
    assert ans2["isCorrect"] is False
    assert ans2["firstTryCorrect"] is False
    assert ans2["attemptsCount"] == 1
    assert ans2["correctOption"] == q2_correct

    # Check that Q2 is automatically recorded in wrong_questions_notebook!
    with app.state.db.read() as db:
        wrong_entry = db.execute(
            "SELECT * FROM wrong_questions_notebook WHERE user_id=? AND question_id=?",
            (u1_id, q2_id)
        ).fetchone()
    assert wrong_entry is not None
    assert wrong_entry["subject_id"] == "math"
    assert wrong_entry["grade"] == 11
    assert wrong_entry["source"] == "AI_PRACTICE"
    assert wrong_entry["state"] == "NEEDS_REVIEW"

    # 3. Retry Q2 with correct option
    # Since the student has already seen the answer, firstTryCorrect MUST be False!
    retry_res = c.post(f"/ai-practice/sessions/{sess_id}/answer", headers=auth(t1), json={
        "questionId": q2_id, "selectedOption": q2_correct
    })
    assert retry_res.status_code == 200
    retry_ans = retry_res.json()
    assert retry_ans["isCorrect"] is True
    assert retry_ans["firstTryCorrect"] is False  # NOT first try correct!
    assert retry_ans["attemptsCount"] == 2

    # 4. Complete session
    comp_res = c.post(f"/ai-practice/sessions/{sess_id}/complete", headers=auth(t1))
    assert comp_res.status_code == 200
    comp_data = comp_res.json()
    assert comp_data["state"] == "COMPLETED"
    summary = comp_data["summary"]
    assert summary["totalQuestions"] == 2
    assert summary["firstTryCorrectCount"] == 1  # only Q1
    assert summary["assistedCount"] == 1         # Q2 answered after retry/hint
    assert summary["incorrectCount"] == 0
    assert summary["skippedCount"] == 0
    assert summary["accuracyFirstTry"] == 50.0   # 1 / 2 = 50%


def test_skip_question_and_zero_exp_streak(env):
    c = env["client"]
    t1 = env["token1"]
    u1_id = env["user1_id"]
    app = env["app"]
    app.state.ai_practice.set_provider(FakeAiProvider())

    # Create session with 3 questions
    r = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "history", "grade": 12, "topic": "Cách mạng tháng Tám", "questionCount": 3
    })
    sess = r.json()
    sess_id = sess["id"]
    q1_id = sess["questions"][0]["id"]
    q2_id = sess["questions"][1]["id"]
    q3_id = sess["questions"][2]["id"]

    # Skip Q1
    skip_res = c.post(f"/ai-practice/sessions/{sess_id}/skip", headers=auth(t1), json={"questionId": q1_id})
    assert skip_res.status_code == 200
    assert skip_res.json()["skipped"] is True
    assert "correctOption" in skip_res.json()

    # Complete session
    comp_res = c.post(f"/ai-practice/sessions/{sess_id}/complete", headers=auth(t1))
    assert comp_res.status_code == 200
    summary = comp_res.json()["summary"]
    assert summary["totalQuestions"] == 3
    assert summary["skippedCount"] == 3  # q1 skipped, q2 and q3 unattempted (treated as skipped)

    # STRICT AUDIT: Zero EXP / streak impact!
    with app.state.db.read() as db:
        awards = db.execute("SELECT COUNT(*) FROM awards WHERE user_id=?", (u1_id,)).fetchone()[0]
        days = db.execute("SELECT COUNT(*) FROM learning_days WHERE user_id=?", (u1_id,)).fetchone()[0]
    assert awards == 0
    assert days == 0

    dashboard = c.get("/me/dashboard", params={"grade": 12}, headers=auth(t1)).json()
    assert dashboard["totalXp"] == 0
    assert dashboard["streakDays"] == 0


def test_report_question_issue(env):
    c = env["client"]
    t1 = env["token1"]
    u1_id = env["user1_id"]
    app = env["app"]
    app.state.ai_practice.set_provider(FakeAiProvider())

    r = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "geography", "grade": 10, "topic": "Khí quyển", "questionCount": 2
    })
    sess_id = r.json()["id"]
    q1_id = r.json()["questions"][0]["id"]

    # Report question
    rep_res = c.post(f"/ai-practice/sessions/{sess_id}/report", headers=auth(t1), json={
        "questionId": q1_id,
        "reason": "Đáp án A và B quá giống nhau, gây nhầm lẫn",
        "details": "Nên phân biệt rõ giữa nhiệt độ trung bình và biên độ nhiệt."
    })
    assert rep_res.status_code == 200
    data = rep_res.json()
    assert data["status"] == "RECEIVED"
    report_id = data["reportId"]

    # Check SQLite record
    with app.state.db.read() as db:
        rep_row = db.execute("SELECT * FROM ai_question_reports WHERE id=?", (report_id,)).fetchone()
    assert rep_row is not None
    assert rep_row["user_id"] == u1_id
    assert rep_row["question_id"] == q1_id
    assert "Đáp án A và B" in rep_row["reason"]


def test_explain_more_followup(env):
    c = env["client"]
    t1 = env["token1"]
    env["app"].state.ai_practice.set_provider(FakeAiProvider())

    r = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "english", "grade": 11, "topic": "Conditional Sentences", "questionCount": 2
    })
    sess_id = r.json()["id"]
    q1_id = r.json()["questions"][0]["id"]

    # Full solutions are available only after the learner answers (or skips).
    blocked = c.post(f"/ai-practice/sessions/{sess_id}/explain-more", headers=auth(t1), json={
        "questionId": q1_id, "query": "Explain the answer"
    })
    assert blocked.status_code == 409
    answered = c.post(f"/ai-practice/sessions/{sess_id}/answer", headers=auth(t1), json={
        "questionId": q1_id, "selectedOption": "A", "submissionId": "followup-answer"
    })
    assert answered.status_code == 200
    # Ask more
    exp_res = c.post(f"/ai-practice/sessions/{sess_id}/explain-more", headers=auth(t1), json={
        "questionId": q1_id,
        "query": "Tại sao lại dùng 'were' thay vì 'was' ở câu điều kiện loại 2?"
    })
    assert exp_res.status_code == 200
    data = exp_res.json()
    assert "explanation" in data
    assert "Gia sư AI giải thích thêm" in data["explanation"]


def test_strict_output_contract_duplicate_options_and_empty_fields(env):
    c = env["client"]
    t1 = env["token1"]
    app = env["app"]

    class CustomMockProvider(FakeAiProvider):
        def __init__(self, questions_to_return):
            super().__init__()
            self.q_list = questions_to_return

        def generate_questions(self, *args, **kwargs):
            return self.q_list

    # Case A: Duplicate options with different whitespace and casing
    bad_questions_opts = [{
        "prompt": "Câu hỏi thử nghiệm 1 về Quang hợp ở thực vật C3 và C4?",
        "options": {
            "A": "Quang hợp",
            "B": "  quang   hợp  ",
            "C": "Hô hấp sáng",
            "D": "Chu trình Canvin"
        },
        "correctOption": "A",
        "explanation": "Lời giải chi tiết.",
        "hint": "Gợi ý."
    }]
    app.state.ai_practice.set_provider(CustomMockProvider(bad_questions_opts))
    res = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "biology", "grade": 11, "topic": "Quang hợp", "questionCount": 1
    })
    assert res.status_code == 502
    assert "các phương án trùng nhau sau khi chuẩn hóa" in res.json()["message"]

    # Case B: Empty explanation
    bad_questions_no_exp = [{
        "prompt": "Câu hỏi thử nghiệm 2 về Quang hợp ở thực vật C3 và C4?",
        "options": {
            "A": "Quang hợp A", "B": "Quang hợp B", "C": "Quang hợp C", "D": "Quang hợp D"
        },
        "correctOption": "A",
        "explanation": "   ",
        "hint": "Gợi ý."
    }]
    app.state.ai_practice.set_provider(CustomMockProvider(bad_questions_no_exp))
    res = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "biology", "grade": 11, "topic": "Quang hợp", "questionCount": 1
    })
    assert res.status_code == 502
    assert "có lời giải rỗng" in res.json()["message"]

    # Case C: Invalid option keys (has E)
    bad_questions_bad_keys = [{
        "prompt": "Câu hỏi thử nghiệm 3 về Quang hợp ở thực vật C3 và C4?",
        "options": {
            "A": "A", "B": "B", "C": "C", "D": "D", "E": "E"
        },
        "correctOption": "A",
        "explanation": "Lời giải.",
        "hint": "Gợi ý."
    }]
    app.state.ai_practice.set_provider(CustomMockProvider(bad_questions_bad_keys))
    res = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "biology", "grade": 11, "topic": "Quang hợp", "questionCount": 1
    })
    assert res.status_code == 502
    assert "phải có đúng 4 phương án A, B, C, D" in res.json()["message"]

    # Case D: Duplicate prompt after normalizing whitespace and case
    bad_duplicate_prompts = [
        {
            "prompt": "Nhận định nào sau đây là chính xác về định luật Ôm?",
            "options": {"A": "A1", "B": "B1", "C": "C1", "D": "D1"},
            "correctOption": "A",
            "explanation": "Giải thích 1.",
            "hint": "Gợi ý 1."
        },
        {
            "prompt": "  nhận định nào sau đây là  chính xác  về định luật Ôm?  ",
            "options": {"A": "A2", "B": "B2", "C": "C2", "D": "D2"},
            "correctOption": "B",
            "explanation": "Giải thích 2.",
            "hint": "Gợi ý 2."
        }
    ]
    app.state.ai_practice.set_provider(CustomMockProvider(bad_duplicate_prompts))
    res = c.post("/ai-practice/sessions", headers=auth(t1), json={
        "subjectId": "physics", "grade": 11, "topic": "Định luật Ôm", "questionCount": 2
    })
    assert res.status_code == 502
    assert "Phát hiện câu hỏi trùng lặp" in res.json()["message"]


def test_failed_generation_records_failed_operation_and_no_half_baked_session(env):
    c = env["client"]
    t1 = env["token1"]
    app = env["app"]

    class FailingValidationProvider(FakeAiProvider):
        def generate_questions(self, *args, **kwargs):
            return [{"invalid": True}]

    app.state.ai_practice.set_provider(FailingValidationProvider())
    key = "fail-test-key-1"
    res = c.post("/ai-practice/sessions", headers={**auth(t1), "Idempotency-Key": key}, json={
        "subjectId": "chemistry", "grade": 10, "topic": "Bảng tuần hoàn", "questionCount": 1
    })
    assert res.status_code == 502

    # Verify no half-baked session exists
    with app.state.db.read() as db:
        sessions = db.execute("SELECT * FROM ai_practice_sessions WHERE topic='Bảng tuần hoàn'").fetchall()
        assert len(sessions) == 0

        # Verify operation marked FAILED
        op = db.execute("SELECT * FROM ai_provider_operations WHERE operation_key=?", (key,)).fetchone()
        assert op is not None
        assert op["state"] == "FAILED"


from unittest.mock import patch, MagicMock
import io
import urllib.error
from aimentor.ai_practice import DEFAULT_GEMINI_MODEL, GeminiAiProvider, AiProviderError, AiPracticeService


def test_gemini_configuration_and_whitespace_validation(monkeypatch, tmp_path):
    # 1. No implicit hard-coded default model is assumed (policy requires explicit GEMINI_MODEL)
    assert DEFAULT_GEMINI_MODEL is None
    prov = GeminiAiProvider(api_key="valid-mock-key", model="gemini-3.8-flash")
    assert prov.model == "gemini-3.8-flash"

    # 2. Reject empty or whitespace API key
    with pytest.raises(AiProviderError) as exc_key_empty:
        GeminiAiProvider(api_key="", model="gemini-3.8-flash")
    assert exc_key_empty.value.code == "AI_PROVIDER_NOT_CONFIGURED"

    with pytest.raises(AiProviderError) as exc_key_ws:
        GeminiAiProvider(api_key="   ", model="gemini-3.8-flash")
    assert exc_key_ws.value.code == "AI_PROVIDER_NOT_CONFIGURED"

    # 3. Reject empty or whitespace model
    with pytest.raises(AiProviderError) as exc_model_empty:
        GeminiAiProvider(api_key="valid-mock-key", model="")
    assert exc_model_empty.value.code == "AI_CONFIG_ERROR"

    with pytest.raises(AiProviderError) as exc_model_ws:
        GeminiAiProvider(api_key="valid-mock-key", model="   ")
    assert exc_model_ws.value.code == "AI_CONFIG_ERROR"

    # 4. create_app environment validation
    data_dir = tmp_path / "gemini_test_data"

    # Case: AI_PROVIDER_TYPE=gemini but missing GEMINI_API_KEY
    monkeypatch.setenv("AI_PROVIDER_TYPE", "gemini")
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    monkeypatch.setenv("GEMINI_MODEL", "gemini-3.8-flash")
    with pytest.raises(ValueError, match="GEMINI_API_KEY"):
        create_app(Settings(data_dir=data_dir))

    # Case: AI_PROVIDER_TYPE=gemini but missing GEMINI_MODEL
    monkeypatch.setenv("GEMINI_API_KEY", "mock-key")
    monkeypatch.delenv("GEMINI_MODEL", raising=False)
    with pytest.raises(ValueError, match="GEMINI_MODEL"):
        create_app(Settings(data_dir=data_dir))

    # Case: GEMINI_MODEL is whitespace only
    monkeypatch.setenv("GEMINI_API_KEY", "mock-key")
    monkeypatch.setenv("GEMINI_MODEL", "   ")
    with pytest.raises(ValueError, match="GEMINI_MODEL"):
        create_app(Settings(data_dir=data_dir))

    # Case: Valid GEMINI_API_KEY with explicit GEMINI_MODEL
    monkeypatch.setenv("AI_PROVIDER_TYPE", "gemini")
    monkeypatch.setenv("GEMINI_API_KEY", "mock-secret-key")
    monkeypatch.setenv("GEMINI_MODEL", "gemini-3.8-flash")
    app = create_app(Settings(data_dir=data_dir))
    assert isinstance(app.state.ai_practice.provider, GeminiAiProvider)
    assert app.state.ai_practice.provider.model == "gemini-3.8-flash"

    # Case: Unconfigured provider works without any key or model
    monkeypatch.setenv("AI_PROVIDER_TYPE", "unconfigured")
    monkeypatch.delenv("GEMINI_API_KEY", raising=False)
    monkeypatch.delenv("GEMINI_MODEL", raising=False)
    app_unconf = create_app(Settings(data_dir=data_dir))
    assert not app_unconf.state.ai_practice.provider.is_configured()

    # Case: Fake provider works without any key or model
    monkeypatch.setenv("AI_PROVIDER_TYPE", "fake")
    app_fake = create_app(Settings(data_dir=data_dir))
    assert app_fake.state.ai_practice.provider.is_configured()


def test_gemini_mock_http_request_construction_and_parsing():
    mock_key = "mock-gemini-key-12345"
    mock_model = "gemini-3.8-flash"
    provider = GeminiAiProvider(api_key=mock_key, model=mock_model)
    expected_endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{mock_model}:generateContent"

    # 1. Mock successful generate_questions
    mock_candidates = {
        "candidates": [
            {
                "content": {
                    "parts": [
                        {
                            "text": json.dumps([
                                {
                                    "prompt": "Vật rơi tự do có gia tốc xấp xỉ bằng bao nhiêu?",
                                    "options": {
                                        "A": "9.8 m/s^2",
                                        "B": "5.0 m/s^2",
                                        "C": "0 m/s^2",
                                        "D": "15 m/s^2"
                                    },
                                    "correctOption": "A",
                                    "explanation": "Gia tốc trọng trường gần mặt đất xấp xỉ 9.8 m/s^2.",
                                    "hint": "Nhớ lại hằng số g."
                                }
                            ])
                        }
                    ]
                },
                "finishReason": "STOP"
            }
        ]
    }
    mock_resp_bytes = json.dumps(mock_candidates).encode("utf-8")

    captured_requests = []

    class MockHttpResponse:
        def __init__(self, data):
            self.data = data
        def read(self):
            return self.data
        def __enter__(self):
            return self
        def __exit__(self, exc_type, exc_val, exc_tb):
            pass

    def mock_urlopen(req, timeout=None):
        captured_requests.append(req)
        return MockHttpResponse(mock_resp_bytes)

    with patch("urllib.request.urlopen", side_effect=mock_urlopen):
        questions = provider.generate_questions("physics", 10, "Rơi tự do", "EASY", 1)
        assert len(questions) == 1
        assert questions[0]["correctOption"] == "A"

    assert len(captured_requests) == 1
    req = captured_requests[0]
    # URL must contain ONLY the endpoint and model; NO ?key=... parameter and NO API key in URL!
    assert req.full_url == expected_endpoint
    assert mock_key not in req.full_url
    assert "key=" not in req.full_url
    # Key must be present ONLY in x-goog-api-key header
    assert req.get_header("X-goog-api-key") == mock_key
    assert req.get_header("Content-type") == "application/json"

    body = json.loads(req.data.decode("utf-8"))
    assert body["generationConfig"]["responseMimeType"] == "application/json"
    assert "Rơi tự do" in body["contents"][0]["parts"][0]["text"]

    # 2. Mock explain_more request
    captured_requests.clear()
    mock_explain_response = {
        "candidates": [
            {
                "content": {
                    "parts": [{"text": "Gia tốc trọng trường g là đại lượng không đổi đối với mọi vật rơi tự do."}]
                },
                "finishReason": "STOP"
            }
        ]
    }

    def mock_urlopen_explain(req, timeout=None):
        captured_requests.append(req)
        return MockHttpResponse(json.dumps(mock_explain_response).encode("utf-8"))

    with patch("urllib.request.urlopen", side_effect=mock_urlopen_explain):
        explanation = provider.explain_more(
            question={"prompt": "Vật rơi tự do...", "correctOption": "A", "explanation": "g = 9.8"},
            user_selection="B",
            student_query="Tại sao không phụ thuộc vào khối lượng?"
        )
        assert "Gia tốc trọng trường g" in explanation

    assert len(captured_requests) == 1
    req_explain = captured_requests[0]
    # URL must contain ONLY the endpoint and model; NO ?key=... parameter and NO API key in URL!
    assert req_explain.full_url == expected_endpoint
    assert mock_key not in req_explain.full_url
    assert "key=" not in req_explain.full_url
    # Key must be present ONLY in x-goog-api-key header
    assert req_explain.get_header("X-goog-api-key") == mock_key
    assert req_explain.get_header("Content-type") == "application/json"

    body_explain = json.loads(req_explain.data.decode("utf-8"))
    assert body_explain["generationConfig"]["responseMimeType"] == "text/plain"


def test_gemini_error_handling_and_no_fallback(tmp_path):
    mock_key = "mock-error-key"
    mock_model = "gemini-3.8-flash"
    provider = GeminiAiProvider(api_key=mock_key, model=mock_model)

    # 1. HTTP 401 raises AI_AUTH_FAILURE
    http_401 = urllib.error.HTTPError("https://api", 401, "Unauthorized", {}, None)
    with patch("urllib.request.urlopen", side_effect=http_401):
        with pytest.raises(AiProviderError) as exc401:
            provider.generate_questions("physics", 10, "Quang học", "EASY", 1)
        assert exc401.value.code == "AI_AUTH_FAILURE"
        assert exc401.value.status_code == 502

    # 2. HTTP 429 raises AI_RATE_LIMIT
    http_429 = urllib.error.HTTPError("https://api", 429, "Too Many Requests", {}, None)
    with patch("urllib.request.urlopen", side_effect=http_429):
        with pytest.raises(AiProviderError) as exc429:
            provider.generate_questions("physics", 10, "Quang học", "EASY", 1)
        assert exc429.value.code == "AI_RATE_LIMIT"
        assert exc429.value.status_code == 429

    # 3. HTTP 500 raises AI_UPSTREAM_ERROR
    http_500 = urllib.error.HTTPError("https://api", 500, "Internal Server Error", {}, None)
    with patch("urllib.request.urlopen", side_effect=http_500):
        with pytest.raises(AiProviderError) as exc500:
            provider.generate_questions("physics", 10, "Quang học", "EASY", 1)
        assert exc500.value.code == "AI_UPSTREAM_ERROR"
        assert exc500.value.status_code == 502

    # 4. Timeout / URLError raises AI_TIMEOUT
    url_err = urllib.error.URLError("timed out")
    with patch("urllib.request.urlopen", side_effect=url_err):
        with pytest.raises(AiProviderError) as excto:
            provider.generate_questions("physics", 10, "Quang học", "EASY", 1)
        assert excto.value.code == "AI_TIMEOUT"
        assert excto.value.status_code == 504

    # 5. Safety filter block raises AI_SAFETY_BLOCKED
    safety_resp = {
        "candidates": [
            {
                "content": {"parts": [{"text": ""}]},
                "finishReason": "SAFETY"
            }
        ]
    }
    class MockResp:
        def read(self):
            return json.dumps(safety_resp).encode("utf-8")
        def __enter__(self):
            return self
        def __exit__(self, *args):
            pass

    with patch("urllib.request.urlopen", return_value=MockResp()):
        with pytest.raises(AiProviderError) as excsafe:
            provider.generate_questions("physics", 10, "Quang học", "EASY", 1)
        assert excsafe.value.code == "AI_SAFETY_BLOCKED"

    # 6. Verify NO silent fallback to Fake provider when configured with Gemini
    from aimentor.database import Database
    from fastapi import HTTPException
    db = Database(tmp_path / "nofallback_db")
    db.initialize()
    service = AiPracticeService(db, provider=provider)

    # When provider fails with 500 error:
    with patch("urllib.request.urlopen", side_effect=http_500):
        with pytest.raises(HTTPException) as exc_http:
            service.create_session(
                user_id=1,
                subject_id="physics",
                grade=10,
                topic="Quang học",
                difficulty="EASY",
                question_count=1,
                idempotency_key="key-no-fallback-test"
            )
        assert exc_http.value.status_code == 502
        assert exc_http.value.detail["code"] == "AI_UPSTREAM_ERROR"

    # Verify no session was created by fallback
    with db.read() as read_conn:
        sessions = read_conn.execute("SELECT * FROM ai_practice_sessions").fetchall()
        assert len(sessions) == 0
