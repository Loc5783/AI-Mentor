# Báo cáo hiệu chỉnh ngân hàng câu hỏi Lớp 12 — Vòng 2 (AI-Mentor)

**Ngày báo cáo:** 07/11/2026  
**Thư mục ngân hàng:** `C:\Users\PhamDinhDong\Documents\ChatGPT\Dự án AI-Mentor\Ngan-hang-cau-hoi\Lop-12`  
**Dự án ứng dụng:** `E:\AI-Mentor`  
**Trạng thái ngân hàng:** `DRAFT` (Chưa import database, chưa xuất bản, chưa commit/push)  
**Tài liệu kiểm tra độc lập gốc:** `AUDIT-CODEX-ROUND2.md` & `AUDIT-CODEX-ROUND2.json` (được bảo toàn nguyên vẹn, không chỉnh sửa)

---

## 1. Tổng quan kết quả thực hiện

Toàn bộ các lỗi học thuật, kỹ thuật trình bày và mô tả bằng chứng kiểm định vòng 2 do Codex chỉ ra đã được rà soát, giải lại độc lập và khắc phục triệt để trên cả 9 môn học (1.800 câu hỏi, mỗi môn 200 câu):

| Môn học | Số câu | EASY / MED / HARD | Phân bổ đáp án A-B-C-D | Chuỗi lặp tối đa | Parser backend hợp lệ | Lỗi parser |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Toán** | 200 | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 3 | 200 / 200 | 0 |
| **Vật lí** | 200 | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 3 | 200 / 200 | 0 |
| **Hóa học** | 200 | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 3 | 200 / 200 | 0 |
| **Sinh học** | 200 | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 3 | 200 / 200 | 0 |
| **Tiếng Anh** | 200 | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 3 | 200 / 200 | 0 |
| **Lịch sử** | 200 | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 3 | 200 / 200 | 0 |
| **Địa lí** | 200 | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 3 | 200 / 200 | 0 |
| **GDKT & PL** | 200 | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 3 | 200 / 200 | 0 |
| **Ngữ văn** | 200 | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 3 | 200 / 200 | 0 |
| **TỔNG CỘNG** | **1.800** | **720 / 720 / 360** | **450 / 450 / 450 / 450** | **<= 3** | **1.800 / 1.800** | **0** |

---

## 2. Chi tiết các nội dung hiệu chỉnh trọng tâm

### A. Toán — `AIM_MATH12_BANK01_Q175`
* **Vấn đề Codex phát hiện (P1):** Đề cũ cho đường thẳng $d: x = 1 + t, y = 2 - t, z = mt$ và mặt cầu $(S): x^2 + y^2 + z^2 - 2x - 4y + 1 = 0$. Mặt cầu có tâm $I(1; 2; 0)$, bán kính $R = 2$. Do tại $t = 0$ ta luôn có $x = 1, y = 2, z = 0$ với mọi $m$ nên đường thẳng $d$ luôn đi qua tâm $I$ với mọi giá trị $m$. Khoảng cách hai giao điểm luôn bằng đường kính $2R = 4$, dẫn đến cả 4 lựa chọn trong đề bài đều đúng.
* **Giải lại độc lập và khắc phục:**
  - Điều chỉnh phương trình tham số của $d$ thành: $x = 1 + t, y = 2 - t, z = m + t$.
  - Đường thẳng $d$ đi qua $M_0(1; 2; m)$ và có vectơ chỉ phương $\vec{u} = (1; -1; 1)$.
  - Ta có $\vec{IM_0} = (0; 0; m)$, $[\vec{IM_0}, \vec{u}] = (m; m; 0)$.
  - Khoảng cách từ tâm $I$ đến đường thẳng $d$:
    $$d(I, d) = \frac{|[\vec{IM_0}, \vec{u}]|}{|\vec{u}|} = \frac{\sqrt{m^2 + m^2}}{\sqrt{1^2 + (-1)^2 + 1^2}} = |m|\sqrt{\frac{2}{3}}.$$
  - Đường thẳng $d$ cắt mặt cầu tại hai điểm phân biệt khi $d(I, d) < R \Leftrightarrow \frac{2m^2}{3} < 4 \Leftrightarrow m^2 < 6$.
  - Độ dài dây cung: $AB = 2\sqrt{R^2 - d(I, d)^2} = 2\sqrt{4 - \frac{2m^2}{3}} \le 2\sqrt{4} = 4$.
  - Dây cung lớn nhất bằng 4 khi và chỉ khi $d(I, d) = 0 \Leftrightarrow m = 0$.
  - Các giá trị $m = 1, m = -1, m = 2$ đều cho $d(I, d) > 0$ nên dây cung nhỏ hơn 4.
