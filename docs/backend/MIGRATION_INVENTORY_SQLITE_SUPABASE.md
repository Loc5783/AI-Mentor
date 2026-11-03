# Kiểm Kê Di Chuyển Dữ Liệu: SQLite (aimentor.sqlite3) Sang Supabase Postgres

> **Trạng thái:** Kế hoạch di chuyển & kiểm kê dữ liệu Ngoại tuyến (Phase 1 Offline Preparation).  
> **Dự án đích:** Supabase Project `qvhdoczieavwjmfrcibe` (`https://qvhdoczieavwjmfrcibe.supabase.co`).  
> **Cơ sở dữ liệu nguồn:** `backend/data/aimentor.sqlite3` (Bản sao lưu bất biến: `backend/data/aimentor-before-supabase-20261101T015355Z.sqlite3`).  
> **Nguyên tắc an toàn:** Tuyệt đối không xóa/ghi đè bản sao lưu; không đọc hoặc in ra giá trị mật khẩu hash hay thông tin cá nhân học sinh; không áp dụng di chuyển lên cụm live trong pha 1.

---

## 1. Bảng Tổng Hợp Kiểm Kê 26 Bảng SQLite

| STT | Bảng Nguồn (SQLite) | Số Dòng Thực Tế | Tính Chất Dữ Liệu | Đích Đến Trên Supabase | Cơ Chế Ánh Xạ ID / Quyền Sở Hữu | Xử Lý Khi Di Chuyển |
| :---: | :--- | :---: | :--- | :--- | :--- | :--- |
| **1** | `users` | 6 | Bền vững (Durable) | `auth.users` + `public.mentor_profiles` + `public.mentor_preferences` | `id (INT)` $\rightarrow$ `id (UUID)` trong Supabase Auth | **Không copy trực tiếp scrypt hash**. Thực hiện chuyển đổi danh tính qua email verified signup hoặc one-time claim token. |
| **2** | `sessions` | 14 | Tạm thời (Security-Ephemeral) | *Loại bỏ (Excluded)* | Token hash cục bộ của FastAPI | **Loại trừ khỏi di chuyển**. Supabase Auth tự cấp và quản lý JWT access token / refresh token độc lập. |
| **3** | `resources` | 0 | Bền vững (Durable) | `public.mentor_chat_folders`, `mentor_chat_threads`, `mentor_chat_messages`, `mentor_chat_files` | `user_id (INT)` $\rightarrow$ `user_id (UUID)` | Metadata chuyển sang cấu trúc bảng chat/storage của Supabase. |
| **4** | `enrollments` | 4 | Bền vững (Durable) | `public.mentor_enrollments` | `user_id (INT)` $\rightarrow$ `user_id (UUID)`, `id (TEXT)` $\rightarrow$ `id (UUID)` | Giữ nguyên lựa chọn môn học, khối lớp 10–12, kết quả placement JSONB. |
| **5** | `attempts` | 5 | Bền vững (Durable) | `public.mentor_attempts` | `user_id` $\rightarrow$ `UUID`, `enrollment_id` $\rightarrow$ `UUID` | Giữ nguyên tiến độ bài tập, số bước (step), câu trả lời (answers JSONB). |
| **6** | `completions` | 5 | Bền vững (Durable) | `public.mentor_completions` | `attempt_id` $\rightarrow$ `UUID`, `user_id` $\rightarrow$ `UUID` | Chuyển đổi kết quả hoàn thành bài học (result JSONB), điểm thưởng. |
| **7** | `awards` | 2 | Bền vững (Durable) | `public.mentor_awards` | `user_id` $\rightarrow$ `UUID`, `(user_id, subject_id, grade, unit_id)` | Lưu giữ điểm EXP đạt được (200 / 400 XP) và ngày học tích lũy. |
| **8** | `learning_days` | 1 | Bền vững (Durable) | `public.mentor_learning_days` | `user_id` $\rightarrow$ `UUID`, `day (DATE)` | Giữ chuỗi học liên tục (streak calendar) của từng học sinh. |
| **9** | `friendships` | 0 | Bền vững (Durable) | `public.mentor_friendships` | `requester` $\rightarrow$ `UUID`, `recipient` $\rightarrow$ `UUID` | Quan hệ kết bạn hai chiều (`PENDING`, `ACCEPTED`). |
| **10** | `events` | 109 | Tạm thời (Ephemeral) | *Loại bỏ (Excluded)* | Event stream tuần tự (`seq`) | **Loại trừ khỏi di chuyển**. Supabase Realtime lắng nghe CDC trực tiếp qua WAL PostgreSQL (`supabase_realtime` publication). |
| **11** | `idempotency` | 21 | Tạm thời (Ephemeral) | `public.mentor_requests` | `owner` $\rightarrow$ `UUID`, `key` $\rightarrow$ `key` | Lưu giữ các khóa request trong cửa sổ 24h để chống lặp tác vụ. |
| **12** | `recovery_tokens` | 0 | Tạm thời (Security-Ephemeral) | *Loại bỏ (Excluded)* | Token đặt lại mật khẩu của FastAPI | **Loại trừ khỏi di chuyển**. Supabase Auth cung cấp quy trình khôi phục mật khẩu qua Email OTP bảo mật riêng. |
| **13** | `rate_limits` | 3 | Tạm thời (Ephemeral) | *Loại bỏ (Excluded)* | Bộ đếm tần suất cửa sổ trượt | **Loại trừ khỏi di chuyển**. Bộ đếm cục bộ hết hạn ngay sau vài phút, Supabase có lớp rate limiting tại API Gateway. |
| **14** | `badges` | 0 | Bền vững (Durable) | `public.mentor_badge_catalog` | `code (TEXT)` $\rightarrow$ `code (TEXT)` | Danh mục danh hiệu, quy tắc mở khóa. |
| **15** | `user_badges` | 0 | Bền vững (Durable) | `public.mentor_badge_awards` | `user_id` $\rightarrow$ `UUID`, `badge_code` $\rightarrow$ `code` | Huy hiệu học sinh đã đạt được. |
| **16** | `friend_streaks` | 0 | Bền vững (Durable) | `public.mentor_friend_streaks` | `(user1_id, user2_id)` $\rightarrow$ `UUID` | Chuỗi ngày cùng học của đôi bạn. |
| **17** | `questions` | 5 | Bền vững (Durable) | `public.mentor_course_units.questions` + `public.mentor_library_quizzes.questions` | `created_by` $\rightarrow$ `UUID` | Chuyển đổi câu hỏi trắc nghiệm vào cấu trúc JSONB của từng đơn vị bài học và bộ đề. |
| **18** | `library_quizzes` | 1 | Bền vững (Durable) | `public.mentor_library_quizzes` | `id (TEXT)` $\rightarrow$ `id (TEXT)` | Bộ đề ôn tập thư viện câu hỏi kèm danh sách questions JSONB. |
| **19** | `practice_sessions`| 1 | Bền vững (Durable) | `public.mentor_practice_sessions` | `id` $\rightarrow$ `UUID`, `user_id` $\rightarrow$ `UUID` | Phiên luyện tập trắc nghiệm dở dang hoặc đã xong, kèm snapshot câu hỏi. |
| **20** | `wrong_questions_notebook` | 2 | Bền vững (Durable) | `public.mentor_wrong_questions` | `id` $\rightarrow$ `UUID`, `user_id` $\rightarrow$ `UUID` | Sổ tay ghi chép câu hỏi làm sai, phục vụ ôn tập lặp lại ngắt quãng (Spaced Repetition). |
| **21** | `role_audit_logs` | 0 | Bền vững (Durable) | `public.mentor_role_audit_logs` | `user_id` $\rightarrow$ `UUID` | Lịch sử phân quyền tài khoản (STUDENT $\rightarrow$ TEACHER $\rightarrow$ ADMIN). |
| **22** | `exams` | 2 | Bền vững (Durable) | `public.mentor_exams` | `id (TEXT)`, `created_by` $\rightarrow$ `UUID` | Đề thi định kỳ (Giữa kỳ, Cuối kỳ) do giáo viên biên soạn. |
| **23** | `exam_sessions` | 0 | Bền vững (Durable) | `public.mentor_exam_sessions` | `id` $\rightarrow$ `UUID`, `user_id` $\rightarrow$ `UUID`, `exam_id` | Phiên làm bài kiểm tra chính thức của học sinh. |
| **24** | `ai_practice_sessions` | 0 | Bền vững (Durable) | `public.mentor_ai_practice_sessions` | `id` $\rightarrow$ `UUID`, `user_id` $\rightarrow$ `UUID` | Phiên luyện tập do gia sư AI tạo tự động theo chủ đề. |
| **25** | `ai_question_reports` | 0 | Bền vững (Durable) | `public.mentor_ai_question_reports` | `id` $\rightarrow$ `UUID`, `user_id` $\rightarrow$ `UUID`, `session_id` | Báo cáo phản hồi lỗi câu hỏi do AI tạo. |
| **26** | `ai_provider_operations` | 1 | Tạm thời (Ephemeral) | `public.mentor_ai_usage` | `user_id` $\rightarrow$ `UUID`, `operation_key` | Kiểm soát hạn ngạch và tính bất biến khi gọi mô hình ngôn ngữ lớn (LLM). |

