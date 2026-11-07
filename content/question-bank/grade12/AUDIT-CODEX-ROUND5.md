# Phúc tra cuối hồ sơ học thuật — vòng 5

Ngày 2026-11-07. **CHƯA ĐẠT nghiệm thu học thuật.** Không nhập hoặc xuất bản dựa trên tuyên bố 1.800/1.800 đã đúng.

## Kỹ thuật đã kiểm tra lại
Parser backend trong database tạm: 1.800 hợp lệ, 0 lỗi; 1.800 mã duy nhất; ma trận khớp CSV cả 9 môn; hash CSV/Markdown khớp manifest. academic-audit.csv có 1.800 dòng: 1.686 PASS và 114 FIXED_VERIFIED.

## P1: Lỗi kiến thức/tính toán vẫn còn trong câu được duyệt

### AIM_PHY12_BANK01_Q149 — Hiệu ứng Hall
I=0,01 A, B=0,8 T, d=0,0002 m, V_H=0,002 V, |q|=1,6e-19 C.
n=IB/(|q|dV_H)=0,008/(6,4e-26)=1,25e23 m^-3.
CSV chọn D=1,25e20 m^-3, lệch 1.000 lần. Lời giải tự tính ra 1,25e23 nhưng vẫn trỏ D=1,25e20. Không có lựa chọn đúng. Hồ sơ học thuật đánh PASS.

### AIM_PHY12_BANK01_Q199 — Chất đánh dấu phóng xạ
A sau 6 giờ=1e-3*2^(-6/15)=7,57858e-4 Ci.
Mẫu 10 cm3 có 1,5e-7 Ci nên mật độ=1,5e-8 Ci/cm3.
V=7,57858e-4/(1,5e-8)=50.523,9 cm3=50,5239 lít.
CSV chọn A=5,0 lít và lời giải ghi khoảng 5.050 cm3, sai 10 lần. Không có lựa chọn đúng. Nếu muốn kết quả sinh lý khoảng 5 lít phải sửa dữ kiện có căn cứ, không tự sửa phép tính để khớp phương án. Hồ sơ học thuật đánh PASS.

## P2: Hồ sơ không phản ánh dữ liệu hiện tại
- Toán Q175: CSV đúng m=0, đáp án A; correction trong academic-audit.csv và FINAL-ACADEMIC-REPORT.md lại mô tả m=1, dây cung 2sqrt(3), báo cáo nói đáp án B.
- Anh Q021: CSV là culture/custom/public/mutual; nhật ký và báo cáo lại mô tả public/publish/culture/mutual.
- independent_result của toàn bộ 1.800 dòng trùng đúng chuỗi [correctOption] + nội dung đáp án hiện có. Không dòng evidence nào chứa URL. Điều này không tự chứng minh cách tạo hồ sơ, nhưng hồ sơ hiện chưa cung cấp phép giải/đối chiếu đủ để chứng minh từng câu đã được kiểm tra độc lập; hai lỗi PASS nêu trên trực tiếp bác bỏ kết luận tất cả đạt.

## Hướng xử lý
Sửa Q149/Q199; kiểm tra lại toàn bộ bài định lượng, đơn vị và số mũ, đặc biệt môn Vật lí. Rà theo nguyên nhân, không chỉ sửa hai ví dụ. Lưu phép tính độc lập có thể đối chiếu cho từng câu định lượng; các câu cần nguồn lưu tài liệu cụ thể và trang/mục hoặc URL thực sự đã đọc. Đưa câu chưa kiểm chứng về NEEDS_REVIEW, đồng bộ báo cáo với CSV sau cùng. Không nhận đủ 1.800 câu đúng chỉ vì có đủ 1.800 dòng PASS.

Chỉ kiểm tra nội dung theo mẫu trong lượt này; các lỗi trên là ví dụ đã xác nhận, không phải danh sách toàn bộ lỗi còn lại. Không sửa câu hỏi hoặc dữ liệu thật. Báo cáo cũ được giữ nguyên.
