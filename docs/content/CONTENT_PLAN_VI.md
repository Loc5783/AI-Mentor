# Kế hoạch nội dung chín môn — phạm vi đang làm: lớp 10

Cập nhật 30/10/2026. App đã có lộ trình và nội dung nhập môn lớp 10 cho cả chín môn; coverage chương trình từng môn vẫn còn một phần. Lớp 11–12 để giai đoạn sau.

## Nguồn và mốc nội dung
- Bám chương trình GDPT hiện hành và các cập nhật liên quan; Kết nối tri thức với cuộc sống làm mốc sắp xếp nội dung cho năm học 2026–2027.
- [Báo Điện tử Chính phủ, 26/12/2025](https://baochinhphu.vn/ket-noi-tri-thuc-voi-cuoc-song-la-sach-giao-khoa-thong-nhat-toan-quoc-tu-nam-hoc-20262027-10225122622052997.htm): quyết định 3588/QĐ-BGDĐT lựa chọn Kết nối tri thức dùng thống nhất từ 2026–2027.
- [Thông tư 17/2025/TT-BGDĐT trên Cổng Chính phủ](https://chinhphu.vn/?docid=215347&pageid=27160): cập nhật chương trình; cần đọc đúng phụ lục/phạm vi, không suy ra tất cả môn đều đã đổi.
- [Chương trình tổng thể hợp nhất của Bộ GDĐT](https://moet.gov.vn/content/vanban/Lists/VBPQ/Attachments/1483/vbhn-chuong-trinh-tong-the.pdf) để đối chiếu cấu trúc. Chưa đủ thay cho chuẩn chi tiết từng môn.

## Trạng thái runtime
| Môn | Lớp 10 | Lớp 11 | Lớp 12 |
|---|---|---|---|
| Vật lý, Hóa học, Sinh học | 20 bài/60 câu, 4 quiz ôn mỗi môn | Chưa biên soạn | Chưa biên soạn |
| Toán, Ngữ văn, Tiếng Anh, Lịch sử, Địa lí, GDKT & Pháp luật | 15 bài/45 câu, 3 quiz ôn mỗi môn; nội dung nhập môn từng phần | Chưa biên soạn | Chưa biên soạn |

Chín gói backend/curriculum chưa được ánh xạ là đủ mục lục một bộ SGK. Sáu gói 15 bài là phần nhập môn tự biên soạn theo yêu cầu cần đạt chung, chưa thay thế đầy đủ chương trình môn học.

## Bước xây dựng
1. Đọc chuẩn môn học và mục lục tài liệu nhà xuất bản đúng chín tổ hợp môn/lớp; ghi nguồn/phiên bản vào bảng coverage.
2. Lập chương/chủ đề, yêu cầu cần đạt, kiến thức tiên quyết; chia bài ngắn của app. Không mặc định một tiết SGK bằng một bài app.
3. Mỗi bài có mục tiêu, giải thích tự biên soạn, ví dụ có lời giải, bài dễ/vừa/vận dụng và giải thích từng câu. Kiểm tra đơn vị/điều kiện công thức/thuật ngữ.
4. Sau mỗi năm bài review10/15 câu phủ đúng năm bài. Placement phân bố theo chủ đề có nội dung; không gọi ngân hàng nhỏ là đánh giá đủ cả năm.
5. Kiểm tra đáp án duy nhất, phép tính, độ khó/phản hồi sai và giáo viên duyệt. AI hỗ trợ soạn, không tự coi nội dung sinh ra là đã xác minh.
6. Giữ ID thưởng ổn định khi sửa nhỏ; thay cấu trúc phải có version/migration, không làm mất tiến độ hoặc nhận EXP lặp.

Tài liệu này chốt nguồn và cách triển khai, chưa phải mục lục chi tiết đã đối chiếu hay nội dung đủ cả năm.

## Sáu môn bổ sung lớp 10 — 30/10/2026

Đã biên soạn 90 bài nhập môn (15 bài mỗi môn), 270 câu hỏi có giải thích và 18 quiz ôn (3 quiz 10 câu mỗi môn) cho Toán, Ngữ văn, Tiếng Anh, Lịch sử, Địa lí, Giáo dục kinh tế và pháp luật. Nội dung tự biên soạn theo chủ đề và yêu cầu cần đạt chung; chưa đối chiếu trọn mục lục Kết nối tri thức hay các bộ SGK khác. Cần giáo viên rà soát tính chính xác, thuật ngữ và độ khó trước khi dùng chính thức.

Các gói được đưa vào backend local ngay khi build nội dung. Hai migration `202610300001_remaining_grade10_curricula.sql` và `202610300002_adaptive_placement.sql` đã được áp dụng lên project Supabase đã liên kết ngày 30/10/2026; trạng thái remote đã được xác nhận bằng `migration list`.

## Bổ sung 15 bài mỗi môn — 20/10/2026

Tổng thêm 45 bài, 135 câu có đáp án và lời giải. Mỗi môn hiện có 20 bài, 60 câu thực hành và 4 checkpoint 10 câu, mỗi checkpoint lấy hai câu từ từng bài trong đúng nhóm năm bài. Không thêm 15 bài tổng cộng và không chia thành nội dung lớp 11–12 chưa được biên soạn.

- Lý 6–10: biến đổi đều, độ dịch chuyển, đồ thị v–t, rơi tự do, ném ngang. 11–15: hợp lực, ba định luật Newton, trọng lực/lực đỡ. 16–20: ma sát, công, công suất, động/thế năng, cơ năng.
- Hóa 6–10: lớp/phân lớp, cấu hình, electron ngoài cùng, chu kì, xu hướng tuần hoàn. 11–15: octet, ion, cộng hóa trị, phân cực, hydrogen. 16–20: số oxi hóa, redox, nhiệt phản ứng, tốc độ, halogen.
- Sinh 6–10: nước, carbohydrate, lipid, protein, nucleic acid. 11–15: bào quan, enzyme, ATP, hô hấp, quang hợp. 16–20: chu kì tế bào, nguyên phân, giảm phân, vi khuẩn, virus.

Nội dung tự biên soạn theo chủ đề lớp 10; không tự nhận trùng đầy đủ mục lục SGK. Các nguồn khoa học dùng đối chiếu khái niệm: [OpenStax Physics, công và năng lượng](https://openstax.org/books/physics/pages/9-1-work-power-and-the-work-energy-theorem), [Chemistry 2e, cấu hình electron](https://openstax.org/books/chemistry-2e/pages/6-4-electronic-structure-of-atoms-electron-configurations), [Biology 2e, virus và vật chủ](https://openstax.org/books/biology-2e/pages/21-2-virus-infections-and-hosts). Cần giáo viên duyệt mức độ khó, thuật ngữ và coverage trước dùng chính thức.

Giữ version `intro-2026.1` vì đây là mở rộng tương thích bằng cách nối cuối: năm bài đầu, câu hỏi, vị trí đáp án, placement và review đầu không đổi. Không dùng cách này nếu sửa nội dung đang làm hay đổi ID. Kiểm thử nâng cấp thực tế khởi động lại server với cùng SQLite, giữ bài đã xong, số lần sai, bước đang làm và ledger; bài mới nhận thưởng, bài cũ không nhận lại. 33 ca backend đạt, bao gồm đi hết 20 bài + 4 review cho từng môn.
