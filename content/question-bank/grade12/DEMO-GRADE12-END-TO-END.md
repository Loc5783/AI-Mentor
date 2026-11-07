# Báo cáo kết nối ngân hàng 1.798 câu lớp 12 với luồng luyện tập thực tế trên AI-Mentor

- **Thời điểm hoàn thành:** 2026-11-07
- **Dự án:** AI-Mentor (`E:\AI-Mentor`)
- **Database Supabase:** `hnxphwuaccztzanjcuca` (`https://hnxphwuaccztzanjcuca.supabase.co`)
- **Tài khoản kiểm thử học sinh:** `student_test@ai-mentor.example`
- **Tài liệu đối chiếu:** `AUDIT-DEMO-IMPORT.md`, `AUDIT-CODEX-ROUND5.md`, `AGENTS.md`

---

## 1. Mục tiêu và phạm vi

1. **Mục tiêu:**
   - Kết nối toàn diện 1.798 câu trắc nghiệm lớp 12 (đã nhập và `PUBLISHED` trên bảng `mentor_questions`) với luồng luyện tập thực tế của học sinh trên ứng dụng AI-Mentor và hệ thống Web Admin.
   - Giải quyết triệt để vấn đề được chỉ ra trong báo cáo kiểm tra độc lập `AUDIT-DEMO-IMPORT.md`: ứng dụng trước đây chỉ hiển thị 30 bộ đề cũ (chứa 300 câu mock thời kỳ đầu) mà chưa có liên kết với 1.798 câu mới.
   - Thiết lập các bộ đề luyện tập chuẩn (khoảng 30 câu/bộ) bao phủ 100% lô 1.798 câu mới qua đủ 9 môn học.
   - Kiểm thử độc lập toàn bộ vòng đời làm bài: nhận diện đề, nộp bài, tính điểm độc lập, thử tính lũy đẳng (idempotency), thử từ chối đáp án xung đột (conflict rejection), xem lời giải, và ôn tập sổ tay câu sai (wrong questions notebook).

2. **Nguyên tắc cam kết:**
   - **Tập trung chức năng hoạt động thực tế:** Báo cáo này chứng minh luồng kỹ thuật và kết nối dữ liệu hoạt động hoàn chỉnh từ đầu đến cuối; **không** tuyên bố Codex đã nghiệm thu nội dung học thuật toàn bộ ngân hàng.
   - **Bảo toàn dữ liệu:** Không xóa tài khoản người dùng, không xóa tiến độ học tập, không xóa hoặc ghi đè 30 bộ đề cũ.
   - **Bảo mật:** Tuyệt đối không để lộ khóa bí mật (service key), mật khẩu trong mã nguồn hay tài liệu.
   - **Tuân thủ quy trình Git:** Không tự ý commit hoặc push lên repository.
   - **Minh bạch về thiết bị:** Ghi nhận trung thực kết quả kiểm tra `adb devices` (không có thiết bị vật lý kết nối), không mạo nhận đã thử nghiệm trên phần cứng.

---

## 2. Phân tích nguyên nhân gốc (Root Cause Analysis)

Theo `AUDIT-DEMO-IMPORT.md` mục 12:
> *“mentor_library_quizzes có 30 bộ đề lớp 12, chứa 300 lượt câu hỏi JSON. Không câu nào có ID/code nối tới ID/code của lô mentor_questions lớp 12... Script test_student_practice_flow.py đọc /library/quizzes, chọn quizzes[0] và làm một bộ đề cũ...”*

### Cơ chế hoạt động của AI-Mentor:
- Bảng `mentor_questions` đóng vai trò là **Kho câu hỏi gốc (Master Question Bank)** dùng chung cho Web Admin và tạo đề thi.
- Luồng luyện tập trên ứng dụng di động (`SupabaseLibraryRepository.kt` và RPC `mentor_library_api`) truy vấn danh sách đề từ bảng `mentor_library_quizzes`.
- Mỗi bản ghi trong `mentor_library_quizzes` chứa mảng JSONB `questions` đóng vai trò là **Question Snapshots** (lưu đề bài, 4 phương án, đáp án đúng, lời giải, điểm số). Khi học sinh bắt đầu phiên luyện tập (`POST /library/sessions`), hệ thống sao chép snapshot này sang phiên làm bài (`mentor_practice_sessions`).
- **Nguyên nhân:** Trước đây, 1.798 câu hỏi lớp 12 mới chỉ được nhập vào `mentor_questions`, nhưng chưa có bộ đề tương ứng nào được biên tập và xuất bản vào `mentor_library_quizzes` sử dụng mã và ID của lô mới. Do đó, app chỉ nhìn thấy 30 bộ đề mock được tạo từ trước.

