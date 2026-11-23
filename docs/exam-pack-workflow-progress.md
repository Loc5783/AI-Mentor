# Kho đề AI: luồng nhập và rà đề

Nhánh `feature/exam-pack-workflow` nối bộ xử lý của PR #10 với màn Bài học.
Người học đã đăng nhập có thể chọn PDF tối đa 50 trang hoặc ảnh tối đa 4 MB,
chọn lớp/môn, tách tối đa 40 câu thành đề nháp riêng tư, mở lại đề đã lưu,
sửa từng câu, tự xác nhận đáp án và luyện các câu đã rà đầy đủ.
Đề chưa đủ bốn đáp án, lời giải, chủ đề và nguồn hợp lệ không được luyện.
Khi mất kết nối, màn hình báo lỗi; ứng dụng không tự tạo kết quả AI giả.

## Ranh giới hiện tại

- Tệp nguồn chưa được lưu trong Storage; chỉ lưu tên và số trang. Giao diện hiển thị
  trang nguồn để đối chiếu với bản gốc người học giữ trên máy. Cần thêm kho tệp
  riêng tư trước khi có thể xem lại tệp nguồn trong app trên thiết bị khác.
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
phải được giữ nguyên, không xóa theo mã.
