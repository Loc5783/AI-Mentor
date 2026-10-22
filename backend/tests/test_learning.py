"""Real API/SQLite regression tests. Synthetic lessons never ship as curriculum."""
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import json
import pytest
from fastapi.testclient import TestClient
from aimentor.config import Settings
from aimentor.main import create_app
from aimentor.learning import Catalog
from test_api import signup, headers, key


def content(subject="physics", grade=10):
    lessons = []
    for index in range(10):
        lid = f"{subject}-{grade}-unit-{index + 1}"
        questions = [{"id": f"{lid}-q{n}", "prompt": f"Fixture {index}/{n}",
                      "difficulty": difficulty, "options": [{"id": x, "text": x} for x in ("a", "b", "c")],
                      "correctOptionId": "a", "explanation": "Fixture explanation"}
                     for n, difficulty in enumerate(("EASY", "MEDIUM", "APPLIED"))]
        lessons.append({"id": lid, "title": f"Fixture lesson {index + 1}", "guidance": ["Fixture guidance"],
                        "examples": ["Fixture example"], "questions": questions})
    return {"subjectId": subject, "grade": grade, "version": "test-v1", "title": "Test only",
            "sourceNote": "Test fixture, not curriculum", "published": True,
            "extendedReviews": grade == 12, "lessons": lessons}


@pytest.fixture
def api(tmp_path):
    directory = tmp_path / "packs"
    directory.mkdir()
    for subject in ("biology", "chemistry", "physics"):
        for grade in (10, 11, 12):
            (directory / f"{subject}-{grade}.json").write_text(json.dumps(content(subject, grade)), encoding="utf-8")
    app = create_app(Settings(tmp_path / "data", curriculum_dir=directory))
    now = [datetime(2026, 10, 17, 16, 59, tzinfo=timezone.utc).timestamp()]
    app.state.learning.clock = lambda: now[0]
    with TestClient(app) as client:
        yield client, app, signup(client, "Learner"), now


def post(c, s, path, body, k=None):
    return c.post(path, json=body, headers=headers(s, **{"Idempotency-Key": k or key()}))


def ok(response, status=201):
    assert response.status_code == status, response.text
    return response.json()


def enroll(c, s, grade=10):
    return ok(post(c, s, "/enrollments", {"subjectId": "physics", "grade": grade}))


def start(c, s, e, unit):
    return ok(post(c, s, "/attempts", {"enrollmentId": e["id"], "lessonId": unit, "curriculumVersion": "test-v1"}))


def answer(c, s, a, values=None, step=2):
    values = values if values is not None else [{"questionId": q["id"], "selectedOptionId": "a", "submissionId": key()} for q in a["questions"]]
    return ok(c.patch("/attempts/" + a["id"], json={"step": step, "answers": values},
                      headers=headers(s, **{"If-Match": str(a["revision"])})), 200)


def finish(c, s, a, k=None):
    return ok(post(c, s, "/completions", {"attemptId": a["id"]}, k))


def placed(c, s, e):
    a = ok(post(c, s, "/placements", {"enrollmentId": e["id"]}))
    result = finish(c, s, answer(c, s, a))
    assert result["xpAwarded"] == 0 and not result["streakLit"]


def test_placement_required_final_answers_no_rewards(api):
    c, app, s, _ = api
    e = enroll(c, s)
    path = c.get(f"/enrollments/{e['id']}/path", headers=headers(s)).json()
    assert all(x["state"] == "LOCKED" for x in path["units"])
    r = post(c, s, "/attempts", {"enrollmentId": e["id"], "lessonId": "physics-10-unit-1", "curriculumVersion": "test-v1"})
    assert r.status_code == 409 and r.json()["code"] == "PLACEMENT_REQUIRED"
    a = ok(post(c, s, "/placements", {"enrollmentId": e["id"]}))
    assert len(a["questions"]) == 10
    assert all("correctOptionId" not in q and "explanation" not in q for q in a["questions"])
    a = answer(c, s, a, [{"questionId": a["questions"][0]["id"], "selectedOptionId": "b", "submissionId": key()}])
    r = c.patch("/attempts/" + a["id"], json={"step": 2, "answers": [
        {"questionId": a["questions"][0]["id"], "selectedOptionId": "a", "submissionId": key()}]},
        headers=headers(s, **{"If-Match": str(a["revision"])}))
    assert r.status_code == 409 and r.json()["code"] == "PLACEMENT_ANSWER_FINAL"
    a = answer(c, s, a, [{"questionId": q["id"], "selectedOptionId": "a", "submissionId": key()} for q in a["questions"][1:]])
    done = finish(c, s, a)
    assert done["assessment"]["correct"] == 9 and done["assessment"]["total"] == 10
    assert done["xpAwarded"] == 0 and not done["streakLit"]
    path = c.get(f"/enrollments/{e['id']}/path", headers=headers(s)).json()
    assert path["units"][0]["state"] == "AVAILABLE" and path["units"][1]["state"] == "LOCKED"


