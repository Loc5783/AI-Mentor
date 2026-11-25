# Supabase trong AI Mentor

Tài liệu này mô tả **mã nguồn trong repo**, không xác nhận trạng thái của project cloud. Android chọn các adapter Supabase qua `AppServices.java` khi `SUPABASE_ENABLED=true`; nhánh FastAPI local chỉ dùng khi tắt cấu hình đó.

## Thành phần đã có trong code

| Phần | Vị trí | Giới hạn của bằng chứng |
| --- | --- | --- |
| Auth, hồ sơ, bạn bè, tiến độ, lịch, chat, thư viện, đề thi | `app/src/main/java/com/example/asm_final/supabase/`, `app/src/main/java/com/example/asm_final/learning/` | Adapter/RPC caller có trong app; chưa chứng minh project cloud đã có đủ bảng, policy hoặc dữ liệu. |
| Xưởng phá mệnh đề | `202611210002_misconception_buster_api.sql`, `app/src/main/java/com/example/asm_final/learning/MisconceptionRepository.kt` | Danh mục chỉ trả metadata hiển thị qua RPC; bảng đáp án không đọc trực tiếp được. Server kiểm tra câu sai đã mở, môn/lớp và chấm kết quả. Bản nháp cục bộ chưa phải kết quả đã xác nhận. |
| Schema, RPC và nội dung seed | `supabase/migrations/` | Migration lớp 10–12 có trong Git. Phải so sánh lịch sử remote trước khi áp dụng; file SQL không chứng minh đã deploy. |
| Chat, lập kế hoạch to-do, đọc ảnh thời khóa biểu, luyện AI | `supabase/functions/mentor-ai/` | Source có các mode `general`, `todo_plan`, `timetable_scan`, `practice_generate`, `practice_explain`. `config.toml` đặt `verify_jwt = true`. Cần Edge Function đã triển khai và Gemini secrets hợp lệ để chạy online. |
| SQL smoke | `supabase/tests/*_smoke.sql` | Test được viết để chạy trong transaction và rollback; chỉ có kết quả khi thực sự chạy trong môi trường phù hợp. |

Các file dữ liệu và migration có thể bổ sung nội dung lớp 11, 12; cần kiểm tra số lượng/độ đúng của từng môn trong cơ sở dữ liệu đang dùng. Không suy ra cloud đã cập nhật chỉ từ branch hoặc commit.

## Cấu hình an toàn

- `supabase.local.properties.example` chỉ chứa Project URL và publishable key công khai. `supabase.properties` và `supabase.local.properties` là cấu hình máy cá nhân bị bỏ qua bởi Git.
- Không đặt Gemini key, database password, access token, `service_role` hoặc secret vào Android, migration, test hay GitHub Actions log.
- Callback Android cho email cần được đối chiếu giữa app và Supabase Auth URL Configuration. Việc có deep link trong app không chứng minh dashboard đã cho phép callback.
- Test CI không nhận secret production, không `link`, `db push` hoặc deploy Edge Function.

## Kiểm tra local và cloud

1. **Local migration:** CI dùng Supabase CLI và Docker để chạy stack local mới; nếu migration lỗi, job SQL thất bại.
2. **Local SQL smoke:** sau migration, CI chạy `supabase/tests/*_smoke.sql` qua `psql -X --set ON_ERROR_STOP=1`. Test tự rollback dữ liệu tạo trong transaction.
3. **Local Edge test:** `node --test supabase/functions/mentor-ai/*.test.mjs backend/tests/test_gemini_key_pool.mjs backend/tests/test_mentor_ai_handler.mjs` kiểm tra logic bằng mock; không gọi Gemini thật.
4. **Cloud verification:** kiểm tra `migration list --linked`, phiên bản function, Auth callback, RLS và test tài khoản riêng trên project đích. Cần ghi ngày, project ref, kết quả thực tế và người thực hiện; tài liệu này không cung cấp kết quả cloud hiện tại.
5. **Deploy:** chỉ thực hiện sau khi xem diff và lịch sử migration, có kế hoạch rollback và được người phụ trách cho phép. Không dùng `db reset --remote` với dữ liệu người dùng.

## Trạng thái cần báo cáo trong PR

| Trạng thái | Cách ghi |
| --- | --- |
| Code có trong repo | Dẫn file hoặc commit. |
| Test local đã chạy | Ghi lệnh, kết quả và môi trường. |
| Test cloud đã chạy | Ghi project đích, luồng thử và kết quả; không ghi secret. |
| Đã deploy | Ghi migration/function version hoặc link run triển khai; nếu chưa có bằng chứng thì ghi “chưa xác minh”. |

Chi tiết backend local: [backend/README.md](../backend/README.md). Hướng dẫn chung và CI: [README.md](../README.md).
