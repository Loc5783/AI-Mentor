# Tiến độ AIMentor

## Cập nhật mới hơn — chia môn và thêm nội dung, 20/10/2026
- Đã push snapshot trước khi thêm bài lên Loc5783/AI-Mentor: 2d35d81eb6d36f4ffe4c6a2beb8234ed2d9a6e3d.
- Thêm đúng 15 bài/môn Lý–Hóa–Sinh: tổng 60 bài, 180 câu thực hành lớp 10, 4 quiz ôn/môn. Giữ nguyên 5 bài đầu, ID, đáp án, placement/review cũ và ledger; 33 ca backend PASS.
- Dashboard bỏ hẳn fallback bốn module; chỉ hiển thị môn đang chọn, biểu đồ cũng lọc cùng danh sách, chưa có dữ liệu/máy chủ có thông báo rõ.
- Bỏ source/module Programming, cấu hình, điều hướng, seeder và các ca kiểm thử riêng đã hết phạm vi. Chín môn độc lập; science-support chỉ là thư viện dùng chung/compatibility. Công cụ mạch điện thuộc physics, bảng tuần hoàn và dữ liệu nguyên tố thuộc chemistry.
- API bổ trợ quiz đổi SCIENCE_GEMINI_* thành QUIZ_GEMINI_*; Math/History và Chat giữ key pool riêng. Tất cả key Git vẫn trống. Các báo cáo cũ phía dưới là lịch sử, không phải trạng thái hiện tại.
- Chi tiết rà soát và kiểm chứng cuối ở MODULE_AUDIT_2026-10-20.md.


## Hiệu ứng nhẹ và API cũ — 20/10/2026, mới nhất
- Lộ trình Lý/Hóa/Sinh có chặng đánh số/đường nối/progress, review riêng. Tách LessonPathData/View, LessonCompletionView, SoftMotion/MotionSettings theo SOLID.
- Motion160–240ms, press0.99, không loop/icon trang trí mới; tắt motion có hỗ trợ minSDK24. Nút xuống dòng; công cụ bổ trợ mở/thu gọn giữ qua rotation, vẫn tới mạch điện/bảng tuần hoàn.
- Debug/release/test APK, lint, 173 JVM tests PASS. Bật/tắt motion mỗi lượt2/2; connected/công cụ2/2; năm tab PASS. Lỗi ID/test lifecycle ban đầu đã sửa; không tính lượt lỗi là toàn PASS.
- Dùng key từ đúng ZIP theo cho phép: 3/3 ca Gemini thật Lý/Hóa/Sinh PASS qua adapter chat. Key nạp file QA runtime, đã xóa; cấu hình public trống. Mặc định live tests SKIP đúng opt-in.
- Nội dung vẫn15 bài/45 câu lớp10. Người dùng chọn loại đang dùng lớp10–12; đã đối chiếu nguồn Chính phủ, chốt Kết nối tri thức2026–2027. Chưa thêm gói11/12. Xem docs/content/CONTENT_PLAN_VI.md.
- Chi tiết: RESUMED_PROGRESS_2026-10-20.md. APK/ảnh/log tại ../AIMentor_QA/soft-learning/. Chưa commit/push hay deployment.

## Thi đua và hồ sơ công khai — 19/10/2026, bước mới nhất
- SOLID được lưu thành tiêu chí chung trong AGENTS.md của repository. Không áp dụng quy trình Git main/integration đã gửi nhầm từ project khác.
- Đã thay CompetitionFragment cũ bằng view nhận state từ CompetitionViewModel. CompetitionRepository/PublicProfileRepository là các hợp đồng; adapter backend, DTO public, kiểm tra rank, đọc ảnh và UI được tách riêng, lắp ghép ở AppServices.
- Thi đua đã dùng GET /rankings cho STREAK/DAILY_XP × FRIENDS/GLOBAL, hiện rank server, giữ đồng hạng, thông báo tối đa 100 người và chỉ bạn ACCEPTED. List/header cùng RecyclerView, có loading/lỗi/chưa kết nối/chưa có bạn bè; không dựng người dùng giả.
- Bấm người học mở hồ sơ có nickname/bio/streak/EXP ngày/ảnh server. Nhận SSE cho board, friendship và public profile đang xem. Ảnh tải cùng origin có xác thực, giới hạn 4 MiB, decode mẫu tối đa khoảng 512 px.
- Giữ filter qua recreation, giữ số liệu cá nhân khi đổi filter; một request đang chạy chỉ giữ một lượt refresh tiếp theo, không để burst sự kiện làm kết quả mãi bị loại. Khi phiên hết hạn có nút đăng nhập lại.
- Debug/release/test APK, lint PASS; 173 JVM tests không lỗi. Bảy ca connected đã đạt qua các lượt: lần đầu 5 đạt/2 bị rate-limit setup; chạy lại 3 ca (gồm 2 bị chặn và ca ranking bản cuối) OK. Xem RESUMED_PROGRESS_2026-10-19.md.
- Chưa có UI quản lý lời mời kết bạn, profile editor cloud, chat cloud/outbox hoặc deployment Internet. URL/key vẫn trống, APK dùng backend QA khi được cấu hình. Chưa commit/push.

