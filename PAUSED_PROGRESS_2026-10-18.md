# Tạm dừng theo yêu cầu người dùng — 18/10/2026

Người dùng yêu cầu: "tạm dừng và lưu tiến độ". Không tiếp tục triển khai, sửa lỗi hay push cho tới khi người dùng yêu cầu tiếp tục.

## Phạm vi mới nhất
- Làm cả frontend/backend, năm tab Dashboard / Chat AI / Lesson / Thi đua / Profile.
- Hoàn thiện Sinh, Hóa, Lý; sáu môn khác chỉ để khung.
- Giữ auth đã duyệt; giữ công cụ cũ hữu ích và chuyển dữ liệu trước khi gỡ luồng cũ.
- Bài 200 EXP, review mỗi năm bài 10/15 câu và 400 EXP; chỉ thưởng lần đầu. Làm lại 0 EXP nhưng được thắp streak ngày mới. Replay completion cũ không thắp streak.
- Repo đích: Loc5783/AI-Mentor. Không force push, không đưa API key thật lên Git.

## Đã lưu trong mã nguồn
- Năm tab; chat chung SQLite theo account, tệp/ảnh/camera, thư mục/lịch sử, đổi tên/di chuyển/xóa, bản nháp và cơ chế gọi Gemini cũ.
- Profile có settings góc trái; Lesson chọn ba môn/lớp; Thi đua có bộ lọc nhưng Android chưa nối ranking realtime.
- Backend FastAPI/SQLite: auth, hồ sơ riêng/công khai, chat/thư mục/tệp, bạn bè, ranking, sự kiện SSE.
- LearningService và API: enrollment, placement, path, attempt, lưu câu trả lời có revision/submission ID, hỗ trợ sau hai lần sai, completion/reward/streak trong giao dịch; checkpoint review bắt buộc trước bài thứ sáu; question bank và handoff/resume có kiểm tra owner.
- Ba gói nhập môn lớp 10: 15 bài, 45 câu gốc cho Sinh/Hóa/Lý. Không phải toàn bộ chương trình, chưa có gói lớp 11/12 và chưa được giáo viên duyệt. Công cụ tạo: backend/tools/build_intro_curriculum.py.
- Android mới: BackendClient, BackendSession (Keystore AES-GCM), LessonViewModel, LessonActivity; màn lộ trình/đọc kiến thức/ví dụ/làm bài/kết quả; chuyển câu qua Chat AI và nút Đã hiểu quay về bài.
- MENTOR_BACKEND_URL trong api.properties đang trống. Key Gemini vẫn trống. Không có server Internet được triển khai.

## Kết quả kiểm chứng, không đánh đồng với bản mới nhất
- Backend trước khi thêm content/native integration: 17/17 pytest PASS (backend-learning-tests-verification.log). Sau đó đã thêm gói thật và sửa health test; CHƯA chạy lại.
- Android trước LessonActivity/BackendClient mới: build debug/release/test và lint PASS (overhaul-learning-android-verification.log). Lint WrongConstant đã sửa.
- GeneralChatTest: phiên bản tám ca trước đó PASS. Ca thứ chín giữ draft qua recreation chưa được xác nhận riêng.
- Full instrumented suite mới trên emulator-5562 có ít nhất hai ca ResearchUxTest thất bại vì vẫn chờ màn danh mục legacy sau khi chuyển sang Lesson. Hai assertion đã sửa theo đích mới; CHƯA chạy lại. Không báo toàn suite PASS.
- BUILD MỚI NHẤT THẤT BẠI: lesson-connected-build-verification.log. app/build.gradle.kts dùng java.util.Properties bị Kotlin DSL hiểu nhầm java extension, kéo theo unresolved load/getProperty. Cần import java.util.Properties ở đầu rồi dùng Properties(), sau khi người dùng tiếp tục. Chưa kiểm tra compile/lint Java mới.

## Điểm cần xử lý khi tiếp tục
1. Sửa Gradle DSL nêu trên, format Java mới, build debug/release/test/lint; sửa lỗi mới nếu có.
2. Rà soát LessonViewModel: SavedStateHandle tránh cập nhật từ worker (fetchPath đã bỏ saved.set enrollmentId; stable enroll key tạo trước task); retry mutation và process recreation, stale revision, token refresh khi account đổi.
3. BackendClient cần kiểm tra signedIn khi Keystore mất/endpoint thay đổi, guard sau request retry refresh; session lỗi không được khiến màn đăng nhập mắc kẹt.
4. Thử luồng native trên backend thật qua adb reverse ở máy QA riêng. Không nhầm hợp đồng API/test fixture với luồng Android đã chạy.
5. Chạy lại pytest sau content; health hiện learningEngineReady=true và publishedCourses ba môn lớp 10; cập nhật mô tả/README backend còn cũ.
6. Full device regression; kiểm tra chat-return qua rotation/process recreation, bàn phím/chữ lớn/theme/VI-EN và camera.
7. Chưa nối Android SSE, ranking/public profile, dashboard server, offline outbox, account migration và chat legacy migration. Chưa hoàn tất toàn bộ đại tu.
8. Content mới chỉ ba block nhập môn lớp 10. Không quảng cáo đủ chương trình/lớp 11–12; cần mở rộng có kiểm chứng.
9. Backend upload còn cần xử lý giới hạn request chunked, ghi metadata/tệp nguyên tử và dọn tệp sau xóa.
10. REQUIREMENTS_AUDIT_VI.md, backend/README.md và các tài liệu bàn giao cũ cần cập nhật theo code mới sau kiểm chứng; bảng audit hiện phản ánh trạng thái trước bước learning này.

## Trạng thái Git và tiến trình
- Nhánh codex/publish-completed; base được biết trước đó a7772e1 theo origin/main.
- Mã hiện tại là WIP; không push bản đang lỗi build lên GitHub khi tạm dừng.
- QA emulator riêng: emulator-5562, ADB port5041; không chạm emulator-5554 của người dùng.
- Backend QA trước đó chạy 127.0.0.1:18080, dữ liệu ngoài Git tại ../AIMentor_QA/backend_runtime. Phải khởi động lại để nhận mã mới; không giả định tiến trình cũ đã dùng LearningService.
- Các log verification và dữ liệu runtime/venv bị Git ignore; mã nguồn và checkpoint này nằm trên đĩa.
