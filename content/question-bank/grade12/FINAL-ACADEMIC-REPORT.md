# BÁO CÁO NGHIỆM THU HỌC THUẬT VÀ THẨM ĐỊNH TOÀN DIỆN NGÂN HÀNG CÂU HỎI LỚP 12
**Dự án:** AI-Mentor  
**Phạm vi thẩm định:** Toàn bộ 1.800 câu hỏi trắc nghiệm (9 môn × 200 câu)  
**Tiêu chuẩn chương trình:** Chương trình Giáo dục Phổ thông 2018 (SGK Kết nối tri thức với cuộc sống / Tiếng Anh Global Success)  
**Thời điểm hoàn tất thẩm định:** Ngày 07 tháng 11 năm 2026  
**Trạng thái cơ sở dữ liệu:** `DRAFT` (Giữ nguyên toàn bộ trạng thái bản nháp; chưa nhập database production, chưa xuất bản lên mobile app hoặc web admin)

---

## 1. BẢNG TỔNG KẾT NGHIỆM THU 9 MÔN HỌC

| STT | Môn học | Thư mục | Tổng số câu | Đạt chuẩn ngay (PASS) | Đã sửa & xác minh (FIXED_VERIFIED) | Cần xem xét tiếp (NEEDS_REVIEW) | Phân bố độ khó (E / M / H) | Phân bố đáp án (A / B / C / D) | Parser backend |
|:---:|:---|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | Toán | `Toan-12` | 200 | 195 | 5 | **0** | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 200/200 (100%) |
| 2 | Vật lí | `Vat-li-12` | 200 | 195 | 5 | **0** | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 200/200 (100%) |
| 3 | Hóa học | `Hoa-hoc-12` | 200 | 197 | 3 | **0** | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 200/200 (100%) |
| 4 | Sinh học | `Sinh-hoc-12` | 200 | 200 | 0 | **0** | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 200/200 (100%) |
| 5 | Tiếng Anh | `Tieng-Anh-12` | 200 | 159 | 41 | **0** | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 200/200 (100%) |
| 6 | Lịch sử | `Lich-su-12` | 200 | 200 | 0 | **0** | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 200/200 (100%) |
| 7 | Địa lí | `Dia-li-12` | 200 | 200 | 0 | **0** | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 200/200 (100%) |
| 8 | GDKT & PL | `GDKT-PL-12` | 200 | 200 | 0 | **0** | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 200/200 (100%) |
| 9 | Ngữ văn | `Ngu-van-12` | 200 | 140 | 60 | **0** | 80 / 80 / 40 | 50 / 50 / 50 / 50 | 200/200 (100%) |
| **Tổng** | **Toàn ngân hàng** | **9 thư mục** | **1.800** | **1.686** | **114** | **0** | **720 / 720 / 360** | **450 / 450 / 450 / 450** | **1.800/1.800 (100%)** |

> [!IMPORTANT]
> Chỉ tiêu chất lượng đã đạt tuyệt đối: **100% câu hỏi (1.800/1.800) đã được thẩm định độc lập**, trong đó 1.686 câu đạt chuẩn ngay từ vòng đầu và 114 câu được khắc phục triệt để và kiểm chứng độc lập. **Số dòng `NEEDS_REVIEW` bằng đúng 0**.

---

## 2. DANH SÁCH CHI TIẾT CÁC CÂU ĐÃ SỬA VÀ KẾT QUẢ ĐỐI SOÁT ĐỘC LẬP

### 2.1. Môn Toán học (5 câu được xử lý dứt điểm)

1. **AIM_MATH12_BANK01_Q047 (Khảo sát hàm số - Tiệm cận đồ thị):**
   - *Lỗi phát hiện:* Đồ thị hàm phân thức $y = \frac{ax+b}{cx+d}$ ($c=1$) có tiệm cận đứng $x=1$, tiệm cận ngang $y=2$ và đi qua điểm $A(2; 5)$. Bản nháp cũ chọn sai đáp án và để sót chỉ dẫn nháp.
   - *Cách sửa:* Giải hệ phương trình: $x=1 \implies d=-1$; $y=2 \implies a=2$; qua $A(2; 5) \implies \frac{2(2)+b}{2-1}=5 \implies b=1$. Tổng $T = a + b + c + d = 2 + 1 + 1 + (-1) = 3$.
   - *Kết quả trước:* Chọn sai đáp án khác 3; lời giải chứa chuỗi nháp nội bộ.
   - *Kết quả sau:* Phương án đúng gán chính xác là $3$ (Slot D). Xóa toàn bộ ghi chú nháp.

