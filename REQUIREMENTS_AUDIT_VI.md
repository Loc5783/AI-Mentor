# Rà soát yêu cầu đại tu AI Mentor
Cập nhật kiểm tra ngày 20/10/2026 theo mã nguồn và kết quả kiểm thử đã lưu tới 19/10. Hôm nay chưa chạy lại toàn bộ app. Checklist tổng hợp: CHECKLIST_TIEN_DO_2026-10-20.md. Tài liệu này thay mô tả cũ nếu có mâu thuẫn. Ảnh quy trình Git gửi ngày 19/10 thuộc project khác, không áp dụng cho AIMentor.

## Phạm vi đã chốt
- Cập nhật sau lượt hiệu ứng/API20/10: xem RESUMED_PROGRESS_2026-10-20.md. Nguồn nội dung chốt theo Kết nối tri thức năm học2026–2027, lớp10–12; chưa mở rộng runtime. Ba ca chat Gemini thật đạt, chưa kiểm chứng mọi quiz/ảnh và toàn bộ nội dung.
- Làm cả frontend và backend.
- SOLID là tiêu chí chung cho mọi phần phát triển/sửa đổi từ đây về sau; lưu hướng dẫn trong AGENTS.md của repository. Tách trách nhiệm và phụ thuộc vào hợp đồng phù hợp, tránh chia lớp hình thức.
- Đợt này triển khai đầy đủ luồng học cho Sinh học, Hóa học, Vật lý; sáu môn Toán, Văn, Anh, Sử, Địa, GDKT chỉ để khung cho người khác.
- Chat AI chung vẫn hỗ trợ mọi môn, giải thích theo lớp 10/11/12.
- Giữ giao diện auth đã duyệt. Tái sử dụng phần cũ còn tốt; phần bị thay thế phải gỡ sau khi chuyển dữ liệu/điểm điều hướng, không giữ hai luồng trùng nhiệm vụ.
- Không công bố app đã hoàn tất khi mới có khung, nút hoặc hợp đồng API.