def test_first_reward_retake_new_day_and_replay_no_new_streak(api):
    c, app, s, now = api
    e = enroll(c, s)
    placed(c, s, e)
    a = answer(c, s, start(c, s, e, "physics-10-unit-1"))
    k = key()
    first = finish(c, s, a, k)
    assert first["xpAwarded"] == 200 and first["streakLit"] and first["streakDays"] == 1
    assert first["learningDay"] == "2026-10-17"
    now[0] += 120  # Midnight in Vietnam, not UTC; client timestamps cannot change it.
    same, replay = finish(c, s, a, k), finish(c, s, a)
    assert same["id"] == replay["id"] == first["id"] and replay["replayed"]
    with app.state.db.read() as db:
        assert db.execute("SELECT COUNT(*) FROM learning_days").fetchone()[0] == 1
    retake = finish(c, s, answer(c, s, start(c, s, e, "physics-10-unit-1")))
    assert retake["xpAwarded"] == 0 and not retake["firstRewardForUnit"]
    assert retake["streakLit"] and retake["streakDays"] == 2 and retake["dailyXp"] == 0
    assert enroll(c, s)["id"] == e["id"]


@pytest.mark.parametrize("grade,count", [(10, 10), (12, 15)])
def test_review_exact_five_sources_and_blocks_sixth_lesson(api, grade, count):
    c, _, s, _ = api
    e = enroll(c, s, grade)
    placed(c, s, e)
    prefix = f"physics-{grade}"
    for n in range(1, 6):
        finish(c, s, answer(c, s, start(c, s, e, f"{prefix}-unit-{n}")))
    r = post(c, s, "/attempts", {"enrollmentId": e["id"], "lessonId": f"{prefix}-unit-6", "curriculumVersion": "test-v1"})
    assert r.status_code == 409 and r.json()["code"] == "PREVIOUS_REVIEW_REQUIRED"
    review = start(c, s, e, f"{prefix}-review-1")
    assert len(review["questions"]) == count
    assert review["sourceLessonIds"] == [f"{prefix}-unit-{n}" for n in range(1, 6)]
    done = finish(c, s, answer(c, s, review))
    assert done["xpAwarded"] == 400 and done["dailyXp"] == 1400
    for n in range(6, 11):
        finish(c, s, answer(c, s, start(c, s, e, f"{prefix}-unit-{n}")))
    review = start(c, s, e, f"{prefix}-review-2")
    assert review["sourceLessonIds"] == [f"{prefix}-unit-{n}" for n in range(6, 11)]


def test_two_wrong_submissions_give_help_retry_does_not_double_count(api):
    c, _, s, _ = api
    e = enroll(c, s)
    placed(c, s, e)
    a = start(c, s, e, "physics-10-unit-1")
    value = {"questionId": a["questions"][0]["id"], "selectedOptionId": "b", "submissionId": key()}
    a = answer(c, s, a, [value])
    assert a["answers"][0]["wrongCount"] == 1 and "explanation" not in a["questions"][0]
    a = answer(c, s, a, [value])
    assert a["answers"][0]["wrongCount"] == 1
    a = answer(c, s, a, [{**value, "submissionId": key()}])
    assert a["answers"][0]["wrongCount"] == 2 and a["answers"][0]["assisted"]
    assert a["questions"][0]["explanation"] == "Fixture explanation"
    assert post(c, s, "/completions", {"attemptId": a["id"]}).status_code == 409
    assert not answer(c, s, a)["answers"][0]["firstTryCorrect"]


