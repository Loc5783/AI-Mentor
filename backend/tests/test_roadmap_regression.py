"""Comprehensive regression tests for Learning Roadmap (Lộ trình học).
Covers:
1. Draft / Unpublished curriculum rejection.
2. Cross-user ownership isolation (enrollment, attempt, completion, handoff, question bank).
3. Checkpoint review gating at lessons 4, 5, 6.
4. Completed attempt read-back includes authoritative completion object.
5. Idempotency conflict on different payload.
6. PATCH on completed attempt returns 409 ATTEMPT_ALREADY_COMPLETED.
7. Two wrong answers reveal assisted mode and explanation.
8. In-progress attempt rejected when curriculum version changes.
9. Completed attempt readable after curriculum version bump (uses frozen historical snapshot).
10. Completed attempt readable when curriculum is unpublished/removed.
11. Legacy completed attempt without snapshot handled gracefully (historyUnavailable=True).
12. Placement zero-reward baseline verification.
13. Genuine isolation from Library practice and Exam Prep (executes full sessions with baseline).
"""
from copy import deepcopy
from datetime import datetime, timezone
import json
import pytest
from fastapi.testclient import TestClient
from aimentor.config import Settings
from aimentor.database import dump
from aimentor.main import create_app
from test_api import signup, headers, key


def course_content(subject="physics", grade=10, published=True, version="test-v1"):
    lessons = []
    for index in range(10):
        lid = f"{subject}-{grade}-unit-{index + 1}"
        questions = [
            {
                "id": f"{lid}-q{n}",
                "prompt": f"Fixture question {index}/{n}",
                "difficulty": difficulty,
                "options": [{"id": x, "text": f"Option {x}"} for x in ("a", "b", "c")],
                "correctOptionId": "a",
                "explanation": f"Explanation for {lid}-q{n}",
            }
            for n, difficulty in enumerate(("EASY", "MEDIUM", "APPLIED"))
        ]
        lessons.append({
            "id": lid,
            "title": f"Lesson {index + 1}",
            "guidance": [f"Guidance {index + 1}"],
            "examples": [f"Example {index + 1}"],
            "questions": questions,
        })
    return {
        "subjectId": subject,
        "grade": grade,
        "version": version,
        "title": f"{subject.capitalize()} {grade}",
        "sourceNote": "Test fixture",
        "published": published,
        "extendedReviews": False,
        "lessons": lessons,
    }


@pytest.fixture
def api(tmp_path):
    directory = tmp_path / "packs"
    directory.mkdir()
    # Published courses
    (directory / "physics-10.json").write_text(json.dumps(course_content("physics", 10, published=True)), encoding="utf-8")
    (directory / "chemistry-10.json").write_text(json.dumps(course_content("chemistry", 10, published=True)), encoding="utf-8")
    # Draft course (published: False)
    (directory / "math-10.json").write_text(json.dumps(course_content("math", 10, published=False)), encoding="utf-8")

    app = create_app(Settings(tmp_path / "data", curriculum_dir=directory))
    now = [datetime(2026, 10, 20, 10, 0, tzinfo=timezone.utc).timestamp()]
    app.state.learning.clock = lambda: now[0]
    with TestClient(app) as client:
        yield client, app, signup(client, "Learner1"), signup(client, "Learner2"), now


def ok(response, status=201):
    assert response.status_code == status, f"Expected {status}, got {response.status_code}: {response.text}"
    return response.json()


def post(c, s, path, body, k=None):
    return c.post(path, json=body, headers=headers(s, **{"Idempotency-Key": k or key()}))


def enroll(c, s, subject="physics", grade=10):
    return ok(post(c, s, "/enrollments", {"subjectId": subject, "grade": grade}))


def start_placement(c, s, enrollment_id):
    return ok(post(c, s, "/placements", {"enrollmentId": enrollment_id}))


def start_attempt(c, s, enrollment_id, unit_id, version="test-v1"):
    return ok(post(c, s, "/attempts", {"enrollmentId": enrollment_id, "lessonId": unit_id, "curriculumVersion": version}))


