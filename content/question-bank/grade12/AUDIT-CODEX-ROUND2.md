# Kiểm tra độc lập ngân hàng lớp 12 — vòng 2

Ngày: 2026-11-07. **CHƯA ĐẠT — chưa nên nhập hoặc xuất bản.**

Đã kiểm tra kỹ thuật toàn bộ ngân hàng bằng parser backend trong database tạm. Nội dung học thuật kiểm tra theo mẫu, không chứng nhận toàn bộ 1.800 câu. Không sửa nội dung hay dữ liệu thật. Báo cáo vòng 1 được giữ nguyên.

| Môn | Số câu | Parser hợp lệ | Lỗi parser | Hash CSV/MD khớp manifest |
|---|---:|---:|---:|---|
| Toan-12 | 200 | 200 | 0 | True/True |
| Vat-li-12 | 200 | 200 | 0 | True/True |
| Hoa-hoc-12 | 200 | 200 | 0 | True/True |
| Sinh-hoc-12 | 200 | 200 | 0 | True/True |
| Tieng-Anh-12 | 200 | 200 | 0 | True/True |
| Lich-su-12 | 200 | 200 | 0 | True/True |
| Dia-li-12 | 200 | 200 | 0 | True/True |
| GDKT-PL-12 | 200 | 200 | 0 | True/True |
| Ngu-van-12 | 200 | 200 | 0 | True/True |

## Các lỗi còn lại

- **P1 — AIM_MATH12_BANK01_Q175**: Cả bốn lựa chọn đều đúng. Mọi m thực đều cho đường thẳng đi qua tâm (1,2,0) của mặt cầu bán kính 2. Hai giao điểm luôn cách nhau 4; không có điều kiện z=0 trong đề để suy ra m=0.
- **P1 — AIM_ENG12_BANK01_Q021**: Lời giải nói chữ u trong diverse, nhưng diverse không có chữ u. Đề yêu cầu phần gạch chân nhưng không đánh dấu phần cần so sánh.
- **P1 — AIM_ENG12_BANK01_Q141**: Nếu so sánh chữ a đầu tiên: nature/danger và habitat/manage tạo hai cặp phát âm, không có một từ khác biệt duy nhất. Đề không đánh dấu phần gạch chân.
- **P1 — AIM_ENG12_BANK01_Q149**: Lời giải phân tích ecosystem, sanctuary, conservation, biodiversity, không phải natural, habitat, endangered, predator trong câu hỏi. Còn ghi chú Cần xét rõ từ khác biệt duy nhất.
- **P1 — ENGLISH_EMBEDDED_LABELS**: Nhãn A/B/C/D trong đề không đồng bộ với bốn trường lựa chọn sau hoán vị; gồm Q001, Q009, Q019. Q019 còn dùng đánh dấu lỗi và phương án không khớp vị trí.
- **P2 — AUDIT_RESOLUTION**: Nhật ký mô tả Q069 là hình chiếu, Q160 là hình chiếu lên mặt phẳng khác, Q200 là hai đồng xu, Q047 có hàm số/tiệm cận khác. Không khớp câu hỏi hiện tại dù ghi RESOLVED.
- **P1 — LITERATURE_MATRIX**: Ma trận Ngữ văn không khớp chapter của CSV ở cả 10 dòng. Ví dụ matrix Bài 1: Khát vọng tự do (Truyện truyền kì), nhưng CSV Bài 1: Khả năng lớn lao của tiểu thuyết. Không thể dùng ma trận hiện tại làm bằng chứng phân bổ nội dung.

Số câu Tiếng Anh có danh sách lựa chọn lặp trong đề nhưng khác thứ tự với các trường option: **14**. Mã cụ thể trong JSON.

## Những sửa chữa đã xác nhận

- Toán Q069, Q160, Q200 đã có đáp án đúng; Q047 đã nêu rõ c=1.
- Đã bỏ chu kỳ ABCD toàn bộ ở cả 9 môn; kiểm tra lại phân bố, ma trận, nguồn tham chiếu và hash trong JSON.

## Giới hạn nghiệm thu

Parser hợp lệ không chứng minh kiến thức đúng. Nguồn ghi chưa kiểm chứng cần giữ đúng trạng thái; không nhận là đã đối chiếu trực tiếp toàn bộ. Cần sửa lỗi trên, đồng bộ câu hỏi/lựa chọn/lời giải và nhật ký, rồi thẩm định học thuật từng môn trước xuất bản.