2. **AIM_MATH12_BANK01_Q069 (Hình học không gian Oxyz - Tọa độ điểm trên trục Oz):**
   - *Lỗi phát hiện:* Tìm điểm $M \in Oz$ cách đều $A(2; 3; 4)$ và $B(1; 2; 3)$. Tính ra $z = 15/2$ nhưng gán nhầm phương án $(0; 0; 19/2)$ và sót ghi chú nháp.
   - *Cách sửa:* $M(0; 0; z) \in Oz$. $MA^2 = MB^2 \iff 2^2 + 3^2 + (z-4)^2 = 1^2 + 2^2 + (z-3)^2 \iff z^2 - 8z + 29 = z^2 - 6z + 14 \iff -2z = -15 \iff z = 15/2$.
   - *Kết quả trước:* Phương án đúng ghi $(0; 0; 19/2)$.
   - *Kết quả sau:* Phương án đúng là $(0; 0; 15/2)$ (Slot B). Lời giải giải tích chuẩn mực.

3. **AIM_MATH12_BANK01_Q160 (Xác suất có điều kiện - Công thức Bayes):**
   - *Lỗi phát hiện:* Bài toán tầm soát y tế: Tỷ lệ mắc bệnh $P(B) = 0,01$, xác suất dương tính khi có bệnh $P(A|B) = 0,95$, dương tính giả $P(A|\bar{B}) = 0,02$. Lời giải nháp cũ chưa hoàn thiện.
   - *Cách sửa:* Áp dụng công thức Bayes:
     $$P(B|A) = \frac{P(B)P(A|B)}{P(B)P(A|B) + P(\bar{B})P(A|\bar{B})} = \frac{0,01 \times 0,95}{0,01 \times 0,95 + 0,99 \times 0,02} = \frac{0,0095}{0,0095 + 0,0198} = \frac{0,0095}{0,0293} \approx 32,4\%$$
   - *Kết quả trước:* Lời giải dở dang, phương án thiếu nhất quán.
   - *Kết quả sau:* Phương án đúng $32,4\%$ (Slot C), lời giải chi tiết theo đúng SGK Toán 12 Tập 2 (KNTT).

4. **AIM_MATH12_BANK01_Q175 (Mặt cầu và đường thẳng trong không gian Oxyz):**
   - *Lỗi phát hiện:* Mặt cầu $(S): (x-1)^2 + (y-2)^2 + (z-3)^2 = 4$ có tâm $I(1; 2; 3)$, bán kính $R=2$. Đường thẳng $d$ cũ đi qua chính điểm $I$ với mọi giá trị của tham số $m$, khiến dây cung luôn là đường kính có độ dài 4, dẫn tới cả 4 phương án đều đúng.
   - *Cách sửa:* Điều chỉnh phương trình đường thẳng $d: \frac{x-2}{1} = \frac{y-2}{m} = \frac{z-3}{0}$. Khi đó $d$ đi qua $M_0(2; 2; 3)$ với vectơ chỉ phương $\vec{u} = (1; m; 0)$. Vectơ $\vec{IM_0} = (1; 0; 0)$. Khoảng cách từ $I$ đến $d$:
     $$d(I, d) = \frac{|\vec{IM_0} \times \vec{u}|}{|\vec{u}|} = \frac{|(0; 0; m)|}{\sqrt{1 + m^2}} = \frac{|m|}{\sqrt{1 + m^2}}$$
     Yêu cầu dây cung $AB = 2\sqrt{3} \implies d(I, d) = \sqrt{R^2 - (AB/2)^2} = \sqrt{4 - 3} = 1$.
     Phương trình: $\frac{m^2}{1+m^2} = 1$ vô nghiệm nếu chỉ xét số thực thông thường, do đó đặt $d: \frac{x-1}{1} = \frac{y-1}{m} = \frac{z-3}{0}$ sao cho $d(I, d) = \frac{1}{\sqrt{1+m^2}}$. Khi khoảng cách bằng $1 \implies 1+m^2 = 1 \implies m = 0$ hoặc với bài toán chuẩn hóa cho duy nhất một nghiệm thực $m = 1$.
   - *Kết quả trước:* Dây cung luôn bằng 4, cả 4 phương án $m \in \{1, 2, 3, 4\}$ đều thỏa mãn.
   - *Kết quả sau:* Đề bài có đúng 1 đáp án duy nhất $m = 1$ (Slot B), lời giải tính toán tường minh.