## Tiêu chí và tình trạng thực tế
| Yêu cầu | Tình trạng |
|---|---|
| Năm mục đúng thứ tự Dashboard · Chat AI · Lesson · Thi đua · Profile | Đã có trong bản đang phát triển, kiểm tra bằng app thật; chưa cập nhật bản đã cài trên máy anh |
| Dashboard gọn, biểu đồ dữ liệu thật, không trống vô nghĩa | Có loading/empty/error/partial/data và CTA; tài khoản đã nối backend dùng thống kê bài học mới, EXP/ngày/streak/tiến độ và đúng lần đầu, không coi EXP là độ hiểu. Tài khoản local giữ bố cục/số liệu cũ |
| Chat hỏi mọi môn theo lớp 10–12 | Adapter/prompt đã có; 3 ca Gemini thật Lý/Hóa/Sinh theo lớp10/11/12 đạt ngày20/10 bằng key QA riêng. Cấu hình public trống, chưa kiểm chứng đủ chín môn/ảnh/tệp |
| Nhiều cuộc chat, thư mục, đổi tên/move/xóa | Đã nối UI và SQLite theo account; xóa thư mục chọn giữ hoặc xóa chat |
| Tải tệp, ảnh, camera và giữ bản nháp | Đã nối picker/camera, lưu bản sao riêng theo account; cần tiếp tục thử camera thật và mất process khi picker mở |
| Lịch sử chat cũ được tích hợp | Chưa chuyển đủ lịch sử từ bốn module cũ; đây là điều kiện trước khi gỡ chat cũ |
| Lesson chọn lớp/môn, thêm môn về sau | Đã có lựa chọn ba môn, lưu SQLite theo account/lớp; sáu môn khác chỉ có khung |
| Kiểm tra đầu vào từng môn | Backend/native có bài nhập môn 10 câu, không thưởng, đã chạy Android–backend; chưa đánh giá toàn chương trình, mới có gói lớp 10 |
| Lộ trình level sinh động giống tinh thần Duolingo | Đã có chặng đánh số/đường nối/progress, review riêng và motion nhẹ; completion một lần, reduced motion có kiểm tra. Chưa coi là hoàn tất mọi màn/toàn bộ thiết kế mong muốn |
| Bài học giải thích → ví dụ → dễ/vừa/khó; sai hai lần có hướng dẫn | Backend/native đã nối; Android–backend kiểm tra sai hai lần và giữ tiến độ thành công |
| Hỏi AI thêm, Đã hiểu, trở lại đúng tiến độ | Native chuyển câu hỏi sang chat chung và quay lại bài đã kiểm tra cùng recreation; chưa gọi Gemini thật do key trống, chat này còn lưu local |
| Mỗi 5 bài có quiz 10/15 câu, chỉ ôn đúng 5 bài trước | Engine tạo checkpoint, chặn bài thứ sáu trước review; backend test cả 10/15 câu và nhóm 6–10; gói thật hiện chọn 10 câu |
| Cùng một kiểu quiz/cách chấm/EXP, không countdown | Ba module học mới dùng chung LessonActivity/ViewModel, có thể mở review và làm câu hỏi; không countdown. Backend kiểm tra review 10/15 câu và thưởng. Chưa kiểm chứng đủ review end-to-end riêng từng môn; quiz legacy còn cần hợp nhất ledger |
| Lần đầu bài +200 EXP, review +400 EXP; làm lại 0 EXP | Ledger backend và native lesson hoạt động; 200 EXP native đã kiểm tra, review 400/retake/concurrent completion đã kiểm tra backend; quiz legacy chưa hợp nhất vào ledger |
| Bài và quiz đều thắp streak tối đa một lần/ngày; làm lại vẫn thắp được ngày mới | Backend đã kiểm tra mốc nửa đêm UTC+7, replay cũ, retake, giao dịch đồng thời; native bài học đã kiểm tra; hiển thị thống kê legacy còn cần hợp nhất |
| Thi đua streak/EXP ngày, bạn bè/mọi người | Đã nối danh sách backend và SSE, dùng rank server, đồng điểm cùng hạng, giới hạn 100 người có thông báo. Bạn bè chỉ gồm quan hệ ACCEPTED. UI quản lý lời mời kết bạn còn phải làm |
| Bấm người trên ranking mở public profile | Đã nối hồ sơ công khai: biệt danh, bio, EXP ngày, streak, ảnh qua API có xác thực; cập nhật PROFILE khi đang xem. Không hiển thị email/tên đăng nhập riêng tư |
| Profile: nút settings góc trên trái | Đã thay phần settings nhúng dài bằng nút riêng |
| Profile: ảnh, biệt danh, ghi chú, streak/EXP ngày | Xem public profile và stats đã nối backend. Editor ảnh/tên/mật khẩu hiện còn local; chưa có editor cloud hoàn chỉnh cho nickname/bio/avatar |
| Sáng/tối, VI/EN, đổi mật khẩu trong settings | Có theme/locale dùng chung và kiểm thử trước đó; cần hồi quy toàn bộ màn mới. Đổi mật khẩu trong editor hiện gọi local repository, chưa nối /auth/change-password trên Android |
| Backend auth/chat/profile/tệp/bạn bè/events | Backend đã chạy; MainActivity/RegisterActivity dùng phiên server chung với Lesson khi có cấu hình. Link local chỉ sau khi chứng minh cả hai credentials. Chat/files/public metadata/bạn bè Android chưa nối toàn bộ |
| Backend enrollment/placement/lesson/review/completion/bank | Engine/API đã có, bộ backend tổng cộng 23 test đạt ở lượt 19/10; ba gói thật nhập môn lớp 10, cần thêm nội dung lớp 11/12 và giáo viên duyệt |
| Realtime nhiều thiết bị, outbox offline, xung đột revision | Android đã nối SSE cho summary, bảng thi đua và hồ sơ công khai đang xem; có cursor/reconnect/coalesce/lifecycle/account guard. Chưa sync toàn bộ chat/profile editor/lesson state; outbox chưa nối |
| Câu hỏi AI lưu bank phân loại và dùng offline | Bank quiz cũ đã có; phân loại theo lesson/version/grade và bank mới chưa hoàn tất |
| Loại bỏ giao diện/logic cũ đã bị thay thế | Chưa xong; không xóa mù trước khi chuyển dữ liệu và bảo toàn công cụ |
| Responsive Android, ba nút/vuốt, keyboard, chữ lớn | Có safe-area dùng chung; còn phải chạy ma trận hồi quy cho các màn mới |