def answer_questions(c, s, attempt, correct=True, step=2):
    values = [
        {"questionId": q["id"], "selectedOptionId": "a" if correct else "b", "submissionId": key()}
        for q in attempt["questions"]
    ]
    return ok(c.patch(
        f"/attempts/{attempt['id']}",
        json={"step": step, "answers": values},
        headers=headers(s, **{"If-Match": str(attempt["revision"])}),
    ), 200)


def complete_attempt(c, s, attempt_id, k=None):
    return ok(post(c, s, "/completions", {"attemptId": attempt_id}, k))


def complete_placement_for_user(c, s, enrollment_id):
    p = start_placement(c, s, enrollment_id)
    p_answered = answer_questions(c, s, p, correct=True)
    done = complete_attempt(c, s, p_answered["id"])
    assert done["kind"] == "PLACEMENT"
    assert done["xpAwarded"] == 0
    return done


def test_draft_or_unpublished_curriculum_rejected(api):
    c, app, u1, _, _ = api
    # 1. Draft course with published=False
    r = post(c, u1, "/enrollments", {"subjectId": "math", "grade": 10})
    assert r.status_code == 409
    assert r.json()["code"] == "CURRICULUM_NOT_PUBLISHED"

    # 2. Non-existent curriculum file
    r = post(c, u1, "/enrollments", {"subjectId": "literature", "grade": 10})
    assert r.status_code == 409
    assert r.json()["code"] == "CURRICULUM_NOT_PUBLISHED"


def test_user_ownership_isolation(api):
    c, app, u1, u2, _ = api
    e1 = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e1["id"])
    a1 = start_attempt(c, u1, e1["id"], "physics-10-unit-1")

    # User 2 attempts to access User 1's enrollment path
    r = c.get(f"/enrollments/{e1['id']}/path", headers=headers(u2))
    assert r.status_code == 404

    # User 2 attempts to fetch User 1's attempt
    r = c.get(f"/attempts/{a1['id']}", headers=headers(u2))
    assert r.status_code == 404

    # User 2 attempts to patch User 1's attempt
    r = c.patch(f"/attempts/{a1['id']}", json={"step": 2, "answers": []}, headers=headers(u2, **{"If-Match": "1"}))
    assert r.status_code == 404

    # User 2 attempts to complete User 1's attempt
    r = post(c, u2, "/completions", {"attemptId": a1["id"]})
    assert r.status_code == 404

    # User 2 attempts to handoff User 1's attempt
    r = post(c, u2, f"/attempts/{a1['id']}/handoffs", {"questionId": a1["questions"][0]["id"]})
    assert r.status_code == 404

    # User 2 attempts to query bank with User 1's enrollmentId
    r = c.get(f"/question-bank?enrollmentId={e1['id']}", headers=headers(u2))
    assert r.status_code == 404


