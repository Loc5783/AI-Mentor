# Ngân hàng câu hỏi Lớp 11 (Chương trình GDPT 2018 - SGK Kết nối tri thức)

Tập hợp bộ câu hỏi ngân hàng Lớp 11 chuẩn hóa cho cả 9 môn THPT:
Toán, Vật lý, Hóa học, Sinh học, Ngữ văn, Tiếng Anh, Lịch sử, Địa lý, GD Kinh tế & Pháp luật.

- Bản nguồn hiện có 180 câu, nhưng chỉ 45 câu nền tảng (5 câu/môn) đang được xuất bản; phần câu theo bài còn thiếu phương án D nên vẫn là bản nháp.
- Mục tiêu hiện tại theo yêu cầu là ít nhất **50 câu độc lập và 10 đề cho mỗi môn**. Chưa môn nào đạt mốc 50 câu đã xuất bản.
- Migration `202611140003_grade11_question_bank_and_drafts.sql` đưa các câu hỏi vào `public.mentor_questions` trên Supabase với ID ổn định và `ON CONFLICT DO NOTHING`.

## Bổ sung theo mốc 50 câu

`backend/tools/build_grade11_math_bank.mjs` tạo 45 câu Toán 11 mới theo quy tắc
xác định, không gọi Gemini. Migration `202611160016_grade11_math_bank_to_50.sql`
thêm câu vào kho với 4 lựa chọn, đáp án và giải thích; kiểm tra đi kèm nằm tại
`supabase/tests/grade11_math_bank_smoke.sql`. Đây mới là mã nguồn, chưa áp dụng
lên Supabase production.

`202611160018_grade11_lesson_bank_extension.sql` hoàn thiện 15 câu bài học
đang có cho từng môn còn thiếu phương án D. `backend/tools/build_grade11_science_bank.mjs`
tạo thêm 30 câu Vật lý và 30 câu Hóa học, ở migration
`202611160019_grade11_physics_chemistry_to_50.sql`. Sau khi chạy lần lượt các
migration, Toán, Vật lý và Hóa học đạt ít nhất 50 câu; sáu môn còn lại mới có
20 câu và vẫn cần bổ sung 30 câu mỗi môn. Toàn bộ nội dung được tạo offline,
không gọi Gemini.
