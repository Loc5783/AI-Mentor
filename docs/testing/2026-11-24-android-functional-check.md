# Kiểm thử chức năng Android — 24/11/2026

## Phạm vi và môi trường

- Nền mã: `integration` tại `33b36fc`; thay đổi kiểm thử trên `fix/placement-device-test-thread`.
- Thiết bị: máy ảo `Medium_Phone`, Android 17 (API 37).
- Bài kiểm thử thiết bị dùng `-PdisableSupabaseForTests=true` để không ghi vào production. Các bài kiểm thử dùng fake repository/transport không chứng minh kết nối cloud thật.
- Không xóa hoặc dùng dữ liệu người dùng production làm fixture.

## Kết quả đã chạy

| Kiểm tra | Kết quả | Bằng chứng |
| --- | --- | --- |
| Android `assembleDebug`, `testDebugUnitTest`, `lintDebug` với cấu hình chuẩn | Đạt; 309/309 unit test; lint không có error, còn 243 warning và 2 hint | `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/lint-results-debug.html` |
| Edge Function và handler AI, `node --test supabase/functions/mentor-ai/*.test.mjs backend/tests/test_gemini_key_pool.mjs backend/tests/test_mentor_ai_handler.mjs` | 27/27 đạt | Kết quả lệnh local ngày 24/11/2026 |
| Android instrumented test trên máy ảo, 16 lớp test chọn lọc | 54 đạt, 0 lỗi, 1 bỏ qua | `app/build/reports/androidTests/connected/debug/index.html`, `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone(AVD) - 17.xml` |
| Khởi động app khi đã tắt Wi-Fi và dữ liệu di động trên máy ảo | Bản debug cô lập Supabase có process sau 8 giây, Logcat `AndroidRuntime:E` không có crash | Lệnh `adb install`, `svc wifi/data disable`, `am start`, `pidof`, `logcat`; mạng được bật lại sau kiểm tra |

Bài instrumented test gồm điều hướng 5 tab, chế độ tối và ngôn ngữ, giữ bản nháp chat, đăng nhập/đăng ký giả lập, khôi phục phiên, bài học và lộ trình, kiểm tra đầu vào, công cụ môn học, máy tính, nhập lịch, công thức chat, xưởng phá mệnh đề và cô lập tài khoản. Bài `testLiveBackendIntegration_RealNetworkRoundtrip` được bỏ qua do thiếu backend local. Bảy bài `ConnectedLearningTest` cũng được bỏ qua trong lượt chạy riêng vì cần backend local; chúng không nằm trong con số 55 của lượt chạy chung.

## Sửa trong nhánh này

- Bài kiểm tra đầu vào trước đây khởi tạo ViewModel trên luồng sai, fake API không trả dữ liệu lộ trình và tạo `View` nhưng không gắn vào màn hình. Đã sửa để kiểm thử thao tác chọn/sửa đáp án, chuyển câu, quay lại và cảnh báo câu bỏ trống trên UI thật của máy ảo.
- Bổ sung trường hợp mất mạng vào bài kiểm thử phiên Supabase: phiên đã xác minh vẫn khôi phục dữ liệu local của đúng tài khoản; kiểm tra khác vẫn từ chối phiên có ID sai.

## Chưa xác minh và bước tiếp theo

- Chưa chạy pytest FastAPI trên máy này vì `python`/`py` hiện chỉ trỏ tới WindowsApps và không khởi chạy được. CI trên PR cần chạy pytest bằng dependency khóa.
- Chưa chạy SQL smoke test local vì chưa có môi trường Supabase/PostgreSQL cô lập. Không chạy test ghi dữ liệu lên production.
- Chưa xác minh end-to-end với tài khoản Supabase thật, email xác thực, AI provider, nhập ảnh thời khóa biểu thật, đề thi cloud và đồng bộ đa thiết bị. Bài test fake và lần mở app offline không đủ để kết luận các luồng này hoàn tất.
- Chưa thử máy thật. Cần kiểm tra riêng app đang đăng nhập rồi mất mạng, mở lại bài học đã tải và đồng bộ khi có mạng trở lại.
- Lint còn 243 warning, chủ yếu tài nguyên chưa dùng (99) và gợi ý số nhiều (63). Chưa sửa hàng loạt vì cần đối chiếu ảnh hưởng UI/ngôn ngữ.
