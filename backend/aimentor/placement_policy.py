"""Pure entry-assessment policy for mapping demonstrated mastery to a course path."""


class PlacementPolicy:
    BANDS = ((.5, "FOUNDATION"), (.75, "DEVELOPING"), (.9, "PROFICIENT"))

    @classmethod
    def proficiency_band(cls, correct, total):
        ratio = correct / max(1, total)
        for threshold, band in cls.BANDS:
            if ratio < threshold:
                return band
        return "STRONG"

    @classmethod
    def assess(cls, course, questions, answers, completed_at):
        score = sum(int(answers[q["id"]]["correct"]) for q in questions)
        mastered_lessons = []
        lessons = course["lessons"]
        for lesson in lessons:
            diagnostic = lesson["questions"][:2]
            if len(diagnostic) == 2 and all(answers.get(q["id"], {}).get("correct")
                                            for q in diagnostic):
                mastered_lessons.append(lesson["id"])

        skipped_unit_ids = list(mastered_lessons)
        skipped_review_count = 0
        for end in range(5, len(lessons) + 1, 5):
            block = lessons[end - 5:end]
            if all(lesson["id"] in mastered_lessons for lesson in block):
                skipped_unit_ids.append(f"{course['subjectId']}-{course['grade']}-review-{end // 5}")
                skipped_review_count += 1

        return {
            "correct": score,
            "total": len(questions),
            "scorePercent": round(score / max(1, len(questions)) * 100),
            "proficiencyBand": cls.proficiency_band(score, len(questions)),
            "recommendation": "PERSONALIZED_PATH",
            "scope": "PUBLISHED_LESSONS",
            "masteredLessonIds": mastered_lessons,
            "masteredLessonCount": len(mastered_lessons),
            "totalLessonCount": len(lessons),
            "skippedUnitIds": skipped_unit_ids,
            "skippedLessonCount": len(mastered_lessons),
            "skippedReviewCount": skipped_review_count,
            "completedAt": completed_at,
        }
