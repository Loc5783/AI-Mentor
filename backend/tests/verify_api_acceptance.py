import sys
import tempfile
import time
import subprocess
import json

# API integration only: no browser/device and no listening network port.
from fastapi.testclient import TestClient
from aimentor.main import create_app
from aimentor.config import Settings
from aimentor.ai_practice import FakeAiProvider
from pathlib import Path
CLIENT = None

def api_call(method, path, headers=None, data=None):
    assert CLIENT is not None
    kwargs = {"headers": headers or {}}
    if isinstance(data, (dict, list)):
        kwargs["json"] = data
    elif data is not None:
        kwargs["content"] = data
    resp = CLIENT.request(method, path, **kwargs)
    if "application/json" in resp.headers.get("content-type", ""):
        return resp.status_code, resp.json()
    return resp.status_code, resp.content

def run_tests():
    global CLIENT
    temporary = tempfile.TemporaryDirectory(prefix="mentor_api_acceptance_")
    temp_dir = temporary.name
    settings = Settings(data_dir=Path(temp_dir))
    app = create_app(settings)
    app.state.ai_practice.set_provider(FakeAiProvider(demo_mode=True))
    CLIENT = TestClient(app)
    CLIENT.__enter__()
    results = []
    
    try:
        # ----------------------------------------------------------------------
        # SCENARIO 1: Auth & Isolation Setup
        # ----------------------------------------------------------------------
        print("\n--- Running Scenario 1: Auth & Local Admin Elevation ---")
        # Register Admin candidate
        s, r_admin = api_call("POST", "/auth/sign-up", data={
            "displayName": "Admin Test",
            "email": "admintest_oct29@school.test",
            "password": "Password123!",
            "acceptedTermsVersion": "2026-10"
        })
        assert s == 201, f"Admin sign-up failed: {s} {r_admin}"
        admin_token = r_admin["accessToken"]
        admin_id = int(r_admin["userId"])
        
        # Register Student
        s, r_student = api_call("POST", "/auth/sign-up", data={
            "displayName": "Student Test",
            "email": "studenttest_oct29@school.test",
            "password": "Password123!",
            "acceptedTermsVersion": "2026-10"
        })
        assert s == 201, f"Student sign-up failed: {s} {r_student}"
        student_token = r_student["accessToken"]
        student_id = int(r_student["userId"])
        
        # Verify student CANNOT access /admin/questions
        s, _ = api_call("GET", "/admin/questions", headers={"Authorization": f"Bearer {student_token}"})
        assert s == 403, f"Expected 403 for student on admin endpoint, got {s}"
        
        # Use the actual role CLI against only this isolated database.
        subprocess.run([sys.executable, "-m", "aimentor.manage_roles",
            "--db-path", str(Path(temp_dir) / "aimentor.sqlite3"),
            "--user", "admintest_oct29@school.test", "--role", "ADMIN", "--apply"],
            check=True, capture_output=True, timeout=20,
            cwd=str(Path(__file__).resolve().parents[1]))
        s, login = api_call("POST", "/auth/sign-in", data={
            "identifier": "admintest_oct29@school.test", "password": "Password123!"})
        assert s == 200
        admin_token = login["accessToken"]

        # Verify admin now has access
        s, q_list = api_call("GET", "/admin/questions", headers={"Authorization": f"Bearer {admin_token}"})
        assert s == 200, f"Expected 200 for elevated admin, got {s} {q_list}"
        results.append(("Scenario 1: Local Admin Elevation & Authorization Isolation", "PASS"))
        print("  [PASS] Student rejected with 403, elevated Admin accepted with 200")

        # ----------------------------------------------------------------------
        # SCENARIO 2: Web Admin -> Import Preview/Confirm -> Library -> Student Practice
        # ----------------------------------------------------------------------
        print("\n--- Running Scenario 2: Web Admin -> Import Preview/Confirm -> Library ---")
        # 2.1 Download CSV template
        s, tpl_csv = api_call("GET", "/admin/template/excel?format=csv")
        assert s == 200
        tpl_text = tpl_csv.decode("utf-8-sig")
        assert "Mã câu" in tpl_text
        print("  [PASS] Downloaded CSV template with expected headers ('Mã câu')")

        # 2.2 Upload CSV with 2 valid rows and 1 invalid row
        batch_id = f"OCT29_TEST_{int(time.time())}"
        test_csv_content = f"""Mã câu,Môn (wireId),Lớp (10-12),Chương,Chủ đề,Bộ sách,Câu hỏi,Đáp án A,Đáp án B,Đáp án C,Đáp án D,Đáp án đúng (A/B/C/D),Lời giải,Độ khó (EASY/MEDIUM/HARD),Điểm,Nguồn,Tác giả,Quyền
{batch_id}_Q1,physics,10,Chương 1: Động học,Chuyển động thẳng đều,Kết nối tri thức,Vật rơi tự do có gia tốc xấp xỉ bằng bao nhiêu?,9.8 m/s2,5 m/s2,0 m/s2,20 m/s2,A,Gia tốc g xấp xỉ 9.8 m/s2,EASY,10,Nội bộ,GV,Mở
{batch_id}_Q2,physics,10,Chương 1: Động học,Chuyển động thẳng đều,Kết nối tri thức,Đơn vị của vận tốc trong hệ SI là gì?,m/s,km/h,m/s2,cm/s,A,Đơn vị chuẩn SI là m/s,MEDIUM,10,Nội bộ,GV,Mở
{batch_id}_QERR,physics,10,Chương 1: Động học,Chuyển động thẳng đều,Kết nối tri thức,Câu hỏi cố ý bị lỗi đáp án đúng?,A1,B1,C1,D1,INVALID_OPT,Giải thích lỗi,EASY,10,Nội bộ,GV,Mở
"""
        s, preview_res = api_call("POST", "/admin/import/preview", headers={
            "Authorization": f"Bearer {admin_token}",
            "Content-Type": "text/csv; charset=utf-8"
        }, data=test_csv_content.encode("utf-8-sig"))
        assert s == 200, f"Preview failed: {s} {preview_res}"
        assert preview_res["validCount"] == 2, f"Expected 2 valid rows, got {preview_res['validCount']}"
        assert preview_res["errorCount"] == 1, f"Expected 1 error row, got {preview_res['errorCount']}"
        assert len(preview_res["errors"]) == 1
        print(f"  [PASS] Preview detected 2 valid rows, 1 error row: {preview_res['errors'][0]['message']}")

        # 2.3 Import valid rows as DRAFT (publish=False)
        valid_rows = preview_res["validRows"]
        import_key = f"import_key_{batch_id}"
        s, confirm_res = api_call("POST", "/admin/import/confirm", headers={
            "Authorization": f"Bearer {admin_token}",
            "Idempotency-Key": import_key
        }, data={
            "validRows": valid_rows,
            "publish": False,
            "importKey": import_key
        })
        assert s == 200 and confirm_res["importedCount"] == 2
        q_ids = [q["id"] for q in confirm_res["questions"]]
        print(f"  [PASS] Confirmed import as DRAFT (publish=False), {len(q_ids)} questions created")

        # 2.4 No quiz has been published yet. This is only a listing check;
        # the following publication integrity assertion tests DRAFT rejection.
        s, student_quizzes = api_call("GET", "/library/quizzes?subject_id=physics&grade=10", headers={
            "Authorization": f"Bearer {student_token}"
        })
        assert s == 200
        for q in student_quizzes:
            assert batch_id not in q.get("title", "")
        print("  [PASS] Unpublished batch absent from student quiz listing")

        # 2.5a Attempting to publish quiz with DRAFT questions is rejected (integrity check)
        s, bad_quiz = api_call("POST", "/admin/quizzes", headers={
            "Authorization": f"Bearer {admin_token}"
        }, data={
            "title": f"Bài ôn tập {batch_id}",
            "subjectId": "physics",
            "grade": 10,
            "status": "PUBLISHED",
            "questionIds": q_ids
        })
        assert s == 400 and "DRAFT" in str(bad_quiz)
        print("  [PASS] Publishing quiz with DRAFT questions correctly rejected by system policy")

        # 2.5b Publish each question via product flow: POST /admin/questions/{qid}/publish
        for qid in q_ids:
            s_pub, pub_res = api_call("POST", f"/admin/questions/{qid}/publish?publish=true", headers={
                "Authorization": f"Bearer {admin_token}"
            })
            assert s_pub == 200 and pub_res["status"] == "PUBLISHED"
        print(f"  [PASS] Published {len(q_ids)} questions via product flow /admin/questions/{{qid}}/publish")

        # 2.5c Admin now creates and publishes the Quiz successfully
        s, quiz_res = api_call("POST", "/admin/quizzes", headers={
            "Authorization": f"Bearer {admin_token}"
        }, data={
            "title": f"Bài ôn tập {batch_id}",
            "subjectId": "physics",
            "grade": 10,
            "status": "PUBLISHED",
            "questionIds": q_ids
        })
        assert s == 201, f"Quiz creation failed: {s} {quiz_res}"
        quiz_id = quiz_res["id"]
        print(f"  [PASS] Admin created and published quiz: {quiz_id}")

        # 2.6 Student sees published quiz
        s, student_quizzes_after = api_call("GET", "/library/quizzes?subject_id=physics&grade=10", headers={
            "Authorization": f"Bearer {student_token}"
        })
        assert s == 200
        found_quiz = next((q for q in student_quizzes_after if q["id"] == quiz_id), None)
        assert found_quiz is not None, "Student could not find published quiz!"
        print(f"  [PASS] Student found published quiz: {found_quiz['title']}")

        s, before_rewards = api_call("GET", "/me/dashboard?grade=10", headers={"Authorization": f"Bearer {student_token}"})
        assert s == 200
        reward_fields = ("dailyXp", "totalXp", "streakDays")
        baseline = {field: before_rewards[field] for field in reward_fields}

        # 2.7 Student starts session, answers Q1 correctly, Q2 incorrectly
        s, sess_res = api_call("POST", "/library/sessions", headers={
            "Authorization": f"Bearer {student_token}"
        }, data={"quizId": quiz_id})
        assert s == 201
        sess_id = sess_res["id"]

        # Submit Q1 correct ('A')
        s, ans1 = api_call("POST", f"/library/sessions/{sess_id}/answers", headers={
            "Authorization": f"Bearer {student_token}"
        }, data={"questionId": q_ids[0], "selectedOption": "A"})
        assert s == 200 and ans1["answers"][q_ids[0]]["isCorrect"] is True

        # Submit Q2 incorrect ('B')
        s, ans2 = api_call("POST", f"/library/sessions/{sess_id}/answers", headers={
            "Authorization": f"Bearer {student_token}"
        }, data={"questionId": q_ids[1], "selectedOption": "B"})
        assert s == 200 and ans2["answers"][q_ids[1]]["isCorrect"] is False

        # Complete session
        s, comp_res = api_call("POST", f"/library/sessions/{sess_id}/complete", headers={
            "Authorization": f"Bearer {student_token}"
        })
        assert s == 200
        print(f"  [PASS] Student completed library practice session: score={comp_res['score']}/{comp_res['totalCount']}")

        # 2.8 Verify wrong question in /me/wrong-questions
        s, notebook = api_call("GET", "/me/wrong-questions", headers={
            "Authorization": f"Bearer {student_token}"
        })
        assert s == 200
        wrong_entry = next((item for item in notebook if item["questionId"] == q_ids[1]), None)
        assert wrong_entry is not None, "Wrong question was not recorded in notebook!"
        print(f"  [PASS] Question {q_ids[1]} verified in student's wrong_questions_notebook")

        # 2.9 Verify library practice did not award EXP (policy check)
        s, profile = api_call("GET", "/me/dashboard?grade=10", headers={"Authorization": f"Bearer {student_token}"})
        assert s == 200
        assert {field: profile[field] for field in reward_fields} == baseline
        print("  [PASS] Library dailyXp/totalXp/streakDays match exact pre-session baseline")

        # 2.10 Replay confirm import with same idempotency key
        s, replayed_res = api_call("POST", "/admin/import/confirm", headers={
            "Authorization": f"Bearer {admin_token}",
            "Idempotency-Key": import_key
        }, data={
            "validRows": valid_rows,
            "publish": False,
            "importKey": import_key
        })
        assert s == 200 and replayed_res["importedCount"] == 2
        replayed_ids = [q["id"] for q in replayed_res["questions"]]
        assert replayed_ids == q_ids
        print("  [PASS] Idempotent confirm replay safely returned identical response without duplication")
        results.append(("Scenario 2: Import/library API integration (not browser UI)", "PASS"))

        # ----------------------------------------------------------------------
        # SCENARIO 3: Exam Prep Flow
        # ----------------------------------------------------------------------
        print("\n--- Running Scenario 3: Exam Prep Flow ---")
        # 3.1 Admin creates Exam with 2 questions
        s, exam_create = api_call("POST", "/admin/exams", headers={
            "Authorization": f"Bearer {admin_token}"
        }, data={
            "title": f"Đề thi Giữa kỳ Vật Lí 10 - {batch_id}",
            "subjectId": "physics",
            "grade": 10,
            "examType": "MIDTERM",
            "semester": 1,
            "durationMinutes": 15,
            "questionIds": q_ids,
            "status": "PUBLISHED"
        })
        assert s == 201, f"Exam creation failed: {s} {exam_create}"
        exam_id = exam_create["id"]
        print(f"  [PASS] Admin created and published Exam: {exam_id}")

        # 3.2 Student filters exams
        s, exam_list = api_call("GET", "/exams?subject_id=physics&grade=10&exam_type=MIDTERM&semester=1", headers={
            "Authorization": f"Bearer {student_token}"
        })
        assert s == 200
        found_exam = next((e for e in exam_list["items"] if e["id"] == exam_id), None)
        assert found_exam is not None, "Student could not find published exam!"
        print(f"  [PASS] Student retrieved filtered exam: {found_exam['title']}")

        # 3.3 Student starts exam session
        s, exam_sess = api_call("POST", "/exams/sessions", headers={
            "Authorization": f"Bearer {student_token}"
        }, data={"examId": exam_id})
        assert s == 201, f"Failed starting exam session: {s} {exam_sess}"
        e_sess_id = exam_sess["id"]

        # 3.4 Student answers Q1 with revision protection
        s, ans_e1 = api_call("POST", f"/exams/sessions/{e_sess_id}/answers", headers={
            "Authorization": f"Bearer {student_token}"
        }, data={"questionId": q_ids[0], "selectedOption": "A", "revision": 1})
        assert s == 200, f"Failed saving exam answer: {s} {ans_e1}"

        # Submit exam
        s, submit_e = api_call("POST", f"/exams/sessions/{e_sess_id}/submit", headers={
            "Authorization": f"Bearer {student_token}"
        })
        assert s == 200 and submit_e["state"] == "SUBMITTED"
        print(f"  [PASS] Student submitted exam: score={submit_e.get('score')}")

        # 3.5 Double submission preserves the completed session timestamp.
        s, submit_re = api_call("POST", f"/exams/sessions/{e_sess_id}/submit", headers={
            "Authorization": f"Bearer {student_token}"
        })
        assert s == 200 and submit_re["state"] == "SUBMITTED"
        assert submit_re["submittedAt"] == submit_e["submittedAt"]
        print("  [PASS] Double submission handled idempotently without re-grading")
        results.append(("Scenario 3: Exam API submit/replay (not device lifecycle)", "PASS"))

        # ----------------------------------------------------------------------
        # SCENARIO 4: Roadmap Flow & Honest Missing Data Handling
        # ----------------------------------------------------------------------
        print("\n--- Running Scenario 4: Roadmap Flow ---")
        # Real roadmap success/reward tests run in test_roadmap_regression.py.
        # No invented /curriculum route or missing-data-only PASS.
        s, enrollment = api_call("POST", "/enrollments", headers={
            "Authorization": f"Bearer {student_token}", "Idempotency-Key": "acceptance-enroll"},
            data={"subjectId": "physics", "grade": 10})
        assert s == 201, f"Published Physics 10 enrollment failed: {s}"
        s, roadmap = api_call("GET", f"/enrollments/{enrollment['id']}/path",
            headers={"Authorization": f"Bearer {student_token}"})
        assert s == 200 and roadmap, "Roadmap readback failed"
        results.append(("Scenario 4: published Physics 10 enrollment/path API", "PASS"))

        # ----------------------------------------------------------------------
        # SCENARIO 5: AI Practice Status (Fake Provider on Test Env)
        # ----------------------------------------------------------------------
        print("\n--- Running Scenario 5: AI Practice Status & Flow ---")
        s, ai_cfg = api_call("GET", "/ai-practice/config")
        assert s == 200 and ai_cfg["configured"] is True and ai_cfg["provider"] == "demo"
        print(f"  [PASS] AI Practice config correctly reports Demo/Fake provider: {ai_cfg['provider']}")

        s, ai_sess = api_call("POST", "/ai-practice/sessions", headers={
            "Authorization": f"Bearer {student_token}"
        }, data={
            "subjectId": "physics",
            "grade": 10,
            "topic": "Động học chất điểm",
            "difficulty": "EASY",
            "questionCount": 2
        })
        assert s == 201
        ai_sess_id = ai_sess["id"]
        assert len(ai_sess["questions"]) == 2
        # Verify answers are hidden initially
        assert "correctOption" not in ai_sess["questions"][0]

        # Student answers question 1
        q1_id = ai_sess["questions"][0]["id"]
        s, ai_ans = api_call("POST", f"/ai-practice/sessions/{ai_sess_id}/answer", headers={
            "Authorization": f"Bearer {student_token}"
        }, data={"questionId": q1_id, "selectedOption": "A"})
        assert s == 200
        assert "correctOption" in ai_ans
        print("  [PASS] AI session answered and feedback revealed accurately")

        # Student skips question 2
        q2_id = ai_sess["questions"][1]["id"]
        s, ai_skip = api_call("POST", f"/ai-practice/sessions/{ai_sess_id}/skip", headers={
            "Authorization": f"Bearer {student_token}"
        }, data={"questionId": q2_id})
        assert s == 200 and ai_skip["skipped"] is True
        print("  [PASS] AI session skipped question accurately")

        # Complete session
        s, ai_comp = api_call("POST", f"/ai-practice/sessions/{ai_sess_id}/complete", headers={
            "Authorization": f"Bearer {student_token}"
        })
        assert s == 200 and ai_comp["state"] == "COMPLETED"
        assert ai_comp["summary"] is not None
        print(f"  [PASS] AI practice session completed: summary={ai_comp['summary']}")

        # Verify skipped question in notebook with source AI_PRACTICE
        s, nb_ai = api_call("GET", "/me/wrong-questions", headers={
            "Authorization": f"Bearer {student_token}"
        })
        assert s == 200
        ai_nb_entry = next((item for item in nb_ai if item["questionId"] == q2_id), None)
        assert ai_nb_entry is not None and ai_nb_entry["source"] == "AI_PRACTICE"
        print("  [PASS] Skipped AI question recorded in notebook with source AI_PRACTICE")
        results.append(("Scenario 5: AI Practice Flow (Fake Provider)", "PASS"))

    finally:
        CLIENT.__exit__(None, None, None)
        CLIENT = None
        temporary.cleanup()

    print("\n=======================================================")
    print("           ISOLATED VERIFICATION SUMMARY               ")
    print("=======================================================")
    for name, st in results:
        print(f"[{st}] {name}")
    print("=======================================================")

if __name__ == "__main__":
    run_tests()
