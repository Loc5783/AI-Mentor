# Tiếp tục AIMentor — 19/10/2026

## Bước mới nhất: thi đua, hồ sơ công khai và tiêu chí SOLID
- Yêu cầu mới: giữ SOLID làm tiêu chí chung. Đã lưu AGENTS.md trong repository và ghi vào REQUIREMENTS_AUDIT_VI.md; không tạo lại quy tắc main/integration đã xác nhận gửi nhầm.
- Đã nối CompetitionRepository, PublicProfileRepository và các adapter backend. CompetitionQuery/RankingBoard/PublicLearner là dữ liệu domain; CompetitionJson là biên chuyển schema. RankingBoard xác nhận thứ tự, đồng hạng 1/1/3, metric, ngày học, ID không trùng và dữ liệu bất biến. Không xếp theo level.
- CompetitionViewModel giữ selection bằng SavedStateHandle, chặn dữ liệu của account khác và kết quả sai filter. Chỉ một request hoạt động, burst giữ một refresh tiếp theo; kết quả hiện tại vẫn được công bố, tránh starvation khi nhiều sự kiện tới liên tiếp.
- CompetitionFragment chỉ xử lý view/selection/lifecycle; bảng dùng một RecyclerView chứa cả header để cuộn được trên màn nhỏ/chữ lớn. Có loading/error/local/not-connected/accepted-friends-empty, ghi rõ tối đa 100 người và đồng điểm cùng hạng. Giữ số liệu cá nhân sẵn có lúc đổi metric. Nút tải lại chuyển thành đăng nhập khi session invalid, đi qua logout chuẩn.
- PublicLearnerDialog/PublicProfileViewModel nối hồ sơ qua /profiles/{id}, chỉ public nickname/bio/streak/dailyXp/day/avatar. Ảnh qua BackendAvatar, same-origin/header auth/no redirect, tối đa 4 MiB và BitmapFactory lấy mẫu theo kích thước ~512px. Metadata có thể hiện trước khi ảnh tải xong; ảnh lỗi không làm mất hồ sơ.
- Mở rộng SSE bằng EventPolicy ở LiveOverviewUpdates, giữ constructor/policy summary cũ. Board lắng nghe RANKING/PROFILE/FRIENDSHIP/COMPLETION; dialog chỉ PROFILE đúng ID. Không giữ khóa session suốt stream hoặc tải ảnh.
- rankings-checked-build-verification.log: debug/release/test APK và lint PASS. Tổng JVM hiện tại 173 tests, 0 failures/errors; 9 test mới về rank/tie/metric/ngày/immutability/event scope.
- rankings-connected-verification.log: lượt đầu 7 ca có 5 đạt và 2 thất bại ở setup vì /auth/sign-up giới hạn 5 lần/phút. Không ghi lượt đó là PASS và không nới bảo vệ production. Ca board/profile realtime và ca 100 refresh chỉ tải hai lượt đã đạt trong lượt này.
- rankings-retest-verification.log: chạy lại riêng 3 ca trên bản cuối, OK (3 tests), 44,695 giây. Gồm ranking/profile/ảnh/filter/recreation/session-expired/login action; email không gộp nhầm; luồng placement/lesson/help/chat return/EXP/streak. Như vậy cả 7 ca chức năng đã đạt qua các lượt, không phải một full regression mới.
- Ca ranking: tạo người thứ hai trên backend QA, chưa ACCEPT thì FRIENDS chỉ có mình, GLOBAL có dữ liệu; đổi XP/FRIENDS, người kia ACCEPT thì bảng tự thêm; bấm profile, đổi nickname/bio và upload ảnh từ client kia, dialog tự cập nhật. Recreation giữ metric/scope. Xóa session rồi refresh cho nút đăng nhập thật đưa về MainActivity.
- Bằng chứng: ../AIMentor_QA/rankings/public-profile.png; log bản sao trong cùng thư mục. Screenshot dùng dark theme, font 1.3, màn QA compact và điều hướng ba nút; nội dung dialog cuộn, nút đóng luôn ở ngoài vùng cuộn. Không gọi Gemini/AI trả phí.
- Chưa có UI gửi/nhận/hủy lời mời kết bạn; FRIENDS hiện phản ánh quan hệ server đã ACCEPT. Editor hồ sơ vẫn local, chưa đồng bộ editor/onboarding/avatar upload production. Chat cloud/bank/outbox/lesson state nhiều thiết bị và deployment còn tiếp tục. Đang xem qua ngày mới chưa có timer invalidation riêng; hiện cập nhật theo event/resume/manual refresh.
- URL/key vẫn trống; APK và code lưu local, chưa commit/push. Những mục phía dưới là lịch sử của các bước trước.
- Đã đóng backend và emulator QA riêng sau kiểm chứng; không tác động emulator-5554 của người dùng.

