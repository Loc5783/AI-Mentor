import concurrent.futures
import io
import json
import sqlite3
import time
import pytest
from starlette.testclient import TestClient
from aimentor.main import create_app
from aimentor.config import Settings
from aimentor.database import Database
from aimentor.library import LibraryService

def setup_client(tmp_path):
    data_dir = tmp_path / "data"
    data_dir.mkdir(parents=True, exist_ok=True)
    settings = Settings(data_dir)
    app = create_app(settings)
    client = TestClient(app)

    # 1. Sign up admin
    res = client.post("/auth/sign-up", json={
        "displayName": "Admin Test",
        "email": "admin_test@school.example",
        "password": "Mentor01!2026",
        "acceptedTermsVersion": "2026-10"
    })
    assert res.status_code == 201, res.text
    admin_id = res.json()["userId"]
    admin_token = res.json()["accessToken"]
    admin_headers = {"Authorization": f"Bearer {admin_token}"}

    db_file = data_dir / "aimentor.sqlite3"
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("UPDATE users SET role='ADMIN' WHERE id=?", (admin_id,))
    conn.commit()

    # 2. Sign up student
    res = client.post("/auth/sign-up", json={
        "displayName": "Student Test",
        "email": "student_test@school.example",
        "password": "Mentor01!2026",
        "acceptedTermsVersion": "2026-10"
    })
    assert res.status_code == 201, res.text
    student_token = res.json()["accessToken"]
    student_headers = {"Authorization": f"Bearer {student_token}"}

    # 3. Sign up second admin
    res = client.post("/auth/sign-up", json={
        "displayName": "Admin Two",
        "email": "admin_two@school.example",
        "password": "Mentor01!2026",
        "acceptedTermsVersion": "2026-10"
    })
    assert res.status_code == 201, res.text
    admin_two_id = res.json()["userId"]
    admin_two_token = res.json()["accessToken"]
    admin_two_headers = {"Authorization": f"Bearer {admin_two_token}"}
    cur.execute("UPDATE users SET role='ADMIN' WHERE id=?", (admin_two_id,))
    conn.commit()
    conn.close()

    return client, admin_headers, student_headers, admin_two_headers, db_file, settings


# =========================================================================
# ISSUE 1 TESTS: CHỐNG IMPORT LẶP, KỂ CẢ KHI KHÔNG CÓ MÃ CÂU HỎI
# =========================================================================

def test_issue1_A_same_operation_without_code_sent_twice_creates_single_batch(tmp_path):
    client, admin_h, student_h, _, db_file, _ = setup_client(tmp_path)

    # Payload with question having NO code
    payload = {
        "validRows": [
            {
                "subjectId": "physics",
                "grade": 10,
                "prompt": "Câu hỏi không có mã định danh số 1?",
                "optionA": "Đáp án A",
                "optionB": "Đáp án B",
                "optionC": "Đáp án C",
                "optionD": "Đáp án D",
                "correctOption": "A",
                "points": 10
            }
        ],
        "publish": False
    }

    idem_key = "op_batch_no_code_001"
    headers_with_key = {**admin_h, "Idempotency-Key": idem_key}

    # First request
    res1 = client.post("/admin/import/confirm", headers=headers_with_key, json=payload)
    assert res1.status_code == 200
    data1 = res1.json()
    assert data1["importedCount"] == 1
    q1_id = data1["questions"][0]["id"]
    q1_code = data1["questions"][0]["code"]

    # Second request (Retry of same operation)
    res2 = client.post("/admin/import/confirm", headers=headers_with_key, json=payload)
    assert res2.status_code == 200
    data2 = res2.json()
    assert data2["importedCount"] == 1
    assert data2["questions"][0]["id"] == q1_id
    assert data2["questions"][0]["code"] == q1_code

    # CRITICAL CHECK: In DB, there must be EXACTLY 1 question, not 2!
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT count(*) FROM questions WHERE prompt LIKE '%Câu hỏi không có mã định danh số 1?%'")
    count = cur.fetchone()[0]
    conn.close()
    assert count == 1, f"Expected exactly 1 question in DB, but found {count} (duplicate import bug!)"


