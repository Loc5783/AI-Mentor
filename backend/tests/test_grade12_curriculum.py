"""Offline regression tests validating Batch 1 Grade 12 curricula (Math, Physics, Chemistry).

Validates:
- Catalog loading and structural compliance (15/20/20 lessons, 5-lesson blocks, difficulties, unique IDs).
- Real API progression for Grade 12: enrollment, placement, lesson attempts, 5-lesson review, and gating.
- Reward and identity isolation between different student accounts.
- Dual coexistence ensuring Grade 10 courses remain functional.
"""
import json
from pathlib import Path
import pytest
from fastapi.testclient import TestClient

from aimentor.config import Settings
from aimentor.learning import Catalog
from aimentor.main import create_app
from test_api import headers, key, signup
from test_learning import answer, finish, ok, post


PACKS = Path(__file__).resolve().parents[1] / "curriculum"


def course(subject, grade=12):
    return json.loads((PACKS / f"{subject}-{grade}.json").read_text(encoding="utf-8"))


def solve(client, session, attempt, content):
    solutions = {q["id"]: q["correctOptionId"] for l in content["lessons"] for q in l["questions"]}
    return answer(client, session, attempt, [
        {"questionId": q["id"], "selectedOptionId": solutions[q["id"]], "submissionId": key()}
        for q in attempt["questions"]
    ])


def begin(client, session, enrollment, unit, content):
    return ok(post(client, session, "/attempts", {
        "enrollmentId": enrollment["id"],
        "lessonId": unit,
        "curriculumVersion": content["version"]
    }))


def prepare_grade12(client, session, content, skip_first_n_lessons=0):
    enrollment = ok(post(client, session, "/enrollments", {"subjectId": content["subjectId"], "grade": 12}))
    placement = ok(post(client, session, "/placements", {"enrollmentId": enrollment["id"]}))
    
    # Map questions to lessons
    solutions = {q["id"]: q["correctOptionId"] for l in content["lessons"] for q in l["questions"]}
    mastered_qids = set()
    for lesson in content["lessons"][:skip_first_n_lessons]:
        for q in lesson["questions"][:2]:
            mastered_qids.add(q["id"])
            
    answers = []
    for question in placement["questions"]:
        qid = question["id"]
        if qid in mastered_qids:
            selected = solutions[qid]
        else:
            selected = next(option["id"] for option in question["options"]
                            if option["id"] != solutions[qid])
        answers.append({"questionId": qid, "selectedOptionId": selected, "submissionId": key()})
        
    completed = finish(client, session, answer(client, session, placement, answers))
    assert completed["xpAwarded"] == 0 and not completed["streakLit"]
    return enrollment, completed


@pytest.mark.parametrize("subject,expected_lessons", [
    ("math", 15),
    ("physics", 20),
    ("chemistry", 20),
    ("biology", 20),
    ("literature", 15),
    ("english", 15)
])
def test_grade12_catalog_structure_and_functional_contract(subject, expected_lessons):
    content = course(subject, 12)
    Catalog.validate(content)
    assert content["subjectId"] == subject
    assert content["grade"] == 12
    assert content["version"] == "intro-2026.1"
    assert content["published"] is True
    assert content["coverage"] == "PARTIAL_GRADE_12"
    assert len(content["lessons"]) == expected_lessons

    catalog = Catalog(PACKS)
    assert (subject, 12) in catalog.courses

    question_ids = []
    for lesson in content["lessons"]:
        assert lesson["id"].startswith(f"{subject}-12-")
        assert len(lesson["guidance"]) >= 1 and bool(lesson["guidance"][0].strip())
        assert len(lesson["examples"]) >= 1 and bool(lesson["examples"][0].strip())
        assert len(lesson["questions"]) >= 3
        
        difficulties = {q["difficulty"] for q in lesson["questions"]}
        assert difficulties == {"EASY", "MEDIUM", "APPLIED"}
        
        for q in lesson["questions"]:
            assert q["id"].startswith(f"{lesson['id']}-")
            question_ids.append(q["id"])
            assert q["correctOptionId"] in [o["id"] for o in q["options"]]
            assert len(q["explanation"].strip()) > 0
            texts = [o["text"].strip().casefold() for o in q["options"]]
            assert len(texts) == len(set(texts)), f"Duplicate option in {q['id']}"

    assert len(question_ids) == expected_lessons * 3
    assert len(set(question_ids)) == len(question_ids)

    # Validate 5-lesson reviews
    for end in range(5, expected_lessons + 1, 5):
        kind, idx, questions, sources = catalog.unit(content, catalog.review_id(content, end))
        assert kind == "FIVE_LESSON_REVIEW"
        assert len(questions) == 10
        block = content["lessons"][end - 5:end]
        assert sources == [l["id"] for l in block]
        assert {q["id"] for q in questions} == {q["id"] for l in block for q in l["questions"][:2]}

    # Validate placement diagnostic
    kind, idx, placement_questions, sources = catalog.unit(content, "placement")
    assert kind == "PLACEMENT"
    assert len(placement_questions) == expected_lessons * 2
    assert {q["id"] for q in placement_questions} == {
        q["id"] for lesson in content["lessons"] for q in lesson["questions"][:2]
    }