---

## 3. Tạo và liên kết 63 bộ đề luyện tập mới (Bao phủ 100% 1.798 câu)

Hệ thống đã biên tập tự động 63 bộ đề luyện tập mới trực tiếp từ 1.798 câu hỏi đã xuất bản trong `mentor_questions`, phân bổ đều cho 9 môn học:

### 3.1. Quy tắc phân bổ
- Mỗi bộ đề gồm khoảng 30 câu hỏi (theo từng chương/chủ đề nối tiếp).
- Đối với 8 môn có 200 câu (Toán, Hóa học, Sinh học, Tiếng Anh, Lịch sử, Địa lí, GDKT&PL, Ngữ văn):
  - 6 bộ đầu $\times$ 30 câu = 180 câu.
  - Bộ thứ 7 $\times$ 20 câu = 20 câu (phần còn lại).
  - Tổng: 7 bộ $\times$ 8 môn = 56 bộ (1.600 câu).
- Đối với môn Vật lí có 198 câu (đã loại trừ `AIM_PHY12_BANK01_Q149` và `AIM_PHY12_BANK01_Q199`):
  - 6 bộ đầu $\times$ 30 câu = 180 câu.
  - Bộ thứ 7 $\times$ 18 câu = 18 câu.
  - Tổng: 7 bộ $\times$ 1 môn = 7 bộ (198 câu).
- **Tổng cộng toàn hệ thống:** **63 bộ đề mới**, bao phủ chính xác **1.798/1.798 câu hỏi**, đạt **độ bao phủ 100,0%**.

### 3.2. Cấu trúc định danh và snapshot
- **Mã định danh bộ đề (Quiz ID):** `quiz_g12_demo_{subject_id}_{set_idx:02d}` (ví dụ: `quiz_g12_demo_math_01` đến `quiz_g12_demo_math_07`).
  - Tiền tố `quiz_g12_demo_` giúp phân biệt rõ ràng với 30 bộ đề cũ (`review-*`), hỗ trợ cập nhật lũy đẳng (upsert/idempotent) mà không gây trùng lặp.
- **Liên kết câu hỏi:** Mỗi câu trong snapshot giữ nguyên:
  - `id`: trùng khớp `mentor_questions.id` (ví dụ: `q_724bcd6b817e`).
  - `code`: trùng khớp `mentor_questions.code` (ví dụ: `AIM_MATH12_BANK01_Q001`).
  - `source_id`: tham chiếu tường minh tới ID gốc.
  - `options`: 4 lựa chọn chuẩn `A`, `B`, `C`, `D`.
  - `correctOption`: đáp án đúng đối chiếu từ ngân hàng.
  - `explanation`: lời giải chi tiết.
  - `points`: 10 điểm/câu.
- **Hồ sơ lưu trữ:** Toàn bộ thông tin liên kết 63 bộ đề được lưu trữ tại file manifest:
  `C:\Users\PhamDinhDong\.gemini\antigravity\brain\80b0164a-931d-4df3-b293-2dce2db739cf\scratch\manifest-demo-quizzes.json`.

### 3.3. Trạng thái bảng `mentor_library_quizzes` sau khi triển khai
- **Số bộ đề cũ được bảo toàn:** 61 bộ (31 bộ lớp 10, 30 bộ lớp 12).
- **Số bộ đề mới được thêm vào:** 63 bộ (đều thuộc lớp 12, trạng thái `PUBLISHED`).
- **Tổng số bộ đề hiện có:** 124 bộ đề.

---

## 4. Kết quả kiểm thử toàn diện vòng đời luyện tập (End-to-End Test)