def test_issue1_B_concurrent_same_operation_creates_only_single_batch(tmp_path):
    client, admin_h, student_h, _, db_file, settings = setup_client(tmp_path)

    payload = {
        "importKey": "concurrent_key_001",
        "validRows": [
            {
                "subjectId": "chemistry",
                "grade": 11,
                "prompt": "Câu hỏi đồng thời không có mã?",
                "optionA": "Opt A", "optionB": "Opt B", "optionC": "Opt C", "optionD": "Opt D",
                "correctOption": "B",
                "points": 10
            }
        ],
        "publish": False
    }

    def send_request():
        # New client instance sharing the same database
        c = TestClient(create_app(settings))
        return c.post("/admin/import/confirm", headers=admin_h, json=payload)

    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        f1 = executor.submit(send_request)
        f2 = executor.submit(send_request)
        res1 = f1.result()
        res2 = f2.result()

    assert res1.status_code == 200
    assert res2.status_code == 200
    assert res1.json()["importedCount"] == 1
    assert res2.json()["importedCount"] == 1
    # Both receive identical result
    assert res1.json()["questions"][0]["id"] == res2.json()["questions"][0]["id"]

    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT count(*) FROM questions WHERE prompt LIKE '%Câu hỏi đồng thời không có mã?%'")
    assert cur.fetchone()[0] == 1
    conn.close()


def test_issue1_C_retry_after_network_loss_returns_cached_result(tmp_path):
    client, admin_h, _, _, db_file, _ = setup_client(tmp_path)

    payload = {
        "validRows": [
            {
                "subjectId": "math",
                "grade": 12,
                "prompt": "Câu hỏi toán mất kết nối mạng?",
                "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
                "correctOption": "C",
                "points": 10
            }
        ],
        "publish": True
    }
    idem_key = "op_retry_network_loss_001"
    headers = {**admin_h, "Idempotency-Key": idem_key}

    # 1. Request succeeds on server
    res1 = client.post("/admin/import/confirm", headers=headers, json=payload)
    assert res1.status_code == 200
    first_res = res1.json()

    # 2. Client retries with same key
    res_retry = client.post("/admin/import/confirm", headers=headers, json=payload)
    assert res_retry.status_code == 200
    assert res_retry.json() == first_res

    # Only 1 question inserted
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT count(*) FROM questions WHERE prompt LIKE '%Câu hỏi toán mất kết nối mạng?%'")
    assert cur.fetchone()[0] == 1
    conn.close()


def test_issue1_D_same_key_different_payload_or_publish_rejected_with_409(tmp_path):
    client, admin_h, _, _, db_file, _ = setup_client(tmp_path)

    idem_key = "op_conflict_key_001"
    headers = {**admin_h, "Idempotency-Key": idem_key}

    payload_a = {
        "validRows": [
            {
                "subjectId": "physics", "grade": 10,
                "prompt": "Nội dung ban đầu A",
                "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
                "correctOption": "A"
            }
        ],
        "publish": False
    }

    res1 = client.post("/admin/import/confirm", headers=headers, json=payload_a)
    assert res1.status_code == 200

    # Same key, but changed publish to True
    payload_diff_publish = {**payload_a, "publish": True}
    res_conflict1 = client.post("/admin/import/confirm", headers=headers, json=payload_diff_publish)
    assert res_conflict1.status_code == 409
    assert res_conflict1.json()["code"] == "IDEMPOTENCY_CONFLICT"

    # Same key, but changed questions content
    payload_diff_rows = {
        "validRows": [
            {
                "subjectId": "physics", "grade": 10,
                "prompt": "Nội dung hoàn toàn khác B",
                "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
                "correctOption": "A"
            }
        ],
        "publish": False
    }
    res_conflict2 = client.post("/admin/import/confirm", headers=headers, json=payload_diff_rows)
    assert res_conflict2.status_code == 409
    assert res_conflict2.json()["code"] == "IDEMPOTENCY_CONFLICT"

    # Ensure "Nội dung hoàn toàn khác B" was NOT inserted
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT count(*) FROM questions WHERE prompt LIKE '%Nội dung hoàn toàn khác B%'")
    assert cur.fetchone()[0] == 0
    conn.close()


