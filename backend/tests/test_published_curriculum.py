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
NEW_GRADE_10_SUBJECTS = ("math", "literature", "english", "history", "geography", "economics_law")
ALL_GRADE_12_SUBJECTS = (
    "math", "physics", "chemistry", "biology",
    "literature", "english", "history", "geography", "economics_law"
)


def course(subject):
    return json.loads((PACKS / f"{subject}-10.json").read_text(encoding="utf-8"))


def course12(subject):
    return json.loads((PACKS / f"{subject}-12.json").read_text(encoding="utf-8"))


def solve(client, session, attempt, content):
    solutions = {q["id"]: q["correctOptionId"] for l in content["lessons"] for q in l["questions"]}
    return answer(client, session, attempt, [
        {"questionId": q["id"], "selectedOptionId": solutions[q["id"]], "submissionId": key()}
        for q in attempt["questions"]])


def begin(client, session, enrollment, unit, content):
    return ok(post(client, session, "/attempts", {"enrollmentId": enrollment["id"],
                   "lessonId": unit, "curriculumVersion": content["version"]}))


<<<<<<< HEAD
def prepare(client, session, content, grade=10):
    enrollment = ok(post(client, session, "/enrollments", {"subjectId": content["subjectId"], "grade": grade}))
=======
def prepare(client, session, content):
    enrollment = ok(post(client, session, "/enrollments", {"subjectId": content["subjectId"], "grade": content["grade"]}))
>>>>>>> c5f2011 (feat: add authentic Grade 11 SGK KNTT curriculum, dollar-quoted Supabase migration, and fix backend unit test assertions)
    placement = ok(post(client, session, "/placements", {"enrollmentId": enrollment["id"]}))
    solutions = {q["id"]: q["correctOptionId"] for l in content["lessons"] for q in l["questions"]}
    answers = [{"questionId": question["id"],
                "selectedOptionId": next(option["id"] for option in question["options"]
<<<<<<< HEAD
                                          if option["id"] != solutions[question["id"]]),
=======
                                          if option["id"] != solutions.get(question["id"])),
>>>>>>> c5f2011 (feat: add authentic Grade 11 SGK KNTT curriculum, dollar-quoted Supabase migration, and fix backend unit test assertions)
                "submissionId": key()} for question in placement["questions"]]
    completed = finish(client, session, answer(client, session, placement, answers))
    assert completed["xpAwarded"] == 0 and not completed["streakLit"]
    return enrollment


def test_generation_reproduces_shipped_packs_and_android_fixture():
    before = {p: p.read_bytes() for p in PACKS.glob("*.json")}
    subprocess.run([sys.executable, str(BACKEND / "tools" / "build_intro_curriculum.py")], check=True)
    assert all(p.read_bytes() == value for p, value in before.items())
    assert (BACKEND.parent / "app/src/androidTest/assets/physics-10.json").read_bytes() == (PACKS / "physics-10.json").read_bytes()


def test_supabase_migration_seeds_placement_lessons_reviews_and_practice_quizzes():
    subprocess.run([sys.executable, str(BACKEND / "tools" / "build_grade10_supabase_migration.py")], check=True)
    migration = (BACKEND.parent / "supabase/migrations/202610300001_remaining_grade10_curricula.sql").read_text(encoding="utf-8")
    assert migration.count("insert into public.mentor_course_units(") == 114
    assert migration.count("'PLACEMENT'") == 6
    assert sum(line.startswith("values (") for line in migration.splitlines()) == 114
    assert migration.count("'FIVE_LESSON_REVIEW'") == 18
    assert "insert into public.mentor_library_quizzes" in migration
    assert "on conflict(subject_id,grade,unit_id) do nothing" in migration


def test_adaptive_placement_migration_updates_assessment_and_path_rules():
    subprocess.run([sys.executable, str(BACKEND / "tools" / "build_adaptive_placement_migration.py")], check=True)
    migration = (BACKEND.parent / "supabase/migrations/202610300002_adaptive_placement.sql").read_text(encoding="utf-8")
    assert "q.ordinality<=2" in migration
    assert "question_count=2 and correct_count=2" in migration
    assert "'SKIPPED'" in migration
    assert "skipped_units ? prior.unit_id" in migration
    assert "Bài đầu vào dùng hai câu mỗi bài đã xuất bản" in migration


