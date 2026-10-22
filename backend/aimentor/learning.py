"""Server-owned learning progression. Transport and screen state do not award rewards."""
from copy import deepcopy
from datetime import datetime
import hashlib
import json
from pathlib import Path
import time

from .common import ACTIVE_SUBJECTS, error, iso, idem, check_revision, owned_resource, resource
from .database import Database, dump, identifier
from .learning_stats import ZONE, reward_summary



class Catalog:
    """Versioned, original content packs; incomplete/unpublished packs never become courses."""

    def __init__(self, directory):
        self.courses = {}
        for path in sorted(Path(directory).glob("*.json")):
            course = json.loads(path.read_text(encoding="utf-8"))
            if not course.get("published", False):
                continue
            self.validate(course)
            key = course["subjectId"], course["grade"]
            if key in self.courses:
                raise ValueError("Duplicate subject/grade course")
            self.courses[key] = course

    @staticmethod
    def validate(course):
        if course["subjectId"] not in ACTIVE_SUBJECTS or course["grade"] not in (10, 11, 12):
            raise ValueError("Only supported active subjects in grades 10–12 can publish")
        if not course.get("version") or not course.get("title") or not course.get("sourceNote"):
            raise ValueError("A course needs a version, title and source note")
        lessons = course["lessons"]
        if not lessons or len(lessons) % 5:
            raise ValueError("Published courses need complete five-lesson blocks")
        units, questions = set(), set()
        for lesson in lessons:
            if not lesson["id"] or lesson["id"] in units:
                raise ValueError("Duplicate or empty lesson ID")
            units.add(lesson["id"])
            if not lesson.get("title") or not lesson.get("guidance") or not lesson.get("examples"):
                raise ValueError("A lesson needs a title, guidance and examples")
            if len(lesson["questions"]) < 3:
                raise ValueError("A lesson needs easy, medium and applied practice")
            if {q.get("difficulty") for q in lesson["questions"]} != {"EASY", "MEDIUM", "APPLIED"}:
                raise ValueError("Each lesson must cover the three difficulty stages")
            for q in lesson["questions"]:
                options = [x["id"] for x in q["options"]]
                if (not q["id"] or q["id"] in questions or len(options) < 3
                        or len(options) != len(set(options)) or any(not x for x in options)
                        or q["correctOptionId"] not in options or not q.get("explanation")
                        or not q.get("prompt") or any(not x.get("text") for x in q["options"])):
                    raise ValueError("Invalid question identity, options or explanation")
                questions.add(q["id"])
        for end in range(5, len(lessons) + 1, 5):
            if Catalog.review_id(course, end) in units:
                raise ValueError("Lesson ID collides with review ID")

    @staticmethod
    def review_id(course, end):
        return f"{course['subjectId']}-{course['grade']}-review-{end // 5}"

    def course(self, subject, grade):
        result = self.courses.get((subject, grade))
        if not result:
            error("CURRICULUM_NOT_PUBLISHED", 409)
        return result

    def unit(self, course, unit_id):
        if unit_id == "placement":
            # Ten questions sampling the first five lessons, with no rewards or auto-unlocks.
            return "PLACEMENT", -1, [q for l in course["lessons"][:5] for q in l["questions"][:2]], []
        for index, lesson in enumerate(course["lessons"]):
            if lesson["id"] == unit_id:
                return "LESSON", index, lesson["questions"], [unit_id]
        for end in range(5, len(course["lessons"]) + 1, 5):
            if unit_id == self.review_id(course, end):
                block = course["lessons"][end - 5:end]
                per = 3 if course.get("extendedReviews", False) else 2
                return ("FIVE_LESSON_REVIEW", end - 1,
                        [q for lesson in block for q in lesson["questions"][:per]],
                        [x["id"] for x in block])
        error("LESSON_NOT_FOUND", 404)