@pytest.mark.parametrize("subject,expected_lessons", [
    ("math", 15),
    ("physics", 20),
    ("chemistry", 20),
    ("biology", 20),
    ("literature", 15),
    ("english", 15)
])
def test_grade12_real_api_progression_enrollment_placement_and_review(tmp_path, subject, expected_lessons):
    content = course(subject, 12)
    app = create_app(Settings(tmp_path))
    with TestClient(app) as client:
        session = signup(client, f"Learner_{subject}_12")
        
        # Verify /subjects exposes grade 12 for this subject
        subj_resp = client.get("/subjects", headers=headers(session)).json()
        item = next(s for s in subj_resp["items"] if s["id"] == subject)
        assert 12 in item["availableGrades"]
        
        # Enroll in Grade 12
        enrollment = ok(post(client, session, "/enrollments", {"subjectId": subject, "grade": 12}))
        assert enrollment["grade"] == 12
        assert enrollment["placementStatus"] == "NOT_STARTED"

        # Attempting to start a lesson before placement is rejected
        premature = post(client, session, "/attempts", {
            "enrollmentId": enrollment["id"],
            "lessonId": content["lessons"][0]["id"],
            "curriculumVersion": content["version"]
        })
        assert premature.status_code == 409
        assert premature.json()["code"] == "PLACEMENT_REQUIRED"

        # Run placement skipping the first 2 lessons
        placement = ok(post(client, session, "/placements", {"enrollmentId": enrollment["id"]}))
        assert len(placement["questions"]) == expected_lessons * 2
        
        solutions = {q["id"]: q["correctOptionId"] for l in content["lessons"] for q in l["questions"]}
        answers = []
        for q in placement["questions"]:
            qid = q["id"]
            # Answer correctly for lesson 1 and lesson 2 only
            if qid in [item["id"] for item in content["lessons"][0]["questions"][:2]] or \
               qid in [item["id"] for item in content["lessons"][1]["questions"][:2]]:
                sel = solutions[qid]
            else:
                sel = next(o["id"] for o in q["options"] if o["id"] != solutions[qid])
            answers.append({"questionId": qid, "selectedOptionId": sel, "submissionId": key()})

        placement_ans = answer(client, session, placement, answers)
        placement_done = finish(client, session, placement_ans)
        assessment = placement_done["assessment"]
        assert assessment["masteredLessonCount"] == 2
        assert content["lessons"][0]["id"] in assessment["skippedUnitIds"]
        assert content["lessons"][1]["id"] in assessment["skippedUnitIds"]
        assert content["lessons"][2]["id"] not in assessment["skippedUnitIds"]
        assert assessment["skippedReviewCount"] == 0

        # Check Path reflects skipped and available lessons
        path = client.get(f"/enrollments/{enrollment['id']}/path", headers=headers(session)).json()
        assert path["units"][0]["state"] == "SKIPPED"
        assert path["units"][1]["state"] == "SKIPPED"
        assert path["units"][2]["state"] == "AVAILABLE"
        assert path["units"][3]["state"] == "LOCKED"

        # Cannot skip directly to lesson 4 without finishing lesson 3
        blocked = post(client, session, "/attempts", {
            "enrollmentId": enrollment["id"],
            "lessonId": content["lessons"][3]["id"],
            "curriculumVersion": content["version"]
        })
        assert blocked.status_code == 409
        assert blocked.json()["code"] == "PREVIOUS_LESSON_REQUIRED"

        # Start and complete lesson 3
        lesson3_attempt = begin(client, session, enrollment, content["lessons"][2]["id"], content)
        assert len(lesson3_attempt["questions"]) == 3
        assert all("correctOptionId" not in q for q in lesson3_attempt["questions"])
        
        solved3 = solve(client, session, lesson3_attempt, content)
        res3 = finish(client, session, solved3)
        assert res3["xpAwarded"] == 200
        assert res3["dailyXp"] == 200
        assert res3["streakDays"] == 1
        assert res3["streakLit"] is True

        # Replaying the exact same attempt returns replayed=True
        replayed = finish(client, session, solved3)
        assert replayed["replayed"] is True

        # Retaking the completed lesson in a new attempt awards 0 XP
        retake = begin(client, session, enrollment, content["lessons"][2]["id"], content)
        res_retake = finish(client, session, solve(client, session, retake, content))
        assert res_retake["xpAwarded"] == 0
        assert res_retake["dailyXp"] == 200


