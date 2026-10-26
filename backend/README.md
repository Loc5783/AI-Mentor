# Backend AI Mentor — đang triển khai

Backend thật chạy bằng FastAPI và SQLite, tách khỏi cơ sở dữ liệu trên điện thoại. Đây là bản phát triển, chưa phải backend đã nối hoàn chỉnh với Android hay đã triển khai trên Internet.

## Chạy tại máy
Từ thư mục backend:
1. Tạo môi trường Python 3.11 trở lên: python -m venv .venv
2. Cài: .venv/Scripts/python.exe -m pip install -r requirements-lock.txt
3. Chạy: .venv/Scripts/python.exe -m uvicorn aimentor.main:app --host 127.0.0.1 --port 18080
4. API tương tác tại http://127.0.0.1:18080/docs ; trạng thái tại /health.

Dữ liệu mặc định trong backend/data, bị loại khỏi Git. MENTOR_DATA_DIR chọn thư mục dữ liệu khác. MENTOR_DEMO_MODE=1 tạo ba tài khoản mẫu student 01 / student 02 / student 03, mật khẩu 123123123. Mặc định không tạo demo.

## Quản trị phân quyền tài khoản (Local Role Management)
Mặc định mọi tài khoản đăng ký mới luôn là `STUDENT`. Không cho phép tự nâng quyền qua API.

Để nâng quyền Quản trị / Biên soạn (`ADMIN` hoặc `TEACHER`) cho tài khoản local:
1. Xem trước thay đổi (Dry-run):
   `.\.venv\Scripts\python.exe -m aimentor.manage_roles --data-dir data --user admin@example.com --role ADMIN`
2. Áp dụng thay đổi vào cơ sở dữ liệu:
   `.\.venv\Scripts\python.exe -m aimentor.manage_roles --data-dir data --user admin@example.com --role ADMIN --apply`
3. Hạ quyền tài khoản về `STUDENT`:
   `.\.venv\Scripts\python.exe -m aimentor.manage_roles --data-dir data --user admin@example.com --role STUDENT --apply`

*Lưu ý*: Lệnh đổi quyền sẽ tự động thu hồi các phiên làm việc đang chạy của tài khoản đó. Người dùng cần đăng nhập lại từ giao diện Web Admin (`/admin`) để nhận token với quyền hạn mới.

## Module Import Câu hỏi Excel/CSV (Web Admin & API)

Tính năng hỗ trợ người dùng có quyền Quản trị (`ADMIN`) hoặc Biên soạn tải lên ngân hàng câu hỏi trắc nghiệm qua giao diện Web Admin (`/admin`) hoặc API.

### 1. Định dạng file hỗ trợ
- **CSV (.csv)**:
  - Mã hóa UTF-8 hoặc UTF-8 kèm BOM (`utf-8-sig`).
  - Hỗ trợ chuẩn RFC 4180: Bảo toàn dấu xuống dòng (`\n`) và ký tự phân cách nằm trong dấu ngoặc kép (`"..."`).
- **Excel (.xlsx)**:
  - File nhị phân chuẩn Microsoft Excel (`.xlsx`), được xử lý bằng thư viện `openpyxl`.
  - Tự động ưu tiên đọc trang tính (sheet) có tên `Câu hỏi`, `Questions`, hoặc `Sheet1` (hoặc sheet đầu tiên nếu không có các tên trên).
  - Giới hạn kích thước giải nén tối đa 25MB và kiểm tra tỷ lệ nén để chống Zip-bomb.

### 2. File mẫu (Template) và Tiêu đề cột (Headers)
- Tải file mẫu qua endpoint:
  - Excel: `GET /admin/template/excel?format=xlsx` (Mặc định).
  - CSV: `GET /admin/template/excel?format=csv` (UTF-8 BOM tương thích Excel trên Windows).