* **Kết quả:** Đề bài có duy nhất nghiệm đúng $m = 0$. Phương án đúng gán vào Option A ($m = 0$).

---

### B. Tiếng Anh — `AIM_ENG12_BANK01_Q021`
* **Vấn đề Codex phát hiện (P1):** Lời giải cũ phân tích cách phát âm chữ cái "u" trong từ "diverse", trong khi từ "diverse" hoàn toàn không có chữ cái "u". Đề bài ghi yêu cầu "underlined part" nhưng giao diện không có gạch chân, gây khó hiểu cho người học.
* **Khắc phục sư phạm:**
  - Thay từ "diverse" bằng từ "current" (/ʌ/).
  - Xác lập nhóm 4 từ chuẩn xác bám sát SGK Tiếng Anh 12 Global Success Unit 2 (*A multicultural world*): `culture` (/ˈkʌl.tʃər/), `custom` (/ˈkʌs.təm/), `current` (/ˈkʌr.ənt/) và `mutual` (/ˈmjuː.tʃu.əl/).
  - Đề bài ghi rõ chữ cái cần so sánh bằng văn bản:
    > *"Mark the letter A, B, C, or D to indicate the word whose letter 'u' is pronounced differently from the others: mutual, culture, custom, current."*
  - Lời giải phân tích chi tiết: 3 từ phát âm là /ʌ/, từ "mutual" phát âm là /juː/ (ở chữ cái 'u' đầu tiên).
* **Kết quả:** Đáp án đúng gán vào Option D (`mutual`). 100% chuẩn xác từ điển Oxford Learner's Dictionary và chương trình Global Success 12.

---

### C. Tiếng Anh — `AIM_ENG12_BANK01_Q141`
* **Vấn đề Codex phát hiện (P1):** Đề cũ chia 4 từ thành 2 cặp phát âm nguyên âm "a": `nature`/`danger` phát âm là /eɪ/, còn `habitat`/`manage` phát âm là /æ/. Không có một từ duy nhất khác biệt với ba từ còn lại.
* **Khắc phục sư phạm:**
  - Chuẩn hóa bộ 4 từ bám sát Unit 8 (*Wildlife conservation*): `nature` (/ˈneɪ.tʃər/), `danger` (/ˈdeɪn.dʒər/), `native` (/ˈneɪ.tɪv/) và `habitat` (/ˈhæb.ɪ.tæt/).
  - Ba từ thuộc cùng một nhóm phát âm /eɪ/, từ duy nhất khác biệt mang âm /æ/ là `habitat`.
  - Đề bài chỉ rõ chữ cái cần so sánh:
    > *"Mark the letter A, B, C, or D to indicate the word whose letter 'a' is pronounced differently from the others: habitat, nature, danger, native."*
* **Kết quả:** Đáp án đúng gán vào Option B (`habitat`).

---

### D. Tiếng Anh — `AIM_ENG12_BANK01_Q149`
* **Vấn đề Codex phát hiện (P1):** Lời giải phân tích bộ từ `ecosystem, sanctuary, conservation, biodiversity`, không khớp với 4 từ thực tế trong đề `endangered, natural, habitat, predator`. Đồng thời lời giải còn sót ghi chú nháp: *"Cần xét rõ từ khác biệt duy nhất"*.
* **Khắc phục:**
  - Viết lại lời giải phân tích đúng 4 từ của đề bài:
    - `endangered` (/ɪnˈdeɪn.dʒəd/): trọng âm rơi vào âm tiết thứ 2.
    - `natural` (/ˈnætʃ.ər.əl/): trọng âm rơi vào âm tiết thứ 1.
    - `habitat` (/ˈhæb.ɪ.tæt/): trọng âm rơi vào âm tiết thứ 1.
    - `predator` (/ˈpred.ə.tər/): trọng âm rơi vào âm tiết thứ 1.
  - Xóa sạch 100% các từ khóa nháp.
* **Kết quả:** Đáp án đúng gán vào Option B (`endangered`).

---

### E. Tiếng Anh — Khắc phục lỗi lệch nhãn phương án trong đề (`embedded option label mismatch`)
* **Vấn đề Codex phát hiện (P1 - 14 câu ngữ âm/trọng âm + 20 câu tìm lỗi sai):**
  - Trong 14 câu Ngữ âm/Trọng âm (Q001, Q009, Q029, Q041, Q049, Q061, Q081, Q101, Q109, Q129, Q141, Q149, Q161, Q181), đề bài nhúng danh sách cứng `\nA. ...\nB. ...\nC. ...\nD. ...`. Khi hoán vị các trường option ngẫu nhiên, chữ cái ghi trong thân đề bị lệch hoàn toàn với nút lựa chọn A/B/C/D của người học.
  - Trong 20 câu Tìm lỗi sai (Q012, Q019, Q032, Q039, Q052, Q059, Q072, Q079, Q092, Q099, Q112, Q119, Q132, Q139, Q152, Q159, Q172, Q179, Q192, Q199), thân câu văn chèn các ký hiệu `(A)`, `(B)`, `(C)`, `(D)` không đồng bộ với 4 trường lựa chọn.