def test_checkpoint_gating_at_lessons_4_5_6(api):
    c, app, u1, _, _ = api
    e = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e["id"])

    # Complete Lessons 1, 2, 3, 4
    for i in range(1, 5):
        lid = f"physics-10-unit-{i}"
        att = start_attempt(c, u1, e["id"], lid)
        att = answer_questions(c, u1, att, correct=True)
        res = complete_attempt(c, u1, att["id"])
        assert res["xpAwarded"] == 200

    path = c.get(f"/enrollments/{e['id']}/path", headers=headers(u1)).json()
    u5 = next(x for x in path["units"] if x["id"] == "physics-10-unit-5")
    assert u5["state"] == "AVAILABLE"
    rev1 = next(x for x in path["units"] if x["id"] == "physics-10-review-1")
    assert rev1["state"] == "LOCKED"

    # Complete Lesson 5
    att5 = start_attempt(c, u1, e["id"], "physics-10-unit-5")
    att5 = answer_questions(c, u1, att5, correct=True)
    complete_attempt(c, u1, att5["id"])

    # Now Lesson 5 is completed: review-1 must be AVAILABLE, but Lesson 6 must be LOCKED
    path = c.get(f"/enrollments/{e['id']}/path", headers=headers(u1)).json()
    rev1 = next(x for x in path["units"] if x["id"] == "physics-10-review-1")
    assert rev1["state"] == "AVAILABLE"
    u6 = next(x for x in path["units"] if x["id"] == "physics-10-unit-6")
    assert u6["state"] == "LOCKED"

    # Student tries to bypass review-1 and start Lesson 6 directly -> 409 PREVIOUS_REVIEW_REQUIRED
    r = post(c, u1, "/attempts", {"enrollmentId": e["id"], "lessonId": "physics-10-unit-6", "curriculumVersion": "test-v1"})
    assert r.status_code == 409
    assert r.json()["code"] == "PREVIOUS_REVIEW_REQUIRED"

    # Start and complete review-1
    att_rev = start_attempt(c, u1, e["id"], "physics-10-review-1")
    assert att_rev["kind"] == "FIVE_LESSON_REVIEW"
    assert len(att_rev["questions"]) == 10  # 2 questions from each of 5 lessons
    att_rev = answer_questions(c, u1, att_rev, correct=True)
    done_rev = complete_attempt(c, u1, att_rev["id"])
    assert done_rev["xpAwarded"] == 400

    # Now Lesson 6 must be UNLOCKED and AVAILABLE
    path = c.get(f"/enrollments/{e['id']}/path", headers=headers(u1)).json()
    u6 = next(x for x in path["units"] if x["id"] == "physics-10-unit-6")
    assert u6["state"] == "AVAILABLE"

    # Start Lesson 6 succeeds
    att6 = start_attempt(c, u1, e["id"], "physics-10-unit-6")
    assert att6["id"] is not None


def test_completed_attempt_readback_includes_completion(api):
    c, app, u1, _, _ = api
    e = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e["id"])

    att = start_attempt(c, u1, e["id"], "physics-10-unit-1")
    att = answer_questions(c, u1, att, correct=True)
    comp = complete_attempt(c, u1, att["id"])

    # Read back completed attempt via GET /attempts/{id}
    res = c.get(f"/attempts/{att['id']}", headers=headers(u1))
    assert res.status_code == 200
    data = res.json()
    assert data["state"] == "COMPLETED"
    assert "completion" in data
    assert data["completion"]["id"] == comp["id"]
    assert data["completion"]["xpAwarded"] == 200
    assert data["completion"]["attemptId"] == att["id"]


def test_idempotency_conflict_on_different_payload(api):
    c, app, u1, _, _ = api
    e = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e["id"])

    att1 = start_attempt(c, u1, e["id"], "physics-10-unit-1")
    att1 = answer_questions(c, u1, att1, correct=True)

    shared_key = key()
    # First complete with att1 and clientCompletedAt=T1
    comp1 = ok(post(c, u1, "/completions", {
        "attemptId": att1["id"],
        "clientCompletedAt": "2026-10-20T10:00:00Z",
    }, k=shared_key))
    assert comp1["xpAwarded"] == 200

    # Repeat complete with att1 and same key and same payload -> succeeds and replays
    same = post(c, u1, "/completions", {
        "attemptId": att1["id"],
        "clientCompletedAt": "2026-10-20T10:00:00Z",
    }, k=shared_key)
    assert same.status_code == 201
    assert same.json()["id"] == comp1["id"]

    # Repeat complete with same key and DIFFERENT payload -> 409 IDEMPOTENCY_CONFLICT
    conflict = post(c, u1, "/completions", {
        "attemptId": att1["id"],
        "clientCompletedAt": "2026-10-20T10:05:00Z",
    }, k=shared_key)
    assert conflict.status_code == 409
    assert conflict.json()["code"] == "IDEMPOTENCY_CONFLICT"