@pytest.mark.parametrize("subject", NEW_GRADE_10_SUBJECTS)
def test_remaining_grade_ten_subjects_publish_fifteen_lessons_and_three_reviews(subject):
    content = course(subject)
    Catalog.validate(content)
    assert content["subjectId"] == subject
    assert content["grade"] == 10
    assert content["coverage"] == "PARTIAL_GRADE_10"
    assert len(content["lessons"]) == 15

    catalog = Catalog(PACKS)
    question_ids = [q["id"] for lesson in content["lessons"] for q in lesson["questions"]]
    assert len(question_ids) == 45 and len(set(question_ids)) == 45
    for end in (5, 10, 15):
        _, _, questions, sources = catalog.unit(content, catalog.review_id(content, end))
        assert len(questions) == 10
        assert sources == [lesson["id"] for lesson in content["lessons"][end - 5:end]]
        assert len({q["id"] for q in questions}) == 10
    _, _, placement_questions, _ = catalog.unit(content, "placement")
    assert len(placement_questions) == 30
    assert {q["id"] for q in placement_questions} == {
        q["id"] for lesson in content["lessons"] for q in lesson["questions"][:2]}


@pytest.mark.parametrize("subject", ORIGINAL_DIGESTS)
def test_packs_preserve_old_questions_and_cover_four_disjoint_blocks(subject):
    content = course(subject)
    Catalog.validate(content)
    assert len(content["lessons"]) == 20
    _, _, placement_questions, _ = Catalog(PACKS).unit(content, "placement")
    assert len(placement_questions) == 40
    assert {q["id"] for q in placement_questions} == {
        q["id"] for lesson in content["lessons"] for q in lesson["questions"][:2]}
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
    assert (subject, 12) not in catalog.courses


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


@pytest.mark.parametrize("subject", ALL_GRADE_12_SUBJECTS)
def test_all_nine_grade_twelve_packs_valid_catalog_and_reviews(subject):
    content = course12(subject)
    Catalog.validate(content)
    assert content["subjectId"] == subject
    assert content["grade"] == 12
    assert content["coverage"] == "PARTIAL_GRADE_12"
    assert content["published"] is True

    expected_lesson_count = 20 if subject in ("physics", "chemistry", "biology") else 15
    expected_question_count = expected_lesson_count * 3
    assert len(content["lessons"]) == expected_lesson_count

    catalog = Catalog(PACKS)
    question_ids = [q["id"] for lesson in content["lessons"] for q in lesson["questions"]]
    assert len(question_ids) == expected_question_count
    assert len(set(question_ids)) == expected_question_count

    for lesson in content["lessons"]:
        diffs = {q["difficulty"] for q in lesson["questions"]}
        assert diffs == {"EASY", "MEDIUM", "APPLIED"}
        for q in lesson["questions"]:
            texts = [o["text"].strip().casefold() for o in q["options"]]
            assert len(texts) == len(set(texts)), q["id"]
            assert len(texts) == 3

    for end in range(5, expected_lesson_count + 1, 5):
        _, _, questions, sources = catalog.unit(content, catalog.review_id(content, end))
        assert len(questions) == 10
        assert sources == [lesson["id"] for lesson in content["lessons"][end - 5:end]]
        assert len({q["id"] for q in questions}) == 10

    _, _, placement_questions, _ = catalog.unit(content, "placement")
    assert len(placement_questions) == expected_lesson_count * 2
    assert {q["id"] for q in placement_questions} == {
        q["id"] for lesson in content["lessons"] for q in lesson["questions"][:2]}