def test_issue1_E_different_account_cannot_receive_other_users_cached_result(tmp_path):
    client, admin1_h, _, admin2_h, db_file, _ = setup_client(tmp_path)

    shared_key = "op_shared_key_name"
    payload = {
        "validRows": [
            {
                "subjectId": "biology", "grade": 10,
                "prompt": "Câu hỏi sinh học tài khoản 1?",
                "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
                "correctOption": "D"
            }
        ],
        "publish": False
    }

    # Admin 1 imports
    res1 = client.post("/admin/import/confirm", headers={**admin1_h, "Idempotency-Key": shared_key}, json=payload)
    assert res1.status_code == 200
    q1_id = res1.json()["questions"][0]["id"]

    # Admin 2 sends with same key name: Admin 2 must execute their own batch, NOT steal Admin 1's cached response!
    payload_admin2 = {
        "validRows": [
            {
                "subjectId": "biology", "grade": 10,
                "prompt": "Câu hỏi sinh học tài khoản 2?",
                "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
                "correctOption": "D"
            }
        ],
        "publish": False
    }
    res2 = client.post("/admin/import/confirm", headers={**admin2_h, "Idempotency-Key": shared_key}, json=payload_admin2)
    assert res2.status_code == 200
    q2_id = res2.json()["questions"][0]["id"]
    assert q1_id != q2_id, "Admin 2 must not receive Admin 1's result!"


def test_issue1_F_batch_failure_rolls_back_idempotency_record(tmp_path):
    client, admin_h, _, _, db_file, _ = setup_client(tmp_path)

    idem_key = "op_failing_batch_001"
    # Row 1 is valid, row 2 is invalid
    payload = {
        "validRows": [
            {
                "subjectId": "physics", "grade": 10,
                "prompt": "Câu 1 hợp lệ",
                "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
                "correctOption": "A"
            },
            {
                "subjectId": "physics", "grade": 10,
                "prompt": "Câu 2 lỗi đáp án",
                "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
                "correctOption": "INVALID"  # Triggers validation failure
            }
        ],
        "publish": False
    }

    res = client.post("/admin/import/confirm", headers={**admin_h, "Idempotency-Key": idem_key}, json=payload)
    assert res.status_code in (400, 422)

    # Check that idempotency table does NOT store false success
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT count(*) FROM idempotency WHERE key=?", (idem_key,))
    assert cur.fetchone()[0] == 0
    cur.execute("SELECT count(*) FROM questions WHERE prompt='Câu 1 hợp lệ'")
    assert cur.fetchone()[0] == 0
    conn.close()


def test_issue1_G_idempotency_persists_across_backend_restart(tmp_path):
    client, admin_h, _, _, db_file, settings = setup_client(tmp_path)

    idem_key = "op_restart_persist_001"
    payload = {
        "validRows": [
            {
                "subjectId": "history", "grade": 11,
                "prompt": "Câu hỏi lịch sử tồn tại qua restart?",
                "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
                "correctOption": "A"
            }
        ],
        "publish": False
    }

    # 1. Post to first instance
    res1 = client.post("/admin/import/confirm", headers={**admin_h, "Idempotency-Key": idem_key}, json=payload)
    assert res1.status_code == 200
    first_data = res1.json()

    # 2. Simulate complete restart of backend service
    new_app = create_app(settings)
    new_client = TestClient(new_app)

    # 3. Post to restarted service
    res_after_restart = new_client.post("/admin/import/confirm", headers={**admin_h, "Idempotency-Key": idem_key}, json=payload)
    assert res_after_restart.status_code == 200
    assert res_after_restart.json() == first_data

    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT count(*) FROM questions WHERE prompt LIKE '%Câu hỏi lịch sử tồn tại qua restart?%'")
    assert cur.fetchone()[0] == 1
    conn.close()


def test_issue1_legacy_client_without_key_safe_compatibility(tmp_path):
    client, admin_h, _, _, db_file, _ = setup_client(tmp_path)

    payload = {
        "validRows": [
            {
                "subjectId": "geography", "grade": 10,
                "prompt": "Câu hỏi địa lý client cũ không gửi key?",
                "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
                "correctOption": "B"
            }
        ],
        "publish": False
    }

    # Request 1 (no key header, no importKey in body)
    res1 = client.post("/admin/import/confirm", headers=admin_h, json=payload)
    assert res1.status_code == 200
    q1_id = res1.json()["questions"][0]["id"]

    # Request 2 (retry by old client)
    res2 = client.post("/admin/import/confirm", headers=admin_h, json=payload)
    assert res2.status_code == 200
    assert res2.json()["questions"][0]["id"] == q1_id

    # Must NOT create duplicate question in database!
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT count(*) FROM questions WHERE prompt LIKE '%Câu hỏi địa lý client cũ không gửi key?%'")
    assert cur.fetchone()[0] == 1
    conn.close()