## Realtime thống kê và SOLID — bước tiếp theo ngày 19/10/2026
- Android đã nhận SSE cho Dashboard, thống kê cá nhân trong Profile và Thi đua khi mở màn. Khi server có enrollment/completion hoặc hồ sơ của chính tài khoản thay đổi, tải lại summary thật, không cộng EXP trên client.
- Tách EventStream (hợp đồng), BackendEventStream (xác thực/kết nối/cursor), SseReader (framing có giới hạn), OverviewEventPolicy (lọc sự kiện), LiveOverviewUpdates (gộp/reconnect/hủy), OverviewLiveBinding (kết nối lifecycle). AppServices lắp ghép; Fragment không quản lý socket/token.
- Chốt cursor trước khi refresh; nối lại vẫn giữ cursor. Không giữ khóa session suốt stream. Gộp burst 500 ms, reconnect lùi 1–30 giây, hủy khi pause/đổi tài khoản. Dashboard không xóa đồ thị đang xem trong mỗi lần cập nhật nền.
- Debug/test APK, release, lint PASS; 164 unit tests không lỗi. Năm ca Android–backend PASS qua hai lượt (4 + 1), gồm Dashboard nhận thay đổi khi vẫn ở nguyên màn, tự nối lại/gộp 100 sự kiện/hủy callback. Kết quả cuối xem RESUMED_PROGRESS_2026-10-19.md.
- Đây là realtime summary, chưa phải đồng bộ toàn app: cloud chat, metadata hồ sơ/onboarding, danh sách ranking/public profile, outbox và lesson path giữa hai thiết bị còn tiếp tục. Không có deployment; URL/key vẫn trống. Chưa commit/push.

## Tài khoản và tiến độ chung — 19/10/2026, lượt mới nhất
- Đã nối Main login/register với backend và dùng chung phiên cho Lesson khi URL được cấu hình. Liên kết local chỉ khi chứng minh cả hai credentials; không gộp theo email. Refresh đa client và logout trễ đã kiểm tra.
- Dashboard, Profile stats và số liệu cá nhân ở Thi đua dùng GET /me/dashboard; có cache riêng theo server/account/grade, trạng thái lỗi/đã lưu rõ ràng. Profile có đường xem thống kê công cụ cũ riêng, không cộng lại EXP legacy.
- Completion/overview/public profile/ranking cùng ngày học và streak policy. Ca mô phỏng qua nửa đêm phát hiện lỗi, đã sửa và kiểm tra lại.
- Build debug/release/test + lint PASS; 155 unit test; 23 backend test; 3 ca Android–backend PASS. Full regression OK (95 tests, opt-in bỏ qua theo cấu hình). Xem RESUMED_PROGRESS_2026-10-19.md và ../AIMentor_QA/unified-progress/.
- Chưa có deployment, native SSE, cloud chat/bank/outbox hoặc sync đầy đủ metadata. URL/API key vẫn trống. Mã và APK lưu local, chưa commit/push. Backend bàn giao đã cập nhật trong app/BAN_GIAO_BACKEND.txt và backend/README.md.