@pytest.mark.parametrize("subject", ALL_GRADE_12_SUBJECTS)
def test_grade_twelve_behavioral_flow_enrollment_placement_review_and_isolation(tmp_path, subject):
    content = course12(subject)
    app = create_app(Settings(tmp_path))
    with TestClient(app) as client:
        student_a = signup(client, f"Grade12StudentA_{subject}")
        student_b = signup(client, f"Grade12StudentB_{subject}")

        # 1. Enrollment in Grade 12
        enrollment_a = ok(post(client, student_a, "/enrollments", {"subjectId": subject, "grade": 12}))
        assert enrollment_a["subjectId"] == subject
        assert enrollment_a["grade"] == 12

        # 2. User isolation: Student B cannot access Student A's enrollment path
        assert client.get(f"/enrollments/{enrollment_a['id']}/path", headers=headers(student_b)).status_code == 404
        path_a = client.get(f"/enrollments/{enrollment_a['id']}/path", headers=headers(student_a)).json()
        assert all(u["state"] == "LOCKED" for u in path_a["units"])

        # 3. Lesson attempt blocked before placement
        blocked = post(client, student_a, "/attempts", {
            "enrollmentId": enrollment_a["id"],
            "lessonId": content["lessons"][0]["id"],
            "curriculumVersion": content["version"]
        })
        assert blocked.status_code == 409 and blocked.json()["code"] == "PLACEMENT_REQUIRED"

        # 4. Placement flow: questions hide answers/explanations
        placement = ok(post(client, student_a, "/placements", {"enrollmentId": enrollment_a["id"]}))
        assert all("correctOptionId" not in q and "explanation" not in q for q in placement["questions"])

        # User isolation: Student B cannot view Student A's placement attempt
        assert client.get(f"/attempts/{placement['id']}", headers=headers(student_b)).status_code == 404

        # Complete placement with wrong answers (guaranteed 0 XP, no streak, lesson 1 becomes available)
        solutions = {q["id"]: q["correctOptionId"] for l in content["lessons"] for q in l["questions"]}
        wrong_answers = [{"questionId": q["id"],
                          "selectedOptionId": next(o["id"] for o in q["options"] if o["id"] != solutions[q["id"]]),
                          "submissionId": key()} for q in placement["questions"]]
        completed_placement = finish(client, student_a, answer(client, student_a, placement, wrong_answers))
        assert completed_placement["xpAwarded"] == 0 and not completed_placement["streakLit"]

        path_after = client.get(f"/enrollments/{enrollment_a['id']}/path", headers=headers(student_a)).json()
        assert path_after["units"][0]["state"] == "AVAILABLE"
        assert path_after["units"][1]["state"] == "LOCKED"

        # 5. Complete Lessons 1 to 5 with correct answers
        for idx in range(5):
            lid = content["lessons"][idx]["id"]
            att = begin(client, student_a, enrollment_a, lid, content)
            # User isolation: Student B cannot view Student A's attempt
            assert client.get(f"/attempts/{att['id']}", headers=headers(student_b)).status_code == 404
            res = finish(client, student_a, solve(client, student_a, att, content))
            assert res["xpAwarded"] == 200
            assert res["dailyXp"] == (idx + 1) * 200

        # 6. Attempting Lesson 6 before Review 1 must be blocked
        blocked_six = post(client, student_a, "/attempts", {
            "enrollmentId": enrollment_a["id"],
            "lessonId": content["lessons"][5]["id"],
            "curriculumVersion": content["version"]
        })
        assert blocked_six.status_code == 409 and blocked_six.json()["code"] == "PREVIOUS_REVIEW_REQUIRED"

        # 7. Complete 5-Lesson Review 1
        review_id = Catalog.review_id(content, 5)
        review_att = begin(client, student_a, enrollment_a, review_id, content)
        assert review_att["sourceLessonIds"] == [l["id"] for l in content["lessons"][:5]]
        done_review = finish(client, student_a, solve(client, student_a, review_att, content))
        assert done_review["xpAwarded"] == 400
        assert done_review["dailyXp"] == 1400

        # Lesson 6 is now AVAILABLE
        path_after_rev = client.get(f"/enrollments/{enrollment_a['id']}/path", headers=headers(student_a)).json()
        assert next(u for u in path_after_rev["units"] if u["id"] == content["lessons"][5]["id"])["state"] == "AVAILABLE"

        # 8. Grade Isolation: Enrolling Student A in Grade 10 of same subject has isolated progression
        enrollment_g10 = ok(post(client, student_a, "/enrollments", {"subjectId": subject, "grade": 10}))
        assert enrollment_g10["id"] != enrollment_a["id"]
        assert enrollment_g10["grade"] == 10
        path_g10 = client.get(f"/enrollments/{enrollment_g10['id']}/path", headers=headers(student_a)).json()
        assert all(u["state"] == "LOCKED" for u in path_g10["units"])