## Bước tiếp theo: realtime thống kê và SOLID
- Phạm vi: invalidation của Dashboard, Profile stats, số liệu cá nhân Thi đua khi màn đang hiển thị. Không tuyên bố chat/ranking/lesson state đa thiết bị đã sync.
- AppServices cung cấp OverviewUpdates; UI chỉ đăng ký callback và hủy ở onPause. EventStream là hợp đồng nguồn, BackendEventStream phụ trách SSE/session/cursor, SseReader xử lý framing có giới hạn, OverviewEventPolicy lọc thay đổi đúng phạm vi, LiveOverviewUpdates xử lý reconnect/coalesce/cancel. Có thể thay nguồn mà không sửa Fragment.
- Cursor được lấy trước lần refresh sau khi nối stream, tránh mất thay đổi giữa snapshot và subscribe. Reconnect giữ Last-Event-ID; khởi động lại subscription lấy cursor mới rồi refresh. Không lưu nội dung chat/token trong event, không giữ session lock suốt stream.
- Gộp sự kiện trong 500 ms, backoff 1–30 giây; không tự retry mutation. Chặn callback sau khi close/đổi ActiveUser, bỏ public profile người khác và sự kiện lưu từng câu khỏi refresh summary cá nhân. Dashboard giữ đồ thị hiện tại khi refresh nền.
- realtime-build-verification.log: debug/test APK, lint, JVM PASS. Tổng báo cáo JVM hiện tại 164 tests, 0 failures/errors; thêm 6 ca SSE framing/bounds và 3 ca event policy.
- realtime-final-build-verification.log: release và test APK PASS.
- realtime-connected-verification.log: OK (4 tests), 68,662 giây. Ca mới đăng ký lần lượt hai môn từ client khác, Dashboard tự thay tổng bài mà không chuyển tab; ba ca auth/lesson trước đó vẫn đạt. Không gọi Gemini thật.
- realtime-reconnect-verification.log: OK (1 test), 10,832 giây. Mô phỏng lỗi transport lần đầu, tự nối lần hai, gộp 100 sự kiện thành một callback và loại bỏ cả callback đang chờ lẫn sự kiện đến trễ sau close. Tổng 5 ca connected đã chạy đạt qua hai lượt (4 + 1), không phải full regression mới.
- URL/API key vẫn trống, chưa deployment và chưa commit/push. Các mục dưới đây là lịch sử trước bước SSE này.

