# Kiểm tra độc lập ngân hàng lớp 12

Ngày kiểm tra: 2026-11-06. Kết luận: **CHƯA ĐẠT, chưa nên nhập hoặc xuất bản.**

## Phạm vi

Kiểm tra kỹ thuật toàn bộ 1.800 câu bằng parser backend thật trong database tạm.
Kiểm tra nội dung theo mẫu, có giải lại các ví dụ Toán dưới đây. Chưa kiểm định học
thuật đầy đủ 1.800 câu. Không sửa câu hỏi, manifest, mã ứng dụng hay dữ liệu thật.

| Môn/thư mục | Số câu | Parser hợp lệ | Lỗi parser | Chu kỳ ABCD toàn bộ |
|---|---:|---:|---:|---|
| Toan-12 | 200 | 200 | 0 | Có |
| Vat-li-12 | 200 | 200 | 0 | Có |
| Hoa-hoc-12 | 200 | 200 | 0 | Có |
| Sinh-hoc-12 | 200 | 200 | 0 | Có |
| Tieng-Anh-12 | 200 | 200 | 0 | Có |
| Lich-su-12 | 200 | 200 | 0 | Có |
| Dia-li-12 | 200 | 200 | 0 | Có |
| GDKT-PL-12 | 200 | 200 | 0 | Có |
| Ngu-van-12 | 200 | 200 | 0 | Có |

Mã câu duy nhất: 1800/1800. Chi tiết hash, ma trận,
nguồn và thống kê xem AUDIT-CODEX.json. Parser hợp lệ chỉ chứng minh định dạng.

## P1 — Sai đáp án hoặc không có đáp án đúng

1. **AIM_MATH12_BANK01_Q160**, Toan-12/questions-import.csv dòng 161:
   d: x=1+2t, y=-1+t, z=2t; mặt phẳng x+y+z-6=0 cho 5t=6.
   Giao điểm đúng là (17/5; 1/5; 12/5), không có trong bốn lựa chọn.
   File lại chọn D=(-1;-2;-2). Lời giải còn ghi 'chỉnh hệ số để t=1', chưa phải lời giải hoàn chỉnh.
2. **AIM_MATH12_BANK01_Q200**, cùng file dòng 201:
   Theo giả thiết đồng xu công bằng mà lời giải đang dùng, Bayes cho
   (0,5×3/5)/(0,5×3/5+0,5×2/10)=3/4. Đây là **A**, nhưng correctOption là **D=2/3**.
   Đề cũng nên nêu rõ đồng xu cân đối/công bằng.
3. **AIM_MATH12_BANK01_Q069**, cùng file dòng 70:
   Đặt M=(0;0;z). MA²=MB² cho 29-8z=14-6z, nên z=15/2.
   Không có lựa chọn (0;0;15/2); file chọn A=(0;0;19/2). Lời giải còn ghi
   'Chỉnh số liệu cho khớp đáp án', chứng tỏ câu chưa hoàn thiện.

Đây là ví dụ lỗi đã xác minh, không phải danh sách đầy đủ các câu sai.

## P1 — Đáp án có quy luật ở cả 9 môn

Tất cả môn đều lặp đúng ABCD 50 lần, nên có thể đoán toàn bộ đáp án theo số thứ tự.
Mỗi môn thực tế có chuỗi đáp án giống nhau dài nhất là 1, nhưng manifest và báo cáo
lại ghi 0. Cân bằng 50 A/B/C/D không thay thế kiểm tra tính đúng và tính ngẫu nhiên.
Cần giải đúng từng câu trước, rồi mới hoán vị phương án và cập nhật đáp án theo vị trí.

## P1 — Chưa đủ bằng chứng nguồn/học thuật

Có 18 mục nguồn chỉ trỏ tới trang chủ, không phải tài liệu cụ thể.
Ví dụ nguồn THPT Yên Hòa của Toán dùng http://thptyenhoa.edu.vn, nguồn Bộ dùng
https://moet.gov.vn, nhưng đều nhận là đã kiểm chứng. Thiếu PDF/bài cụ thể,
số trang hoặc đoạn kiến thức để truy vết từng câu. Không truy cập được liên kết
SGK bằng công cụ kiểm tra hiện tại không chứng minh sách/liên kết là giả, nhưng
cũng không đủ để xác nhận các tuyên bố kiểm chứng trong bộ bàn giao.

Có 7 mục nguồn đề cập tỷ lệ 80-80-40 trong mô tả ma trận trường.
Phải minh định đây là mục tiêu riêng của AI-Mentor, không gán cho trường nếu không có tài liệu chứng minh.
Phát hiện 2 lời giải chứa dấu hiệu chỉ dẫn sửa bản nháp; danh sách ở JSON.

## P2 — Chất lượng câu hỏi và ma trận cần rà soát lại

Các câu Ngữ văn Q160/Q200 và GDKT-PL Q200 được gán HARD nhưng các phương án nhiễu
quá phi lý, có thể loại ngay mà không cần kỹ năng vận dụng. Cần người biên tập
đánh giá lại mức độ khó, lời giải và độ hợp lý của phương án nhiễu.
Tiếng Anh có 200/200 câu dạng điền chỗ trống. Chưa đủ căn cứ nhận bộ này bao phủ
toàn bộ kỹ năng/dạng trắc nghiệm cả năm khi không có ma trận đọc hiểu tương ứng.
Ma trận chương/bài phải được đối chiếu mục lục SGK gốc; kiểm tra số lượng không xác nhận đúng phạm vi SGK.

Tham khảo chính thức đã mở để đối chiếu định hướng Ngữ văn, không thay cho việc
xác minh từng nguồn của bộ câu hỏi:
https://www.nxbgd.vn/bai-viet/sgk-ngu-van-12-bo-ket-noi-tri-thuc-voi-cuoc-song-hoan-thanh-chang-duong-doi-moi

## Trạng thái database và bước tiếp theo

Truy vấn chỉ đọc Supabase nhóm hnxphwuaccztzanjcuca: 0 câu mang mã 9 lô lớp 12
AIM_(MATH|PHY|CHEM|BIO|ENG|HIS|GEO|ECL|LIT)12_BANK01_Qxxx; 0 câu đã xuất bản.
Đây là kiểm tra theo mã lô, không khẳng định database không có nội dung lớp 12 khác.

Giữ nguyên các file làm bản nháp. Yêu cầu bên biên soạn rà soát đáp án/lời giải,
kiểm chứng nguồn và thiết kế lại phương án nhiễu; cập nhật báo cáo theo kiểm tra
thật. Sau sửa mới chạy lại parser, kiểm định học thuật và nghiệm thu trước import.
