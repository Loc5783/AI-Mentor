# Ngân hàng câu hỏi lớp 10

Bản nguồn này lấy **câu hỏi đã có trong bài học lớp 10** của cả 9 môn. Mỗi câu
hiện có 3 lựa chọn và lời giải. File CSV giữ trống phương án D để người biên tập
thêm một phương án sai hợp lý, kiểm tra đáp án và nguồn trước khi xuất bản.

Migration `202611100001_grade10_lesson_question_drafts.sql` sao chép các câu vào
`mentor_questions` với trạng thái **DRAFT**. Giá trị phương án D trong database
chỉ là nhãn nhắc biên tập; tuyệt đối không xuất bản nguyên trạng. Migration không
đụng đến câu Lý lớp 10 đã có, các đề đang xuất bản, phiên làm bài hay tiến độ.

Ngân hàng lớp 12 có 200 câu/môn và bộ đề demo liên kết; nội dung lớp 10 hiện mới
phủ các bài nhập môn 15–20 bài/môn. Vì vậy bản này chưa được gọi là đủ 200
câu/môn hoặc phủ hết chương trình năm học.

Đối chiếu trực tiếp project Supabase `hnxphwuaccztzanjcuca` ngày 10/11/2026:
450 câu trên đã được thêm vào `mentor_questions` dưới dạng DRAFT (45–60 câu/môn).
Trước đó ngân hàng có 200 câu Lý DRAFT và 35 câu Lý PUBLISHED. 31 bộ đề ôn tập
lớp 10 có sẵn vẫn PUBLISHED; lần bổ sung này không tạo đề mới hoặc thay đổi đề cũ.
`mentor_questions` vẫn bật Row Level Security. Migration lưu trong Git có thể
chạy lại an toàn nhờ ID ổn định và `ON CONFLICT DO NOTHING`.
