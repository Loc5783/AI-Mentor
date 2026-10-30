from aimentor.placement_policy import PlacementPolicy


def course_with_lessons(count=10):
    lessons = []
    for index in range(count):
        lesson_id = f"math-10-lesson-{index + 1}"
        lessons.append({"id": lesson_id, "questions": [
            {"id": f"{lesson_id}-q1"}, {"id": f"{lesson_id}-q2"},
            {"id": f"{lesson_id}-practice"},
        ]})
    return {"subjectId": "math", "grade": 10, "lessons": lessons}


def diagnostic(course):
    questions = [q for lesson in course["lessons"] for q in lesson["questions"][:2]]
    answers = {q["id"]: {"correct": True} for q in questions}
    return questions, answers


def test_skips_lesson_only_after_both_topic_questions_are_correct():
    course = course_with_lessons()
    questions, answers = diagnostic(course)
    answers["math-10-lesson-1-q2"]["correct"] = False

    result = PlacementPolicy.assess(course, questions, answers, "2026-10-30T00:00:00Z")

    assert result["masteredLessonCount"] == 9
    assert "math-10-lesson-1" not in result["skippedUnitIds"]
    assert "math-10-lesson-2" in result["skippedUnitIds"]
    assert "math-10-review-1" not in result["skippedUnitIds"]
    assert "math-10-review-2" in result["skippedUnitIds"]
    assert result["proficiencyBand"] == "STRONG"


def test_skips_review_only_when_all_five_lessons_in_block_are_mastered():
    course = course_with_lessons()
    questions, answers = diagnostic(course)

    result = PlacementPolicy.assess(course, questions, answers, "2026-10-30T00:00:00Z")

    assert result["masteredLessonCount"] == 10
    assert result["skippedReviewCount"] == 2
    assert "math-10-review-1" in result["skippedUnitIds"]
    assert "math-10-review-2" in result["skippedUnitIds"]


def test_proficiency_bands_follow_score_ranges():
    assert PlacementPolicy.proficiency_band(4, 10) == "FOUNDATION"
    assert PlacementPolicy.proficiency_band(5, 10) == "DEVELOPING"
    assert PlacementPolicy.proficiency_band(8, 10) == "PROFICIENT"
    assert PlacementPolicy.proficiency_band(9, 10) == "STRONG"
