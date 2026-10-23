# AI Mentor

Ứng dụng Android mới gồm 5 mục: Dashboard, Chat AI, Bài học, Thi đua và Hồ sơ.

## Cấu trúc
- `app/`: giao diện, điều hướng, tài khoản và AppServices.
- `subjects/`: 9 môn Toán, Văn, Anh, Sử, Địa, Lý, Hóa, Sinh, GDKT & Pháp luật.
- `core/`: learning-core, chat-core, data-core, ui-core dùng chung.
- `backend/`: FastAPI, SQLite, lộ trình, tài khoản, EXP/streak, bảng xếp hạng và SSE.
- `docs/`: hợp đồng API và nội dung môn học.
- `tools/`: dọn thư mục build cũ sau khi cập nhật Git.

Chỉ Lý–Hóa–Sinh có nội dung học hiện tại: mỗi môn 20 bài ngắn lớp 10, 60 câu luyện và 4 quiz ôn 10 câu. Sáu môn còn lại giữ khung; Toán có máy tính bong bóng, Lý có mô phỏng mạch điện, Hóa có bảng tuần hoàn. Lớp 11–12 và toàn bộ chương trình năm học chưa hoàn tất.

Bốn nhóm module cũ và dữ liệu local cũ được loại bỏ theo yêu cầu làm mới. Lần chạy đầu bản này xóa dữ liệu local cũ một lần; các lần mở sau giữ dữ liệu mới. Backend đang phát triển riêng, chưa triển khai Internet.

## Chạy app
1. Mở thư mục gốc bằng Android Studio; dùng Gradle JDK 21 và Android SDK 36.
2. Chạy `Start-AIMentor-Backend.cmd`, giữ cửa sổ server mở. Launcher tạo cấu hình debug cục bộ `http://10.0.2.2:8000` cho máy giả lập.
3. Sync Gradle và Run module `app`.
4. Key Chat AI điền trong `api.properties`, pool `CHAT_GEMINI_API_KEY_1/2/3`. File trên Git để trống. Sync/rebuild sau khi đổi cấu hình.
5. Backend public dùng `MENTOR_BACKEND_URL` HTTPS; không đưa đường dẫn máy hoặc key thật lên Git.

Tài khoản debug và backend chế độ demo: `student 01`, `student 02`, `student 03`; mật khẩu `123123123`.

Nếu máy giả lập báo System UI/Launcher không phản hồi hoặc cửa sổ không có focus, hãy Cold Boot máy giả lập rồi thử lại; không cần xóa dữ liệu để kiểm tra bước này. Đây là lỗi đã quan sát trong môi trường QA, chưa phải kết luận cho mọi trường hợp màn hình đen.

## Quy tắc học tập
Bài học nhận 200 EXP; quiz ôn sau mỗi 5 bài nhận 400 EXP. Chỉ lần hoàn thành đầu tiên có EXP. Làm lại có thể thắp streak ngày mới nhưng không cộng EXP lần nữa. Ngày học thống nhất UTC+7. Không có đếm ngược quiz, không tạo thống kê giả.

## Phát triển và kiểm tra
Áp dụng SOLID: tách giao diện, trạng thái, chính sách, lưu trữ và giao tiếp mạng.
`./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug testDebugUnitTest`

Backend: từ `backend/`, chạy `.venv/Scripts/python.exe -m pytest tests -q`.
Hợp đồng đang chạy: [runtime-openapi.json](docs/backend/runtime-openapi.json).
Nội dung môn: [CONTENT_PLAN_VI.md](docs/content/CONTENT_PLAN_VI.md).
Bàn giao backend: [BAN_GIAO_BACKEND.txt](app/BAN_GIAO_BACKEND.txt).

Chưa hoàn tất: đồng bộ chat cloud, chỉnh sửa hồ sơ/mật khẩu với server, quản lý lời mời kết bạn, outbox offline và triển khai production.