## Dashboard — cập nhật 19/10/2026
- Không để vòng biểu đồ rỗng và bốn thanh 0 thay cho thông báo: khi chưa có dữ liệu, ẩn nhóm đồ thị và hiện biểu tượng, lời nhắc, nút bắt đầu luyện tập.
- Phân biệt đang tải, chưa có dữ liệu, tải một phần, tải lỗi toàn bộ và có dữ liệu. Lỗi đọc không bị diễn giải thành người dùng chưa học; cho tải lại.
- Giữ bố cục Dashboard cũ, dùng VI/EN và màu theo theme; không dựng số liệu giả. Biểu đồ hiện vẫn là phân bố EXP từ nguồn cũ trên thiết bị, chưa nối thống kê bài học server.
- Build debug/test APK, unit và lint PASS: dashboard-states-build-verification.log. Sáu test trạng thái mới đạt; hai ca giao diện chạy tiếng Việt và hai ca tiếng Anh đạt (gồm đổi sáng/tối và có/chưa có dữ liệu). Báo cáo: ../AIMentor_QA/dashboard-states/vi-result.txt và en-result.txt.
- Lượt chạy đầu bị System UI ANR che cửa sổ, không tính PASS; đã đóng hộp thoại trên emulator QA riêng và chạy lại thành công.
- APK QA đã cập nhật. Chưa commit/push, chưa phải đã cập nhật app đang cài trên máy người dùng.

> ĐÃ TIẾP TỤC ngày 19/10/2026. Checkpoint tạm dừng cũ: PAUSED_PROGRESS_2026-10-18.md. Lỗi Gradle DSL và lỗi lint cấu hình mạng QA đã sửa; build debug/release/test APK và lint PASS trong resumed-learning-device-build-verification.log. Backend 17/17 test PASS trong resumed-backend-tests-verification.log. Đang kiểm tra Android với backend thật; chưa hoàn tất đại tu và chưa push. Người dùng đã yêu cầu làm cả backend. Ảnh quy trình Git main/integration được người dùng xác nhận thuộc project khác, không áp dụng cho AIMentor; tệp AGENTS.md vừa tạo nhầm đã gỡ, nhánh làm việc đã trở về codex/publish-completed.

## Lịch sử — 17/10/2026

Kiểm chứng bổ sung ngày 19/10: full device regression OK (93 tests; ca API opt-in bỏ qua theo cấu hình), ca backend-connected chạy riêng PASS, tổng báo cáo unit 149/149, backend 17/17. Chi tiết và giới hạn hiện tại: RESUMED_PROGRESS_2026-10-19.md.

Người dùng đã tiếp tục sửa trên máy, không dùng cloud. Backend máy chủ để bàn giao riêng.

## Đã hoàn thiện trong lượt sửa
- Bốn môn dùng chung tạo quiz 5/10/15 câu, bố cục làm bài/kết quả và chính sách EXP/cấp độ.
- Ngân hàng đề theo tài khoản, phân loại, tìm kiếm/lọc và ôn offline; lưu nguyên đề trong giao dịch và chống trùng bộ đề.
- Câu điền/ghép giữ ngữ nghĩa dấu âm, thập phân và loại câu hỏi khi lưu/mở lại.
- Lịch sử bốn môn dùng bố cục, tìm kiếm không dấu/toàn nội dung và nhóm/bộ lọc ngày chung; giữ mô hình hội thoại/câu hỏi thật.
- Công thức KaTeX đóng gói offline; sửa doctype HTML để công thức thực sự render khi mất mạng.
- Android 13+ dùng LocaleManager qua context, xử lý lựa chọn trước khi có Activity và đồng bộ lựa chọn ngôn ngữ của hệ thống.
- Bàn phím nhường vùng hiển thị; giữ bản nháp khi AI chưa cấu hình hoặc lưu ghi chú thất bại.
- Giữ máy tính bong bóng chỉ ở Toán, timer và công cụ mạch điện Science.

## Kiểm chứng mới nhất
- resumed-fixes-verification.log: assembleDebug, assembleDebugAndroidTest, testDebugUnitTest, lintDebug toàn project PASS; 84 unit test, 0 thất bại, 0 lỗi.
- resumed-locale-build-verification.log: debug, release và APK kiểm thử PASS sau bản sửa LocaleManager cuối.
- resumed-locale-lint-verification.log: lintDebug app PASS sau sửa LocaleManager; cảnh báo lint cũ vẫn còn.
- resumed-target-clean-runtime-verification.log: 2/2 PASS — máy tính xoay ngang và đổi ngôn ngữ/theme giữ bài, không cộng EXP lặp.
- resumed-ime-runtime-verification.log: 2/2 PASS với bàn phím thật, kiểm tra giữ câu nhập và lịch sử cả bốn môn.
- resumed-full-compact-runtime-verification.log: PASS 38/38 ca trên màn hình 320dp, font 1.3, điều hướng ba nút, offline (155,77 giây).
- Lượt chạy target đầu bị System UI ANR của giả lập che app; đã đóng hộp thoại và chạy lại thành công. Không coi kết quả đó là lỗi AIMentor.

