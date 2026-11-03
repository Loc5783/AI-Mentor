# Hướng Dẫn Chuẩn Bị Kỹ Thuật Cho Supabase Postgres (AI Mentor)

> **Trạng thái tài liệu:** Tài liệu chuẩn bị kỹ thuật & tham khảo tương lai (Future Technical Reference).  
> **Phạm vi áp dụng:** Dự án AI Mentor (`E:/AI-Mentor`).  
> **Cam kết cốt lõi:** Hiện tại toàn bộ hệ thống đang vận hành hoàn toàn trên SQLite cục bộ (`aimentor.sqlite3`). Tài liệu này chỉ chuẩn bị kế hoạch, KHÔNG thay đổi runtime đang chạy, KHÔNG chạm dữ liệu thật, và KHÔNG giả lập chuyển đổi backend khi chưa có dự án Supabase chính thức được người dùng phê duyệt.

---

## 1. So Sánh Kiến Trúc: SQLite Hiện Tại vs Supabase PostgreSQL

| Tiêu chí | SQLite Hiện Tại (`aimentor.sqlite3`) | Supabase PostgreSQL Tương Lai |
| :--- | :--- | :--- |
| **Mô hình lưu trữ** | Cơ sở dữ liệu nhúng (in-process, file đơn trên ổ đĩa cục bộ `backend/data/aimentor.sqlite3`). | Hệ quản trị CSDL quan hệ Client - Server lưu trữ trên đám mây, kết nối qua mạng TCP/IP có mã hóa SSL/TLS (`sslmode=require`). |
| **Cơ chế khóa & Concurrency** | Khóa cấp file (File locking). Ghi tuần tự qua `BEGIN IMMEDIATE` và chế độ WAL (`PRAGMA journal_mode=WAL`). | Đa phiên bản đồng thời (MVCC - Multi-Version Concurrency Control), hỗ trợ nhiều kết nối đọc/ghi đồng thời không khóa chéo bảng. |
| **Quản lý kết nối** | Mở/đóng kết nối file cục bộ theo từng context manager (`with database.transaction()`, `with database.read()`). | Quản lý qua Connection Pooler (Supavisor) với chế độ Session Pooler (Cổng 5432). |
| **Ranh giới ứng dụng (Boundary)** | FastAPI là ranh giới duy nhất xử lý business logic, xác thực JWT, và kiểm tra quyền truy cập. | **FastAPI tiếp tục là RANH GIỚI DUY NHẤT**. Ứng dụng Android và WebAdmin tuyệt đối KHÔNG kết nối trực tiếp vào Postgres hay Supabase Data API. |
| **Dịch vụ đi kèm của Supabase** | Không áp dụng. | Supabase cung cấp sẵn Supabase Auth, Storage, PostgREST (Data API). **Tuy nhiên, AI Mentor chỉ định hướng sử dụng PostgreSQL làm CSDL lưu trữ cho FastAPI**, KHÔNG chuyển đổi xác thực sang Supabase Auth hay chuyển tệp sang Supabase Storage ở giai đoạn này. |

---

## 2. Hướng Dẫn Từng Bước Tạo Dự Án Supabase & Lấy Connection String

Dành cho người mới bắt đầu (Novice), các thao tác trực tiếp trên giao diện Supabase Dashboard:

### Bước 1: Đăng ký / Đăng nhập
1. Truy cập [https://supabase.com](https://supabase.com) và chọn **Sign In**.
2. Chọn Organization của bạn trong trang Dashboard.

### Bước 2: Tạo dự án mới (New Project)
1. Bấm nút **New Project**.
2. Cấu hình các mục:
   - **Name:** Đặt tên định danh (ví dụ: `aimentor-backend`).
   - **Database Password:** Bấm tạo mật khẩu ngẫu nhiên hoặc tự đặt mật khẩu tối thiểu 16 ký tự.
     > [!CAUTION]
     > **BẢO MẬT MẬT KHẨU:** Sao chép và lưu ngay mật khẩu này vào trình quản lý mật khẩu an toàn (như 1Password, Bitwarden).  
     > **Supabase KHÔNG hiển thị lại mật khẩu văn bản thuần sau khi tạo.**  
     > **TUYỆT ĐỐI KHÔNG:** Gửi mật khẩu vào tin nhắn chat, không gửi email, không commit lên Git!
   - **Region:** Chọn khu vực địa lý gần học sinh nhất (khuyến nghị **Singapore - `ap-southeast-1`** cho người dùng tại Việt Nam).
   - **Pricing Plan:** Chọn gói **Free** phục vụ chuẩn bị và diễn tập.
3. Bấm **Create new project** và đợi cụm máy chủ hoàn tất khởi tạo.

### Bước 3: Lấy chuỗi kết nối từ Dashboard Connect
> [!IMPORTANT]
> **LƯU Ý THEO TÀI LIỆU CHÍNH THỨC CỦA SUPABASE:**  
> Host của Connection Pooler (Supavisor) **KHÔNG THỂ tự suy đoán hay ghép từ tên vùng (Region)**.  
> Người dùng **BẮT BUỘC phải sao chép chính xác Host và Username từ Dashboard Connect**.

1. Tại Dashboard của dự án, bấm nút **Connect** ở thanh điều hướng trên cùng (hoặc vào **Project Settings** -> **Database**).
2. Trong hộp thoại kết nối, chọn tab **Connection String** -> định dạng **URI**.
3. Chọn phương thức kết nối:
   - **Khuyến nghị cho FastAPI:** Chọn **Connection Pooling** -> chế độ **Session mode** (Cổng `5432`).
   - *Lý do:* Chế độ Session (Port 5432) hỗ trợ tương thích đầy đủ với các giao thức chuẩn của PostgreSQL, hỗ trợ prepared statements, và kết nối qua địa chỉ IPv4 thông qua Supavisor pooler.
4. Định dạng chuỗi kết nối nhận được có cấu trúc như sau (chú ý: đây là placeholder minh họa cấu trúc):
   ```text
   postgresql://[USER-FROM-DASHBOARD]:[PASSWORD-PERCENT-ENCODED]@[HOST-FROM-DASHBOARD]:5432/[DB-NAME]?sslmode=require
   ```

### Bước 4: Quy tắc mã hóa mật khẩu (Password Percent-Encoding)
Nếu mật khẩu cơ sở dữ liệu của bạn chứa các ký tự đặc biệt được chuẩn URI bảo lưu (RFC 3986) như `@`, `:`, `/`, `?`, `#`, `[`, `]`, `%`, `!`, `&`, `$`, `'`, `(`, `)`, `*`, `+`, `,`, `;`, `=`, bạn **BẮT BUỘC phải mã hóa URL percent-encoding** cho mật khẩu trước khi đưa vào chuỗi kết nối:
- `@` đổi thành `%40`
- `:` đổi thành `%3A`
- `#` đổi thành `%23`
- `%` đổi thành `%25`
- `!` đổi thành `%21`
- `/` đổi thành `%2F`

Nếu không mã hóa, driver kết nối của PostgreSQL sẽ phân tích sai cú pháp và báo lỗi xác thực hoặc lỗi định dạng host.

### Bước 5: Cấu hình tệp tham khảo
- Tệp mẫu cấu hình được lưu tại: `backend/supabase.env.example` (xem chi tiết tệp này; tệp được đặt tên rõ ràng để Git theo dõi, không bị chặn bởi quy tắc `**/.env.*` trong `.gitignore`).
- **Lưu ý:** Không thêm biến `SUPABASE_DATABASE_URL` vào tệp `.env.example` chính thức của backend hiện tại vì runtime hiện hành không hỗ trợ và sẽ gây hiểu nhầm cho lập trình viên.

---

## 3. Bảng Kiểm Kê Chi Tiết Khác Biệt SQL & Mã Nguồn Cần Chuyển Đổi

Kiểm kê thực tế trực tiếp từ mã nguồn `backend/aimentor/`: Toàn bộ CSDL hiện có gồm đúng **26 bảng** và **8 chỉ mục tuỳ chỉnh**.

### 3.1. Danh mục 26 bảng trong `backend/aimentor/database.py`
1. `users`: Tài khoản người dùng (id autoincrement, username/email NOCASE, password_hash, role, rank_tier).
2. `sessions`: Phiên đăng nhập người dùng (access_hash, refresh_hash, expires, revoked).
3. `resources`: Tài nguyên chung (Dự án, Hội thoại chat, Tin nhắn, Metadata tệp đính kèm).
4. `enrollments`: Đăng ký môn học theo khối lớp 10 - 12.
5. `attempts`: Phiên làm bài tập lộ trình và bài kiểm tra đầu vào.
6. `completions`: Kết quả hoàn thành bài học, tích lũy điểm EXP.
7. `awards`: Ghi nhận phần thưởng theo môn học và đơn vị bài học.
8. `learning_days`: Nhật ký ngày học duy trì streak.
9. `friendships`: Quan hệ kết bạn hai chiều.
10. `events`: Bảng log sự kiện hỗ trợ Server-Sent Events (SSE).
11. `idempotency`: Bảng kiểm soát tính bất biến khi gửi lại yêu cầu API.
12. `recovery_tokens`: Token đặt lại mật khẩu người dùng.
13. `rate_limits`: Kiểm soát tần suất yêu cầu chống lạm dụng.
14. `badges`: Danh mục huy hiệu theo tháng.
15. `user_badges`: Huy hiệu người dùng đã mở khóa.
16. `friend_streaks`: Chuỗi ngày cùng học của đôi bạn bè.
17. `questions`: Ngân hàng câu hỏi trắc nghiệm (môn, lớp, chương, đáp án A-D, lời giải).
18. `library_quizzes`: Bộ đề ôn tập thư viện câu hỏi.
19. `practice_sessions`: Phiên làm bài luyện tập thư viện.
20. `wrong_questions_notebook`: Sổ tay ghi chép câu hỏi làm sai.
21. `role_audit_logs`: Lịch sử kiểm toán thay đổi quyền hạn người dùng.
22. `exams`: Đề thi khảo thí (Giữa kỳ, Cuối kỳ).
23. `exam_sessions`: Phiên làm bài thi chính thức.
24. `ai_practice_sessions`: Phiên luyện tập tạo tự động bởi AI.
25. `ai_question_reports`: Báo cáo phản hồi câu hỏi AI sai sót.
26. `ai_provider_operations`: Quản lý hạn ngạch và tính bất biến khi gọi AI.

### 3.2. Bảng đối chiếu kỹ thuật SQLite vs PostgreSQL

| Hạng mục SQLite hiện tại | Vị trí trong mã nguồn | Cú pháp chuyển đổi tương ứng trên PostgreSQL |
| :--- | :--- | :--- |
| **AUTOINCREMENT** | `database.py` (bảng `users.id`, `events.seq`) | `BIGSERIAL PRIMARY KEY` hoặc `GENERATED ALWAYS AS IDENTITY`. |
| **Lấy ID vừa tạo (`lastrowid`)** | `auth.py:35` (`owner = row.lastrowid`) | Thêm mệnh đề `RETURNING id` vào câu lệnh `INSERT INTO users ... RETURNING id;`. |
| **Placeholder tham số** | Tất cả các file trong `backend/aimentor/` (dùng dấu `?`) | PostgreSQL driver dùng `$1, $2, ...` (đối với `asyncpg`) hoặc `%s` (đối với `psycopg/psycopg2`). |
| **Transaction Concurrency** | `database.py:23`, `manage_roles.py:110` (`BEGIN IMMEDIATE`); `main.py`, `overview.py` (`BEGIN`) | Dùng khối giao dịch chuẩn PostgreSQL: `BEGIN;` với isolation level phù hợp (hoặc `SELECT ... FOR UPDATE` khi cần khóa dòng chống xung đột). |
| **PRAGMA commands** | `database.py` (`foreign_keys=ON`, `journal_mode=WAL`, `user_version=1`, `table_info`) | PostgreSQL luôn bật Foreign Keys mặc định. Bỏ WAL PRAGMA. Quản lý schema bằng migration tool chuẩn thay vì PRAGMA. |
| **Kiểm tra schema nội tại** | `manage_roles.py:49` (`sqlite_master`) | Truy vấn bảng chuẩn của Postgres: `information_schema.tables` hoặc `pg_catalog.pg_tables`. |
| **Không phân biệt hoa thường (Collation)** | `users` (`COLLATE NOCASE`), `auth.py:43`, `manage_roles.py:115` | Dùng kiểu dữ liệu `CITEXT` (qua extension `CREATE EXTENSION IF NOT EXISTS citext;`) hoặc hàm chuẩn hóa `LOWER(username) = LOWER($1)`. |
| **INSERT OR IGNORE** | `learning.py:377` (`awards`), `learning.py:381` (`learning_days`) | Chuyển thành cú pháp chuẩn PostgreSQL: `INSERT INTO ... ON CONFLICT (...) DO NOTHING;`. |
| **INSERT OR REPLACE** | `library.py:633, 655` (`questions`, `library_quizzes`), `exam.py:168` (`idempotency`) | Chuyển thành: `INSERT INTO ... ON CONFLICT (...) DO UPDATE SET ...;`. |
| **Lớp biệt lệ (Exception Classes)** | `auth.py:38` (`sqlite3.IntegrityError`), `manage_roles.py:74` (`sqlite3.DatabaseError`) | Chuyển sang bắt các ngoại lệ của PostgreSQL driver (ví dụ: `asyncpg.exceptions.UniqueViolationError`, `psycopg.errors.UniqueViolation`). |
| **Kiểu dữ liệu thời gian & JSON** | `created_at REAL NOT NULL` (lưu Unix epoch float `time.time()`), `answers TEXT` | PostgreSQL có thể giữ `DOUBLE PRECISION` để tương thích logic code hiện hành, hoặc chuyển đổi sang `TIMESTAMPTZ` và `JSONB` trong bước chuẩn hóa sau. |

---

## 4. Phân Tách Dữ Liệu Tệp Đính Kèm (`backend/data/uploads`)

- **Thực tế lưu trữ hiện tại:**
  - Bảng cơ sở dữ liệu `resources` (với `kind = 'ATTACHMENT'`) chỉ lưu trữ siêu dữ liệu (metadata) dưới dạng JSON trong cột `body` (tên, kích thước, mimeType, mã sha256).
  - Tệp nhị phân gốc (binary `.bin`) được lưu trữ trực tiếp trên hệ thống tập tin máy chủ tại:
    ```text
    backend/data/uploads/<user_id>/<attachment_id>.bin
    ```
  - Khi tải tệp, FastAPI đọc trực tiếp từ ổ cứng bằng `FileResponse` (`main.py:641`).
- **Nguyên tắc khi chuyển sang Supabase:**
  - **Quá trình di chuyển CSDL sang Supabase Postgres CHỈ di chuyển dữ liệu bảng metadata trong `resources`, HOÀN TOÀN KHÔNG di chuyển hay đụng chạm các file vật lý trong thư mục `backend/data/uploads`**.
  - Server FastAPI tiếp tục đọc/ghi thư mục `uploads` trên ổ cứng máy chủ như hiện tại.
  - Không tự ý chuyển đổi sang Supabase Storage khi chưa có yêu cầu và thiết kế riêng.

---

## 5. Kế Hoạch Kỹ Thuật Trong Tương Lai (Future Engineering Work)

> [!WARNING]
> **LÀM RÕ HIỆN TRẠNG:**  
> Mã nguồn hiện tại của AI Mentor (`Settings.from_env` và `Database`) **HOÀN TOÀN KHÔNG đọc biến môi trường `SUPABASE_DATABASE_URL`**. Việc cấu hình biến này lúc này sẽ không có bất kỳ tác động nào đến ứng dụng.  
> Toàn bộ nội dung bên dưới là **kế hoạch kỹ thuật cần triển khai trong tương lai** khi có quyết định chính thức chuyển đổi sang Supabase.

### 5.1. Các hạng mục kỹ thuật cần phát triển trước khi chuyển đổi
1. **Xây dựng PostgreSQL Adapter:** Viết lớp adapter CSDL mới (thay thế hoặc mở rộng `Database` trong `backend/aimentor/database.py`) sử dụng thư viện chuẩn (`asyncpg` hoặc `psycopg`), hỗ trợ connection pool, quản lý transaction (`async with` hoặc context manager), và chuyển đổi placeholder tham số từ `?` sang `$1` / `%s`.
2. **Xây dựng bộ test suite hồi quy trên PostgreSQL:** Thiết lập môi trường test chạy tự động trên instance PostgreSQL thực tế để kiểm chứng hành vi locking, transaction isolation, và ánh xạ ngoại lệ.
3. **Áp dụng công cụ quản lý Schema (Migration Tool):** Sử dụng Alembic hoặc công cụ di trú tương đương để quản lý các phiên bản schema có khả năng nâng cấp (upgrade) và hạ cấp (downgrade) tin cậy, thay thế việc kiểm tra PRAGMA khi khởi động.
4. **Kiểm chứng Schema thực tế (Schema Validation Precondition):** Tệp tham chiếu `docs/backend/supabase_schema_reference.sql` hiện tại chỉ là bản thảo cấu trúc sơ bộ chưa qua kiểm chứng và tuyệt đối không được tự ý chạy (must not be run). Bắt buộc phải thực thi và kiểm chứng tính tương thích trên một instance PostgreSQL thật trước khi áp dụng.
5. **Rà soát An toàn thông tin (Security Review):** Kiểm tra cấu hình RLS và quyền truy cập schema để đảm bảo không để lộ dữ liệu học sinh ra Internet qua Supabase PostgREST API.

### 5.2. Trình tự thực hiện an toàn (Safe Sequence)
1. **Sao lưu toàn diện (Offline Backup):** Tạo bản sao lưu toàn vẹn của `aimentor.sqlite3` và toàn bộ thư mục `backend/data/uploads`.
2. **Khởi tạo Schema trên staging PostgreSQL:** Chạy script schema đã qua kiểm chứng lên một instance PostgreSQL thử nghiệm.
3. **Diễn tập di chuyển dữ liệu (Rehearsal Migration):** Viết script ETL chuyển toàn bộ dữ liệu từ file SQLite sao lưu sang PostgreSQL staging.
4. **Kiểm thử bất biến & toàn vẹn:**
   - Đối chiếu số lượng bản ghi (Row Counts) từng bảng giữa SQLite và Postgres.
   - Kiểm tra toàn vẹn khóa ngoại (Foreign Key Invariants).
   - Đồng bộ giá trị sequence (`setval`) cho các cột `BIGSERIAL`.
   - Kiểm tra mã hóa ký tự tiếng Việt (UTF-8) trên nội dung đề thi, câu hỏi và đáp án.

### 5.3. Chuyển đổi chính thức (Cutover) & Kế hoạch Rollback Thực Tế
- **Thời điểm chuyển đổi:** Thực hiện trong khung giờ bảo trì, tạm dừng nhận yêu cầu ghi vào SQLite.
- **RỦI RO ROLLBACK & DỮ LIỆU PHÁT SINH (POST-CUTOVER WRITE LOSS):**
  > [!CAUTION]
  > **KHÔNG CÓ KHÁI NIỆM "ZERO-LOSS ROLLBACK" TỰ ĐỘNG:**  
  > Sau khi chuyển đổi sang PostgreSQL, nếu người dùng bắt đầu thực hiện các giao dịch ghi mới (đăng ký tài khoản mới, hoàn thành bài học, tích lũy EXP, nộp đề thi), **các dữ liệu này chỉ nằm trên PostgreSQL**.  
  > Nếu xảy ra sự cố và phải quay về (Rollback) file SQLite cũ, các dữ liệu phát sinh trên PostgreSQL này sẽ **KHÔNG TỰ ĐỘNG ĐƯỢC ĐỒNG BỘ NGƯỢC LẠI SQLite**.  
  > Do đó, đội ngũ kỹ thuật phải chuẩn bị một trong hai phương án:
  > - **Phương án A (Khuyến nghị):** Xây dựng sẵn script đối soát và đồng bộ ngược (Reverse-sync / Reconciliation script) để bù đắp các giao dịch đã ghi trên Postgres về SQLite trước khi hoàn tất rollback.
  > - **Phương án B:** Có sự chấp thuận trước của người dùng về việc chấp nhận mất mát phần dữ liệu ghi phát sinh trong khoảng thời gian thử nghiệm sau cutover nếu bắt buộc phải rollback khẩn cấp.

---

## 6. Chính Sách Bảo Mật (Security Policy)

1. **Bảo vệ chứng thực phía Server (Server-Side DB Credentials Only):**
   - Thông tin kết nối CSDL và các khóa mật vụ chỉ được cấu hình duy nhất trên máy chủ backend FastAPI.
   - Mã nguồn ứng dụng Android (`app/`) và trang web quản trị (`admin_web.py`) **TUYỆT ĐỐI KHÔNG** được chứa chuỗi kết nối Postgres, database password, hay Supabase `service_role` key.
2. **Kiểm soát Supabase Data API (PostgREST) & Row Level Security (RLS):**
   - Mặc định, Supabase tự động phơi bày các bảng trong schema `public` thành REST API qua PostgREST.
   - **Chính sách bắt buộc:**
     - Hoặc đưa toàn bộ bảng vào schema riêng (ví dụ `aimentor.*`) không bị PostgREST quét.
     - Hoặc nếu giữ trong schema `public`, **BẮT BUỘC bật Row Level Security (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY;`)** và thực thi `REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon, authenticated;`.
     - **NGUYÊN TẮC BẤT DI BẤT DỊCH: TUYỆT ĐỐI KHÔNG BAO GIỜ mở policy đọc công khai (`publicly readable`) cho các bảng chỉ để vượt qua kiểm thử!**

---

## 7. Trạng Thái Hiện Tại & Giới Hạn Kiểm Thử Ngoại Tuyến (Offline Status)

- **Trạng thái thực tế:** AI Mentor hiện tại vẫn sử dụng **100% SQLite cục bộ**. Chưa có dự án Supabase nào được kết nối vào luồng chạy chính.
- **Những gì ĐÃ được kiểm chứng ngoại tuyến (Offline Verified):**
  - Schema SQLite 26 bảng nguyên vẹn và các chỉ mục hoạt động ổn định.
  - Toàn bộ 189 bài kiểm thử hồi quy backend SQLite hiện có (Auth, Học tập, Khảo thí, Quản trị, Luyện tập AI chế độ unconfigured/fake) chạy thành công độc lập.
  - Phân tách độc lập giữa thư mục lưu trữ file `uploads` và cơ sở dữ liệu.
- **Những gì CHƯA THỂ kiểm tra cho đến khi có cơ sở dữ liệu PostgreSQL thật:**
  - Độ trễ mạng (Network Latency), bắt tay bảo mật SSL qua Internet tới Supabase.
  - Tác động hiệu năng khi kết nối qua PgBouncer/Supavisor session pooler.
  - Hành vi khóa dòng và transaction isolation thực tế trên PostgreSQL cụm đám mây.
  - Tính hợp lệ cú pháp DDL và kiểu dữ liệu của tệp tham khảo `supabase_schema_reference.sql`.
