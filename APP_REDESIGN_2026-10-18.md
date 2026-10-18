# Đại tu AI Mentor — đặc tả triển khai theo giai đoạn

**PHẠM VI MỚI NHẤT:** chỉ triển khai luồng Lesson đầy đủ cho Sinh học, Hóa học và Vật lý trong đợt này. Sáu môn còn lại giữ khung module, chưa có bài/đầu vào/quiz mới; UI phải ghi Sắp có và không mở luồng rỗng. Chat AI chung vẫn hỗ trợ tất cả môn theo lớp 10/11/12. Các đoạn mô tả 9 môn là kiến trúc mở rộng, không phải cam kết có đủ nội dung 9 môn ở đợt hiện tại.
Ngày: 18/10/2026. Trạng thái: bước nền móng đã có code; các màn hình mới và backend chưa hoàn tất.

## Quyết định đã chốt với người dùng
- Giữ giao diện đăng nhập/đăng ký đã duyệt (bản f674b6d). Không thiết kế lại phần này.
- Năm mục cố định: Dashboard · Chat AI · Lesson · Thi đua · Profile.
- Kiến trúc chín môn, mỗi môn một module (đợt này chỉ triển khai Lesson Sinh/Hóa/Lý, còn lại giữ khung): Toán, Ngữ văn, Tiếng Anh, Lịch sử, Địa lý, Hóa học, Vật lý, Sinh học, Giáo dục kinh tế và pháp luật (GDKT).
- Chat dùng thao tác gần ChatGPT, giải thích theo lớp 10/11/12 trong hồ sơ. Tiếp tục cơ chế API key của chat cũ. Chưa có backend đồng bộ; chuẩn bị hợp đồng API.
- Lần hoàn thành đầu tiên mỗi bài học: 200 EXP. Sau mỗi 5 bài có quiz ôn tập 10 hoặc 15 câu, chỉ ôn 5 bài ngay trước đó; hoàn thành quiz lần đầu: 400 EXP. Làm lại hoàn chỉnh nhận 0 EXP nhưng vẫn có thể thắp streak ngày mới.
- Cả bài học và quiz ôn tập đều có thể thắp streak; ngày đã thắp không tăng thêm streak, nhưng vẫn nhận EXP của lần hoàn thành hợp lệ.
- Không có đồng hồ đếm ngược hoặc giới hạn thời gian quiz.
- Không xóa chức năng riêng từng môn hay dữ liệu cũ.

## Bước 1: ranh giới module (code đã có)
| Module | Trách nhiệm |
|---|---|
| app | Năm mục điều hướng, ghép các provider, account hiện hành, liên kết giữa các mục |
| learning-core | Danh tính 9 môn, lớp học, hợp đồng module, quy tắc quiz/reward, con trỏ quay lại bài học |
| chat-core | Request chat có lịch sử, ngữ cảnh lớp, tệp đính kèm, adapter Gemini và pool key riêng |
| math | Toán; giữ công cụ máy tính bong bóng riêng môn Toán |
| literature | Ngữ văn |
| english | Tiếng Anh |
| history | Lịch sử; giữ công cụ và dữ liệu hiện hữu |
| geography | Địa lý |
| chemistry | Hóa học; sở hữu chức năng bảng tuần hoàn khi chuyển từ Science |
| physics | Vật lý; sở hữu chức năng mạch điện khi chuyển từ Science |
| biology | Sinh học |
| economics-law | Giáo dục kinh tế và pháp luật |
| ui-core | Thành phần giao diện, kích thước, theme, hiệu ứng, safe area dùng chung |
| data-core | Cơ chế SQLite và dữ liệu chung |
| science (chuyển tiếp) | Giữ hoạt động Lý/Hóa/Sinh cũ cho đến khi di chuyển xong, không còn được coi là một môn mới |
| programming (công cụ bổ sung) | Giữ chức năng lập trình cũ, không tự thay thế một môn trong 9 môn đã chốt |

Bảy module môn mới hiện có entry contract, chưa có nội dung bài học mới. Math/History vẫn giữ code hiện hữu và cùng triển khai entry contract. Science chưa bị di chuyển hàng loạt. Không tuyên bố đã tách hết công cụ Science.

Mỗi môn phụ thuộc learning-core, không phụ thuộc app hay một môn khác. SubjectModules là điểm ghép duy nhất và kiểm tra đủ 9 môn, không trùng ID. Khung bài học/quiz/thưởng dùng chung; môn chỉ cung cấp nội dung, công cụ và cấu hình chuyên môn.