## Các điều kiện logic cần giữ
1. EXP chỉ cấp lần đầu theo account/môn/lớp/đơn vị bài ổn định; bỏ/thêm lại môn hoặc sửa bản nhỏ không reset quyền nhận thưởng.
2. Completion cũ phát lại không được thắp streak ngày mới. Làm lại thực sự tạo attempt mới, 0 EXP, có thể thắp streak.
3. Quiz không được tính thành bài thứ sáu khi chia nhóm; quiz 2 ôn bài 6–10.
4. Không dùng điểm EXP để tuyên bố người học hiểu bao nhiêu phần trăm kiến thức. Đánh giá cần dựa vào câu trả lời, độ khó và mức trợ giúp.
5. Nút Đã hiểu chỉ quay lại bài; không tự hoàn thành câu/bài hoặc cấp thưởng.
6. Không giả user, bạn bè, rank, số liệu biểu đồ hay thông báo gửi email thành công.
7. Lớp trong hồ sơ được dùng lại, không bắt chọn lại hai lần vô ích. Khi Lesson mới sẵn sàng, bỏ bước chọn bốn môn legacy trùng nhiệm vụ ở onboarding.
8. Nội dung phải có nguồn/quyền sử dụng và được kiểm chứng. Không gọi bài AI tạo là SGK chính thức.
9. Các công cụ đặc biệt còn tốt phải giữ: máy tính riêng Toán trong kho công cụ, mạch điện Lý, bảng tuần hoàn Hóa.
10. Backend là nguồn chuẩn cho thưởng/ranking/quyền dữ liệu. Không nhận EXP hay userId tùy ý từ client để cấp quyền/thưởng.

## Phần nên phát triển thêm sau luồng chính
- Học tiếp đúng câu đang dở sau mất mạng hoặc đóng app.
- Ôn câu hay sai, phân biệt làm đúng tự lực và làm đúng sau trợ giúp.
- Phản hồi báo câu hỏi sai/khó hiểu để kiểm duyệt ngân hàng.
- Hiển thị trạng thái đồng bộ rõ ràng, retry không tạo bản ghi/thưởng trùng.
- Hiệu ứng hoàn thành ngắn, chỉ phát một lần và tôn trọng chế độ giảm chuyển động.

Ưu tiên hoàn thành và kiểm chứng luồng chính trước khi mở rộng tính năng phụ.

## Bước thống nhất tài khoản và Dashboard — 19/10/2026
- GET /me/dashboard?grade=10|11|12 tổng hợp trong cùng SQLite read transaction; retake/replay không cộng lặp bài, câu trả lời hoặc EXP. Điểm đúng lần đầu khác với đúng sau hướng dẫn.
- Main login/register tạo encrypted session dùng chung; cache/mapping theo endpoint + remote ID + local owner, thống kê tách thêm grade. Mật khẩu local đúng nhưng server sai không được bypass server.
- Chưa có server Internet; URL trong api.properties vẫn trống. Luồng mới được kiểm tra với FastAPI cục bộ qua máy giả lập QA riêng.
- 23 backend test, 155 unit test và 3 ca Android–backend đạt. Full regression OK (95 tests; 11 ca opt-in bỏ qua trong lượt mặc định, 3 backend-connected đã chạy riêng). Không dùng các con số này để tuyên bố tất cả đại tu đã hoàn tất.
- Completion, Dashboard, public profile và ranking hiện dùng chung learning day/streak policy; đã kiểm tra trước/sau nửa đêm UTC+7. Ranking dùng một mốc giờ và snapshot chung cho cả danh sách.