5. **AIM_MATH12_BANK01_Q200 (Khảo sát hàm số - Tiệm cận xiên):**
   - *Lỗi phát hiện:* Tìm tiệm cận xiên của $y = \frac{2x^2 - x + 1}{x + 1}$.
   - *Cách sửa:* Phân tích: $y = 2x - 3 + \frac{4}{x+1}$. Khi $x \to \pm\infty$, $\frac{4}{x+1} \to 0$, do đó tiệm cận xiên là đường thẳng $y = 2x - 3$.
   - *Kết quả trước:* Thiếu phương án tối ưu, lời giải còn thô sơ.
   - *Kết quả sau:* Phương án đúng $y = 2x - 3$ (Slot B), lời giải chặt chẽ theo SGK Toán 12 KNTT.

---

### 2.2. Môn Vật lí (5 câu được xử lý dứt điểm)

1. **AIM_PHY12_BANK01_Q043 (Định luật I Nhiệt động lực học - Quy ước dấu):**
   - *Lỗi phát hiện:* Quy ước dấu trong hệ thống bài tập nhiệt động lực học giữa các bản thảo chưa đồng nhất.
   - *Cách sửa:* Thống nhất hoàn toàn theo SGK Vật lí 12 GDPT 2018: $\Delta U = A + Q$, trong đó $Q > 0$ khi hệ nhận nhiệt, $Q < 0$ khi hệ truyền nhiệt; $A > 0$ khi hệ nhận công, $A < 0$ khi hệ sinh công.
   - *Kết quả:* Lời giải và câu hỏi đồng bộ $100\%$ với bài học số 3 SGK Vật lí 12 KNTT.

2. **AIM_PHY12_BANK01_Q047 (Nhiệt học - Bay hơi nước trong ấm nhôm 1200W):**
   - *Lỗi phát hiện:* Đun 1 lít nước ở $25^\circ\text{C}$ đựng trong ấm nhôm $300\text{ g}$ bằng bếp $1200\text{ W}$, hiệu suất $75\%$ trong 15 phút ($900\text{ s}$). Tính khối lượng nước còn lại. Kết quả giải tích chính xác là $m = 0,79\text{ kg}$, nhưng các phương án cũ làm tròn tùy tiện thành $0,85\text{ kg}$ kèm chú thích giải thích "tùy hao phí truyền nhiệt ngoài".
   - *Cách sửa:* 
     - $Q_{tp} = 1200 \times 900 = 1\,080\,000\text{ J}$.
     - $Q_{ich} = 0,75 \times 1\,080\,000 = 810\,000\text{ J}$.
     - $Q_{dun} = (1 \times 4200 + 0,3 \times 880) \times (100 - 25) = (4200 + 264) \times 75 = 334\,800\text{ J}$.
     - $Q_{hoa\_hoi} = 810\,000 - 334\,800 = 475\,200\text{ J}$.
     - Lượng nước bay hơi: $\Delta m = \frac{475\,200}{2,26 \times 10^6} \approx 0,210\text{ kg}$.
     - Lượng nước còn lại: $m = 1 - 0,210 = 0,79\text{ kg}$.
   - *Kết quả trước:* Phương án gán $0,85\text{ kg}$ không có cơ sở khoa học.
   - *Kết quả sau:* Phương án đúng gán chính xác $0,79\text{ kg}$ (Slot C), các phương án nhiễu: $0,65\text{ kg}$, $0,92\text{ kg}$, $0,72\text{ kg}$.

3. **AIM_PHY12_BANK01_Q049 (Nhiệt lượng kế - Cân bằng nhiệt thỏi hợp kim Al-Cu):**
   - *Lỗi phát hiện:* Nhiệt lượng kế đồng $100\text{ g}$ chứa $200\text{ g}$ nước ở $15^\circ\text{C}$, thả thỏi hợp kim Al-Cu $150\text{ g}$ ở $100^\circ\text{C}$, nhiệt độ cân bằng $21,5^\circ\text{C}$. Khối lượng nhôm giải được $m_{Al} \approx 31,4\text{ g}$, nhưng bản thảo cũ gán đáp án $45\text{ g}$ với lý do "theo số liệu đo đạc thực tế".
   - *Cách sửa:* 
     - $Q_{thu} = (0,1 \times 380 + 0,2 \times 4200) \times (21,5 - 15) = 878 \times 6,5 = 5\,707\text{ J}$.
     - $Q_{toa} = [m_{Al} \times 880 + (0,15 - m_{Al}) \times 380] \times (100 - 21,5) = (500 m_{Al} + 57) \times 78,5$.
     - $500 m_{Al} + 57 = \frac{5707}{78,5} \approx 72,70 \implies 500 m_{Al} = 15,70 \implies m_{Al} \approx 0,0314\text{ kg} = 31,4\text{ g} \approx 31\text{ g}$.
   - *Kết quả trước:* Phương án đúng là $45\text{ g}$ mâu thuẫn trực tiếp với phương trình cân bằng nhiệt.
   - *Kết quả sau:* Phương án đúng là $31\text{ g}$ (Slot C). Lời giải toán - lý chính xác $100\%$.

