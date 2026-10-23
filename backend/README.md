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