## Giới hạn và bàn giao
API key vẫn trống; chưa gọi Gemini thật. Science mẫu cục bộ chưa đủ 10/15 câu từng nhóm, nên báo thiếu và cho chọn 5 hoặc đề đã lưu. Bank hiện ở SQLite, chưa đồng bộ máy chủ. Chưa xác nhận trên điện thoại thật/tablet. Chống nộp lặp xuyên sự cố chết tiến trình cần idempotency phía backend. Bộ đề Toán cũ owner 0 được bảo toàn, chưa tự gán người dùng.

Chi tiết chức năng và danh sách backend: QUIZ_AND_HISTORY_REPORT.md. Nghiên cứu UX và nguồn tham khảo: UX_RESEARCH.md. Bản APK bàn giao: ../AIMentor_QA/AImentor-debug.apk (đã cập nhật từ bản build đã kiểm tra).

## Git
Remote được fetch lại ngày 17/10/2026, origin/master vẫn a0e190a. Nhánh làm việc codex/quiz-bank-unification; bản sửa 6ba21e7 đã đẩy fast-forward lên origin/master; không force push.

## Rà soát logic tiếp theo — 17/10/2026

Người dùng yêu cầu kiểm tra logic toàn bộ app. Chi tiết: LOGIC_AUDIT_2026-10-17.md.

- Đã sửa các nhánh parser/đáp án AI, false-save success, khôi phục chat/ghi chú/tab, xác nhận rời quiz, mã phân loại Science, giới hạn ảnh Toán và kiểm tra reminder.
- logic-audit-final-build-verification.log: debug, release, APK kiểm thử, 97 unit test và lintDebug toàn project PASS; 0 lỗi lint, còn 476 cảnh báo.
- logic-audit-new-cases-device-verification.log: 10/10 ca mới PASS (24,529 giây), gồm parse/service/bank, repository lỗi, recreate, retry và xác nhận rời quiz.
- logic-audit-all-compact-device-verification.log: PASS 48/48 ca (155,186 giây) ở 320dp, font 1.3, điều hướng ba nút, tối, offline.
- Mốc Git đầu lượt: 7566cfe, remote chưa có thay đổi mới. Lượt audit đã hoàn tất kiểm thử; bản sửa và báo cáo được lưu trong commit tiếp theo của nhánh, cập nhật origin/master bằng fast-forward.
- API key vẫn trống; không gọi Gemini thật, không thêm backend máy chủ.

APK bàn giao ../AIMentor_QA/AImentor-debug.apk đã cập nhật từ bản build cuối; SHA256 7799FD7B0D43E4BA66FDE2C7B4663486191FAC6E0814881BC1C15BDAE0265E70.


## API cũ — kiểm thử thật hoàn tất 17/10/2026

Người dùng cho phép thử lại API cũ. Chi tiết LIVE_API_TEST_2026-10-17.md.
- 7/7 khóa cũ gọi danh sách model HTTP 200.
- live-old-api-runtime-verification.log: 8/8 ca Gemini thật PASS (181,603 giây): chat bốn môn; quiz Toán/Science/Lập trình 5 câu và Lịch sử 15 câu, đều lưu bank.
- Science tạm dùng pool khóa Toán vì ZIP không có khóa riêng.
- Test chỉ chạy khi mentorLiveAi=true; mặc định 8 ca được bỏ qua (đã xác nhận), không gọi mạng.
- Đã phục hồi API công khai trống và BuildConfig bốn module trống; đã cài lại APK trống, xóa APK chứa khóa và đóng giả lập.
- Chưa thử ảnh, các lỗi mạng/quota cưỡng bức hoặc kiểm duyệt toàn bộ kiến thức AI.

## Hoàn tất streak quiz, bỏ thời gian và nghiên cứu mã nguồn mở — 17/10/2026

