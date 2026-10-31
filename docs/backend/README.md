# Hợp đồng API AI Mentor

- `runtime-openapi.json`: đặc tả của backend đang triển khai. Khi sửa route/schema, xuất lại từ `create_app(Settings(...)).openapi()`.
- `openapi.json`: bản thiết kế gốc được giữ để nhóm đối chiếu, không phải bằng chứng chức năng đã chạy; ưu tiên runtime khi có khác biệt.
- Hướng dẫn khởi động: [backend/README.md](../../backend/README.md).
- Tồn đọng và bàn giao: [BAN_GIAO_BACKEND.txt](../../app/BAN_GIAO_BACKEND.txt).

Backend xác thực bằng access/refresh token và kiểm tra ownership; không lấy userId client gửi để cấp quyền. API key Gemini không được đưa vào payload hay sự kiện.

Reward ledger quyết định 200 EXP/bài, 400 EXP/quiz, chỉ một lần mỗi đơn vị học. Completion, EXP ngày, streak và bảng thi đua dùng cùng ngày UTC+7. Quiz ôn phải phủ đúng 5 bài trước; bản thân client không tự cấp thưởng.

Sửa/xóa dùng revision/If-Match; tạo và hoàn thành dùng idempotency. SSE chỉ phát sau commit, giới hạn dữ liệu theo quyền truy cập. Hồ sơ công khai không chứa email, mật khẩu hay token.

Chín môn đều có gói nội dung lớp 10: Lý–Hóa–Sinh có 20 bài ngắn mỗi môn; Toán, Văn, Anh, Sử, Địa và GDKT & Pháp luật có 15 bài nhập môn mỗi môn. Các gói chưa phủ hết chương trình năm học; lớp 11–12 chưa xuất bản. Reset local một lần cho app mới không xóa dữ liệu backend.