Kịch bản kiểm thử độc lập tự động (`test_e2e_student_demo_flow.py`) được thực thi trên môi trường Supabase thực tế với tài khoản học sinh `student_test@ai-mentor.example`.

### Giai đoạn 1: Khám phá danh mục đề thi (Quiz Discovery)
- Gọi API `GET /library/quizzes?grade=12` qua RPC `mentor_library_api`.
- Trả về tổng cộng 93 bộ đề lớp 12 (30 bộ cũ + 63 bộ mới).
- Xác nhận đủ 9 môn học, mỗi môn có đúng 7 bộ đề demo mới:
  - `math`: 7 bộ đề.
  - `physics`: 7 bộ đề.
  - `chemistry`: 7 bộ đề.
  - `biology`: 7 bộ đề.
  - `english`: 7 bộ đề.
  - `history`: 7 bộ đề.
  - `geography`: 7 bộ đề.
  - `economics_law`: 7 bộ đề.
  - `literature`: 7 bộ đề.
- **Kết quả:** **ĐẠT (100%)**.

### Giai đoạn 2: Khởi tạo phiên luyện tập (Start Session)
- Chọn bộ đề: `quiz_g12_demo_math_01` (*Toán 12 - Luyện tập Bộ 01: Ứng dụng đạo hàm khảo sát hàm số*).
- Gọi `POST /library/sessions` với `quizId: "quiz_g12_demo_math_01"`.
- Phiên được tạo thành công: ID = `232ce4da-6160-4219-b0e9-f3280672dfac`.
- Kiểm tra tính riêng tư và bảo mật đề thi:
  - Tổng số câu: 30 câu, trạng thái `IN_PROGRESS`.
  - Mỗi câu hỏi hiển thị đầy đủ 4 phương án `A`, `B`, `C`, `D`.
  - `correctOption` và `explanation` **bị ẩn hoàn toàn** (không rò rỉ đáp án khi học sinh chưa nộp bài).
- **Kết quả:** **ĐẠT**.

### Giai đoạn 3: Tính toán điểm độc lập & Nộp bài (Independent Scoring)
Để khắc phục điểm hạn chế Codex đã nêu (*"Script học sinh không tính độc lập expectedScore để đối chiếu score/correctCount"*):
- Kịch bản truy vấn ground-truth trực tiếp từ `mentor_questions` để lập kế hoạch trả lời:
  - **20 câu đầu (câu 0 đến 19):** Trả lời **ĐÚNG** theo `correct_option`.
  - **10 câu sau (câu 20 đến 29):** Cố tình trả lời **SAI** (chọn phương án nhiễu).
- Kỳ vọng độc lập tính toán trước:
  - `expected_correct_count` = 20 / 30 câu.
  - `expected_score` = 20 câu $\times$ 10 điểm = 200 / 300 điểm.
- Nộp lần lượt 30 câu qua `POST /library/sessions/{id}/answers`.
  - Mỗi lượt nộp trả về HTTP 200, trường `answeredCount` tăng dần từ 1 đến 30.
- **Kết quả:** **ĐẠT**.

### Giai đoạn 4: Kiểm tra tính lũy đẳng và từ chối đáp án xung đột
Để giải quyết yêu cầu của Codex (*"Script kiểm tra gửi lại cùng đáp án; không có ca gửi đáp án xung đột"*):
1. **Thử nghiệm lũy đẳng (Idempotency):**
   - Gửi lại chính xác đáp án cũ cho câu hỏi 1.
   - Kết quả: Trả về HTTP 200 OK, `answeredCount` giữ nguyên 30 (không tính trùng lặp).
2. **Thử nghiệm xung đột (Conflict Rejection):**
   - Gửi đáp án khác (ví dụ: đã chọn 'B', gửi lại 'A') cho câu hỏi đã có câu trả lời.
   - Kết quả: API từ chối với HTTP 409 Conflict, mã lỗi `PT409`, thông điệp: `"ANSWER_ALREADY_SUBMITTED"`.
- **Kết quả:** **ĐẠT**.

### Giai đoạn 5: Hoàn tất phiên & Khớp điểm độc lập
- Gọi `POST /library/sessions/{id}/complete`.
- Đối chiếu độc lập:
  - `actual_score` = **200** == `expected_score` = **200**.
  - `actual_correct` = **20** == `expected_correct_count` = **20**.
  - `totalCount` = **30**, `maxScore` = **300**.
