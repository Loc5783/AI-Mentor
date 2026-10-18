# Hợp đồng backend AI Mentor

**PHẠM VI MỚI NHẤT:** chỉ triển khai luồng Lesson đầy đủ cho Sinh học, Hóa học và Vật lý trong đợt này. Sáu môn còn lại giữ khung module, chưa có bài/đầu vào/quiz mới; UI phải ghi Sắp có và không mở luồng rỗng. Chat AI chung vẫn hỗ trợ tất cả môn theo lớp 10/11/12. Các đoạn mô tả 9 môn là kiến trúc mở rộng, không phải cam kết có đủ nội dung 9 môn ở đợt hiện tại.

Trạng thái: DRAFT 0.1.0; không có dịch vụ đã triển khai. `openapi.json` là OpenAPI 3.0.3, gồm 34 đường dẫn, 47 thao tác và 51 schema. Đã kiểm tra JSON, liên kết schema, tham số đường dẫn và operationId; chưa chạy bộ kiểm định OpenAPI đầy đủ hoặc kiểm thử với máy chủ.

Đọc quyết định sản phẩm và thứ tự migration trong `../../APP_REDESIGN_2026-10-18.md` trước khi triển khai. Các phần này thay thế hướng dẫn cũ nếu xung đột: 9 môn, 5 tab, streak từ bài học và quiz, 200/400 EXP, quiz sau mỗi 5 bài, xếp hạng streak/EXP ngày.

## Quy tắc không được bỏ qua
- JWT subject quyết định ownership. Không lấy userId do client gửi để cấp quyền đọc/sửa.
- PublicProfile không có email, tên đăng nhập, mật khẩu hay thông tin phiên.
- Completion ledger có UNIQUE(attempt_id) và idempotency: retry cùng kết quả, không cộng thêm 200/400 EXP. Ghi reward, streak, daily EXP trong một transaction.
- Quiz ôn tập có đúng 5 sourceLessonIds ngay trước nó và 10/15 câu. Server xác minh unlock/coverage/hoàn thành; client không tự gửi số EXP được nhận.
- Ngày học thống nhất theo Asia/Ho_Chi_Minh ở giai đoạn đầu; daily EXP trong profile và ranking dùng cùng khoảng ngày. Timezone chỉ đọc, client không tự đổi để lấy thêm streak.
- If-Match/revision cho sửa/xóa; 409/412 phải giải quyết xung đột. Tombstone/SSE chỉ phát sau commit.
- Không upload key Gemini, mật khẩu hay đường dẫn file local trong events/logs/resource.
- Account SQLite hiện tại cần migration/liên kết được xác minh; không gộp theo email chưa xác minh.
- Tệp phải kiểm tra nội dung thật, quyền sở hữu, kích thước và hạn URL. Xóa folder có lựa chọn giữ/xóa chat rõ ràng.

## Chat trực tiếp và đồng bộ là hai phần riêng

Chat vẫn gọi Gemini bằng api.properties và key pool, đúng hướng người dùng chọn. API này lưu/đồng bộ chat và tài nguyên; assistant message nhận từ client phải gắn nguồn DIRECT_GEMINI_CLIENT, không coi là bằng chứng hoàn thành bài hay cấp thưởng.

Client đang giữ key trong bản build theo cơ chế cũ. Khi chuyển sang bản phát hành dùng chung, backend có thể thay TutorGateway bằng proxy để quản lý key; contract không yêu cầu đưa key vào tài khoản hoặc payload đồng bộ.

## Các quyết định cần backend chốt trước khi tích hợp
- URL môi trường, cơ chế access/refresh/revoke, verification/reset email và liên kết tài khoản local.
- Chính sách timezone, xử lý completion offline/nhiều thiết bị và chống lặp attempt có chủ đích.
- Quy tắc hòa hạng và chống abuse; ranking từ daily EXP/streak thật.
- Thời gian giữ SSE cursor/tombstone, thứ tự revision, retry/outbox và reset toàn cache khi cursor hết hạn.
- Nội dung bài học được kiểm chứng, quyền sử dụng nguồn SGK, phiên bản nội dung và ngân hàng câu hỏi offline.

Không cần đổi provider AI để bắt đầu dựng frontend. Không được hiển thị đồng bộ realtime hay thứ hạng nhiều người là đã hoạt động khi backend chưa có.

Quyết định bổ sung: EXP chỉ nhận lần hoàn thành đầu tiên của mỗi bài/quiz (200/400). Làm lại hoàn chỉnh bằng attempt mới nhận 0 EXP nhưng có thể thắp streak ngày mới. Ledger thưởng khóa theo account/môn/lớp/rewardUnitId ổn định, không theo enrollment mới hoặc bản sửa nội dung nhỏ. Gửi lại completion cũ không được thắp streak của một ngày khác.