## Bước mới nhất: tài khoản và thống kê chung
- KẾT QUẢ CUỐI: 23 backend test PASS (unified-learning-day-final-tests-verification.log); 155 unit test, không lỗi; full Android regression OK (95 tests), 356,247 giây (../AIMentor_QA/unified-progress/full-pass.txt). Lượt mặc định bỏ qua 11 ca opt-in: 8 Gemini thật và 3 connected backend.
- 3 ca connected chạy riêng trên backend bản cuối PASS: ../AIMentor_QA/unified-progress/canonical-connected-pass.txt. Không gọi Gemini thật.
- Đã thêm ca qua nửa đêm làm lộ public profile/ranking dùng clock khác Dashboard; có log thất bại trước sửa shared-learning-day-before-fix-verification.log. Sau sửa, completion/overview/public profile/ranking dùng learning_stats.reward_summary và cùng learning clock; ranking đọc một SQLite snapshot và một mốc giờ cho toàn bộ entries. Kiểm thử đã đạt sau sửa.
- Mã và APK QA đã lưu, môi trường QA riêng được đóng sau xác nhận; không tác động emulator-5554. Chưa commit/push hay deploy Internet. Những dòng đang chạy/22 test bên dưới là lịch sử của lượt trước khi chốt kết quả.
- MainActivity/RegisterActivity dùng AccountAccess: backend có cấu hình thì xác thực server, tạo/link local cache theo endpoint/remote ID. Chỉ gộp dữ liệu cũ khi xác minh được cả credentials local và server; không gộp chỉ vì trùng email.
- Phiên dùng chung với Lesson. Khi phiên thật sự hết hạn, xác nhận lại chỉ được cùng remote identity. Logout lấy/xóa ciphertext ngay, thu hồi phiên cũ riêng; refresh khóa chung giữa các client cùng account. Đã kiểm tra delayed logout không xóa phiên mới, đổi account không đọc được account cũ.
- Backend LearningOverview + GET /me/dashboard?grade: thống kê một snapshot transaction, tổng EXP/ngày, streak, bài/review, từng môn, tuần, đúng lần đầu/hướng dẫn. Retake/replay không inflate số bài hoặc accuracy.
- Dashboard, Profile stats, Competition own stats dùng chung LearningOverviewRepository; cache theo local user/server/remote ID/grade. Có nhãn cached/error/auth required, không trộn lại EXP legacy.
- Profile có đường xem dữ liệu công cụ cũ với nhãn riêng. Đã phát hiện nút OK bị khuất khi thông báo và custom view cùng đặt trên Material dialog; chuyển thông báo vào vùng cuộn, ca chữ lớn đã chạy lại PASS.
- 22/22 backend test PASS: unified-overview-final-backend-tests-verification.log.
- Build debug/release/test APK và lint PASS: unified-large-text-dialog-build-verification.log. Báo cáo unit toàn bộ module: 155 tests, 0 failures/errors.
- 3/3 Android–backend PASS trên bản cuối: ../AIMentor_QA/unified-progress/connected-pass.txt. Gồm Main login một lần, vào Lesson không cần login lại, học/giải thích/chat/rotation/reward, số liệu 200 EXP/1 streak/1 trên 5 bài đồng nhất, xem thống kê cũ và đóng dialog, chống merge nhầm email, concurrent refresh, delayed logout, cache isolation.
- Bằng chứng số liệu Thi đua: ../AIMentor_QA/unified-progress/shared-stats.png (là ảnh số liệu cá nhân, không phải ranking người khác).
- Full regression bản này đang chạy; chưa ghi PASS khi chưa có kết quả cuối. Các mục kiểm chứng bên dưới là lịch sử của bước trước.
- Chưa có deployment, URL/API key vẫn trống. Native SSE, cloud chat/bank, metadata/profile/onboarding đa thiết bị, outbox và danh sách ranking/public profile còn phải nối. Metadata local còn được giữ, không tuyên bố đã sync tất cả hồ sơ.
- Local account chưa liên kết/identifier mơ hồ không tự nhập vào tài khoản mới. Cần wizard liên kết tường minh cho trường hợp không thể chứng minh local identity qua identifier hiện tại; dữ liệu của chúng không bị xóa.
- Các thay đổi vẫn chưa commit/push.

Người dùng tiếp tục từ checkpoint 18/10. Ảnh quy trình Git main/integration được xác nhận là gửi nhầm từ project khác; không áp dụng cho AIMentor. Đã xóa AGENTS.md vừa tạo nhầm, trở lại codex/publish-completed và xóa nhánh rỗng feature/learning-platform. Không push hoặc thay đổi GitHub trong lượt này.

