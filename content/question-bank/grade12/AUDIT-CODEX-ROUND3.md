# Phúc tra ngân hàng lớp 12 — vòng 3

Ngày: 2026-11-07. Kết luận: ĐẠT kiểm tra kỹ thuật đã chạy; CHƯA nghiệm thu học thuật toàn bộ.

## Đã xác nhận
- Parser backend trong database tạm: 1.800/1.800 hợp lệ, 0 lỗi.
- Cả 9 môn: 200 câu, 80 EASY/80 MEDIUM/40 HARD, 200 prompt phân biệt trong từng môn, không trùng nội dung bốn lựa chọn trong cùng câu; ma trận khớp số liệu CSV.
- Hash CSV/Markdown và nhật ký sửa chữa khớp manifest. Hash khớp không chứng minh CSV và Markdown đồng bộ ngữ nghĩa.
- Toán Q175 đã đổi z=mt thành z=m+t. Khoảng cách tâm tới đường thẳng bằng |m|sqrt(2/3), dây cung lớn nhất chỉ khi m=0. Sửa đúng.
- Anh Q021 đã bỏ diverse; Q141 đã bỏ manage; Q149 đã thay đúng lời giải. Các lỗi nhãn A/B/C/D cũ không còn trong nhóm đã sửa; 20 câu tìm lỗi sai có phần ngoặc vuông khớp bốn lựa chọn.
- Ngữ văn: ma trận đã khớp nội bộ CSV, khác với vòng trước.

## Cần xử lý hoặc làm rõ
1. Anh Q021: lời giải khẳng định current có /ʌ/ theo cả RP và GA. Không đúng với phiên âm Mỹ /ˈkɝː.ənt/ được ghi trong tài liệu phát âm dẫn Cambridge. Cần giới hạn đề và lời giải theo Anh–Anh, hoặc thay current bằng từ không gây khác biệt vùng miền. Đáp án mutual theo cách đọc Anh–Anh vẫn phù hợp; không nên nhận câu đã chuẩn mọi biến thể.
   Đối chiếu: https://repositori.kemendikdasmen.go.id/20737/1/Seri%20Bahan%20Ajar%20Bahasa%20Asing%20untuk%20Misi%20Perdamaian%20Bahasa%20Inggris%20%28Madya%29.pdf (trang in 86).
2. Ngữ văn: chưa có đủ bằng chứng cho tuyên bố khớp 100% mục lục SGK, đặc biệt Bài 10 tự đặt Nghệ thuật sân khấu và Tiếng Việt thực hành. Cần dẫn trang mục lục SGK/SGV đã đối chiếu hoặc ghi đây là nhóm bổ sung của dự án. Bài 8 trong tài liệu trường là Dữ liệu trong văn bản thông tin; ngân hàng dùng Đọc văn bản thông tin. Hai tên không giống nguyên văn. Việc cùng đổi tên CSV và ma trận chỉ chứng minh thống nhất nội bộ.
   Tài liệu trường tìm được: https://thptdodangtuyen.edu.vn/uploads/ho-so-ca-nhan/2025_04/bai-8-k12.vb12-tiet-79-84.pdf . Công cụ chỉ đọc được chỉ mục tìm kiếm, chưa mở được toàn bộ PDF; không dùng làm chứng nhận toàn bộ mục lục.

## Giới hạn
Đã đọc lại các câu sửa và nhóm ngữ âm/tìm lỗi sai Tiếng Anh, chưa giải độc lập toàn bộ 1.800 câu. Không sửa câu hỏi, không nhập database thật, không xuất bản. Báo cáo cũ giữ nguyên. Chi tiết kiểm tra kỹ thuật: AUDIT-CODEX-ROUND3.json.