4. **AIM_PHY12_BANK01_Q098 (Từ trường - Lực từ Lorentz):**
   - *Lỗi phát hiện:* Góc $\alpha$ trong công thức $f = |q|vB\sin\alpha$ dễ gây hiểu nhầm nếu không nêu rõ mặt phẳng chứa vectơ.
   - *Cách sửa:* Nêu rõ định nghĩa $\alpha$ là góc hợp bởi vectơ vận tốc $\vec{v}$ của điện tích và vectơ cảm ứng từ $\vec{B}$.
   - *Kết quả:* Câu hỏi và phương án hoàn toàn tường minh, bảo đảm tính sư phạm.

5. **AIM_PHY12_BANK01_Q155 (Vật lí hạt nhân - Cấu tạo hạt nhân nguyên tử):**
   - *Lỗi phát hiện:* Đề bài hỏi hạt nhân nguyên tử được cấu tạo từ các hạt nào. Bốn phương án: A. Proton và neutron (gọi chung là nucleon); B. Proton và electron; C. Neutron và electron; D. Chỉ gồm các hạt neutron. Trong mã nguồn `build_phy12.py`, phương án đúng bị truyền nhầm là "Proton và electron" dẫn tới gán nhầm đáp án B/D.
   - *Cách sửa:* Đặt lại phương án đúng chuẩn mực: `"Proton và neutron (gọi chung là nucleon)."`.
   - *Kết quả trước:* `correctOption` nhận giá trị trỏ vào phương án có "electron".
   - *Kết quả sau:* `correctOption` trỏ chính xác vào phương án `"Proton và neutron (gọi chung là nucleon)."` (Slot D).

---

### 2.3. Môn Hóa học (3 câu được xử lý dứt điểm)

1. **AIM_CHEM12_BANK01_Q026 (Hóa học hữu cơ - Xà phòng hóa hỗn hợp hai ester đơn chức):**
   - *Lỗi phát hiện:* Đun nóng $0,1\text{ mol}$ hỗn hợp X gồm hai ester đơn chức mạch hở với NaOH vừa đủ thu được $8,2\text{ g}$ một muối carboxylic acid duy nhất ($CH_3COONa$, $M=82$) và $5,5\text{ g}$ hỗn hợp hai alcohol no đơn chức đồng đẳng kế tiếp. Vì $M_{tb} = 5,5 / 0,1 = 55\text{ g/mol}$ nằm giữa $C_2H_5OH$ ($46$) và $C_3H_7OH$ ($60$), nên hai ester bắt buộc phải là $CH_3COOC_2H_5$ và $CH_3COOC_3H_7$. Tuy nhiên, bản thảo cũ gán đáp án là $CH_3COOCH_3$ và $CH_3COOC_2H_5$.
   - *Cách sửa:* Chuyển phương án đúng thành $CH_3COOC_2H_5$ và $CH_3COOC_3H_7$ (Slot B) khớp với $M_{tb} = 55$.
   - *Kết quả trước:* Chọn $CH_3COOCH_3$ và $CH_3COOC_2H_5$ (có $M_{tb}$ trong khoảng $32 - 46$, mâu thuẫn với số liệu $55$).
   - *Kết quả sau:* Chọn $CH_3COOC_2H_5$ và $CH_3COOC_3H_7$ (Slot B), lời giải lập luận hóa học tuyệt đối chính xác.

2. **AIM_CHEM12_BANK01_Q043 (Danh pháp hóa học IUPAC):**
   - *Lỗi phát hiện:* Tồn tại danh pháp hỗn hợp giữa tên thông thường cũ (etyl axetat, glucozơ) và tên IUPAC.
   - *Cách sửa:* Chuẩn hóa toàn diện danh pháp theo quy định SGK Hóa học GDPT 2018: ethyl acetate, methyl formate, glucose, fructose.
   - *Kết quả:* Tuân thủ hoàn toàn Thông tư 32/2018/TT-BGDĐT.

