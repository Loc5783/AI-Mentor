# Checklist AIMentor đối chiếu yêu cầu — 20/10/2026

> Checklist này là ảnh chụp tiến độ trước lượt sửa hiệu ứng cùng ngày. Cập nhật mới hơn ở RESUMED_PROGRESS_2026-10-20.md: lộ trình đánh số/đường nối, motion nhẹ đã sửa và kiểm tra; 3 ca chat Gemini thật Lý/Hóa/Sinh đã đạt bằng key QA riêng. Nội dung runtime vẫn 15 bài/45 câu lớp10; nguồn mở rộng đã chốt Kết nối tri thức2026–2027 cho lớp10–12. Các mục còn lại bên dưới giữ mốc audit ban đầu.

Kiểm tra mã local và kết quả QA đã lưu tới 19/10/2026. Không phải lượt chạy lại toàn bộ app hôm nay, không xác nhận bản đã cài trên điện thoại/GitHub giống bản local. Không gán phần trăm hoàn thành vì nội dung và kiểm chứng còn nhiều khoảng trống.

Quy ước: [x] đã có trong phạm vi ghi rõ; [ ] chưa hoàn tất. Mục ghi "một phần" có triển khai nhưng còn điều kiện để đạt yêu cầu đầy đủ.

## Giao diện và cấu trúc
- [x] Năm tab Dashboard / Chat AI / Lesson / Thi đua / Profile đúng thứ tự.
- [x] Giữ giao diện đăng nhập/đăng ký đã được duyệt; nối xác thực backend khi có cấu hình.
- [x] SOLID thành tiêu chí chung, lưu tại AGENTS.md. Các phần mới tách view/state/policy/repository/transport, lắp ghép ở AppServices.
- [ ] Đồng nhất và tối ưu toàn bộ app: một phần; màn công cụ cũ vẫn còn, chưa chuyển/gỡ toàn bộ luồng trùng nhiệm vụ.
- [ ] Responsive mọi màn, navigation vuốt/ba nút, keyboard, chữ lớn: một phần; có safe-area và QA compact/chữ 1.3, chưa đủ ma trận thiết bị thật.
- [ ] Sáng/tối và Việt/Anh toàn app: có hạ tầng và kiểm thử cũ; cần hồi quy mọi màn mới, kiểm tra cả nội dung học theo ngôn ngữ.

## Dashboard
- [x] Có thông báo và hành động khi chưa có dữ liệu; tách loading/empty/error/partial/data, không dựng số liệu giả.
- [x] Tài khoản backend đọc thống kê chung: bài học, EXP/ngày, streak, dữ liệu từng môn và câu đúng lần đầu/trợ giúp.
- [x] Nhận cập nhật sự kiện khi đang xem, giữ đồ thị lúc refresh nền.
- [ ] Đánh giá năng lực toàn diện theo chương trình: chưa có đủ nội dung/chuẩn đánh giá; EXP không được gọi là phần trăm hiểu bài.

## Chat AI
- [x] Chat riêng, thư mục, đổi tên/chuyển/xóa, bản nháp; lưu SQLite theo tài khoản.
- [x] Có UI chọn tệp/ảnh/camera và cơ chế sao chép tệp riêng.
- [x] Có adapter hỏi đa môn theo lớp 10–12 và cấu hình/key rotation theo cơ chế cũ.
- [ ] AI thật và camera trên điện thoại thật: chưa xác nhận đầy đủ; 13 API keys hiện trống, chưa gọi Gemini trong lượt QA mới.
- [ ] Đồng bộ chat/thư mục/tệp/realtime nhiều thiết bị: backend có API, Android còn local.
- [ ] Chuyển đầy đủ lịch sử/bank từ module cũ trước khi gỡ luồng cũ.

## Ba module Vật lý / Hóa học / Sinh học
- [x] Tách module từng môn, dùng chung nền tảng luồng học; sáu môn còn lại giữ khung đúng phạm vi.
- [x] Chọn lớp, chọn/thêm môn trong Lesson; lựa chọn hiện lưu local theo tài khoản/lớp.
- [x] Có kiểm tra đầu vào 10 câu, giải thích → ví dụ → thực hành và hướng dẫn sau hai lần sai.
- [x] Có chuyển câu sang Chat AI, nút Đã hiểu quay lại đúng tiến độ; mới kiểm chứng end-to-end bằng Vật lý, chưa gọi AI thật.
- [x] Quiz sau mỗi 5 bài; review dùng chung màn làm câu hỏi, engine hỗ trợ 10/15 câu và đúng nhóm 5 bài trước.
- [x] Có màn chúc mừng và hiệu ứng scale nhẹ, có xét cài đặt animation của hệ thống.
- [ ] Nội dung đủ chương trình: hiện mỗi môn 5 bài lớp 10/15 câu thực hành, tổng 15 bài/45 câu. Chưa có gói lớp 11/12, cần giáo viên duyệt.
- [ ] Placement đánh giá đủ năng lực từng môn: mới đánh giá trên ngân hàng nhập môn nhỏ.
- [ ] Đường level sinh động và hiệu ứng hoàn thành theo thiết kế mong muốn: mới danh sách thẻ trạng thái/hiệu ứng nhẹ.
- [ ] Kiểm tra end-to-end riêng Hóa và Sinh, review 400 EXP trên từng môn; luồng chung có mã và test backend nhưng không coi là đã thử mọi môn.
- [ ] Tích hợp/rà soát đầy đủ công cụ đặc thù, đặc biệt mạch điện Vật lý và bảng tuần hoàn Hóa. Công cụ cũ vẫn giữ, chưa coi các lỗi đã hết.
- [ ] Học/ôn offline hoàn chỉnh, tự lưu câu Gemini tạo vào bank mới và đồng bộ bài đang dở giữa thiết bị.

