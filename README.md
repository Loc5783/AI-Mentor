# AI Mentor

Ứng dụng Android mới gồm 5 mục: Dashboard, Chat AI, Bài học, Thi đua và Hồ sơ.

## Cấu trúc
- `app/`: giao diện, điều hướng, tài khoản và AppServices.
- `subjects/`: 9 môn Toán, Văn, Anh, Sử, Địa, Lý, Hóa, Sinh, GDKT & Pháp luật.
- `core/`: learning-core, chat-core, data-core, ui-core dùng chung.
- `backend/`: FastAPI, SQLite, lộ trình, tài khoản, EXP/streak, bảng xếp hạng và SSE.
- `docs/`: hợp đồng API và nội dung môn học.
- `tools/`: dọn thư mục build cũ sau khi cập nhật Git.

Lý–Hóa–Sinh có 20 bài ngắn lớp 10 mỗi môn; Toán, Văn, Anh, Sử, Địa và GDKT & Pháp luật có thêm 15 bài nhập môn mỗi môn. Các bài có câu luyện theo ba mức và quiz ôn sau mỗi 5 bài. Đây là nội dung tự biên soạn mức khởi đầu, chưa phủ toàn bộ năm học và cần giáo viên rà soát; lớp 11–12 chưa có gói bài. Toán có máy tính bong bóng, Lý có mô phỏng mạch điện, Hóa có bảng tuần hoàn.

Bốn nhóm module cũ và dữ liệu local cũ được loại bỏ theo yêu cầu làm mới. Lần chạy đầu bản này xóa dữ liệu local cũ một lần; các lần mở sau giữ dữ liệu mới. App có tích hợp backend FastAPI local và Supabase; các migration mới phải được áp dụng vào project Supabase trước khi nội dung lớp 10 và kiểm tra đầu vào cập nhật trên cloud.

## Chạy app
1. Mở thư mục gốc bằng Android Studio; dùng Gradle JDK 21 và Android SDK 36.
2. Sync Gradle và Run module `app`. Mặc định app dùng Supabase theo cấu hình public trong `supabase.local.properties.example`; áp dụng các migration còn thiếu trước khi thử khóa học lớp 10 trên cloud.
3. Muốn chạy FastAPI local: thêm `SUPABASE_ENABLED=false` vào `supabase.local.properties`, chạy `Start-AIMentor-Backend.cmd` và sync/rebuild. Launcher dùng `http://10.0.2.2:8000` cho giả lập.
4. Chat AI qua Supabase cần Edge Function `mentor-ai` đã deploy và Gemini key trong Supabase Secrets. Không đưa secret vào Git hoặc APK. `api.properties` chỉ dành cho cấu hình backend/tutor cũ.

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

Trước khi dùng cloud đầy đủ cần áp dụng migration còn thiếu `202610300001_remaining_grade10_curricula.sql` và `202610300002_adaptive_placement.sql`, xác nhận Edge Function/email auth, và kiểm thử trên thiết bị. Hàng đợi gửi thay đổi khi offline, xử lý xung đột nhiều thiết bị và triển khai production vẫn còn ngoài bản hiện tại.

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
