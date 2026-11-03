import json
import sqlite3
import time
import uuid
from fastapi import HTTPException
from .database import dump
from .common import ALL_SUBJECTS, ACTIVE_SUBJECTS

class ExamService:
    def __init__(self, database, time_provider=time.time):
        self.db = database
        self.time = time_provider

    def seed_sample_exams(self):
        """Seeds standard midterm and final physics exams for Grade 10 if none exist."""
        with self.db.transaction() as db:
            existing = db.execute("SELECT COUNT(*) as cnt FROM exams").fetchone()["cnt"]
            if existing > 0:
                return

            # Check if we have published physics grade 10 questions
            q_rows = db.execute(
                "SELECT id, points FROM questions WHERE subject_id='physics' AND grade=10 AND status='PUBLISHED' ORDER BY created_at"
            ).fetchall()
            if not q_rows or len(q_rows) < 1:
                return

            now = self.time()
            all_q_ids = [r["id"] for r in q_rows]
            
            # Midterm exam: up to 10 questions, 45 minutes
            midterm_q_ids = all_q_ids[:min(10, len(all_q_ids))]
            midterm_points = sum((r["points"] if r["points"] is not None else 10) for r in q_rows if r["id"] in midterm_q_ids)
            db.execute("""
                INSERT INTO exams (
                    id, title, subject_id, grade, exam_type, semester,
                    duration_minutes, question_ids, total_points, question_count,
                    status, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                str(uuid.uuid4()), "Đề thi Giữa Học kỳ 1 - Vật lý 10", "physics", 10, "MIDTERM", 1,
                45, dump(midterm_q_ids), midterm_points, len(midterm_q_ids),
                "PUBLISHED", 1, now, now
            ))

            # Final exam: all available questions, 60 minutes
            final_q_ids = all_q_ids
            final_points = sum((r["points"] if r["points"] is not None else 10) for r in q_rows)
            db.execute("""
                INSERT INTO exams (
                    id, title, subject_id, grade, exam_type, semester,
                    duration_minutes, question_ids, total_points, question_count,
                    status, version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                str(uuid.uuid4()), "Đề thi Cuối Học kỳ 1 - Vật lý 10", "physics", 10, "FINAL", 1,
                60, dump(final_q_ids), final_points, len(final_q_ids),
                "PUBLISHED", 1, now, now
            ))

    # =========================================================================
    # STUDENT ENDPOINTS
    # =========================================================================

    def list_exams(self, subject_id: str, grade: int, exam_type: str | None = None, semester: int | None = None):
        """Lists published exams for students filtered by subject, grade, and optional exam type / semester."""
        if grade not in (10, 11, 12):
            raise HTTPException(400, "Khối lớp không hợp lệ, phải từ 10 đến 12.")

        query = """
            SELECT id, title, subject_id, grade, exam_type, semester,
                   duration_minutes, total_points, question_count, status, created_at
            FROM exams
            WHERE status = 'PUBLISHED' AND subject_id = ? AND grade = ?
        """
        params = [subject_id, grade]

        if exam_type:
            if exam_type.upper() not in ("MIDTERM", "FINAL"):
                raise HTTPException(400, "Loại kỳ thi không hợp lệ (phải là MIDTERM hoặc FINAL).")
            query += " AND exam_type = ?"
            params.append(exam_type.upper())

        if semester:
            if semester not in (1, 2):
                raise HTTPException(400, "Học kỳ không hợp lệ (phải là 1 hoặc 2).")
            query += " AND semester = ?"
            params.append(semester)

        query += " ORDER BY created_at DESC"

        with self.db.read() as db:
            rows = db.execute(query, params).fetchall()
            items = []
            for r in rows:
                items.append({
                    "id": r["id"],
                    "title": r["title"],
                    "subjectId": r["subject_id"],
                    "grade": r["grade"],
                    "examType": r["exam_type"],
                    "semester": r["semester"],
                    "durationMinutes": r["duration_minutes"],
                    "totalPoints": r["total_points"],
                    "questionCount": r["question_count"],
                    "status": r["status"],
                    "createdAt": r["created_at"]
                })
            return {"items": items, "total": len(items)}

    def get_exam_for_student(self, exam_id: str):
        """Gets published exam metadata for student before starting."""
        with self.db.read() as db:
            row = db.execute(
                "SELECT * FROM exams WHERE id = ? AND status = 'PUBLISHED'", (exam_id,)
            ).fetchone()
            if not row:
                raise HTTPException(404, "Không tìm thấy đề thi hoặc đề thi chưa được xuất bản.")

            return {
                "id": row["id"],
                "title": row["title"],
                "subjectId": row["subject_id"],
                "grade": row["grade"],
                "examType": row["exam_type"],
                "semester": row["semester"],
                "durationMinutes": row["duration_minutes"],
                "totalPoints": row["total_points"],
                "questionCount": row["question_count"],
                "status": row["status"],
                "createdAt": row["created_at"]
            }

    def start_session(self, user_id: int, exam_id: str, idempotency_key: str | None = None):
        """Starts a new exam session or resumes an ongoing session before deadline."""
        with self.db.transaction() as db:
            # 1. Idempotency check
            if idempotency_key:
                cached = db.execute(
                    "SELECT response FROM idempotency WHERE owner=? AND operation='START_EXAM_SESSION' AND key=?",
                    (user_id, idempotency_key)
                ).fetchone()
                if cached:
                    return json.loads(cached["response"])

            # 2. Fetch exam and validate published status
            exam = db.execute("SELECT * FROM exams WHERE id = ?", (exam_id,)).fetchone()
            if not exam:
                raise HTTPException(404, "Đề thi không tồn tại.")
            if exam["status"] != "PUBLISHED":
                raise HTTPException(403, "Đề thi chưa được xuất bản, học sinh không thể làm bài.")

            now = self.time()

            # 3. Resume ongoing session check:
            # If student has an active IN_PROGRESS session for this exam where now < deadline_at, resume it
            active_session = db.execute(
                """SELECT * FROM exam_sessions
                   WHERE user_id = ? AND exam_id = ? AND state = 'IN_PROGRESS' AND deadline_at > ?
                   ORDER BY started_at DESC LIMIT 1""",
                (user_id, exam_id, now)
            ).fetchone()

            if active_session:
                resp = self._format_session_response(active_session, reveal_answers=False)
                if idempotency_key:
                    db.execute(
                        "INSERT OR REPLACE INTO idempotency (owner, operation, key, request_hash, response) VALUES (?, ?, ?, ?, ?)",
                        (user_id, "START_EXAM_SESSION", idempotency_key, exam_id, dump(resp))
                    )
                return resp

            # 4. Parse question IDs and take fresh snapshots from questions table
            try:
                q_ids = json.loads(exam["question_ids"])
            except Exception:
                raise HTTPException(500, "Dữ liệu danh sách câu hỏi của đề thi bị lỗi.")

            if not q_ids or len(q_ids) == 0:
                raise HTTPException(400, "Đề thi không có câu hỏi nào để bắt đầu làm bài.")

            placeholders = ",".join("?" for _ in q_ids)
            q_rows = db.execute(
                f"SELECT * FROM questions WHERE id IN ({placeholders})", q_ids
            ).fetchall()
            q_map = {r["id"]: r for r in q_rows}

            # Integrity validation
            snapshots = []
            total_points = 0
            for qid in q_ids:
                q = q_map.get(qid)
                if not q:
                    raise HTTPException(400, f"Câu hỏi định danh [{qid}] trong đề thi không tồn tại.")
                if q["status"] != "PUBLISHED":
                    raise HTTPException(400, f"Câu hỏi [{q['code'] or qid}] chưa được xuất bản.")
                if q["subject_id"] != exam["subject_id"] or q["grade"] != exam["grade"]:
                    raise HTTPException(400, f"Câu hỏi [{q['code'] or qid}] không cùng môn hoặc cùng khối lớp với đề thi.")

                pts = q["points"] if q["points"] is not None else 10
                total_points += pts
                snapshots.append({
                    "id": q["id"],
                    "code": q["code"],
                    "prompt": q["prompt"],
                    "optionA": q["option_a"],
                    "optionB": q["option_b"],
                    "optionC": q["option_c"],
                    "optionD": q["option_d"],
                    "correctOption": q["correct_option"],
                    "explanation": q["explanation"],
                    "difficulty": q["difficulty"],
                    "chapter": q["chapter"],
                    "topic": q["topic"],
                    "points": pts
                })

            # 5. Calculate authoritative time
            duration = exam["duration_minutes"]
            deadline = now + (duration * 60.0)
            session_id = str(uuid.uuid4())

            db.execute("""
                INSERT INTO exam_sessions (
                    id, user_id, exam_id, subject_id, grade, exam_title, exam_type, semester,
                    duration_minutes, question_snapshots, answers, state,
                    score, score_10, raw_score, total_points, correct_count, total_count,
                    started_at, deadline_at, submitted_at, revision, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                session_id, user_id, exam_id, exam["subject_id"], exam["grade"],
                exam["title"], exam["exam_type"], exam["semester"],
                duration, dump(snapshots), dump({}), "IN_PROGRESS",
                0.0, 0.0, 0, total_points, 0, len(snapshots),
                now, deadline, None, 1, now, now
            ))

            new_session = db.execute("SELECT * FROM exam_sessions WHERE id = ?", (session_id,)).fetchone()
            resp = self._format_session_response(new_session, reveal_answers=False)

            if idempotency_key:
                db.execute(
                    "INSERT OR REPLACE INTO idempotency (owner, operation, key, request_hash, response) VALUES (?, ?, ?, ?, ?)",
                    (user_id, "START_EXAM_SESSION", idempotency_key, exam_id, dump(resp))
                )

            return resp

    def get_session(self, user_id: int, session_id: str):
        """Retrieves session details. Performs lazy finalization if deadline has passed."""
        with self.db.transaction() as db:
            session = db.execute("SELECT * FROM exam_sessions WHERE id = ?", (session_id,)).fetchone()
            if not session:
                raise HTTPException(404, "Không tìm thấy phiên làm bài thi.")
            if session["user_id"] != user_id:
                raise HTTPException(403, "Bạn không có quyền truy cập phiên làm bài thi này.")

            now = self.time()

            # Lazy finalization if expired
            if session["state"] == "IN_PROGRESS" and now >= session["deadline_at"]:
                self._finalize_session(db, session, now, final_state="TIMED_OUT")
                session = db.execute("SELECT * FROM exam_sessions WHERE id = ?", (session_id,)).fetchone()

            reveal = session["state"] in ("SUBMITTED", "TIMED_OUT")
            return self._format_session_response(session, reveal_answers=reveal)

    def save_answer(self, user_id: int, session_id: str, question_id: str, selected_option: str | None, revision: int | None = None):
        """Saves or clears an answer choice before deadline with revision guard."""
        should_raise_expired = False
        with self.db.transaction() as db:
            session = db.execute("SELECT * FROM exam_sessions WHERE id = ?", (session_id,)).fetchone()
            if not session:
                raise HTTPException(404, "Không tìm thấy phiên làm bài thi.")
            if session["user_id"] != user_id:
                raise HTTPException(403, "Bạn không có quyền chỉnh sửa phiên làm bài thi này.")

            now = self.time()

            # State & Deadline guard
            if session["state"] != "IN_PROGRESS":
                raise HTTPException(409, {"code": "EXAM_SESSION_CLOSED", "message": "Bài thi đã được nộp hoặc đã kết thúc."})
            if now >= session["deadline_at"]:
                self._finalize_session(db, session, now, final_state="TIMED_OUT")
                should_raise_expired = True

            if not should_raise_expired:
                # Validate question belongs to session
                snapshots = json.loads(session["question_snapshots"] or "[]")
                q_ids = {q["id"] for q in snapshots}
                if question_id not in q_ids:
                    raise HTTPException(400, "Câu hỏi không thuộc đề thi này.")

                answers = json.loads(session["answers"] or "{}")
                existing = answers.get(question_id)
                existing_rev = existing.get("revision", 0) if isinstance(existing, dict) else 0
                existing_opt = existing.get("selectedOption") if isinstance(existing, dict) else (existing if isinstance(existing, str) else None)

                # Normalize incoming selected option
                if selected_option is None or (isinstance(selected_option, str) and not selected_option.strip()):
                    incoming_opt = None
                else:
                    incoming_opt = selected_option.upper().strip()
                    if incoming_opt not in ("A", "B", "C", "D"):
                        raise HTTPException(400, "Lựa chọn đáp án không hợp lệ (phải là A, B, C, D hoặc null).")

                # Revision contract check
                if revision is not None:
                    if revision < existing_rev:
                        # Request cũ: Không được ghi đè lựa chọn mới hơn hoặc thao tác bỏ chọn mới hơn
                        resp = self._format_session_response(session, reveal_answers=False)
                        resp["status"] = "IGNORED_STALE"
                        resp["questionId"] = question_id
                        return resp
                    elif revision == existing_rev:
                        if incoming_opt == existing_opt:
                            # Cùng revision, cùng payload -> Safe Retry (idempotent 200)
                            resp = self._format_session_response(session, reveal_answers=False)
                            resp["status"] = "SAVED"
                            resp["questionId"] = question_id
                            return resp
                        else:
                            # Cùng revision, khác payload -> Phải từ chối 409 Conflict rõ ràng
                            raise HTTPException(409, {
                                "code": "REVISION_CONFLICT",
                                "message": f"Xung đột phiên bản đáp án câu hỏi [{question_id}]: revision {revision} đã được ghi nhận với giá trị khác."
                            })
                    else:
                        # revision > existing_rev
                        new_rev = revision
                else:
                    new_rev = existing_rev + 1

                # Record answer with tombstone support (preserves revision even when cleared)
                answers[question_id] = {
                    "selectedOption": incoming_opt,
                    "savedAt": now,
                    "revision": new_rev
                }

                new_revision = session["revision"] + 1
                db.execute("""
                    UPDATE exam_sessions SET
                        answers = ?, revision = ?, updated_at = ?
                    WHERE id = ?
                """, (dump(answers), new_revision, now, session_id))

                updated_session = db.execute("SELECT * FROM exam_sessions WHERE id = ?", (session_id,)).fetchone()
                resp = self._format_session_response(updated_session, reveal_answers=False)
                resp["status"] = "SAVED"
                resp["questionId"] = question_id
                return resp

        # If expired, raise AFTER the transaction has committed TIMED_OUT state!
        if should_raise_expired:
            raise HTTPException(409, {
                "code": "EXAM_SESSION_EXPIRED",
                "message": "Thời gian làm bài đã kết thúc. Bài thi đã được tự động nộp."
            })

    def submit_session(self, user_id: int, session_id: str, idempotency_key: str | None = None):
        """Submits exam session. Finalizes grading and saves wrong questions to notebook."""
        with self.db.transaction() as db:
            # 1. Idempotency check
            if idempotency_key:
                cached = db.execute(
                    "SELECT response FROM idempotency WHERE owner=? AND operation='SUBMIT_EXAM_SESSION' AND key=?",
                    (user_id, idempotency_key)
                ).fetchone()
                if cached:
                    return json.loads(cached["response"])

            session = db.execute("SELECT * FROM exam_sessions WHERE id = ?", (session_id,)).fetchone()
            if not session:
                raise HTTPException(404, "Không tìm thấy phiên làm bài thi.")
            if session["user_id"] != user_id:
                raise HTTPException(403, "Bạn không có quyền nộp bài thi này.")

            # 2. Already completed check (idempotent submission)
            if session["state"] in ("SUBMITTED", "TIMED_OUT"):
                resp = self._format_session_response(session, reveal_answers=True)
                if idempotency_key:
                    db.execute(
                        "INSERT OR REPLACE INTO idempotency (owner, operation, key, request_hash, response) VALUES (?, ?, ?, ?, ?)",
                        (user_id, "SUBMIT_EXAM_SESSION", idempotency_key, session_id, dump(resp))
                    )
                return resp

            now = self.time()
            final_state = "SUBMITTED" if now < session["deadline_at"] else "TIMED_OUT"

            # 3. Finalize and grade
            self._finalize_session(db, session, now, final_state=final_state)
            session = db.execute("SELECT * FROM exam_sessions WHERE id = ?", (session_id,)).fetchone()
            resp = self._format_session_response(session, reveal_answers=True)

            if idempotency_key:
                db.execute(
                    "INSERT OR REPLACE INTO idempotency (owner, operation, key, request_hash, response) VALUES (?, ?, ?, ?, ?)",
                    (user_id, "SUBMIT_EXAM_SESSION", idempotency_key, session_id, dump(resp))
                )

            return resp

    def list_sessions(self, user_id: int, exam_id: str | None = None, subject_id: str | None = None, grade: int | None = None, state: str | None = None, limit: int = 20):
        """Lists student's exam sessions history."""
        query = "SELECT * FROM exam_sessions WHERE user_id = ?"
        params = [user_id]

        if exam_id:
            query += " AND exam_id = ?"
            params.append(exam_id)
        if subject_id:
            query += " AND subject_id = ?"
            params.append(subject_id)
        if grade:
            query += " AND grade = ?"
            params.append(grade)
        if state:
            query += " AND state = ?"
            params.append(state)

        query += " ORDER BY started_at DESC LIMIT ?"
        params.append(min(max(1, limit), 100))

        with self.db.transaction() as db:
            now = self.time()
            rows = db.execute(query, params).fetchall()
            items = []
            for r in rows:
                # Lazy finalization check
                if r["state"] == "IN_PROGRESS" and now >= r["deadline_at"]:
                    self._finalize_session(db, r, now, final_state="TIMED_OUT")
                    r = db.execute("SELECT * FROM exam_sessions WHERE id = ?", (r["id"],)).fetchone()

                reveal = r["state"] in ("SUBMITTED", "TIMED_OUT")
                items.append(self._format_session_response(r, reveal_answers=reveal))

            return {"items": items, "total": len(items)}

    # =========================================================================
    # ADMIN ENDPOINTS
    # =========================================================================

    def list_exams_admin(self, subject_id: str | None = None, grade: int | None = None, exam_type: str | None = None, semester: int | None = None, status: str | None = None):
        """Lists exams for admin with filters."""
        query = "SELECT * FROM exams WHERE 1=1"
        params = []

        if subject_id:
            query += " AND subject_id = ?"
            params.append(subject_id)
        if grade:
            query += " AND grade = ?"
            params.append(grade)
        if exam_type:
            query += " AND exam_type = ?"
            params.append(exam_type.upper())
        if semester:
            query += " AND semester = ?"
            params.append(semester)
        if status:
            query += " AND status = ?"
            params.append(status.upper())

        query += " ORDER BY updated_at DESC"

        with self.db.read() as db:
            rows = db.execute(query, params).fetchall()
            items = []
            for r in rows:
                q_ids = json.loads(r["question_ids"] or "[]")
                items.append({
                    "id": r["id"],
                    "title": r["title"],
                    "subjectId": r["subject_id"],
                    "grade": r["grade"],
                    "examType": r["exam_type"],
                    "semester": r["semester"],
                    "durationMinutes": r["duration_minutes"],
                    "questionIds": q_ids,
                    "totalPoints": r["total_points"],
                    "questionCount": r["question_count"],
                    "status": r["status"],
                    "version": r["version"],
                    "createdAt": r["created_at"],
                    "updatedAt": r["updated_at"]
                })
            return items

    def get_exam_admin_by_id(self, exam_id: str):
        """Gets detailed exam for admin, including question details."""
        with self.db.read() as db:
            exam = db.execute("SELECT * FROM exams WHERE id = ?", (exam_id,)).fetchone()
            if not exam:
                raise HTTPException(404, "Không tìm thấy đề thi.")

            q_ids = json.loads(exam["question_ids"] or "[]")
            return {
                "id": exam["id"],
                "title": exam["title"],
                "subjectId": exam["subject_id"],
                "grade": exam["grade"],
                "examType": exam["exam_type"],
                "semester": exam["semester"],
                "durationMinutes": exam["duration_minutes"],
                "questionIds": q_ids,
                "totalPoints": exam["total_points"],
                "questionCount": exam["question_count"],
                "status": exam["status"],
                "version": exam["version"],
                "createdAt": exam["created_at"],
                "updatedAt": exam["updated_at"]
            }

    def create_or_update_exam_admin(self, admin_id: int, data: dict, expected_version: int | None = None):
        """Creates or updates an exam with optimistic locking and question validation."""
        title = (data.get("title") or "").strip()
        if not title:
            raise HTTPException(400, "Tiêu đề đề thi không được để trống.")
        if len(title) > 200:
            raise HTTPException(400, "Tiêu đề đề thi không được vượt quá 200 ký tự.")

        subject_id = data.get("subjectId", "physics")
        if subject_id not in ALL_SUBJECTS and subject_id not in ACTIVE_SUBJECTS:
            raise HTTPException(400, f"Môn học [{subject_id}] không hợp lệ.")

        grade = data.get("grade", 10)
        if grade not in (10, 11, 12):
            raise HTTPException(400, "Khối lớp phải là 10, 11 hoặc 12.")

        exam_type = (data.get("examType") or "MIDTERM").upper()
        if exam_type not in ("MIDTERM", "FINAL"):
            raise HTTPException(400, "Loại kỳ thi phải là MIDTERM hoặc FINAL.")

        semester = data.get("semester", 1)
        if semester not in (1, 2):
            raise HTTPException(400, "Học kỳ phải là 1 hoặc 2.")

        duration = data.get("durationMinutes", 45)
        if not isinstance(duration, int) or duration < 5 or duration > 300:
            raise HTTPException(400, "Thời lượng thi phải từ 5 đến 300 phút.")

        q_ids = data.get("questionIds", [])
        if not isinstance(q_ids, list) or len(q_ids) == 0:
            raise HTTPException(400, "Đề thi phải có ít nhất 1 câu hỏi.")
        if len(q_ids) > 500:
            raise HTTPException(400, "Số lượng câu hỏi trong đề thi không được vượt quá 500 câu.")

        # Check duplicate question IDs in the list
        if len(q_ids) != len(set(q_ids)):
            raise HTTPException(400, "Danh sách câu hỏi trong đề thi có chứa câu trùng lặp.")

        status = (data.get("status") or "DRAFT").upper()
        if status not in ("DRAFT", "PUBLISHED"):
            raise HTTPException(400, "Trạng thái đề thi phải là DRAFT hoặc PUBLISHED.")

        now = self.time()

        with self.db.transaction() as db:
            # Validate all questions
            placeholders = ",".join("?" for _ in q_ids)
            q_rows = db.execute(
                f"SELECT id, code, subject_id, grade, points, status FROM questions WHERE id IN ({placeholders})", q_ids
            ).fetchall()
            q_map = {r["id"]: r for r in q_rows}

            total_points = 0
            for qid in q_ids:
                q = q_map.get(qid)
                if not q:
                    raise HTTPException(400, f"Câu hỏi [{qid}] không tồn tại trong hệ thống.")
                if q["subject_id"] != subject_id or q["grade"] != grade:
                    raise HTTPException(400, f"Câu hỏi [{q['code'] or qid}] không thuộc môn {subject_id} lớp {grade}.")
                if status == "PUBLISHED" and q["status"] != "PUBLISHED":
                    raise HTTPException(400, f"Không thể xuất bản đề thi khi câu hỏi [{q['code'] or qid}] đang ở trạng thái Bản nháp (DRAFT).")
                pts = q["points"] if q["points"] is not None else 10
                total_points += pts

            exam_id = data.get("id")
            if exam_id:
                # Update existing exam
                existing = db.execute("SELECT * FROM exams WHERE id = ?", (exam_id,)).fetchone()
                if not existing:
                    raise HTTPException(404, f"Không tìm thấy đề thi với mã [{exam_id}] để cập nhật.")

                current_ver = existing["version"]
                if expected_version is not None and expected_version != current_ver:
                    raise HTTPException(409, f"Xung đột phiên bản: Đề thi đã được cập nhật lên v{current_ver} bởi người khác. Vui lòng tải lại trước khi lưu.")

                new_ver = current_ver + 1
                db.execute("""
                    UPDATE exams SET
                        title = ?, subject_id = ?, grade = ?, exam_type = ?, semester = ?,
                        duration_minutes = ?, question_ids = ?, total_points = ?, question_count = ?,
                        status = ?, version = ?, updated_at = ?
                    WHERE id = ?
                """, (
                    title, subject_id, grade, exam_type, semester,
                    duration, dump(q_ids), total_points, len(q_ids),
                    status, new_ver, now, exam_id
                ))
            else:
                # Create new exam
                exam_id = str(uuid.uuid4())
                new_ver = 1
                db.execute("""
                    INSERT INTO exams (
                        id, title, subject_id, grade, exam_type, semester,
                        duration_minutes, question_ids, total_points, question_count,
                        status, version, created_by, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (
                    exam_id, title, subject_id, grade, exam_type, semester,
                    duration, dump(q_ids), total_points, len(q_ids),
                    status, new_ver, admin_id, now, now
                ))

            updated = db.execute("SELECT * FROM exams WHERE id = ?", (exam_id,)).fetchone()
            return {
                "id": updated["id"],
                "title": updated["title"],
                "subjectId": updated["subject_id"],
                "grade": updated["grade"],
                "examType": updated["exam_type"],
                "semester": updated["semester"],
                "durationMinutes": updated["duration_minutes"],
                "questionIds": json.loads(updated["question_ids"] or "[]"),
                "totalPoints": updated["total_points"],
                "questionCount": updated["question_count"],
                "status": updated["status"],
                "version": updated["version"],
                "createdAt": updated["created_at"],
                "updatedAt": updated["updated_at"]
            }

    def publish_exam_admin(self, exam_id: str, publish: bool = True):
        """Publishes or unpublishes an exam."""
        with self.db.transaction() as db:
            exam = db.execute("SELECT * FROM exams WHERE id = ?", (exam_id,)).fetchone()
            if not exam:
                raise HTTPException(404, "Không tìm thấy đề thi.")

            now = self.time()
            new_status = "PUBLISHED" if publish else "DRAFT"

            if publish:
                q_ids = json.loads(exam["question_ids"] or "[]")
                if not q_ids:
                    raise HTTPException(400, "Đề thi không có câu hỏi nào, không thể xuất bản.")
                placeholders = ",".join("?" for _ in q_ids)
                draft_qs = db.execute(
                    f"SELECT id, code FROM questions WHERE id IN ({placeholders}) AND status != 'PUBLISHED'", q_ids
                ).fetchall()
                if draft_qs:
                    codes = ", ".join(q["code"] or q["id"] for q in draft_qs)
                    raise HTTPException(400, f"Không thể xuất bản đề thi vì các câu hỏi sau chưa được xuất bản: {codes}")

            new_ver = exam["version"] + 1
            db.execute(
                "UPDATE exams SET status = ?, version = ?, updated_at = ? WHERE id = ?",
                (new_status, new_ver, now, exam_id)
            )

            updated = db.execute("SELECT * FROM exams WHERE id = ?", (exam_id,)).fetchone()
            return {
                "id": updated["id"],
                "title": updated["title"],
                "status": updated["status"],
                "version": updated["version"]
            }

    # =========================================================================
    # INTERNAL HELPERS
    # =========================================================================

    def _finalize_session(self, db, session, now: float, final_state: str = "SUBMITTED"):
        """Grades session, updates wrong questions notebook, updates status to SUBMITTED or TIMED_OUT."""
        # Ensure session is still IN_PROGRESS to prevent duplicate side effects or double grading
        curr = db.execute("SELECT state FROM exam_sessions WHERE id = ?", (session["id"],)).fetchone()
        if not curr or curr["state"] != "IN_PROGRESS":
            return

        snapshots = json.loads(session["question_snapshots"] or "[]")
        answers = json.loads(session["answers"] or "{}")

        raw_score = 0
        correct_count = 0
        total_points = session["total_points"]
        user_id = session["user_id"]
        subject_id = session["subject_id"]
        grade = session["grade"]

        for q in snapshots:
            ans_info = answers.get(q["id"])
            user_opt = ans_info.get("selectedOption") if isinstance(ans_info, dict) else (ans_info if isinstance(ans_info, str) else None)
            correct_opt = q.get("correctOption")

            is_correct = (user_opt is not None and user_opt.strip().upper() == (correct_opt or "").strip().upper())
            if is_correct:
                raw_score += q.get("points", 10)
                correct_count += 1
            else:
                # Add to wrong questions notebook
                qid = q["id"]
                db.execute("""
                    INSERT INTO wrong_questions_notebook (
                        id, user_id, question_id, subject_id, grade,
                        selected_option, correct_option, explanation,
                        question_snapshot, state, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'NEEDS_REVIEW', ?, ?)
                    ON CONFLICT(user_id, question_id) DO UPDATE SET
                        selected_option = excluded.selected_option,
                        correct_option = excluded.correct_option,
                        explanation = excluded.explanation,
                        question_snapshot = excluded.question_snapshot,
                        state = 'NEEDS_REVIEW',
                        updated_at = excluded.updated_at
                """, (
                    str(uuid.uuid4()), user_id, qid, subject_id, grade,
                    user_opt or "", correct_opt or "", q.get("explanation") or "",
                    dump(q), now, now
                ))

        score_10 = round((raw_score / total_points * 10.0), 2) if total_points > 0 else 0.0
        new_rev = session["revision"] + 1

        db.execute("""
            UPDATE exam_sessions SET
                state = ?, score = ?, score_10 = ?, raw_score = ?,
                correct_count = ?, submitted_at = ?, revision = ?, updated_at = ?
            WHERE id = ?
        """, (
            final_state, raw_score, score_10, raw_score,
            correct_count, now, new_rev, now, session["id"]
        ))

    def _format_session_response(self, session, reveal_answers: bool = False):
        """Formats session row into response dictionary. Hides correct answers while IN_PROGRESS."""
        snapshots = json.loads(session["question_snapshots"] or "[]")
        answers = json.loads(session["answers"] or "{}")

        now = self.time()
        deadline = session["deadline_at"]
        remaining = max(0, int(deadline - now)) if session["state"] == "IN_PROGRESS" else 0

        formatted_questions = []
        for q in snapshots:
            ans_info = answers.get(q["id"])
            user_opt = ans_info.get("selectedOption") if isinstance(ans_info, dict) else (ans_info if isinstance(ans_info, str) else None)

            item = {
                "id": q["id"],
                "code": q.get("code"),
                "prompt": q["prompt"],
                "optionA": q["optionA"],
                "optionB": q["optionB"],
                "optionC": q["optionC"],
                "optionD": q["optionD"],
                "points": q.get("points", 10),
                "chapter": q.get("chapter"),
                "topic": q.get("topic"),
                "difficulty": q.get("difficulty"),
                "selectedOption": user_opt
            }

            if reveal_answers:
                correct_opt = q.get("correctOption")
                is_correct = (user_opt is not None and user_opt.strip().upper() == (correct_opt or "").strip().upper())
                item["correctOption"] = correct_opt
                item["explanation"] = q.get("explanation")
                item["isCorrect"] = is_correct

            formatted_questions.append(item)

        answers_map = {}
        question_revisions = {}
        for qid, a in answers.items():
            if isinstance(a, dict):
                if a.get("selectedOption"):
                    answers_map[qid] = a["selectedOption"]
                if "revision" in a:
                    question_revisions[qid] = a["revision"]
            elif isinstance(a, str) and a:
                answers_map[qid] = a

        answered_count = len(answers_map)

        return {
            "id": session["id"],
            "examId": session["exam_id"],
            "subjectId": session["subject_id"],
            "grade": session["grade"],
            "examTitle": session["exam_title"],
            "examType": session["exam_type"],
            "semester": session["semester"],
            "durationMinutes": session["duration_minutes"],
            "state": session["state"],
            "serverNow": now,
            "startedAt": session["started_at"],
            "deadlineAt": session["deadline_at"],
            "submittedAt": session["submitted_at"],
            "remainingSeconds": remaining,
            "score": session["score"],
            "score10": session["score_10"],
            "rawScore": session["raw_score"],
            "totalPoints": session["total_points"],
            "correctCount": session["correct_count"],
            "totalCount": session["total_count"],
            "answeredCount": answered_count,
            "questions": formatted_questions,
            "answers": answers_map,
            "questionRevisions": question_revisions,
            "revision": session["revision"],
            "createdAt": session["created_at"],
            "updatedAt": session["updated_at"]
        }
