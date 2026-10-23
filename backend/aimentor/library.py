import json
import time
import uuid
import zipfile
import io
import csv
import xml.etree.ElementTree as ET
from .common import error, iso

class LibraryService:
    def __init__(self, database):
        self.db = database

    def seed_sample_physics(self):
        with self.db.transaction() as db:
            existing = db.execute("SELECT COUNT(*) FROM questions WHERE subject_id='physics' AND grade=10").fetchone()[0]
            if existing > 0:
                return
            now = time.time()
            sample_questions = [
                {
                    "id": "q_phy10_01",
                    "code": "PHY10_CH1_Q01",
                    "subject_id": "physics",
                    "grade": 10,
                    "chapter": "Chương 1: Mở đầu & Động học",
                    "topic": "Chuyển động thẳng đều",
                    "textbook": "Kết nối tri thức",
                    "prompt": "Phương trình chuyển động của một chất điểm dọc theo trục Ox có dạng $x = 5 + 6t$ (m; s). Vận tốc của chất điểm là:",
                    "option_a": "5 m/s",
                    "option_b": "6 m/s",
                    "option_c": "-5 m/s",
                    "option_d": "11 m/s",
                    "correct_option": "B",
                    "explanation": "So sánh với phương trình tổng quát $x = x_0 + vt$, ta thấy vận tốc $v = 6\\text{ m/s}$.",
                    "difficulty": "EASY",
                    "points": 10,
                    "source": "Bộ đề ôn tập Vật lý 10",
                    "author": "Ban biên soạn AI Mentor",
                    "license": "Nội bộ",
                    "status": "PUBLISHED"
                },
                {
                    "id": "q_phy10_02",
                    "code": "PHY10_CH1_Q02",
                    "subject_id": "physics",
                    "grade": 10,
                    "chapter": "Chương 1: Mở đầu & Động học",
                    "topic": "Chuyển động thẳng biến đổi đều",
                    "textbook": "Kết nối tri thức",
                    "prompt": "Công thức liên hệ giữa vận tốc $v$, gia tốc $a$ và độ dịch chuyển $s$ trong chuyển động thẳng biến đổi đều là:",
                    "option_a": "$v^2 - v_0^2 = 2as$",
                    "option_b": "$v - v_0 = 2as$",
                    "option_c": "$v^2 + v_0^2 = 2as$",
                    "option_d": "$v^2 - v_0^2 = as$",
                    "correct_option": "A",
                    "explanation": "Công thức độc lập thời gian là $v^2 - v_0^2 = 2as$.",
                    "difficulty": "EASY",
                    "points": 10,
                    "source": "Bộ đề ôn tập Vật lý 10",
                    "author": "Ban biên soạn AI Mentor",
                    "license": "Nội bộ",
                    "status": "PUBLISHED"
                },
                {
                    "id": "q_phy10_03",
                    "code": "PHY10_CH2_Q03",
                    "subject_id": "physics",
                    "grade": 10,
                    "chapter": "Chương 2: Động lực học",
                    "topic": "Định luật II Newton",
                    "textbook": "Cánh Diều",
                    "prompt": "Một lực $F = 10\\text{ N}$ tác dụng vào vật khối lượng $m = 2\\text{ kg}$. Gia tốc của vật là:",
                    "option_a": "$5\\text{ m/s}^2$",
                    "option_b": "$20\\text{ m/s}^2$",
                    "option_c": "$0.2\\text{ m/s}^2$",
                    "option_d": "$8\\text{ m/s}^2$",
                    "correct_option": "A",
                    "explanation": "Theo Định luật II Newton: $a = \\frac{F}{m} = \\frac{10}{2} = 5\\text{ m/s}^2$.",
                    "difficulty": "MEDIUM",
                    "points": 10,
                    "source": "Bộ đề ôn tập Vật lý 10",
                    "author": "Ban biên soạn AI Mentor",
                    "license": "Nội bộ",
                    "status": "PUBLISHED"
                },
                {
                    "id": "q_phy10_04",
                    "code": "PHY10_CH1_Q04",
                    "subject_id": "physics",
                    "grade": 10,
                    "chapter": "Chương 1: Mở đầu & Động học",
                    "topic": "Sự rơi tự do",
                    "textbook": "Chân trời sáng tạo",
                    "prompt": "Chuyển động rơi tự do của một vật là chuyển động:",
                    "option_a": "Thẳng đều",
                    "option_b": "Thẳng nhanh dần đều với gia tốc g",
                    "option_c": "Thẳng chậm dần đều",
                    "option_d": "Cong đều",
                    "correct_option": "B",
                    "explanation": "Rơi tự do là chuyển động thẳng nhanh dần đều dưới tác dụng duy nhất của trọng lực với gia tốc rơi tự do g.",
                    "difficulty": "EASY",
                    "points": 10,
                    "source": "Bộ đề ôn tập Vật lý 10",
                    "author": "Ban biên soạn AI Mentor",
                    "license": "Nội bộ",
                    "status": "PUBLISHED"
                },
                {
                    "id": "q_phy10_05",
                    "code": "PHY10_CH2_Q05",
                    "subject_id": "physics",
                    "grade": 10,
                    "chapter": "Chương 2: Động lực học",
                    "topic": "Tổng hợp và phân tích lực",
                    "textbook": "Kết nối tri thức",
                    "prompt": "Hai lực đồng quy $F_1 = 3\\text{ N}$ và $F_2 = 4\\text{ N}$ vuông góc với nhau. Độ lớn của hợp lực $F$ là:",
                    "option_a": "1 N",
                    "option_b": "7 N",
                    "option_c": "5 N",
                    "option_d": "12 N",
                    "correct_option": "C",
                    "explanation": "Vì hai lực vuông góc nên $F = \\sqrt{F_1^2 + F_2^2} = \\sqrt{3^2 + 4^2} = 5\\text{ N}$.",
                    "difficulty": "MEDIUM",
                    "points": 10,
                    "source": "Bộ đề ôn tập Vật lý 10",
                    "author": "Ban biên soạn AI Mentor",
                    "license": "Nội bộ",
                    "status": "PUBLISHED"
                }
            ]

            for q in sample_questions:
                db.execute("""
                INSERT OR REPLACE INTO questions
                (id, code, version, subject_id, grade, chapter, topic, textbook, prompt, option_a, option_b, option_c, option_d, correct_option, explanation, difficulty, points, source, author, license, status, created_at, updated_at)
                VALUES (?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (q["id"], q["code"], q["subject_id"], q["grade"], q["chapter"], q["topic"], q["textbook"],
                      q["prompt"], q["option_a"], q["option_b"], q["option_c"], q["option_d"], q["correct_option"],
                      q["explanation"], q["difficulty"], q["points"], q["source"], q["author"], q["license"],
                      q["status"], now, now))

            quiz_id = "quiz_phy10_basic"
            q_ids = json.dumps([q["id"] for q in sample_questions])
            db.execute("""
            INSERT OR REPLACE INTO library_quizzes (id, title, subject_id, grade, chapter, topic, difficulty, question_ids, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (quiz_id, "Bộ đề ôn tập Vật lý 10 - Động học & Động lực học", "physics", 10, "Tổng hợp", "Tổng hợp Lớp 10", "MEDIUM", q_ids, "PUBLISHED", now))

    def list_quizzes(self, subject_id, grade, difficulty=None, chapter=None, topic=None):
        query = "SELECT * FROM library_quizzes WHERE subject_id=? AND grade=? AND status='PUBLISHED'"
        params = [subject_id, grade]
        if difficulty and difficulty.upper() != "ALL":
            query += " AND difficulty=?"
            params.append(difficulty.upper())
        if chapter:
            query += " AND chapter LIKE ?"
            params.append(f"%{chapter}%")
        if topic:
            query += " AND topic LIKE ?"
            params.append(f"%{topic}%")
        query += " ORDER BY created_at DESC"

        with self.db.read() as db:
            rows = db.execute(query, params).fetchall()
            quizzes = []
            for r in rows:
                q_ids = json.loads(r["question_ids"])
                quizzes.append({
                    "id": r["id"],
                    "title": r["title"],
                    "subjectId": r["subject_id"],
                    "grade": r["grade"],
                    "chapter": r["chapter"],
                    "topic": r["topic"],
                    "difficulty": r["difficulty"],
                    "questionCount": len(q_ids),
                    "createdAt": iso(r["created_at"])
                })
            return quizzes

    def get_quiz_for_student(self, quiz_id):
        with self.db.read() as db:
            quiz = db.execute("SELECT * FROM library_quizzes WHERE id=? AND status='PUBLISHED'", (quiz_id,)).fetchone()
            if not quiz:
                error("QUIZ_NOT_FOUND", 404)
            q_ids = json.loads(quiz["question_ids"])
            questions = []
            if q_ids:
                placeholders = ",".join("?" * len(q_ids))
                q_rows = db.execute(f"SELECT * FROM questions WHERE id IN ({placeholders}) AND status='PUBLISHED'", q_ids).fetchall()
                q_map = {r["id"]: r for r in q_rows}
                for qid in q_ids:
                    if qid in q_map:
                        r = q_map[qid]
                        # Student snapshot: DO NOT EXPOSE correct_option or explanation before answering
                        questions.append({
                            "id": r["id"],
                            "code": r["code"],
                            "chapter": r["chapter"],
                            "topic": r["topic"],
                            "prompt": r["prompt"],
                            "options": [
                                {"id": "A", "text": r["option_a"]},
                                {"id": "B", "text": r["option_b"]},
                                {"id": "C", "text": r["option_c"]},
                                {"id": "D", "text": r["option_d"]}
                            ],
                            "difficulty": r["difficulty"],
                            "points": r["points"],
                            "imageUrl": r["image_url"]
                        })
            return {
                "id": quiz["id"],
                "title": quiz["title"],
                "subjectId": quiz["subject_id"],
                "grade": quiz["grade"],
                "chapter": quiz["chapter"],
                "topic": quiz["topic"],
                "difficulty": quiz["difficulty"],
                "questions": questions
            }

    def start_session(self, user_id, quiz_id):
        quiz_data = self.get_quiz_for_student(quiz_id)
        session_id = f"psess_{uuid.uuid4().hex}"
        now = time.time()
        snapshots_json = json.dumps(quiz_data["questions"], ensure_ascii=False)

        with self.db.transaction() as db:
            db.execute("""
            INSERT INTO practice_sessions (id, user_id, quiz_id, subject_id, grade, question_snapshots, step, answers, state, score, correct_count, total_count, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, 0, '{}', 'IN_PROGRESS', 0, 0, ?, ?, ?)
            """, (session_id, user_id, quiz_id, quiz_data["subjectId"], quiz_data["grade"], snapshots_json, len(quiz_data["questions"]), now, now))

        return self.get_session(user_id, session_id)

    def get_session(self, user_id, session_id):
        with self.db.read() as db:
            row = db.execute("SELECT * FROM practice_sessions WHERE id=? AND user_id=?", (session_id, user_id)).fetchone()
            if not row:
                error("SESSION_NOT_FOUND", 404)

            questions = json.loads(row["question_snapshots"])
            answers = json.loads(row["answers"])

            # Enrich answered questions with correct_option & explanation
            q_ids = [q["id"] for q in questions]
            q_info = {}
            if q_ids:
                placeholders = ",".join("?" * len(q_ids))
                q_rows = db.execute(f"SELECT id, correct_option, explanation FROM questions WHERE id IN ({placeholders})", q_ids).fetchall()
                q_info = {r["id"]: r for r in q_rows}

            enriched_questions = []
            for q in questions:
                item = dict(q)
                ans = answers.get(q["id"])
                if ans:
                    info = q_info.get(q["id"])
                    item["userAnswer"] = ans.get("selectedOption")
                    item["isCorrect"] = ans.get("isCorrect")
                    if info:
                        item["correctOption"] = info["correct_option"]
                        item["explanation"] = info["explanation"]
                enriched_questions.append(item)

            return {
                "id": row["id"],
                "quizId": row["quiz_id"],
                "subjectId": row["subject_id"],
                "grade": row["grade"],
                "step": row["step"],
                "state": row["state"],
                "score": row["score"],
                "correctCount": row["correct_count"],
                "totalCount": row["total_count"],
                "questions": enriched_questions,
                "answers": answers,
                "createdAt": iso(row["created_at"]),
                "updatedAt": iso(row["updated_at"])
            }

    def submit_answer(self, user_id, session_id, question_id, selected_option):
        with self.db.transaction() as db:
            session = db.execute("SELECT * FROM practice_sessions WHERE id=? AND user_id=?", (session_id, user_id)).fetchone()
            if not session:
                error("SESSION_NOT_FOUND", 404)
            if session["state"] == "COMPLETED":
                error("SESSION_ALREADY_COMPLETED", 409)

            answers = json.loads(session["answers"])
            # Idempotent check
            if question_id in answers:
                return self.get_session(user_id, session_id)

            q_row = db.execute("SELECT * FROM questions WHERE id=?", (question_id,)).fetchone()
            if not q_row:
                error("QUESTION_NOT_FOUND", 404)

            correct = (selected_option.upper().strip() == q_row["correct_option"].upper().strip())
            answers[question_id] = {
                "selectedOption": selected_option.upper().strip(),
                "isCorrect": correct,
                "answeredAt": time.time()
            }

            correct_count = session["correct_count"] + (1 if correct else 0)
            points_added = q_row["points"] if correct else 0
            new_score = session["score"] + points_added
            now = time.time()

            db.execute("""
            UPDATE practice_sessions SET answers=?, score=?, correct_count=?, updated_at=? WHERE id=?
            """, (json.dumps(answers), new_score, correct_count, now, session_id))

            # Record in wrong questions notebook if incorrect
            if not correct:
                db.execute("""
                INSERT OR REPLACE INTO wrong_questions_notebook (id, user_id, question_id, subject_id, grade, selected_option, correct_option, explanation, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (f"wq_{uuid.uuid4().hex[:8]}", user_id, question_id, session["subject_id"], session["grade"],
                      selected_option.upper().strip(), q_row["correct_option"], q_row["explanation"], now))

        return self.get_session(user_id, session_id)

    def complete_session(self, user_id, session_id):
        with self.db.transaction() as db:
            session = db.execute("SELECT * FROM practice_sessions WHERE id=? AND user_id=?", (session_id, user_id)).fetchone()
            if not session:
                error("SESSION_NOT_FOUND", 404)
            now = time.time()
            db.execute("UPDATE practice_sessions SET state='COMPLETED', updated_at=? WHERE id=?", (now, session_id))
        return self.get_session(user_id, session_id)

    def get_wrong_questions(self, user_id, subject_id=None, grade=None):
        query = "SELECT w.*, q.prompt, q.option_a, q.option_b, q.option_c, q.option_d, q.chapter, q.topic FROM wrong_questions_notebook w JOIN questions q ON w.question_id=q.id WHERE w.user_id=?"
        params = [user_id]
        if subject_id:
            query += " AND w.subject_id=?"
            params.append(subject_id)
        if grade:
            query += " AND w.grade=?"
            params.append(grade)
        query += " ORDER BY w.created_at DESC"

        with self.db.read() as db:
            rows = db.execute(query, params).fetchall()
            items = []
            for r in rows:
                items.append({
                    "id": r["id"],
                    "questionId": r["question_id"],
                    "subjectId": r["subject_id"],
                    "grade": r["grade"],
                    "chapter": r["chapter"],
                    "topic": r["topic"],
                    "prompt": r["prompt"],
                    "options": [
                        {"id": "A", "text": r["option_a"]},
                        {"id": "B", "text": r["option_b"]},
                        {"id": "C", "text": r["option_c"]},
                        {"id": "D", "text": r["option_d"]}
                    ],
                    "selectedOption": r["selected_option"],
                    "correctOption": r["correct_option"],
                    "explanation": r["explanation"],
                    "createdAt": iso(r["created_at"])
                })
            return items

    # --- ADMIN FEATURES ---
    def list_questions_admin(self, subject_id=None, grade=None, status=None):
        query = "SELECT * FROM questions WHERE 1=1"
        params = []
        if subject_id:
            query += " AND subject_id=?"
            params.append(subject_id)
        if grade:
            query += " AND grade=?"
            params.append(grade)
        if status:
            query += " AND status=?"
            params.append(status)
        query += " ORDER BY updated_at DESC"

        with self.db.read() as db:
            rows = db.execute(query, params).fetchall()
            items = []
            for r in rows:
                items.append({
                    "id": r["id"],
                    "code": r["code"],
                    "version": r["version"],
                    "subjectId": r["subject_id"],
                    "grade": r["grade"],
                    "chapter": r["chapter"],
                    "topic": r["topic"],
                    "textbook": r["textbook"],
                    "prompt": r["prompt"],
                    "optionA": r["option_a"],
                    "optionB": r["option_b"],
                    "optionC": r["option_c"],
                    "optionD": r["option_d"],
                    "correctOption": r["correct_option"],
                    "explanation": r["explanation"],
                    "difficulty": r["difficulty"],
                    "points": r["points"],
                    "imageUrl": r["image_url"],
                    "source": r["source"],
                    "author": r["author"],
                    "license": r["license"],
                    "status": r["status"],
                    "createdAt": iso(r["created_at"]),
                    "updatedAt": iso(r["updated_at"])
                })
            return items

    def create_or_update_question_admin(self, admin_id, data):
        now = time.time()
        qid = data.get("id") or f"q_{uuid.uuid4().hex[:12]}"
        code = data.get("code") or f"Q_{uuid.uuid4().hex[:8].upper()}"

        with self.db.transaction() as db:
            existing = db.execute("SELECT * FROM questions WHERE id=?", (qid,)).fetchone()
            version = (existing["version"] + 1) if existing else 1

            db.execute("""
            INSERT OR REPLACE INTO questions
            (id, code, version, subject_id, grade, chapter, topic, textbook, prompt, option_a, option_b, option_c, option_d, correct_option, explanation, difficulty, points, image_url, source, author, license, status, created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (qid, code, version, data.get("subjectId", "physics"), data.get("grade", 10),
                  data.get("chapter", "Tổng hợp"), data.get("topic", "Chủ đề chung"), data.get("textbook"),
                  data.get("prompt", ""), data.get("optionA", ""), data.get("optionB", ""),
                  data.get("optionC", ""), data.get("optionD", ""), data.get("correctOption", "A").upper(),
                  data.get("explanation", ""), data.get("difficulty", "MEDIUM"), data.get("points", 10),
                  data.get("imageUrl"), data.get("source", "Admin Import"), data.get("author", "Quản trị viên"),
                  data.get("license", "Nội bộ"), data.get("status", "DRAFT"), admin_id,
                  existing["created_at"] if existing else now, now))
        return self.get_question_by_id(qid)

    def get_question_by_id(self, qid):
        with self.db.read() as db:
            r = db.execute("SELECT * FROM questions WHERE id=?", (qid,)).fetchone()
            if not r:
                error("QUESTION_NOT_FOUND", 404)
            return {
                "id": r["id"],
                "code": r["code"],
                "version": r["version"],
                "subjectId": r["subject_id"],
                "grade": r["grade"],
                "chapter": r["chapter"],
                "topic": r["topic"],
                "textbook": r["textbook"],
                "prompt": r["prompt"],
                "optionA": r["option_a"],
                "optionB": r["option_b"],
                "optionC": r["option_c"],
                "optionD": r["option_d"],
                "correctOption": r["correct_option"],
                "explanation": r["explanation"],
                "difficulty": r["difficulty"],
                "points": r["points"],
                "imageUrl": r["image_url"],
                "source": r["source"],
                "author": r["author"],
                "license": r["license"],
                "status": r["status"],
                "createdAt": iso(r["created_at"]),
                "updatedAt": iso(r["updated_at"])
            }

    def publish_question_admin(self, qid, publish=True):
        status = "PUBLISHED" if publish else "DRAFT"
        now = time.time()
        with self.db.transaction() as db:
            db.execute("UPDATE questions SET status=?, updated_at=? WHERE id=?", (status, now, qid))
        return self.get_question_by_id(qid)

    def generate_excel_template(self):
        content = "Mã câu,Môn (wireId),Lớp (10-12),Chương,Chủ đề,Bộ sách,Câu hỏi,Đáp án A,Đáp án B,Đáp án C,Đáp án D,Đáp án đúng (A/B/C/D),Lời giải,Độ khó (EASY/MEDIUM/HARD),Điểm,Nguồn,Tác giả,Quyền\nPHY10_01,physics,10,Chương 1: Động học,Chuyển động thẳng đều,Kết nối tri thức,Phương trình chuyển động x = 5 + 6t (m;s). Vận tốc là:,5 m/s,6 m/s,-5 m/s,11 m/s,B,So sánh với x = x0 + vt có v = 6 m/s,EASY,10,Bộ đề mẫu,Giáo viên biên soạn,Nội bộ\n"
        return content.encode("utf-8")

    def parse_import_file(self, content_bytes):
        # Parses CSV / UTF-8 text import format with detailed line/column validation reports
        try:
            text = content_bytes.decode("utf-8-sig", errors="replace")
        except Exception:
            error("INVALID_FILE_ENCODING", 400)

        reader = csv.reader(text.splitlines())
        rows = list(reader)
        if not rows:
            return {"totalRows": 0, "validRows": [], "errors": [{"row": 0, "message": "File rỗng"}]}

        header = rows[0]
        valid_rows = []
        errors = []

        for idx, row in enumerate(rows[1:], start=2):
            if not row or not any(field.strip() for field in row):
                continue # Skip blank lines

            if len(row) < 13:
                errors.append({"row": idx, "message": f"Dòng {idx}: Số lượng cột không đủ ({len(row)}/13 cột tối thiểu)"})
                continue

            code = row[0].strip() if len(row) > 0 else ""
            subject_id = row[1].strip().lower() if len(row) > 1 else "physics"
            try:
                grade = int(row[2].strip()) if len(row) > 2 and row[2].strip().isdigit() else 10
            except ValueError:
                grade = 10

            chapter = row[3].strip() if len(row) > 3 else "Tổng hợp"
            topic = row[4].strip() if len(row) > 4 else "Chủ đề chung"
            textbook = row[5].strip() if len(row) > 5 else "Chưa xác định"
            prompt = row[6].strip() if len(row) > 6 else ""
            option_a = row[7].strip() if len(row) > 7 else ""
            option_b = row[8].strip() if len(row) > 8 else ""
            option_c = row[9].strip() if len(row) > 9 else ""
            option_d = row[10].strip() if len(row) > 10 else ""
            correct_option = row[11].strip().upper() if len(row) > 11 else ""
            explanation = row[12].strip() if len(row) > 12 else ""
            difficulty = row[13].strip().upper() if len(row) > 13 and row[13].strip() in ("EASY", "MEDIUM", "HARD") else "MEDIUM"

            try:
                points = int(row[14].strip()) if len(row) > 14 and row[14].strip().isdigit() else 10
            except ValueError:
                points = 10

            source = row[15].strip() if len(row) > 15 else "Giáo viên upload"
            author = row[16].strip() if len(row) > 16 else "Người biên soạn"
            license_str = row[17].strip() if len(row) > 17 else "Nội bộ"

            # Validations
            if not prompt:
                errors.append({"row": idx, "message": f"Dòng {idx}: Nội dung câu hỏi không được để trống"})
                continue
            if not option_a or not option_b or not option_c or not option_d:
                errors.append({"row": idx, "message": f"Dòng {idx}: Đủ 4 phương án A, B, C, D không được để trống"})
                continue
            if correct_option not in ("A", "B", "C", "D"):
                errors.append({"row": idx, "message": f"Dòng {idx}: Đáp án đúng phải là A, B, C hoặc D (nhận được: '{correct_option}')"})
                continue

            valid_rows.append({
                "code": code or f"Q_IMP_{uuid.uuid4().hex[:6].upper()}",
                "subjectId": subject_id,
                "grade": grade,
                "chapter": chapter,
                "topic": topic,
                "textbook": textbook,
                "prompt": prompt,
                "optionA": option_a,
                "optionB": option_b,
                "optionC": option_c,
                "optionD": option_d,
                "correctOption": correct_option,
                "explanation": explanation,
                "difficulty": difficulty,
                "points": points,
                "source": source,
                "author": author,
                "license": license_str,
                "status": "DRAFT"
            })

        return {
            "totalRows": len(rows) - 1,
            "validCount": len(valid_rows),
            "errorCount": len(errors),
            "validRows": valid_rows,
            "errors": errors
        }

    def confirm_import(self, admin_id, valid_rows, publish=False):
        status = "PUBLISHED" if publish else "DRAFT"
        imported = []
        for item in valid_rows:
            item["status"] = status
            q = self.create_or_update_question_admin(admin_id, item)
            imported.append(q)
        return {"importedCount": len(imported), "questions": imported}