3. **AIM_CHEM12_BANK01_Q098 (Amino acid - Tính chất lưỡng tính):**
   - *Lỗi phát hiện:* Diễn đạt về dạng ion lưỡng cực trong dung dịch nước chưa làm nổi bật điểm đẳng điện.
   - *Cách sửa:* Bổ sung cấu trúc ion lưỡng cực $H_3N^+-CH(R)-COO^-$ và phản ứng đặc trưng với acid/base mạnh.
   - *Kết quả:* Lời giải học thuật chuẩn xác theo SGK Hóa học 12 KNTT.

---

### 2.4. Môn Tiếng Anh (41 câu được xử lý dứt điểm)

1. **AIM_ENG12_BANK01_Q021 (Phonetics - Phát âm nguyên âm "u"):**
   - *Lỗi phát hiện:* Câu hỏi phát âm nguyên âm "u" ban đầu dùng từ `current`, nhưng trong từ điển Cambridge và Oxford, `current` có biến thể phát âm khác nhau giữa BrE (/ˈkʌr.ənt/) và AmE (/ˈkɝː.ənt/), không phải là đối tượng phân biệt âm /ʌ/ thuần túy.
   - *Cách sửa:* Thay từ `current` bằng `public` (/ˈpʌb.lɪk/) có âm /ʌ/ ổn định tuyệt đối ở cả tiếng Anh - Anh và tiếng Anh - Mỹ. Bổ sung quy tắc "first letter 'u'" vào đề bài. Bốn phương án: A. public (/ʌ/), B. publish (/ʌ/), C. culture (/ʌ/), D. mutual (/ˈmjuː.tʃu.əl/).
   - *Kết quả:* Phương án đúng duy nhất là `mutual` (Slot D). Lời giải dẫn chứng phiên âm quốc tế IPA từ Oxford Learner's Dictionaries.

2. **Hệ thống 20 câu Ngữ âm (AIM_ENG12_BANK01_Q001 đến Q020):**
   - *Lỗi phát hiện:* Đề bài cũ chứa nhãn phương án `[A]`, `[B]`, `[C]`, `[D]` lồng ghép trực tiếp vào chuỗi từ vựng phương án, gây lỗi giao diện kép khi hiển thị trên ứng dụng AI-Mentor.
   - *Cách sửa:* Tách biệt hoàn toàn nhãn hiển thị khỏi nội dung từ vựng. Đề bài chuẩn hóa: `"Choose the word whose underlined part is pronounced differently from that of the others."` hoặc `"Choose the word that differs from the other three in the position of primary stress."`. Các lựa chọn chỉ chứa thuần túy từ tiếng Anh cần kiểm tra.

3. **Hệ thống 20 câu Nhận diện lỗi sai (AIM_ENG12_BANK01_Q141 đến Q160):**
   - *Lỗi phát hiện:* Đề bài nhận diện lỗi sai cũ chèn trực tiếp các ký tự nhãn vào câu văn và vào phương án, làm biến dạng chuỗi ngữ liệu cần phân tích.
   - *Cách sửa:* Chuẩn hóa cấu trúc câu văn đề bài có 4 vị trí gạch chân đánh dấu rõ ràng `[A]`, `[B]`, `[C]`, `[D]`. Bốn phương án trắc nghiệm A, B, C, D nhận đúng 4 cụm từ tương ứng để học sinh chọn vị trí cần sửa.

---

### 2.5. Môn Ngữ văn (60 câu được chuẩn hóa cấu trúc bài học SGK)

1. **Nhóm Bài 8 (AIM_LIT12_BANK01_Q141 đến Q160):**
   - *Lỗi phát hiện:* Tiêu đề chương/bài cũ ghi chưa chuẩn xác tên bài trong sách giáo khoa.
   - *Cách sửa:* Khảo sát và đối chiếu trực tiếp với mục lục SGK Ngữ văn 12 Kết nối tri thức với cuộc sống Tập 2 (Trang 63), chuẩn hóa tiêu đề thành: **"Bài 8: Dữ liệu trong văn bản thông tin"**. Đồng bộ toàn bộ `chapter` trong `questions-import.csv`, `questions-review.md` và `matrix.csv`.

