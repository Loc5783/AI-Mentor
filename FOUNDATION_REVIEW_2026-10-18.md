# Rà soát tính hợp lý — bước nền móng

**PHẠM VI MỚI NHẤT:** chỉ triển khai luồng Lesson đầy đủ cho Sinh học, Hóa học và Vật lý trong đợt này. Sáu môn còn lại giữ khung module, chưa có bài/đầu vào/quiz mới; UI phải ghi Sắp có và không mở luồng rỗng. Chat AI chung vẫn hỗ trợ tất cả môn theo lớp 10/11/12. Các đoạn mô tả 9 môn là kiến trúc mở rộng, không phải cam kết có đủ nội dung 9 môn ở đợt hiện tại.

Phạm vi: cấu trúc 9 môn, lõi bài học, Chat AI adapter và hợp đồng backend. Chưa phải xác nhận giao diện mới hoàn tất.

## Các điểm đã đối chiếu và điều chỉnh

1. **Chín môn khác với bốn lựa chọn cũ:** dùng ID môn mới ổn định và module riêng; không coi Science là môn thứ mười hoặc biến Lập trình thành GDKT. Giữ code Science/Lập trình để chức năng cũ không biến mất trong lúc chuyển đổi. Các entry mới không đồng nghĩa có nội dung bài học hoàn chỉnh.
2. **Không chọn lớp hai lần vô ích:** đặc tả yêu cầu Lesson dùng lớp hồ sơ hiện tại. Account cũ chưa có lớp phải chọn, không âm thầm gán lớp 10. Chat request từ chối grade ngoài 10–12 và lịch sử của account khác.
3. **Quiz không làm lệch nhóm năm bài:** quiz là node riêng, không tính thành bài thứ sáu. Nhóm ôn kế tiếp là bài 6–10, không phải quiz trước + bốn bài mới. Test bao phủ nhóm đầu, nhóm sau, nhóm thiếu và ID trùng.
4. **Streak và EXP là hai loại dữ liệu:** policy nhận 200/400 EXP độc lập với việc hôm nay đã thắp streak hay chưa. Cơ chế phát thưởng phải qua ledger, không đặt trong callback UI. Chưa bật policy vào quiz cũ nên chưa có hai đường thưởng cùng chạy.
5. **Bảng xếp hạng phải so cùng ngày:** phát hiện nguy cơ mỗi người dùng timezone khác nhau khiến “EXP hôm nay” so hai khoảng thời gian khác nhau. Đã thống nhất contract giai đoạn đầu là Asia/Ho_Chi_Minh, timezone chỉ đọc; profile và ranking dùng cùng ngày. Chưa có backend thực thi.
6. **Lỗi AI không phải câu trả lời:** key trống không gọi mạng, 401/403/429 xoay pool có giới hạn; phản hồi rỗng/bị chặn không báo thành công. Lỗi mạng/server không tự lặp một lượt sinh có thể đã tính phí; cho phép UI thử lại có chủ đích. Có cờ báo câu trả lời bị cắt.
7. **Nút Đã hiểu không phải hoàn thành bài:** con trỏ chuyển chat giữ attempt/question/step/phiên bản chương trình; không có quyền thay tiến độ hay cộng EXP trong adapter chat.
8. **Realtime chưa có thì không hiển thị như đã có:** contract có ownership/revision/idempotency/tombstone/SSE; servers chưa cấu hình. Không tạo user, đối thủ, thứ hạng hoặc số liệu biểu đồ giả.
9. **Giữ giao diện đã duyệt:** bước này không đổi login/register hay thay điều hướng bằng màn hình rỗng. Khi nối UI cần test lại bàn phím, chữ lớn, ba nút/vuốt, theme và VI/EN.

10. **Không để cấu hình AI làm sập app:** AppServices có khởi tạo chat chung, nên model cấu hình sai có thể ném exception ngay khi mở app. Factory đã đổi sang provider trả INVALID_CONFIGURATION, các phần khác vẫn khởi động; trim khoảng trắng model và có unit test cho cấu hình sai.

## Những điểm cần kiểm tra khi nối các bước sau

- Quiz 15 câu phải có đủ câu đã kiểm chứng của cả 5 bài; thiếu dữ liệu phải báo và chọn cấu hình hợp lệ, không lặp câu để đủ số.
- Đổi lớp/chương trình khi đang học cần xác nhận rõ và bảo toàn attempt cũ.
- Đi khỏi app hoặc process bị dừng trong lúc hỏi AI vẫn phải có bản lưu tiến độ và bản nháp chat.
- Xóa thư mục cần làm rõ giữ hay xóa chat; xóa profile không được để tệp public còn truy cập vô hạn.
- Phải nối lesson/review/quiz hiện hữu vào ledger trước khi đổi reward thật. Phát lại cùng completion không được nhân EXP.
- Nội dung 9 môn chưa được biên soạn; nguồn SGK cần quyền sử dụng và kiểm chứng, không gọi dữ liệu AI là chương trình chính thức.

## Kiểm chứng

Debug/release/APK kiểm thử/unit/lint: build thành công. 149 unit tests, không thất bại; lint 0 lỗi và 534 cảnh báo. Tám cảnh báo tăng thêm đều là gợi ý nâng compileSdk 36 lên 37 cho các module mới; giữ SDK 36 đồng nhất trong lần tách này.

Runner toàn bộ bản phạm vi ba môn: OK (83 tests), gồm 75 ca chạy đạt và 8 ca API thật bỏ qua; log learning-foundation-full-runtime-verification.log. Sau sửa factory cấu hình chat cuối cùng: 12/12 ca SharedTutorContractTest và CanonicalSmokeTest đạt trên APK cuối; log learning-foundation-reviewed-focused-verification.log. Không có API key thật hoặc paid generation.

Lượt toàn bộ đầu có một lỗi máy tính ngang không ra 42. Chưa xác định nguyên nhân sản phẩm; đã thêm kiểm tra biểu thức sau từng phím. Lượt tập trung 1/1 và lượt toàn bộ kế tiếp đều đạt, không sửa logic tính toán để che lỗi. Giữ ghi nhận này để theo dõi khi thay UI máy tính; không tuyên bố đã tìm và sửa được nguyên nhân gốc. Chưa thử điện thoại thật.

Quyết định bổ sung: EXP chỉ nhận lần hoàn thành đầu tiên của mỗi bài/quiz (200/400). Làm lại hoàn chỉnh bằng attempt mới nhận 0 EXP nhưng có thể thắp streak ngày mới. Ledger thưởng khóa theo account/môn/lớp/rewardUnitId ổn định, không theo enrollment mới hoặc bản sửa nội dung nhỏ. Gửi lại completion cũ không được thắp streak của một ngày khác.

Điểm chặn trước khi phát hành Lesson mới: bỏ bước chọn bốn môn legacy bắt buộc ở thiết lập hồ sơ đăng ký; giữ cách trình bày auth đã duyệt, dùng lại lớp/mục tiêu và gom chọn môn về Lesson. Bản nền móng chưa thay đổi form đó vì chưa đổi điều hướng; không để hai bước chọn môn cùng xuất hiện trong bản UI mới.