- **Quy tắc Header**:
  - Dòng đầu tiên (Row 1) bắt buộc là dòng tiêu đề.
  - Chuẩn hóa không phân biệt hoa thường và loại bỏ khoảng trắng thừa.
  - Hỗ trợ cả tên cột tiếng Việt và tiếng Anh.
  - Các cột bắt buộc:
    - `prompt` (Câu hỏi / Nội dung)
    - `optionA`, `optionB`, `optionC`, `optionD` (Đáp án A, B, C, D)
    - `correctOption` (Đáp án đúng: A, B, C hoặc D)
  - Các cột tùy chọn: `code` (Mã câu), `subjectId` (Môn học), `grade` (Khối lớp 10–12), `chapter` (Chương), `topic` (Chủ đề), `textbook` (Bộ sách), `explanation` (Lời giải), `difficulty` (EASY/MEDIUM/HARD), `points` (Điểm 1–100), `source` (Nguồn), `author` (Tác giả), `license` (Bản quyền).

### 3. Giới hạn thực tế và Quy tắc độ dài trường
- **Giới hạn số lượng**:
  - Mỗi file hoặc mỗi lượt import API hỗ trợ tối đa **2000 câu hỏi** (đối với file gồm tối đa 2001 dòng: 1 dòng tiêu đề + 2000 dòng câu hỏi).
  - File rỗng, file chỉ có tiêu đề, hoặc vượt quá 2000 câu hỏi sẽ bị từ chối với thông báo lỗi cụ thể.
- **Giới hạn độ dài trường**:
  - `prompt`: Tối đa 5.000 ký tự (bắt buộc, không được để trống).
  - `optionA`, `optionB`, `optionC`, `optionD`: Mỗi phương án tối đa 2.000 ký tự (bắt buộc, không được để trống).
  - `explanation`: Tối đa 5.000 ký tự (tùy chọn, mặc định `""`).
  - `chapter`: Tối đa 200 ký tự (tùy chọn, mặc định `"Tổng hợp"`).
  - `topic`: Tối đa 200 ký tự (tùy chọn, mặc định `"Chủ đề chung"`).
  - `textbook`: Tối đa 100 ký tự (tùy chọn, mặc định `"Chưa xác định"`).
  - `source`: Tối đa 200 ký tự (tùy chọn, mặc định `"Giáo viên upload"`).
  - `author`: Tối đa 100 ký tự (tùy chọn, mặc định `"Người biên soạn"`).
  - `license`: Tối đa 100 ký tự (tùy chọn, mặc định `"Nội bộ"`).
  - `code`: Tối đa 64 ký tự (tự sinh mã `Q_IMP_...` ngẫu nhiên nếu để trống).
- **Nguyên tắc xử lý**: Hệ thống **tuyệt đối không cắt xén âm thầm (no silent slicing)** nội dung của người dùng. Nếu bất kỳ trường nào vượt quá giới hạn trên dù chỉ 1 ký tự, hệ thống sẽ từ chối dòng đó và báo lỗi rõ ràng tên trường cùng giới hạn ký tự.

### 4. Chính sách xử lý công thức (Formulas)
- Đối với file Excel (`.xlsx`), các ô có kiểu dữ liệu công thức (`cell.data_type == 'f'`) hoặc chuỗi văn bản bắt đầu bằng dấu bằng (`=`) sẽ bị phát hiện và từ chối ngay ở bước preview nhằm ngăn chặn rủi ro Formula Injection trong Excel.
- Các ký tự dấu trừ (`-`) hoặc dấu cộng (`+`) ở đầu văn bản (ví dụ đáp án toán học, vật lý như `-5 m/s` hoặc `+10 V`) được coi là dữ liệu hợp lệ và không bị chặn máy móc.

### 5. Chính sách mã câu hỏi (Question Code Policy)
- **Import chỉ dùng để thêm mới**, không dùng để cập nhật hay ghi đè câu hỏi đã có.
- Trùng mã trong cùng file: Báo lỗi ngay tại dòng trùng lặp ở bước preview.
- Trùng mã với cơ sở dữ liệu: Hệ thống kiểm tra đối chiếu bảng `questions`; nếu mã câu hỏi đã tồn tại, yêu cầu sẽ bị từ chối với lỗi xung đột `409 CODE_CONFLICT` và bảo toàn dữ liệu của câu hỏi cũ (không thay đổi ID, version, status, hoặc prompt).

