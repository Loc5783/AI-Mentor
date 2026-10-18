# Kiểm tra lỗi AIMentor — 18/10/2026

Repo: https://github.com/Loc5783/AI-Mentor ; nhánh main.

## Lỗi xác nhận và đã sửa

1. **Thông báo sai ở cấp tối đa:** trang chủ vẫn hiển thị “Còn 0 EXP để lên cấp” dù cấp 20 đã là giới hạn. Đổi thành “Đã đạt cấp độ tối đa”, có bản tiếng Anh.
2. **Mốc mặc định sai:** nội dung tĩnh của thanh tiến độ là 140 EXP, trong khi quy tắc quiz chung bắt đầu ở 120. Đã đồng bộ.
3. **Nhãn cấp dính số:** ghép resource kết thúc bằng dấu cách với số dẫn tới “Cấp1” / “Level1”, vì Android bỏ khoảng trắng ở cuối resource. Dùng chuỗi định dạng có tham số, giữ đúng khoảng cách ở hai ngôn ngữ.
4. **Tràn tổng EXP:** cộng bốn giá trị int có thể quay về số âm khi nhập dữ liệu lớn, khiến cấp tổng hợp tụt về 1. Tổng dùng long, loại giá trị âm và chặn tại Integer.MAX_VALUE trước khi đưa vào chính sách level. Không sửa EXP đã lưu trong database.

Thêm ba unit tests cho trường hợp cộng tổng bình thường, dữ liệu âm và tràn số; thêm một kiểm thử thiết bị xác minh nhãn/thanh tiến độ tại cấp tối đa.

## Kiểm chứng

- Build debug, release và APK kiểm thử: thành công.
- Unit tests: **119 đạt; 0 thất bại, 0 lỗi**.
- Android lint toàn bộ module: **0 lỗi, 495 cảnh báo**. Nhóm nhiều nhất: 114 HardcodedText, 53 Overdraw, 37 PluralsCandidate; cần rà từng mục vì không phải tất cả đều là lỗi hiển thị thực tế. Còn cảnh báo về tài nguyên/giao diện, cách viết mã và thư viện; không coi số lỗi bằng 0 là bảo đảm ứng dụng không còn lỗi.
- Lượt thiết bị đầu có 10 ca thất bại tại bước lấy ảnh (bitmap null), không phải kết luận rằng cả 10 chức năng lỗi. Chưa tính lượt đó là đạt. Lượt xác minh cuối chạy trong máy giả lập để giữ kết nối công cụ kiểm thử.
- **6/6 ca thiết bị tập trung đạt**: cấp tối đa, dashboard sau đổi theme, chọn môn với đúng tài khoản và mở rộng lịch sử, streak/XP tuần tiếng Anh/tối, quiz giữ đáp án khi đổi ngôn ngữ/theme và không thưởng hai lần, lọc/tìm lịch sử cả bốn môn. Log: published-audit-target-runtime-verification.log, thời gian 37,011 giây. Cấu hình: 320dp, font 1.3, điều hướng 3 nút.
- **Chưa xác nhận trọn bộ 67 ca thiết bị trên bản cuối**: máy giả lập bị ngắt giữa lượt chạy, kết quả không có tổng kết. Sau khi mở lại, 6 ca tập trung phía trên hoàn tất; máy giả lập tiếp tục mất kết nối khi chuẩn bị lượt toàn bộ. Không ghi nhận lượt chưa hoàn tất là PASS.

## Phạm vi và giới hạn

- Kiểm tra tài khoản mẫu, đăng ký/đăng nhập, điều hướng bốn môn, database và chuyển dữ liệu, tách dữ liệu người dùng, lịch sử, ghi chú, quiz dùng chung, ngân hàng câu hỏi/ôn offline, EXP, streak chỉ tính quiz, mạch điện và máy tính riêng của Toán.
- Có kiểm tra đổi ngôn ngữ, sáng/tối, xoay màn hình, chữ lớn, ba nút Android; ảnh kiểm thử và log để cục bộ ngoài nội dung phát hành.
- Chưa kiểm tra điện thoại thật, nhiều phiên bản Android hoặc mọi tổ hợp màn hình.
- 10 trường API key trong api.properties vẫn để trống. Không kiểm tra phản hồi Gemini thật trong lượt này; các bài live API chủ động bỏ qua.
- Chưa có backend giải đấu nhiều người, đồng bộ nhiều thiết bị và chống sửa giờ/EXP. Hợp đồng bàn giao vẫn nằm trong app/BAN_GIAO_BACKEND.txt.