## Bước 2: cấu trúc màn hình và dữ liệu theo account
1. Dashboard chỉ có tóm tắt học hôm nay, biểu đồ và nút học tiếp. Độ chính xác/mức độ nắm kiến thức lấy từ đánh giá, không dùng EXP để giả làm phần trăm hiểu bài. Trạng thái chưa có dữ liệu có lời giải thích/nút bắt đầu; lỗi tải có thử lại. Dữ liệu quiz cũ vẫn xem được, ghi nhãn nguồn.
2. Chat AI: hàng trên cùng có mở lịch sử, tên chat, chat mới. Nội dung ở giữa, composer cố định phía trên bàn phím/thanh hệ thống. Lịch sử có tìm kiếm, chat gần đây và thư mục; mỗi chat có đổi tên, di chuyển, xóa. Xóa thư mục phải cho lựa chọn giữ chat ngoài thư mục hoặc xóa kèm, có xác nhận rõ.
3. Lesson: chọn lớp và môn Sinh/Hóa/Lý khi vào lần đầu; sáu môn còn lại có nhãn Sắp có và không cho bắt đầu; đã có lớp thì dùng lại, không hỏi lại vô ích. Sau đó danh sách môn đã chọn, nút thêm/quản lý môn. Vào một môn: bài kiểm tra đầu vào nếu chưa làm; tiếp theo là đường học với nút học tiếp và các level.
4. Thi đua: hai tiêu chí Streak và EXP hôm nay; lọc Bạn bè/Mọi người. Khi chưa có máy chủ, hiển thị trạng thái chưa kết nối; không tạo đối thủ hay thứ hạng giả.
5. Profile: cài đặt ở góc trên trái; ảnh đại diện, biệt danh, ghi chú công khai, streak, EXP hôm nay. Hồ sơ người khác chỉ hiển thị dữ liệu công khai; không lộ email, tài khoản đăng nhập hay tuỳ chọn riêng tư.

Danh sách cài đặt gồm sáng/tối, ngôn ngữ, hồ sơ học, đổi mật khẩu và đăng xuất. Không nhúng tất cả cài đặt ngay trong profile như hiện tại.

Lựa chọn môn từ onboarding cũ cần chuyển đổi có chủ đích: MATH/HISTORY khớp môn mới; SCIENCE phải cho chọn Lý/Hóa/Sinh, không âm thầm đăng ký cả ba. PROGRAMMING vẫn là công cụ bổ sung. Không ép account cũ chạy lại đăng ký.

## Bước 3: Chat AI chung
- Lưu hội thoại, thư mục, tin nhắn và tệp theo account. Chat mới không xoá chat cũ. Đổi tên/xóa/move cập nhật danh sách ngay trên thiết bị; đồng bộ máy khác sẽ nối sau.
- Gửi lịch sử đúng thứ tự user/model và câu hiện tại; không gửi toàn bộ các chat khác. Không trộn lịch sử giữa account hoặc giữa thư mục.
- TutorInstructions đặt lớp/ngôn ngữ và phạm vi 9 môn trong systemInstruction; user text/tài liệu ở contents riêng. Thiếu lớp không tự đoán lớp 10; yêu cầu chọn lớp.
- CHAT_GEMINI_API_KEY_1/2/3 và CHAT_GEMINI_MODEL nằm trong api.properties cùng các section cũ. Các key trống trên Git; không mượn key một môn ngầm định.
- Giữ cơ chế key chính/dự phòng, loại trùng, tạm ngưng key 429 trong 60 giây, loại key 401/403 khỏi pool cho phiên hiện tại. Gửi lại sau lỗi mạng/server do người dùng chủ động để tránh tạo nhiều lượt sinh không rõ kết quả.
- Adapter hiện nhận JPEG/PNG/WebP, PDF và TXT; giới hạn ứng dụng 4 MiB/tệp, 4 tệp/lượt, 8 MiB tổng payload nhị phân/lịch sử. Đây là giới hạn ứng dụng, không phải giới hạn tối đa của Gemini.
- UI chọn tệp/ảnh/camera cần kiểm tra MIME và dữ liệu thực, sao chép vào vùng riêng theo account. Lưu URI/path nội bộ, không đưa đường dẫn thiết bị lên API backend. Tệp mất/hỏng báo rõ; không giả đã đọc.
- Hiển thị trạng thái đang trả lời; lỗi cho thử lại, không thêm câu trả lời giả. Phản hồi bị cắt có nhãn. Hủy request không xóa nội dung đã nhập; chuyển tab không tạo request mới.
- Request budget hiện 41 turns, 120000 ký tự. UI phải giải thích khi cần mở chat mới/rút bớt tệp, không âm thầm bỏ mất ngữ cảnh.
- Nền tảng API đã có; UI chat, SQLite chat/project, camera và lịch sử mới chưa nối ở bước nền móng.
- Tham chiếu kỹ thuật: https://ai.google.dev/api/generate-content ; https://ai.google.dev/gemini-api/docs/generate-content/text-generation