### 6. Tính nguyên tử toàn batch (Batch Atomicity & Transaction Rollback)
- Thao tác lưu câu hỏi qua `POST /admin/import/confirm` được bảo vệ bằng một database transaction duy nhất.
- Kiểm tra toàn diện trước khi ghi: Nếu có bất kỳ dòng nào không hợp lệ, server trả lỗi `400 VALIDATION_FAILED` (kèm vị trí dòng và mô tả) và không mở transaction ghi.
- Rollback toàn bộ: Nếu xảy ra bất kỳ sự cố bất ngờ nào trong quá trình insert (lỗi disk, lỗi ràng buộc cơ sở dữ liệu), toàn bộ batch sẽ được **Rollback 100%**. Không có tình trạng lưu sót hoặc lưu dở dang một phần câu hỏi vào database.

## Module Kho câu hỏi & Xuất bản (Question Bank & Publishing)

### 1. Phân quyền và Cô lập dữ liệu nháp (Draft Isolation)
- Quyền Admin/Biên soạn: Chỉ tài khoản có `role` là `ADMIN` hoặc `TEACHER` mới có quyền truy cập các endpoint quản trị:
  - `GET /admin/questions` (kèm bộ lọc môn, lớp, trạng thái, độ khó, chương, chủ đề)
  - `POST /admin/questions` (tạo mới hoặc cập nhật câu hỏi)
  - `POST /admin/questions/{qid}/publish` (duyệt xuất bản hoặc ẩn về bản nháp)
  - `GET /admin/quizzes`, `POST /admin/quizzes`, `POST /admin/quizzes/{quiz_id}/publish` (quản trị bộ đề)
- Học sinh (`STUDENT`) và người dùng ẩn danh tuyệt đối bị từ chối với mã lỗi `403 ADMIN_REQUIRED` hoặc `401 AUTHENTICATION_REQUIRED`.
- Học sinh không bao giờ nhận được câu hỏi hoặc bộ đề ở trạng thái `DRAFT` qua bất kỳ endpoint thư viện nào (`/library/quizzes`, `/library/quizzes/{quiz_id}`, `/library/sessions`). Đáp án đúng và lời giải được ẩn hoàn toàn trước khi học sinh gửi câu trả lời.

### 2. Quy tắc Mã câu hỏi, ID và Phiên bản (Question Versioning)
- Thao tác cập nhật câu hỏi giữ nguyên `id` gốc và cập nhật `updated_at`.
- Trường `created_at` không bị thay đổi khi cập nhật.
- Mỗi lần nội dung câu hỏi được chỉnh sửa hợp lệ, `version` tự động tăng 1 (`version = version + 1`). Thao tác xuất bản/ẩn nháp (`publish`) mang tính idempotent và không tự ý thay đổi hoặc reset `version`.
- Nếu cập nhật câu hỏi mà không truyền `code`, hệ thống bảo toàn `code` hiện có, không tự sinh mã ngẫu nhiên.
- Xung đột mã câu hỏi với câu hỏi khác lập tức bị chặn với lỗi `409 CODE_CONFLICT`.
- Cập nhật với `id` không tồn tại trả về lỗi `404 QUESTION_NOT_FOUND`, không âm thầm tạo câu mới.
- Không cho phép xuất bản câu hỏi có dữ liệu không hợp lệ (trả về `400 VALIDATION_FAILED`).

### 3. Bảo toàn Phiên học và Lịch sử làm bài (Session Snapshot & Grading Integrity)
- Khi học sinh bắt đầu phiên luyện tập (`POST /library/sessions`), hệ thống chụp một bản snapshot độc lập toàn diện (`question_snapshots`) của các câu hỏi tại thời điểm đó (bao gồm nội dung câu hỏi, các phương án, đáp án đúng, điểm và lời giải).
- Việc giáo viên/quản trị viên sau đó chỉnh sửa nội dung, đổi đáp án, đổi điểm hoặc unpublish câu hỏi trong kho câu hỏi **tuyệt đối không làm thay đổi kết quả chấm điểm hoặc nội dung hiển thị của phiên đang làm cũng như các phiên lịch sử đã hoàn thành**.
- Học sinh làm bài trên phiên v1 luôn được chấm điểm theo đáp án và biểu điểm của v1; xem lại lịch sử phiên luôn hiển thị đúng nội dung và lời giải v1.