* **Giải pháp chuẩn hóa đồng bộ:**
  1. **Toàn bộ 20 câu Ngữ âm/Trọng âm:** Xóa hoàn toàn danh sách có chữ cái `A. / B. / C. / D.` trong đề bài. Đề bài liệt kê trung tính bộ từ cần xét (ngăn cách bằng dấu phẩy). Bốn trường `optionA..optionD` chứa 4 từ tương ứng. Người học chọn nút phương án nào thì nội dung nút đó khớp hoàn toàn với từ đó.
  2. **Toàn bộ 20 câu Tìm lỗi sai:** Chuyển tất cả 4 vị trí gạch dưới thành định dạng đóng mở ngoặc vuông `[...]` không kèm nhãn `(A)/(B)/(C)/(D)`. Bốn phương án `optionA..optionD` hiển thị trực tiếp 4 cụm từ trong ngoặc vuông. Người học chọn cụm từ sai mà không lo nhãn ký hiệu trong câu bị đảo lộn.
  3. **Kiểm tra tự động:** Quét regex toàn bộ 200 câu Tiếng Anh: **0 lỗi** lệch nhãn.

---

### F. Ngữ văn — Đồng bộ Ma trận với Chapter (`P1 — LITERATURE_MATRIX`)
* **Vấn đề Codex phát hiện (P1):** Cả 10 dòng của `matrix.csv` Ngữ văn ghi tên bài học từ bản phác thảo cũ (ví dụ: *Bài 1: Khát vọng tự do (Truyện truyền kì)*), trong khi tệp dữ liệu thực tế `questions-import.csv` ghi *Bài 1: Khả năng lớn lao của tiểu thuyết*.
* **Khắc phục:**
  - Đồng bộ hóa định nghĩa `MATRIX` trong mã nguồn builder và biên dịch lại `Ngu-van-12/matrix.csv`.
  - Cả 10 bài học trong ma trận khớp chính xác 100% với mục lục SGK Ngữ văn 12 Kết nối tri thức với cuộc sống (Tập 1 & Tập 2) và 200 câu hỏi trong `questions-import.csv`:
    1. `LESSON01`: Bài 1: Khả năng lớn lao của tiểu thuyết (20 câu: 8 EASY, 8 MEDIUM, 4 HARD)
    2. `LESSON02`: Bài 2: Những thế giới thơ (20 câu: 8 EASY, 8 MEDIUM, 4 HARD)
    3. `LESSON03`: Bài 3: Lập luận trong văn bản nghị luận (20 câu: 8 EASY, 8 MEDIUM, 4 HARD)
    4. `LESSON04`: Bài 4: Yếu tố kỳ ảo trong truyện kể (20 câu: 8 EASY, 8 MEDIUM, 4 HARD)
    5. `LESSON05`: Bài 5: Tiếng cười của hài kịch (20 câu: 8 EASY, 8 MEDIUM, 4 HARD)
    6. `LESSON06`: Bài 6: Hồ Chí Minh - Văn hóa và con người (20 câu: 8 EASY, 8 MEDIUM, 4 HARD)
    7. `LESSON07`: Bài 7: Sự thật trong tác phẩm kí và hồi kí (20 câu: 8 EASY, 8 MEDIUM, 4 HARD)
    8. `LESSON08`: Bài 8: Đọc văn bản thông tin (20 câu: 8 EASY, 8 MEDIUM, 4 HARD)
    9. `LESSON09`: Bài 9: Văn học và đời sống (20 câu: 8 EASY, 8 MEDIUM, 4 HARD)
    10. `LESSON10`: Bài 10: Nghệ thuật sân khấu và Tiếng Việt thực hành (20 câu: 8 EASY, 8 MEDIUM, 4 HARD)
  - Đã đối chiếu kiểm tra 1-1: Mỗi bài học có đúng 20 câu hỏi khớp tên bài trong CSV.

---