- Streak chỉ tính quiz đã trả lời đầy đủ, nộp/lưu thành công; chat không được tính. Cả bốn môn và ôn offline dùng chung quy tắc; mỗi ngày một lần, giữ kỷ lục khi đứt chuỗi.
- Trang chủ có chuỗi hiện tại/kỷ lục/lịch 7 ngày và XP quiz tuần tách khỏi level. Chưa có giải đấu nhiều người; đã ghi rõ trạng thái và bàn giao cơ chế XP theo tuần/ghép nhóm/thăng-xuống hạng cho backend. Không có leaderboard theo level cũ trong nguồn để gỡ.
- Đã bỏ hoàn toàn countdown, deadline và tự nộp quiz Toán; form chung chỉ cho nộp khi trả lời đủ. Bundle cũ có timeout vẫn chỉnh sửa được.
- Áp dụng ý tưởng phù hợp từ AnkiDroid/Moodle/LibreLingo: tiến độ cá nhân gọn, giữ ôn offline, thêm xem riêng câu sai sau quiz; lọc/khôi phục không cộng XP lại. Chưa triển khai spaced repetition.
- Sửa thêm lỗi nút ẩn máy tính Toán bị cắt khi font lớn: vùng phím cuộn nhường chiều cao cho các nút xếp dọc.
- `learning-responsive-verification.log`: debug, release, APK test, toàn bộ unit tests và lint PASS. **110 unit tests, 0 thất bại; lint toàn project 0 lỗi, 479 cảnh báo.**
- `learning-target-runtime-verification.log`: **10/10 PASS** ở cấu hình thường.
- `learning-calculator-target-verification.log`: **8/8 PASS** sau sửa máy tính và kiểm tra điểm ghi streak Khoa học ngay khi nộp.
- `learning-full-compact-final-runtime-verification.log`: **58 ca chức năng PASS, 8 live API chủ động bỏ qua** (runner ghi OK 66 tests), 190,949 giây; màn hình 320dp/font 1.3/ba nút/offline, tiếng Anh + chế độ tối ở các ca có cấu hình này.
- Lượt full đầu phát hiện nút máy tính bị cắt; đã sửa, xác minh trực tiếp và chạy lại toàn bộ thành công. Lượt đầu bị kết thúc trước khi hoàn tất, không tính là đạt.
- Đã xem ảnh lịch streak tiếng Anh/tối trên màn hình nhỏ: 7 cột vừa chiều ngang, đọc được, card cuộn nằm trên điều hướng.
- API: 10 trường key trong file cấu hình và BuildConfig các môn đều trống; không gọi Gemini trong lượt này. APK bàn giao được cập nhật ở `../AIMentor_QA/AImentor-debug.apk`.

Quy tắc, nghiên cứu, giới hạn và hợp đồng dữ liệu giải đấu đề xuất: [STREAK_AND_LEAGUES_2026-10-17.md](STREAK_AND_LEAGUES_2026-10-17.md). Giải đấu, đồng bộ streak nhiều máy và chống chỉnh đồng hồ/XP vẫn thuộc backend.


## Kiểm tra bản phát hành tại repo mới — 18/10/2026

- Repo nhận cập nhật: https://github.com/Loc5783/AI-Mentor ; nhánh main. Bản b5c9b75 có cây code tương đương bản làm xong tại máy, không mang lịch sử commit cũ chứa API key sang repo mới.
- Giao diện được gom nhóm: dashboard vẫn giữ biểu đồ/bốn môn/hoạt động; streak và XP tuần nằm trong Hồ sơ > Tiến độ học tập. Chỉ quiz được tính streak.
- Đã sửa thông báo cấp tối đa vẫn hứa lên cấp; mốc EXP mặc định 140 thành 120; nhãn cấp bị dính số do khoảng trắng cuối resource bị Android bỏ; tổng EXP dùng long và chặn tràn số trước khi tính level.
- Thêm 3 unit tests cho tổng EXP và 1 kiểm thử thiết bị cho nhãn cấp tối đa.
- published-audit-final-build-verification.log: debug, release, APK test, unit và lint PASS. 119 unit tests, 0 thất bại; lint 0 lỗi và 495 cảnh báo trên toàn bộ module.
- Lượt thiết bị đầu có 10 lỗi tại bước chụp ảnh (bitmap null), chưa được coi là PASS. Máy giả lập tiếp tục bị ngắt trong lượt toàn bộ. Sau khi khởi động lại, 6/6 ca tập trung PASS (37,011 giây), đã xem ảnh cấp 20 và streak tiếng Anh/tối. Chưa xác nhận trọn bộ 67 ca thiết bị trên bản cuối. API key 10 trường vẫn trống; không gọi AI thật. Báo cáo: ERROR_CHECK_2026-10-18.md.


## Hoàn tất giao diện gọn và biểu đồ có ngữ cảnh — 18/10/2026

