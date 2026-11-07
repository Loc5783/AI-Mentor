# Báo cáo hiệu chỉnh ngân hàng câu hỏi Lớp 12 — Vòng 3 (AI-Mentor)

**Ngày báo cáo:** 07/11/2026  
**Thư mục ngân hàng:** `C:\Users\PhamDinhDong\Documents\ChatGPT\Dự án AI-Mentor\Ngan-hang-cau-hoi\Lop-12`  
**Dự án ứng dụng:** `E:\AI-Mentor`  
**Trạng thái ngân hàng:** `DRAFT` (Chưa import database thật, chưa xuất bản, chưa commit/push)  
**Tài liệu kiểm tra độc lập gốc:** `AUDIT-CODEX-ROUND3.md` & `AUDIT-CODEX-ROUND3.json` (được bảo toàn nguyên vẹn, không chỉnh sửa)

---

## 1. Nội dung hiệu chỉnh cụ thể

### A. Tiếng Anh — `AIM_ENG12_BANK01_Q021` (Phát âm chữ cái 'u')
* **Vấn đề Codex vòng 3 chỉ ra:** Lời giải vòng 2 nhận định từ *current* có âm /ʌ/ ở cả Anh–Anh (RP) và Anh–Mỹ (GA). Điều này chưa chính xác vì theo các từ điển chuẩn, biến thể Anh–Mỹ của *current* là /ˈkɝː.ənt/ (mang âm /ɝː/), dẫn đến sự thiếu ổn định về ngữ âm vùng miền.
* **Hiệu chỉnh đã thực hiện:**
  - Thay thế hoàn toàn từ *current* bằng từ ***public*** (/ʌ/).
  - Xác lập nhóm 4 từ chuẩn mực bám sát Unit 2 (*A multicultural world*): `culture`, `custom`, `public` và `mutual`.
  - Tra cứu và đối chiếu trực tiếp trên hệ thống từ điển **Oxford Advanced Learner's Dictionary** (`oxfordlearnersdictionaries.com`) và **Cambridge Dictionary** (`dictionary.cambridge.org`):
    1. **culture**: Oxford BrE /ˈkʌltʃə(r)/, NAmE /ˈkʌltʃər/; Cambridge UK /ˈkʌl.tʃər/, US /ˈkʌl.tʃɚ/ $\rightarrow$ chữ cái 'u' đầu tiên phát âm ổn định là **/ʌ/** ở cả hai biến thể.
    2. **custom**: Oxford BrE /ˈkʌstəm/, NAmE /ˈkʌstəm/; Cambridge UK /ˈkʌs.təm/, US /ˈkʌs.təm/ $\rightarrow$ chữ cái 'u' phát âm ổn định là **/ʌ/** ở cả hai biến thể.
    3. **public**: Oxford BrE /ˈpʌblɪk/, NAmE /ˈpʌblɪk/; Cambridge UK /ˈpʌb.lɪk/, US /ˈpʌb.lɪk/ $\rightarrow$ chữ cái 'u' phát âm ổn định là **/ʌ/** ở cả hai biến thể.
    4. **mutual**: Oxford BrE /ˈmjuːtʃuəl/, NAmE /ˈmjuːtʃuəl/; Cambridge UK /ˈmjuː.tʃu.əl/, US /ˈmjuː.tʃu.əl/ $\rightarrow$ chữ cái 'u' đầu tiên phát âm là **/juː/** ở cả hai biến thể.
  - Đề bài ghi rõ quy tắc xét chữ cái 'u' đầu tiên:
    > *"Mark the letter A, B, C, or D to indicate the word whose first letter 'u' is pronounced differently from the others: mutual, culture, custom, public."*
  - Đồng bộ tuyệt đối: Option A: `culture`, Option B: `custom`, Option C: `public`, Option D: `mutual`. Đáp án đúng: **Option D** (`mutual`). Lời giải ghi rõ phiên âm BrE/NAmE và trích dẫn nguồn từ điển cụ thể.

---

### B. Ngữ văn — Xác minh cấu trúc mục lục SGK và định danh chuẩn hóa
* **Vấn đề Codex vòng 3 chỉ ra:**
  - Ngân hàng vòng 2 tuyên bố "khớp 100% mục lục SGK" nhưng lại có "Bài 10: Nghệ thuật sân khấu và Tiếng Việt thực hành" (SGK không có bài 10).
  - Tên Bài 8 trong tài liệu trường và SGK là *Dữ liệu trong văn bản thông tin*, nhưng ngân hàng ghi *Đọc văn bản thông tin*.
* **Xác minh thực tế nguồn tài liệu:**
  - Tra cứu đối chiếu mục lục chính thức của Sách giáo khoa Ngữ văn 12 (Tập 1 và Tập 2) – Bộ sách *Kết nối tri thức với cuộc sống* (Nhà xuất bản Giáo dục Việt Nam) trên Hành trang số (`hanhtrangso.nxbgd.vn`):
    - **Tập một** gồm 5 bài:
      - *Bài 1: Khả năng lớn lao của tiểu thuyết*
      - *Bài 2: Những thế giới thơ*
      - *Bài 3: Lập luận trong văn bản nghị luận*
      - *Bài 4: Yếu tố kì ảo trong truyện kể*
      - *Bài 5: Tiếng cười của hài kịch*
      - (Phần Ôn tập học kỳ I)
    - **Tập hai** gồm 4 bài:
      - *Bài 6: Hồ Chí Minh – “Văn hóa phải soi đường cho quốc dân đi”*
      - *Bài 7: Sự thật trong tác phẩm kí*
      - *Bài 8: Dữ liệu trong văn bản thông tin*
      - *Bài 9: Văn học và cuộc đời* (Văn bản đọc: *Vội vàng*, *Hồn Trương Ba, da hàng thịt*; Tri thức ngữ văn về giá trị của văn học và giữ gìn tiếng Việt)
      - (Phần Ôn tập học kỳ II và Hệ thống hóa kiến thức cuối năm)
  - **Kết luận xác minh:** Toàn bộ SGK Ngữ văn 12 Kết nối tri thức **chỉ có 9 bài học chính khóa (Bài 1 đến Bài 9)**. Hoàn toàn không có "Bài 10".
* **Hiệu chỉnh và chuẩn hóa cấu trúc:**
  1. **Bài 1 đến Bài 9:** Chuẩn hóa tên chương mục khớp 100% nguyên văn tiêu đề SGK:
     - Bài 4: `Bài 4: Yếu tố kì ảo trong truyện kể` (dùng chữ "kì" theo chuẩn SGK).
     - Bài 6: `Bài 6: Hồ Chí Minh – “Văn hóa phải soi đường cho quốc dân đi”`.
     - Bài 7: `Bài 7: Sự thật trong tác phẩm kí` (bỏ chữ "và hồi kí").
     - Bài 8: `Bài 8: Dữ liệu trong văn bản thông tin` (đổi từ "Đọc văn bản thông tin", nội dung 20 câu hỏi xoay quanh số liệu định lượng, biểu đồ, tính khách quan, nguồn tin).
     - Bài 9: `Bài 9: Văn học và cuộc đời` (đổi từ "Văn học và đời sống").
  2. **Nhóm thứ 10 (Q181 đến Q200):**
     - Nội dung 20 câu này gồm 14 câu phân tích kịch bản *Hồn Trương Ba, da hàng thịt* (Lưu Quang Vũ) và 6 câu thực hành tiếng Việt tổng hợp.
     - Bỏ hoàn toàn cách gọi "Bài 10 SGK". Định danh chính thức thành:
       **`Chủ đề bổ sung: Nghệ thuật kịch và Tiếng Việt thực hành`** (Mã ma trận: `TOPIC_SUPPLEMENT`).
     - Ghi chú minh bạch trong `matrix.csv`, `sources.csv` và báo cáo: Đây là chủ đề ôn tập bổ sung chuyên sâu của dự án AI-Mentor, không phải bài học số 10 trong SGK.
  3. **Đồng bộ toàn diện:** Đã cập nhật thống nhất trong `questions-import.csv`, `questions-review.md`, `matrix.csv`, `sources.csv`, `corrections.md` và mã nguồn sinh dữ liệu `build_lit12.py`.

---

## 2. Kết quả kiểm tra và xác minh kỹ thuật

* **Mã câu hỏi và số lượng:** Giữ nguyên vẹn 200 câu hỏi mỗi môn, mã câu duy nhất từ Q001 đến Q200, không mất mát dữ liệu.
* **Kiểm tra Parser Backend (`LibraryService.parse_import_file`):**
  - Chạy kiểm thử tự động với Mock DB chuẩn backend trên cả 2 môn đã hiệu chỉnh và toàn bộ 9 môn:
    - Tiếng Anh 12: **200/200 valid**, 0 errors.
    - Ngữ văn 12: **200/200 valid**, 0 errors.
    - Toàn bộ 9 môn: **1.800/1.800 valid**, 0 errors.
* **Tính toán lại ma trận từ CSV thực tế:**
  - Tiếng Anh: 10 Unit $\times$ 20 câu = 200 câu (80 EASY, 80 MEDIUM, 40 HARD).
  - Ngữ văn: 9 Bài SGK + 1 Chủ đề bổ sung $\times$ 20 câu = 200 câu (80 EASY, 80 MEDIUM, 40 HARD). Khớp chính xác từng dòng giữa `matrix.csv` và cột `chapter` trong `questions-import.csv`.
* **Phân bổ đáp án:**
  - Cả hai môn đều bảo đảm chính xác: 50 A, 50 B, 50 C, 50 D.
  - Chuỗi lặp đáp án liên tiếp tối đa (max run) bằng đúng 3, không có chu kỳ tuần hoàn lặp lại.
* **Cập nhật mã nguồn sinh dữ liệu:**
  - `eng_part1.py` và `build_eng12.py`: Đã cập nhật bộ từ `public`, prompt và lời giải để khi chạy lại builder sẽ không bị tái tạo lỗi cũ.
  - `lit_parts/lit_part2.py`, `lit_parts/lit_part3.py` và `build_lit12.py`: Đã cập nhật tiêu đề chương mục chuẩn xác và định danh chủ đề bổ sung.
* **Đồng bộ Manifest:**
  - Tệp `manifest.json` và `audit-resolution.csv` đã được tái tạo với mã băm SHA-256 mới nhất của toàn bộ các thư mục.

---

## 3. Phần chưa xác minh & Giới hạn học thuật

1. **Giới hạn kiểm chứng toàn văn sách giáo khoa:**
   - Mục lục 9 bài học của SGK Ngữ văn 12 (Tập 1 và Tập 2) đã được kiểm chứng đối chiếu qua mục lục chính thức trên Hành trang số (NXB Giáo dục Việt Nam) và kế hoạch bài dạy của trường THPT. Tuy nhiên, toàn bộ từng trang văn bản ngữ liệu in giấy chưa được đọc trực tiếp 100% tất cả các trang; dự án sử dụng các văn bản trích dẫn tiêu biểu theo đặc tả chương trình GDPT 2018.
2. **Trạng thái nghiệm thu:**
   - Báo cáo này xác nhận việc hoàn thành sửa đổi kỹ thuật và học thuật theo đúng yêu cầu của người dùng và biên bản `AUDIT-CODEX-ROUND3.md`.
   - Ngân hàng **CHƯA** tự nhận đã được Codex nghiệm thu học thuật cuối cùng; sẵn sàng chuyển giao để Codex thực hiện đánh giá độc lập tiếp theo.
3. **Cam kết an toàn dữ liệu:**
   - Không import vào cơ sở dữ liệu thật của ứng dụng.
   - Không xuất bản lên app hoặc admin.
   - Không commit hoặc push lên Git.
   - Giữ nguyên toàn bộ các báo cáo kiểm định gốc `AUDIT-CODEX*.md/json`.