def test_answer_order_varies_by_attempt_and_survives_reload(api):
    c, _, s, _ = api
    e = enroll(c, s)
    placed(c, s, e)
    orders = set()
    for _ in range(12):
        a = start(c, s, e, "physics-10-unit-1")
        displayed = [[option["id"] for option in q["options"]] for q in a["questions"]]
        reloaded = c.get("/attempts/" + a["id"], headers=headers(s)).json()
        assert displayed == [[option["id"] for option in q["options"]] for q in reloaded["questions"]]
        orders.add(tuple(tuple(ids) for ids in displayed))
        finish(c, s, answer(c, s, a))
    assert len(orders) > 1


def test_handoff_keeps_newer_progress_owner_and_no_auto_completion(api):
    c, app, s, _ = api
    other = signup(c, "Other")
    e = enroll(c, s)
    placed(c, s, e)
    a = start(c, s, e, "physics-10-unit-1")
    a = answer(c, s, a, [{"questionId": a["questions"][0]["id"], "selectedOptionId": "b", "submissionId": key()}], 1)
    handoff = ok(post(c, s, f"/attempts/{a['id']}/handoffs", {"questionId": a["questions"][0]["id"]}))
    chat = ok(post(c, s, "/conversations", {"grade": 10, "handoff": {"id": handoff["id"]}}))
    assert chat["handoff"]["attemptId"] == a["id"]
    assert post(c, other, "/conversations", {"grade": 10, "handoff": {"id": handoff["id"]}}).status_code == 404
    a = answer(c, s, a, [{"questionId": a["questions"][1]["id"], "selectedOptionId": "a", "submissionId": key()}], 3)
    result = c.get(f"/lesson-handoffs/{handoff['id']}/resume", headers=headers(s)).json()
    assert result["attempt"]["step"] == 3 and result["attempt"]["revision"] == a["revision"]
    assert result["attempt"]["state"] == "IN_PROGRESS"
    assert c.get("/attempts/" + a["id"], headers=headers(other)).status_code == 404
    bank = c.get("/question-bank", params={"enrollmentId": e["id"]}, headers=headers(s)).json()
    assert len(bank["items"]) == 2 and not bank["offlinePracticeAwardsXp"]
    assert c.get("/question-bank", params={"enrollmentId": e["id"]}, headers=headers(other)).status_code == 404
    with app.state.db.read() as db:
        assert db.execute("SELECT COUNT(*) FROM awards").fetchone()[0] == 0


def test_stale_revision_and_fake_reward_fields_rejected(api):
    c, _, s, _ = api
    e = enroll(c, s)
    placed(c, s, e)
    a = start(c, s, e, "physics-10-unit-1")
    newer = answer(c, s, a)
    r = c.patch("/attempts/" + a["id"], json={"step": 0, "answers": []}, headers=headers(s, **{"If-Match": "1"}))
    assert r.status_code == 412
    assert post(c, s, "/completions", {"attemptId": a["id"], "xpAwarded": 99999}).status_code == 422
    r = c.patch("/attempts/" + a["id"], json={"step": 1, "answers": [
        {"questionId": a["questions"][0]["id"], "selectedOptionId": "a", "submissionId": key(), "wrongCount": 0}]},
        headers=headers(s, **{"If-Match": str(newer["revision"])}))
    assert r.status_code == 422


def test_concurrent_completions_award_once(api):
    c, app, s, _ = api
    e = enroll(c, s)
    placed(c, s, e)
    a = answer(c, s, start(c, s, e, "physics-10-unit-1"))
    def complete(_):
        return app.state.learning.complete(int(s["userId"]), {"attemptId": a["id"]}, key())
    with ThreadPoolExecutor(max_workers=6) as pool:
        results = list(pool.map(complete, range(12)))
    assert len({r["id"] for r in results}) == 1
    with app.state.db.read() as db:
        assert db.execute("SELECT SUM(xp) FROM awards").fetchone()[0] == 200
        assert db.execute("SELECT COUNT(*) FROM learning_days").fetchone()[0] == 1


def test_catalog_rejects_duplicate_question_and_framework_course():
    course = content()
    course["lessons"][1]["questions"][0]["id"] = course["lessons"][0]["questions"][0]["id"]
    with pytest.raises(ValueError):
        Catalog.validate(course)
    with pytest.raises(ValueError):
        Catalog.validate(content("invalid_subject"))
