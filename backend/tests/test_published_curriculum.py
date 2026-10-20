"""Exercise shipped lessons, their four reviews and append-only upgrades through real APIs."""
from copy import deepcopy
import hashlib
import json
from pathlib import Path
import subprocess
import sys

from fastapi.testclient import TestClient
import pytest

from aimentor.config import Settings
from aimentor.learning import Catalog
from aimentor.main import create_app
from test_api import headers, key, signup
from test_learning import answer, finish, ok, post


BACKEND = Path(__file__).resolve().parents[1]
PACKS = BACKEND / "curriculum"
ORIGINAL_DIGESTS = {
    "physics": "f1dbf0462dad850a0c42c55b64e2fad1746c3532c0910c85764de182711ce6f4",
    "chemistry": "ec871d5417ab28876708777f3b7bf290101fdf57d8cec5557e709ed0ff1d285d",
    "biology": "82164b79e9f83e87de8f342d530fb390d8f0a4ad9629428aeda0a37063980dd0",
}


def course(subject):
    return json.loads((PACKS / f"{subject}-10.json").read_text(encoding="utf-8"))


def solve(client, session, attempt, content):
    solutions = {q["id"]: q["correctOptionId"] for l in content["lessons"] for q in l["questions"]}
    return answer(client, session, attempt, [
        {"questionId": q["id"], "selectedOptionId": solutions[q["id"]], "submissionId": key()}
        for q in attempt["questions"]])


def begin(client, session, enrollment, unit, content):
    return ok(post(client, session, "/attempts", {"enrollmentId": enrollment["id"],
                   "lessonId": unit, "curriculumVersion": content["version"]}))


def prepare(client, session, content):
    enrollment = ok(post(client, session, "/enrollments", {"subjectId": content["subjectId"], "grade": 10}))
    placement = ok(post(client, session, "/placements", {"enrollmentId": enrollment["id"]}))
    completed = finish(client, session, solve(client, session, placement, content))
    assert completed["xpAwarded"] == 0 and not completed["streakLit"]
    return enrollment


def test_generation_reproduces_shipped_packs_and_android_fixture():
    before = {p: p.read_bytes() for p in PACKS.glob("*.json")}
    subprocess.run([sys.executable, str(BACKEND / "tools" / "build_intro_curriculum.py")], check=True)
    assert all(p.read_bytes() == value for p, value in before.items())
    assert (BACKEND.parent / "app/src/androidTest/assets/physics-10.json").read_bytes() == (PACKS / "physics-10.json").read_bytes()


@pytest.mark.parametrize("subject", ORIGINAL_DIGESTS)
def test_packs_preserve_old_questions_and_cover_four_disjoint_blocks(subject):
    content = course(subject)
    Catalog.validate(content)
    assert len(content["lessons"]) == 20
    original = json.dumps(content["lessons"][:5], ensure_ascii=False, sort_keys=True).encode()
    assert hashlib.sha256(original).hexdigest() == ORIGINAL_DIGESTS[subject]
    assert content["version"] == "intro-2026.1"  # Append-compatible, no re-enrollment.
    assert content["coverage"] == "PARTIAL_GRADE_10"
    catalog = Catalog(PACKS)
    all_review_questions = []
    for end in (5, 10, 15, 20):
        _, _, questions, sources = catalog.unit(content, catalog.review_id(content, end))
        block = content["lessons"][end - 5:end]
        assert sources == [l["id"] for l in block]
        assert len(questions) == 10
        assert {q["id"] for q in questions} == {q["id"] for l in block for q in l["questions"][:2]}
        all_review_questions.extend(q["id"] for q in questions)
    assert len(set(all_review_questions)) == 40
    for lesson in content["lessons"]:
        for q in lesson["questions"]:
            texts = [o["text"].strip().casefold() for o in q["options"]]
            assert len(texts) == len(set(texts)), q["id"]
    assert (subject, 11) not in catalog.courses and (subject, 12) not in catalog.courses