# =========================================================================
# ISSUE 2 TESTS: GIAO DIỆN & BACKEND CHỈNH SỬA CÂU HỎI
# =========================================================================

def test_issue2_A_open_edit_save_changes_updates_data_and_increments_version(tmp_path):
    client, admin_h, _, _, db_file, _ = setup_client(tmp_path)

    # 1. Create initial question
    res_create = client.post("/admin/questions", headers=admin_h, json={
        "code": "PHY10_EDIT_01",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1",
        "topic": "Chủ đề 1",
        "textbook": "KNTT",
        "prompt": "Nội dung ban đầu của câu hỏi?",
        "optionA": "A1", "optionB": "B1", "optionC": "C1", "optionD": "D1",
        "correctOption": "A",
        "explanation": "Lời giải gốc",
        "difficulty": "EASY",
        "points": 10,
        "source": "Sách GK",
        "author": "Tác giả gốc",
        "license": "Bản quyền gốc"
    })
    assert res_create.status_code == 200
    q_created = res_create.json()
    qid = q_created["id"]
    assert q_created["version"] == 1

    # 2. GET question by ID
    res_get = client.get(f"/admin/questions/{qid}", headers=admin_h)
    assert res_get.status_code == 200
    q_fetched = res_get.json()
    assert q_fetched["prompt"] == "Nội dung ban đầu của câu hỏi?"
    assert q_fetched["author"] == "Tác giả gốc"

    # 3. Edit and save valid update with expectedVersion=1
    res_edit = client.post("/admin/questions", headers=admin_h, json={
        "id": qid,
        "expectedVersion": 1,
        "code": "PHY10_EDIT_01",
        "subjectId": "physics",
        "grade": 10,
        "chapter": "Chương 1 - Nâng cao",
        "topic": "Chuyển động biến đổi đều",
        "textbook": "Cánh diều",
        "prompt": "Nội dung ĐÃ ĐƯỢC CHỈNH SỬA?",
        "optionA": "A_mới", "optionB": "B_mới", "optionC": "C_mới", "optionD": "D_mới",
        "correctOption": "C",
        "explanation": "Lời giải chi tiết mới",
        "difficulty": "HARD",
        "points": 20,
        "source": "Đề thi thử 2026",
        "author": "Thầy Giáo Mới",
        "license": "CC-BY"
    })
    assert res_edit.status_code == 200
    q_updated = res_edit.json()
    assert q_updated["version"] == 2
    assert q_updated["prompt"] == "Nội dung ĐÃ ĐƯỢC CHỈNH SỬA?"
    assert q_updated["correctOption"] == "C"
    assert q_updated["difficulty"] == "HARD"
    assert q_updated["points"] == 20
    assert q_updated["author"] == "Thầy Giáo Mới"

    # 4. Verify in DB
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT version, prompt, correct_option, difficulty, points FROM questions WHERE id=?", (qid,))
    row = cur.fetchone()
    conn.close()
    assert row[0] == 2
    assert row[1] == "Nội dung ĐÃ ĐƯỢC CHỈNH SỬA?"
    assert row[2] == "C"
    assert row[3] == "HARD"
    assert row[4] == 20


def test_issue2_B_invalid_edit_data_rejected_and_db_untouched(tmp_path):
    client, admin_h, _, _, db_file, _ = setup_client(tmp_path)

    # Create question
    res_create = client.post("/admin/questions", headers=admin_h, json={
        "code": "Q_VAL_ERR_01", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi trước khi thử sửa sai",
        "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
        "correctOption": "A"
    })
    qid = res_create.json()["id"]

    # Try invalid edit: empty prompt
    res_bad1 = client.post("/admin/questions", headers=admin_h, json={
        "id": qid, "expectedVersion": 1,
        "prompt": "   ",  # Blank prompt!
        "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
        "correctOption": "A"
    })
    assert res_bad1.status_code == 400
    assert res_bad1.json()["code"] == "VALIDATION_FAILED"

    # Try invalid edit: invalid correctOption
    res_bad2 = client.post("/admin/questions", headers=admin_h, json={
        "id": qid, "expectedVersion": 1,
        "prompt": "Prompt hợp lệ",
        "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
        "correctOption": "Z"  # Invalid!
    })
    assert res_bad2.status_code == 400
    assert res_bad2.json()["code"] == "VALIDATION_FAILED"

    # Verify DB still has version 1 and original prompt
    conn = sqlite3.connect(db_file)
    cur = conn.cursor()
    cur.execute("SELECT version, prompt FROM questions WHERE id=?", (qid,))
    row = cur.fetchone()
    conn.close()
    assert row[0] == 1
    assert row[1] == "Câu hỏi trước khi thử sửa sai"


