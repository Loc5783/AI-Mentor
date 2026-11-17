# Ngân hàng câu hỏi lớp 10

Bản nguồn này lấy **câu hỏi đã có trong bài học lớp 10** của cả 9 môn. Mỗi câu
hiện có 3 lựa chọn và lời giải. File CSV giữ trống phương án D để người biên tập
thêm một phương án sai hợp lý, kiểm tra đáp án và nguồn trước khi xuất bản.

Migration `202611100001_grade10_lesson_question_drafts.sql` sao chép các câu vào
`mentor_questions` với trạng thái **DRAFT**. Giá trị phương án D trong database
chỉ là nhãn nhắc biên tập; tuyệt đối không xuất bản nguyên trạng. Migration không
đụng đến câu Lý lớp 10 đã có, các đề đang xuất bản, phiên làm bài hay tiến độ.

Mục tiêu hiện tại theo yêu cầu là ít nhất 50 câu hỏi độc lập và 10 đề cho mỗi môn. Dữ liệu
bài học hiện mới phủ 15–20 bài nhập môn/môn, chưa phải toàn bộ năm học. Không
được gọi các đề luyện tổng hợp từ số bài này là đề bao quát chương trình lớp 10.
Bộ sách đối chiếu để mở rộng tiếp là **Kết nối tri thức với cuộc sống** và
**Tiếng Anh Global Success**. Câu đã xuất bản cần đúng chủ đề lớp 10, không
chỉ là kiến thức phổ thông có thể gặp ở nhiều lớp.

Đối chiếu trực tiếp project Supabase `hnxphwuaccztzanjcuca` ngày 10/11/2026:
450 câu trên đã được thêm vào `mentor_questions` dưới dạng DRAFT (45–60 câu/môn).
Trước đó ngân hàng có 200 câu Lý DRAFT và 35 câu Lý PUBLISHED. 31 bộ đề ôn tập
lớp 10 có sẵn vẫn PUBLISHED; lần bổ sung này không tạo đề mới hoặc thay đổi đề cũ.
`mentor_questions` vẫn bật Row Level Security. Migration lưu trong Git có thể
chạy lại an toàn nhờ ID ổn định và `ON CONFLICT DO NOTHING`.

## Cập nhật 14/11/2026

- Kho câu hỏi có 75 câu `PUBLISHED`: 35 câu Vật lý và 5 câu cho mỗi môn còn lại.
  Có 650 câu `DRAFT`; 450 câu chép từ bài học còn thiếu phương án D. Không xuất
  bản các câu này trước khi biên tập đáp án, lời giải và đối chiếu nguồn.
- 40 câu mới được phân loại lại theo tên bài lớp 10 trong
  `202611140005_grade10_foundation_curriculum_alignment.sql`. Bảy câu kiến thức
  chung trước đó đã được sửa để khớp đúng bài học lớp 10 hiện có.
  `20261114132201_grade10_math_two_variable_scope.sql` sửa thêm câu Toán về hệ
  bất phương trình hai ẩn để bám nội dung Kết nối tri thức lớp 10.
- Kho đề có 90 đề `PUBLISHED`, đúng 10 đề/môn. 59 đề tổng hợp mới chỉ dùng câu
  từ các bài lớp 10 hiện có; mỗi đề có 15 câu khác nhau, nhưng một câu có thể
  xuất hiện trong nhiều đề. Không tính 885 lượt câu trong 59 đề thành 885 câu
  hỏi độc lập của kho câu hỏi.
- Trên Supabase production, mốc 50 câu/môn **chưa đạt** với hầu hết môn. Các
  migration ở phần dưới đang chờ được áp dụng; không tính số câu trong đề ôn
  tập thành câu hỏi độc lập.

## Cập nhật bộ Toán 10

Migration `20261114134001_grade10_math_question_bank.sql` thêm 195 câu Toán
được tạo từ 15 dạng bài có quy tắc tính và lời giải xác định. Bộ kiểm tra tự
động đối chiếu đáp án độc lập cho từng câu, bảo đảm bốn phương án khác nhau,
ID và đề bài không trùng nhau. Đã áp dụng trên project Supabase
`hnxphwuaccztzanjcuca`: Toán hiện có **200 câu PUBLISHED** (5 cũ + 195 mới).
Mười bốn chủ đề của bộ này thuộc các bài Toán 10 đang có trong app; nhiều câu
chỉ khác dữ kiện số, vì vậy chưa thể coi là bao phủ toàn bộ SGK hoặc có độ đa
dạng tương đương 200 dạng bài.

## Bổ sung theo mốc 50 câu

Migration `202611160015_grade10_physics_bank_to_50.sql` bổ sung 15 câu Vật lí
10 tự biên soạn, có bốn lựa chọn và lời giải. Migration
`202611160017_grade10_banks_to_50.sql` hoàn thiện thêm 45 câu cho mỗi môn Sinh,
Hóa, Kinh tế & Pháp luật, Anh, Địa lý, Lịch sử và Ngữ văn từ câu hỏi bài học
đã có; lựa chọn D lấy từ phương án sai của bài khác cùng môn. Toán đã có 200 câu.
Hai kiểm tra `supabase/tests/grade10_banks_smoke.sql` và
`supabase/tests/grade10_physics_bank_smoke.sql` xác nhận từng môn đạt ít nhất
50 câu, đủ lựa chọn, đáp án và lời giải. Đây là migration trong mã nguồn, chưa
áp dụng lên Supabase production.