def test_patch_on_completed_attempt_returns_409_already_completed(api):
    c, app, u1, _, _ = api
    e = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e["id"])

    att = start_attempt(c, u1, e["id"], "physics-10-unit-1")
    att = answer_questions(c, u1, att, correct=True)
    complete_attempt(c, u1, att["id"])

    # If client sends old revision before completion -> 412 REVISION_CONFLICT
    r_old = c.patch(
        f"/attempts/{att['id']}",
        json={"step": 2, "answers": []},
        headers=headers(u1, **{"If-Match": str(att["revision"])}),
    )
    assert r_old.status_code == 412

    # If client sends current revision after completion -> 409 ATTEMPT_ALREADY_COMPLETED
    r_current = c.patch(
        f"/attempts/{att['id']}",
        json={"step": 2, "answers": []},
        headers=headers(u1, **{"If-Match": str(att["revision"] + 1)}),
    )
    assert r_current.status_code == 409
    assert r_current.json()["code"] == "ATTEMPT_ALREADY_COMPLETED"


def test_two_wrong_answers_reveal_explanation_and_assisted(api):
    c, app, u1, _, _ = api
    e = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e["id"])

    att = start_attempt(c, u1, e["id"], "physics-10-unit-1")
    q0 = att["questions"][0]
    qid = q0["id"]

    # Initially, no explanation or correctOptionId
    assert "explanation" not in q0
    assert "correctOptionId" not in q0

    # 1st wrong answer (option 'b')
    sub1 = key()
    r1 = ok(c.patch(
        f"/attempts/{att['id']}",
        json={"step": 2, "answers": [{"questionId": qid, "selectedOptionId": "b", "submissionId": sub1}]},
        headers=headers(u1, **{"If-Match": str(att["revision"])}),
    ), 200)
    ans1 = next(a for a in r1["answers"] if a["questionId"] == qid)
    assert ans1["wrongCount"] == 1
    assert not ans1["assisted"]
    q1 = next(q for q in r1["questions"] if q["id"] == qid)
    assert "explanation" not in q1
    assert "correctOptionId" not in q1

    # 2nd wrong answer (option 'c')
    sub2 = key()
    r2 = ok(c.patch(
        f"/attempts/{att['id']}",
        json={"step": 2, "answers": [{"questionId": qid, "selectedOptionId": "c", "submissionId": sub2}]},
        headers=headers(u1, **{"If-Match": str(r1["revision"])}),
    ), 200)
    ans2 = next(a for a in r2["answers"] if a["questionId"] == qid)
    assert ans2["wrongCount"] == 2
    assert ans2["assisted"]
    q2 = next(q for q in r2["questions"] if q["id"] == qid)
    assert "explanation" in q2
    assert q2["correctOptionId"] == "a"


def test_in_progress_attempt_conflict_on_version_change(api):
    c, app, u1, _, _ = api
    e = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e["id"])

    # Start attempt with test-v1
    att = start_attempt(c, u1, e["id"], "physics-10-unit-1", version="test-v1")

    # Simulate catalog updating version to test-v2
    app.state.learning.catalog.courses[("physics", 10)]["version"] = "test-v2"

    # GET on in-progress attempt with old curriculumVersion -> 409 CURRICULUM_VERSION_CONFLICT
    r_get = c.get(f"/attempts/{att['id']}", headers=headers(u1))
    assert r_get.status_code == 409
    assert r_get.json()["code"] == "CURRICULUM_VERSION_CONFLICT"

    # Trying to patch attempt with old curriculumVersion -> 409 CURRICULUM_VERSION_CONFLICT
    r = c.patch(
        f"/attempts/{att['id']}",
        json={"step": 2, "answers": []},
        headers=headers(u1, **{"If-Match": str(att["revision"])}),
    )
    assert r.status_code == 409
    assert r.json()["code"] == "CURRICULUM_VERSION_CONFLICT"

    # Trying to complete attempt with old curriculumVersion -> 409 CURRICULUM_VERSION_CONFLICT
    r = post(c, u1, "/completions", {"attemptId": att["id"]})
    assert r.status_code == 409
    assert r.json()["code"] == "CURRICULUM_VERSION_CONFLICT"


