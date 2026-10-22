# Cập nhật Thời khóa biểu và Nền (Background)

## 1. Giao diện Thời khóa biểu
- Đã chuyển từ danh sách dạng Tab sang dạng bảng (Grid) hiển thị 6 ngày trong tuần (Thứ Hai đến Thứ Bảy).
- Bảng hiển thị 10 tiết chia làm 2 ca: "SÁNG" (Tiết 1-5) và "CHIỀU" (Tiết 6-10).
- Khi thêm hoặc sửa tiết học:
  - Cho phép nhập trực tiếp "Tiết" (từ 1 đến 10) thông qua menu thả xuống, thay vì nhập giờ bắt đầu / kết thúc.
  - Đã loại bỏ trường nhập "Phòng học" theo đúng yêu cầu.
  - Vẫn giữ nguyên khả năng báo trùng tiết học nếu tiết đó đã được xếp vào lịch của ngày tương ứng.
- Khi bấm vào các ô trống trên lưới, form thêm tiết sẽ tự động mở lên với tiết và ngày tương ứng. 

## 2. Hình nền tuỳ chỉnh toàn ứng dụng
- Đã bổ sung cài đặt "Thay đổi hình nền" ở phần "TÙY CHỌN KHÁC" trong **Cài đặt**.
- Sử dụng Image Picker hệ thống (thông qua `ActivityResultContracts.GetContent()`) để chọn file ảnh.
- Ảnh người dùng chọn sẽ được lưu cục bộ và hiển thị thành lớp dưới cùng của toàn bộ ứng dụng thông qua tính năng của Coil `AsyncImage` trên root view ở `ShellUi`.
- Để nội dung chính dễ nhìn trên nền ảnh, lớp nền của giao diện ở trên cùng (các thanh Bar và App Surface) đã được làm trong suốt / mờ vừa phải. Lớp nền hệ thống sẽ phản chiếu hình ảnh phía sau theo đúng mong đợi. 

## Ghi chú thiết kế
- Dữ liệu lịch học (Tiết) được tận dụng lưu vào `startTimeMin` trên Database để tương thích với cấu trúc hiện tại, không phải phá vỡ Schema đã được thiết lập trước đó. 
- API Coil 3.x đã được áp dụng để tải hình nền tối ưu.
- Việc thay đổi hình nền sẽ tự động tải lại HomeActivity.