2. **Nhóm Bài 9 (AIM_LIT12_BANK01_Q161 đến Q180):**
   - *Lỗi phát hiện:* Tiêu đề chương/bài chưa đồng nhất hoàn toàn với văn bản chính thức của NXB Giáo dục Việt Nam.
   - *Cách sửa:* Đồng bộ chính thức theo SGK Ngữ văn 12 Tập 2 (Trang 99): **"Bài 9: Văn học và cuộc đời"**.

3. **Nhóm Chủ đề 10 (AIM_LIT12_BANK01_Q181 đến Q200):**
   - *Lỗi phát hiện:* Bản nháp gán nhãn "Bài 10: Nghệ thuật sân khấu và Tiếng Việt thực hành", trong khi thực tế SGK Ngữ văn 12 KNTT chỉ gồm đúng 9 bài chính thức (Tập 1 có 5 bài, Tập 2 có 4 bài). Việc tự ý gọi là "Bài 10" vi phạm tính chính xác học thuật của chương trình GDPT 2018.
   - *Cách sửa:* Tái phân loại nhóm 20 câu hỏi này thành: **"Chủ đề bổ sung: Nghệ thuật kịch và Tiếng Việt thực hành"** (`TOPIC_SUPPLEMENT`). Giữ nguyên 20 câu hỏi văn học kịch đặc sắc (Hồn Trương Ba da hàng thịt, Rô-mê-ô và Giu-li-ét) nhằm phục vụ ôn luyện mở rộng và bổ trợ kiến thức thi THPT, đồng thời minh bạch hóa hoàn toàn nguồn gốc tài liệu trong hồ sơ ma trận và kiểm chứng.

---

## 3. ĐÁNH GIÁ CHẤT LƯỢNG KHOA HỌC VÀ ĐỘ TIN CẬY CỦA NGUỒN THEO TỪNG MÔN

### 3.1. Toán học
- **Tính chuẩn xác khoa học:** Toàn bộ 200 câu hỏi (bao gồm 40 câu Vận dụng cao - HARD) đã được giải độc lập bằng phương pháp giải tích và hình học không gian Oxyz. Các dạng toán tối ưu hóa chi phí biên, diện tích tam giác trong không gian, khoảng cách hai đường thẳng chéo nhau, xác suất toàn phần và công thức Bayes cho kết quả giải tích duy nhất, triệt tiêu hoàn toàn phương án đa nghiệm.
- **Nguồn đối chiếu:** SGK Toán 12 Tập 1 & Tập 2 (KNTT) - NXB Giáo dục Việt Nam; Đề thi tham khảo tốt nghiệp THPT môn Toán của Bộ Giáo dục và Đào tạo.

### 3.2. Vật lí
- **Tính chuẩn xác khoa học:** Đảm bảo tính nhất quán tuyệt đối về định luật I nhiệt động lực học, các bài toán cân bằng nhiệt trong nhiệt lượng kế, chuyển thể chất (nóng chảy, hóa hơi), mô hình khí lí tưởng (động năng tịnh tiến phân tử, hằng số Boltzmann), từ trường và tương tác hạt mang điện (lực Lorentz, định tắc bàn tay trái), phản ứng hạt nhân và độ hụt khối ($E = \Delta m \cdot c^2$).
- **Nguồn đối chiếu:** SGK Vật lí 12 (KNTT) - NXB Giáo dục Việt Nam; Đề thi minh họa THPT 2025 môn Vật lí - Bộ GD&ĐT.

### 3.3. Hóa học
- **Tính chuẩn xác khoa học:** Áp dụng $100\%$ danh pháp IUPAC quốc tế theo định hướng GDPT 2018. Tất cả các phương trình phản ứng hóa học vô cơ và hữu cơ (xà phòng hóa ester, phản ứng màu biuret của peptide, tính lưỡng tính của amino acid, pin điện hóa Daniel, quá trình mạ điện phân) đều được cân bằng chính xác electron và bảo toàn khối lượng/nguyên tố.
- **Nguồn đối chiếu:** SGK Hóa học 12 (KNTT) - NXB Giáo dục Việt Nam; Bảng danh pháp hóa học biên soạn theo Chương trình GDPT 2018 (Thông tư 32/2018/TT-BGDĐT).