def test_completed_attempt_readable_after_curriculum_version_bump(api):
    c, app, u1, _, _ = api
    e = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e["id"])

    # Complete lesson 1 under test-v1
    att = start_attempt(c, u1, e["id"], "physics-10-unit-1", version="test-v1")
    att = answer_questions(c, u1, att, correct=True)
    comp = complete_attempt(c, u1, att["id"])
    assert comp["xpAwarded"] == 200

    # Initial readback under test-v1 succeeds
    res_before = c.get(f"/attempts/{att['id']}", headers=headers(u1))
    assert res_before.status_code == 200
    assert res_before.json()["state"] == "COMPLETED"
    assert res_before.json()["questions"][0]["prompt"] == "Fixture question 0/0"

    # Advance catalog to test-v2 with totally DIFFERENT question prompt
    v2_course = deepcopy(app.state.learning.catalog.courses[("physics", 10)])
    v2_course["version"] = "test-v2"
    v2_course["lessons"][0]["questions"][0]["prompt"] = "MODIFIED QUESTION PROMPT FOR V2"
    app.state.learning.catalog.courses[("physics", 10)] = v2_course

    # READBACK AFTER VERSION BUMP: MUST SUCCEED WITH 200 AND PRESERVE V1 QUESTIONS!
    res_after = c.get(f"/attempts/{att['id']}", headers=headers(u1))
    assert res_after.status_code == 200
    body = res_after.json()
    assert body["state"] == "COMPLETED"
    assert "completion" in body
    assert body["completion"]["id"] == comp["id"]
    assert body["completion"]["xpAwarded"] == 200
    # Must use frozen snapshot prompt, NEVER the new v2 prompt!
    assert body["questions"][0]["prompt"] == "Fixture question 0/0"
    assert body["questions"][0]["prompt"] != "MODIFIED QUESTION PROMPT FOR V2"

    # Repeated readback does NOT create new completions or change awards
    user_id = int(u1["userId"])
    with app.state.db.read() as db:
        awards_count = db.execute("SELECT COUNT(*) FROM awards WHERE user_id=?", (user_id,)).fetchone()[0]
        comp_count = db.execute("SELECT COUNT(*) FROM completions WHERE user_id=?", (user_id,)).fetchone()[0]
    assert awards_count == 1
    assert comp_count == 2  # placement + unit-1


def test_completed_attempt_readable_when_curriculum_unpublished(api):
    c, app, u1, _, _ = api
    e = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e["id"])

    # Complete lesson 1
    att = start_attempt(c, u1, e["id"], "physics-10-unit-1", version="test-v1")
    att = answer_questions(c, u1, att, correct=True)
    comp = complete_attempt(c, u1, att["id"])

    # Course is completely removed/unpublished from active catalog
    del app.state.learning.catalog.courses[("physics", 10)]

    # Readback of completed attempt MUST still succeed from snapshot!
    res = c.get(f"/attempts/{att['id']}", headers=headers(u1))
    assert res.status_code == 200
    data = res.json()
    assert data["state"] == "COMPLETED"
    assert data["completion"]["id"] == comp["id"]
    assert len(data["questions"]) == 3
    assert data["questions"][0]["prompt"] == "Fixture question 0/0"


def test_legacy_completed_attempt_without_snapshot_handled_gracefully(api):
    c, app, u1, _, now = api
    e = enroll(c, u1, "physics", 10)
    user_id = int(u1["userId"])

    # Manually insert a legacy completed attempt without unit_snapshot (unit_snapshot = NULL)
    legacy_att_id = "legacy-att-999"
    legacy_comp_id = "legacy-comp-999"
    comp_result = {
        "id": legacy_comp_id, "attemptId": legacy_att_id, "rewardUnitId": "physics-10-unit-1",
        "kind": "LESSON", "xpAwarded": 200, "firstRewardForUnit": True, "streakLit": True,
        "streakDays": 1, "dailyXp": 200, "learningDay": "2026-10-20", "timezone": "Asia/Ho_Chi_Minh",
        "completedAt": "2026-10-20T10:00:00Z", "replayed": False, "revision": 1
    }
    with app.state.db.transaction() as db:
        db.execute(
            "INSERT INTO attempts(id, user_id, enrollment_id, unit_id, kind, curriculum_version, step, answers, state, revision, updated_at, unit_snapshot) "
            "VALUES(?, ?, ?, ?, 'LESSON', 'test-v1', 2, '[]', 'COMPLETED', 2, ?, NULL)",
            (legacy_att_id, user_id, e["id"], "physics-10-unit-1", now[0])
        )
        db.execute(
            "INSERT INTO completions(id, attempt_id, user_id, xp, day, result, completed_at) VALUES(?, ?, ?, ?, ?, ?, ?)",
            (legacy_comp_id, legacy_att_id, user_id, 200, "2026-10-20", dump(comp_result), now[0])
        )

    # Catalog is now on test-v2 (so historical questions cannot be reconstructed from course)
    app.state.learning.catalog.courses[("physics", 10)]["version"] = "test-v2"

    # GET /attempts/{legacy_att_id}: MUST NOT CRASH, returns 200 with historyUnavailable=True
    res = c.get(f"/attempts/{legacy_att_id}", headers=headers(u1))
    assert res.status_code == 200
    data = res.json()
    assert data["state"] == "COMPLETED"
    assert data["historyUnavailable"] is True
    assert "[Phiên học lịch sử:" in data["historyMessage"]
    assert data["completion"]["id"] == legacy_comp_id
    assert data["questions"] == []