## Bước 4: Lesson, kiểm tra đầu vào và đường học
- Mỗi enrollment gắn account, môn, lớp và phiên bản chương trình. Nội dung được biên soạn/kiểm chứng và ghi nguồn; không sao chép nguyên SGK có bản quyền hoặc gọi nội dung AI là SGK chính thức.
- Đầu vào là trắc nghiệm để ước lượng mức bắt đầu. Thay lớp cần xác nhận tạo lộ trình mới; giữ lịch sử cũ, không đè lên attempt đang học.
- Bài học có hướng dẫn/định nghĩa → ví dụ → thực hành dễ → vừa → khó vừa phải. Dùng chung bố cục và điều khiển giữa 9 môn.
- Đếm sai theo từng câu trong cùng attempt. Sai hai lần mở giải thích chi tiết, ghi trạng thái đã được trợ giúp. Không tự bỏ qua câu hoặc tạo attempt khác.
- “Hỏi AI thêm” lưu tiến độ trước, mở đúng chat với câu hỏi/phần đang mắc. LessonHandoff có enrollmentId/attemptId/lessonId/questionId/curriculumVersion/step. “Đã hiểu!” quay về cùng attempt và bước, không reset câu/trả lời/lượt sai.
- Mỗi nhóm 5 bài mới có quiz ôn. Quiz không được tính là bài thứ sáu khi chia nhóm tiếp theo.
- Ví dụ: bài 1–5 → quiz 1; bài 6–10 → quiz 2. Quiz 2 không hỏi lại bài 1–5.
- 10 câu = 2 câu mỗi bài; 15 câu = 3 câu mỗi bài. Chọn coverage mode theo nội dung; không có tuỳ chọn 5 câu cho loại quiz ôn này. Ngân hàng cũ 5/10/15 chưa tự đổi vì có chức năng độc lập.
- Câu tạo bởi AI cần lưu ngân hàng kèm môn/lớp/bài/phiên bản/độ khó/nguồn/trạng thái kiểm chứng để ôn offline.
- Animation đường học, mở level và chúc mừng ngắn, tôn trọng tuỳ chọn animation hệ thống; không chặn nút học tiếp.

## Bước 5: phần thưởng và streak (policy mới có code, chưa bật vào luồng cũ)
- Lần hoàn thành đầu tiên của mỗi đơn vị bài: LESSON 200 EXP; FIVE_LESSON_REVIEW 400 EXP. Làm lại hoàn chỉnh nhận 0 EXP nhưng có thể thắp streak ngày mới. Hoàn thành hợp lệ lần đầu mới có EXP; mở trang kết quả hoặc bấm “Đã hiểu!” không tạo thưởng.
- Mỗi attempt có một completion ID/idempotency key ổn định. Cùng completion gửi lại do mạng, rotation hay đồng bộ phải trả lại kết quả cũ, không cộng lần hai.
- Thưởng, daily EXP và streak ghi cùng transaction. Ngày đã có completion hợp lệ chỉ cập nhật EXP; streak không tăng thêm.
- Ngày học do server quyết định; giai đoạn đầu dùng chung Asia/Ho_Chi_Minh cho streak, EXP ngày và ranking để mọi người được so trong cùng một khoảng ngày. Client không được đổi timezone hay dùng đồng hồ máy để cấp thưởng. Bản offline phải ghi pending, reconcile khi có mạng.
- Làm lại bằng attempt mới cần đáp ứng đủ điều kiện hoàn thành; nhận 0 EXP và có thể thắp streak ngày mới. Award ledger khóa account/môn/lớp/rewardUnitId nên bỏ/thêm lại môn hoặc sửa phiên bản nhỏ không cấp thêm EXP. Không dùng lại event hoàn thành cũ để thắp streak ngày khác.
- Trước khi bật, nối quiz cũ và bài học mới vào một completion ledger; không chạy hai đường cộng thưởng song song. Không tự chuyển lịch sử EXP cũ thành các bài 200/400.
- Cần kiểm tra mốc nửa đêm, thiếu ngày, timezone, nhiều thiết bị, trả request lặp và đồng bộ sai thứ tự.