---

## 2. Kiểm Kê Tệp Tin & Lưu Trữ Đính Kèm (Uploads / Binary Files)

- **Vị trí hiện tại trên đĩa:** `backend/data/uploads/<user_id>/<attachment_id>.bin`.
- **Cơ chế lưu trữ:**
  - SQLite chỉ lưu metadata trong bảng `resources` (`kind = 'ATTACHMENT'`, kích thước, MIME type, SHA256).
  - Tệp nhị phân được lưu phân tán theo thư mục ID người dùng.
- **Kế hoạch chuyển đổi sang Supabase Storage:**
  - Tạo Storage Bucket: `avatars` (Công khai có kiểm soát) và `chat-files` (Riêng tư - Private).
  - Tệp ảnh đại diện được chuyển vào bucket `avatars/<user_id>/profile.jpg`.
  - Tệp tài liệu đính kèm chat được chuyển vào bucket `chat-files/<user_id>/<file_uuid>.bin`.
  - **Chính sách bảo mật RLS trên Storage:** Áp dụng policy `(bucket_id = 'chat-files' AND auth.uid()::text = (storage.foldername(name))[1])` đảm bảo học sinh chỉ tải được tệp tin của chính mình.

---

## 3. Kế Hoạch Chuyển Đổi Tài Khoản Người Dùng (Account Transition Plan)