## EXP, streak và quiz
- [x] Luồng mới: bài 200 EXP/review 400 EXP ở lần đầu; làm lại 0 EXP, không thưởng lặp khi gửi lại completion.
- [x] Bài/review có thể thắp streak tối đa một lần/ngày; retake thật ngày mới được thắp, replay kết quả cũ không được thắp.
- [x] Luồng mới không có đếm ngược/giới hạn giờ làm quiz.
- [x] Backend dùng chung learning day UTC+7 cho completion/dashboard/profile/ranking.
- [ ] Hợp nhất quiz/EXP/bank legacy với hệ thống mới; hiện giữ số liệu công cụ cũ riêng để tránh cộng trùng.
- [ ] Tự làm mới số liệu khi giữ màn qua nửa đêm mà không có sự kiện; hiện theo event/resume/tải lại.

## Thi đua và hồ sơ
- [x] Bảng server theo streak hoặc EXP ngày, Bạn bè/Mọi người; bằng điểm cùng hạng, hiển thị tối đa 100 người.
- [x] Bạn bè chỉ gồm quan hệ đã ACCEPT; bấm người học xem nickname/bio/avatar/streak/EXP ngày.
- [x] Bảng và hồ sơ đang xem nhận SSE; giữ bộ lọc khi recreation, giới hạn refresh đồng thời, có đường đăng nhập lại khi hết phiên.
- [ ] UI gửi/nhận/chấp nhận/từ chối/hủy lời mời kết bạn: chưa có, chưa coi tính năng bạn bè hoàn chỉnh.
- [x] Nút Settings ở góc trên trái Profile, thống kê chung, đường xem dữ liệu công cụ cũ riêng.
- [ ] Editor nickname/bio/avatar đồng bộ máy chủ: chưa xong. Avatar server đã đọc được nhưng upload trong kiểm thử dùng client QA; không đồng nghĩa editor app đã nối.
- [ ] Đổi mật khẩu server trong app: backend có endpoint, editor Android hiện vẫn cập nhật local; đây là điểm cần sửa sớm để tránh người dùng hiểu sai.

## Backend, dữ liệu và phát hành
- [x] Có FastAPI/SQLite, auth, ownership/revision, chat/project/file/friendship/ranking/events và learning engine.
- [x] Mapping tài khoản không gộp theo email; encrypted session, refresh và logout trễ có kiểm thử.
- [x] API keys và backend URL để trống trong cấu hình hiện tại; có tài liệu bàn giao tiếng Việt trong app/BAN_GIAO_BACKEND.txt.
- [ ] Realtime toàn bộ app, outbox lưu bền khi offline, giải quyết xung đột/sync đầy đủ mọi dữ liệu.
- [ ] Hoàn thiện vận hành backend: giới hạn body chunked, tính nguyên tử tệp/metadata, dọn tệp, email thật và triển khai HTTPS.
- [ ] Hoàn tất chuyển dữ liệu, gỡ sạch luồng cũ đã bị thay thế và rà soát SOLID toàn mã cũ.
- [ ] Đưa bản đại tu hiện tại lên GitHub: chưa commit/push. Local HEAD a7772e1; remote cấu hình Loc5783/AI-Mentor. Chưa fetch để kết luận trạng thái GitHub mới nhất trong lượt audit này.

## Bằng chứng kiểm thử đã lưu
- Build debug/release/test APK và lint PASS: rankings-checked-build-verification.log.
- Tổng JVM: 173 test, không lỗi, theo báo cáo đã lưu.
- Backend: 23 test đạt ở lượt trước; không phải kiểm thử backend mới ngày 20/10.
- Bảy ca connected đã đạt qua các lượt; lượt đầu 5 đạt/2 setup bị 429, lượt chạy lại 3 ca đạt (có một ca trùng). Không gọi đó là một lượt 7/7 PASS.
- Full Android regression 95 tests thuộc mốc trước thay đổi bảng xếp hạng mới; không coi là xác nhận tất cả màn hiện tại.
- Chưa kiểm chứng AI thật, điện thoại thật và hệ thống Internet production.

## Thứ tự ưu tiên đề xuất
1. Hoàn thiện riêng ba môn: nội dung, review, công cụ đặc thù; kiểm chứng từng môn, thiết kế đường level và hiệu ứng.
2. Sửa editor hồ sơ/đổi mật khẩu để cập nhật đúng tài khoản server; hoàn thiện quản lý lời mời.
3. Nối cloud chat, bank/offline/outbox và tiến độ đa thiết bị; hoàn tất migration legacy.
4. Hồi quy sáng/tối/VI-EN/responsive/thiết bị thật, cấu hình AI thật trong môi trường riêng, chuẩn bị triển khai và phát hành.
