# Rà soát cơ cấu môn, nội dung và backend — 20/10/2026

## Đã hoàn thành

- Push bản trước khi mở rộng: `2d35d81eb6d36f4ffe4c6a2beb8234ed2d9a6e3d` trên Loc5783/AI-Mentor.
- Thêm **15 bài mỗi môn Lý, Hóa, Sinh**, không phải 15 bài tổng cộng. Hiện mỗi môn có 20 bài/60 câu lớp 10, 4 quiz ôn 10 câu phủ đúng từng nhóm năm bài. Tổng 60 bài/180 câu thực hành. Quiz dùng lại câu trong bài, không tính thành câu mới.
- Năm bài đầu, ID, lựa chọn/đáp án, placement và review đầu không đổi. Nâng cấp cùng SQLite giữ bài đã hoàn thành, bước/số lần sai đang làm, thưởng và streak. Không đổi version cho phép nối thêm cuối tương thích; không áp dụng cách này nếu sửa câu hỏi đang làm hoặc cấu trúc.
- Chín module môn: math, literature, english, history, geography, physics, chemistry, biology, economics-law. Chỉ ba môn Lý–Hóa–Sinh có lộ trình mới; sáu môn khác giữ khung. Toán/Sử vẫn mở công cụ cũ phù hợp.
- Gỡ toàn bộ module/source Programming, dependency, manifest, cấu hình key, seeder và đường mở cũ. Gỡ CategoryFragment/bố cục bốn nhóm và fallback dashboard cũ cùng các adapter thống kê đã thay thế. Các bài kiểm thử riêng cho chức năng đã bỏ được loại bỏ; ca kiểm thử dùng chung vẫn giữ phần còn hiệu lực.
- Dashboard tạo thẻ và chú giải theo môn đang chọn. Bỏ chọn môn không để sót thẻ/EXP trong biểu đồ. Không dùng EXP module cũ thay dữ liệu server. Không có thống kê thì hiện thông báo/nút tiếp tục, không dựng số liệu giả.
- Mạch điện (activity/view/model/test) thuộc physics; bảng tuần hoàn (activity/adapter/repository/model/asset) thuộc chemistry. Các môn không phụ thuộc app hay môn khác. science-support là thư viện tương thích dùng chung cho quiz bổ trợ, lưu trữ, lịch sử và giao diện cũ còn được tái sử dụng; không đăng ký môn Science.
- Giữ tên schema/package tương thích cho dữ liệu đã lưu. Metadata migration Programming trong data-core chỉ để bảo toàn khả năng đọc dữ liệu lịch sử; không có feature/helper đăng ký schema mới hay UI Programming.
- Hồ sơ sở thích dùng ID chín môn; không tự đoán SCIENCE cũ thành cả ba môn hoặc đổi PROGRAMMING thành GDKT. Sở thích không bắt buộc, khác với chọn môn học thực tế ở Lesson.
- Thu gọn phần liệt kê lặp, bỏ route menu cũ không còn đích, dùng chuỗi VI/EN cho menu. Không thêm icon trang trí.

## Backend local cho Android Studio

- `backend/start-local.ps1` và `Start-AIMentor-Backend.cmd` khởi động FastAPI tại `127.0.0.1:8000`, dữ liệu thật trong `backend/data` bị Git bỏ qua. Chế độ demo được bật rõ bằng `-Demo`.
- `backend.local.properties` bị Git bỏ qua, cấu hình debug tới `http://10.0.2.2:8000`; example được đưa lên Git. Cần Sync/rebuild APK. Release không đọc thiết lập local này và vẫn chỉ chấp nhận HTTPS; địa chỉ public vẫn để trống.
- BackendEndpointPolicy chỉ cho HTTP loopback/emulator trong debug và có kiểm thử chặn host/port, user-info/query/fragment không hợp lệ. Network security debug chỉ mở đúng hai host local.
- Có endpoint nhưng account chưa liên kết thì yêu cầu xác nhận đăng nhập, không báo nhầm chưa cấu hình server. Đăng nhập lại dùng quy tắc xác minh hai danh tính, không gộp chỉ theo email.
- Health thông báo transport SSE có sẵn, bỏ cờ Android-connected=false cố định vì server không thể suy ra trạng thái mọi client từ health.
- Math/History giữ key pool riêng; quiz bổ trợ dùng `QUIZ_GEMINI_*`, Chat chung dùng `CHAT_GEMINI_*`. Key Git trống; bỏ toàn bộ `PROGRAMMING_GEMINI_*` và tên cấu hình `SCIENCE_GEMINI_*` cũ. Không thêm key thật vào APK/public source.

## Kiểm chứng

- Debug/release/test APK và lint PASS: `school-local-backend-build-verification.log`. 140 JVM tests, 0 lỗi/thất bại.
- Backend: 33 ca PASS (`school-backend-final-tests-verification.log`), gồm đi hết 20 bài + 4 review của từng môn, gating checkpoint, 200/400 EXP một lần và nâng cấp từ gói năm bài có tiến độ đang làm.
- Native: 4 ca Dashboard/5 tab/công cụ PASS; 2 ca connected lộ trình + SSE PASS; 13 ca auth/lớp/tài khoản/công cụ/schema PASS; 3 ca cuối dùng URL emulator thật, đổi môn/rotation và ma trận VI/EN sáng/tối PASS. Tổng 22 lượt ca đã chạy đạt ở các nhóm trên.
- Test URL thật đăng nhập demo 03, tải đủ 60 bài/12 review và render Dashboard live; không hoàn thành bài giả để tạo EXP. Trạng thái không có dữ liệu được kiểm tra ở 320dp/font1.3, cả VI/EN/sáng/tối. Ảnh QA có `school-live-dashboard.png` và `school-dashboard.png` trong vùng riêng của test app.
- APK debug khoảng 14,1 MiB, không chứa class `com.example.modulelaptrinh`. BuildConfig debug có URL local; release để trống. Quét source không thấy mẫu API credential thật.

## Giới hạn và việc tiếp theo

- Chưa biên soạn lớp 11/12 hoặc đủ toàn bộ mục lục SGK. Nội dung tự biên soạn theo chủ đề lớp 10, vẫn cần giáo viên duyệt. Xem docs/content/CONTENT_PLAN_VI.md.
- Backend đang chạy local, không phải dịch vụ Internet. Phải giữ server mở hoặc chạy lại launcher sau khởi động máy. Không thay thông báo lỗi bằng trạng thái thành công giả.
- Đồng bộ chat cloud, editor hồ sơ/password phía server, UI lời mời kết bạn và outbox offline vẫn chưa hoàn tất. Không dùng kết quả các ca đã chạy để tuyên bố toàn app đã xong.
- Bản Android Studio ở `C:/Users/ACER/StudioProjects/AI-Mentor` được xác nhận còn tại snapshot `2d35d81`, có file `.run/app.run.xml` được stage và một thay đổi khoảng trắng tại GeminiApiClient. Không đưa các thay đổi cục bộ đó lên Git; cần giữ nguyên khi đồng bộ bản mới. Trong lúc lưu mốc, backend đã được kiểm tra trên máy giả lập QA riêng, chưa tuyên bố đã cập nhật máy giả lập người dùng.

Các báo cáo cũ giữ lại làm lịch sử theo ngày; số bài, module và kết quả kiểm chứng ở báo cáo này thay thế các con số snapshot cũ.