1. **Vấn đề mật khẩu scrypt vs bcrypt:**
   - FastAPI lưu mật khẩu dưới dạng `scrypt` với tham số bảo mật cao (`$scrypt$...`).
   - Supabase Auth sử dụng `bcrypt` tiêu chuẩn và quản lý bảng kín `auth.users`.
   - **Tuyệt đối không thể copy trực tiếp mã băm scrypt vào auth.users**.
2. **Quy trình chuyển đổi danh tính an toàn (Safe Identity Transition):**
   - **Phương án 1 (Verified Signup with Matching Email):**
     - Học sinh đăng ký tài khoản mới trên Supabase Auth bằng đúng Email đã đăng ký trước đó.
     - Sau khi xác thực email, migration tool so khớp Email học sinh và liên kết `auth.uid()` mới với `users.id` trong SQLite.
     - Toàn bộ dữ liệu học tập cũ (`enrollments`, `attempts`, `completions`, `awards`, `learning_days`, `wrong_questions`) được gán lại khóa ngoại `user_id = auth.uid()`.
   - **Phương án 2 (Secure One-Time Claim Token):**
     - Sinh mã bí mật một lần (One-Time Claim Token) ký bởi server backend cũ cho từng học sinh.
     - Học sinh đăng nhập vào tài khoản Supabase mới và gửi claim token để nhận lại quyền sở hữu tiến trình học tập mà không cần nhập lại mật khẩu cũ.
3. **Bảo tồn tiến trình học tập:**
   - Tất cả các bản ghi học tập (5 completions, 4 enrollments, 2 awards, 2 wrong questions) được gắn kết toàn vẹn với tài khoản sau khi chuyển đổi. Học sinh không bị mất chuỗi streak hay điểm EXP.

---

## 4. Lý Do Loại Trừ Dữ Liệu Tạm Thời Khỏi Cloud Import

1. **`sessions` (14 dòng):** Chứa access token và refresh token của FastAPI server. Các token này bị vô hiệu hóa khi đổi backend và không tương thích với định dạng JWT của Supabase GoTrue Auth.
2. **`recovery_tokens` (0 dòng):** Token đặt lại mật khẩu một lần của FastAPI. Để bảo mật, không được phép đưa token nhạy cảm lên cloud.
3. **`events` (109 dòng):** Hàng đợi sự kiện SSE chỉ phục vụ client đang mở ứng dụng. Supabase Realtime hoạt động theo cơ chế CDC qua WAL Postgres, không cần nạp lại log cũ.
4. **`rate_limits` (3 dòng):** Bộ đếm tạm thời theo khung thời gian ngắn, tự động reset sau vài phút.
