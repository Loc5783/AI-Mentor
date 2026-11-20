# AI Mentor

Ứng dụng học tập Android cho học sinh lớp 10–12. Năm mục trên thanh điều hướng là **Bài học, Lịch học, Trang chủ, Thi đua, Hồ sơ**; Chat AI là màn hình riêng, không phải một mục trên thanh này (`ShellUi.kt`).

## Mã nguồn trong repo

| Thành phần | Có trong code | Ý nghĩa |
| --- | --- | --- |
| Android | `app/`, `subjects/`, `core/`, `schedule/` | Giao diện Compose/Fragment, chín môn học và các luồng học tập. |
| FastAPI | `backend/aimentor/`, `backend/curriculum/` | Backend chạy độc lập với SQLite; có bộ nội dung lớp 10, 11 và 12 trong source. Sự có mặt của file không xác nhận độ phủ hay chất lượng của từng môn. |
| Supabase | `supabase/migrations/`, `supabase/functions/mentor-ai/` | Schema/RPC, dữ liệu seed và mã Edge Function. File migration không chứng minh migration đã chạy trên project cloud. |
| Kiểm tra | `app/src/test/`, `backend/tests/`, `supabase/functions/mentor-ai/*.test.mjs`, `supabase/tests/` | Unit test, kiểm tra backend/Edge và SQL smoke. Chúng không thay cho kiểm thử trên thiết bị hoặc cloud. |

App chọn adapter Supabase khi `SUPABASE_ENABLED=true`; khi tắt cấu hình đó, các luồng tương ứng dùng backend FastAPI local (`AppServices.java`). Không có cơ chế tự chuyển mọi chức năng online sang local lúc mất mạng.

## Chạy Android

1. Mở thư mục gốc trong Android Studio với **JDK 21** và **Android SDK 36**. Gradle Wrapper của repo là **9.6.0**.
2. Cấu hình công khai mẫu nằm trong `supabase.local.properties.example`. File `supabase.properties` hoặc `supabase.local.properties` bị bỏ qua bởi Git có thể ghi đè cấu hình máy cá nhân. Không đưa secret hoặc `service_role` key vào ứng dụng.
3. Sync Gradle rồi chạy module `app`. Bản debug mặc định sử dụng Supabase theo cấu hình mẫu; hoạt động online còn phụ thuộc vào schema, Auth, Edge Function và secret thực tế của project cloud.
4. Để dùng FastAPI local, đặt `SUPABASE_ENABLED=false` trong `supabase.local.properties`, cấu hình backend debug rồi khởi động `backend/`. Đây là lựa chọn phát triển/test, không phải xác nhận backend đã deploy.

Kiểm tra Android trên máy phát triển: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. Các bài `androidTest` cần máy ảo hoặc thiết bị và không nằm trong CI mặc định.

## Kiểm tra backend, Edge và SQL

- FastAPI: dùng Python 3.12, cài `backend/requirements-lock.txt`, chạy `python -m pytest tests -q` trong `backend/`.
- Edge Function: dùng Node 24, chạy `node --test supabase/functions/mentor-ai/*.test.mjs backend/tests/test_gemini_key_pool.mjs backend/tests/test_mentor_ai_handler.mjs` từ thư mục gốc.
- SQL: GitHub Actions khởi động Supabase **local, dùng một lần**, chạy mọi migration và `supabase/tests/*_smoke.sql` bằng `psql` với `ON_ERROR_STOP=1`. Các smoke script mở transaction rồi rollback. Không dùng production làm môi trường test.

CI trong `.github/workflows/ci.yml` chạy các kiểm tra trên PR vào `integration`/`main` và khi có commit mới trên hai nhánh đó. Kết quả CI chỉ xác nhận môi trường của lần chạy ấy; trạng thái cloud, email, Gemini và thiết bị thật cần kiểm thử riêng.

## Tình trạng cần xác minh riêng

| Mức chứng cứ | Trạng thái của tài liệu này |
| --- | --- |
| Đã triển khai trong code | Các thành phần ở bảng trên được xác nhận từ source của nhánh này. |
| Đã kiểm thử local | Xem kết quả CI hoặc log kiểm thử của commit cụ thể; không suy ra từ sự có mặt của test. |
| Đã kiểm thử cloud | Chưa xác nhận từ source; cần kiểm tra project Supabase và luồng đăng nhập/học/AI bằng tài khoản test. |
| Đã deploy | Chưa xác nhận từ source; đối chiếu lịch sử migration và phiên bản Edge Function trên dashboard trước khi công bố. |

Tài liệu thêm: [API FastAPI](backend/README.md), [Supabase](supabase/README.md), [kế hoạch nội dung](docs/content/CONTENT_PLAN_VI.md), [hệ màu](COLOR_SYSTEM_2026-10-22.md).

Phát triển trên `feature/*` hoặc `fix/*`, mở PR vào `integration`; trưởng nhóm quyết định merge vào `main`. Mỗi PR ghi cách kiểm chứng và phần chưa xác minh theo [mẫu PR](.github/pull_request_template.md).
