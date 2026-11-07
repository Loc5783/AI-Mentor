# Kiểm tra nhập và demo lớp 12

Kiểm tra trực tiếp Supabase nhóm hnxphwuaccztzanjcuca, chỉ truy vấn đọc.

## Đã xác nhận
- mentor_questions: 1.798 câu lớp 12, đều PUBLISHED; physics 198, tám môn còn lại mỗi môn 200.
- Không có AIM_PHY12_BANK01_Q149 hoặc AIM_PHY12_BANK01_Q199.
- Lớp 10 hiện có 200 DRAFT và 35 PUBLISHED. Đây là số lượng hiện tại, không phải đối chiếu snapshot chứng minh mọi trường trước/sau không đổi.
- File danh sách ID lô nhập tồn tại trong scratch của Antigravity.

## Chưa chứng minh luồng app sử dụng lô mới
mentor_library_quizzes có 30 bộ đề lớp 12, chứa 300 lượt câu hỏi JSON. Không câu nào có ID/code nối tới ID/code của lô mentor_questions lớp 12. Chỉ hai prompt trùng nguyên văn; điều này không chứng minh có liên kết dữ liệu.
Script test_student_practice_flow.py đọc /library/quizzes, chọn quizzes[0] và làm một bộ đề. Ví dụ bộ review-biology-12-1 có câu biology-12-dna-replication-q1, chỉ ba lựa chọn, khác định dạng bốn lựa chọn của lô mới. Vì vậy kết quả làm bài hiện tại chưa xác minh khả năng sử dụng 1.798 câu mới trong app.

## Giới hạn bằng chứng kiểm thử
- test_web_admin_g12.py là kiểm tra HTTP API, không phải kiểm thử màn hình trình duyệt.
- Script học sinh không tính độc lập expectedScore để đối chiếu score/correctCount; chỉ kiểm tra trạng thái COMPLETED và in điểm.
- Script kiểm tra gửi lại cùng đáp án; không có ca gửi đáp án xung đột trong script đã đọc.
- Không có ca ngắt mạng/chặn máy chủ trong script đã đọc. Unit test/đọc controller không thay thế thử lỗi mạng trên thiết bị.
- Chưa thực hiện lại kiểm thử trên điện thoại trong lượt phúc tra này. Báo cáo agent cũng ghi chưa có thiết bị.

Kết luận: ĐẠT xác nhận số lượng/trạng thái nhập database; CHƯA ĐẠT xác nhận demo app với lô mới. Cần kiểm tra đường app lấy câu hỏi và tạo/liên kết bộ luyện tập dùng đúng mã câu thuộc lô mới, sau đó kiểm thử một phiên thực tế và đối chiếu điểm độc lập. Không sửa dữ liệu hoặc tạo phiên trong lượt kiểm tra này.