def test_grade12_five_lesson_review_and_block_gating(tmp_path):
    content = course("math", 12)
    app = create_app(Settings(tmp_path))
    with TestClient(app) as client:
        session = signup(client, "MathStudent12")
        enrollment, _ = prepare_grade12(client, session, content, skip_first_n_lessons=0)
        
        # Complete lessons 1 to 5 sequentially
        total_xp = 0
        for i in range(5):
            attempt = begin(client, session, enrollment, content["lessons"][i]["id"], content)
            res = finish(client, session, solve(client, session, attempt, content))
            total_xp += 200
            assert res["xpAwarded"] == 200
            assert res["dailyXp"] == total_xp

        # Attempting to start lesson 6 without review 1 must be blocked
        blocked = post(client, session, "/attempts", {
            "enrollmentId": enrollment["id"],
            "lessonId": content["lessons"][5]["id"],
            "curriculumVersion": content["version"]
        })
        assert blocked.status_code == 409
        assert blocked.json()["code"] == "PREVIOUS_REVIEW_REQUIRED"

        # Start and complete 5-lesson review 1
        rev1_id = Catalog.review_id(content, 5)
        review_attempt = begin(client, session, enrollment, rev1_id, content)
        assert len(review_attempt["questions"]) == 10
        assert review_attempt["kind"] == "FIVE_LESSON_REVIEW"
        
        rev_solved = solve(client, session, review_attempt, content)
        rev_res = finish(client, session, rev_solved)
        total_xp += 400
        assert rev_res["xpAwarded"] == 400
        assert rev_res["dailyXp"] == total_xp

        # Now lesson 6 is unblocked and can be started
        lesson6_attempt = begin(client, session, enrollment, content["lessons"][5]["id"], content)
        assert lesson6_attempt["lessonId"] == content["lessons"][5]["id"]


def test_grade12_reward_and_identity_isolation(tmp_path):
    math12 = course("math", 12)
    app = create_app(Settings(tmp_path))
    with TestClient(app) as client:
        user_a = signup(client, "StudentA_12")
        user_b = signup(client, "StudentB_12")

        # User A enrolls and completes lesson 1
        enroll_a, _ = prepare_grade12(client, user_a, math12, skip_first_n_lessons=0)
        att_a = begin(client, user_a, enroll_a, math12["lessons"][0]["id"], math12)
        res_a = finish(client, user_a, solve(client, user_a, att_a, math12))
        assert res_a["dailyXp"] == 200

        # User B dashboard and enrollment list are isolated from User A
        b_enrolls = client.get("/enrollments", headers=headers(user_b)).json()
        assert len(b_enrolls["items"]) == 0

        b_dash = client.get("/me/dashboard", params={"grade": 12}, headers=headers(user_b)).json()
        assert b_dash["totalXp"] == 0
        assert b_dash["dailyXp"] == 0
        assert b_dash["completedLessons"] == 0

        # User B cannot access or modify User A's attempt
        foreign_get = client.get(f"/attempts/{att_a['id']}", headers=headers(user_b))
        assert foreign_get.status_code == 404

        foreign_patch = client.patch(f"/attempts/{att_a['id']}", headers=headers(user_b, **{"If-Match": "1"}), json={
            "step": 1, "answers": []
        })
        assert foreign_patch.status_code == 404

        foreign_comp = client.post("/completions", headers=headers(user_b, **{"Idempotency-Key": key()}), json={"attemptId": att_a["id"]})
        assert foreign_comp.status_code == 404


def test_grade10_and_grade12_coexist_without_interference(tmp_path):
    math10 = course("math", 10)
    math12 = course("math", 12)
    app = create_app(Settings(tmp_path))
    with TestClient(app) as client:
        session = signup(client, "DualGradeStudent")

        # Enroll in both Grade 10 and Grade 12 Math
        e10 = ok(post(client, session, "/enrollments", {"subjectId": "math", "grade": 10}))
        e12 = ok(post(client, session, "/enrollments", {"subjectId": "math", "grade": 12}))
        assert e10["id"] != e12["id"]
        assert e10["grade"] == 10
        assert e12["grade"] == 12

        # Complete placement on both
        prepare_grade10 = ok(post(client, session, "/placements", {"enrollmentId": e10["id"]}))
        prepare_grade12_att = ok(post(client, session, "/placements", {"enrollmentId": e12["id"]}))

        ans10 = [{"questionId": q["id"], "selectedOptionId": q["options"][0]["id"], "submissionId": key()}
                 for q in prepare_grade10["questions"]]
        ans12 = [{"questionId": q["id"], "selectedOptionId": q["options"][0]["id"], "submissionId": key()}
                 for q in prepare_grade12_att["questions"]]

        finish(client, session, answer(client, session, prepare_grade10, ans10))
        finish(client, session, answer(client, session, prepare_grade12_att, ans12))

        # Check paths are distinct
        p10 = client.get(f"/enrollments/{e10['id']}/path", headers=headers(session)).json()
        p12 = client.get(f"/enrollments/{e12['id']}/path", headers=headers(session)).json()
        assert p10["enrollment"]["grade"] == 10
        assert p12["enrollment"]["grade"] == 12
        assert p10["units"][0]["id"].startswith("math-10-")
        assert p12["units"][0]["id"].startswith("math-12-")