## Đã sửa và kiểm chứng
- Gradle Kotlin DSL: import Properties đúng, không còn nhầm extension java với package java.util.
- Cấu hình mạng debug chỉ cho HTTP ở 127.0.0.1, thêm includeSubdomains=false theo lint; release vẫn yêu cầu HTTPS.
- Session backend mã hóa Keystore/AES-GCM theo tài khoản/endpoint; nếu ciphertext hoặc khóa không hợp lệ thì yêu cầu đăng nhập lại. Kiểm tra account trước/sau refresh để tránh trả dữ liệu của phiên cũ.
- LessonViewModel không tạo/cập nhật SavedStateHandle từ worker; chặn thao tác nộp kép khi đang gửi hoặc còn mutation chưa xác nhận.
- Các kiểm thử điều hướng cũ chuyển sang màn Lesson mới và settings tách riêng trong Profile; không giữ assertion đòi giao diện cũ.
- Java mới đã format. Debug, release, test APK và lint PASS: resumed-learning-device-build-verification.log.
- Backend 17/17 pytest PASS: resumed-backend-tests-verification.log.
- ConnectedLearningTest PASS trên Android thật trong emulator kết nối FastAPI/SQLite cục bộ: ../AIMentor_QA/connected-learning-pass.txt. Kiểm tra placement 0 EXP/streak, học bài, sai hai lần có hướng dẫn, recreation, gửi câu qua Chat AI, Đã hiểu quay lại đúng step, 200 EXP/streak và nộp kết quả lại không tăng tổng EXP.
- Ảnh bằng chứng: ../AIMentor_QA/connected-learning-completion.png.
- Lần chạy connected đầu bị hộp thoại System UI ANR che app, không tính PASS. Đã đóng đúng hộp thoại hệ thống trên QA emulator, chạy lại và đạt.
- API key vẫn trống; không gọi Gemini thật. Native integration dùng backend local, không giả định đã có deployment Internet.
- Hợp đồng API thực tế được xuất riêng: docs/backend/runtime-openapi.json. docs/backend/openapi.json trước đó là bản thiết kế, chưa phải tất cả đều đã triển khai.
- APK debug đã build được lưu ../AIMentor_QA/AImentor-debug.apk.

## Giới hạn còn tồn tại
- Ba gói thật mới có 15 bài/45 câu nhập môn lớp 10. Chưa có gói lớp 11/12, chưa phải toàn bộ SGK và cần giáo viên duyệt.
- Luồng học mới cần MENTOR_BACKEND_URL HTTPS và đăng nhập đồng bộ. Chưa hợp nhất hoàn toàn auth local/server, chưa migrate account/chat legacy.
- Android chưa nối SSE/outbox, ranking/public profile/dashboard server; thống kê legacy chưa hợp nhất ledger server. Chưa coi đại tu hoàn tất.
- Lộ trình native hiện là danh sách thẻ theo trạng thái, chưa hoàn tất đường level sinh động và hiệu ứng theo yêu cầu.
- Backend còn cần giới hạn body chunked, ghi metadata/tệp nguyên tử, dọn tệp và vận hành deployment/email thật.

## Kết quả cuối bước kiểm chứng
- Full instrumented regression: OK (93 tests), 345,892 giây, không lỗi. Chín ca opt-in (8 Gemini thật, 1 backend-connected) không chạy trong lượt mặc định; ca backend-connected đã chạy riêng và đạt như trên. Báo cáo: ../AIMentor_QA/resumed-learning-full-pass.txt.
- Báo cáo unit test hiện có toàn bộ module: 149 tests, 0 failures, 0 errors.
- Backend pytest: 17 tests đạt. Không coi kiểm thử giả lập là xác nhận trên điện thoại thật.
- Đã lưu APK/báo cáo/mã nguồn, chưa commit hay push. Đóng môi trường backend và emulator QA riêng sau kiểm tra, không tác động emulator-5554.

## Công việc tiếp theo
Nối thống kê/ranking/public profile với backend và realtime; hợp nhất auth và lưu trữ cũ, làm offline/outbox; hoàn thiện đường level và nội dung ba môn. Chưa tuyên bố toàn bộ đại tu hoàn tất.
