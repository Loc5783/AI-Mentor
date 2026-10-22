"""One consistent, read-only summary of server-owned learning and rewards."""
from datetime import datetime, timedelta
import json
import time
from .common import ACTIVE_SUBJECTS, iso
from .learning_stats import ZONE, reward_summary


class LearningOverview:
    def __init__(self, database, catalog, clock=time.time):
        self.database, self.catalog, self.clock = database, catalog, clock

    def load(self, owner, grade):
        now = self.clock()
        today = datetime.fromtimestamp(now, ZONE).date()
        with self.database.read() as db:
            # All numbers describe the same SQLite snapshot, including during concurrent completion.
            db.execute("BEGIN")
            awards = db.execute("SELECT subject_id,grade,xp,day FROM awards WHERE user_id=?", (owner,)).fetchall()
            days = {r[0] for r in db.execute("SELECT day FROM learning_days WHERE user_id=?", (owner,))}
            enrolled = db.execute("SELECT * FROM enrollments WHERE user_id=? AND grade=?", (owner, grade)).fetchall()
            attempts = db.execute("""SELECT a.*,e.subject_id,e.grade,c.completed_at
                FROM attempts a JOIN enrollments e ON a.enrollment_id=e.id
                JOIN completions c ON c.attempt_id=a.id
                WHERE a.user_id=? AND e.user_id=? AND e.grade=? AND a.kind<>'PLACEMENT'
                ORDER BY c.completed_at,a.id""", (owner, owner, grade)).fetchall()
            cursor = db.execute("SELECT COALESCE(MAX(seq),0) FROM events WHERE owner=? OR owner IS NULL", (owner,)).fetchone()[0]
        unique = {}
        for attempt in attempts:
            unique.setdefault((attempt["subject_id"], attempt["unit_id"]), attempt)
        first_answers = [a for row in unique.values() if row["kind"] == "LESSON" for a in json.loads(row["answers"])]
        rewards=reward_summary(awards,days,now)
        subjects = []
        for subject in ACTIVE_SUBJECTS:
            course = self.catalog.courses.get((subject, grade))
            enrollment = next((e for e in enrolled if e["subject_id"] == subject), None)
            lessons = course["lessons"] if course and enrollment else []
            complete = {r["unit_id"] for r in unique.values() if r["subject_id"] == subject and r["kind"] == "LESSON"}
            subjects.append({"subjectId": subject, "grade": grade, "enrolled": enrollment is not None,
                             "curriculumAvailable": course is not None,
                             "totalXp": sum(a["xp"] for a in awards if a["subject_id"] == subject),
                             "completedLessons": sum(l["id"] in complete for l in lessons),
                             "totalLessons": len(lessons)})
        week = []
        for offset in range(6, -1, -1):
            date = (today - timedelta(days=offset)).isoformat()
            week.append({"day": date, "xp": sum(a["xp"] for a in awards if a["day"] == date), "streakLit": date in days})

        total_xp = rewards.get("totalXp", 0)
        rank_tier = "DIAMOND" if total_xp>=10000 else "PLATINUM" if total_xp>=5000 else "GOLD" if total_xp>=2000 else "SILVER" if total_xp>=500 else "BRONZE"

        return {"profileId": str(owner), "grade": grade, "learningDay": today.isoformat(),
                "timezone": "Asia/Ho_Chi_Minh", "asOf": iso(now), "eventCursor": str(cursor),
                **rewards, "rankTier": rank_tier, "subjects": subjects, "week": week,
                "completedLessons": sum(s["completedLessons"] for s in subjects),
                "totalLessons": sum(s["totalLessons"] for s in subjects),
                "completedReviews": sum(r["kind"] == "FIVE_LESSON_REVIEW" for r in unique.values()),
                "practice": {"firstTryCorrect": sum(bool(a.get("firstTryCorrect", False)) for a in first_answers),
                             "questionCount": len(first_answers),
                             "assisted": sum(bool(a.get("assisted", False)) for a in first_answers),
                             "scope": "FIRST_COMPLETED_LESSON_ATTEMPTS_IN_GRADE"}}