def test_placement_zero_rewards_baseline(api):
    c, app, u1, _, _ = api
    user_id = int(u1["userId"])
    e = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e["id"])

    with app.state.db.read() as db:
        awards = db.execute("SELECT COUNT(*) FROM awards WHERE user_id=?", (user_id,)).fetchone()[0]
        days = db.execute("SELECT COUNT(*) FROM learning_days WHERE user_id=?", (user_id,)).fetchone()[0]

    assert awards == 0
    assert days == 0

    dashboard = c.get("/me/dashboard", params={"grade": 10}, headers=headers(u1)).json()
    assert dashboard["streakDays"] == 0
    assert dashboard["dailyXp"] == 0
    assert dashboard["totalXp"] == 0


def test_isolation_from_library_and_exam_prep(api):
    c, app, u1, _, now = api
    user_id = int(u1["userId"])
    e = enroll(c, u1, "physics", 10)
    complete_placement_for_user(c, u1, e["id"])

    # 1. ESTABLISH NON-ZERO ROADMAP BASELINE: Complete Lesson 1
    att = start_attempt(c, u1, e["id"], "physics-10-unit-1")
    att = answer_questions(c, u1, att, correct=True)
    comp = complete_attempt(c, u1, att["id"])
    assert comp["xpAwarded"] == 200

    # Record baseline
    with app.state.db.read() as db:
        baseline_awards = db.execute("SELECT COUNT(*) FROM awards WHERE user_id=?", (user_id,)).fetchone()[0]
        baseline_days = db.execute("SELECT COUNT(*) FROM learning_days WHERE user_id=?", (user_id,)).fetchone()[0]
    assert baseline_awards == 1
    assert baseline_days == 1

    dashboard_before = c.get("/me/dashboard", params={"grade": 10}, headers=headers(u1)).json()
    assert dashboard_before["totalXp"] == 200
    assert dashboard_before["dailyXp"] == 200
    assert dashboard_before["streakDays"] == 1
    assert dashboard_before["completedLessons"] == 1

    # 2. RUN REAL LIBRARY PRACTICE FLOW
    # Seed 2 questions in question bank
    qid1 = "lib-q-1"
    qid2 = "lib-q-2"
    with app.state.db.transaction() as db:
        db.execute(
            "INSERT INTO questions(id, code, version, subject_id, grade, chapter, topic, prompt, option_a, option_b, option_c, option_d, correct_option, explanation, difficulty, points, status, created_at, updated_at) "
            "VALUES(?, 'LIB01', 1, 'physics', 10, 'Chương 1', 'Động học', 'Câu hỏi Library 1?', 'A', 'B', 'C', 'D', 'A', 'Giải thích 1', 'EASY', 10, 'PUBLISHED', ?, ?)",
            (qid1, now[0], now[0])
        )
        db.execute(
            "INSERT INTO questions(id, code, version, subject_id, grade, chapter, topic, prompt, option_a, option_b, option_c, option_d, correct_option, explanation, difficulty, points, status, created_at, updated_at) "
            "VALUES(?, 'LIB02', 1, 'physics', 10, 'Chương 1', 'Động học', 'Câu hỏi Library 2?', 'A', 'B', 'C', 'D', 'B', 'Giải thích 2', 'EASY', 10, 'PUBLISHED', ?, ?)",
            (qid2, now[0], now[0])
        )
        quiz_id = "quiz-isolation-1"
        db.execute(
            "INSERT INTO library_quizzes(id, title, subject_id, grade, chapter, topic, difficulty, question_ids, status, created_at) "
            "VALUES(?, 'Bộ đề Library test', 'physics', 10, 'Chương 1', 'Động học', 'EASY', ?, 'PUBLISHED', ?)",
            (quiz_id, json.dumps([qid1, qid2]), now[0])
        )

    # Start library practice session
    lib_sess = ok(c.post("/library/sessions", json={"quizId": quiz_id}, headers=headers(u1)))
    sess_id = lib_sess["id"]

    # Submit answer: q1 correct ('A'), q2 incorrect ('C') to test wrong questions notebook
    c.post(f"/library/sessions/{sess_id}/answers", json={"questionId": qid1, "selectedOption": "A"}, headers=headers(u1))
    c.post(f"/library/sessions/{sess_id}/answers", json={"questionId": qid2, "selectedOption": "C"}, headers=headers(u1))

    # Complete library session
    lib_done = ok(c.post(f"/library/sessions/{sess_id}/complete", json={}, headers=headers(u1)), 200)
    assert lib_done["state"] == "COMPLETED"
    assert lib_done["correctCount"] == 1
    assert lib_done["totalCount"] == 2

    # Check wrong questions notebook
    with app.state.db.read() as db:
        wrong_count = db.execute("SELECT COUNT(*) FROM wrong_questions_notebook WHERE user_id=?", (user_id,)).fetchone()[0]
    assert wrong_count == 1

    # 3. RUN REAL EXAM PREP FLOW
    exam_id = "exam-isolation-1"
    with app.state.db.transaction() as db:
        db.execute(
            "INSERT INTO exams(id, title, subject_id, grade, exam_type, semester, duration_minutes, question_ids, total_points, question_count, status, version, created_at, updated_at) "
            "VALUES(?, 'Đề thi Ôn tập test', 'physics', 10, 'MIDTERM', 1, 45, ?, 20, 2, 'PUBLISHED', 1, ?, ?)",
            (exam_id, json.dumps([qid1, qid2]), now[0], now[0])
        )

    # Start exam session
    exam_sess = ok(c.post("/exams/sessions", json={"examId": exam_id}, headers=headers(u1)))
    exam_sess_id = exam_sess["id"]

    # Save answer in exam session
    c.post(
        f"/exams/sessions/{exam_sess_id}/answers",
        json={"questionId": qid1, "selectedOption": "A", "revision": 1},
        headers=headers(u1)
    )

    # Submit exam session
    exam_done = ok(c.post(f"/exams/sessions/{exam_sess_id}/submit", json={}, headers=headers(u1)), 200)
    assert exam_done["state"] == "SUBMITTED"

    # 4. VERIFY STRICT ISOLATION OF ROADMAP REWARDS & STREAKS
    with app.state.db.read() as db:
        final_awards = db.execute("SELECT COUNT(*) FROM awards WHERE user_id=?", (user_id,)).fetchone()[0]
        final_days = db.execute("SELECT COUNT(*) FROM learning_days WHERE user_id=?", (user_id,)).fetchone()[0]

    # Awards and learning days in SQLite MUST REMAIN EXACTLY AS BEFORE!
    assert final_awards == baseline_awards == 1
    assert final_days == baseline_days == 1

    # Dashboard values MUST REMAIN 100% UNCHANGED!
    dashboard_after = c.get("/me/dashboard", params={"grade": 10}, headers=headers(u1)).json()
    assert dashboard_after["totalXp"] == baseline_awards * 200 == 200
    assert dashboard_after["dailyXp"] == 200
    assert dashboard_after["streakDays"] == 1
    assert dashboard_after["completedLessons"] == 1
