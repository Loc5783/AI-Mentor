# AIMentor — hiệu ứng nhẹ và API cũ, 20/10/2026

## Yêu cầu và thay đổi
- SOLID là tiêu chí chung. Ưu tiên chuyển động nhẹ hơn Duolingo, chữ rõ ràng, hạn chế icon trang trí, giữ affordance chức năng cần thiết. Đã lưu AGENTS.md.
- MotionSettings/SoftMotion gom chính sách bật/tắt và lifecycle, kiểm tra animator/transition scale cả minSDK24. Panel đổi bước 160 ms/4 dp; completion scale 0.985→1 trong 240 ms, một lần; press 0.99/80 ms, nhả 120 ms. Không loop/spring/confetti hay icon trang trí mới; không gắn việc cấp thưởng hoặc click vào animation.
- LessonPathData/View tách snapshot trình bày và rendering khỏi Activity. Lộ trình dùng số/đường nối và tiến độ bài; quiz không tính thành bài thứ sáu. State/XP/quyền mở bài vẫn do server quyết định. Khóa không gọi callback kể cả performClick trực tiếp.
- LessonCompletionView chỉ hiển thị kết quả server. Key chuyển bước/feedback lưu qua recreation; SavedStateHandle completion ngăn phát lại animation. Detach đưa view về alpha/scale/translation cuối.
- ScreenViews button xuống tối đa ba dòng, không ellipsize; dùng press nhẹ chung. LessonHub gom công cụ bổ trợ vào phần mở/thu gọn giữ trạng thái qua rotation, bỏ mô tả lặp; cảnh báo EXP legacy riêng vẫn có. Mạch điện/bảng tuần hoàn/quiz bổ trợ giữ đường truy cập.

## Kiểm chứng
- soft-learning-final-build-verification.log: debug/release/test APK, lint và JVM PASS; 173 JVM tests, 0 failures/errors. Các build sau chỉ đổi/bổ sung test.
- LearningPresentationTest: 2/2 khi bật, 2/2 khi tắt motion. Kiểm tra 320dp/font1.3, VI/EN, light/dark, bounds ngang, review tách tổng bài, khóa không gọi callback, click không chờ motion; reduced motion có trạng thái cuối ngay.
- soft-learning-connected-retest-verification.log: 2/2 PASS, 16,121 giây. Placement→lesson→sai hai lần/hướng dẫn→Chat/Đã hiểu→200 EXP/streak; recreate completion không phát lại; path 1/5. Công cụ mở/thu gọn giữ qua rotation, CircuitDiagramActivity mở được, periodic button có thể truy cập sau mở rộng. Không có nghĩa đã kiểm tra/sửa mọi logic mạch điện.
- fiveDestinationsExistAndEachOpensItsOwnScreen PASS trong lượt đầu soft-learning-connected-verification.log. Cùng lượt có lỗi test mới truy cập lifecycle ngoài main thread, đã sửa và chạy lại; không gọi lượt đầu toàn PASS.
- soft-learning-frames-optout-verification.log: 1 connected PASS, 3 live-AI SKIP đúng opt-in. Runner OK (4 tests) không đồng nghĩa 4 ca AI đã chạy. Ảnh cuối đợi khung hình mới: ../AIMentor_QA/soft-learning/path.png và completion.png.
- Build đầu thiếu ba ID cũ vì file resource bị ghi đè khi thêm ID; đã khôi phục mentor_motion_entered/card/list. Các build sau PASS, log đầu vẫn giữ.

## API thật bằng key ZIP cũ
- Người dùng cho phép dùng key trong C:/Users/ACER/Downloads/AI_Mentor_English_Comments_No_Defense_Note.zip. Đọc được 7 key; lượt đầu chỉ tìm mẫu AIza nên bỏ sót mẫu AQ., đã sửa cách lấy. Không hiển thị giá trị.
- LiveTutorSmokeTest đọc file QA riêng lúc chạy; không điền key vào api.properties, BuildConfig hoặc APK. Mặc định SKIP; opt-in thiếu file phải fail rõ ràng.
- soft-learning-live-ai-verification.log: 3/3 PASS, 39,005 giây qua GeminiTutorGateway thật: Lý lớp10, Hóa lớp11, Sinh lớp12. Đã đọc mẫu trả lời: s=5×4=20 m; Z=8 trung hòa có 8 proton/8 electron; vai trò màng và tính thấm chọn lọc.
- Đây là smoke test adapter chat, chưa phải test UI chat/ảnh/quiz thật mới, chưa duyệt chuyên môn toàn nội dung. Phản hồi không có key: ../AIMentor_QA/soft-learning/live-tutor/.
- Tệp key tạm trên host và emulator đã xóa sau test. ZIP gốc chỉ đọc. Cấu hình public vẫn trống 13 key và backend URL.

## Nội dung đã chốt và phần còn lại
- Người dùng muốn nội dung lớp10–11–12 đang dùng hiện nay. Đã đối chiếu nguồn Chính phủ, chốt Kết nối tri thức với cuộc sống cho năm học2026–2027 làm mốc. Kế hoạch tại docs/content/CONTENT_PLAN_VI.md.
- Runtime vẫn 15 bài/45 câu nhập môn lớp10. Chưa thêm gói11/12, chưa coi đủ chương trình hoặc đủ mọi màn theo thiết kế. Nội dung phải bổ sung và duyệt riêng.
- Chưa hoàn tất profile editor cloud/đổi mật khẩu, bạn bè, chat cloud/bank/outbox, cập nhật tự động qua nửa đêm và deployment; giới hạn chi tiết giữ trong checklist20/10 và checkpoint19/10.
- Chưa commit/push. APK ../AIMentor_QA/AImentor-debug.apk đã cập nhật. Repo đích Loc5783/AI-Mentor; workflow main/integration gửi nhầm không áp dụng.
- Đã khôi phục cấu hình motion/mạng của emulator QA, đóng emulator5562 và backend18082, không tác động máy giả lập5554 của người dùng. Kiểm tra cuối: 14 trường key/URL public và 39 trường credential BuildConfig sinh ra đều trống, host key tạm không còn.