- Tham khảo tài liệu chính thức Duolingo/Khan Academy/Brilliant, chọn nguyên tắc phù hợp và áp dụng vào dashboard, chọn môn, banner/thao tác bốn môn và tiêu đề tiến độ học tập; nguồn và quyết định trong UI_COMPACT_CHART_2026-10-18.md.
- Biểu đồ phân biệt đang tải / chưa có EXP / dữ liệu thật / tải thiếu-lỗi; có hướng dẫn và CTA phù hợp, không tạo số liệu giả. Thẻ tiến độ tự xuống hàng, có nhãn cấp/EXP thật.
- Giữ mọi chức năng, ID/listener mở môn, máy tính riêng Toán và mạch điện Khoa học. Không đổi database, API, reward/streak hay logic quiz.
- Debug/release/test APK/unit/lint PASS. 119 unit tests, 0 lỗi lint, 499 cảnh báo. Kiểm thử UI thường 4/4 và nhỏ/chữ lớn/ba nút 4/4; sau chỉnh từ ảnh thực tế, lượt bản cuối 4/4 PASS (37,975 giây).
- 10 trường API key vẫn trống, không gọi AI thật. APK bàn giao đã cập nhật trong ../AIMentor_QA/AImentor-debug.apk. Chưa thử máy thật; không chạy lại toàn bộ 67 ca thiết bị trong đợt giao diện.

## Chốt auth/onboarding — 18/10/2026

- Đăng nhập/đăng ký, hướng dẫn bốn bước, hồ sơ lớp 10–12, theme và VI/EN đã hoàn thành; người dùng xác nhận hài lòng. Recovery có giao diện và contract, chưa có backend email nên không giả gửi thành công.
- Build debug/release/test APK/unit/lint đạt; 125 unit tests; lint 0 lỗi, 526 cảnh báo. Runner thiết bị OK (76 tests): 68 ca chạy đạt, 8 ca API thật bỏ qua có chủ đích. Chi tiết AUTH_ONBOARDING_2026-10-18.md.
- Yêu cầu kế tiếp: 5 tab, 9 môn độc lập, Chat AI chung giải thích theo lớp và giữ cách cấu hình API key cũ. Chưa có backend: chuẩn bị giao diện/hợp đồng API, chưa tuyên bố đồng bộ realtime giữa thiết bị.
- Quy tắc bài học +200 EXP/streak một lần/ngày là yêu cầu mới, chưa bật thay cơ chế quiz hiện hữu trong bản chốt auth. Cần chuyển đổi riêng, tránh thưởng hai lần.

## Nền móng đại tu, phạm vi ba môn — 18/10/2026

- Đăng nhập đã chốt/push riêng f674b6d. Bước mới thêm learning-core, chat-core và khung 9 module môn, chỉ BIOLOGY/CHEMISTRY/PHYSICS thuộc phạm vi Lesson hiện tại; sáu môn khác để khung, không giả có nội dung.
- Chat chung giữ pool API key kiểu cũ, section CHAT riêng; có ngữ cảnh lớp 10/11/12, lịch sử/tệp trong request, lỗi an toàn và fallback cấu hình sai để không chặn khởi động app. Chưa có UI chat/project/upload/camera mới hoặc backend realtime.
- Quiz sau mỗi 5 bài, 10/15 câu đúng nhóm 5 bài trước; lần đầu nhận 200 EXP/bài hoặc 400 EXP/quiz. Làm lại 0 EXP nhưng có thể thắp streak ngày mới. Policy có code/test, chưa nối ledger vào quiz cũ.
- Đặc tả APP_REDESIGN_2026-10-18.md; review FOUNDATION_REVIEW_2026-10-18.md; draft backend docs/backend/openapi.json (34 paths/47 operations/51 schemas). Bàn giao tiếng Việt trong app/BAN_GIAO_BACKEND.txt đã cập nhật.
- Debug/release/test APK/unit/lint đạt: 149 unit tests, 0 lỗi lint, 534 cảnh báo. Full runner 83 (75 đạt +8 API thật bỏ qua) trên bản scope/reward; sau hardening factory cuối, 12/12 ca chat + Canonical smoke đạt. API key 13 trường trống.
- Ca máy tính ngang từng lỗi ở lượt full đầu; thêm assert theo từng phím, tập trung 1/1 và full tiếp theo đạt. Chưa xác định nguyên nhân gốc, giữ theo dõi; không coi một lần chạy lại đạt là sửa logic sản phẩm.
- Bước kế tiếp: Chat UI + local repository/lịch sử/thư mục/tệp, Lesson shell chỉ ba môn, rồi completion ledger và đổi 5 tab. Gom chọn môn về Lesson để tránh trùng bước chọn bốn môn cũ ở onboarding. Không bật các tab trống.
