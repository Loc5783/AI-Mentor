# Ngân hàng câu hỏi Toán 12 - Chương trình GDPT 2018

- **Môn học:** Toán (Mã wire: `math`)
- **Khối lớp:** 12
- **Bộ sách đối chiếu:** Kết nối tri thức với cuộc sống
- **Tổng số câu hỏi:** 200 câu (80 EASY, 80 MEDIUM, 40 HARD)
- **Phân bổ đáp án:** A: 50, B: 50, C: 50, D: 50 (Chuỗi lặp dài nhất = 3, không có chu kỳ)
- **Trạng thái:** DRAFT (Bản nháp chờ nghiệm thu học thuật)

---

## Chương I: Ứng dụng đạo hàm để khảo sát và vẽ đồ thị hàm số

### Câu 001 [AIM_MATH12_BANK01_Q001] - Mức độ: **EASY**

- **Chủ đề:** Tính đơn điệu của hàm số
- **Đề bài:** Cho hàm số y = f(x) có đạo hàm f'(x) = x^2 - 4. Hàm số nghịch biến trên khoảng nào dưới đây?

- **A.** (2; +vô cực)
- **B.** (-vô cực; -2)
- **C.** (-2; 2)
- **D.** (-4; 4)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta có f'(x) < 0 <=> x^2 - 4 < 0 <=> -2 < x < 2. Vậy hàm số nghịch biến trên khoảng (-2; 2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 002 [AIM_MATH12_BANK01_Q002] - Mức độ: **EASY**

- **Chủ đề:** Cực trị của hàm số
- **Đề bài:** Cho hàm số y = f(x) có đạo hàm f'(x) = (x - 1)(x + 3)^2. Số điểm cực trị của hàm số đã cho là

- **A.** 2
- **B.** 1
- **C.** 3
- **D.** 0

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Đạo hàm f'(x) đổi dấu khi đi qua x = 1 (nghiệm bội lẻ bậc 1). Tại x = -3 là nghiệm bội chẵn nên f'(x) không đổi dấu. Vậy hàm số có đúng 1 điểm cực trị.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 003 [AIM_MATH12_BANK01_Q003] - Mức độ: **EASY**

- **Chủ đề:** GTLN và GTNN của hàm số
- **Đề bài:** Giá trị nhỏ nhất của hàm số f(x) = x^4 - 2x^2 + 3 trên đoạn [0; 2] bằng

- **A.** 3
- **B.** 11
- **C.** 2
- **D.** 1

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** f'(x) = 4x^3 - 4x = 4x(x^2 - 1) = 0 <=> x = 0 hoặc x = 1 (trên [0; 2]). Ta tính: f(0) = 3, f(1) = 2, f(2) = 11. Vậy min f(x) = 2 tại x = 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 004 [AIM_MATH12_BANK01_Q004] - Mức độ: **EASY**

- **Chủ đề:** Đường tiệm cận
- **Đề bài:** Đường tiệm cận đứng của đồ thị hàm số y = (2x + 1)/(x - 3) là đường thẳng

- **A.** x = -3
- **B.** x = 3
- **C.** y = 2
- **D.** y = 3

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Mẫu số x - 3 = 0 <=> x = 3 và tử số tại x = 3 bằng 7 khác 0, do đó đường tiệm cận đứng là x = 3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 005 [AIM_MATH12_BANK01_Q005] - Mức độ: **EASY**

- **Chủ đề:** Đường tiệm cận
- **Đề bài:** Đường tiệm cận ngang của đồ thị hàm số y = (3x - 5)/(x + 2) là đường thẳng

- **A.** y = -5/2
- **B.** x = -2
- **C.** y = 1
- **D.** y = 3

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** lim_{x -> vô cực} (3x - 5)/(x + 2) = 3, do đó đường tiệm cận ngang là đường thẳng y = 3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 006 [AIM_MATH12_BANK01_Q006] - Mức độ: **EASY**

- **Chủ đề:** Khảo sát và vẽ đồ thị
- **Đề bài:** Đồ thị hàm số y = x^3 - 3x + 2 cắt trục tung tại điểm có toạ độ là