### 4. Kết nối từ Câu hỏi đã Xuất bản đến Thư viện học sinh (Quiz Management Pipeline)
- Cung cấp API và giao diện Web Admin quản lý bộ đề cho giáo viên/quản trị viên:
  - `POST /admin/quizzes`: Tạo hoặc cập nhật bộ đề (`title`, `subjectId`, `grade`, `chapter`, `topic`, `difficulty`, `questionIds`, `status`).
  - `POST /admin/quizzes/{quiz_id}/publish`: Xuất bản hoặc ẩn bộ đề (`publish=true/false`).
  - `GET /admin/quizzes`: Liệt kê danh sách bộ đề (kèm bộ lọc môn, lớp, trạng thái).
  - `GET /admin/quizzes/{quiz_id}`: Chi tiết bộ đề kèm danh sách `questionIds`.
- **Ràng buộc xuất bản nghiêm ngặt**:
  - Không thể xuất bản bộ đề rỗng hoặc bộ đề chứa bất kỳ câu hỏi nào ở trạng thái `DRAFT` (trả về lỗi `400 DRAFT_QUESTIONS_NOT_ALLOWED`).
  - Danh sách câu hỏi trong bộ đề không được chứa trùng lặp ID (từ chối `400 DUPLICATE_QUESTION_IDS`).
  - Tự động hạ nháp (Auto-demotion): Khi một câu hỏi thuộc bộ đề đã xuất bản bị chuyển về trạng thái `DRAFT`, mọi bộ đề đã xuất bản chứa câu hỏi đó sẽ tự động bị hạ trạng thái về `DRAFT` và lập tức dừng cung cấp cho học sinh tạo phiên mới.
  - Học sinh gọi Thư viện (`GET /library/quizzes`, `GET /library/quizzes/{quiz_id}`) hoặc tạo phiên (`POST /library/sessions`) trên bộ đề không đủ điều kiện (chứa câu hỏi nháp/không tồn tại/lệch môn lớp) sẽ nhận lỗi `404 QUIZ_NOT_FOUND` hoặc `400 QUIZ_NOT_AVAILABLE`, tuyệt đối không âm thầm cắt bớt số câu hỏi của bộ đề.

### 5. Tính nhất quán Môn học, Khối lớp và Bảo vệ thành viên bộ đề
- Môn học và khối lớp giữa bộ đề và tất cả các câu hỏi thành viên phải hoàn toàn đồng nhất (hỗ trợ chuẩn hóa alias: `physics`/`vat ly`, `math`/`toan`, v.v.; khối lớp 10, 11, 12).
- Ngăn chặn xung đột bộ đề khi chỉnh sửa câu hỏi: Nếu một câu hỏi đang là thành viên của bất kỳ bộ đề đã xuất bản (`PUBLISHED`), thao tác cập nhật câu hỏi nhằm thay đổi `subjectId` hoặc `grade` sẽ bị hệ thống từ chối ngay với mã lỗi `409 CONFLICT_QUIZ_MEMBERSHIP`, bảo vệ tính toàn vẹn của các bộ đề đang phục vụ học sinh.

### 6. Xử lý trung thực Phiên học cũ thiếu Snapshot (Legacy Session Fallback)
- Đối với phiên học cũ đã hoàn thành (`COMPLETED`) thiếu dữ liệu snapshot:
  - Bảo toàn 100% kết quả chấm thi gồm `isCorrect` và `score`.
  - Thiết lập `correctOption = None` và hiển thị thông báo trung thực: `[Phiên học lịch sử: Câu hỏi không có bản lưu snapshot tại thời điểm làm bài. Để bảo toàn tính trung thực của kết quả làm bài, đáp án và lời giải chi tiết được ẩn và không tra cứu ngược câu hỏi hiện tại]`.
  - Tuyệt đối không tra cứu ngược vào bảng câu hỏi hiện tại (`questions`) để tránh sinh ra hiện tượng mâu thuẫn thị giác (ví dụ: làm đúng 10 điểm đáp án A nhưng lại hiển thị đáp án đúng hiện tại là B).