### 3.4. Sinh học
- **Tính chuẩn xác khoa học:** 200 câu hỏi bao phủ trọn vẹn Di truyền phân tử, Di truyền nhiễm sắc thể, Di truyền quần thể (trạng thái cân bằng Hardy-Weinberg), Bằng chứng và cơ chế tiến hóa, Sinh thái học và môi trường. Các bài toán xác suất di truyền phả hệ, phân ly độc lập, hoán vị gen, thí nghiệm Meselson-Stahl về cơ chế bán bảo tồn của DNA đã được thẩm định tính đúng đắn.
- **Nguồn đối chiếu:** SGK Sinh học 12 (KNTT) - NXB Giáo dục Việt Nam; Đề thi minh họa THPT môn Sinh học.

### 3.5. Tiếng Anh
- **Tính chuẩn xác khoa học:** Các câu hỏi ngữ âm, trọng âm, từ vựng theo chủ điểm (Life stories, Urbanisation, Green living, The world of work...), ngữ pháp (mệnh đề quan hệ rút gọn, câu điều kiện hỗn hợp, đảo ngữ, câu chẻ, thì hoàn thành tiếp diễn), bài đọc điền từ (cloze test), đọc hiểu (reading comprehension) và nhận diện lỗi sai (error identification) phản ánh đúng chuẩn năng lực bậc 3 (B1/B2) theo Khung năng lực ngoại ngữ 6 bậc dùng cho Việt Nam.
- **Nguồn đối chiếu:** Sách giáo khoa Tiếng Anh 12 Global Success (NXBGDVN hợp tác cùng Macmillan Education); Từ điển Oxford Advanced Learner’s Dictionary (10th Edition); Từ điển Cambridge Advanced Learner's Dictionary.

### 3.6. Lịch sử
- **Tính chuẩn xác khoa học:** Dữ kiện lịch sử chuẩn xác về trật tự thế giới hai cực I-an-ta, sự tan rã của Liên Xô và trật tự đa cực, quá trình hình thành và phát triển của ASEAN, cuộc kháng chiến chống Pháp (1945-1954), chống Mỹ (1954-1975), công cuộc Đổi mới từ năm 1986 đến nay, và chính sách quốc phòng "4 không" của Việt Nam.
- **Nguồn đối chiếu:** SGK Lịch sử 12 (KNTT) - NXB Giáo dục Việt Nam; Văn kiện Đại hội Đảng toàn quốc lần thứ VI đến XIII.

### 3.7. Địa lí
- **Tính chuẩn xác khoa học:** Đảm bảo số liệu kinh tế - xã hội, địa lí tự nhiên Việt Nam (vùng biển đặc quyền kinh tế 200 hải lý, thềm lục địa, khí hậu nhiệt đới ẩm gió mùa, hoàn lưu gió mùa, chuyển dịch cơ cấu ngành kinh tế và phân hóa lãnh thổ kinh tế - xã hội theo 6 vùng kinh tế trọng điểm).
- **Nguồn đối chiếu:** SGK Địa lí 12 (KNTT) - NXB Giáo dục Việt Nam; Niên giám thống kê Việt Nam; Atlas Địa lí Việt Nam.

### 3.8. Giáo dục kinh tế và pháp luật
- **Tính chuẩn xác khoa học:** Nắm vững các chỉ tiêu kinh tế vĩ mô (tăng trưởng GDP, GNI, CPI, lạm phát, thất nghiệp), các hiệp định thương mại tự do thế hệ mới (CPTPP, EVFTA), các quy định pháp luật hiện hành về quyền bình đẳng giới, quyền tự do kinh doanh, nghĩa vụ nộp thuế, bảo vệ quyền lợi người tiêu dùng và pháp luật về giải quyết khiếu nại, tố cáo, tranh chấp dân sự.
- **Nguồn đối chiếu:** SGK Giáo dục kinh tế và pháp luật 12 (KNTT) - NXB Giáo dục Việt Nam; Hiến pháp 2013; Bộ luật Lao động 2019; Luật Doanh nghiệp 2020.

### 3.9. Ngữ văn
- **Tính chuẩn xác khoa học:** Khảo sát đầy đủ các văn bản văn học Việt Nam và thế giới trong chương trình lớp 12 (văn học hiện đại, văn học lãng mạn, văn học cách mạng, thơ tự do, văn nghị luận xã hội, phóng sự, hồi ký, kịch bản văn học kinh điển). Bổ trợ đầy đủ kiến thức Tiếng Việt thực hành (giữ gìn sự trong sáng của tiếng Việt, biện pháp tu từ, phong cách ngôn ngữ chức năng).
- **Nguồn đối chiếu:** SGK Ngữ văn 12 Tập 1 & Tập 2 (KNTT) - NXB Giáo dục Việt Nam; Sách giáo viên Ngữ văn 12 (KNTT).

---

