# Kho đề AI: luồng nhập và rà đề

Nhánh `feature/exam-pack-workflow` nối bộ xử lý của PR #10 với màn Bài học.
Người học đã đăng nhập có thể chọn PDF tối đa 50 trang hoặc ảnh tối đa 4 MB,
chọn lớp/môn, tách tối đa 40 câu thành đề nháp riêng tư, mở lại đề đã lưu,
xem tệp nguồn riêng tư, sửa từng câu, tự xác nhận đáp án và luyện các câu đã rà đầy đủ.
Đề chưa đủ bốn đáp án, lời giải, chủ đề và nguồn hợp lệ không được luyện.
Khi mất kết nối, màn hình báo lỗi; ứng dụng không tự tạo kết quả AI giả.

## Ranh giới hiện tại

- Tệp nguồn lưu trong bucket Storage riêng tư `mentor-exam-sources`, truy cập theo
  `auth.uid()` và gói đề. Tệp chỉ được tải khi người học mở để đối chiếu; không
  đính kèm bản gốc vào ngân hàng đề chung.
- Bài luyện từ kho đề hiện chỉ tự đối chiếu đáp án trong phiên, chưa ghi kết quả
  vào tiến độ học, EXP hay sổ lỗi sai.
- Chưa có bài kiểm thử trên thiết bị với tài khoản Supabase thật. Migration schema
  của PR #14 và Edge Function của PR #10 chưa được chứng minh đã deploy cloud.
- Việc bổ sung đủ số câu lớp 11 cần kiểm kê nội dung và kiểm duyệt chuyên môn riêng;
  nhánh này không tự xuất bản câu AI vào ngân hàng chung.

## Dữ liệu

Mọi bản nháp do người dùng tạo thuộc `auth.uid()` và đi qua RLS của
`mentor_exam_packs`/`mentor_exam_pack_questions`. Không sửa hoặc xóa dữ liệu cũ.
Nếu tải lên đứt giữa chừng, đề còn ở trạng thái nháp để người dùng nhận biết.
Rollback giao diện bằng revert commit; dữ liệu người dùng tạo sau triển khai
phải được giữ nguyên, không xóa bucket khi rollback mã.