- Trạng thái phiên chuyển sang `COMPLETED`.
- **Kết quả:** **ĐẠT (Khớp 100% với kỳ vọng tính toán độc lập)**.

### Giai đoạn 6: Xem lại lịch sử & Hiển thị lời giải
- Gọi `GET /library/sessions/{id}`.
- Kiểm tra dữ liệu trả về cho học sinh:
  - Cả 30 câu hỏi đều hiển thị rõ ràng: `userAnswer`, `isCorrect`, `correctOption` và `explanation`.
  - 20 câu đầu có `isCorrect = true`, 10 câu sau có `isCorrect = false`.
  - Lời giải hiển thị chi tiết (ví dụ: câu 1 giải thích phương trình đạo hàm $f'(x) < 0 \Leftrightarrow x^2 - 4 < 0 \Leftrightarrow -2 < x < 2$).
- **Kết quả:** **ĐẠT**.

### Giai đoạn 7: Sổ tay câu hỏi sai & Luyện tập lại (Wrong Questions Notebook)
- Gọi `GET /me/wrong-questions?grade=12&subject_id=math`.
- Tìm thấy đầy đủ **10/10 câu hỏi** đã làm sai ở phiên luyện tập trên, với trạng thái `NEEDS_REVIEW`.
- Chọn câu hỏi sai đầu tiên (`q_8699b8f58fed`, entry ID `22ebcaaf-819d-425f-adf2-300ba0170a20`).
- Gọi `POST /me/wrong-questions/{entryId}/retry` với phương án đúng (`A`).
- Kết quả: Trả về HTTP 200, `isCorrect = true`, trạng thái chuyển thành **`RESOLVED`**.
- **Kết quả:** **ĐẠT**.

### Giai đoạn 8: Xử lý lỗi biên (Edge Cases)
- Thử bắt đầu bài với ID bộ đề không tồn tại (`quiz_non_existent_999`): Bị chặn với lỗi `404 QUIZ_NOT_FOUND`.
- Thử chọn đáp án không hợp lệ (`Z`): Bị chặn với lỗi `INVALID_ANSWER`.
- **Kết quả:** **ĐẠT**.

---

## 5. Xác minh hệ thống Web Admin

Kiểm tra trực tiếp thông qua Backend Web Admin đang chạy tại `http://127.0.0.1:18080` (sử dụng JWT token quản trị viên):

1. **API Quản lý câu hỏi (`GET /admin/questions?subject_id={subj}&grade=12`):**
   - Đủ 9 môn học: Vật lí 198 câu, 8 môn còn lại mỗi môn 200 câu (tổng 1.798 câu).
   - 100% câu hỏi ở trạng thái `PUBLISHED`.
   - Các trường `prompt`, `optionA`, `optionB`, `optionC`, `optionD`, `correctOption`, `explanation` hiển thị đầy đủ tiếng Việt.
2. **API Quản lý bộ đề (`GET /admin/quizzes?grade=12`):**
   - Trả về 93 bộ đề lớp 12 (gồm 30 bộ cũ và 63 bộ demo mới).
   - Đủ 9 môn, mỗi môn 7 bộ đề mới.
   - Mỗi bộ đề hiển thị đúng danh sách `questionIds`, `questionCount` (30 câu hoặc 20/18 câu), `chapter`, `topic` và trạng thái `PUBLISHED`.
3. **Lưu ý phương pháp kiểm thử:**
   - Việc kiểm tra Web Admin được thực hiện thông qua HTTP API của máy chủ backend; không thực hiện mô phỏng render trình duyệt DOM do môi trường không cài đặt Playwright/Selenium.

---

## 6. Trạng thái kiểm thử ứng dụng di động Android

1. **Kiểm tra thiết bị phần cứng qua ADB:**
   - Đã thực thi lệnh `adb devices` từ Android SDK platform-tools.
   - Kết quả: Danh sách thiết bị trống (`List of devices attached` rỗng).
   - **Xác nhận trung thực:** Không có thiết bị Android vật lý hay máy ảo kết nối trong phiên làm việc. Do đó, **chưa thực hiện kiểm thử trên phần cứng thiết bị di động**.
2. **Kiểm thử tự động mã nguồn Android (Unit & Repository Tests):**
   - Đã chạy kiểm thử tự động Gradle:
     `cmd /c gradlew.bat :app:testDebugUnitTest --tests "*Library*"`
   - Kết quả: **`BUILD SUCCESSFUL in 1m 32s`** (toàn bộ 215 tác vụ thực thi/up-to-date thành công, bao gồm các lớp kiểm thử `LibraryPracticeFlowTest`, `LibrarySelectionPresentationTest`, `SupabaseLibraryRepository`).

---

## 7. Bảng tổng hợp số liệu kỹ thuật

| Tiêu chí | Mục tiêu | Kết quả thực tế | Trạng thái |
| :--- | :---: | :---: | :---: |
| Số câu hỏi lớp 12 trong `mentor_questions` | 1.798 câu | 1.798 câu (`PUBLISHED`) | **ĐẠT** |
| Hai câu lỗi Vật lí loại trừ (`Q149`, `Q199`) | Vắng mặt | Đã xác nhận không tồn tại | **ĐẠT** |
| Số câu hỏi lớp 10 được bảo toàn | 235 câu | 200 DRAFT + 35 PUBLISHED | **ĐẠT** |
| Số bộ đề luyện tập lớp 12 mới tạo | 63 bộ | 63 bộ (`PUBLISHED`) | **ĐẠT** |
| Phân bổ bộ đề theo môn học | 7 bộ / môn $\times$ 9 môn | Đủ 9 môn, mỗi môn 7 bộ | **ĐẠT** |
| Tỷ lệ bao phủ câu hỏi trong các bộ đề | 100% (1.798 câu) | 1.798 / 1.798 câu duy nhất | **ĐẠT** |
| Bảo toàn 30 bộ đề cũ của dự án | Không xóa | 30/30 bộ đề cũ nguyên vẹn | **ĐẠT** |
| Luồng học sinh nộp bài & ẩn đáp án trước khi nộp | Bảo mật | Ẩn `correctOption`/`explanation` khi làm bài | **ĐẠT** |
| Tính điểm độc lập (Expected vs Actual) | Khớp 100% | 200/200 điểm, 20/20 câu đúng | **ĐẠT** |
| Tính lũy đẳng khi nộp lại cùng đáp án | HTTP 200 | Không tính trùng lặp | **ĐẠT** |
| Từ chối đáp án xung đột cho câu đã nộp | HTTP 409 | Trả về `ANSWER_ALREADY_SUBMITTED` | **ĐẠT** |
| Xem lại bài & lời giải chi tiết | Đầy đủ | Hiển thị 30/30 lời giải | **ĐẠT** |
| Ghi nhận câu sai vào Sổ tay & Sửa lại | `RESOLVED` | Chuyển trạng thái khi chọn đúng | **ĐẠT** |
| Web Admin API (Câu hỏi & Bộ đề) | 200 OK | Trả về đủ 1.798 câu & 63 bộ đề | **ĐẠT** |
| Kiểm thử đơn vị Android (`testDebugUnitTest`) | Passed | `BUILD SUCCESSFUL` | **ĐẠT** |
| Thử nghiệm trên phần cứng điện thoại | Đánh giá thực tế | Chưa có thiết bị gắn ngoài | **Ghi nhận rõ** |

---

## 8. Kết luận

- Hệ thống đã kết nối hoàn tất 100% lô 1.798 câu hỏi lớp 12 đã nhập Supabase vào luồng luyện tập thực tế thông qua 63 bộ đề luyện tập mới chuẩn mực.
- Học sinh khi mở ứng dụng chọn bất kỳ môn học nào trong 9 môn lớp 12 đều có thể tìm thấy các bộ đề mới, làm bài, nộp bài, nhận điểm số chính xác, xem lời giải và lưu câu sai vào sổ tay để ôn tập lại.
- Các yêu cầu kiểm thử độc lập của Codex (tính điểm độc lập, thử đáp án xung đột, xử lý lỗi biên) đều đã được thực thi và xác nhận thành công.