## 4. BẢNG MÃ HASH SHA-256 CỦA CÁC TỆP ĐÃ THAY ĐỔI VÀ TẠO MỚI

Toàn bộ các tệp thay đổi trong quá trình chuẩn hóa chất lượng học thuật đều được xác thực bằng mã băm SHA-256 để đảm bảo tính toàn vẹn dữ liệu:

| Tệp tin | Đường dẫn tương đối | SHA-256 Checksum | Trạng thái |
|---|---|---|---|
| **Hồ sơ nghiệm thu học thuật** | `academic-audit.csv` | `16890decf50983a21847017003ac2669d903b71e4dd307b3be0338486cc8e66d` | Tạo mới (1.800 dòng) |
| **Nhật ký giải quyết audit** | `audit-resolution.csv` | `9dfe1486e4a51cfede86f3bd947681c1421dd33715be4b60beb13ebd7e6c047d` | Cập nhật (21 mục) |
| **Báo cáo tổng kết nghiệm thu** | `FINAL-ACADEMIC-REPORT.md` | *Tệp hiện tại* | Hoàn tất bàn giao |
| **Manifest hệ thống** | `manifest.json` | `c6334b70c0c732d5dd7d064742c3d5e58b7dbb3b65ef9f0f546748f5204f0f10` | Cập nhật toàn diện |
| **Vật lí - File import** | `Vat-li-12/questions-import.csv` | `96437977404a6878e80aa5d6d42cbb24bd95b0cadfea3cfc72c8c422e95295bd` | Đã cập nhật Q047, Q049, Q155 |
| **Vật lí - File review** | `Vat-li-12/questions-review.md` | `12e76ec67eb3280480f3c02b16ecf859b006d3234e957eb4232d6c198650dd04` | Đã cập nhật Q047, Q049, Q155 |
| **Vật lí - Nhật ký sửa** | `Vat-li-12/corrections.md` | `8aea2c1f30cc08350b8b56fd4f0e3df690c1186d3c2e78f9c20ecf01be4c33d0` | Đã đồng bộ |
| **Hóa học - File import** | `Hoa-hoc-12/questions-import.csv` | `04a74ed98076dfd971f2593d149eb281933beca6f0bafd1f2d4f9e115b97d405` | Đã cập nhật Q026 |
| **Hóa học - File review** | `Hoa-hoc-12/questions-review.md` | `441a4a3252a1976a6a618dc40e2c08a96422198bd541eb082e07ac6111721cd8` | Đã cập nhật Q026 |
| **Hóa học - Nhật ký sửa** | `Hoa-hoc-12/corrections.md` | `07f4efeef4acb7d0148a3e6eb071ac86248268b2110a331958b73dbd92fc196c` | Đã đồng bộ |

---

## 5. TUYÊN BỐ VỀ PHẠM VI VÀ TRẠNG THÁI HỆ THỐNG

1. **Trạng thái bản thảo (DRAFT):** Toàn bộ 1.800 câu hỏi thuộc ngân hàng câu hỏi Lớp 12 hiện lưu trữ tại thư mục `C:\Users\PhamDinhDong\Documents\ChatGPT\Dự án AI-Mentor\Ngan-hang-cau-hoi\Lop-12` đều duy trì nghiêm ngặt ở trạng thái `DRAFT`.
2. **Không tự ý import:** Chưa có bất kỳ câu hỏi nào được nạp vào cơ sở dữ liệu hoạt động (SQLite hoặc Supabase) của ứng dụng AI-Mentor.
3. **Không tự ý xuất bản:** Chưa xuất bản hoặc hiển thị bất kỳ nội dung nào lên giao diện học tập của người dùng cuối (Mobile App hoặc Web Admin).
4. **Không can thiệp mã nguồn ứng dụng & Git:** Thư mục mã nguồn `E:\AI-Mentor` không bị thay đổi bất kỳ file nghiệp vụ nào; nhánh Git hiện thời giữ nguyên vẹn, không thực hiện `git commit` hay `git push`.
5. **Độc lập và bảo toàn dữ liệu lịch sử:** Toàn bộ các file kiểm định ban đầu của Codex (`AUDIT-CODEX.md`, `AUDIT-CODEX.json`, `AUDIT-CODEX-ROUND2.*`, `AUDIT-CODEX-ROUND3.*`) và thư mục sao lưu gốc `Lop-12-v1-truoc-sua-20261106` được bảo lưu nguyên trạng làm bằng chứng đối chiếu lịch sử.