### G. Đồng bộ và sửa mô tả trong nhật ký sửa chữa (`audit-resolution.csv`)
* **Vấn đề Codex phát hiện (P2):** Nhật ký ghi sai mô tả thực tế của các câu Toán:
  - Q047: nhật ký ghi hàm số hoặc tiệm cận khác với câu hỏi thực tế.
  - Q069: nhật ký ghi "hình chiếu", trong khi đề thực tế là tìm điểm $M \in Oz$ cách đều hai điểm $A, B$.
  - Q160: nhật ký ghi "hình chiếu lên mặt phẳng", trong khi đề thực tế là tìm giao điểm của đường thẳng và mặt phẳng.
  - Q200: nhật ký ghi "tung hai đồng xu", trong khi đề thực tế là bài toán xác suất có điều kiện Bayes (tung một đồng xu, ngửa bốc hộp A, sấp bốc hộp B).
* **Khắc phục:**
  - Tái tạo lại toàn bộ tệp `audit-resolution.csv` gồm 17 mục giải quyết đã được kiểm chứng độc lập.
  - Mô tả chính xác từng câu:
    - **Q047:** Đồ thị $y = \frac{ax+b}{cx+d}$ ($c=1$) có TCĐ $x=1$, TCN $y=2$ và qua $A(2; 5)$. Nghiệm đúng $a=2, b=1, c=1, d=-1 \Rightarrow T = a+b+c+d = 3$.
    - **Q069:** Tìm $M(0; 0; z) \in Oz$ sao cho $MA = MB$ với $A(2; 3; 4), B(1; 2; 3)$. Nghiệm đúng $z = 15/2 \Rightarrow M(0; 0; 15/2)$.
    - **Q160:** Giao điểm của đường thẳng $d: \frac{x-1}{2} = \frac{y+1}{1} = \frac{z}{2}$ và mặt phẳng $(P): x + y + z - 6 = 0$. Nghiệm đúng $(17/5; 1/5; 12/5)$.
    - **Q200:** Tung đồng xu cân đối đồng chất ($P(N)=P(S)=0,5$), ngửa bốc hộp A (3 trúng/2 trượt), sấp bốc hộp B (2 trúng/8 trượt). Biết bốc trúng vé, xác suất đồng xu ngửa: $P(N|Trúng) = \frac{0,5 \cdot 0,6}{0,5 \cdot 0,6 + 0,5 \cdot 0,2} = \frac{0,3}{0,4} = \frac{3}{4}$.
    - Bổ sung đầy đủ các mục vòng 2: `AIM_MATH12_BANK01_Q175`, `AIM_ENG12_BANK01_Q021`, `AIM_ENG12_BANK01_Q141`, `AIM_ENG12_BANK01_Q149`, `AIM_ENG12_BANK01_EMBEDDED_LABELS`, `AIM_LIT12_BANK01_MATRIX`.

---

## 3. Kết quả kiểm tra xác minh kỹ thuật & parser backend

Chạy công cụ kiểm thử chuẩn backend `LibraryService.parse_import_file` trên môi trường Python thực tế:
- **Tổng số câu đã xác minh:** 1.800 / 1.800 câu hợp lệ 100%.
- **Tổng số lỗi parser:** 0 lỗi.
- **Tính phản chu kỳ (Anti-cyclic):** Không tồn tại chu kỳ lặp tuần hoàn ở bất kỳ chu kỳ nào từ $1 \le p \le 20$.
- **Độ dài chuỗi cùng đáp án dài nhất:** Đúng $\le 3$ trên toàn bộ 9 môn.
- **Rà soát từ khóa bản nháp:** Đạt 0 từ khóa sót lại (`"chỉnh số liệu"`, `"khớp đáp án"`, `"chỉnh hệ số"`, `"bản nháp"`, `"lời giải cũ"`, `"đáp án cũ"`, `"Cần xét rõ"`).
- **Mã băm SHA-256:** Tệp `manifest.json` đã được tái tạo và đồng bộ mã băm SHA-256 mới nhất của tất cả 9 thư mục môn học và tệp `audit-resolution.csv`.

---

## 4. Cam kết tuân thủ quy trình & Bàn giao Codex

1. Toàn bộ ngân hàng tiếp tục được duy trì ở trạng thái **`DRAFT`**.
2. Không thực hiện import vào database Supabase hay SQLite đang chạy của ứng dụng.
3. Không xuất bản (publish) lên giao diện ứng dụng di động hay Web Admin.
4. Không thực hiện commit hay push lên Git repository (giữ nguyên nhánh làm việc hiện tại).
5. Các hồ sơ kiểm định độc lập của Codex (`AUDIT-CODEX.md`, `AUDIT-CODEX.json`, `AUDIT-CODEX-ROUND2.md`, `AUDIT-CODEX-ROUND2.json`) và thư mục sao lưu gốc `Lop-12-v1-truoc-sua-20261106` được bảo toàn nguyên vẹn làm bằng chứng pháp lý và học thuật.
6. Hồ sơ sẵn sàng bàn giao cho Codex thực hiện phúc tra độc lập vòng 3.