## Bước 6: hợp đồng backend/realtime
Đặc tả máy đọc được ở docs/backend/openapi.json (DRAFT, không phải API đang chạy). Có account/auth, hồ sơ công khai/riêng, enrollment/placement/curriculum, lesson attempt/completion, chat/project/tệp, friendship, ranking và event stream.

- API key Gemini không phải token đăng nhập hay quyền xem lịch sử. Account local hiện tại chưa tự tương đương account server. Cần luồng liên kết/migration được xác minh, không gộp người dùng chỉ theo email nhập tay.
- Server là nguồn chuẩn của ownership, ngày học, reward/ranking và quyền truy cập public profile. Client không gửi userId để tự nhận quyền.
- Mọi resource có revision; sửa/xóa có If-Match, xung đột 409/412 phải refetch/giải quyết, không ghi đè mù.
- Event stream có cursor, tombstone và revision; ngắt mạng nối lại, hết cursor thì full resync. Event chỉ thông báo resource đổi; refetch qua API có quyền tương ứng.
- Nhật ký outbox lưu thao tác offline có idempotency key. Account đăng xuất huỷ observer/network và tách cache; tệp/thư mục không bị lộ sang account sau.
- Profile ảnh/ghi chú, tin nhắn, tiến độ, dashboard và ranking dùng cùng dữ liệu chuẩn. Không gọi polling đơn thuần là realtime.
- Chưa có backend nên không xác nhận cập nhật giữa thiết bị hay bảng xếp hạng nhiều người.

## Thứ tự bàn giao để tránh xung đột
1. Chốt auth riêng → nền móng module/policy/API contract (đợt hiện tại).
2. Chat UI + lịch sử/thư mục + ảnh/tệp/camera + quay lại bài đang học; kiểm tra rotation, keyboard, offline và đổi account.
3. Lesson shell + chọn Sinh/Hóa/Lý + đầu vào + một luồng bài học mẫu đầy đủ qua engine chung; sau khi đạt mới mở rộng nội dung đủ ba môn; sáu môn khác để khung.
4. Completion ledger + reward/streak mới + quiz mỗi 5 bài + migration dữ liệu; kiểm tra chống cộng trùng.
5. Dashboard gọn + profile mới + trang profile công khai + màn thi đua, sử dụng trạng thái chưa kết nối khi chưa có server.
6. Nối backend/realtime theo contract, xử lý xung đột/mất mạng/nhiều thiết bị.
7. Hồi quy toàn app: 320dp, tablet, chữ lớn, sáng/tối, VI/EN, ba nút/vuốt, camera, tệp mất, resume lesson, circuit/calculator và dữ liệu cũ.

Không bật tab mới chỉ để dẫn vào màn hình rỗng. Kiểm tra xong luồng tối thiểu mới thay điều hướng chính.


Quyết định bổ sung: EXP chỉ nhận lần hoàn thành đầu tiên của mỗi bài/quiz (200/400). Làm lại hoàn chỉnh bằng attempt mới nhận 0 EXP nhưng có thể thắp streak ngày mới. Ledger thưởng khóa theo account/môn/lớp/rewardUnitId ổn định, không theo enrollment mới hoặc bản sửa nội dung nhỏ. Gửi lại completion cũ không được thắp streak của một ngày khác.

Điểm chặn trước khi phát hành Lesson mới: bỏ bước chọn bốn môn legacy bắt buộc ở thiết lập hồ sơ đăng ký; giữ cách trình bày auth đã duyệt, dùng lại lớp/mục tiêu và gom chọn môn về Lesson. Bản nền móng chưa thay đổi form đó vì chưa đổi điều hướng; không để hai bước chọn môn cùng xuất hiện trong bản UI mới.

## Kiểm chứng bước nền móng

Build debug/release/test APK/unit/lint đạt (learning-foundation-reviewed-build-verification.log). 149 unit tests, không thất bại; lint 0 lỗi và 534 cảnh báo. Bản phạm vi ba môn chạy full runner OK (83 tests: 75 chạy đạt, 8 API thật bỏ qua). Sau sửa factory cấu hình chat, 12/12 ca chat và Canonical smoke trên APK cuối đạt. Không có key thật/call tính phí. Chi tiết giới hạn và ca máy tính từng lỗi trong FOUNDATION_REVIEW_2026-10-18.md.
