# Giao diện gọn và trạng thái tiến độ — 18/10/2026

## Tham khảo và quyết định áp dụng

- Duolingo: https://blog.duolingo.com/core-tabs-redesign/ — hệ chữ/khoảng cách/tiêu đề nhất quán, giản lược có mục đích. AIMentor giảm banner và bóng/viền lớn; giữ khác biệt màu và công cụ riêng của từng môn.
- Khan Academy: https://support.khanacademy.org/hc/en-us/articles/360030629852-What-is-my-Learner-Home-page-and-what-can-I-do-there — phân nhóm môn/hoạt động/tiến độ và ưu tiên hoạt động đang học. Giữ nút mở môn gần đây theo timestamp thật; mục chọn môn có mô tả công dụng ngắn.
- Brilliant: https://brilliant.org/help/features/ — thực hành tương tác và tiến độ có ngữ cảnh. Thu gọn giới thiệu, đưa thao tác học hiện có lên rõ hơn; giữ form quiz dùng chung và công cụ đặc biệt của mỗi môn.

Đây là lựa chọn thiết kế cho AIMentor, không phải kết luận đã đo được hiệu quả học tập. Không sao chép hình ảnh, mã nguồn, linh vật hoặc tạo lộ trình/độ thành thạo giả.

## Các thay đổi

- Hai khối thống kê chính dùng nền thẻ tương phản theo theme, viền nhẹ và bo góc thống nhất; vẫn giữ thứ tự/các thông tin hữu ích của dashboard.
- Biểu đồ chưa có dữ liệu: vòng trung tính + số 0 + hướng dẫn rõ ràng + nút chọn môn luyện tập. Không gán EXP hay phân bố giả.
- Đã có dữ liệu: vòng màu theo môn, từng môn ghi EXP thật; nút gợi ý ban đầu tự ẩn. Không biểu diễn EXP như phần trăm kiến thức đã thành thạo.
- Tải thiếu/lỗi: báo dữ liệu chưa đầy đủ, đánh dấu môn chưa tải được, có nút tải lại; không gọi dữ liệu lỗi là dữ liệu trống.
- Bốn thẻ tiến độ tự xuống hàng theo chiều rộng/font, có cấp và EXP; không cần vuốt ngang để tìm môn bị khuất.
- Trang chọn môn dùng bốn hàng gọn, biểu tượng và mô tả công dụng; giữ mọi listener/ID/đường mở module.
- Banner Toán/Khoa học/Lịch sử/Lập trình gọn hơn; hai thao tác chính của Lịch sử thành hàng biểu tượng + chữ. Giữ máy tính Toán, mạch điện Khoa học, ghi chú và quiz.
- Tiêu đề tiến độ học tập ngắn hơn ở cả tiếng Việt/Anh. Không đổi quy tắc streak hay XP.

## Kiểm chứng

- Build debug/release, APK kiểm thử, unit tests và lint thành công: compact-chart-verified-build-verification.log.
- 119 unit tests đạt, 0 thất bại. Lint 0 lỗi và 499 cảnh báo; chưa xử lý hết các cảnh báo tồn tại.
- Cấu hình thường (1080×2400, font 1.0, vuốt): 4/4 ca đạt, 46,044 giây; compact-chart-normal-final-runtime-verification.log.
- Cấu hình nhỏ (320dp, font 1.3, ba nút): 4/4 ca đạt, 37,254 giây; compact-chart-small-final-runtime-verification.log.
- Sau chỉnh khoảng cách thẻ chọn môn/viền Khoa học/hai thao tác Toán từ ảnh thực tế, cài lại APK mới và kiểm tra 4/4 ca đạt, 37,975 giây: compact-chart-final-device-verification.log.
- Các ca trên kiểm tra mở đủ bốn module, đăng nhập/đăng ký, chart không dữ liệu hiển thị đúng 0 và có CTA hoạt động, chart có dữ liệu dùng EXP thật và ẩn CTA, mở rộng/thu gọn lịch sử, chọn môn đúng tài khoản, đổi theme và tiến độ học tập tiếng Anh/tối.
- Đã đối chiếu ảnh thực tế: dashboard sáng/tối, chart 0 EXP và 10 EXP (dữ liệu kiểm thử), trang chọn môn và trang đầu bốn module. Ảnh/log chỉ lưu trong vùng QA cục bộ, không đẩy tài khoản QA hoặc database lên Git.
- Lượt kiểm tra đầu trên kết nối cũ bị dừng/mất thiết bị, không tính là PASS. Các lượt đạt phía trên sử dụng máy kiểm thử riêng emulator-5562 qua cổng ADB 5041. Không thao tác máy emulator-5554 của người dùng.
- Không chạy lại toàn bộ 67 ca chức năng thiết bị trong đợt sửa giao diện này.

Không gọi API thật; file cấu hình API vẫn trống. Chưa kiểm tra điện thoại thật.