def test_issue2_C_student_or_unauthenticated_cannot_edit_question(tmp_path):
    client, admin_h, student_h, _, _, _ = setup_client(tmp_path)

    # Admin creates question
    res_create = client.post("/admin/questions", headers=admin_h, json={
        "code": "Q_SEC_01", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi bảo mật quyền?",
        "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
        "correctOption": "A"
    })
    qid = res_create.json()["id"]

    # Student cannot GET
    assert client.get(f"/admin/questions/{qid}", headers=student_h).status_code == 403
    # Unauthenticated cannot GET
    assert client.get(f"/admin/questions/{qid}").status_code == 401

    # Student cannot POST update
    res_stud = client.post("/admin/questions", headers=student_h, json={
        "id": qid, "prompt": "Học sinh cố tình hack sửa câu hỏi"
    })
    assert res_stud.status_code == 403

    # Unauthenticated cannot POST update
    res_anon = client.post("/admin/questions", json={
        "id": qid, "prompt": "Ẩn danh cố tình hack sửa câu hỏi"
    })
    assert res_anon.status_code == 401


def test_issue2_D_draft_published_status_preserved_on_content_edit(tmp_path):
    client, admin_h, _, _, _, _ = setup_client(tmp_path)

    # 1. Create question in DRAFT status
    res1 = client.post("/admin/questions", headers=admin_h, json={
        "code": "Q_DRAFT_01", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi draft gốc",
        "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
        "correctOption": "A", "status": "DRAFT"
    })
    q1_id = res1.json()["id"]
    assert res1.json()["status"] == "DRAFT"

    # Edit without specifying status: MUST REMAIN DRAFT!
    res_edit1 = client.post("/admin/questions", headers=admin_h, json={
        "id": q1_id, "expectedVersion": 1,
        "prompt": "Câu hỏi draft đã sửa nội dung"
    })
    assert res_edit1.status_code == 200
    assert res_edit1.json()["status"] == "DRAFT"

    # 2. Publish question
    res_pub = client.post(f"/admin/questions/{q1_id}/publish?publish=true", headers=admin_h)
    assert res_pub.status_code == 200
    assert res_pub.json()["status"] == "PUBLISHED"

    # Edit published question without specifying status: MUST REMAIN PUBLISHED!
    res_edit2 = client.post("/admin/questions", headers=admin_h, json={
        "id": q1_id, "expectedVersion": 2,
        "prompt": "Câu hỏi published đã sửa nội dung"
    })
    assert res_edit2.status_code == 200
    assert res_edit2.json()["status"] == "PUBLISHED"


def test_issue2_E_version_conflict_detected_and_notifies_user(tmp_path):
    client, admin_h, _, _, _, _ = setup_client(tmp_path)

    # Create question v1
    res = client.post("/admin/questions", headers=admin_h, json={
        "code": "Q_CONF_VER_01", "subjectId": "physics", "grade": 10,
        "prompt": "Nội dung gốc v1",
        "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
        "correctOption": "A"
    })
    qid = res.json()["id"]

    # Admin 1 updates to v2
    res_up1 = client.post("/admin/questions", headers=admin_h, json={
        "id": qid, "expectedVersion": 1,
        "prompt": "Nội dung cập nhật lên v2",
        "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
        "correctOption": "A"
    })
    assert res_up1.status_code == 200
    assert res_up1.json()["version"] == 2

    # Admin 2 (who opened when it was v1) attempts to save with expectedVersion=1
    res_up2 = client.post("/admin/questions", headers=admin_h, json={
        "id": qid, "expectedVersion": 1,
        "prompt": "Admin 2 cố ghi đè v1",
        "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
        "correctOption": "A"
    })
    assert res_up2.status_code == 409
    assert res_up2.json()["code"] == "VERSION_CONFLICT"
    assert "v2" in res_up2.json()["message"]


