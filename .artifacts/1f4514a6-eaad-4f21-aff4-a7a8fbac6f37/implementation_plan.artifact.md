# Nâng cấp toàn diện Module Schedule (Mobile-First, To-Do List, Activity Log)

Dự án sẽ nâng cấp module `:schedule` thành một không gian quản lý học tập cá nhân mạnh mẽ, gọn gàng, tối ưu cho thao tác một tay trên mobile theo đúng chuẩn Clean Architecture và Jetpack Compose.

## User Review Required
> [!WARNING]
> **Cập nhật Database (SQLite):** Cơ sở dữ liệu sẽ được nâng cấp từ version 2 lên version 3. Các bảng mới `todo_tasks` và `activity_logs` sẽ được thêm vào. Bảng `schedule_entries` hiện tại sẽ được giữ nguyên dữ liệu, không bị mất lịch học cũ.

> [!TIP]
> **Quy trình gộp UI (Tabs vs Scroll):** Để tối ưu hiển thị, màn hình chính sẽ có 2 Tab lớn: **"Lịch học"** và **"Nhiệm vụ"**. Thanh chọn ngày (Day Strip) sẽ dùng chung nằm ở trên cùng. Lịch sử hoạt động sẽ được mở dưới dạng `ModalBottomSheet` khi bấm icon đồng hồ ở TopBar. Cách bố trí này giúp tránh nhồi nhét quá nhiều vào 1 luồng cuộn dọc.

## Proposed Changes

### 1. Domain & Data Layer (Database & Repository)
* **[NEW] `TodoTask.kt`**: Model lưu trữ nhiệm vụ (id, accountId, title, isCompleted, dayOfWeek, priority, linkedSubject, createdAt).
* **[NEW] `ScheduleActivityLog.kt`**: Model lưu trữ lịch sử (id, accountId, actionType, entityType, description, timestamp).
* **[MODIFY] `ScheduleRepository.kt`**: Bổ sung các hàm CRUD cho Todo và ActivityLog.
* **[MODIFY] `ScheduleRepositoryImpl.kt`**:
  - Nâng cấp `ScheduleDatabaseHelper` lên version 3.
  - Tạo bảng `todo_tasks` và `activity_logs`.
  - Implement các logic truy vấn dữ liệu.

### 2. Presentation Layer (ViewModel)
* **[MODIFY] `ScheduleViewModel.kt`**:
  - Thêm state cho list `TodoTask` và `ScheduleActivityLog`.
  - Tích hợp logic tự động ghi log (Auto-logging) mỗi khi có thay đổi từ Schedule hoặc Todo.
  - Xử lý phân loại Todo (Chưa xong / Đã xong) và tính toán phần trăm hoàn thành.

### 3. UI Layer (Jetpack Compose)
Tái cấu trúc hoàn toàn `ScheduleScreen.kt` và chia nhỏ thành các file/composable:
* **[MODIFY] `ScheduleScreen.kt`**: Màn hình gốc chứa Scaffold, TopBar (có nút History), FAB (thêm sự kiện tùy Tab).
* **[NEW] `DaySelectorStrip.kt`**: Thanh chọn 7 ngày dạng thẻ tròn bo góc, có dấu chấm indicator nếu ngày đó có dữ liệu.
* **[NEW] `ScheduleTimelineList.kt`**: Timeline giao diện mới. Card bo góc 14dp, màu sắc theo `SubjectPalette`. Tính toán logic hiển thị Badge (Đang diễn ra, Sắp tới, Đã xong) dựa vào giờ hiện tại. Hỗ trợ Swipe-to-dismiss.
* **[NEW] `DailyTodoList.kt`**:
  - Thanh Progress Bar tổng quan.
  - `TodoCard` với Checkbox màu Emerald Green, hiệu ứng Strikethrough (gạch ngang) và Fade khi hoàn thành.
  - Form Quick Add (Thêm nhanh).
* **[NEW] `ActivityLogSheet.kt`**: BottomSheet hiển thị dòng thời gian các hoạt động (Thêm/Sửa/Xóa).

## Verification Plan
### Automated Tests
- Viết thêm `ScheduleAdvancedLogicTest.kt` kiểm tra:
  - Logic tính toán trạng thái thời gian của Schedule (Ongoing/Upcoming/Done).
  - Logic phân loại và tính % hoàn thành của Todo List.
- Chạy `gradlew :schedule:testDebugUnitTest`.

### Manual Verification
- Kiểm tra UI hiển thị tốt trên màn hình hẹp (360dp).
- Test vuốt xóa (Swipe to dismiss) và nút Undo trên Snackbar.
- Test hiệu ứng gạch ngang nhiệm vụ.
- Xác minh Lịch sử hoạt động được ghi nhận chính xác sau mỗi thao tác.