- Đối với phiên học cũ đang làm dở (`IN_PROGRESS`) thiếu snapshot:
  - Khi học sinh nộp câu trả lời, hệ thống trả về lỗi rõ ràng `400 LEGACY_SESSION_REQUIRES_RESTART` yêu cầu bắt đầu phiên làm bài mới có snapshot đầy đủ, ngăn chặn việc chấm điểm sai lệch với nội dung câu hỏi bị chỉnh sửa.

### 7. Giao diện Quản trị Web Admin Bộ đề (`/admin`)
- Mục "📚 Quản lý bộ đề" tích hợp trên thanh điều hướng Web Admin.
- Bộ lọc danh sách đa chiều: Môn học, Khối lớp, Trạng thái (Đã xuất bản / Bản nháp).
- Form modal Tạo/Chỉnh sửa bộ đề:
  - Nhập tiêu đề, môn, lớp, chương, chủ đề, độ khó.
  - Bộ chọn câu hỏi trực quan theo checkbox tự động tải ứng viên câu hỏi theo Môn và Lớp đã chọn, hiển thị kèm mã câu, độ khó, trạng thái `PUBLISHED` / `DRAFT` và đếm số lượng câu hỏi đã chọn thời gian thực.
  - Phím lưu linh hoạt: "Lưu bản nháp (Draft)" hoặc "Xuất bản ngay (Publish)".
  - Nút chuyển đổi nhanh trạng thái xuất bản ("🚀 Xuất bản" / "Ẩn bản nháp") trực tiếp trên thẻ từng bộ đề.
  - Cơ chế phòng thủ: Khóa nút bấm chống double-click (`isSubmittingQuiz`, `isPublishingQuiz`), xử lý lỗi chi tiết từ backend qua `parseApiErrorMessage`, và hiển thị an toàn chống mã độc XSS.

## Đã triển khai ở bước nền tảng
- Đăng ký/đăng nhập, hash scrypt có salt; token ngẫu nhiên chỉ lưu hash, access hết hạn, refresh quay vòng, đăng xuất và đổi mật khẩu thu hồi phiên.
- Hồ sơ riêng/công khai tách biệt; lớp 10/11/12, biệt danh/ghi chú/ảnh.
- Thư mục/chat/tin nhắn có ownership, revision/If-Match và idempotency.
- Upload ảnh/PDF/TXT có giới hạn; ảnh được xác thực bằng Pillow, đọc tệp có kiểm tra account.
- Kết bạn có lời mời/xác nhận; ranking streak/EXP ngày đọc ledger server, không xếp theo level.
- Event stream SSE có cursor, kiểm tra phiên và sự kiện riêng theo account. Dữ liệu sự kiện không có nội dung chat, mật khẩu hay API key.

## Chưa hoàn tất
- Mở rộng và giáo viên duyệt nội dung ba môn cho lớp 10–12; hiện có 20 bài ngắn/60 câu/4 review cho mỗi môn lớp 10, không phải toàn bộ năm học.
- Đồng bộ Android, migration account/chat cũ, outbox offline và xử lý xung đột nhiều thiết bị.
- Gửi email reset/xác minh bằng SMTP/provider thật; không giả gửi email.
- Dọn tệp sau xóa, lưu trữ dài hạn/pagination lớn, retention cursor và hoàn thiện kiểm thử tải.