def test_issue2_F_editing_source_question_preserves_practice_session_snapshot(tmp_path):
    client, admin_h, student_h, _, db_file, _ = setup_client(tmp_path)

    # 1. Create and publish question
    res_q = client.post("/admin/questions", headers=admin_h, json={
        "code": "PHY_SNAP_01", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi snapshot gốc?",
        "optionA": "A_gốc", "optionB": "B_gốc", "optionC": "C_gốc", "optionD": "D_gốc",
        "correctOption": "A",
        "explanation": "Lời giải gốc",
        "status": "PUBLISHED"
    })
    qid = res_q.json()["id"]

    # 2. Create and publish quiz containing this question
    res_qz = client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Bộ đề kiểm tra Snapshot",
        "subjectId": "physics",
        "grade": 10,
        "questionIds": [qid],
        "status": "PUBLISHED"
    })
    quiz_id = res_qz.json()["id"]

    # 3. Student starts practice session
    res_sess = client.post("/library/sessions", headers=student_h, json={"quizId": quiz_id})
    assert res_sess.status_code == 201
    session_id = res_sess.json()["id"]

    # 4. Student submits answer 'A' (correct at snapshot time)
    res_ans = client.post(f"/library/sessions/{session_id}/answers", headers=student_h, json={
        "questionId": qid, "selectedOption": "A"
    })
    assert res_ans.status_code == 200
    assert res_ans.json()["score"] == 10

    # 5. Admin EDITS question in question bank (changes prompt, correctOption to 'B', and explanation)
    res_edit = client.post("/admin/questions", headers=admin_h, json={
        "id": qid,
        "expectedVersion": 1,
        "prompt": "Câu hỏi đã bị Admin thay đổi hoàn toàn?",
        "optionA": "A_mới", "optionB": "B_mới", "optionC": "C_mới", "optionD": "D_mới",
        "correctOption": "B",  # Changed from A to B!
        "explanation": "Lời giải đã thay đổi hoàn toàn",
        "points": 50
    })
    assert res_edit.status_code == 200

    # 6. Student completes session and inspects results
    res_comp = client.post(f"/library/sessions/{session_id}/complete", headers=student_h)
    assert res_comp.status_code == 200
    comp_data = res_comp.json()

    # CRITICAL CHECK: Score, prompt, correctOption, explanation in session must match SNAPSHOT, NOT modified question!
    assert comp_data["score"] == 10
    q_in_session = comp_data["questions"][0]
    assert q_in_session["prompt"] == "Câu hỏi snapshot gốc?"
    assert q_in_session["correctOption"] == "A"
    assert q_in_session["explanation"] == "Lời giải gốc"


def test_issue2_G_changing_subject_or_grade_of_question_in_published_quiz_blocked(tmp_path):
    client, admin_h, _, _, _, _ = setup_client(tmp_path)

    # 1. Create question
    res_q = client.post("/admin/questions", headers=admin_h, json={
        "code": "PHY_MEMBERSHIP_01", "subjectId": "physics", "grade": 10,
        "prompt": "Câu hỏi kiểm tra ràng buộc bộ đề",
        "optionA": "1", "optionB": "2", "optionC": "3", "optionD": "4",
        "correctOption": "A", "status": "PUBLISHED"
    })
    qid = res_q.json()["id"]

    # 2. Add to published quiz
    client.post("/admin/quizzes", headers=admin_h, json={
        "title": "Đề Vật lý 10 Xuất Bản",
        "subjectId": "physics", "grade": 10,
        "questionIds": [qid],
        "status": "PUBLISHED"
    })

    # 3. Attempt to change subject to chemistry
    res_change_sub = client.post("/admin/questions", headers=admin_h, json={
        "id": qid, "expectedVersion": 1,
        "subjectId": "chemistry", "grade": 10,
        "prompt": "Câu hỏi cố tình đổi sang môn Hóa"
    })
    assert res_change_sub.status_code == 409
    assert res_change_sub.json()["code"] == "CONFLICT_QUIZ_MEMBERSHIP"

    # 4. Attempt to change grade to 12
    res_change_grade = client.post("/admin/questions", headers=admin_h, json={
        "id": qid, "expectedVersion": 1,
        "subjectId": "physics", "grade": 12,
        "prompt": "Câu hỏi cố tình đổi sang lớp 12"
    })
    assert res_change_grade.status_code == 409
    assert res_change_grade.json()["code"] == "CONFLICT_QUIZ_MEMBERSHIP"