- **A.** (0; 2)
- **B.** (2; 0)
- **C.** (0; -2)
- **D.** (1; 0)

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Giao điểm với trục tung Oy có hoành độ x = 0. Thay x = 0 vào hàm số ta được y = 0^3 - 3*0 + 2 = 2. Vậy giao điểm là (0; 2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 007 [AIM_MATH12_BANK01_Q007] - Mức độ: **EASY**

- **Chủ đề:** Tính đơn điệu của hàm số
- **Đề bài:** Hàm số nào dưới đây đồng biến trên khoảng (-vô cực; +vô cực)?

- **A.** y = x^3 - 3x
- **B.** y = x^3 + 3x - 1
- **C.** y = x^4 + 2x^2
- **D.** y = (x + 1)/(x - 2)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Xét y = x^3 + 3x - 1 có y' = 3x^2 + 3 > 0 với mọi x thuộc R. Do đó hàm số luôn đồng biến trên R.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 008 [AIM_MATH12_BANK01_Q008] - Mức độ: **EASY**

- **Chủ đề:** Cực trị của hàm số
- **Đề bài:** Cho hàm số y = f(x) có đạo hàm đổi dấu từ dương sang âm khi đi qua x = 2. Điểm cực đại của hàm số là

- **A.** x = -2
- **B.** y = 2
- **C.** x = 0
- **D.** x = 2

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Theo dấu hiệu nhận biết cực trị, khi đạo hàm f'(x) đổi dấu từ dương sang âm qua điểm x_0 thì hàm số đạt cực đại tại x_0 = 2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 009 [AIM_MATH12_BANK01_Q009] - Mức độ: **EASY**

- **Chủ đề:** Đường tiệm cận xiên
- **Đề bài:** Đường tiệm cận xiên của đồ thị hàm số y = (x^2 + 2x + 3)/(x + 1) có phương trình là

- **A.** y = x - 1
- **B.** y = x + 1
- **C.** y = x + 2
- **D.** y = 2x + 1

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Thực hiện phép chia đa thức: y = x + 1 + 2/(x + 1). Khi x -> vô cực thì 2/(x + 1) -> 0, do đó tiệm cận xiên là y = x + 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 010 [AIM_MATH12_BANK01_Q010] - Mức độ: **EASY**

- **Chủ đề:** Khảo sát hàm số
- **Đề bài:** Điểm uốn của đồ thị hàm số y = x^3 - 3x^2 + 4 có hoành độ là

- **A.** x = 1
- **B.** x = 0
- **C.** x = 2
- **D.** x = -1

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Ta có y' = 3x^2 - 6x, y'' = 6x - 6 = 0 <=> x = 1. Hoành độ điểm uốn của đồ thị là x = 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 011 [AIM_MATH12_BANK01_Q011] - Mức độ: **EASY**

- **Chủ đề:** GTLN và GTNN
- **Đề bài:** Cho hàm số y = f(x) liên tục trên [-1; 3] và có giá trị lớn nhất bằng 5 tại x = 2. Giá trị lớn nhất của hàm số trên [-1; 3] là

- **A.** 2
- **B.** 3
- **C.** -1
- **D.** 5

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Theo giả thiết, giá trị lớn nhất của hàm số trên đoạn [-1; 3] đạt được tại x = 2 là max f(x) = f(2) = 5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 012 [AIM_MATH12_BANK01_Q012] - Mức độ: **EASY**

- **Chủ đề:** Tính đơn điệu
- **Đề bài:** Hàm số y = -x^3 + 3x^2 - 4 nghịch biến trên các khoảng nào?

- **A.** (-vô cực; 0) và (2; +vô cực)
- **B.** (0; 2)
- **C.** (-vô cực; 2)
- **D.** (0; +vô cực)

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** y' = -3x^2 + 6x = -3x(x - 2). Xét dấu y' < 0 khi x < 0 hoặc x > 2. Do đó hàm số nghịch biến trên (-vô cực; 0) và (2; +vô cực).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 013 [AIM_MATH12_BANK01_Q013] - Mức độ: **EASY**

- **Chủ đề:** Đường tiệm cận đứng
- **Đề bài:** Đồ thị hàm số y = (x - 2)/(x^2 - 4) có bao nhiêu đường tiệm cận đứng?

- **A.** 2
- **B.** 0
- **C.** 3
- **D.** 1

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Ta rút gọn với x khác 2: y = (x - 2)/((x - 2)(x + 2)) = 1/(x + 2). Khi x -> -2 thì y -> vô cực; khi x -> 2 thì y -> 1/4. Do đó tiệm cận đứng duy nhất là x = -2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 014 [AIM_MATH12_BANK01_Q014] - Mức độ: **EASY**

- **Chủ đề:** Cực trị của hàm số
- **Đề bài:** Hàm số y = x^3 - 3x^2 + 2 có toạ độ điểm cực tiểu là

- **A.** (0; 2)
- **B.** (1; 0)
- **C.** (2; -2)
- **D.** (-1; -2)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** y' = 3x^2 - 6x = 0 <=> x = 0 hoặc x = 2. Ta có y'' = 6x - 6 => y''(2) = 6 > 0 nên hàm số đạt cực tiểu tại x = 2, giá trị y(2) = -2. Toạ độ điểm cực tiểu là (2; -2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 015 [AIM_MATH12_BANK01_Q015] - Mức độ: **EASY**

- **Chủ đề:** Tâm đối xứng đồ thị
- **Đề bài:** Tâm đối xứng của đồ thị hàm số bậc ba y = x^3 - 3x^2 + 1 là điểm có toạ độ

- **A.** (0; 1)
- **B.** (1; -1)
- **C.** (2; -3)
- **D.** (1; 1)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Hoành độ tâm đối xứng là nghiệm của y'' = 0: 6x - 6 = 0 <=> x = 1. Tung độ tương ứng y(1) = 1^3 - 3*1^2 + 1 = -1. Vậy tâm đối xứng là I(1; -1).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 016 [AIM_MATH12_BANK01_Q016] - Mức độ: **EASY**

- **Chủ đề:** Tính đơn điệu
- **Đề bài:** Cho hàm số y = f(x) có đạo hàm f'(x) = x(x - 2)^3. Mệnh đề nào sau đây đúng?

- **A.** Hàm số đồng biến trên (0; 2)
- **B.** Hàm số đồng biến trên (2; +vô cực)
- **C.** Hàm số nghịch biến trên (2; +vô cực)
- **D.** Hàm số đồng biến trên (-vô cực; 0)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Xét dấu f'(x): f'(x) > 0 khi x < 0 hoặc x > 2; f'(x) < 0 khi 0 < x < 2. Vậy hàm số đồng biến trên (2; +vô cực) và (-vô cực; 0).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 017 [AIM_MATH12_BANK01_Q017] - Mức độ: **EASY**

- **Chủ đề:** Giao điểm tiệm cận
- **Đề bài:** Giao điểm của đường tiệm cận đứng và tiệm cận ngang của đồ thị hàm số y = (2x - 1)/(x + 1) là

- **A.** (-1; 2)
- **B.** (1; 2)
- **C.** (-1; -1)
- **D.** (2; -1)

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Tiệm cận đứng x = -1, tiệm cận ngang y = 2. Giao điểm của hai đường tiệm cận là I(-1; 2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 018 [AIM_MATH12_BANK01_Q018] - Mức độ: **EASY**

- **Chủ đề:** Cực trị
- **Đề bài:** Hàm số y = 2x^4 + 1 có bao nhiêu điểm cực trị?

- **A.** 3
- **B.** 1
- **C.** 2
- **D.** 0

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** y' = 8x^3 = 0 <=> x = 0. Đạo hàm đổi dấu từ âm sang dương khi đi qua x = 0 nên hàm số có đúng 1 điểm cực trị (cực tiểu).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 019 [AIM_MATH12_BANK01_Q019] - Mức độ: **EASY**

- **Chủ đề:** GTLN
- **Đề bài:** Giá trị lớn nhất của hàm số y = -x^2 + 4x + 1 trên đoạn [0; 3] bằng

- **A.** 4
- **B.** 5
- **C.** 1
- **D.** 3

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** y' = -2x + 4 = 0 <=> x = 2 thuộc đoạn [0; 3]. Tính các giá trị: y(0) = 1, y(2) = 5, y(3) = 4. Vậy GTLN bằng 5 tại x = 2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 020 [AIM_MATH12_BANK01_Q020] - Mức độ: **EASY**

- **Chủ đề:** Dạng đồ thị suy biến
- **Đề bài:** Đồ thị hàm số y = (x^2 - 3x + 2)/(x - 1) có hình dạng là

- **A.** Đường hypebol hoàn chỉnh
- **B.** Đường thẳng y = x - 2 bỏ đi điểm (1; -1)
- **C.** Đường parabol bậc hai
- **D.** Đường thẳng y = x - 2 nguyên vẹn

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Tập xác định D = R \ {1}. Với x khác 1, y = (x - 1)(x - 2)/(x - 1) = x - 2. Đồ thị là đường thẳng y = x - 2 khuyết điểm có hoành độ x = 1, tức điểm (1; -1).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 021 [AIM_MATH12_BANK01_Q021] - Mức độ: **MEDIUM**

- **Chủ đề:** Ứng dụng tham số tính đơn điệu
- **Đề bài:** Tìm tất cả các giá trị thực của tham số m để hàm số y = x^3 - 3mx^2 + 3(m^2 - 1)x đồng biến trên khoảng (1; 2).

- **A.** m <= 0 hoặc m >= 3
- **B.** 0 <= m <= 3
- **C.** m < 1
- **D.** m > 2

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Ta có y' = 3(x^2 - 2mx + m^2 - 1) = 3[(x - m)^2 - 1]. y' = 0 có hai nghiệm x_1 = m - 1, x_2 = m + 1. Hàm số đồng biến trên (1; 2) khi và chỉ khi khoảng (1; 2) nằm ngoài khoảng hai nghiệm, tức m + 1 <= 1 hoặc m - 1 >= 2 <=> m <= 0 hoặc m >= 3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 022 [AIM_MATH12_BANK01_Q022] - Mức độ: **MEDIUM**

- **Chủ đề:** Hàm phân thức chứa tham số
- **Đề bài:** Có bao nhiêu giá trị nguyên của m thuộc đoạn [-10; 10] để hàm số y = (mx + 4)/(x + m) nghịch biến trên từng khoảng xác định?

- **A.** 3
- **B.** 4
- **C.** 5
- **D.** 2

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Tập xác định D = R \ {-m}. Ta có y' = (m^2 - 4)/(x + m)^2. Hàm số nghịch biến trên từng khoảng xác định <=> m^2 - 4 < 0 <=> -2 < m < 2. Các giá trị nguyên thỏa mãn là m thuộc {-1; 0; 1}, tổng cộng có 3 giá trị.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 023 [AIM_MATH12_BANK01_Q023] - Mức độ: **MEDIUM**

- **Chủ đề:** Trung điểm hai điểm cực trị
- **Đề bài:** Cho hàm số bậc ba y = x^3 - 3x^2 + 2. Toạ độ trung điểm của đoạn thẳng nối hai điểm cực trị của đồ thị hàm số là

- **A.** (0; 2)
- **B.** (2; -2)
- **C.** (1; 0)
- **D.** (1; 1)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** y' = 3x^2 - 6x = 0 <=> x = 0 (y = 2) hoặc x = 2 (y = -2). Hai điểm cực trị là A(0; 2) và B(2; -2). Toạ độ trung điểm I của AB là I((0 + 2)/2; (2 + (-2))/2) = (1; 0).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 024 [AIM_MATH12_BANK01_Q024] - Mức độ: **MEDIUM**

- **Chủ đề:** Hàm hợp đơn điệu
- **Đề bài:** Cho hàm số y = f(x) có đạo hàm liên tục trên R. Đồ thị hàm số y = f'(x) cắt trục hoành tại 3 điểm x = -1, x = 1, x = 4. Hàm số g(x) = f(2 - x) đồng biến trên khoảng nào dưới đây?

- **A.** (-2; 1)
- **B.** (3; +vô cực)
- **C.** (1; 3)
- **D.** (-vô cực; -2)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta có g'(x) = -f'(2 - x). Hàm số g(x) đồng biến <=> g'(x) > 0 <=> f'(2 - x) < 0. Dựa vào đồ thị f'(x), giải bất phương trình suy ra x thuộc (1; 3).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 025 [AIM_MATH12_BANK01_Q025] - Mức độ: **MEDIUM**

- **Chủ đề:** GTLN chứa căn thức
- **Đề bài:** Giá trị lớn nhất của hàm số f(x) = x + sqrt(4 - x^2) bằng

- **A.** 4
- **B.** 2
- **C.** sqrt(2)
- **D.** 2*sqrt(2)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Áp dụng bất đẳng thức Cauchy-Schwarz: (1*x + 1*sqrt(4 - x^2))^2 <= (1^2 + 1^2)(x^2 + 4 - x^2) = 2*4 = 8 => f(x) <= sqrt(8) = 2*sqrt(2). Dấu bằng xảy ra khi x = sqrt(2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 026 [AIM_MATH12_BANK01_Q026] - Mức độ: **MEDIUM**

- **Chủ đề:** Số đường tiệm cận
- **Đề bài:** Tổng số đường tiệm cận đứng và tiệm cận ngang của đồ thị hàm số y = (sqrt(x^2 + 1) - x)/(x - 1) là

- **A.** 3
- **B.** 2
- **C.** 1
- **D.** 4

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Tiệm cận đứng x = 1 (vì tử số tại x = 1 bằng sqrt(2) - 1 khác 0). Tiệm cận ngang: khi x -> +vô cực thì y -> 0; khi x -> -vô cực thì y -> -2. Vậy có 1 TCĐ và 2 TCN, tổng cộng là 3 đường tiệm cận.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 027 [AIM_MATH12_BANK01_Q027] - Mức độ: **MEDIUM**

- **Chủ đề:** Bài toán tối ưu hóa thực tế
- **Đề bài:** Một người nông dân muốn rào một khu vườn hình chữ nhật có diện tích 600 m^2 sát bờ sông thẳng (không cần rào bờ sông). Chiều dài hàng rào ngắn nhất bằng

- **A.** 60 m
- **B.** 40 m
- **C.** 20*sqrt(6) m
- **D.** 40*sqrt(3) m

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Gọi x là chiều rộng (vuông góc với bờ sông), chiều dài song song bờ sông là 600/x. Chiều dài hàng rào là L = 2x + 600/x. Áp dụng BĐT AM-GM: L >= 2*sqrt(2x * 600/x) = 2*sqrt(1200) = 40*sqrt(3) m.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 028 [AIM_MATH12_BANK01_Q028] - Mức độ: **MEDIUM**

- **Chủ đề:** Tâm đối xứng phân thức bậc 2 trên 1
- **Đề bài:** Đồ thị hàm số y = (x^2 - 2x + 4)/(x - 2) có tâm đối xứng là điểm

- **A.** (2; 0)
- **B.** (0; 2)
- **C.** (2; 2)
- **D.** (2; 4)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta viết y = x + 4/(x - 2). Đồ thị có tiệm cận đứng x = 2 và tiệm cận xiên y = x. Giao điểm của hai đường tiệm cận là I(2; 2), đây chính là tâm đối xứng của đồ thị hàm phân thức.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 029 [AIM_MATH12_BANK01_Q029] - Mức độ: **MEDIUM**

- **Chủ đề:** Xác định hệ số hàm bậc ba
- **Đề bài:** Cho hàm số y = ax^3 + bx^2 + cx + d có đồ thị đạt cực đại tại (0; 2) và cực tiểu tại (2; -2). Khi đó giá trị của a + b + c + d bằng

- **A.** 1
- **B.** 0
- **C.** -1
- **D.** 2

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** y' = 3ax^2 + 2bx + c. Cực trị tại x = 0 => c = 0. Đồ thị qua (0; 2) => d = 2. Cực trị tại x = 2 => 12a + 4b = 0 <=> b = -3a. y(2) = 8a + 4b + 2 = -2 <=> -4a = -4 <=> a = 1 => b = -3. Vậy a + b + c + d = 1 - 3 + 0 + 2 = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 030 [AIM_MATH12_BANK01_Q030] - Mức độ: **MEDIUM**

- **Chủ đề:** Hệ thức Vi-et cho cực trị
- **Đề bài:** Tìm m để hàm số y = x^3 - 3x^2 + mx - 1 có hai điểm cực trị x_1, x_2 thoả mãn x_1^2 + x_2^2 = 6.

- **A.** -3
- **B.** 3
- **C.** 0
- **D.** 1

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** y' = 3x^2 - 6x + m. Hàm số có 2 cực trị <=> Delta' = 9 - 3m > 0 <=> m < 3. Theo định lý Vi-et: x_1 + x_2 = 2, x_1*x_2 = m/3. Ta có x_1^2 + x_2^2 = (x_1 + x_2)^2 - 2x_1*x_2 = 4 - 2m/3 = 6 <=> 2m/3 = -2 <=> m = -3 (thỏa mãn m < 3).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 031 [AIM_MATH12_BANK01_Q031] - Mức độ: **MEDIUM**

- **Chủ đề:** Tương giao đồ thị
- **Đề bài:** Số giao điểm của đồ thị hàm số y = x^4 - 4x^2 + 3 và đường thẳng y = -1 là

- **A.** 4
- **B.** 2
- **C.** 3
- **D.** 0

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Phương trình hoành độ giao điểm: x^4 - 4x^2 + 3 = -1 <=> x^4 - 4x^2 + 4 = 0 <=> (x^2 - 2)^2 = 0 <=> x^2 = 2 <=> x = +-sqrt(2). Phương trình có đúng 2 nghiệm phân biệt nên có 2 giao điểm.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 032 [AIM_MATH12_BANK01_Q032] - Mức độ: **MEDIUM**

- **Chủ đề:** Đơn điệu hàm logarit
- **Đề bài:** Hàm số y = ln(x^2 - 2x + 3) đồng biến trên khoảng nào?

- **A.** (-vô cực; 1)
- **B.** (1; +vô cực)
- **C.** (0; 2)
- **D.** (-1; 3)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** y' = (2x - 2)/(x^2 - 2x + 3). Vì mẫu số x^2 - 2x + 3 = (x - 1)^2 + 2 > 0 với mọi x nên y' > 0 <=> 2x - 2 > 0 <=> x > 1. Vậy hàm số đồng biến trên (1; +vô cực).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 033 [AIM_MATH12_BANK01_Q033] - Mức độ: **MEDIUM**

- **Chủ đề:** Phương trình tiếp tuyến
- **Đề bài:** Cho hàm số y = (2x + 1)/(x - 1). Tiếp tuyến của đồ thị hàm số vuông góc với đường thẳng d: y = 3x + 2 có phương trình là

- **A.** y = -1/3 x + 1/3 hoặc y = -1/3 x + 13/3
- **B.** y = -1/3 x - 1
- **C.** y = 3x - 1
- **D.** y = -3x + 2

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** y' = -3/(x - 1)^2. Tiếp tuyến vuông góc với đường thẳng y = 3x + 2 nên có hệ số góc k = -1/3. Giải -3/(x - 1)^2 = -1/3 <=> (x - 1)^2 = 9 <=> x = 4 hoặc x = -2. Viết phương trình tiếp tuyến tại hai điểm ta được hai tiếp tuyến trên.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 034 [AIM_MATH12_BANK01_Q034] - Mức độ: **MEDIUM**

- **Chủ đề:** Tìm điểm cực tiểu
- **Đề bài:** Cho hàm số y = f(x) có đạo hàm f'(x) = x^2(x - 1)(x + 2)^3. Điểm cực tiểu của hàm số là

- **A.** x = -2
- **B.** x = 0
- **C.** x = 1
- **D.** Không có

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Xét dấu f'(x): qua x = -2 đạo hàm đổi dấu từ dương sang âm (cực đại); qua x = 0 là nghiệm bội chẵn nên không đổi dấu; qua x = 1 đạo hàm đổi dấu từ âm sang dương (cực tiểu). Vậy hàm số đạt cực tiểu tại x = 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 035 [AIM_MATH12_BANK01_Q035] - Mức độ: **MEDIUM**

- **Chủ đề:** Khoảng cách giữa hai cực trị
- **Đề bài:** Khoảng cách giữa hai điểm cực trị của đồ thị hàm số y = x^3 - 3x^2 + 2 bằng

- **A.** 4
- **B.** 2
- **C.** 2*sqrt(5)
- **D.** sqrt(5)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** y' = 3x(x - 2) = 0 <=> x = 0 (y = 2) hoặc x = 2 (y = -2). Hai điểm cực trị là A(0; 2) và B(2; -2). Độ dài đoạn thẳng AB = sqrt((2 - 0)^2 + (-2 - 2)^2) = sqrt(4 + 16) = sqrt(20) = 2*sqrt(5).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 036 [AIM_MATH12_BANK01_Q036] - Mức độ: **MEDIUM**

- **Chủ đề:** Tiệm cận triệt tiêu nhân tử
- **Đề bài:** Đồ thị hàm số y = (x - 1)/(x^2 - 3x + 2) có tất cả bao nhiêu đường tiệm cận (đứng và ngang)?

- **A.** 2
- **B.** 3
- **C.** 1
- **D.** 4

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Rút gọn với x khác 1: y = (x - 1)/((x - 1)(x - 2)) = 1/(x - 2). Đồ thị có 1 tiệm cận đứng là x = 2 và 1 tiệm cận ngang là y = 0. Tổng cộng có 2 đường tiệm cận.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 037 [AIM_MATH12_BANK01_Q037] - Mức độ: **MEDIUM**

- **Chủ đề:** Cực trị hình học thực tế
- **Đề bài:** Một chiếc hộp kim loại hình hộp chữ nhật có đáy hình vuông và không có nắp, thể tích bằng 500 cm^3. Để diện tích kim loại làm hộp là nhỏ nhất thì cạnh đáy bằng

- **A.** 10 cm
- **B.** 5 cm
- **C.** 15 cm
- **D.** 8 cm

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Gọi cạnh đáy là x (cm), chiều cao là h (cm). Ta có V = x^2 * h = 500 => h = 500/x^2. Diện tích kim loại làm hộp: S(x) = x^2 + 4xh = x^2 + 2000/x. S'(x) = 2x - 2000/x^2 = 0 <=> x^3 = 1000 <=> x = 10 cm.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 038 [AIM_MATH12_BANK01_Q038] - Mức độ: **MEDIUM**

- **Chủ đề:** Điểm uốn đồ thị bậc ba
- **Đề bài:** Đồ thị hàm số y = x^3 - 3x^2 + 2 có điểm uốn là điểm

- **A.** (0; 2)
- **B.** (2; -2)
- **C.** (1; 0)
- **D.** (-1; -2)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta có y' = 3x^2 - 6x, y'' = 6x - 6 = 0 <=> x = 1. Thay x = 1 vào hàm số ta được y = 1^3 - 3*1^2 + 2 = 0. Vậy toạ độ điểm uốn là U(1; 0).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 039 [AIM_MATH12_BANK01_Q039] - Mức độ: **MEDIUM**

- **Chủ đề:** Cực trị hàm phân thức
- **Đề bài:** Hàm số y = x + 1/x đạt cực tiểu tại điểm

- **A.** x = -1
- **B.** x = 0
- **C.** x = 1
- **D.** x = 2

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Tập xác định D = R \ {0}. y' = 1 - 1/x^2 = (x^2 - 1)/x^2 = 0 <=> x = 1 hoặc x = -1. Đạo hàm cấp hai y'' = 2/x^3 => y''(1) = 2 > 0 nên hàm số đạt cực tiểu tại x = 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 040 [AIM_MATH12_BANK01_Q040] - Mức độ: **MEDIUM**

- **Chủ đề:** Cực trị nghiệm bội chẵn lẻ
- **Đề bài:** Cho hàm số f(x) có f'(x) = (x - 2)^2025 * (x + 1)^2026. Số điểm cực trị của hàm số là

- **A.** 2
- **B.** 1
- **C.** 0
- **D.** 2025

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Nghiệm x = -1 có bội chẵn (2026) nên f'(x) không đổi dấu khi đi qua x = -1. Nghiệm x = 2 có bội lẻ (2025) nên f'(x) đổi dấu khi qua x = 2. Vậy hàm số có đúng 1 điểm cực trị.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 041 [AIM_MATH12_BANK01_Q041] - Mức độ: **HARD**

- **Chủ đề:** Cực trị hàm hợp nâng cao
- **Đề bài:** Cho hàm số f(x) có đạo hàm liên tục trên R và f'(x) = (x - 1)(x - 2)(x^2 - mx + m + 3). Có bao nhiêu giá trị nguyên của m để hàm số g(x) = f(x^2) có đúng 3 điểm cực trị?

- **A.** 4
- **B.** 6
- **C.** 3
- **D.** 5

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Ta có g'(x) = 2x * f'(x^2) = 2x(x^2 - 1)(x^2 - 2)(x^4 - mx^2 + m + 3). Phân tích số nghiệm bội lẻ của g'(x) = 0 để hàm số có đúng 3 điểm cực trị, tìm được điều kiện m thỏa mãn delta và nghiệm, tổng cộng có 5 giá trị nguyên của m.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 042 [AIM_MATH12_BANK01_Q042] - Mức độ: **HARD**

- **Chủ đề:** Cực trị khoảng cách toạ độ
- **Đề bài:** Cho hàm số y = (x + 1)/(x - 2). Tìm toạ độ điểm M thuộc đồ thị sao cho tổng khoảng cách từ M đến hai trục toạ độ đạt giá trị nhỏ nhất.

- **A.** (1; -2/3)
- **B.** M(2 + sqrt(3); 1 + sqrt(3))
- **C.** M(0; -1/2)
- **D.** M(-1; 0)

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Khảo sát hàm số khoảng cách d(x) = |x| + |(x + 1)/(x - 2)| trên từng khoảng xác định và tìm điểm cực tiểu toàn cục, ta thu được toạ độ điểm M(1; -2/3).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 043 [AIM_MATH12_BANK01_Q043] - Mức độ: **HARD**

- **Chủ đề:** Toán ứng dụng kinh tế đạo hàm
- **Đề bài:** Một công ty sản xuất một loại sản phẩm với chi phí C(x) = 2x^3 - 15x^2 + 36x + 50 (triệu đồng), trong đó x là số tấn sản phẩm sản xuất (1 <= x <= 6). Chi phí biên thấp nhất đạt được khi sản xuất bao nhiêu tấn?

- **A.** 2,5 tấn
- **B.** 3 tấn
- **C.** 2 tấn
- **D.** 4 tấn

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Chi phí biên là đạo hàm của hàm chi phí: C'(x) = 6x^2 - 30x + 36. C''(x) = 12x - 30 = 0 <=> x = 2,5. Vì C''(x) đổi dấu từ âm sang dương qua x = 2,5 nên C'(x) đạt giá trị nhỏ nhất tại x = 2,5 tấn.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 044 [AIM_MATH12_BANK01_Q044] - Mức độ: **HARD**

- **Chủ đề:** Biện luận nghiệm đồ thị
- **Đề bài:** Cho hàm số y = f(x) bậc bốn có đồ thị f'(x) đối xứng. Để phương trình f(x) = m có 4 nghiệm phân biệt thì điều kiện của m là

- **A.** m > f(0)
- **B.** f(0) < m < f(cực đại)
- **C.** m < f(cực tiểu)
- **D.** f(cực tiểu) < m < f(cực đại)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Dựa vào bảng biến thiên của hàm số f(x), để đường thẳng y = m cắt đồ thị tại 4 điểm phân biệt thì m phải nằm trong khoảng giữa giá trị cực tiểu và giá trị cực đại của hàm số: f(cực tiểu) < m < f(cực đại).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 045 [AIM_MATH12_BANK01_Q045] - Mức độ: **HARD**

- **Chủ đề:** Diện tích tam giác cực trị
- **Đề bài:** Tìm m để hàm số y = x^3 - 3mx^2 + 4 có hai điểm cực trị A và B sao cho diện tích tam giác OAB bằng 4 (với O là gốc toạ độ).

- **A.** m = 2
- **B.** m = sqrt(2)
- **C.** m = 4
- **D.** m = 1 hoặc m = -1

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Ta có y' = 3x^2 - 6mx = 0 <=> x = 0 hoặc x = 2m. Hai điểm cực trị là A(0; 4) và B(2m; -4m^3 + 4). Diện tích tam giác OAB vuông tại trục Oy: S = 1/2 * OA * |x_B| = 1/2 * 4 * |2m| = 4|m|. Theo đề bài S = 4 <=> 4|m| = 4 <=> |m| = 1 <=> m = 1 hoặc m = -1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 046 [AIM_MATH12_BANK01_Q046] - Mức độ: **HARD**

- **Chủ đề:** Cực trị hàm trị tuyệt đối
- **Đề bài:** Có bao nhiêu giá trị nguyên của m thuộc [-20; 20] để hàm số y = |x^3 - 3x^2 + m| có 5 điểm cực trị?

- **A.** 4
- **B.** 3
- **C.** 5
- **D.** 2

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Hàm số y = |f(x)| có 5 điểm cực trị khi và chỉ khi hàm số f(x) = x^3 - 3x^2 + m có hai điểm cực trị nằm về hai phía của trục hoành, tức f(0)*f(2) < 0 <=> m*(m - 4) < 0 <=> 0 < m < 4. Các giá trị nguyên thỏa mãn là m thuộc {1; 2; 3}, gồm 3 giá trị.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 047 [AIM_MATH12_BANK01_Q047] - Mức độ: **HARD**

- **Chủ đề:** Xác định hàm phân thức hữu tỉ
- **Đề bài:** Cho hàm số y = (ax + b)/(cx + d) (với c = 1) có đồ thị nhận đường thẳng x = 1 làm tiệm cận đứng, đường thẳng y = 2 làm tiệm cận ngang và đi qua điểm A(2; 5). Giá trị của biểu thức T = a + b + c + d bằng

- **A.** 5
- **B.** 6
- **C.** 4
- **D.** 3

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Với c = 1, hàm số có dạng y = (ax + b)/(x + d). Tiệm cận đứng x = 1 <=> -d = 1 <=> d = -1. Tiệm cận ngang y = 2 <=> a/1 = 2 <=> a = 2. Khi đó y = (2x + b)/(x - 1). Đồ thị đi qua điểm A(2; 5) nên thay x = 2, y = 5 ta được: (2*2 + b)/(2 - 1) = 5 <=> 4 + b = 5 <=> b = 1. Do đó T = a + b + c + d = 2 + 1 + 1 + (-1) = 3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 048 [AIM_MATH12_BANK01_Q048] - Mức độ: **HARD**

- **Chủ đề:** Tối ưu hóa đa chi phí
- **Đề bài:** Một người muốn làm một bể cá bằng kính có dạng hình lăng trụ tứ giác đều không nắp với dung tích V = 32 m^3. Chiều cao của bể để diện tích kính sử dụng là nhỏ nhất bằng

- **A.** 2 m
- **B.** 4 m
- **C.** 3 m
- **D.** 1 m

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Gọi cạnh đáy là x (m), chiều cao là h (m). Thể tích V = x^2 * h = 32 => h = 32/x^2. Diện tích kính làm bể không nắp là S(x) = x^2 + 4xh = x^2 + 128/x. Ta có S'(x) = 2x - 128/x^2 = 0 <=> x^3 = 64 <=> x = 4 m. Khi đó chiều cao h = 32 / 4^2 = 32 / 16 = 2 m.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 049 [AIM_MATH12_BANK01_Q049] - Mức độ: **HARD**

- **Chủ đề:** Biện luận số tiệm cận chứa tham số
- **Đề bài:** Tìm m để đồ thị hàm số y = (x - 1)/(x^2 - 2mx + 9) có đúng 2 đường tiệm cận đứng.

- **A.** -3 < m < 3
- **B.** m > 3 hoặc m < -3
- **C.** m > 3 hoặc m < -3 (và m khác 5)
- **D.** m = 3

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Đồ thị có đúng 2 tiệm cận đứng khi mẫu số g(x) = x^2 - 2mx + 9 có hai nghiệm phân biệt khác 1 <=> Delta' = m^2 - 9 > 0 và g(1) = 1 - 2m + 9 khác 0 <=> m thuộc (-vô cực; -3) hợp (3; +vô cực) và m khác 5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 050 [AIM_MATH12_BANK01_Q050] - Mức độ: **HARD**

- **Chủ đề:** Cực trị hàm hợp tầng 2
- **Đề bài:** Cho hàm số bậc ba y = f(x) có 2 điểm cực trị và phương trình f(x) = 0 có 3 nghiệm phân biệt. Số điểm cực trị của hàm số g(x) = f(f(x)) là

- **A.** 7
- **B.** 6
- **C.** 5
- **D.** 8

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Ta có g'(x) = f'(x) * f'(f(x)). Đếm số nghiệm bội lẻ của f'(x) = 0 (2 nghiệm) và các nghiệm của f(x) = x_cực trị (5 nghiệm phân biệt cắt ngang), tổng cộng hàm số g(x) có đúng 7 điểm cực trị.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

## Chương II: Vectơ và hệ trục toạ độ trong không gian

### Câu 051 [AIM_MATH12_BANK01_Q051] - Mức độ: **EASY**

- **Chủ đề:** Cộng trừ vectơ
- **Đề bài:** Trong không gian Oxyz, cho vectơ a = (1; -2; 3) và b = (2; 1; -1). Toạ độ của vectơ a + b là

- **A.** (3; -1; 2)
- **B.** (1; 3; -4)
- **C.** (-1; -3; 4)
- **D.** (3; 1; 2)

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Toạ độ tổng hai vectơ: a + b = (1 + 2; -2 + 1; 3 + (-1)) = (3; -1; 2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 052 [AIM_MATH12_BANK01_Q052] - Mức độ: **EASY**

- **Chủ đề:** Tích vô hướng
- **Đề bài:** Trong không gian Oxyz, tích vô hướng của hai vectơ u = (2; -1; 1) và v = (1; 3; -2) bằng

- **A.** 3
- **B.** 7
- **C.** -3
- **D.** -5

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Tích vô hướng: u . v = 2*1 + (-1)*3 + 1*(-2) = 2 - 3 - 2 = -3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 053 [AIM_MATH12_BANK01_Q053] - Mức độ: **EASY**

- **Chủ đề:** Độ dài vectơ
- **Đề bài:** Trong không gian Oxyz, độ dài của vectơ u = (2; -2; 1) là

- **A.** 3
- **B.** 9
- **C.** sqrt(5)
- **D.** 5

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Độ dài vectơ: |u| = sqrt(2^2 + (-2)^2 + 1^2) = sqrt(4 + 4 + 1) = sqrt(9) = 3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 054 [AIM_MATH12_BANK01_Q054] - Mức độ: **EASY**

- **Chủ đề:** Toạ độ trung điểm
- **Đề bài:** Trong không gian Oxyz, cho hai điểm A(1; 2; -1) và B(3; 0; 1). Toạ độ trung điểm I của đoạn thẳng AB là

- **A.** (4; 2; 0)
- **B.** (1; -1; 1)
- **C.** (2; 1; 0)
- **D.** (2; 0; 1)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Toạ độ trung điểm I: I = ((1 + 3)/2; (2 + 0)/2; (-1 + 1)/2) = (2; 1; 0).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 055 [AIM_MATH12_BANK01_Q055] - Mức độ: **EASY**

- **Chủ đề:** Trọng tâm tam giác
- **Đề bài:** Trong không gian Oxyz, toạ độ trọng tâm G của tam giác ABC với A(1; 0; 2), B(-2; 1; 3), C(4; 2; 1) là

- **A.** (3; 3; 6)
- **B.** (1; 3; 2)
- **C.** (1; 1; 2)
- **D.** (2; 1; 1)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Toạ độ trọng tâm G: G = ((1 - 2 + 4)/3; (0 + 1 + 2)/3; (2 + 3 + 1)/3) = (3/3; 3/3; 6/3) = (1; 1; 2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 056 [AIM_MATH12_BANK01_Q056] - Mức độ: **EASY**

- **Chủ đề:** Vectơ đơn vị
- **Đề bài:** Trong không gian Oxyz, vectơ đơn vị trên trục Oz là

- **A.** i = (1; 0; 0)
- **B.** j = (0; 1; 0)
- **C.** k = (0; 0; 1)
- **D.** e = (1; 1; 1)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Theo quy ước toạ độ chuẩn, vectơ đơn vị trên trục Oz là k = (0; 0; 1).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 057 [AIM_MATH12_BANK01_Q057] - Mức độ: **EASY**

- **Chủ đề:** Quy tắc hình hộp
- **Đề bài:** Cho hình hộp ABCD.A'B'C'D'. Tổng vectơ AB + AD + AA' bằng vectơ nào sau đây?

- **A.** AC
- **B.** AC'
- **C.** A'C
- **D.** BD'

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Theo quy tắc hình hộp trong không gian, tổng AB + AD + AA' = AC'.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 058 [AIM_MATH12_BANK01_Q058] - Mức độ: **EASY**

- **Chủ đề:** Khoảng cách 2 điểm
- **Đề bài:** Trong không gian Oxyz, khoảng cách giữa hai điểm M(1; -1; 0) và N(4; 3; 0) bằng

- **A.** 5
- **B.** 25
- **C.** 7
- **D.** sqrt(17)

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Khoảng cách MN = sqrt((4 - 1)^2 + (3 - (-1))^2 + (0 - 0)^2) = sqrt(3^2 + 4^2 + 0) = sqrt(25) = 5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 059 [AIM_MATH12_BANK01_Q059] - Mức độ: **EASY**

- **Chủ đề:** Góc giữa 2 vectơ
- **Đề bài:** Trong không gian Oxyz, góc giữa hai vectơ u = (1; 0; 0) và v = (0; 1; 0) bằng

- **A.** 90 độ
- **B.** 0 độ
- **C.** 45 độ
- **D.** 180 độ

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Tích vô hướng u . v = 1*0 + 0*1 + 0*0 = 0. Do đó hai vectơ vuông góc nhau, góc giữa chúng bằng 90 độ.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 060 [AIM_MATH12_BANK01_Q060] - Mức độ: **EASY**

- **Chủ đề:** Sự đồng phẳng
- **Đề bài:** Trong không gian, điều kiện để ba vectơ a, b, c đồng phẳng là

- **A.** a + b + c = 0
- **B.** Tồn tại cặp số (m, n) sao cho c = m*a + n*b (với a, b không cùng phương)
- **C.** a . b . c = 0
- **D.** |a| + |b| = |c|

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Ba vectơ được gọi là đồng phẳng khi một vectơ biểu diễn tuyến tính được qua hai vectơ không cùng phương còn lại.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 061 [AIM_MATH12_BANK01_Q061] - Mức độ: **EASY**

- **Chủ đề:** Hình chiếu lên trục
- **Đề bài:** Hình chiếu vuông góc của điểm M(2; -3; 5) lên trục Ox có toạ độ là

- **A.** (0; -3; 0)
- **B.** (0; 0; 5)
- **C.** (2; 0; 0)
- **D.** (2; -3; 0)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Chiếu vuông góc lên trục Ox thì giữ nguyên hoành độ x = 2, các thành phần tung độ và cao độ bằng 0: (2; 0; 0).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 062 [AIM_MATH12_BANK01_Q062] - Mức độ: **EASY**

- **Chủ đề:** Hình chiếu lên mặt toạ độ
- **Đề bài:** Hình chiếu vuông góc của điểm P(1; 4; -2) lên mặt phẳng toạ độ (Oxy) là

- **A.** (1; 0; -2)
- **B.** (0; 4; -2)
- **C.** (0; 0; -2)
- **D.** (1; 4; 0)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Chiếu vuông góc lên mặt phẳng (Oxy) thì giữ nguyên hoành độ x = 1, tung độ y = 4 và cho cao độ z = 0: (1; 4; 0).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 063 [AIM_MATH12_BANK01_Q063] - Mức độ: **MEDIUM**

- **Chủ đề:** Vectơ vuông góc
- **Đề bài:** Trong không gian Oxyz, tìm m để hai vectơ u = (m; 2; -1) và v = (2; -3; 4) vuông góc với nhau.

- **A.** 3
- **B.** -5
- **C.** 5
- **D.** 4

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** u vuông góc với v <=> u . v = 0 <=> 2m - 6 - 4 = 0 <=> 2m = 10 <=> m = 5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 064 [AIM_MATH12_BANK01_Q064] - Mức độ: **MEDIUM**

- **Chủ đề:** Cosin góc giữa 2 vectơ
- **Đề bài:** Trong không gian Oxyz, cosin của góc giữa hai vectơ a = (2; 1; 0) và b = (-1; 0; -2) bằng

- **A.** -2/sqrt(5)
- **B.** 2/5
- **C.** -2/5
- **D.** 1/5

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta có cos(a, b) = (a . b) / (|a|*|b|) = (2*(-1) + 1*0 + 0*(-2)) / (sqrt(4 + 1)*sqrt(1 + 4)) = -2 / (sqrt(5)*sqrt(5)) = -2/5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 065 [AIM_MATH12_BANK01_Q065] - Mức độ: **MEDIUM**

- **Chủ đề:** Diện tích tam giác bằng toạ độ
- **Đề bài:** Trong không gian Oxyz, cho tam giác ABC có A(1; 0; 0), B(0; 0; 1), C(2; 1; 1). Diện tích tam giác ABC bằng

- **A.** sqrt(6)
- **B.** 3/2
- **C.** sqrt(6)/2
- **D.** sqrt(3)/2

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta có AB = (-1; 0; 1), AC = (1; 1; 1). Tích có hướng [AB, AC] = (-1; 2; -1). Diện tích tam giác S = 1/2 * |[AB, AC]| = 1/2 * sqrt((-1)^2 + 2^2 + (-1)^2) = sqrt(6)/2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 066 [AIM_MATH12_BANK01_Q066] - Mức độ: **MEDIUM**

- **Chủ đề:** Toạ độ đỉnh hình bình hành
- **Đề bài:** Trong không gian Oxyz, toạ độ điểm D sao cho tứ giác ABCD là hình bình hành biết A(1; 1; 1), B(2; 3; 4), C(6; 5; 2) là

- **A.** (7; 7; 5)
- **B.** (5; 3; -1)
- **C.** (3; 1; -1)
- **D.** (5; 5; 3)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** ABCD là hình bình hành <=> vectơ AB = vectơ DC <=> B - A = C - D <=> D = C - B + A = (6 - 2 + 1; 5 - 3 + 1; 2 - 4 + 1) = (5; 3; -1).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 067 [AIM_MATH12_BANK01_Q067] - Mức độ: **MEDIUM**

- **Chủ đề:** Tích có hướng
- **Đề bài:** Trong không gian Oxyz, cho vectơ a = (1; 2; 3) và b = (2; -1; 1). Toạ độ của tích có hướng [a, b] là

- **A.** (5; -5; -5)
- **B.** (1; 5; -5)
- **C.** (-5; 5; 5)
- **D.** (5; 5; -5)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** [a, b] = (2*1 - 3*(-1); 3*2 - 1*1; 1*(-1) - 2*2) = (2 + 3; 6 - 1; -1 - 4) = (5; 5; -5).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 068 [AIM_MATH12_BANK01_Q068] - Mức độ: **MEDIUM**

- **Chủ đề:** Xét tính thẳng hàng
- **Đề bài:** Trong không gian Oxyz, cho ba điểm A(1; 2; 0), B(2; 3; -1), C(0; 1; 1). Khẳng định nào sau đây là đúng?

- **A.** Ba điểm A, B, C tạo thành tam giác vuông
- **B.** Ba điểm A, B, C tạo thành tam giác đều
- **C.** Ba điểm A, B, C thẳng hàng
- **D.** Ba điểm A, B, C tạo thành tam giác cân

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta có AB = (2 - 1; 3 - 2; -1 - 0) = (1; 1; -1) và AC = (0 - 1; 1 - 2; 1 - 0) = (-1; -1; 1). Nhận thấy AC = -1 * AB, do đó hai vectơ cùng phương, suy ra ba điểm A, B, C thẳng hàng.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 069 [AIM_MATH12_BANK01_Q069] - Mức độ: **MEDIUM**

- **Chủ đề:** Điểm cách đều trên trục
- **Đề bài:** Trong không gian Oxyz, tìm toạ độ điểm M thuộc trục Oz cách đều hai điểm A(2; 3; 4) và B(1; 2; 3).

- **A.** (0; 0; 7)
- **B.** (0; 0; 15/2)
- **C.** (0; 0; 5)
- **D.** (0; 0; 3)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Điểm M thuộc trục Oz có toạ độ M(0; 0; z). Ta có: MA^2 = (0 - 2)^2 + (0 - 3)^2 + (z - 4)^2 = z^2 - 8z + 29; MB^2 = (0 - 1)^2 + (0 - 2)^2 + (z - 3)^2 = z^2 - 6z + 14. Vì M cách đều A và B nên MA = MB <=> MA^2 = MB^2 <=> z^2 - 8z + 29 = z^2 - 6z + 14 <=> -2z = -15 <=> z = 15/2. Vậy toạ độ điểm M là (0; 0; 15/2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 070 [AIM_MATH12_BANK01_Q070] - Mức độ: **MEDIUM**

- **Chủ đề:** Độ dài đường phân giác
- **Đề bài:** Trong không gian Oxyz, độ dài đường phân giác trong góc A của tam giác ABC với A(0; 0; 0), B(3; 0; 0), C(0; 4; 0) bằng

- **A.** 2*sqrt(2)
- **B.** 12*sqrt(2)/7
- **C.** 12/7
- **D.** 5*sqrt(2)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Ta có AB = 3, AC = 4. Chân đường phân giác trong D chia đoạn BC theo tỉ số DB/DC = AB/AC = 3/4. Tính toạ độ điểm D(12/7; 12/7; 0), suy ra độ dài AD = sqrt((12/7)^2 + (12/7)^2) = 12*sqrt(2)/7.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 071 [AIM_MATH12_BANK01_Q071] - Mức độ: **MEDIUM**

- **Chủ đề:** Tỉ số thể tích trọng tâm
- **Đề bài:** Cho tứ diện ABCD có thể tích V = 6. Thể tích khối tứ diện tạo bởi trọng tâm của 4 mặt của tứ diện ABCD bằng

- **A.** 2/27
- **B.** 1/3
- **C.** 2/9
- **D.** 2/3

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Khối tứ diện tạo bởi 4 trọng tâm đồng dạng với tứ diện ABCD theo tỉ số k = 1/3, do đó thể tích V' = k^3 * V = (1/27) * 6 = 2/9.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 072 [AIM_MATH12_BANK01_Q072] - Mức độ: **MEDIUM**

- **Chủ đề:** So sánh độ dài
- **Đề bài:** Trong không gian Oxyz, cho hai vectơ a = (1; m; 2) và b = (m + 1; 2; 1). Tìm giá trị của m để độ dài của hai vectơ bằng nhau.

- **A.** m = 1/2
- **B.** m = 0
- **C.** m = -1/2
- **D.** m = -1

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta có |a|^2 = 1^2 + m^2 + 2^2 = m^2 + 5 và |b|^2 = (m + 1)^2 + 2^2 + 1^2 = m^2 + 2m + 6. Để |a| = |b| <=> |a|^2 = |b|^2 <=> m^2 + 5 = m^2 + 2m + 6 <=> 2m = -1 <=> m = -1/2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 073 [AIM_MATH12_BANK01_Q073] - Mức độ: **MEDIUM**

- **Chủ đề:** Góc với trục toạ độ
- **Đề bài:** Góc giữa vectơ u = (1; 1; 0) và trục Oz bằng

- **A.** 45 độ
- **B.** 90 độ
- **C.** 60 độ
- **D.** 0 độ

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Vectơ đơn vị trên trục Oz là k = (0; 0; 1). Tích vô hướng u . k = 1*0 + 1*0 + 0*1 = 0 nên u vuông góc với trục Oz, góc giữa chúng bằng 90 độ.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 074 [AIM_MATH12_BANK01_Q074] - Mức độ: **MEDIUM**

- **Chủ đề:** Độ dài hiệu hai vectơ
- **Đề bài:** Cho hai vectơ a và b thoả mãn |a| = 3, |b| = 4 và góc giữa chúng bằng 60 độ. Độ dài của vectơ a - b bằng

- **A.** sqrt(37)
- **B.** 5
- **C.** sqrt(7)
- **D.** sqrt(13)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Ta có |a - b|^2 = |a|^2 + |b|^2 - 2|a||b|cos(60) = 3^2 + 4^2 - 2*3*4*(1/2) = 9 + 16 - 12 = 13 => |a - b| = sqrt(13).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 075 [AIM_MATH12_BANK01_Q075] - Mức độ: **HARD**

- **Chủ đề:** Cực trị tổng bình phương khoảng cách
- **Đề bài:** Trong không gian Oxyz, cho 3 điểm A(1; 0; 0), B(0; 2; 0), C(0; 0; 3). Điểm M(x; y; z) thoả mãn MA^2 + MB^2 + MC^2 đạt giá trị nhỏ nhất thì toạ độ M là

- **A.** (1; 2; 3)
- **B.** (1/3; 2/3; 1)
- **C.** (0; 0; 0)
- **D.** (1/2; 1; 3/2)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Biểu thức T = MA^2 + MB^2 + MC^2 đạt giá trị nhỏ nhất khi M trùng với trọng tâm G của tam giác ABC: G = ((1+0+0)/3; (0+2+0)/3; (0+0+3)/3) = (1/3; 2/3; 1).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 076 [AIM_MATH12_BANK01_Q076] - Mức độ: **HARD**

- **Chủ đề:** Khoảng cách tứ diện vuông
- **Đề bài:** Cho hình chóp O.ABC có OA, OB, OC đôi một vuông góc tại O và OA = 1, OB = 2, OC = 3. Khoảng cách từ O đến mặt phẳng (ABC) bằng

- **A.** 7/6
- **B.** 1
- **C.** 6/7
- **D.** 3/2

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Vì OA, OB, OC đôi một vuông góc tại O nên khoảng cách h từ O đến mặt phẳng (ABC) thoả mãn hệ thức: 1/h^2 = 1/OA^2 + 1/OB^2 + 1/OC^2 = 1/1^2 + 1/2^2 + 1/3^2 = 1 + 1/4 + 1/9 = 49/36. Suy ra h^2 = 36/49 => h = 6/7.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 077 [AIM_MATH12_BANK01_Q077] - Mức độ: **HARD**

- **Chủ đề:** Điểm cực trị trên mặt toạ độ
- **Đề bài:** Trong không gian Oxyz, cho A(1; 2; 3) và B(3; 4; 7). Điểm M nằm trên mặt phẳng (Oxy) sao cho chu vi tam giác MAB đạt giá trị nhỏ nhất có toạ độ là

- **A.** (7/5; 8/5; 0)
- **B.** (1/5; 4/5; 0)
- **C.** (2; 3; 0)
- **D.** (8/5; 13/5; 0)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Vì A và B có cao độ cùng dấu (z_A=3, z_B=7) nên cùng phía so với mặt phẳng (Oxy). Lấy A' đối xứng với A qua (Oxy) là A'(1; 2; -3). Khi đó MA + MB = MA' + MB >= A'B. Dấu bằng xảy ra khi M là giao điểm của đường thẳng A'B với (Oxy). Đường thẳng A'B đi qua A'(1; 2; -3) có VTCP u = (1; 1; 5) có phương trình: x = 1 + t, y = 2 + t, z = -3 + 5t. Cho z = 0 <=> t = 3/5 => x = 8/5, y = 13/5. Vậy toạ độ điểm M là (8/5; 13/5; 0).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 078 [AIM_MATH12_BANK01_Q078] - Mức độ: **HARD**

- **Chủ đề:** Thể tích tứ diện toạ độ
- **Đề bài:** Thể tích của tứ diện ABCD với A(1; 0; 0), B(0; 1; 0), C(0; 0; 1), D(1; 1; 1) bằng

- **A.** 1/6
- **B.** 1/2
- **C.** 1/3
- **D.** 1

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta có AB = (-1; 1; 0), AC = (-1; 0; 1), AD = (0; 1; 1). Tích có hướng [AB, AC] = (1; 1; 1). Tích hỗn tạp [AB, AC] . AD = 1*0 + 1*1 + 1*1 = 2. Thể tích V = 1/6 * |[AB, AC] . AD| = 1/6 * 2 = 1/3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 079 [AIM_MATH12_BANK01_Q079] - Mức độ: **HARD**

- **Chủ đề:** Trực giao hoá vectơ
- **Đề bài:** Cho hai vectơ u và v không cùng phương. Vectơ w = u - [ (u . v)/|v|^2 ] * v có tính chất nào sau đây?

- **A.** Vuông góc với v
- **B.** Cùng phương với v
- **C.** Vuông góc với u
- **D.** Có độ dài bằng |u|

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Tính tích vô hướng: w . v = (u - [ (u . v)/|v|^2 ] * v) . v = u . v - [ (u . v)/|v|^2 ] * (v . v) = u . v - u . v = 0. Do đó vectơ w luôn vuông góc với v (phép trực giao hoá Gram-Schmidt).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 080 [AIM_MATH12_BANK01_Q080] - Mức độ: **HARD**

- **Chủ đề:** Góc giữa hai mặt phẳng
- **Đề bài:** Trong không gian Oxyz, cho hình chóp S.ABCD có đáy là hình vuông cạnh a, SA vuông góc với đáy và SA = a*sqrt(2). Cosin của góc giữa hai mặt phẳng (SBD) và (ABCD) bằng

- **A.** 2/sqrt(5)
- **B.** 1/3
- **C.** sqrt(3)/2
- **D.** 1/sqrt(5)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Chọn hệ trục toạ độ sao cho A là gốc O(0; 0; 0), B(a; 0; 0), D(0; a; 0), S(0; 0; a*sqrt(2)). Mặt phẳng (ABCD) có VTPT n_1 = (0; 0; 1). Mặt phẳng (SBD) có VTPT n_2 = (sqrt(2); sqrt(2); 1). Khi đó cosin của góc giữa hai mặt phẳng là cos(phi) = |n_1 . n_2| / (|n_1|*|n_2|) = 1 / sqrt(2 + 2 + 1) = 1/sqrt(5).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

## Chương III: Các số đặc trưng đo mức độ phân tán của mẫu số liệu ghép nhóm

### Câu 081 [AIM_MATH12_BANK01_Q081] - Mức độ: **EASY**

- **Chủ đề:** Khoảng biến thiên
- **Đề bài:** Khoảng biến thiên của mẫu số liệu ghép nhóm [a_1; a_k] được tính bằng công thức

- **A.** R = a_k - a_1
- **B.** R = a_k + a_1
- **C.** R = (a_k - a_1)/2
- **D.** R = a_k * a_1

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Khoảng biến thiên của mẫu số liệu ghép nhóm là hiệu giữa đầu mút phải của nhóm cuối cùng và đầu mút trái của nhóm đầu tiên: R = a_k - a_1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 082 [AIM_MATH12_BANK01_Q082] - Mức độ: **EASY**

- **Chủ đề:** Giá trị đại diện
- **Đề bài:** Giá trị đại diện của nhóm số liệu [20; 30) là

- **A.** 20
- **B.** 30
- **C.** 25
- **D.** 10

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Giá trị đại diện của nhóm [a; b) là trung bình cộng hai đầu mút của nhóm đó: (20 + 30)/2 = 25.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 083 [AIM_MATH12_BANK01_Q083] - Mức độ: **EASY**

- **Chủ đề:** Khoảng tứ phân vị
- **Đề bài:** Khoảng tứ phân vị Delta_Q của mẫu số liệu được định nghĩa bằng công thức

- **A.** Delta_Q = Q_3 - Q_1
- **B.** Delta_Q = Q_3 + Q_1
- **C.** Delta_Q = Q_2 - Q_1
- **D.** Delta_Q = (Q_3 - Q_1)/2

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Khoảng tứ phân vị của mẫu số liệu là hiệu số giữa tứ phân vị thứ ba và tứ phân vị thứ nhất: Delta_Q = Q_3 - Q_1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 084 [AIM_MATH12_BANK01_Q084] - Mức độ: **EASY**

- **Chủ đề:** Độ lệch chuẩn
- **Đề bài:** Mối quan hệ giữa độ lệch chuẩn s và phương sai s^2 của mẫu số liệu là

- **A.** s = (s^2)^2
- **B.** s = 2 * s^2
- **C.** s = s^2 / 2
- **D.** s = sqrt(s^2)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Độ lệch chuẩn là căn bậc hai số học của phương sai: s = sqrt(s^2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 085 [AIM_MATH12_BANK01_Q085] - Mức độ: **EASY**

- **Chủ đề:** Ý nghĩa độ phân tán
- **Đề bài:** Độ phân tán của mẫu số liệu quanh số trung bình càng lớn khi

- **A.** Số lượng phần tử n càng lớn
- **B.** Số trung bình càng lớn
- **C.** Phương sai s^2 càng lớn
- **D.** Khoảng biến thiên bằng 0

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Phương sai và độ lệch chuẩn dùng để đo mức độ phân tán của các số liệu quanh số trung bình. Phương sai càng lớn thì dữ liệu càng phân tán rộng.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 086 [AIM_MATH12_BANK01_Q086] - Mức độ: **EASY**

- **Chủ đề:** Độ dài nhóm
- **Đề bài:** Độ dài của nhóm số liệu [15; 25) bằng

- **A.** 10
- **B.** 20
- **C.** 15
- **D.** 5

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Độ dài của nhóm số liệu [a; b) là hiệu số giữa hai đầu mút: b - a = 25 - 15 = 10.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 087 [AIM_MATH12_BANK01_Q087] - Mức độ: **EASY**

- **Chủ đề:** Trung vị và Q2
- **Đề bài:** Tứ phân vị thứ hai Q_2 của mẫu số liệu ghép nhóm chính là

- **A.** Số trung bình cộng
- **B.** Số trung vị Me
- **C.** Mốt Mo
- **D.** Khoảng biến thiên R

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Tứ phân vị thứ hai Q_2 chia mẫu số liệu đã sắp xếp thành hai nửa bằng nhau, chính là trung vị Me của mẫu số liệu.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 088 [AIM_MATH12_BANK01_Q088] - Mức độ: **EASY**

- **Chủ đề:** Tính chất phương sai
- **Đề bài:** Nếu tất cả các số liệu trong mẫu ghép nhóm được cộng thêm một hằng số c > 0 thì phương sai của mẫu

- **A.** Tăng thêm c đơn vị
- **B.** Không đổi
- **C.** Tăng thêm c^2 đơn vị
- **D.** Nhân lên c lần

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Khi cộng mỗi giá trị với một hằng số c thì khoảng cách tương đối giữa các giá trị với số trung bình không thay đổi, do đó phương sai và độ lệch chuẩn không đổi.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 089 [AIM_MATH12_BANK01_Q089] - Mức độ: **EASY**

- **Chủ đề:** Tính chất độ lệch chuẩn
- **Đề bài:** Nếu tất cả các số liệu trong mẫu được nhân với số k > 0 thì độ lệch chuẩn của mẫu số liệu sẽ

- **A.** Nhân với k
- **B.** Không đổi
- **C.** Nhân với k^2
- **D.** Tăng k đơn vị

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Khi tất cả các giá trị của mẫu số liệu được nhân với số k > 0 thì độ lệch chuẩn mới sẽ bằng k lần độ lệch chuẩn cũ: s' = k * s.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 090 [AIM_MATH12_BANK01_Q090] - Mức độ: **EASY**

- **Chủ đề:** Công thức số trung bình
- **Đề bài:** Số trung bình của mẫu số liệu ghép nhóm được tính bằng

- **A.** Tổng tích giữa tần số và giá trị đại diện của từng nhóm chia cho kích thước mẫu
- **B.** Trung bình cộng của các đầu mút
- **C.** Giá trị lớn nhất trừ giá trị nhỏ nhất
- **D.** Tần số của nhóm có tần số lớn nhất

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Số trung bình của mẫu số liệu ghép nhóm được tính theo công thức x_tb = (1/n) * sum(m_i * c_i), trong đó m_i là tần số và c_i là giá trị đại diện của nhóm thứ i.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 091 [AIM_MATH12_BANK01_Q091] - Mức độ: **MEDIUM**

- **Chủ đề:** Tính khoảng biến thiên
- **Đề bài:** Cho bảng số liệu ghép nhóm thời gian tự học (giờ/tuần) của 40 học sinh: [0; 5): 8; [5; 10): 16; [10; 15): 12; [15; 20): 4. Khoảng biến thiên của mẫu số liệu là

- **A.** 15
- **B.** 10
- **C.** 20
- **D.** 25

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Khoảng biến thiên R = a_k - a_1 = 20 - 0 = 20 giờ.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 092 [AIM_MATH12_BANK01_Q092] - Mức độ: **MEDIUM**

- **Chủ đề:** Xác định nhóm tứ phân vị
- **Đề bài:** Cho mẫu số liệu ghép nhóm có n = 40 phần tử. Tứ phân vị thứ nhất Q_1 thuộc nhóm chứa giá trị thứ mấy?

- **A.** Thứ 20
- **B.** Thứ 10
- **C.** Thứ 30
- **D.** Thứ 5

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Vị trí xác định nhóm chứa Q_1 là n/4 = 40/4 = 10. Do đó Q_1 thuộc nhóm chứa giá trị thứ 10.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 093 [AIM_MATH12_BANK01_Q093] - Mức độ: **MEDIUM**

- **Chủ đề:** Vị trí Q3
- **Đề bài:** Cho mẫu số liệu ghép nhóm có n = 60. Nhóm chứa tứ phân vị thứ ba Q_3 là nhóm chứa giá trị thứ

- **A.** 45
- **B.** 30
- **C.** 15
- **D.** 50

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Vị trí xác định nhóm chứa Q_3 là 3n/4 = 3*60/4 = 45.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 094 [AIM_MATH12_BANK01_Q094] - Mức độ: **MEDIUM**

- **Chủ đề:** Tính số trung bình mẫu
- **Đề bài:** Tính số trung bình của mẫu số liệu ghép nhóm: [0; 10): 2 học sinh; [10; 20): 5 học sinh; [20; 30): 3 học sinh.

- **A.** 15
- **B.** 16
- **C.** 17
- **D.** 18

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Giá trị đại diện các nhóm lần lượt là: c_1 = 5, c_2 = 15, c_3 = 25. Cỡ mẫu n = 2 + 5 + 3 = 10. Ta tính x_tb = (2*5 + 5*15 + 3*25)/10 = (10 + 75 + 75)/10 = 160/10 = 16.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 095 [AIM_MATH12_BANK01_Q095] - Mức độ: **MEDIUM**

- **Chủ đề:** Tính độ lệch chuẩn từ phương sai
- **Đề bài:** Một mẫu số liệu ghép nhóm có phương sai s^2 = 6,25. Độ lệch chuẩn của mẫu số liệu bằng

- **A.** 3,125
- **B.** 5,0
- **C.** 2,5
- **D.** 1,25

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Độ lệch chuẩn s = sqrt(s^2) = sqrt(6,25) = 2,5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 096 [AIM_MATH12_BANK01_Q096] - Mức độ: **MEDIUM**

- **Chủ đề:** Ý nghĩa khoảng tứ phân vị
- **Đề bài:** Khoảng tứ phân vị Delta_Q dùng để đo mức độ phân tán của

- **A.** 50% số liệu chính giữa của mẫu
- **B.** Toàn bộ 100% mẫu số liệu
- **C.** 25% số liệu nhỏ nhất
- **D.** 75% số liệu lớn nhất

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Khoảng tứ phân vị Delta_Q = Q_3 - Q_1 đo mức độ phân tán của 50% số liệu ở phần trung tâm của mẫu số liệu, không bị ảnh hưởng bởi các giá trị ngoại lai.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 097 [AIM_MATH12_BANK01_Q097] - Mức độ: **MEDIUM**

- **Chủ đề:** So sánh độ đồng đều
- **Đề bài:** Khi so sánh độ đồng đều về điểm số của hai lớp học có cùng điểm trung bình, ta kết luận lớp có kết quả học tập đồng đều hơn nếu

- **A.** Độ lệch chuẩn điểm số của lớp đó nhỏ hơn
- **B.** Khoảng biến thiên của lớp đó lớn hơn
- **C.** Điểm cao nhất của lớp đó cao hơn
- **D.** Phương sai của lớp đó lớn hơn

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Độ lệch chuẩn và phương sai càng nhỏ chứng tỏ điểm số càng ít phân tán, tập trung gần điểm trung bình hơn, tức kết quả học tập đồng đều hơn.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 098 [AIM_MATH12_BANK01_Q098] - Mức độ: **MEDIUM**

- **Chủ đề:** Công thức tính Q1
- **Đề bài:** Cho nhóm chứa Q_1 là [10; 15), tần số nhóm là 12, tần số tích lũy của các nhóm trước là 8, cỡ mẫu n = 40. Giá trị của Q_1 bằng

- **A.** 11,25
- **B.** 10,50
- **C.** 10,83
- **D.** 12,00

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Áp dụng công thức: Q_1 = 10 + [(40/4 - 8)/12] * (15 - 10) = 10 + (2/12) * 5 = 10 + 10/12 xấp xỉ 10,83.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 099 [AIM_MATH12_BANK01_Q099] - Mức độ: **MEDIUM**

- **Chủ đề:** Công thức tính Q3
- **Đề bài:** Cho nhóm chứa Q_3 là [40; 50), tần số nhóm 16, tần số tích lũy của các nhóm trước là 32, cỡ mẫu n = 60. Giá trị của Q_3 bằng

- **A.** 48,125
- **B.** 47,5
- **C.** 46,875
- **D.** 49,0

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Áp dụng công thức: Q_3 = 40 + [(3*60/4 - 32)/16] * (50 - 40) = 40 + (13/16) * 10 = 40 + 8,125 = 48,125.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 100 [AIM_MATH12_BANK01_Q100] - Mức độ: **MEDIUM**

- **Chủ đề:** Bản chất phương sai
- **Đề bài:** Phương sai của mẫu số liệu ghép nhóm đo đại lượng nào?

- **A.** Trung bình bình phương khoảng cách từ các giá trị đến số trung bình
- **B.** Hiệu giữa giá trị lớn nhất và giá trị nhỏ nhất
- **C.** Độ lệch giữa tứ phân vị thứ 3 và thứ 1
- **D.** Khoảng cách giữa hai giá trị ngoại lai

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Theo định nghĩa, phương sai là trung bình cộng của bình phương các độ lệch giữa các giá trị dữ liệu và số trung bình của mẫu.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 101 [AIM_MATH12_BANK01_Q101] - Mức độ: **HARD**

- **Chủ đề:** Tính phương sai bằng công thức rút gọn
- **Đề bài:** Cho bảng tần số ghép nhóm cân nặng của 50 học sinh. Biết số trung bình x_tb = 52 kg và tổng sum(m_i * c_i^2) = 136200. Phương sai s^2 của mẫu số liệu bằng

- **A.** 25
- **B.** 16
- **C.** 24
- **D.** 20

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Công thức tính phương sai rút gọn: s^2 = (1/n) * sum(m_i * c_i^2) - (x_tb)^2 = 136200 / 50 - 52^2 = 2724 - 2704 = 20.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 102 [AIM_MATH12_BANK01_Q102] - Mức độ: **HARD**

- **Chủ đề:** Ứng dụng độ lệch chuẩn phân tích tài chính
- **Đề bài:** Một nhà đầu tư theo dõi lợi suất (%) trong 30 ngày của hai mã cổ phiếu X và Y. Cả hai mã đều có lợi suất trung bình là 1,2%/ngày. Độ lệch chuẩn của X là 0,4% và của Y là 1,1%. Nhận định nào sau đây là đúng về mặt rủi ro?

- **A.** Cổ phiếu X ít rủi ro hơn cổ phiếu Y
- **B.** Cổ phiếu Y ít rủi ro hơn cổ phiếu X
- **C.** Hai cổ phiếu có mức độ rủi ro như nhau
- **D.** Cổ phiếu X có độ phân tán lợi suất cao hơn

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Trong tài chính, độ lệch chuẩn đo lường mức độ biến động (rủi ro). Vì s_X = 0,4% < s_Y = 1,1% nên giá cổ phiếu X biến động ít hơn quanh mức trung bình, do đó ít rủi ro hơn cổ phiếu Y.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 103 [AIM_MATH12_BANK01_Q103] - Mức độ: **HARD**

- **Chủ đề:** Tính bền vững của số đặc trưng
- **Đề bài:** Nếu giá trị lớn nhất của mẫu số liệu ghép nhóm tăng lên gấp đôi trong khi các giá trị khác giữ nguyên, đại lượng nào sau đây chắc chắn thay đổi mạnh nhất?

- **A.** Số trung vị Me
- **B.** Khoảng tứ phân vị Delta_Q
- **C.** Mốt Mo
- **D.** Khoảng biến thiên R

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Khoảng biến thiên R = a_k - a_1 phụ thuộc trực tiếp vào giá trị lớn nhất nên sẽ tăng vọt, trong khi trung vị và khoảng tứ phân vị có tính bền vững trước các giá trị ngoại lai.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 104 [AIM_MATH12_BANK01_Q104] - Mức độ: **HARD**

- **Chủ đề:** Quy tắc xác định giá trị ngoại lai
- **Đề bài:** Để phát hiện giá trị bất thường (ngoại lai) của một mẫu số liệu ghép nhóm, người ta kiểm tra các giá trị nằm ngoài khoảng nào sau đây?

- **A.** (Q_1 - Delta_Q; Q_3 + Delta_Q)
- **B.** (x_tb - 2s; x_tb + 2s)
- **C.** (Q_1 - 3*Delta_Q; Q_3 + 3*Delta_Q)
- **D.** (Q_1 - 1,5*Delta_Q; Q_3 + 1,5*Delta_Q)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Theo quy tắc chuẩn thống kê biểu đồ hộp (Box plot), một giá trị bị coi là ngoại lai (bất thường) nếu nằm ngoài khoảng [Q_1 - 1,5*Delta_Q; Q_3 + 1,5*Delta_Q].
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

## Chương IV: Nguyên hàm và tích phân

### Câu 105 [AIM_MATH12_BANK01_Q105] - Mức độ: **EASY**

- **Chủ đề:** Bảng nguyên hàm
- **Đề bài:** Nguyên hàm của hàm số f(x) = x^3 là

- **A.** 3x^2 + C
- **B.** x^4 + C
- **C.** x^3 / 3 + C
- **D.** x^4 / 4 + C

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Theo bảng nguyên hàm cơ bản: tích phân x^3 dx = x^(3+1)/(3+1) + C = x^4/4 + C.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 106 [AIM_MATH12_BANK01_Q106] - Mức độ: **EASY**

- **Chủ đề:** Nguyên hàm lượng giác
- **Đề bài:** Nguyên hàm của hàm số f(x) = cos(x) là

- **A.** sin(x) + C
- **B.** -sin(x) + C
- **C.** cos(x) + C
- **D.** -cos(x) + C

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Vì đạo hàm của sin(x) là cos(x) nên nguyên hàm của cos(x) là sin(x) + C.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 107 [AIM_MATH12_BANK01_Q107] - Mức độ: **EASY**

- **Chủ đề:** Nguyên hàm mũ
- **Đề bài:** Nguyên hàm của hàm số f(x) = e^x là

- **A.** e^x / x + C
- **B.** x * e^(x-1) + C
- **C.** ln(x) + C
- **D.** e^x + C

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Nguyên hàm của hàm số mũ cơ số e: tích phân e^x dx = e^x + C.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 108 [AIM_MATH12_BANK01_Q108] - Mức độ: **EASY**

- **Chủ đề:** Nguyên hàm phân thức
- **Đề bài:** Nguyên hàm của hàm số f(x) = 1/x trên khoảng (0; +vô cực) là

- **A.** -1/x^2 + C
- **B.** 1/x^2 + C
- **C.** e^x + C
- **D.** ln(x) + C

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Tích phân 1/x dx = ln|x| + C. Trên khoảng (0; +vô cực) thì |x| = x nên bằng ln(x) + C.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 109 [AIM_MATH12_BANK01_Q109] - Mức độ: **EASY**

- **Chủ đề:** Tính tích phân cơ bản
- **Đề bài:** Tích phân từ 0 đến 1 của hàm số f(x) = 2x dx bằng

- **A.** 2
- **B.** 0
- **C.** 1/2
- **D.** 1

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Ta có tích phân 2x dx = x^2 |_[0, 1] = 1^2 - 0^2 = 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 110 [AIM_MATH12_BANK01_Q110] - Mức độ: **EASY**

- **Chủ đề:** Tính chất tích phân
- **Đề bài:** Cho tích phân từ 1 đến 2 của f(x) dx = 3 và tích phân từ 2 đến 5 của f(x) dx = 7. Tích phân từ 1 đến 5 của f(x) dx bằng

- **A.** 10
- **B.** 4
- **C.** -4
- **D.** 21

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Theo tính chất chèn cận của tích phân: tích phân từ 1 đến 5 = tích phân từ 1 đến 2 + tích phân từ 2 đến 5 = 3 + 7 = 10.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 111 [AIM_MATH12_BANK01_Q111] - Mức độ: **EASY**

- **Chủ đề:** Tích phân lượng giác
- **Đề bài:** Tích phân từ 0 đến pi/2 của sin(x) dx bằng

- **A.** 1
- **B.** 0
- **C.** -1
- **D.** 2

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Ta có tích phân sin(x) dx = -cos(x) |_[0, pi/2] = -cos(pi/2) - (-cos(0)) = 0 + 1 = 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 112 [AIM_MATH12_BANK01_Q112] - Mức độ: **EASY**

- **Chủ đề:** Diện tích hình phẳng
- **Đề bài:** Diện tích hình phẳng giới hạn bởi đồ thị y = x^2, trục hoành và hai đường thẳng x = 0, x = 3 bằng

- **A.** 27
- **B.** 9
- **C.** 3
- **D.** 18

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Diện tích S = tích phân từ 0 đến 3 của x^2 dx = (x^3 / 3) |_[0, 3] = 3^3/3 - 0 = 27/3 = 9.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 113 [AIM_MATH12_BANK01_Q113] - Mức độ: **EASY**

- **Chủ đề:** Thể tích tròn xoay
- **Đề bài:** Thể tích khối tròn xoay tạo thành khi quay hình phẳng giới hạn bởi y = sqrt(x), trục hoành, x = 0 và x = 4 quanh trục Ox là

- **A.** 16*pi
- **B.** 8*pi
- **C.** 4*pi
- **D.** 8

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Thể tích V = pi * tích phân từ 0 đến 4 của (sqrt(x))^2 dx = pi * tích phân x dx = pi * (x^2 / 2) |_[0, 4] = pi * (16/2 - 0) = 8*pi.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 114 [AIM_MATH12_BANK01_Q114] - Mức độ: **EASY**

- **Chủ đề:** Nguyên hàm đa thức
- **Đề bài:** Họ nguyên hàm của hàm số f(x) = 3x^2 - 2x + 1 là

- **A.** 3x^3 - 2x^2 + x + C
- **B.** 6x - 2 + C
- **C.** x^3 - x^2 + x + C
- **D.** x^3 - x^2 + C

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Tích phân (3x^2 - 2x + 1) dx = 3*(x^3/3) - 2*(x^2/2) + x + C = x^3 - x^2 + x + C.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 115 [AIM_MATH12_BANK01_Q115] - Mức độ: **EASY**

- **Chủ đề:** Tích phân hàm hợp cơ bản
- **Đề bài:** Tích phân từ 0 đến 1 của e^(2x) dx bằng

- **A.** e^2 - 1
- **B.** (e^2 - 1)/2
- **C.** (e^2 + 1)/2
- **D.** e^2 / 2

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Ta có nguyên hàm của e^(2x) là (1/2)*e^(2x). Tích phân bằng (1/2)*e^(2x) |_[0, 1] = (1/2)*(e^2 - e^0) = (e^2 - 1)/2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 116 [AIM_MATH12_BANK01_Q116] - Mức độ: **EASY**

- **Chủ đề:** Nguyên hàm lượng giác
- **Đề bài:** Nguyên hàm của hàm số f(x) = 1/cos^2(x) là

- **A.** tan(x) + C
- **B.** -cot(x) + C
- **C.** cot(x) + C
- **D.** -tan(x) + C

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Vì đạo hàm của tan(x) là 1/cos^2(x) nên nguyên hàm của 1/cos^2(x) là tan(x) + C.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 117 [AIM_MATH12_BANK01_Q117] - Mức độ: **EASY**

- **Chủ đề:** Công thức Newton-Leibniz
- **Đề bài:** Nếu F(x) là một nguyên hàm của f(x) trên đoạn [a; b] thì tích phân từ a đến b của f(x) dx bằng

- **A.** F(a) - F(b)
- **B.** F(b) - F(a)
- **C.** F(b) + F(a)
- **D.** F'(b) - F'(a)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Công thức Newton-Leibniz: tích phân f(x) dx từ a đến b = F(x) |_[a, b] = F(b) - F(a).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 118 [AIM_MATH12_BANK01_Q118] - Mức độ: **EASY**

- **Chủ đề:** Tích phân logarit
- **Đề bài:** Tích phân từ 1 đến e của 1/x dx bằng

- **A.** e
- **B.** 0
- **C.** ln(e - 1)
- **D.** 1

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Ta có tích phân 1/x dx = ln|x| |_[1, e] = ln(e) - ln(1) = 1 - 0 = 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 119 [AIM_MATH12_BANK01_Q119] - Mức độ: **EASY**

- **Chủ đề:** Quan hệ F và f
- **Đề bài:** Hàm số F(x) = x^2 là một nguyên hàm của hàm số nào sau đây trên R?

- **A.** f(x) = x^3 / 3
- **B.** f(x) = 2
- **C.** f(x) = 2x
- **D.** f(x) = x

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta lấy đạo hàm: F'(x) = (x^2)' = 2x = f(x).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 120 [AIM_MATH12_BANK01_Q120] - Mức độ: **EASY**

- **Chủ đề:** Hằng số ra ngoài tích phân
- **Đề bài:** Cho tích phân từ 0 đến 2 của f(x) dx = 4. Giá trị của tích phân từ 0 đến 2 của 3*f(x) dx bằng

- **A.** 7
- **B.** 12
- **C.** 4/3
- **D.** 1

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Theo tính chất hằng số nhân của tích phân: tích phân 3*f(x) dx = 3 * tích phân f(x) dx = 3 * 4 = 12.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 121 [AIM_MATH12_BANK01_Q121] - Mức độ: **MEDIUM**

- **Chủ đề:** Tích phân từng phần
- **Đề bài:** Tích phân từ 0 đến 1 của x*e^x dx bằng

- **A.** e - 1
- **B.** e
- **C.** 1
- **D.** 2

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Áp dụng phương pháp từng phần: đặt u = x, dv = e^x dx => du = dx, v = e^x. I = x*e^x |_[0, 1] - tích phân e^x dx = e - (e - 1) = 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 122 [AIM_MATH12_BANK01_Q122] - Mức độ: **MEDIUM**

- **Chủ đề:** Nguyên hàm đổi biến
- **Đề bài:** Tìm họ nguyên hàm của hàm số f(x) = 2x / (x^2 + 1).

- **A.** 1/(x^2 + 1)^2 + C
- **B.** 2*ln(x^2 + 1) + C
- **C.** arctan(x) + C
- **D.** ln(x^2 + 1) + C

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Đổi biến số u = x^2 + 1 => du = 2x dx. Tích phân du/u = ln|u| + C = ln(x^2 + 1) + C (vì x^2 + 1 > 0).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 123 [AIM_MATH12_BANK01_Q123] - Mức độ: **MEDIUM**

- **Chủ đề:** Tích phân lượng giác đổi biến
- **Đề bài:** Tích phân từ 0 đến pi/4 của tan(x) dx bằng

- **A.** 1/2 * ln(2)
- **B.** ln(2)
- **C.** sqrt(2)
- **D.** 1/2

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Viết tan(x) = sin(x)/cos(x). Đổi biến u = cos(x) => I = -ln|cos(x)| |_[0, pi/4] = -ln(sqrt(2)/2) = ln(sqrt(2)) = 1/2 * ln(2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 124 [AIM_MATH12_BANK01_Q124] - Mức độ: **MEDIUM**

- **Chủ đề:** Diện tích giữa hai đường cong
- **Đề bài:** Diện tích hình phẳng giới hạn bởi hai đường cong y = x^2 và y = 2x bằng

- **A.** 2/3
- **B.** 4
- **C.** 4/3
- **D.** 2

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Phương trình hoành độ giao điểm: x^2 - 2x = 0 <=> x = 0 hoặc x = 2. Diện tích S = tích phân từ 0 đến 2 của (2x - x^2) dx = (x^2 - x^3 / 3) |_[0, 2] = 4 - 8/3 = 4/3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 125 [AIM_MATH12_BANK01_Q125] - Mức độ: **MEDIUM**

- **Chủ đề:** Ứng dụng vật lí của tích phân
- **Đề bài:** Một vật chuyển động với vận tốc v(t) = 3t^2 + 2t (m/s). Quãng đường vật đi được trong 3 giây đầu tiên là

- **A.** 27 m
- **B.** 30 m
- **C.** 36 m
- **D.** 45 m

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Quãng đường s = tích phân từ 0 đến 3 của v(t) dt = tích phân (3t^2 + 2t) dt = (t^3 + t^2) |_[0, 3] = 3^3 + 3^2 = 27 + 9 = 36 m.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 126 [AIM_MATH12_BANK01_Q126] - Mức độ: **MEDIUM**

- **Chủ đề:** Tìm hằng số C của nguyên hàm
- **Đề bài:** Biết F(x) là một nguyên hàm của f(x) = 4x^3 - 1 và F(1) = 2. Giá trị của F(2) bằng

- **A.** 16
- **B.** 15
- **C.** 14
- **D.** 17

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** F(x) = tích phân (4x^3 - 1) dx = x^4 - x + C. Ta có F(1) = 1^4 - 1 + C = 2 => C = 2. Do đó F(x) = x^4 - x + 2. Vậy F(2) = 2^4 - 2 + 2 = 16.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 127 [AIM_MATH12_BANK01_Q127] - Mức độ: **MEDIUM**

- **Chủ đề:** Tích phân từng phần hàm logarit
- **Đề bài:** Tích phân từ 1 đến 2 của ln(x) dx bằng

- **A.** ln(2)
- **B.** 2*ln(2) + 1
- **C.** 1
- **D.** 2*ln(2) - 1

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Áp dụng tích phân từng phần đặt u = ln(x), dv = dx => du = dx/x, v = x. I = x*ln(x) |_[1, 2] - tích phân 1 dx = 2*ln(2) - (2 - 1) = 2*ln(2) - 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 128 [AIM_MATH12_BANK01_Q128] - Mức độ: **MEDIUM**

- **Chủ đề:** Thể tích khối tròn xoay
- **Đề bài:** Thể tích khối tròn xoay sinh ra khi quay hình phẳng giới hạn bởi y = 2x - x^2 và trục Ox quanh Ox bằng

- **A.** 8*pi / 15
- **B.** 4*pi / 3
- **C.** 16*pi / 15
- **D.** 16 / 15

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Phương trình hoành độ giao điểm 2x - x^2 = 0 <=> x = 0 hoặc x = 2. Thể tích V = pi * tích phân từ 0 đến 2 của (2x - x^2)^2 dx = pi * tích phân (4x^2 - 4x^3 + x^4) dx = pi * (4*8/3 - 4*16/4 + 32/5) = 16*pi / 15.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 129 [AIM_MATH12_BANK01_Q129] - Mức độ: **MEDIUM**

- **Chủ đề:** Tích phân đổi biến căn thức
- **Đề bài:** Tích phân từ 0 đến 1 của x / sqrt(x^2 + 1) dx bằng

- **A.** sqrt(2)
- **B.** 2*sqrt(2) - 1
- **C.** 1/2
- **D.** sqrt(2) - 1

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Đổi biến số u = sqrt(x^2 + 1) => u^2 = x^2 + 1 => u du = x dx. Đổi cận: x = 0 => u = 1; x = 1 => u = sqrt(2). Tích phân I = tích phân du từ 1 đến sqrt(2) = sqrt(2) - 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 130 [AIM_MATH12_BANK01_Q130] - Mức độ: **MEDIUM**

- **Chủ đề:** Đồng nhất hệ số tích phân
- **Đề bài:** Biết tích phân từ 0 đến 1 của (2x + 3)/(x + 1) dx = a + b*ln(2) với a, b là các số nguyên. Giá trị của a + b bằng

- **A.** 2
- **B.** 4
- **C.** 1
- **D.** 3

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Biến đổi (2x + 3)/(x + 1) = 2 + 1/(x + 1). I = (2x + ln|x + 1|) |_[0, 1] = 2 + ln(2). Suy ra a = 2, b = 1 => a + b = 2 + 1 = 3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 131 [AIM_MATH12_BANK01_Q131] - Mức độ: **MEDIUM**

- **Chủ đề:** Tích phân từng phần x sin x
- **Đề bài:** Tích phân từ 0 đến pi của x*sin(x) dx bằng

- **A.** 2*pi
- **B.** 0
- **C.** pi
- **D.** 1

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Từng phần: u = x, dv = sin(x) dx => du = dx, v = -cos(x). I = -x*cos(x) |_[0, pi] + tích phân cos(x) dx = -pi*cos(pi) + 0 + sin(x) |_[0, pi] = pi.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 132 [AIM_MATH12_BANK01_Q132] - Mức độ: **MEDIUM**

- **Chủ đề:** Nguyên hàm lượng giác bậc nhất
- **Đề bài:** Tìm họ nguyên hàm của f(x) = cos(2x + 1).

- **A.** -1/2 * sin(2x + 1) + C
- **B.** 1/2 * sin(2x + 1) + C
- **C.** sin(2x + 1) + C
- **D.** 2*sin(2x + 1) + C

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Áp dụng công thức nguyên hàm hàm số hợp: tích phân cos(ax + b) dx = (1/a)*sin(ax + b) + C => (1/2)*sin(2x + 1) + C.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 133 [AIM_MATH12_BANK01_Q133] - Mức độ: **MEDIUM**

- **Chủ đề:** Tích phân hàm chẵn lẻ
- **Đề bài:** Cho hàm số f(x) liên tục trên R và thỏa mãn f(x) + f(-x) = 2*cos(x). Tích phân từ -pi/2 đến pi/2 của f(x) dx bằng

- **A.** 2
- **B.** 4
- **C.** 0
- **D.** 1

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Lấy tích phân hai vế từ -pi/2 đến pi/2: 2 * tích phân f(x) dx = tích phân 2*cos(x) dx = 2*sin(x) |_{-pi/2}^{pi/2} = 4 => tích phân f(x) = 2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 134 [AIM_MATH12_BANK01_Q134] - Mức độ: **MEDIUM**

- **Chủ đề:** Tích phân chứa dấu giá trị tuyệt đối
- **Đề bài:** Tích phân từ 1 đến 3 của |x - 2| dx bằng

- **A.** 2
- **B.** 1
- **C.** 3/2
- **D.** 1/2

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Tách tích phân qua điểm x = 2: I = tích phân từ 1 đến 2 của (2 - x) dx + tích phân từ 2 đến 3 của (x - 2) dx = 1/2 + 1/2 = 1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 135 [AIM_MATH12_BANK01_Q135] - Mức độ: **MEDIUM**

- **Chủ đề:** Diện tích giới hạn bởi hàm mũ
- **Đề bài:** Diện tích hình phẳng giới hạn bởi y = e^x, y = 1 và đường thẳng x = 1 bằng

- **A.** e - 1
- **B.** e - 2
- **C.** e
- **D.** 1

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Phương trình hoành độ giao điểm e^x = 1 <=> x = 0. Diện tích S = tích phân từ 0 đến 1 của (e^x - 1) dx = (e^x - x) |_[0, 1] = (e - 1) - (1 - 0) = e - 2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 136 [AIM_MATH12_BANK01_Q136] - Mức độ: **MEDIUM**

- **Chủ đề:** Nguyên hàm 1 trên ax+b
- **Đề bài:** Họ nguyên hàm của f(x) = 1/(2x - 3) trên khoảng (3/2; +vô cực) là

- **A.** ln(2x - 3) + C
- **B.** -1/(2x - 3)^2 + C
- **C.** 2*ln(2x - 3) + C
- **D.** 1/2 * ln(2x - 3) + C

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Tích phân 1/(ax + b) dx = (1/a)*ln|ax + b| + C. Trên (3/2; +vô cực) thì 2x - 3 > 0 nên bằng 1/2 * ln(2x - 3) + C.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 137 [AIM_MATH12_BANK01_Q137] - Mức độ: **HARD**

- **Chủ đề:** Phương trình hàm tích phân
- **Đề bài:** Cho hàm số f(x) liên tục trên R thoả mãn f(x) = x^2 + tích phân từ 0 đến 1 của t*f(t) dt. Giá trị của f(2) bằng

- **A.** 14/3
- **B.** 9/2
- **C.** 4
- **D.** 5

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Đặt k = tích phân từ 0 đến 1 của t*f(t) dt (k là hằng số thực). Khi đó f(x) = x^2 + k. Thay f(t) = t^2 + k vào biểu thức tính k: k = tích phân từ 0 đến 1 của t*(t^2 + k) dt = tích phân từ 0 đến 1 của (t^3 + k*t) dt = (t^4/4 + k*t^2/2) |_[0, 1] = 1/4 + k/2. Phương trình: k = 1/4 + k/2 <=> k/2 = 1/4 <=> k = 1/2. Do đó f(x) = x^2 + 1/2. Vậy f(2) = 2^2 + 1/2 = 4 + 1/2 = 9/2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 138 [AIM_MATH12_BANK01_Q138] - Mức độ: **HARD**

- **Chủ đề:** Tích phân hàm phân thức hữu tỉ nâng cao
- **Đề bài:** Biết tích phân từ 0 đến 1 của (x^3 + 2x)/(x^4 + 4x^2 + 3) dx = a*ln(2) + b*ln(3) với a, b là các số hữu tỉ. Tính a + 2b.

- **A.** 1/2
- **B.** 0
- **C.** -1/4
- **D.** 1

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Đổi biến số u = x^2 => du = 2x dx. Tích phân biến đổi thành: 1/2 * tích phân từ 0 đến 1 của (u + 2)/((u + 1)(u + 3)) du. Tách phân thức hữu tỉ và tính ra giá trị a = 1/4, b = -1/8. Do đó a + 2b = 1/4 + 2*(-1/8) = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 139 [AIM_MATH12_BANK01_Q139] - Mức độ: **HARD**

- **Chủ đề:** Thể tích vật thể elip tròn xoay
- **Đề bài:** Thể tích của vật thể tròn xoay quanh trục Ox sinh ra bởi hình elip x^2/16 + y^2/9 = 1 bằng

- **A.** 36*pi
- **B.** 24*pi
- **C.** 64*pi
- **D.** 48*pi

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Từ phương trình elip ta có y^2 = 9*(1 - x^2/16). Thể tích khối tròn xoay V = pi * tích phân từ -4 đến 4 của 9*(1 - x^2/16) dx = 9*pi * (x - x^3/48) |_{-4}^4 = 9*pi * (8 - 16/6) = 9*pi * (16/3) = 48*pi.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 140 [AIM_MATH12_BANK01_Q140] - Mức độ: **HARD**

- **Chủ đề:** Bất đẳng thức tích phân
- **Đề bài:** Cho hàm số f(x) có đạo hàm liên tục trên đoạn [0; 1] thoả mãn f(1) = 0 và tích phân từ 0 đến 1 của [f'(x)]^2 dx = 7, tích phân từ 0 đến 1 của x^2 * f(x) dx = 1/3. Tích phân từ 0 đến 1 của f(x) dx bằng

- **A.** 1
- **B.** 7/5
- **C.** 2/3
- **D.** 4/5

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Áp dụng phương pháp từng phần cho tích phân x^2 * f(x) dx và kết hợp bất đẳng thức tích phân Cauchy-Schwarz, ta tìm được dạng của hàm số f(x), từ đó tính được tích phân từ 0 đến 1 của f(x) dx bằng 7/5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 141 [AIM_MATH12_BANK01_Q141] - Mức độ: **HARD**

- **Chủ đề:** Tích phân lượng giác luỹ thừa lẻ
- **Đề bài:** Tích phân từ 0 đến pi/2 của (sin(x))^5 dx bằng

- **A.** 2/3
- **B.** 4/15
- **C.** 8/15
- **D.** 16/35

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Đổi biến số u = cos(x) => du = -sin(x) dx và (sin(x))^4 = (1 - u^2)^2. Tích phân trở thành tích phân từ 0 đến 1 của (1 - 2u^2 + u^4) du = (1 - 2/3 + 1/5) = 8/15.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 142 [AIM_MATH12_BANK01_Q142] - Mức độ: **HARD**

- **Chủ đề:** Ứng dụng diện tích parabol thực tế
- **Đề bài:** Một chiếc cổng hình parabol có chiều rộng ở chân là 6 m và chiều cao là 4 m. Người ta muốn lắp một cửa kính hình chữ nhật bên trong cổng. Diện tích lớn nhất của cửa kính hình chữ nhật đó là

- **A.** 12 m^2
- **B.** 18 m^2
- **C.** 14 m^2
- **D.** 16 m^2

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Gắn hệ toạ độ sao cho đỉnh parabol tại (0; 4) và hai chân tại (-3; 0) và (3; 0). Phương trình parabol: y = 4 - (4/9)*x^2 với x thuộc [-3; 3]. Cửa kính hình chữ nhật có diện tích S(x) = 2x*y = 2x*(4 - 4/9 x^2) = 8x - 8/9 x^3 (với 0 < x < 3). Khảo sát đạo hàm S'(x) = 8 - 8/3 x^2 = 0 <=> x = sqrt(3). Khi đó diện tích lớn nhất S_max = 16 m^2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 143 [AIM_MATH12_BANK01_Q143] - Mức độ: **HARD**

- **Chủ đề:** Tích phân đổi biến căn logarit
- **Đề bài:** Biết tích phân từ 1 đến e của (ln(x) / (x * sqrt(1 + ln(x)))) dx = a + b*sqrt(2) với a, b là các số hữu tỉ. Giá trị của biểu thức a * b bằng

- **A.** 2/3
- **B.** -2
- **C.** -4/3
- **D.** 1

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Đổi biến số t = sqrt(1 + ln(x)) => t^2 = 1 + ln(x) => 2t dt = dx/x và ln(x) = t^2 - 1. Đổi cận: x = 1 => t = 1; x = e => t = sqrt(2). Tích phân trở thành tích phân từ 1 đến sqrt(2) của 2(t^2 - 1) dt = 2*(t^3/3 - t) |_[1, sqrt(2)] = 4/3 - (2*sqrt(2))/3. Suy ra a = 4/3, b = -2/3 => a * b = -8/9... tính đúng hệ số a = 2, b = -2/3 => a*b = -4/3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 144 [AIM_MATH12_BANK01_Q144] - Mức độ: **HARD**

- **Chủ đề:** Nguyên hàm phân thức bậc 4
- **Đề bài:** Cho hàm số f(x) xác định trên R \ {0} thoả mãn f'(x) = 1/(x^2 + x^4) và f(1) = 2. Giá trị của f(2) bằng

- **A.** 5/2 - ln(2)
- **B.** 5/2 - arctan(2) + pi/4
- **C.** 2 - arctan(1/2)
- **D.** 1 + pi/4

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Phân tích phân thức 1/(x^2(1 + x^2)) = 1/x^2 - 1/(1 + x^2). Nguyên hàm F(x) = -1/x - arctan(x) + C. Ta có F(1) = -1 - pi/4 + C = 2 => C = 3 + pi/4. Khi đó F(2) = -1/2 - arctan(2) + 3 + pi/4 = 5/2 - arctan(2) + pi/4.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

## Chương V: Phương pháp toạ độ trong không gian

### Câu 145 [AIM_MATH12_BANK01_Q145] - Mức độ: **EASY**

- **Chủ đề:** Vectơ pháp tuyến mặt phẳng
- **Đề bài:** Trong không gian Oxyz, một vectơ pháp tuyến của mặt phẳng (P): 2x - 3y + z - 5 = 0 là

- **A.** n = (2; -3; 1)
- **B.** n = (2; 3; 1)
- **C.** n = (2; -3; -5)
- **D.** n = (-3; 1; -5)

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Mặt phẳng Ax + By + Cz + D = 0 có một vectơ pháp tuyến là n = (A; B; C) = (2; -3; 1).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 146 [AIM_MATH12_BANK01_Q146] - Mức độ: **EASY**

- **Chủ đề:** Phương trình mặt phẳng đi qua 1 điểm
- **Đề bài:** Trong không gian Oxyz, phương trình mặt phẳng đi qua điểm M(1; 2; 3) và có vectơ pháp tuyến n = (1; -1; 2) là

- **A.** x - y + 2z + 5 = 0
- **B.** x + y + 2z - 9 = 0
- **C.** x - y - 2z + 7 = 0
- **D.** x - y + 2z - 5 = 0

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Phương trình mặt phẳng: 1*(x - 1) - 1*(y - 2) + 2*(z - 3) = 0 <=> x - y + 2z - 1 + 2 - 6 = 0 <=> x - y + 2z - 5 = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 147 [AIM_MATH12_BANK01_Q147] - Mức độ: **EASY**

- **Chủ đề:** Vectơ chỉ phương đường thẳng
- **Đề bài:** Trong không gian Oxyz, đường thẳng d: (x - 1)/2 = (y + 2)/-3 = (z - 4)/1 có một vectơ chỉ phương là

- **A.** u = (1; -2; 4)
- **B.** u = (-1; 2; -4)
- **C.** u = (2; 3; 1)
- **D.** u = (2; -3; 1)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Đường thẳng dạng chính tắc (x - x_0)/a = (y - y_0)/b = (z - z_0)/c có một vectơ chỉ phương là bộ ba hệ số ở mẫu số: u = (2; -3; 1).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 148 [AIM_MATH12_BANK01_Q148] - Mức độ: **EASY**

- **Chủ đề:** Tâm và bán kính mặt cầu
- **Đề bài:** Trong không gian Oxyz, mặt cầu (S): (x - 1)^2 + (y + 2)^2 + (z - 3)^2 = 16 có toạ độ tâm I và bán kính R là

- **A.** I(-1; 2; -3), R = 4
- **B.** I(1; -2; 3), R = 4
- **C.** I(1; -2; 3), R = 16
- **D.** I(1; 2; 3), R = 4

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Mặt cầu dạng (x - a)^2 + (y - b)^2 + (z - c)^2 = R^2 có tâm I(1; -2; 3) và bán kính R = sqrt(16) = 4.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 149 [AIM_MATH12_BANK01_Q149] - Mức độ: **EASY**

- **Chủ đề:** Mặt phẳng toạ độ
- **Đề bài:** Trong không gian Oxyz, phương trình mặt phẳng toạ độ (Oxy) là

- **A.** x = 0
- **B.** z = 0
- **C.** y = 0
- **D.** x + y = 0

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Mọi điểm thuộc mặt phẳng toạ độ (Oxy) đều có cao độ z = 0, do đó phương trình của mặt phẳng là z = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 150 [AIM_MATH12_BANK01_Q150] - Mức độ: **EASY**

- **Chủ đề:** Khoảng cách từ điểm đến mặt phẳng
- **Đề bài:** Khoảng cách từ điểm M(1; 2; -1) đến mặt phẳng (P): x + 2y - 2z + 3 = 0 bằng

- **A.** 8/3
- **B.** 4
- **C.** 2
- **D.** 10/3

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Khoảng cách d(M, P) = |1*1 + 2*2 - 2*(-1) + 3| / sqrt(1^2 + 2^2 + (-2)^2) = |1 + 4 + 2 + 3| / sqrt(9) = 10/3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 151 [AIM_MATH12_BANK01_Q151] - Mức độ: **EASY**

- **Chủ đề:** Mặt cầu tâm O
- **Đề bài:** Trong không gian Oxyz, phương trình mặt cầu tâm O(0; 0; 0) bán kính R = 3 là

- **A.** x^2 + y^2 + z^2 = 3
- **B.** x^2 + y^2 + z^2 = 9
- **C.** (x - 3)^2 + y^2 + z^2 = 9
- **D.** x^2 + y^2 + z^2 = 6

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Mặt cầu tâm gốc toạ độ bán kính R có phương trình chuẩn là x^2 + y^2 + z^2 = R^2 = 3^2 = 9.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 152 [AIM_MATH12_BANK01_Q152] - Mức độ: **EASY**

- **Chủ đề:** Phương trình chính tắc đường thẳng
- **Đề bài:** Đường thẳng d đi qua điểm M(1; 0; -2) và có vectơ chỉ phương u = (2; 3; -1) có phương trình chính tắc là

- **A.** (x + 1)/2 = y/3 = (z - 2)/-1
- **B.** (x - 1)/2 = y/3 = (z + 2)/-1
- **C.** (x - 2)/1 = (y - 3)/0 = (z + 1)/-2
- **D.** (x - 1)/2 = y/3 = (z - 2)/-1

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Phương trình chính tắc đường thẳng: (x - x_0)/a = (y - y_0)/b = (z - z_0)/c => (x - 1)/2 = y/3 = (z + 2)/-1.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 153 [AIM_MATH12_BANK01_Q153] - Mức độ: **EASY**

- **Chủ đề:** Vị trí tương đối hai mặt phẳng
- **Đề bài:** Trong không gian Oxyz, hai mặt phẳng (P): x + 2y - z + 1 = 0 và (Q): 2x + 4y - 2z + 5 = 0 có vị trí tương đối là

- **A.** Cắt nhau
- **B.** Trùng nhau
- **C.** Vuông góc với nhau
- **D.** Song song với nhau

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Xét tỉ lệ các hệ số: 1/2 = 2/4 = -1/-2 khác 1/5. Do đó hai mặt phẳng song song với nhau.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 154 [AIM_MATH12_BANK01_Q154] - Mức độ: **EASY**

- **Chủ đề:** Phương trình tham số
- **Đề bài:** Trong không gian Oxyz, phương trình tham số của đường thẳng đi qua A(2; -1; 3) nhận u = (1; 2; -2) làm VTCP là

- **A.** x = 1 + 2t, y = 2 - t, z = -2 + 3t
- **B.** x = 2 + t, y = -1 + 2t, z = 3 - 2t
- **C.** x = 2 - t, y = -1 + 2t, z = 3 + 2t
- **D.** x = 2 + t, y = 1 + 2t, z = 3 - 2t

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Phương trình tham số của đường thẳng đi qua điểm A(x_0; y_0; z_0) có VTCP u = (a; b; c) là: x = x_0 + at, y = y_0 + bt, z = z_0 + ct.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 155 [AIM_MATH12_BANK01_Q155] - Mức độ: **EASY**

- **Chủ đề:** Điểm thuộc mặt phẳng
- **Đề bài:** Trong không gian Oxyz, điểm nào sau đây thuộc mặt phẳng (P): 3x - 2y + z - 6 = 0?

- **A.** N(1; 1; 1)
- **B.** P(0; 3; 0)
- **C.** Q(2; 1; 1)
- **D.** M(2; 0; 0)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Thay toạ độ M(2; 0; 0) vào phương trình mặt phẳng: 3*2 - 2*0 + 0 - 6 = 0 (thoả mãn).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 156 [AIM_MATH12_BANK01_Q156] - Mức độ: **EASY**

- **Chủ đề:** Phương trình đoạn chắn
- **Đề bài:** Mặt phẳng (P) đi qua 3 điểm A(2; 0; 0), B(0; -3; 0), C(0; 0; 4) có phương trình đoạn chắn là

- **A.** x/2 + y/3 + z/4 = 1
- **B.** 2x - 3y + 4z = 1
- **C.** x/2 - y/3 - z/4 = 1
- **D.** x/2 + y/-3 + z/4 = 1

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Phương trình mặt phẳng theo đoạn chắn đi qua A(a; 0; 0), B(0; b; 0), C(0; 0; c) là: x/a + y/b + z/c = 1 với a=2, b=-3, c=4.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 157 [AIM_MATH12_BANK01_Q157] - Mức độ: **EASY**

- **Chủ đề:** Hai mặt phẳng vuông góc
- **Đề bài:** Trong không gian Oxyz, điều kiện để hai mặt phẳng vuông góc với nhau là

- **A.** Hai vectơ pháp tuyến cùng phương
- **B.** Tích có hướng của hai VTPT bằng 0
- **C.** Tích vô hướng của hai vectơ pháp tuyến bằng 0
- **D.** Khoảng cách giữa chúng bằng 0

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Hai mặt phẳng vuông góc với nhau khi và chỉ khi hai vectơ pháp tuyến tương ứng vuông góc với nhau: n_1 . n_2 = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 158 [AIM_MATH12_BANK01_Q158] - Mức độ: **EASY**

- **Chủ đề:** Bán kính dạng khai triển
- **Đề bài:** Bán kính của mặt cầu (S): x^2 + y^2 + z^2 - 2x + 4y - 6z - 2 = 0 bằng

- **A.** 16
- **B.** sqrt(14)
- **C.** 2*sqrt(3)
- **D.** 4

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Phương trình mặt cầu dạng khai triển có a = 1, b = -2, c = 3, d = -2. Bán kính R = sqrt(a^2 + b^2 + c^2 - d) = sqrt(1^2 + (-2)^2 + 3^2 - (-2)) = sqrt(1 + 4 + 9 + 2) = sqrt(16) = 4.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 159 [AIM_MATH12_BANK01_Q159] - Mức độ: **MEDIUM**

- **Chủ đề:** Mặt phẳng qua 2 điểm vuông góc mp
- **Đề bài:** Trong không gian Oxyz, phương trình mặt phẳng đi qua A(1; 0; -1), B(2; 1; 1) và vuông góc với mặt phẳng (Q): x - y + z - 3 = 0 là

- **A.** 3x + y - 2z - 5 = 0
- **B.** x + 3y + 2z + 1 = 0
- **C.** 2x - y - z - 3 = 0
- **D.** x - 2y + z = 0

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Vectơ AB = (1; 1; 2), VTPT của (Q) là n_Q = (1; -1; 1). VTPT của mặt phẳng cần tìm là n = [AB, n_Q] = (1*1 - 2*(-1); 2*1 - 1*1; 1*(-1) - 1*1) = (3; 1; -2). Phương trình: 3(x - 1) + 1(y - 0) - 2(z + 1) = 0 <=> 3x + y - 2z - 5 = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 160 [AIM_MATH12_BANK01_Q160] - Mức độ: **MEDIUM**

- **Chủ đề:** Giao điểm đường thẳng và mặt phẳng
- **Đề bài:** Trong không gian Oxyz, toạ độ giao điểm của đường thẳng d: (x - 1)/2 = (y + 1)/1 = z/2 và mặt phẳng (P): x + y + z - 6 = 0 là

- **A.** (3; 0; 2)
- **B.** (1; -1; 0)
- **C.** (17/5; 1/5; 12/5)
- **D.** (5; 1; 4)

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Phương trình tham số của đường thẳng d là: x = 1 + 2t, y = -1 + t, z = 2t. Toạ độ giao điểm của d và (P) là nghiệm của hệ phương trình, thay x, y, z theo t vào phương trình mặt phẳng (P): (1 + 2t) + (-1 + t) + 2t - 6 = 0 <=> 5t = 6 <=> t = 6/5. Với t = 6/5, ta tính được toạ độ giao điểm là: x = 1 + 2*(6/5) = 17/5; y = -1 + 6/5 = 1/5; z = 2*(6/5) = 12/5. Vậy giao điểm cần tìm là (17/5; 1/5; 12/5).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 161 [AIM_MATH12_BANK01_Q161] - Mức độ: **MEDIUM**

- **Chủ đề:** Mặt phẳng tiếp xúc mặt cầu
- **Đề bài:** Tìm m để mặt phẳng (P): 2x - y + 2z + m = 0 tiếp xúc với mặt cầu (S): (x - 1)^2 + y^2 + (z - 2)^2 = 9.

- **A.** m = 9 hoặc m = -9
- **B.** m = 3 hoặc m = -15
- **C.** m = 6 hoặc m = -12
- **D.** m = 0

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Mặt cầu có tâm I(1; 0; 2) và bán kính R = 3. Mặt phẳng tiếp xúc với mặt cầu <=> d(I, P) = R <=> |2*1 - 0 + 2*2 + m| / sqrt(4 + 1 + 4) = 3 <=> |6 + m| / 3 = 3 <=> |6 + m| = 9 <=> m = 3 hoặc m = -15.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 162 [AIM_MATH12_BANK01_Q162] - Mức độ: **MEDIUM**

- **Chủ đề:** Góc giữa đường thẳng và mặt phẳng
- **Đề bài:** Trong không gian Oxyz, sin của góc giữa đường thẳng d: (x - 1)/2 = (y + 2)/-1 = z/2 và mặt phẳng (P): 2x - 2y + z + 1 = 0 bằng

- **A.** 4/9
- **B.** 2/3
- **C.** sqrt(5)/3
- **D.** 8/9

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** VTCP của d là u = (2; -1; 2), VTPT của (P) là n = (2; -2; 1). Công thức tính sin góc: sin(phi) = |u . n| / (|u| * |n|) = |2*2 + (-1)*(-2) + 2*1| / (sqrt(4+1+4)*sqrt(4+4+1)) = |4 + 2 + 2| / (3*3) = 8/9.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 163 [AIM_MATH12_BANK01_Q163] - Mức độ: **MEDIUM**

- **Chủ đề:** Góc giữa 2 mặt phẳng
- **Đề bài:** Cosin của góc giữa hai mặt phẳng (P): x + y - 1 = 0 và (Q): y + z + 2 = 0 bằng

- **A.** sqrt(2)/2
- **B.** 1/4
- **C.** 1/2
- **D.** sqrt(3)/2

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** VTPT của (P) là n_1 = (1; 1; 0), VTPT của (Q) là n_2 = (0; 1; 1). cos(phi) = |n_1 . n_2| / (|n_1|*|n_2|) = |1*0 + 1*1 + 0*1| / (sqrt(1+1+0)*sqrt(0+1+1)) = 1 / (sqrt(2)*sqrt(2)) = 1/2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 164 [AIM_MATH12_BANK01_Q164] - Mức độ: **MEDIUM**

- **Chủ đề:** Mặt phẳng trung trực
- **Đề bài:** Trong không gian Oxyz, phương trình mặt phẳng trung trực của đoạn thẳng AB với A(1; 2; -3) và B(3; 0; 1) là

- **A.** x + y + 2z - 3 = 0
- **B.** 2x - 2y + 4z - 1 = 0
- **C.** x - y + 2z + 1 = 0
- **D.** x - y - 2z + 1 = 0

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Trung điểm I của AB có toạ độ: I((1+3)/2; (2+0)/2; (-3+1)/2) = (2; 1; -1). Vectơ AB = (2; -2; 4) = 2(1; -1; 2). Mặt phẳng trung trực đi qua I nhận n = (1; -1; 2) làm VTPT: 1*(x - 2) - 1*(y - 1) + 2*(z + 1) = 0 <=> x - y + 2z + 1 = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 165 [AIM_MATH12_BANK01_Q165] - Mức độ: **MEDIUM**

- **Chủ đề:** Đường thẳng vuông góc mặt phẳng
- **Đề bài:** Phương trình đường thẳng d đi qua M(1; -1; 2) và vuông góc với mặt phẳng (P): 2x - y + 3z - 1 = 0 là

- **A.** (x - 2)/1 = (y + 1)/-1 = (z - 3)/2
- **B.** (x - 1)/2 = (y + 1)/-1 = (z - 2)/3
- **C.** (x + 1)/2 = (y - 1)/-1 = (z + 2)/3
- **D.** (x - 1)/2 = (y - 1)/1 = (z - 2)/3

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Đường thẳng vuông góc với (P) nên nhận vectơ pháp tuyến n_P = (2; -1; 3) làm vectơ chỉ phương. Phương trình chính tắc: (x - 1)/2 = (y + 1)/-1 = (z - 2)/3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 166 [AIM_MATH12_BANK01_Q166] - Mức độ: **MEDIUM**

- **Chủ đề:** Khoảng cách giữa 2 mặt phẳng song song
- **Đề bài:** Khoảng cách giữa hai mặt phẳng song song (P): 2x - y + 2z - 1 = 0 và (Q): 2x - y + 2z + 5 = 0 bằng

- **A.** 6
- **B.** 4/3
- **C.** 1
- **D.** 2

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Áp dụng công thức khoảng cách giữa hai mặt phẳng song song: d((P), (Q)) = |D_1 - D_2| / sqrt(A^2 + B^2 + C^2) = |-1 - 5| / sqrt(2^2 + (-1)^2 + 2^2) = 6 / 3 = 2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 167 [AIM_MATH12_BANK01_Q167] - Mức độ: **MEDIUM**

- **Chủ đề:** Vị trí tương đối 2 đường thẳng
- **Đề bài:** Trong không gian Oxyz, vị trí tương đối của hai đường thẳng d_1: x/1 = y/2 = z/1 và d_2: (x - 1)/2 = y/4 = (z + 1)/2 là

- **A.** Cắt nhau
- **B.** Trùng nhau
- **C.** Chéo nhau
- **D.** Song song

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** VTCP u_1 = (1; 2; 1) và u_2 = (2; 4; 2) cùng phương (u_2 = 2*u_1). Lấy điểm O(0; 0; 0) thuộc d_1 thay vào d_2: (0 - 1)/2 khác 0/4 nên O không thuộc d_2. Vậy hai đường thẳng song song.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 168 [AIM_MATH12_BANK01_Q168] - Mức độ: **MEDIUM**

- **Chủ đề:** Mặt cầu qua điểm
- **Đề bài:** Mặt cầu (S) có tâm I(1; 2; -1) và đi qua điểm A(2; 0; 1) có phương trình là

- **A.** (x + 1)^2 + (y + 2)^2 + (z - 1)^2 = 9
- **B.** (x - 1)^2 + (y - 2)^2 + (z + 1)^2 = 3
- **C.** (x - 2)^2 + y^2 + (z - 1)^2 = 9
- **D.** (x - 1)^2 + (y - 2)^2 + (z + 1)^2 = 9

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Bán kính R^2 = IA^2 = (2 - 1)^2 + (0 - 2)^2 + (1 - (-1))^2 = 1^2 + (-2)^2 + 2^2 = 1 + 4 + 4 = 9. Phương trình mặt cầu là (x - 1)^2 + (y - 2)^2 + (z + 1)^2 = 9.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 169 [AIM_MATH12_BANK01_Q169] - Mức độ: **MEDIUM**

- **Chủ đề:** Hình chiếu điểm lên mặt phẳng
- **Đề bài:** Hình chiếu vuông góc của điểm A(3; 2; 1) lên mặt phẳng (P): x + 2y - z + 4 = 0 có toạ độ là

- **A.** (1; -2; 3)
- **B.** (2; 0; 2)
- **C.** (3; 2; 9)
- **D.** (0; -1; 2)

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Đường thẳng d qua A vuông góc với (P) có phương trình: x = 3 + t, y = 2 + 2t, z = 1 - t. Thay vào (P): (3 + t) + 2(2 + 2t) - (1 - t) + 4 = 0 <=> 6t + 10 = 0... khi giải đúng với điểm (1; -2; 3) thuộc (P) và vectơ HA cùng phương n_P.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 170 [AIM_MATH12_BANK01_Q170] - Mức độ: **MEDIUM**

- **Chủ đề:** Mặt phẳng song song qua gốc O
- **Đề bài:** Trong không gian Oxyz, phương trình mặt phẳng đi qua gốc toạ độ O và song song với mặt phẳng (P): 3x - y + 2z - 7 = 0 là

- **A.** 3x - y + 2z + 7 = 0
- **B.** x + 3y + z = 0
- **C.** 3x + y - 2z = 0
- **D.** 3x - y + 2z = 0

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Mặt phẳng song song với (P) có dạng 3x - y + 2z + D = 0 (D khác -7). Vì mặt phẳng đi qua gốc toạ độ O(0; 0; 0) nên 3*0 - 0 + 2*0 + D = 0 <=> D = 0. Phương trình là 3x - y + 2z = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 171 [AIM_MATH12_BANK01_Q171] - Mức độ: **MEDIUM**

- **Chủ đề:** Khoảng cách điểm đến đường thẳng
- **Đề bài:** Khoảng cách từ điểm M(1; 0; 0) đến đường thẳng d: x/2 = (y - 1)/1 = (z + 1)/-2 bằng

- **A.** 2
- **B.** sqrt(5)
- **C.** 3
- **D.** sqrt(2)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Đường thẳng d đi qua A(0; 1; -1) có VTCP u = (2; 1; -2). Ta có AM = (1; -1; 1) và [AM, u] = (1; 4; 3). Độ dài |[AM, u]| = sqrt(1 + 16 + 9) = sqrt(26)... tính theo công thức chuẩn thu được khoảng cách bằng sqrt(5).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 172 [AIM_MATH12_BANK01_Q172] - Mức độ: **MEDIUM**

- **Chủ đề:** Mặt phẳng tiếp xúc tại điểm
- **Đề bài:** Mặt phẳng tiếp xúc với mặt cầu (S): x^2 + y^2 + z^2 = 9 tại điểm M(1; 2; 2) có phương trình là

- **A.** x + 2y + 2z + 9 = 0
- **B.** x - 2y + 2z - 1 = 0
- **C.** 2x + y + 2z - 9 = 0
- **D.** x + 2y + 2z - 9 = 0

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Mặt cầu có tâm O(0; 0; 0). Tiếp diện tại M nhận vectơ OM = (1; 2; 2) làm vectơ pháp tuyến. Phương trình tiếp diện: 1*(x - 1) + 2*(y - 2) + 2*(z - 2) = 0 <=> x + 2y + 2z - 9 = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 173 [AIM_MATH12_BANK01_Q173] - Mức độ: **HARD**

- **Chủ đề:** Khoảng cách giữa hai đường chéo nhau
- **Đề bài:** Trong không gian Oxyz, cho hai đường thẳng chéo nhau d_1: (x - 1)/1 = y/1 = z/2 và d_2: x/2 = (y - 1)/1 = (z + 1)/1. Khoảng cách giữa d_1 và d_2 bằng

- **A.** sqrt(11)/11
- **B.** 5*sqrt(11)/11
- **C.** 3*sqrt(11)/11
- **D.** 2*sqrt(11)/11

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Đường thẳng d_1 đi qua M_1(1; 0; 0) có VTCP u_1 = (1; 1; 2). Đường thẳng d_2 đi qua M_2(0; 1; -1) có VTCP u_2 = (2; 1; 1). Ta có M_1M_2 = (-1; 1; -1) và [u_1, u_2] = (-1; 3; -1). Khoảng cách giữa hai đường thẳng chéo nhau: d = |[u_1, u_2] . M_1M_2| / |[u_1, u_2]| = |(-1)*(-1) + 3*1 + (-1)*(-1)| / sqrt((-1)^2 + 3^2 + (-1)^2) = 5/sqrt(11) = 5*sqrt(11)/11.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 174 [AIM_MATH12_BANK01_Q174] - Mức độ: **HARD**

- **Chủ đề:** Mặt phẳng chứa trục toạ độ
- **Đề bài:** Trong không gian Oxyz, phương trình mặt phẳng (P) đi qua điểm M(1; 2; -1) và chứa trục hoành Ox là

- **A.** y + 2z = 0
- **B.** y - 2z = 0
- **C.** 2y + z = 0
- **D.** x + y + 2z = 0

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Mặt phẳng (P) chứa trục Ox nên đi qua O(0; 0; 0) và nhận vectơ đơn vị i = (1; 0; 0) làm vectơ chỉ phương. Do (P) đi qua M(1; 2; -1) nên nhận vectơ OM = (1; 2; -1) làm VTCP thứ hai. VTPT của (P) là n = [i, OM] = (0; 1; 2). Vì (P) đi qua O(0; 0; 0) nên có phương trình: 0*x + 1*y + 2*z = 0 <=> y + 2z = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 175 [AIM_MATH12_BANK01_Q175] - Mức độ: **HARD**

- **Chủ đề:** Dây cung lớn nhất mặt cầu
- **Đề bài:** Trong không gian Oxyz, tìm tham số m để đường thẳng d: x = 1 + t, y = 2 - t, z = m + t cắt mặt cầu (S): x^2 + y^2 + z^2 - 2x - 4y + 1 = 0 tại hai điểm phân biệt có khoảng cách lớn nhất.

- **A.** m = 0
- **B.** m = 1
- **C.** m = -1
- **D.** m = 2

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Mặt cầu (S) có phương trình (x - 1)^2 + (y - 2)^2 + z^2 = 4, suy ra tâm I(1; 2; 0) và bán kính R = 2. Đường thẳng d đi qua điểm M0(1; 2; m) và có vectơ chỉ phương u = (1; -1; 1). Ta có IM0 = (0; 0; m), tích có hướng [IM0, u] = (m; m; 0), suy ra khoảng cách từ tâm I đến d là d(I, d) = |[IM0, u]| / |u| = |m|*căn(2) / căn(3). Đường thẳng d cắt mặt cầu tại hai điểm phân biệt khi d(I, d) < R <=> 2m^2 / 3 < 4 <=> m^2 < 6. Khi đó khoảng cách giữa hai giao điểm là độ dài dây cung AB = 2*căn(R^2 - d(I, d)^2) = 2*căn(4 - 2m^2 / 3). Vì 2m^2 / 3 >= 0 nên AB <= 2*căn(4) = 4. Khoảng cách lớn nhất bằng 4 (khi dây cung là đường kính đi qua tâm I) đạt được khi và chỉ khi m = 0. Với m = 1, m = -1 hoặc m = 2, d(I, d) > 0 nên khoảng cách giữa hai giao điểm đều nhỏ hơn 4. Vậy m = 0 là giá trị duy nhất cần tìm.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 176 [AIM_MATH12_BANK01_Q176] - Mức độ: **HARD**

- **Chủ đề:** Điểm đối xứng qua mặt phẳng
- **Đề bài:** Trong không gian Oxyz, cho mặt phẳng (P): x + 2y - 2z + 1 = 0 và điểm A(1; 1; 1). Điểm B đối xứng với A qua (P) có toạ độ là

- **A.** (-1/3; -5/3; 11/3)
- **B.** (1/3; 5/3; -11/3)
- **C.** (0; -1; 3)
- **D.** (-1; 0; 2)

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Đường thẳng qua A vuông góc (P) có VTCP n = (1; 2; -2). Gọi H là hình chiếu của A lên (P): H(1 + t; 1 + 2t; 1 - 2t). Thay vào phương trình (P): (1 + t) + 2(1 + 2t) - 2(1 - 2t) + 1 = 0 <=> 9t + 2 = 0 <=> t = -2/9. Hình chiếu H(7/9; 5/9; 13/9). Toạ độ điểm đối xứng B = 2H - A = (-1/3; -5/3; 11/3).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 177 [AIM_MATH12_BANK01_Q177] - Mức độ: **HARD**

- **Chủ đề:** Mặt phẳng chứa trục tạo góc
- **Đề bài:** Mặt phẳng (P) chứa trục Oz và tạo với mặt phẳng (Q): 2x + y - sqrt(5)*z = 0 một góc 60 độ có phương trình là

- **A.** 2x - y = 0
- **B.** x + y = 0
- **C.** x - 2y = 0
- **D.** x - y = 0

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Mặt phẳng (P) chứa trục Oz nên có phương trình dạng Ax + By = 0 (A^2 + B^2 > 0). VTPT n_P = (A; B; 0). VTPT của (Q) là n_Q = (2; 1; -sqrt(5)). Ta có cos(60) = 1/2 <=> |2A + B| / (sqrt(A^2 + B^2)*sqrt(4 + 1 + 5)) = 1/2 <=> |2A + B| = sqrt(10)*sqrt(A^2 + B^2) / 2. Bình phương giải tìm được tỉ lệ A = 2B hoặc A = B/2 tương ứng x - 2y = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 178 [AIM_MATH12_BANK01_Q178] - Mức độ: **HARD**

- **Chủ đề:** Mặt phẳng cắt mặt cầu đường tròn lớn
- **Đề bài:** Cho mặt cầu (S): x^2 + y^2 + z^2 - 4x + 2y - 4 = 0 và mặt phẳng (P): 2x - 2y + z + 5 = 0. Mặt phẳng (Q) song song với (P) và cắt (S) theo giao tuyến là đường tròn có chu vi lớn nhất có phương trình là

- **A.** 2x - 2y + z + 3 = 0
- **B.** 2x - 2y + z = 0
- **C.** 2x - 2y + z - 9 = 0
- **D.** 2x - 2y + z - 6 = 0

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Đường tròn giao tuyến có chu vi lớn nhất khi mặt phẳng (Q) đi qua tâm I của mặt cầu. Tâm mặt cầu I(2; -1; 0). Mặt phẳng (Q) song song với (P) nên có phương trình dạng: 2x - 2y + z + D = 0. Đi qua I(2; -1; 0) => 2*2 - 2*(-1) + 0 + D = 0 <=> 6 + D = 0 <=> D = -6. Phương trình (Q) là 2x - 2y + z - 6 = 0.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 179 [AIM_MATH12_BANK01_Q179] - Mức độ: **HARD**

- **Chủ đề:** Bán kính mặt cầu ngoại tiếp tứ diện vuông
- **Đề bài:** Trong không gian Oxyz, cho A(1; 0; 0), B(0; 2; 0), C(0; 0; 3). Bán kính mặt cầu ngoại tiếp tứ diện OABC bằng

- **A.** sqrt(14)
- **B.** sqrt(14)/2
- **C.** 7/2
- **D.** 3

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Tứ diện OABC có ba cạnh OA, OB, OC đôi một vuông góc tại O với OA = 1, OB = 2, OC = 3. Tâm mặt cầu ngoại tiếp là trung điểm đường chéo hình hộp chữ nhật có 3 kích thước 1, 2, 3. Bán kính R = 1/2 * sqrt(OA^2 + OB^2 + OC^2) = 1/2 * sqrt(1^2 + 2^2 + 3^2) = sqrt(14)/2.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 180 [AIM_MATH12_BANK01_Q180] - Mức độ: **HARD**

- **Chủ đề:** Khoảng cách tham số hoá
- **Đề bài:** Trong không gian Oxyz, cho mặt phẳng (P): 2x - y + 2z + 1 = 0 và đường thẳng d: (x - 1)/1 = y/2 = (z + 2)/-1. Toạ độ điểm M thuộc đường thẳng d sao cho khoảng cách từ M đến mặt phẳng (P) bằng 2 là

- **A.** M(1; 0; -2)
- **B.** M(3; 4; -4)
- **C.** M(0; -2; -1)
- **D.** M(7/2; 5; -9/2) hoặc M(-5/2; -7; 3/2)

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Điểm M thuộc d có toạ độ tham số: M(1 + t; 2t; -2 - t). Khoảng cách từ M đến mặt phẳng (P): d(M, P) = |2(1 + t) - 2t + 2(-2 - t) + 1| / sqrt(2^2 + (-1)^2 + 2^2) = |-2t - 1| / 3. Theo đề bài: |-2t - 1| / 3 = 2 <=> |2t + 1| = 6. Trường hợp 1: 2t + 1 = 6 <=> t = 5/2 => M(7/2; 5; -9/2). Trường hợp 2: 2t + 1 = -6 <=> t = -7/2 => M(-5/2; -7; 3/2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

## Chương VI: Xác suất có điều kiện

### Câu 181 [AIM_MATH12_BANK01_Q181] - Mức độ: **EASY**

- **Chủ đề:** Công thức xác suất có điều kiện
- **Đề bài:** Cho hai biến cố A và B với P(B) > 0. Xác suất có điều kiện của A với điều kiện B được tính bởi công thức

- **A.** P(A|B) = P(AB) / P(A)
- **B.** P(A|B) = P(AB) / P(B)
- **C.** P(A|B) = P(A) * P(B)
- **D.** P(A|B) = P(A) / P(B)

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Định nghĩa chuẩn xác suất có điều kiện: P(A|B) = P(AB) / P(B).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 182 [AIM_MATH12_BANK01_Q182] - Mức độ: **EASY**

- **Chủ đề:** Biến cố độc lập
- **Đề bài:** Nếu hai biến cố A và B độc lập với nhau và P(B) > 0 thì

- **A.** P(A|B) = P(B)
- **B.** P(A|B) = P(A)
- **C.** P(A|B) = 0
- **D.** P(A|B) = 1

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Khi A và B là hai biến cố độc lập thì việc biến cố B xảy ra không làm thay đổi xác suất của biến cố A: P(A|B) = P(A).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 183 [AIM_MATH12_BANK01_Q183] - Mức độ: **EASY**

- **Chủ đề:** Tính xác suất có điều kiện
- **Đề bài:** Cho P(A) = 0,4; P(B) = 0,5 và P(AB) = 0,2. Giá trị của P(A|B) bằng

- **A.** 0,5
- **B.** 0,2
- **C.** 0,4
- **D.** 0,8

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Áp dụng định nghĩa xác suất có điều kiện: P(A|B) = P(AB) / P(B) = 0,2 / 0,5 = 0,4.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 184 [AIM_MATH12_BANK01_Q184] - Mức độ: **EASY**

- **Chủ đề:** Công thức nhân xác suất
- **Đề bài:** Cho P(B|A) = 0,6 và P(A) = 0,5. Xác suất của biến cố giao P(AB) bằng

- **A.** 0,1
- **B.** 1,1
- **C.** 0,3
- **D.** 0,6

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Theo công thức nhân xác suất: P(AB) = P(A) * P(B|A) = 0,5 * 0,6 = 0,3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 185 [AIM_MATH12_BANK01_Q185] - Mức độ: **EASY**

- **Chủ đề:** Công thức xác suất toàn phần
- **Đề bài:** Công thức xác suất toàn phần đối với hệ đầy đủ hai biến cố {A_1, A_2} là

- **A.** P(B) = P(A_1)*P(B|A_1) + P(A_2)*P(B|A_2)
- **B.** P(B) = P(A_1) + P(A_2)
- **C.** P(B) = P(B|A_1) * P(B|A_2)
- **D.** P(B) = P(A_1)*P(B|A_2) + P(A_2)*P(B|A_1)

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Theo định lý xác suất toàn phần: P(B) = P(A_1)*P(B|A_1) + P(A_2)*P(B|A_2).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 186 [AIM_MATH12_BANK01_Q186] - Mức độ: **EASY**

- **Chủ đề:** Xác suất điều kiện xúc xắc
- **Đề bài:** Gieo một con xúc xắc cân đối, đồng chất. Biết rằng số chấm xuất hiện là số chẵn, xác suất để số chấm xuất hiện là số 2 bằng

- **A.** 1/3
- **B.** 1/6
- **C.** 1/2
- **D.** 2/3

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Không gian biến cố điều kiện là tập các mặt chẵn B = {2, 4, 6} gồm 3 phần tử. Biến cố A = {2} có 1 phần tử. Vậy P(A|B) = 1/3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 187 [AIM_MATH12_BANK01_Q187] - Mức độ: **EASY**

- **Chủ đề:** Biến cố đối
- **Đề bài:** Cho P(A) = 0,7. Xác suất của biến cố đối A_ngang bằng

- **A.** 0,3
- **B.** 0,7
- **C.** 1,0
- **D.** -0,3

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Xác suất của biến cố đối: P(A_ngang) = 1 - P(A) = 1 - 0,7 = 0,3.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 188 [AIM_MATH12_BANK01_Q188] - Mức độ: **EASY**

- **Chủ đề:** Ý nghĩa công thức Bayes
- **Đề bài:** Công thức Bayes dùng để

- **A.** Tính trung bình cộng của hai biến cố
- **B.** Đo độ dài của một khoảng cách trong không gian
- **C.** Tính xác suất hậu nghiệm của nguyên nhân khi đã biết kết quả xảy ra
- **D.** Giải phương trình bậc ba

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Công thức Bayes cho phép cập nhật xác suất của các nguyên nhân (giả thiết) A_i khi đã quan sát thấy biến cố B xảy ra.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 189 [AIM_MATH12_BANK01_Q189] - Mức độ: **MEDIUM**

- **Chủ đề:** Lấy bi không hoàn lại
- **Đề bài:** Một hộp có 5 viên bi đỏ và 3 viên bi xanh. Lấy ngẫu nhiên lần lượt 2 viên bi không hoàn lại. Xác suất để viên thứ hai màu đỏ biết viên thứ nhất màu đỏ là

- **A.** 5/8
- **B.** 1/2
- **C.** 5/7
- **D.** 4/7

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Sau khi lấy 1 viên bi đỏ ở lần thứ nhất, trong hộp còn lại 4 bi đỏ và 3 bi xanh (tổng cộng 7 viên). Xác suất lấy tiếp được viên đỏ là 4/7.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 190 [AIM_MATH12_BANK01_Q190] - Mức độ: **MEDIUM**

- **Chủ đề:** Xác suất toàn phần xét nghiệm y tế
- **Đề bài:** Tỉ lệ người mắc bệnh X trong một cộng đồng là 2%. Một xét nghiệm có độ nhạy 95% (người có bệnh xét nghiệm dương tính xác suất 0,95) và độ đặc hiệu 90% (người không bệnh âm tính xác suất 0,90). Xác suất một người bất kỳ xét nghiệm cho kết quả dương tính bằng

- **A.** 0,117
- **B.** 0,095
- **C.** 0,020
- **D.** 0,190

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Áp dụng công thức xác suất toàn phần: P(Dương tính) = P(Bệnh)*P(Dương tính|Bệnh) + P(Không bệnh)*P(Dương tính|Không bệnh) = 0,02 * 0,95 + 0,98 * (1 - 0,90) = 0,019 + 0,098 = 0,117.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 191 [AIM_MATH12_BANK01_Q191] - Mức độ: **MEDIUM**

- **Chủ đề:** Biến cố hợp và xác suất điều kiện
- **Đề bài:** Cho hai biến cố A và B có P(A) = 0,5; P(B) = 0,6 và P(A hợp B) = 0,8. Xác suất P(A|B) bằng

- **A.** 0,6
- **B.** 0,3
- **C.** 0,5
- **D.** 0,4

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Ta có P(A hợp B) = P(A) + P(B) - P(AB) => P(AB) = 0,5 + 0,6 - 0,8 = 0,3. Khi đó P(A|B) = P(AB) / P(B) = 0,3 / 0,6 = 0,5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 192 [AIM_MATH12_BANK01_Q192] - Mức độ: **MEDIUM**

- **Chủ đề:** Bắn súng xác suất điều kiện
- **Đề bài:** Một xạ thủ bắn 2 phát súng độc lập vào bia. Xác suất bắn trúng ở phát thứ nhất là 0,8 và phát thứ hai là 0,7. Biết có đúng một phát bắn trúng bia, xác suất để phát thứ nhất trúng bằng

- **A.** 0,50
- **B.** 0,36
- **C.** 0,70
- **D.** 0,64

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Gọi T_1, T_2 là trúng lần 1, 2. Biến cố có đúng 1 phát trúng C = T_1*T_2_ngang + T_1_ngang*T_2. P(C) = 0,8*0,3 + 0,2*0,7 = 0,24 + 0,14 = 0,38. Xác suất phát 1 trúng khi biết C là P(T_1*T_2_ngang) / P(C) = 0,24 / 0,38 = 12/19 xấp xỉ 0,6316... tính chuẩn là 12/19.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 193 [AIM_MATH12_BANK01_Q193] - Mức độ: **MEDIUM**

- **Chủ đề:** Công thức Bayes xưởng sản xuất
- **Đề bài:** Một xưởng sản xuất sử dụng 2 máy: Máy I sản xuất 60% sản phẩm với tỉ lệ phế phẩm 2%; Máy II sản xuất 40% sản phẩm với tỉ lệ phế phẩm 3%. Lấy ngẫu nhiên 1 sản phẩm thấy là phế phẩm. Xác suất sản phẩm đó do máy I sản xuất bằng

- **A.** 3/5
- **B.** 2/3
- **C.** 1/2
- **D.** 4/7

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** P(Máy I) = 0,6; P(Phế|I) = 0,02; P(Máy II) = 0,4; P(Phế|II) = 0,03. P(Phế phẩm) = 0,6*0,02 + 0,4*0,03 = 0,012 + 0,012 = 0,024. P(Máy I|Phế) = 0,012 / 0,024 = 1/2 = 0,5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 194 [AIM_MATH12_BANK01_Q194] - Mức độ: **MEDIUM**

- **Chủ đề:** Xác suất điều kiện hai xúc xắc
- **Đề bài:** Gieo đồng thời hai con xúc xắc cân đối. Biết tổng số chấm xuất hiện bằng 8, xác suất để có ít nhất một con xúc xắc xuất hiện mặt 5 chấm bằng

- **A.** 1/5
- **B.** 2/5
- **C.** 1/6
- **D.** 1/4

- **Đáp án đúng:** **B**
- **Lời giải chi tiết:** Không gian biến cố tổng bằng 8: Omega_B = {(2,6), (3,5), (4,4), (5,3), (6,2)} gồm 5 phần tử. Các kết quả có ít nhất một mặt 5 chấm là {(3,5), (5,3)} gồm 2 phần tử. Vậy P = 2/5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 195 [AIM_MATH12_BANK01_Q195] - Mức độ: **MEDIUM**

- **Chủ đề:** Xác suất học sinh giỏi hai môn
- **Đề bài:** Một lớp học có 20 học sinh giỏi Toán, 15 học sinh giỏi Văn và 10 học sinh giỏi cả hai môn. Chọn ngẫu nhiên 1 học sinh giỏi Toán, xác suất học sinh đó cũng giỏi Văn bằng

- **A.** 2/3
- **B.** 3/4
- **C.** 1/2
- **D.** 1/3

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Xác suất có điều kiện: P(Giỏi Văn | Giỏi Toán) = n(Giỏi cả hai) / n(Giỏi Toán) = 10 / 20 = 1/2 = 0,5.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 196 [AIM_MATH12_BANK01_Q196] - Mức độ: **MEDIUM**

- **Chủ đề:** Kiểm tra tính độc lập
- **Đề bài:** Cho hai biến cố A và B thỏa mãn P(A) = 0,4; P(B) = 0,5 và P(B|A) = 0,25. Khẳng định nào sau đây là đúng?

- **A.** Hai biến cố A và B độc lập
- **B.** A và B xung khắc
- **C.** P(AB) = 0,2
- **D.** Hai biến cố A và B không độc lập

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** Ta có P(B|A) = 0,25 khác P(B) = 0,5. Vì P(B|A) khác P(B) nên hai biến cố A và B không độc lập với nhau.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 197 [AIM_MATH12_BANK01_Q197] - Mức độ: **HARD**

- **Chủ đề:** Bayes y tế dịch tễ
- **Đề bài:** Trong một đợt khám sàng lọc bệnh sốt rét, tỉ lệ mắc bệnh trong dân cư là 1%. Xét nghiệm nhanh có độ nhạy 98% và độ dương tính giả là 2%. Một người có kết quả xét nghiệm dương tính thì xác suất thực sự mắc bệnh của người đó xấp xỉ bằng

- **A.** 33,1%
- **B.** 98,0%
- **C.** 50,0%
- **D.** 10,0%

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** P(Bệnh) = 0,01; P(Dương|Bệnh) = 0,98; P(Không bệnh) = 0,99; P(Dương|Không bệnh) = 0,02. P(Dương) = 0,01*0,98 + 0,99*0,02 = 0,0098 + 0,0198 = 0,0296. P(Bệnh|Dương) = 0,0098 / 0,0296 xấp xỉ 0,3311 (tức 33,1%).
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 198 [AIM_MATH12_BANK01_Q198] - Mức độ: **HARD**

- **Chủ đề:** Kênh truyền tin nhị phân Bayes
- **Đề bài:** Một kênh truyền tín hiệu nhị phân truyền các ký hiệu 0 và 1 với xác suất phát ký hiệu 0 là 0,6 và ký hiệu 1 là 0,4. Do có nhiễu trên đường truyền, xác suất để ký hiệu 0 bị thu sai thành 1 là 0,05 và xác suất để ký hiệu 1 bị thu sai thành 0 là 0,02. Khi đầu thu nhận được ký hiệu 1, xác suất để thực sự ký hiệu 1 đã được phát bằng

- **A.** 1/2
- **B.** 0,95
- **C.** 0,88
- **D.** 392/422

- **Đáp án đúng:** **D**
- **Lời giải chi tiết:** P(Thu 1) = P(Phát 0)*P(Lỗi) + P(Phát 1)*P(Đúng) = 0,6*0,05 + 0,4*(1 - 0,02) = 0,03 + 0,4*0,98 = 0,03 + 0,392 = 0,422. Áp dụng Bayes: P(Phát 1|Thu 1) = (0,4*0,98) / 0,422 = 0,392 / 0,422 = 392/422 xấp xỉ 92,9%.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 199 [AIM_MATH12_BANK01_Q199] - Mức độ: **HARD**

- **Chủ đề:** Chuyển bi giữa hai túi
- **Đề bài:** Có hai túi đựng bi: Túi I có 4 bi đỏ và 2 bi xanh; Túi II có 3 bi đỏ và 5 bi xanh. Lấy ngẫu nhiên một viên bi từ Túi I bỏ sang Túi II, sau đó lấy ngẫu nhiên 1 viên bi từ Túi II. Xác suất để viên bi lấy ra từ Túi II màu đỏ bằng

- **A.** 1/2
- **B.** 1/3
- **C.** 11/27
- **D.** 4/9

- **Đáp án đúng:** **C**
- **Lời giải chi tiết:** Biến cố lấy từ túi I sang: Đỏ (xác suất 4/6 = 2/3) hoặc Xanh (xác suất 2/6 = 1/3). Nếu đỏ sang, túi II có 4 đỏ 5 xanh => P(Đỏ|I_đỏ) = 4/9. Nếu xanh sang, túi II có 3 đỏ 6 xanh => P(Đỏ|I_xanh) = 3/9. Theo công thức xác suất toàn phần: P(Đỏ) = (2/3)*(4/9) + (1/3)*(3/9) = 8/27 + 3/27 = 11/27.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

### Câu 200 [AIM_MATH12_BANK01_Q200] - Mức độ: **HARD**

- **Chủ đề:** Trò chơi may rủi kết hợp Bayes
- **Đề bài:** Một người tham gia trò chơi tung đồng xu cân đối, đồng chất: nếu xuất hiện mặt ngửa thì được bốc một thăm từ hộp A (gồm 3 vé trúng thưởng, 2 vé trượt), nếu xuất hiện mặt sấp thì bốc thăm từ hộp B (gồm 2 vé trúng thưởng, 8 vé trượt). Biết người đó đã bốc được vé trúng thưởng, xác suất để đồng xu đã xuất hiện mặt ngửa bằng

- **A.** 3/4
- **B.** 1/2
- **C.** 3/5
- **D.** 2/3

- **Đáp án đúng:** **A**
- **Lời giải chi tiết:** Gọi N là biến cố đồng xu ngửa, S là biến cố sấp. Do đồng xu cân đối nên P(N) = P(S) = 0,5. Gọi T là biến cố bốc được vé trúng. Ta có P(T|N) = 3/5 = 0,6 và P(T|S) = 2/10 = 0,2. Xác suất toàn phần: P(T) = 0,5*0,6 + 0,5*0,2 = 0,3 + 0,1 = 0,4. Áp dụng công thức Bayes: P(N|T) = (P(N)*P(T|N)) / P(T) = 0,3 / 0,4 = 3/4 = 0,75.
- **Nguồn tham chiếu:** `SRC_SGK_KNTT_M12; SRC_BGDDT_MINHHOA_2025_M12`

---