## Engine học tập đã có
- Enrollment theo account/môn/lớp; đánh giá đầu vào nhập môn 10 câu, không trao EXP/streak và không gọi là đánh giá cả năm học.
- Lộ trình phải hoàn thành lần lượt; mỗi năm bài có quiz ôn đúng năm bài trước, 10 hoặc 15 câu theo gói nội dung. Phải qua checkpoint trước bài thứ sáu.
- Lưu đáp án/tiến độ có If-Match; submissionId chống đếm lại lần sai khi gửi lại. Hai lần sai có lời giải, kể cả hai lần chọn cùng một phương án với hai submissionId khác nhau.
- Ledger server cấp 200 EXP/bài, 400 EXP/review chỉ ở lần đầu; retake 0 EXP vẫn thắp streak ngày mới. Completion cũ phát lại không tạo ngày học hay phần thưởng mới.
- Handoff/resume giữ attempt, câu hỏi và phiên bản nội dung; quay lại đọc tiến độ mới nhất, không ghi đè từ snapshot cũ.
- Question bank các câu đã luyện, phân loại môn/lớp/bài/phiên bản để tải về ôn offline. Ngân hàng câu Gemini tạo trong module cũ chưa đồng bộ vào bank server này.

Android có màn lộ trình/làm bài mới dùng API; cấu hình `MENTOR_BACKEND_URL` (HTTPS) trong api.properties rồi rebuild. Khi trống, app dùng luồng cục bộ và báo chưa có máy chủ ở lộ trình mới, không tính thưởng giả.

## Tài khoản và thống kê chung — 19/10/2026
- Khi có backend, đăng nhập/đăng ký ở màn chính tạo phiên dùng chung cho Lesson, không yêu cầu đăng nhập đồng bộ riêng. Chỉ yêu cầu xác nhận lại nếu phiên hết hiệu lực; không cho đổi sang remote identity khác trong một bài đang học.
- Mapping identity theo endpoint + remote ID. Chỉ liên kết dữ liệu tài khoản local khi xác thực thành công cả mật khẩu local và tài khoản server; trùng email/biệt danh không đủ để gộp dữ liệu.
- Đăng xuất xóa credential trên thiết bị ngay, thu hồi phiên cũ best effort; một tác vụ đăng xuất chạy trễ không xóa phiên mới. Refresh được khóa chung theo account/endpoint giữa các client.
- `GET /me/dashboard?grade=10|11|12`: snapshot nhất quán của ledger, EXP tổng/ngày, streak, bài/review đã hoàn thành, thống kê từng môn, bảy ngày gần nhất, câu đúng lần đầu và câu có hướng dẫn.
- Completion, Dashboard, public profile và ranking dùng chung `learning_stats.reward_summary`, UTC+7 và learning clock. Ranking chốt một mốc giờ/snapshot cho toàn bộ danh sách, tránh trộn ngày khi đi qua nửa đêm.
- Dashboard, số liệu cá nhân trong Profile và Thi đua đọc cùng nguồn. Có cache theo local user/endpoint/remote ID/grade; mất kết nối hiển thị rõ bản đã lưu, không quay về EXP legacy hoặc dựng số 0.
- App mới dùng chín môn học; dữ liệu bốn module cũ trên thiết bị bị xóa một lần theo yêu cầu. Không nhập hoặc cộng EXP cũ vào backend.
- Android đã nối SSE để làm mới summary, bảng thi đua và hồ sơ công khai đang xem: cursor trước refresh, reconnect/backoff, gộp sự kiện và hủy theo lifecycle/account. Bảng dùng rank server và hai trục STREAK/DAILY_XP × FRIENDS/GLOBAL; chỉ bạn ACCEPTED được đưa vào FRIENDS. Ảnh công khai tải qua endpoint cùng origin có xác thực và giới hạn dung lượng/kích thước decode. Đây chưa phải sync toàn bộ app; UI quản lý lời mời, profile editor/onboarding, lesson state trên thiết bị khác, chat cloud và outbox offline còn phải nối.

Sáu môn ngoài Sinh/Hóa/Lý chỉ là khung. /health phân biệt engine đã triển khai với publishedCourses (hiện chỉ ba gói lớp 10, mỗi gói 20 bài); không đồng nghĩa Android đã nối realtime hay đủ nội dung lớp 11–12.

## Nguồn kỹ thuật
https://fastapi.tiangolo.com/tutorial/security/
https://fastapi.tiangolo.com/advanced/custom-response/
Giao diện hiện vẫn dùng lưu trữ local cho các phần chưa nối backend. Khi triển khai thật cần URL HTTPS, cấu hình hạ tầng và kiểm thử lại toàn luồng; không dùng token/API key thật trong Git.