class LearningService:
    def __init__(self, db, catalog, clock=time.time):
        self.db, self.catalog, self.clock = db, catalog, clock

    @staticmethod
    def enrollment(db, owner, item):
        row = db.execute("SELECT * FROM enrollments WHERE id=? AND user_id=?", (item, owner)).fetchone()
        if not row:
            error("ENROLLMENT_NOT_FOUND", 404)
        return row

    @staticmethod
    def owned_attempt(db, owner, item):
        row = db.execute("SELECT * FROM attempts WHERE id=? AND user_id=?", (item, owner)).fetchone()
        if not row:
            error("ATTEMPT_NOT_FOUND", 404)
        return row

    @staticmethod
    def enrollment_dto(row):
        return {"id": row["id"], "subjectId": row["subject_id"], "grade": row["grade"],
                "curriculumVersion": row["curriculum_version"],
                "placementStatus": "COMPLETED" if row["placement"] else "NOT_STARTED",
                "placement": json.loads(row["placement"]) if row["placement"] else None,
                "revision": row["revision"], "updatedAt": iso(row["updated_at"])}

    def list_enrollments(self, owner):
        with self.db.read() as db:
            return {"items": [self.enrollment_dto(r) for r in db.execute(
                "SELECT * FROM enrollments WHERE user_id=? ORDER BY grade,subject_id", (owner,))]}

    def enroll(self, owner, body, key):
        course = self.catalog.course(body["subjectId"], body["grade"])
        with self.db.transaction() as db:
            def action():
                row = db.execute("SELECT * FROM enrollments WHERE user_id=? AND subject_id=? AND grade=?",
                                 (owner, body["subjectId"], body["grade"])).fetchone()
                if not row:
                    item = identifier()
                    db.execute("INSERT INTO enrollments(id,user_id,subject_id,grade,curriculum_version,updated_at) VALUES(?,?,?,?,?,?)",
                               (item, owner, body["subjectId"], body["grade"], course["version"], self.clock()))
                    row = self.enrollment(db, owner, item)
                    Database.event(db, owner, "ENROLLMENT", item)
                return self.enrollment_dto(row)
            return idem(db, owner, "enroll", key, body, action)

    @staticmethod
    def completed_units(db, owner, enrollment):
        return {r[0] for r in db.execute(
            "SELECT unit_id FROM attempts WHERE user_id=? AND enrollment_id=? AND state='COMPLETED' AND kind<>'PLACEMENT'",
            (owner, enrollment))}

    def path(self, owner, item):
        with self.db.read() as db:
            enrollment = self.enrollment(db, owner, item)
            course = self.catalog.course(enrollment["subject_id"], enrollment["grade"])
            done = self.completed_units(db, owner, item)
            pending = db.execute("SELECT id,unit_id FROM attempts WHERE user_id=? AND enrollment_id=? AND state='IN_PROGRESS' ORDER BY updated_at DESC",
                                 (owner, item)).fetchall()
        units, required = [], []
        ready = bool(enrollment["placement"])
        for index, lesson in enumerate(course["lessons"]):
            unit = {"id": lesson["id"], "title": lesson["title"], "kind": "LESSON", "xp": 200}
            unit["state"] = "COMPLETED" if unit["id"] in done else "AVAILABLE" if ready and all(x in done for x in required) else "LOCKED"
            units.append(unit)
            required.append(unit["id"])
            if (index + 1) % 5 == 0:
                review_id = self.catalog.review_id(course, index + 1)
                units.append({"id": review_id, "title": f"Ôn tập {(index + 1) // 5}",
                              "kind": "FIVE_LESSON_REVIEW", "xp": 400,
                              "questionCount": 15 if course.get("extendedReviews", False) else 10,
                              "sourceLessonIds": required[-5:],
                              "state": "COMPLETED" if review_id in done else "AVAILABLE" if ready and all(x in done for x in required) else "LOCKED"})
                # Subsequent lessons require the intervening review, not only the five lessons.
                required.append(review_id)
        return {"enrollment": self.enrollment_dto(enrollment), "title": course["title"],
                "curriculumVersion": course["version"], "sourceNote": course["sourceNote"],
                "units": units, "pendingAttempts": [dict(x) for x in pending]}

    def attempt_dto(self, db, owner, row):
        enrollment = self.enrollment(db, owner, row["enrollment_id"])
        course = self.catalog.course(enrollment["subject_id"], enrollment["grade"])
        if course["version"] != row["curriculum_version"]:
            error("CURRICULUM_VERSION_CONFLICT", 409)
        _, _, questions, sources = self.catalog.unit(course, row["unit_id"])
        answers = json.loads(row["answers"])
        by_id = {x["questionId"]: x for x in answers}
        visible = []
        for q in questions:
            value = {k: deepcopy(v) for k, v in q.items() if k not in ("correctOptionId", "explanation")}
            # Keep answer order stable across reloads, but vary it for each attempt.
            # Grading continues to use option IDs, independent of display position.
            value["options"].sort(key=lambda option: hashlib.sha256(
                f"{row['id']}:{q['id']}:{option['id']}".encode()).digest())
            answer = by_id.get(q["id"], {})
            if row["state"] == "COMPLETED" or (row["kind"] != "PLACEMENT" and answer.get("wrongCount", 0) >= 2):
                value.update(explanation=q["explanation"], correctOptionId=q["correctOptionId"])
            visible.append(value)
        lesson = next((x for x in course["lessons"] if x["id"] == row["unit_id"]), None)
        return {"id": row["id"], "enrollmentId": row["enrollment_id"], "lessonId": row["unit_id"],
                "rewardUnitId": row["unit_id"], "curriculumVersion": row["curriculum_version"],
                "kind": row["kind"], "step": row["step"], "answers": answers,
                "questions": visible, "sourceLessonIds": sources,
                "guidance": lesson["guidance"] if lesson else [], "examples": lesson["examples"] if lesson else [],
                "state": row["state"], "revision": row["revision"], "updatedAt": iso(row["updated_at"])}

    def get_attempt(self, owner, item):
        with self.db.read() as db:
            return self.attempt_dto(db, owner, self.owned_attempt(db, owner, item))

    def start(self, owner, body, key, placement=False):
        with self.db.transaction() as db:
            def action():
                enrollment = self.enrollment(db, owner, body["enrollmentId"])
                course = self.catalog.course(enrollment["subject_id"], enrollment["grade"])
                unit_id = "placement" if placement else body["lessonId"]
                if not placement and (unit_id == "placement" or course["version"] != body["curriculumVersion"]):
                    error("CURRICULUM_VERSION_CONFLICT", 409)
                kind, index, _, _ = self.catalog.unit(course, unit_id)
                if placement:
                    if enrollment["placement"]:
                        error("PLACEMENT_ALREADY_COMPLETED", 409)
                else:
                    if not enrollment["placement"]:
                        error("PLACEMENT_REQUIRED", 409)
                    done = self.completed_units(db, owner, enrollment["id"])
                    lessons = course["lessons"][:index] if kind == "LESSON" else course["lessons"][index - 4:index + 1]
                    if any(x["id"] not in done for x in lessons):
                        error("PREVIOUS_LESSON_REQUIRED", 409)
                    for end in range(5, index + 1, 5):
                        if self.catalog.review_id(course, end) not in done:
                            error("PREVIOUS_REVIEW_REQUIRED", 409)
                # Reopening/resending start resumes the current attempt instead of producing duplicates.
                row = db.execute("SELECT * FROM attempts WHERE user_id=? AND enrollment_id=? AND unit_id=? AND curriculum_version=? AND state='IN_PROGRESS' ORDER BY updated_at DESC LIMIT 1",
                                 (owner, enrollment["id"], unit_id, course["version"])).fetchone()
                if not row:
                    item = identifier()
                    db.execute("INSERT INTO attempts(id,user_id,enrollment_id,unit_id,kind,curriculum_version,updated_at) VALUES(?,?,?,?,?,?,?)",
                               (item, owner, enrollment["id"], unit_id, kind, course["version"], self.clock()))
                    row = self.owned_attempt(db, owner, item)
                    Database.event(db, owner, "ATTEMPT", item)
                return self.attempt_dto(db, owner, row)
            return idem(db, owner, "placement" if placement else "start_attempt", key, body, action)

    def save(self, owner, item, body, revision):
        with self.db.transaction() as db:
            row = self.owned_attempt(db, owner, item)
            check_revision(row, revision)
            if row["state"] != "IN_PROGRESS":
                error("ATTEMPT_ALREADY_COMPLETED", 409)
            enrollment = self.enrollment(db, owner, row["enrollment_id"])
            course = self.catalog.course(enrollment["subject_id"], enrollment["grade"])
            if row["curriculum_version"] != course["version"]:
                error("CURRICULUM_VERSION_CONFLICT", 409)
            _, _, questions, _ = self.catalog.unit(course, row["unit_id"])
            bank = {q["id"]: q for q in questions}
            if not 0 <= body["step"] <= len(questions) + 2:
                error("INVALID_STEP")
            previous = {x["questionId"]: x for x in json.loads(row["answers"])}
            seen = set()
            for answer in body["answers"]:
                qid = answer["questionId"]
                if qid in seen or qid not in bank:
                    error("INVALID_QUESTION")
                seen.add(qid)
                q, old = bank[qid], previous.get(qid, {})
                selected = answer["selectedOptionId"]
                if selected not in [x["id"] for x in q["options"]]:
                    error("INVALID_ANSWER")
                submissions = old.get("submissions", {})
                submission_id = answer["submissionId"]
                if submission_id in submissions:
                    if submissions[submission_id] != selected:
                        error("SUBMISSION_CONFLICT", 409)
                    continue
                if len(submissions) >= 100:
                    error("TOO_MANY_ANSWER_ATTEMPTS", 409)
                if old and row["kind"] == "PLACEMENT":
                    error("PLACEMENT_ANSWER_FINAL", 409)
                correct = selected == q["correctOptionId"]
                wrong = old.get("wrongCount", 0) + int(not correct)
                submissions[submission_id] = selected
                previous[qid] = {"questionId": qid, "selectedOptionId": selected, "correct": correct,
                                 "wrongCount": wrong, "assisted": old.get("assisted", False) or wrong >= 2,
                                 "firstTryCorrect": old.get("firstTryCorrect", correct), "submissions": submissions}
            db.execute("UPDATE attempts SET step=?,answers=?,revision=revision+1,updated_at=? WHERE id=?",
                       (body["step"], dump(list(previous.values())), self.clock(), item))
            Database.event(db, owner, "ATTEMPT", item, row["revision"] + 1)
            return self.attempt_dto(db, owner, self.owned_attempt(db, owner, item))

    def complete(self, owner, body, key):
        with self.db.transaction() as db:
            def action():
                row = self.owned_attempt(db, owner, body["attemptId"])
                existing = db.execute("SELECT result FROM completions WHERE attempt_id=?", (row["id"],)).fetchone()
                if existing:
                    return {**json.loads(existing[0]), "replayed": True}
                enrollment = self.enrollment(db, owner, row["enrollment_id"])
                course = self.catalog.course(enrollment["subject_id"], enrollment["grade"])
                if course["version"] != row["curriculum_version"]:
                    error("CURRICULUM_VERSION_CONFLICT", 409)
                kind, _, questions, _ = self.catalog.unit(course, row["unit_id"])
                answers = {x["questionId"]: x for x in json.loads(row["answers"])}
                if any(q["id"] not in answers or (kind != "PLACEMENT" and not answers[q["id"]]["correct"]) for q in questions):
                    error("PRACTICE_NOT_FINISHED", 409)
                now = self.clock()
                day = datetime.fromtimestamp(now, ZONE).date().isoformat()
                exp, awarded, lit = 0, False, False
                if kind == "PLACEMENT":
                    score = sum(int(answers[q["id"]]["correct"]) for q in questions)
                    assessment = {"correct": score, "total": len(questions),
                                  "recommendation": "GUIDED" if score < len(questions) * .6 else "STANDARD",
                                  "scope": "INTRODUCTORY_BLOCK", "completedAt": iso(now)}
                    db.execute("UPDATE enrollments SET placement=?,revision=revision+1,updated_at=? WHERE id=?",
                               (dump(assessment), now, enrollment["id"]))
                    Database.event(db, owner, "ENROLLMENT", enrollment["id"], enrollment["revision"] + 1)
                else:
                    exp = 200 if kind == "LESSON" else 400
                    awarded = db.execute("INSERT OR IGNORE INTO awards VALUES(?,?,?,?,?,?,?)",
                                         (owner, enrollment["subject_id"], enrollment["grade"], row["unit_id"], exp, day, now)).rowcount == 1
                    if not awarded:
                        exp = 0
                    lit = db.execute("INSERT OR IGNORE INTO learning_days VALUES(?,?)", (owner, day)).rowcount == 1
                days = {r[0] for r in db.execute("SELECT day FROM learning_days WHERE user_id=?", (owner,))}
                stats=reward_summary(db.execute("SELECT xp,day FROM awards WHERE user_id=?",(owner,)).fetchall(),days,now)
                streak,daily=stats["streakDays"],stats["dailyXp"]
                item = identifier()
                result = {"id": item, "attemptId": row["id"], "rewardUnitId": row["unit_id"], "kind": kind,
                          "xpAwarded": exp, "firstRewardForUnit": awarded, "streakLit": lit,
                          "streakDays": streak, "dailyXp": daily, "learningDay": day,
                          "timezone": "Asia/Ho_Chi_Minh", "completedAt": iso(now), "replayed": False, "revision": 1}
                if kind == "PLACEMENT":
                    result["assessment"] = assessment
                db.execute("INSERT INTO completions VALUES(?,?,?,?,?,?,?)", (item, row["id"], owner, exp, day, dump(result), now))
                db.execute("UPDATE attempts SET state='COMPLETED',revision=revision+1,updated_at=? WHERE id=?", (now, row["id"]))
                Database.event(db, owner, "COMPLETION", item)
                Database.event(db, owner, "ATTEMPT", row["id"], row["revision"] + 1)
                if kind != "PLACEMENT":
                    Database.event(db, None, "RANKING", "current")
                    Database.event(db, None, "PROFILE", owner)
                return result
            return idem(db, owner, "completion", key, body, action)

    def handoff(self, owner, item, body, key):
        with self.db.transaction() as db:
            def action():
                row = self.owned_attempt(db, owner, item)
                if row["state"] != "IN_PROGRESS" or row["kind"] == "PLACEMENT":
                    error("HANDOFF_NOT_AVAILABLE", 409)
                attempt = self.attempt_dto(db, owner, row)
                q = next((q for q in attempt["questions"] if q["id"] == body["questionId"]), None)
                if not q:
                    error("QUESTION_NOT_FOUND", 404)
                enrollment = self.enrollment(db, owner, row["enrollment_id"])
                value = {"attemptId": item, "enrollmentId": row["enrollment_id"], "lessonId": row["unit_id"],
                         "questionId": q["id"], "question": q["prompt"], "subjectId": enrollment["subject_id"],
                         "grade": enrollment["grade"], "step": row["step"], "attemptRevision": row["revision"],
                         "curriculumVersion": row["curriculum_version"]}
                handoff_id = identifier()
                db.execute("INSERT INTO resources(id,kind,user_id,body,updated_at) VALUES(?,'LESSON_HANDOFF',?,?,?)",
                           (handoff_id, owner, dump(value), self.clock()))
                Database.event(db, owner, "LESSON_HANDOFF", handoff_id)
                return resource(owned_resource(db, owner, "LESSON_HANDOFF", handoff_id))
            return idem(db, owner, "handoff:" + item, key, body, action)

    def resume(self, owner, item):
        with self.db.read() as db:
            handoff = resource(owned_resource(db, owner, "LESSON_HANDOFF", item))
            row = self.owned_attempt(db, owner, handoff["attemptId"])
            if row["curriculum_version"] != handoff["curriculumVersion"]:
                error("CURRICULUM_VERSION_CONFLICT", 409)
            # Read current state: returning from Chat must never overwrite a newer device's progress.
            return {"handoff": handoff, "attempt": self.attempt_dto(db, owner, row)}

    def bank(self, owner, enrollment_id):
        with self.db.read() as db:
            enrollment = self.enrollment(db, owner, enrollment_id)
            course = self.catalog.course(enrollment["subject_id"], enrollment["grade"])
            rows = db.execute("SELECT * FROM attempts WHERE user_id=? AND enrollment_id=? AND curriculum_version=? AND kind<>'PLACEMENT' ORDER BY updated_at",
                              (owner, enrollment_id, course["version"])).fetchall()
            practiced = {}
            for row in rows:
                for answer in json.loads(row["answers"]):
                    qid = answer["questionId"]
                    old = practiced.get(qid, {"wrongCount": 0, "assisted": False})
                    practiced[qid] = {"wrongCount": max(old["wrongCount"], answer["wrongCount"]),
                                      "assisted": old["assisted"] or answer["assisted"]}
            items = []
            for lesson in course["lessons"]:
                for q in lesson["questions"]:
                    if q["id"] in practiced:
                        items.append({**deepcopy(q), **practiced[q["id"]], "lessonId": lesson["id"],
                                      "subjectId": course["subjectId"], "grade": course["grade"],
                                      "curriculumVersion": course["version"], "source": "CURATED_COURSE"})
            return {"items": items, "enrollmentId": enrollment_id, "downloadedAt": iso(self.clock()),
                    "offlinePracticeAwardsXp": False}