Git: nhánh feature/*, module/* hoặc fix/* → Pull Request integration; trưởng nhóm quản lý main.

## Bảng màu & Hệ thống Design System (22/10/2026)
- **Quy tắc 60-30-10**: Nền trung tính (~60%), màu xanh chủ đạo `#8DB6F7` (~30%), nhấn hồng & vàng cam phần thưởng `#F7CE8D` (~10%).
- **Light Theme**: Nền ngả hồng nhẹ ấm `#FFF7FA`, Card `#FCEFF5`, chữ `#24203A` tương phản **14.18:1**.
- **Dark Theme**: Nền Navy trung bình ngả tím nhẹ `#1F2A44`, Surface `#263352`, Card `#2E3D61`, chữ chính `#F1F4FB` tương phản vượt trội **11.71:1** (dễ đọc, dịu mắt).
- **Tương phản WCAG AA**: Chữ `#0B2545` đặt trên nền `#8DB6F7` đạt tỉ lệ tương phản **7.55:1** ở cả chế độ sáng và tối (không dùng chữ trắng trên nền chủ đạo).
- **Màu môn học (SubjectPalette)**: Palette 10 màu pastel/muted thống nhất tại `ui-core`, container màu tự động tính toán với độ tương phản đạt chuẩn.
- Chi tiết thiết kế và bảng kiểm tra WCAG: xem tại [COLOR_SYSTEM_2026-10-22.md](COLOR_SYSTEM_2026-10-22.md).

## Giao diện Compose (21/10/2026)
- `core/ui-core/.../design`: màu semantic light/dark, typography Material 3, spacing/radius và thành phần dùng chung. Bảng màu chủ đạo (#8db6f7 xanh dương pastel) hỗ trợ WCAG AA với màu chữ navy đậm.
- `app/.../ui`: ShellUi (Navigation Compose), LearningUi, OverviewUi, ChatUi, SubjectHubUi, CompetitionUi. Java Fragment tiếp tục sở hữu lifecycle.
- Module `:schedule`: Quản lý Thời khóa biểu cá nhân, thêm/sửa/xóa lịch học hàng tuần. Cấu trúc độc lập host-independent, gọi từ HomeFragment. Dữ liệu lưu trong `AIMentorSchool.db` phân quyền theo account.
- Bỏ renderer/layout XML cũ khi đã thay bằng Compose. Không duy trì hai bộ Dashboard/Chat/Path chạy song song.
- Kotlin/Compose Compiler 2.2.10; Compose BOM 2025.08.00 (Material 3 theo BOM); Navigation Compose 2.9.3; Activity Compose 1.13.0; Fragment Compose 1.8.9; Coil 3.3.0; Konfetti 2.0.5. Đây là bộ phiên bản ổn định đã chọn cho toolchain hiện có. Lottie 6.7.1 chỉ khai báo alias, chưa kéo vào APK vì animation hiện tại không cần asset.
- UI đọc thêm `week` từ API Dashboard để vẽ bảy ngày thật. Chưa có dữ liệu thì có hướng dẫn, không vẽ thành tích giả. Giờ học, cấp cá nhân và bộ sưu tập huy hiệu cần chính sách/ledger backend trước khi bật.
- Thưởng và streak vẫn do backend quyết định: bài 200 EXP, quiz ôn 400 EXP ở lần đầu; làm lại không nhận lại EXP. Animation không thay đổi kết quả.
- SQLite dùng một connection pool có WAL và reference counting cho mỗi file, tránh mở/đóng kết nối độc lập khi các repository đang đọc/ghi đồng thời.
- Chạy thử: mở project với JDK 21, sync Gradle, chạy cấu hình app. Xem đủ năm mục; đổi Việt/Anh và sáng/tối trong Hồ sơ → Cài đặt; thử giữ bản nháp khi đổi mục/xoay màn hình. Cấu hình backend debug trong `backend.local.properties` để thử bài học và biểu đồ có dữ liệu.

### Thay đổi logic phục vụ giao diện mới
- `ShellUi.kt`: giữ yêu cầu chuyển trang đến khi Navigation sẵn sàng, tránh bỏ mất thao tác ngay sau đăng nhập.
- `LearningUi.kt`: chụp trạng thái bất biến từ mỗi lần ViewModel phát dữ liệu để Compose cập nhật đúng câu hỏi, phản hồi và kết quả; không sửa quy tắc chấm điểm.
- `LearningOverviewData.java`: đọc/kiểm tra chuỗi bảy ngày có sẵn từ backend để biểu đồ dùng dữ liệu thật.
- `SchoolDatabaseConnections.java` và `ModuleDatabaseHelper.java`: dùng chung kết nối SQLite có WAL và đếm tham chiếu để các repository không đóng kết nối của nhau.
- `SystemSafeArea.java`: giao phần inset của màn chính cho Compose, tránh cộng padding hai lần và che nội dung bởi bàn phím/thanh hệ thống.

### Kiểm tra theo từng phần
| Phần | Nơi triển khai | Chạy thử |
| --- | --- | --- |
| Design system | `core/ui-core/src/main/java/com/example/mentor/ui/design/` | Đổi sáng/tối, tắt animation hệ thống, tăng cỡ chữ |
| Level | `app/src/main/java/com/example/asm_final/ui/LearningUi.kt` | Khảo sát đầu vào, bài học, giải thích, quay về từ Chat, quiz sau mỗi 5 bài |
| Profile | `app/src/main/java/com/example/asm_final/ui/OverviewUi.kt` | Mở hồ sơ, sửa hồ sơ, cài đặt; thử có và chưa có dữ liệu |
| Home | `app/src/main/java/com/example/asm_final/ui/OverviewUi.kt` | Chọn môn, tiếp tục học, biểu đồ theo tuần, mất kết nối |
| Chat | `app/src/main/java/com/example/asm_final/ui/ChatUi.kt` | Gửi tin, tệp/ảnh, lịch sử/thư mục, đổi mục và xoay màn hình |