@pytest.mark.parametrize("subject", ORIGINAL_DIGESTS)
def test_all_twenty_lessons_and_four_reviews_reward_once_and_gate_next_block(tmp_path, subject):
    content = course(subject)
    app = create_app(Settings(tmp_path))
    with TestClient(app) as client:
        session = signup(client, "FullCourse")
        enrollment = prepare(client, session, content)
        total = 0
        path_url = f"/enrollments/{enrollment['id']}/path"
        path = client.get(path_url, headers=headers(session)).json()
        assert len(path["units"]) == 24
        for index, lesson in enumerate(content["lessons"]):
            result = finish(client, session, solve(client, session,
                            begin(client, session, enrollment, lesson["id"], content), content))
            total += 200
            assert result["xpAwarded"] == 200 and result["dailyXp"] == total
            assert result["streakDays"] == 1
            if (index + 1) % 5:
                continue
            if index + 1 < 20:
                blocked = post(client, session, "/attempts", {"enrollmentId": enrollment["id"],
                               "lessonId": content["lessons"][index + 1]["id"], "curriculumVersion": content["version"]})
                assert blocked.status_code == 409 and blocked.json()["code"] == "PREVIOUS_REVIEW_REQUIRED"
            review = begin(client, session, enrollment, Catalog.review_id(content, index + 1), content)
            assert len(review["questions"]) == 10
            assert all("correctOptionId" not in q for q in review["questions"])
            assert review["sourceLessonIds"] == [l["id"] for l in content["lessons"][index - 4:index + 1]]
            solved = solve(client, session, review, content)
            result = finish(client, session, solved)
            total += 400
            assert result["xpAwarded"] == 400 and result["dailyXp"] == total
            assert finish(client, session, solved)["replayed"]
        assert total == 5600
        path = client.get(path_url, headers=headers(session)).json()
        assert all(unit["state"] == "COMPLETED" for unit in path["units"])
        repeated = finish(client, session, solve(client, session,
                          begin(client, session, enrollment, content["lessons"][-1]["id"], content), content))
        assert repeated["xpAwarded"] == 0 and repeated["dailyXp"] == 5600


@pytest.mark.parametrize("subject", ORIGINAL_DIGESTS)
def test_upgrade_keeps_completed_block_pending_attempt_and_awards(tmp_path, subject):
    current = course(subject)
    old = deepcopy(current)
    old["lessons"] = old["lessons"][:5]
    old_dir = tmp_path / "old"
    old_dir.mkdir()
    (old_dir / f"{subject}-10.json").write_text(json.dumps(old), encoding="utf-8")
    data = tmp_path / "database"
    with TestClient(create_app(Settings(data, curriculum_dir=old_dir))) as client:
        session = signup(client, "Upgrade")
        enrollment = prepare(client, session, old)
        for lesson in old["lessons"]:
            finish(client, session, solve(client, session, begin(client, session, enrollment, lesson["id"], old), old))
        completed_review = solve(client, session, begin(client, session, enrollment, Catalog.review_id(old, 5), old), old)
        assert finish(client, session, completed_review)["dailyXp"] == 1400
        pending = begin(client, session, enrollment, old["lessons"][0]["id"], old)
        question = pending["questions"][0]
        correct = old["lessons"][0]["questions"][0]["correctOptionId"]
        wrong = next(o["id"] for o in question["options"] if o["id"] != correct)
        pending = answer(client, session, pending, [{"questionId": question["id"],
                         "selectedOptionId": wrong, "submissionId": key()}], step=1)
    # New server process, same persistent database, expanded shipped catalog.
    with TestClient(create_app(Settings(data))) as client:
        resumed = client.get(f"/attempts/{pending['id']}", headers=headers(session))
        assert resumed.status_code == 200
        resumed = resumed.json()
        assert resumed["revision"] == pending["revision"] and resumed["step"] == 1
        assert resumed["answers"][0]["wrongCount"] == 1
        assert finish(client, session, solve(client, session, resumed, current))["xpAwarded"] == 0
        assert finish(client, session, completed_review)["replayed"]
        path = client.get(f"/enrollments/{enrollment['id']}/path", headers=headers(session)).json()
        assert path["enrollment"]["id"] == enrollment["id"]
        assert all(u["state"] == "COMPLETED" for u in path["units"][:6])
        assert path["units"][6]["state"] == "AVAILABLE"
        new = begin(client, session, enrollment, current["lessons"][5]["id"], current)
        result = finish(client, session, solve(client, session, new, current))
        assert result["xpAwarded"] == 200 and result["dailyXp"] == 1600
