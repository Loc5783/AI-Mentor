"""Generate the Grade 12 curriculum packs: Math, Physics, Chemistry, Biology, Literature, English.

Contract:
- 15 lessons for Math (3 complete blocks of 5)
- 20 lessons for Physics (4 complete blocks of 5)
- 20 lessons for Chemistry (4 complete blocks of 5)
- 20 lessons for Biology (4 complete blocks of 5)
- 15 lessons for Literature (3 complete blocks of 5)
- 15 lessons for English (3 complete blocks of 5)
- Every lesson has guidance, examples, and 3 staged questions (EASY, MEDIUM, APPLIED).
- All IDs include subject and 12.
- Grade 12, published=True, version="intro-2026.1", coverage="PARTIAL_GRADE_12".
"""
import json
from pathlib import Path
import sys

# Ensure backend root is on sys.path for Catalog validation
BACKEND = Path(__file__).resolve().parents[1]
if str(BACKEND) not in sys.path:
    sys.path.insert(0, str(BACKEND))

from aimentor.learning import Catalog
try:
    from tools.grade12_batch2_data import BIOLOGY_LESSONS, LITERATURE_LESSONS, ENGLISH_LESSONS
except ImportError:
    from grade12_batch2_data import BIOLOGY_LESSONS, LITERATURE_LESSONS, ENGLISH_LESSONS
try:
    from tools.grade12_batch3_data import HISTORY_LESSONS, GEOGRAPHY_LESSONS, ECONOMICS_LAW_LESSONS
except ImportError:
    from grade12_batch3_data import HISTORY_LESSONS, GEOGRAPHY_LESSONS, ECONOMICS_LAW_LESSONS

SOURCE_NOTE = (
    "Tài liệu học tập lớp 12 do AI Mentor biên soạn theo yêu cầu cần đạt của "
    "Chương trình GDPT 2018 (định hướng bộ sách Kết nối tri thức với cuộc sống); "
    "đây là tuyển chọn chuyên đề nền tảng, không phải bản sao nguyên văn SGK "
    "và chưa bao phủ toàn bộ năm học. Bài đầu vào sử dụng 2 câu hỏi từ mỗi bài học "
    "để hỗ trợ chẩn đoán năng lực. Nội dung cần được giáo viên chuyên môn thẩm định "
    "trước khi áp dụng chính thức."
)

MATH_LESSONS = [
    # Block 1: Ứng dụng đạo hàm khảo sát hàm số
    ("monotonicity", "Tính đơn điệu của hàm số",
     "Hàm số y = f(x) đồng biến trên khoảng K khi f'(x) ≥ 0 với mọi x thuộc K (dấu bằng chỉ xảy ra tại hữu hạn điểm). Hàm số nghịch biến khi f'(x) ≤ 0 trên K.",
     "Xét f(x) = x³ - 3x: f'(x) = 3x² - 3 = 3(x² - 1). f'(x) > 0 khi x < -1 hoặc x > 1, do đó f(x) đồng biến trên (-∞; -1) và (1; +∞).",
     [
         ("Cho hàm số y = f(x) có đạo hàm f'(x) > 0 với mọi x thuộc khoảng (1; 3). Kết luận nào sau đây đúng?",
          "Hàm số đồng biến trên khoảng (1; 3)",
          "Hàm số nghịch biến trên khoảng (1; 3)",
          "Hàm số không đổi trên khoảng (1; 3)",
          "Đạo hàm luôn mang dấu dương trên một khoảng là điều kiện đủ để hàm số đồng biến trên khoảng đó."),
         ("Tìm các khoảng nghịch biến của hàm số y = -x³ + 3x.",
          "(-∞; -1) và (1; +∞)",
          "(-1; 1)",
          "(-∞; 0)",
          "Đạo hàm y' = -3x² + 3 = -3(x² - 1). Ta có y' < 0 khi x² - 1 > 0, tức x < -1 hoặc x > 1."),
         ("Chi phí sản xuất x sản phẩm của một xí nghiệp là C(x) = 2x² + 50x + 800 (nghìn đồng) với x > 0. Ý nghĩa thực tế của việc chi phí biên C'(x) > 0 với mọi x > 0 là gì?",
          "Tổng chi phí sản xuất luôn tăng khi quy mô sản lượng mở rộng",
          "Tổng chi phí sản xuất luôn giảm khi mở rộng sản lượng",
          "Chi phí sản xuất không phụ thuộc vào sản lượng",
          "Chi phí biên C'(x) = 4x + 50 > 0 với mọi x > 0, cho thấy hàm chi phí là hàm đồng biến theo sản lượng x.")
     ]),

    ("extrema", "Cực trị của hàm số",
     "Nếu f'(x₀) = 0 (hoặc f'(x₀) không xác định) và đạo hàm f'(x) đổi dấu từ dương sang âm khi qua x₀ thì x₀ là điểm cực đại. Nếu đổi dấu từ âm sang dương thì x₀ là điểm cực tiểu.",
     "Với y = x³ - 3x²: y' = 3x(x - 2). y' đổi dấu từ + sang - qua x = 0 (cực đại), từ - sang + qua x = 2 (cực tiểu).",
     [
         ("Giả sử hàm số y = f(x) có đạo hàm tại điểm x₀. Điều kiện cần để x₀ là một điểm cực trị của hàm số là:",
          "f'(x₀) = 0",
          "f'(x₀) > 0",
          "f'(x₀) < 0",
          "Theo định lí Fermat, nếu hàm số đạt cực trị tại x₀ và có đạo hàm tại đó thì f'(x₀) = 0."),
         ("Tìm điểm cực tiểu của hàm số y = x³ - 3x + 2.",
          "x = 1",
          "x = -1",
          "x = 0",
          "y' = 3x² - 3 = 0 ⇔ x = ±1. Đạo hàm đổi dấu từ âm sang dương khi đi qua x = 1 nên x = 1 là điểm cực tiểu."),
         ("Hàm lợi nhuận của một cơ sở kinh doanh theo số lượng sản phẩm x bán ra là P(x) = -2x² + 120x - 500 (nghìn đồng). Để lợi nhuận đạt giá trị cực đại, cơ sở cần sản xuất bao nhiêu sản phẩm?",
          "30 sản phẩm",
          "60 sản phẩm",
          "20 sản phẩm",
          "P'(x) = -4x + 120 = 0 ⇔ x = 30. Vì P''(30) = -4 < 0 nên tại x = 30 lợi nhuận đạt cực đại.")
     ]),

    ("min-max", "Giá trị lớn nhất và nhỏ nhất của hàm số",
     "Mọi hàm số liên tục trên đoạn [a; b] đều đạt giá trị lớn nhất (GTLN) và giá trị nhỏ nhất (GTNN) trên đoạn đó. Ta tìm các điểm tới hạn trong khoảng (a; b) rồi so sánh giá trị tại các điểm đó với f(a) và f(b).",
     "Tìm GTLN và GTNN của f(x) = x³ - 3x trên [0; 2]: f'(x) = 3x² - 3 = 0 có nghiệm x = 1 thuộc (0; 2). Ta có f(0) = 0, f(1) = -2, f(2) = 2. Vậy GTLN bằng 2, GTNN bằng -2.",
     [
         ("Cho hàm số y = f(x) liên tục trên đoạn [a; b]. Khẳng định nào sau đây luôn đúng?",
          "Hàm số luôn đạt giá trị lớn nhất và giá trị nhỏ nhất trên [a; b]",
          "Hàm số chỉ đạt cực đại mà không thể đạt giá trị lớn nhất",
          "Giá trị lớn nhất luôn đạt tại một trong hai đầu mút a hoặc b",
          "Theo định lí Weierstrass, hàm số liên tục trên một đoạn đóng bị chặn luôn đạt giá trị lớn nhất và nhỏ nhất trên đoạn đó."),
         ("Giá trị lớn nhất của hàm số y = -x² + 4x + 1 trên đoạn [0; 3] bằng bao nhiêu?",
          "5",
          "4",
          "1",
          "y' = -2x + 4 = 0 ⇔ x = 2 thuộc [0; 3]. Ta tính: y(0) = 1, y(2) = 5, y(3) = 4. Vậy giá trị lớn nhất bằng 5."),
         ("Bác thợ rèn dùng 40 m lưới rào quanh một mảnh vườn hình chữ nhật tựa vào bờ tường thẳng có sẵn (không cần rào phía tường). Diện tích lớn nhất có thể rào được là bao nhiêu?",
          "200 m²",
          "400 m²",
          "100 m²",
          "Gọi chiều rộng vuông góc với tường là x (m) thì chiều dài là 40 - 2x (m) với 0 < x < 20. Diện tích S(x) = x(40 - 2x) = -2x² + 40x. S'(x) = -4x + 40 = 0 ⇔ x = 10, khi đó S = 200 m².")
     ]),

    ("asymptotes", "Đường tiệm cận của đồ thị hàm số",
     "Đường thẳng y = y₀ là tiệm cận ngang nếu lim_{x→+∞} f(x) = y₀ hoặc lim_{x→-∞} f(x) = y₀. Đường thẳng x = x₀ là tiệm cận đứng nếu ít nhất một trong các giới hạn một bên khi x tiến tới x₀ bằng vô cực.",
     "Hàm số y = (2x + 1)/(x - 1) có lim_{x→1⁺} y = +∞ nên x = 1 là tiệm cận đứng; lim_{x→±∞} y = 2 nên y = 2 là tiệm cận ngang.",
     [
         ("Đường thẳng x = a được gọi là đường tiệm cận đứng của đồ thị hàm số y = f(x) nếu thỏa mãn điều kiện nào?",
          "Ít nhất một trong các giới hạn một bên khi x tiến đến a là vô cực",
          "Giới hạn khi x tiến đến vô cực bằng a",
          "Đạo hàm f'(a) bằng 0",
          "Định nghĩa tiệm cận đứng yêu cầu lim_{x→a⁺} f(x) = ±∞ hoặc lim_{x→a⁻} f(x) = ±∞."),
         ("Tìm phương trình đường tiệm cận ngang của đồ thị hàm số y = (3x - 1)/(x + 2).",
          "y = 3",
          "x = -2",
          "y = -1/2",
          "Ta có lim_{x→+∞} (3x - 1)/(x + 2) = 3 và lim_{x→-∞} (3x - 1)/(x + 2) = 3 nên y = 3 là tiệm cận ngang."),
         ("Nồng độ của một loại thuốc trong máu bệnh nhân sau t giờ được mô hình hóa bởi C(t) = 4t / (t² + 2) (mg/L). Khi thời gian t tăng lên vô hạn, nồng độ thuốc tiến về giá trị nào?",
          "0 mg/L",
          "2 mg/L",
          "4 mg/L",
          "lim_{t→+∞} 4t / (t² + 2) = lim_{t→+∞} (4/t) / (1 + 2/t²) = 0 mg/L. Đồ thị có tiệm cận ngang y = 0.")
     ]),

    ("graph-analysis", "Khảo sát và nhận dạng đồ thị hàm số",
     "Khảo sát hàm số gồm: tìm tập xác định, xét chiều biến thiên (đạo hàm, cực trị, tiệm cận, bảng biến thiên) và vẽ đồ thị (giao trục tọa độ, tính đối xứng). Đồ thị hàm bậc ba có tâm đối xứng là điểm uốn.",
     "Hàm số y = x³ - 3x: f'(x) = 3(x² - 1); cực đại (-1; 2), cực tiểu (1; -2). Đồ thị nhận gốc tọa độ O(0; 0) làm tâm đối xứng.",
     [
         ("Đối với hàm số bậc ba y = ax³ + bx² + cx + d (a ≠ 0), nếu lim_{x→+∞} y = +∞ thì dấu của hệ số a là:",
          "a > 0",
          "a < 0",
          "a = 0",
          "Khi x tiến ra dương vô cực, giới hạn của đa thức bậc ba cùng dấu với hệ số cao nhất a. Do đó a > 0."),
         ("Tâm đối xứng của đồ thị hàm phân thức bậc nhất trên bậc nhất y = (ax + b)/(cx + d) (với c ≠ 0, ad - bc ≠ 0) là:",
          "Giao điểm của tiệm cận đứng và tiệm cận ngang",
          "Giao điểm của đồ thị với trục tung Oy",
          "Giao điểm của đồ thị với trục hoành Ox",
          "Đồ thị hàm nhất biến nhận giao điểm của hai đường tiệm cận làm tâm đối xứng."),
         ("Cho hàm số f(x) liên tục trên R có bảng biến thiên: cực đại tại x = -1 với f(-1) = 4, cực tiểu tại x = 2 với f(2) = -1. Khi x → -∞ thì y → -∞; khi x → +∞ thì y → +∞. Số nghiệm phân biệt của phương trình f(x) = 0 là:",
          "3 nghiệm phân biệt",
          "1 nghiệm",
          "2 nghiệm phân biệt",
          "Trên (-∞; -1), f tăng từ -∞ lên 4 nên cắt Ox tại 1 điểm. Trên (-1; 2), f giảm từ 4 xuống -1 nên cắt Ox tại điểm thứ hai. Trên (2; +∞), f tăng từ -1 lên +∞ nên cắt Ox tại điểm thứ ba.")
     ]),

    # Block 2: Vectơ và Hệ tọa độ Oxyz
    ("vectors-space", "Vectơ và các phép toán trong không gian",
     "Vectơ trong không gian là đoạn thẳng có hướng. Phép cộng vectơ tuân theo quy tắc ba điểm, hình bình hành và hình hộp: vectơ đường chéo xuất phát từ một đỉnh bằng tổng ba vectơ cạnh cùng xuất phát từ đỉnh đó.",
     "Cho hình hộp ABCD.A'B'C'D': vectơ AC' = vectơ AB + vectơ AD + vectơ AA'.",
     [
         ("Trong không gian, quy tắc hình hộp khẳng định tổng ba vectơ chung gốc xuất phát từ một đỉnh của hình hộp bằng vectơ nào?",
          "Vectơ đường chéo xuất phát từ đỉnh đó",
          "Vectơ đường chéo của mặt đáy",
          "Vectơ cạnh đối diện",
          "Với hình hộp ABCD.A'B'C'D', ta luôn có vectơ AB + vectơ AD + vectơ AA' = vectơ AC'."),
         ("Cho tứ diện ABCD có G là trọng tâm của tam giác BCD. Đẳng thức vectơ nào sau đây đúng?",
          "vectơ AB + vectơ AC + vectơ AD = 3 vectơ AG",
          "vectơ AB + vectơ AC + vectơ AD = vectơ AG",
          "vectơ AB + vectơ AC + vectơ AD = 4 vectơ AG",
          "Vì G là trọng tâm tam giác BCD nên vectơ GB + vectơ GC + vectơ GD = vectơ 0. Biểu diễn qua điểm A ta được vectơ AB + AC + AD = 3 AG."),
         ("Ba lực vectơ F₁, F₂, F₃ cùng tác dụng vào một chất điểm đứng yên ở trạng thái cân bằng. Hệ thức vectơ nào sau đây phản ánh đúng định luật cân bằng?",
          "vectơ F₁ + vectơ F₂ + vectơ F₃ = vectơ 0",
          "|F₁| + |F₂| + |F₃| = 0",
          "vectơ F₁ + vectơ F₂ = vectơ F₃",
          "Hợp lực của tất cả các lực tác dụng lên một vật cân bằng phải triệt tiêu, tức tổng vectơ bằng vectơ 0.")
     ]),

    ("coordinates-space", "Hệ tọa độ Oxyz và tọa độ điểm, vectơ",
     "Hệ tọa độ Oxyz gồm ba trục Ox, Oy, Oz đôi một vuông góc tại gốc O với ba vectơ đơn vị i, j, k. Vectơ u = x.i + y.j + z.k có tọa độ u = (x; y; z). Tọa độ trung điểm M của đoạn thẳng AB là trung bình cộng tọa độ của A và B.",
     "A(1; 2; 3) và B(3; 0; -1) thì trung điểm M có tọa độ ((1+3)/2; (2+0)/2; (3-1)/2) = (2; 1; 1). Vectơ AB = (2; -2; -4).",
     [
         ("Trong không gian Oxyz, tọa độ của gốc tọa độ O là:",
          "(0; 0; 0)",
          "(1; 1; 1)",
          "(0; 0; 1)",
          "Gốc tọa độ O có hoành độ, tung độ và cao độ đều bằng 0."),
         ("Cho hai điểm A(2; -1; 4) và B(4; 3; 0). Tọa độ trung điểm M của đoạn thẳng AB là:",
          "(3; 1; 2)",
          "(6; 2; 4)",
          "(1; 2; -2)",
          "xM = (2 + 4)/2 = 3; yM = (-1 + 3)/2 = 1; zM = (4 + 0)/2 = 2. Vậy M(3; 1; 2)."),
         ("Trong trạm không lưu, máy bay A tại tọa độ P(120; 80; 10) di chuyển với vận tốc đều v = (30; 20; 2) (km/h) trong không gian Oxyz. Sau 2 giờ bay, tọa độ mới của máy bay là:",
          "(180; 120; 14)",
          "(150; 100; 12)",
          "(240; 160; 20)",
          "Tọa độ mới P' = P + 2v = (120 + 2*30; 80 + 2*20; 10 + 2*2) = (180; 120; 14).")
     ]),

    ("dot-product-space", "Tích vô hướng và góc giữa hai vectơ",
     "Tích vô hướng của u = (x₁; y₁; z₁) và v = (x₂; y₂; z₂) là u.v = x₁x₂ + y₁y₂ + z₁z₂. Góc θ giữa hai vectơ thỏa mãn cos θ = (u.v)/(|u|.|v|). Hai vectơ vuông góc khi và chỉ khi tích vô hướng bằng 0.",
     "Cho u = (1; 2; 2) và v = (2; -1; 0): u.v = 1*2 + 2*(-1) + 2*0 = 0, suy ra u vuông góc với v.",
     [
         ("Hai vectơ u và v khác vectơ không trong không gian vuông góc với nhau khi và chỉ khi:",
          "Tích vô hướng u · v = 0",
          "Tổng u + v = 0",
          "Độ dài |u| = |v|",
          "cos 90° = 0 nên tích vô hướng u · v = |u||v|cos 90° = 0."),
         ("Tính tích vô hướng của hai vectơ a = (1; 3; -2) và b = (4; -1; 1).",
          "-1",
          "5",
          "9",
          "a · b = 1*4 + 3*(-1) + (-2)*1 = 4 - 3 - 2 = -1."),
         ("Tính số đo góc giữa hai vectơ u = (1; 0; 1) và v = (0; 1; 1).",
          "60°",
          "45°",
          "90°",
          "u · v = 1*0 + 0*1 + 1*1 = 1. Độ dài |u| = √(1² + 1²) = √2, |v| = √2. cos θ = 1 / (√2 * √2) = 1/2 ⇒ θ = 60°.")
     ]),

    ("sphere-equation", "Phương trình mặt cầu trong không gian",
     "Mặt cầu tâm I(a; b; c) bán kính R có phương trình chính tắc: (x - a)² + (y - b)² + (z - c)² = R². Dạng tổng quát x² + y² + z² - 2ax - 2by - 2cz + d = 0 với điều kiện a² + b² + c² - d > 0 và R = √(a² + b² + c² - d).",
     "Mặt cầu tâm I(1; -2; 3) bán kính R = 4 có phương trình: (x - 1)² + (y + 2)² + (z - 3)² = 16.",
     [
         ("Mặt cầu (S): (x - 2)² + (y + 1)² + (z - 3)² = 25 có tọa độ tâm I và bán kính R lần lượt là:",
          "I(2; -1; 3) và R = 5",
          "I(-2; 1; -3) và R = 25",
          "I(2; -1; 3) và R = 25",
          "Từ phương trình chính tắc (x - a)² + (y - b)² + (z - c)² = R², ta có a = 2, b = -1, c = 3 và R = √25 = 5."),
         ("Viết phương trình mặt cầu nhận đoạn thẳng AB làm đường kính với A(1; 0; 2) và B(3; 4; 0).",
          "(x - 2)² + (y - 2)² + (z - 1)² = 6",
          "(x - 2)² + (y - 2)² + (z - 1)² = 24",
          "(x - 1)² + y² + (z - 2)² = 6",
          "Tâm I là trung điểm AB: I(2; 2; 1). Bán kính R = AB/2 = √[(3-1)² + (4-0)² + (0-2)²]/2 = √24 / 2 = √6. R² = 6."),
         ("Một trạm radar phủ sóng hình cầu bán kính 10 km, đặt tại tâm I(1; 2; 3) (đơn vị tọa độ km). Điểm nào sau đây nằm bên trong vùng phát sóng của trạm?",
          "Điểm M(3; 4; 5)",
          "Điểm N(10; 10; 10)",
          "Điểm P(15; 2; 3)",
          "Khoảng cách IM² = (3-1)² + (4-2)² + (5-3)² = 4 + 4 + 4 = 12 < 10² = 100, do đó M nằm bên trong mặt cầu.")
     ]),

    ("plane-equation", "Phương trình mặt phẳng trong không gian",
     "Mặt phẳng đi qua M(x₀; y₀; z₀) và có vectơ pháp tuyến n = (A; B; C) có phương trình: A(x - x₀) + B(y - y₀) + C(z - z₀) = 0 hay Ax + By + Cz + D = 0 (A² + B² + C² > 0). Khoảng cách từ M đến (P) là d = |Ax₀ + By₀ + Cz₀ + D| / √(A² + B² + C²).",
     "Mặt phẳng qua M(1; 2; 3) vuông góc với n = (2; -1; 4) có phương trình: 2(x - 1) - 1(y - 2) + 4(z - 3) = 0 ⇔ 2x - y + 4z - 12 = 0.",
     [
         ("Một vectơ pháp tuyến của mặt phẳng (P): 2x - 3y + z - 5 = 0 là:",
          "n = (2; -3; 1)",
          "n = (2; 3; 1)",
          "n = (2; -3; -5)",
          "Các hệ số của x, y, z trong phương trình tổng quát chính là tọa độ của một vectơ pháp tuyến."),
         ("Phương trình mặt phẳng đi qua M(1; 2; -1) và nhận n = (2; 1; 3) làm vectơ pháp tuyến là:",
          "2x + y + 3z - 1 = 0",
          "2x + y + 3z + 1 = 0",
          "x + 2y - z - 6 = 0",
          "Phương trình: 2(x - 1) + 1(y - 2) + 3(z + 1) = 0 ⇔ 2x + y + 3z - 2 - 2 + 3 = 0 ⇔ 2x + y + 3z - 1 = 0."),
         ("Khoảng cách từ điểm A(1; 2; 3) đến mặt phẳng (P): 2x - 2y + z + 5 = 0 bằng bao nhiêu?",
          "2",
          "6",
          "3",
          "d(A, (P)) = |2*1 - 2*2 + 3 + 5| / √(2² + (-2)² + 1²) = |2 - 4 + 3 + 5| / √9 = 6 / 3 = 2.")
     ]),

    # Block 3: Nguyên hàm, Tích phân, Thống kê và Xác suất
    ("antiderivative", "Nguyên hàm và tính chất cơ bản",
     "Hàm số F(x) là một nguyên hàm của f(x) trên K nếu F'(x) = f(x) với mọi x thuộc K. Họ tất cả các nguyên hàm là ∫ f(x) dx = F(x) + C. Các nguyên hàm cơ bản: ∫ xⁿ dx = xⁿ⁺¹/(n+1) + C (n ≠ -1), ∫ eˣ dx = eˣ + C.",
     "∫ (3x² + 2x) dx = x³ + x² + C.",
     [
         ("Cho F(x) là một nguyên hàm của hàm số f(x) trên khoảng K. Khẳng định nào sau đây đúng?",
          "F'(x) = f(x) với mọi x thuộc K",
          "Đạo hàm F'(x) bằng -f(x) với mọi x thuộc K",
          "∫ F(x) dx = f(x) + C",
          "Định nghĩa nguyên hàm: F(x) là nguyên hàm của f(x) khi và chỉ khi đạo hàm của F(x) bằng f(x)."),
         ("Tìm họ nguyên hàm của hàm số f(x) = 4x³ - 2x + 1.",
          "x⁴ - x² + x + C",
          "12x² - 2 + C",
          "4x⁴ - 2x² + x + C",
          "Áp dụng công thức nguyên hàm từng số hạng: ∫ 4x³ dx = x⁴, ∫ (-2x) dx = -x², ∫ 1 dx = x."),
         ("Một vật chuyển động thẳng có vận tốc tức thời v(t) = 3t² + 2t (m/s). Biết tại thời điểm t = 0 vật ở vị trí s(0) = 5 m. Phương trình vị trí s(t) của vật là:",
          "s(t) = t³ + t² + 5 (m)",
          "s(t) = 6t + 2 (m)",
          "s(t) = t³ + t² (m)",
          "s(t) = ∫ (3t² + 2t) dt = t³ + t² + C. Với s(0) = 5 suy ra C = 5, do đó s(t) = t³ + t² + 5.")
     ]),

    ("definite-integral", "Tích phân và ứng dụng tính diện tích",
     "Định nghĩa tích phân theo công thức Newton - Leibniz: ∫_a^b f(x) dx = F(b) - F(a). Diện tích hình phẳng giới hạn bởi đồ thị y = f(x) liên tục, trục hoành và hai đường thẳng x = a, x = b là S = ∫_a^b |f(x)| dx.",
     "Tính diện tích hình phẳng giới hạn bởi y = x², trục hoành, x = 0 và x = 3: S = ∫_0^3 x² dx = [x³/3]_0^3 = 27/3 = 9.",
     [
         ("Theo công thức Newton - Leibniz, nếu F(x) là một nguyên hàm của hàm số f(x) liên tục trên [a; b] thì:",
          "∫_a^b f(x) dx = F(b) - F(a)",
          "∫_a^b f(x) dx = F(a) - F(b)",
          "∫_a^b f(x) dx = F(a) + F(b)",
          "Công thức cơ bản của giải tích liên hệ tích phân xác định với hiệu giá trị của nguyên hàm tại hai đầu mút."),
         ("Tính tích phân I = ∫_1^2 (3x² + 1) dx.",
          "8",
          "10",
          "7",
          "F(x) = x³ + x. Ta có F(2) = 2³ + 2 = 10; F(1) = 1³ + 1 = 2. Vậy I = F(2) - F(1) = 10 - 2 = 8."),
         ("Tính diện tích hình phẳng giới hạn bởi đồ thị hàm số y = 3x², trục hoành Ox và hai đường thẳng x = 0, x = 2.",
          "8",
          "12",
          "6",
          "Vì 3x² ≥ 0 trên [0; 2] nên diện tích S = ∫_0^2 3x² dx = [x³]_0^2 = 2³ - 0³ = 8.")
     ]),

    ("grouped-dispersion", "Đo độ phân tán của mẫu số liệu ghép nhóm",
     "Các số đặc trưng đo độ phân tán của mẫu số liệu ghép nhóm gồm: khoảng biến thiên R = x_max - x_min, khoảng tứ phân vị IQR = Q₃ - Q₁, phương sai s² và độ lệch chuẩn s = √(s²). Độ lệch chuẩn càng nhỏ thể hiện dữ liệu càng phân tán ít quanh giá trị trung bình.",
     "Mẫu số liệu có phương sai s² = 16 thì độ lệch chuẩn s = √16 = 4.",
     [
         ("Số đặc trưng nào bằng căn bậc hai số học của phương sai trong thống kê?",
          "Độ lệch chuẩn",
          "Khoảng biến thiên",
          "Khoảng tứ phân vị",
          "Độ lệch chuẩn s được định nghĩa là căn bậc hai số học của phương sai s²: s = √(s²)."),
         ("Một mẫu số liệu ghép nhóm có tứ phân vị thứ nhất Q₁ = 45 và tứ phân vị thứ ba Q₃ = 75. Khoảng tứ phân vị IQR của mẫu bằng:",
          "30",
          "120",
          "60",
          "Khoảng tứ phân vị được tính theo công thức IQR = Q₃ - Q₁ = 75 - 45 = 30."),
         ("Hai lớp 12A và 12B có cùng điểm trung bình môn Toán là 7,5 điểm. Độ lệch chuẩn điểm kiểm tra của lớp 12A là 0,8 còn của lớp 12B là 1,6. Kết luận nào sau đây chính xác nhất?",
          "Điểm số của học sinh lớp 12A đồng đều hơn học sinh lớp 12B",
          "Học sinh lớp 12B học tốt môn Toán hơn học sinh lớp 12A",
          "Hai lớp có độ phân tán kết quả học tập như nhau",
          "Độ lệch chuẩn đo mức độ phân tán quanh giá trị trung bình. Lớp 12A có độ lệch chuẩn nhỏ hơn nên mức độ đồng đều cao hơn.")
     ]),

    ("conditional-probability", "Xác suất có điều kiện và quy tắc nhân",
     "Xác suất của biến cố A với điều kiện biến cố B đã xảy ra (P(B) > 0) là P(A|B) = P(A ∩ B) / P(B). Quy tắc nhân xác suất: P(A ∩ B) = P(B) · P(A|B). Khi A và B độc lập thì P(A|B) = P(A).",
     "Gieo xúc xắc cân đối. Gọi B là biến cố 'xuất hiện mặt chẵn' ({2, 4, 6}), A là biến cố 'xuất hiện mặt 6'. P(B) = 3/6 = 1/2, P(A ∩ B) = 1/6. Xác suất P(A|B) = (1/6)/(1/2) = 1/3.",
     [
         ("Cho hai biến cố A và B với P(B) > 0. Công thức tính xác suất có điều kiện của A khi biết B đã xảy ra là:",
          "P(A|B) = P(A ∩ B) / P(B)",
          "P(A|B) = P(A) / P(B)",
          "P(A|B) = P(A ∩ B) · P(B)",
          "Định nghĩa xác suất có điều kiện là tỉ số giữa xác suất đồng thời P(A ∩ B) và xác suất điều kiện P(B)."),
         ("Cho P(A) = 0,6; P(B) = 0,5 và P(A ∩ B) = 0,3. Giá trị của P(A|B) bằng:",
          "0,6",
          "0,5",
          "0,15",
          "P(A|B) = P(A ∩ B) / P(B) = 0,3 / 0,5 = 0,6."),
         ("Một hộp có 5 viên bi xanh và 3 viên bi đỏ. Rút ngẫu nhiên lần lượt hai viên không hoàn lại. Xác suất để viên thứ hai rút được màu đỏ biết rằng viên thứ nhất màu xanh là:",
          "3/7",
          "3/8",
          "2/7",
          "Sau khi rút viên 1 màu xanh, trong hộp còn 4 bi xanh và 3 bi đỏ (tổng cộng 7 viên). Xác suất chọn được bi đỏ là 3/7.")
     ]),

    ("bayes-formula", "Công thức xác suất toàn phần và công thức Bayes",
     "Nếu {B₁, B₂} tạo thành một hệ biến cố đầy đủ thì công thức xác suất toàn phần là P(A) = P(B₁)P(A|B₁) + P(B₂)P(A|B₂). Công thức Bayes cho phép cập nhật xác suất tiên nghiệm khi có bằng chứng: P(B₁|A) = [P(B₁)P(A|B₁)] / P(A).",
     "Xét nghiệm y tế có tỉ lệ dương tính giả; công thức Bayes cho phép tính xác suất thực sự mang bệnh khi kết quả xét nghiệm dương tính.",
     [
         ("Nếu {B₁, B₂} là một hệ đầy đủ các biến cố thì công thức xác suất toàn phần đối với biến cố A là:",
          "P(A) = P(B₁) · P(A|B₁) + P(B₂) · P(A|B₂)",
          "P(A) = P(B₁) + P(B₂)",
          "P(A) = P(A|B₁) · P(A|B₂)",
          "Công thức xác suất toàn phần phân tích biến cố A thành tổng hợp các trường hợp loại trừ nhau tương ứng với hệ đầy đủ."),
         ("Một xí nghiệp có hai dây chuyền sản xuất: máy I sản xuất 60% sản phẩm với tỉ lệ phế phẩm 2%; máy II sản xuất 40% sản phẩm với tỉ lệ phế phẩm 3%. Tỉ lệ phế phẩm chung P(A) của xí nghiệp là:",
          "0,024 (2,4%)",
          "0,050 (5,0%)",
          "0,012 (1,2%)",
          "P(A) = P(I)P(A|I) + P(II)P(A|II) = 0,6 * 0,02 + 0,4 * 0,03 = 0,012 + 0,012 = 0,024 = 2,4%."),
         ("Từ dữ liệu xí nghiệp trên: lấy ngẫu nhiên một sản phẩm thấy bị phế phẩm. Xác suất để phế phẩm đó do máy I sản xuất là bao nhiêu (theo công thức Bayes)?",
          "0,50 (50%)",
          "0,60 (60%)",
          "0,40 (40%)",
          "Theo công thức Bayes: P(I|A) = [P(I)P(A|I)] / P(A) = (0,6 * 0,02) / 0,024 = 0,012 / 0,024 = 0,5 (50%).")
     ])
]

PHYSICS_LESSONS = [
    # Block 1: Vật lí nhiệt (Lessons 1-5)
    ("matter-model", "Mô hình động học phân tử và cấu trúc chất",
     "Mọi chất đều cấu tạo từ các phân tử chuyển động không ngừng; nhiệt độ càng cao thì chuyển động nhiệt càng nhanh. Thể rắn có trật tự và thể tích, hình dạng xác định; thể lỏng có thể tích xác định nhưng hình dạng theo bình chứa; thể khí phân tử ở rất xa nhau và chuyển động hỗn loạn.",
     "Mở nắp lọ tinh dầu ở một góc phòng, một lát sau cả phòng đều ngửi thấy mùi thơm do các phân tử khuếch tán vào không khí nhờ chuyển động nhiệt.",
     [
         ("Theo mô hình động học phân tử, khi nhiệt độ của một vật tăng lên thì:",
          "Tốc độ chuyển động hỗn loạn của các phân tử cấu tạo nên vật tăng",
          "Khối lượng của mỗi phân tử cấu tạo nên vật tăng lên",
          "Khoảng cách giữa các phân tử luôn giảm đi đáng kể",
          "Nhiệt độ là số đo động năng chuyển động nhiệt trung bình của các phân tử. Nhiệt độ tăng nghĩa là phân tử chuyển động nhanh hơn."),
         ("Đặc điểm căn bản về cấu trúc phân tử của chất ở thể khí so với thể rắn và lỏng là:",
          "Khoảng cách giữa các phân tử rất lớn so với kích thước của chính chúng",
          "Các phân tử khí liên kết chặt chẽ và chỉ dao động quanh vị trí cân bằng",
          "Các phân tử khí hoàn toàn không có tương tác đẩy",
          "Ở thể khí, khoảng cách trung bình giữa các phân tử lớn gấp hàng chục lần kích thước phân tử nên lực tương tác rất yếu."),
         ("Hiện tượng khuếch tán chất màu trong nước xảy ra nhanh hơn khi nước nóng hơn là do:",
          "Nhiệt độ cao làm các phân tử nước và phân tử màu chuyển động nhiệt nhanh hơn",
          "Nước nóng làm tăng trọng lực tác dụng lên các phân tử màu",
          "Nước nóng làm phân tử chất màu bị phân hủy thành nguyên tử",
          "Chuyển động nhiệt của các phân tử diễn ra mãnh liệt hơn ở nhiệt độ cao, thúc đẩy quá trình va chạm và trộn lẫn lẫn nhau.")
     ]),

    ("temperature-scales", "Nhiệt độ, thang nhiệt độ và cân bằng nhiệt",
     "Cân bằng nhiệt xảy ra khi hai vật tiếp xúc truyền nhiệt cho nhau đến khi có cùng nhiệt độ. Thang nhiệt độ Celsius (°C) dựa trên điểm đóng băng và điểm sôi của nước ở 1 atm; thang Kelvin (K) là thang nhiệt độ tuyệt đối: T(K) = t(°C) + 273,15.",
     "Nhiệt độ phòng 25 °C tương ứng với T = 25 + 273,15 = 298,15 K.",
     [
         ("Hai vật ở trạng thái cân bằng nhiệt với nhau khi và chỉ khi chúng có cùng:",
          "Nhiệt độ",
          "Nội năng",
          "Khối lượng",
          "Điều kiện cân bằng nhiệt giữa hai hệ tiếp xúc nhiệt là nhiệt độ của chúng bằng nhau, khi đó không còn dòng nhiệt truyền qua lại."),
         ("Nhiệt độ sôi của nước ở áp suất tiêu chuẩn là 100 °C tương ứng với bao nhiêu Kelvin?",
          "373,15 K",
          "273,15 K",
          "100 K",
          "Công thức chuyển đổi: T(K) = t(°C) + 273,15 = 100 + 273,15 = 373,15 K."),
         ("Thả một miếng nhôm ở 80 °C vào cốc nước ở 20 °C trong một bình cách nhiệt. Sau một thời gian hệ đạt cân bằng nhiệt ở 30 °C. Nhận xét nào sau đây đúng?",
          "Miếng nhôm tỏa nhiệt lượng và nước thu vào nhiệt lượng đó",
          "Cả miếng nhôm và nước đều thu nhiệt từ môi trường ngoài",
          "Nhiệt độ cuối cùng luôn bằng trung bình cộng hai nhiệt độ ban đầu",
          "Nhiệt tự truyền từ vật có nhiệt độ cao (nhôm) sang vật có nhiệt độ thấp hơn (nước) cho đến khi đạt cân bằng nhiệt.")
     ]),

    ("heat-capacity", "Nhiệt dung riêng và nhiệt chuyển thể",
     "Nhiệt lượng cần để làm biến đổi nhiệt độ: Q = m.c.ΔT (c: nhiệt dung riêng, J/(kg.K)). Nhiệt nóng chảy: Q = λ.m (λ: nhiệt nóng chảy riêng). Nhiệt hóa hơi: Q = L.m (L: nhiệt hóa hơi riêng). Trong quá trình chuyển thể ở áp suất chuẩn, nhiệt độ không đổi.",
     "Cần đun nóng 2 kg nước (c = 4200 J/(kg.K)) từ 25 °C lên 75 °C: Q = 2 * 4200 * (75 - 25) = 420 000 J = 420 kJ.",
     [
         ("Đơn vị đo của nhiệt dung riêng trong hệ đơn vị quốc tế SI là:",
          "J/(kg·K)",
          "J/kg",
          "W/(kg·K)",
          "Nhiệt dung riêng c = Q / (m·ΔT) có đơn vị là Joule trên kilogram Kelvin (J/(kg·K))."),
         ("Tính nhiệt lượng cần thiết để đun nóng 2 kg nước từ 20 °C lên 80 °C. Biết nhiệt dung riêng của nước c = 4200 J/(kg·K).",
          "504 kJ",
          "252 kJ",
          "672 kJ",
          "Q = m · c · ΔT = 2 · 4200 · (80 - 20) = 8400 · 60 = 504 000 J = 504 kJ."),
         ("Trong suốt thời gian một khối nước đá tinh khiết đang nóng chảy hoàn toàn ở 0 °C dưới áp suất tiêu chuẩn 1 atm, nhiệt độ của khối đá:",
          "Giữ nguyên không đổi ở 0 °C",
          "Tăng dần đều từ 0 °C lên 10 °C",
          "Giảm xuống dưới 0 °C do thu nhiệt",
          "Nhiệt lượng thu vào trong quá trình chuyển thể dùng để phá vỡ mạng tinh thể, không làm tăng động năng phân tử nên nhiệt độ không đổi.")
     ]),

    ("internal-energy", "Nội năng và định luật I nhiệt động lực học",
     "Nội năng U là tổng động năng chuyển động hỗn loạn và thế năng tương tác của các phân tử cấu tạo nên hệ. Định luật I nhiệt động lực học: ΔU = Q + A (quy ước: Q > 0 hệ nhận nhiệt, Q < 0 hệ truyền nhiệt; A > 0 hệ nhận công, A < 0 hệ sinh công).",
     "Chất khí nhận 500 J nhiệt lượng và thực hiện công 300 J đẩy piston: Q = +500 J, A = -300 J ⇒ ΔU = 500 - 300 = +200 J (nội năng tăng 200 J).",
     [
         ("Biểu thức toán học của định luật I nhiệt động lực học là:",
          "ΔU = Q + A",
          "ΔU = Q · A",
          "Q = ΔU + A²",
          "Định luật I là dạng biểu diễn của định luật bảo toàn và chuyển hóa năng lượng áp dụng cho các quá trình nhiệt: ΔU = Q + A."),
         ("Một khối khí nhận nhiệt lượng 600 J từ nguồn nhiệt và thực hiện công 400 J đẩy piston nâng vật nặng lên. Độ biến thiên nội năng của khối khí là:",
          "+200 J",
          "+1000 J",
          "-200 J",
          "Hệ nhận nhiệt: Q = +600 J; hệ sinh công: A = -400 J. Ta có ΔU = Q + A = 600 + (-400) = +200 J."),
         ("Dùng bơm tay để bơm lốp xe đạp, sau một thời gian thân ống bơm bị nóng lên. Nguyên nhân chính là do:",
          "Khí bị nén nhận công làm tăng nội năng và ma sát sinh nhiệt",
          "Không khí từ khí quyển truyền nhiệt lượng trực tiếp vào ống bơm",
          "Khối lượng không khí bên trong ống bơm tự tăng lên",
          "Quá trình nén khí thực hiện công lên chất khí làm tăng nội năng (nhiệt độ tăng) kết hợp với lực ma sát giữa piston và xilanh sinh nhiệt.")
     ]),

    ("thermal-efficiency", "Động cơ nhiệt và hiệu suất nhiệt",
     "Động cơ nhiệt nhận nhiệt lượng Q₁ từ nguồn nóng, chuyển hóa một phần thành công cơ học A' và nhả nhiệt lượng |Q₂| cho nguồn lạnh. Hiệu suất nhiệt: H = A' / Q₁ = (Q₁ - |Q₂|) / Q₁ = 1 - |Q₂| / Q₁ < 1.",
     "Động cơ nhận 1000 J từ nguồn nóng và truyền cho nguồn lạnh 600 J: công sinh ra A' = 400 J, hiệu suất H = 400 / 1000 = 40%.",
     [
         ("Hiệu suất nhiệt H của động cơ nhiệt được định nghĩa là tỉ số giữa:",
          "Công cơ học có ích sinh ra và nhiệt lượng nhận từ nguồn nóng",
          "Nhiệt lượng truyền cho nguồn lạnh và nhiệt lượng nhận từ nguồn nóng",
          "Nhiệt lượng nhận từ nguồn nóng và công toàn phần",
          "Hiệu suất H = A' / Q₁ phản ánh tỉ lệ nhiệt năng được chuyển hóa thành cơ công hữu ích."),
         ("Một động cơ nhiệt nhận nhiệt lượng 2000 J từ nguồn nóng trong mỗi chu trình và nhả cho nguồn lạnh 1200 J. Hiệu suất của động cơ là:",
          "40%",
          "60%",
          "30%",
          "Công có ích A' = Q₁ - |Q₂| = 2000 - 1200 = 800 J. Hiệu suất H = 800 / 2000 = 0,4 = 40%."),
         ("Theo nguyên lí II nhiệt động lực học, có thể chế tạo được một động cơ nhiệt làm việc tuần hoàn biến đổi hoàn toàn 100% nhiệt lượng thu được thành công cơ học không?",
          "Không thể chế tạo được",
          "Hoàn toàn có thể chế tạo được nếu dùng vật liệu siêu dẫn",
          "Có thể chế tạo được nếu nhiệt độ nguồn lạnh bằng 100 °C",
          "Theo cách phát biểu của Kelvin - Planck, động cơ nhiệt tuần hoàn không thể biến đổi toàn bộ nhiệt lượng nhận từ một nguồn thành công mà bắt buộc phải truyền một phần cho nguồn lạnh.")
     ]),

    # Block 2: Khí lí tưởng (Lessons 6-10)
    ("ideal-gas-model", "Thuyết động học phân tử chất khí và mô hình khí lí tưởng",
     "Khí lí tưởng là mô hình trong đó các phân tử được coi là chất điểm, chuyển động hỗn loạn không ngừng và chỉ tương tác với nhau khi va chạm; các va chạm được coi là hoàn toàn đàn hồi.",
     "Ở điều kiện nhiệt độ phòng và áp suất thường, các khí thực như heli, hydro, oxy tuân theo các quy luật của khí lí tưởng với độ chính xác rất cao.",
     [
         ("Trong mô hình khí lí tưởng, tương tác giữa các phân tử khí được xem xét như thế nào?",
          "Chỉ tương tác đẩy hoặc hút khi chúng va chạm vào nhau hoặc vào thành bình",
          "Luôn hút nhau bằng lực tĩnh điện mạnh ở mọi khoảng cách",
          "Luôn đẩy nhau liên tục kể cả khi ở rất xa nhau",
          "Mô hình khí lí tưởng bỏ qua tương tác tầm xa giữa các phân tử; lực tương tác chỉ xuất hiện tức thời khi có va chạm."),
         ("Mô hình khí lí tưởng coi các phân tử khí là:",
          "Các chất điểm có khối lượng nhưng thể tích bản thân không đáng kể so với thể tích bình",
          "Các hình cầu rắn có kích thước bằng thể tích bình chứa",
          "Các hạt đứng yên tại các nút mạng tinh thể xác định",
          "Vì khoảng cách giữa các phân tử khí rất lớn so với kích thước của chúng nên thể tích riêng của các phân tử được coi là chất điểm."),
         ("Khi giữ thể tích bình kín không đổi, việc bơm thêm khí làm tăng mật độ hạt phân tử dẫn đến hiện tượng gì ở cấp độ vi mô?",
          "Số va chạm của các phân tử lên một đơn vị diện tích thành bình trong một giây tăng lên",
          "Khối lượng của từng phân tử khí riêng lẻ tự tăng lên",
          "Tốc độ trung bình của các phân tử khí tự động giảm về 0",
          "Mật độ phân tử tăng làm tần số va chạm lên thành bình tăng, qua đó làm tăng lực tác dụng trung bình và tăng áp suất chất khí.")
     ]),

    ("boyle-law", "Định luật Boyle - Quá trình đẳng nhiệt",
     "Quá trình biến đổi trạng thái trong đó nhiệt độ được giữ không đổi gọi là quá trình đẳng nhiệt. Định luật Boyle: Với một lượng khí xác định ở nhiệt độ không đổi, áp suất tỉ lệ nghịch với thể tích: p₁V₁ = p₂V₂ = const.",
     "Một lượng khí ở 1 atm có thể tích 4 lít. Nén đẳng nhiệt xuống 2 lít thì áp suất mới là p₂ = 1 * 4 / 2 = 2 atm.",
     [
         ("Định luật Boyle áp dụng cho quá trình biến đổi trạng thái nào của chất khí?",
          "Quá trình đẳng nhiệt",
          "Quá trình đẳng áp",
          "Quá trình đẳng tích",
          "Định luật Boyle nghiên cứu mối quan hệ giữa áp suất và thể tích khi nhiệt độ T được giữ không đổi."),
         ("Một khối khí lí tưởng có thể tích 6 lít ở áp suất 1,5 atm. Giữ nhiệt độ không đổi, nén khối khí đến thể tích 2 lít thì áp suất mới là:",
          "4,5 atm",
          "2,0 atm",
          "3,0 atm",
          "Theo định luật Boyle: p₁V₁ = p₂V₂ ⇒ p₂ = (1,5 * 6) / 2 = 9 / 2 = 4,5 atm."),
         ("Một bọt khí nổi từ đáy hồ nước sâu lên mặt thoáng ở nhiệt độ không đổi. Thể tích của bọt khí thay đổi thế nào khi nổi lên?",
          "Thể tích tăng dần do áp suất nước giảm khi tiến gần mặt thoáng",
          "Thể tích giảm dần do áp suất nước tăng lên",
          "Thể tích luôn không đổi vì khối lượng khí không đổi",
          "Áp suất ở đáy hồ lớn hơn ở mặt nước do có thêm áp suất cột nước p = p₀ + ρgh. Khi nổi lên áp suất giảm nên theo định luật Boyle thể tích bọt khí tăng lên.")
     ]),

    ("charles-law", "Định luật Charles - Quá trình đẳng áp",
     "Quá trình biến đổi trạng thái khi áp suất không đổi gọi là quá trình đẳng áp. Định luật Charles: Với một lượng khí xác định ở áp suất không đổi, thể tích tỉ lệ thuận với nhiệt độ tuyệt đối T (Kelvin): V₁ / T₁ = V₂ / T₂ = const.",
     "Lượng khí có thể tích 3 lít ở 300 K. Giữ áp suất không đổi, khi nung nóng lên 400 K thì V₂ = 3 * (400 / 300) = 4 lít.",
     [
         ("Trong quá trình đẳng áp của một lượng khí lí tưởng xác định, thể tích của khối khí:",
          "Tỉ lệ thuận với nhiệt độ tuyệt đối T",
          "Tỉ lệ nghịch với nhiệt độ tuyệt đối T",
          "Tỉ lệ thuận với bình phương nhiệt độ Celsius",
          "Định luật Charles: V / T = const, thể tích tỉ lệ thuận với nhiệt độ tính theo thang Kelvin."),
         ("Một khối khí có thể tích 5 lít ở nhiệt độ 250 K. Giữ áp suất không đổi, khi nung nóng khối khí đến 350 K thì thể tích của nó bằng:",
          "7,0 lít",
          "6,0 lít",
          "8,5 lít",
          "Theo định luật Charles: V₂ = V₁ · (T₂ / T₁) = 5 · (350 / 250) = 5 · 1,4 = 7,0 lít."),
         ("Một quả bóng bàn bị móp nhẹ (vỏ chưa bị rách thủng), khi nhúng vào bát nước nóng thì quả bóng phồng trở lại như cũ. Hiện tượng này được giải thích chủ yếu bằng:",
          "Không khí bên trong nhận nhiệt nóng lên, giãn nở đẳng áp đẩy vỏ bóng phồng lại",
          "Nước nóng thấm qua vỏ bóng làm tăng thể tích chất lỏng bên trong",
          "Khối lượng không khí bên trong quả bóng tự động tăng lên",
          "Nhiệt độ không khí bên trong tăng làm áp suất và thể tích khí giãn nở, tạo lực đẩy vỏ nhựa phồng trở lại hình cầu.")
     ]),

    ("state-equation", "Phương trình trạng thái khí lí tưởng",
     "Phương trình trạng thái liên hệ cả ba thông số p, V, T của một lượng khí: (p₁V₁)/T₁ = (p₂V₂)/T₂ = const. Dạng phương trình Clapeyron - Mendeleev: p.V = n.R.T = (m/M).R.T với hằng số khí R ≈ 8,31 J/(mol.K).",
     "Bình 0,02 m³ chứa 2 mol khí ở 300 K: p = (2 * 8,31 * 300) / 0,02 = 249 300 Pa ≈ 2,49 × 10⁵ Pa.",
     [
         ("Phương trình trạng thái của khí lí tưởng cho một lượng khí xác định là:",
          "(p · V) / T = hằng số",
          "p · T / V = hằng số",
          "(p · T) · V = hằng số",
          "Phương trình trạng thái kết hợp định luật Boyle và Charles cho hệ thức tổng quát pV/T = const."),
         ("Tính áp suất của 1 mol khí lí tưởng chứa trong bình kín thể tích 0,01 m³ ở nhiệt độ 300 K. Cho hằng số khí R = 8,31 J/(mol·K).",
          "2,49 × 10⁵ Pa",
          "1,25 × 10⁵ Pa",
          "8,31 × 10⁴ Pa",
          "Áp dụng p = nRT / V = (1 · 8,31 · 300) / 0,01 = 2493 / 0,01 = 249 300 Pa ≈ 2,49 × 10⁵ Pa."),
         ("Bình xịt chứa khí nén luôn có cảnh báo 'Không để ở nơi có nhiệt độ trên 50 °C hoặc ném vào lửa' vì lý do vật lí nào?",
          "Thể tích bình cố định, nhiệt độ tăng cao làm áp suất khí tăng vọt gây nguy cơ nổ",
          "Nhiệt độ cao làm chất khí biến thành chất rắn làm nặng bình",
          "Vỏ kim loại của bình bị tan chảy ngay ở nhiệt độ 50 °C",
          "Theo quá trình đẳng tích (V không đổi), p tỉ lệ thuận với T. Nhiệt độ cao làm áp suất bên trong vượt quá giới hạn chịu lực của vỏ bình.")
     ]),

    ("molecular-pressure", "Áp suất khí theo mô hình động học phân tử",
     "Áp suất chất khí tác dụng lên thành bình sinh ra do các phân tử khí va chạm liên tục và truyền động lượng lên thành bình. Công thức cơ bản: p = (1/3) μ m v_tb² = (2/3) μ E_d, trong đó động năng tịnh tiến trung bình E_d = (3/2) k T (với hằng số Boltzmann k = R/N_A).",
     "Nhiệt độ T đo trực tiếp động năng tịnh tiến trung bình: khi T tăng gấp đôi thì E_d tăng gấp đôi.",
     [
         ("Động năng chuyển động nhiệt tịnh tiến trung bình của các phân tử khí lí tưởng tỉ lệ thuận với:",
          "Nhiệt độ tuyệt đối T của chất khí",
          "Thể tích V của bình chứa",
          "Khối lượng mol M của chất khí",
          "Hệ thức Boltzmann E_d = (3/2)kT cho thấy động năng tịnh tiến trung bình tỉ lệ thuận trực tiếp với nhiệt độ Kelvin."),
         ("Bản chất vi mô của áp suất chất khí tác dụng lên thành bình chứa là do:",
          "Hàng tỉ phân tử khí va chạm liên tục và truyền động lượng lên thành bình",
          "Trọng lực của các phân tử khí đè nén lên đáy bình chứa",
          "Lực hút tĩnh điện giữa thành bình và các electron của chất khí",
          "Mỗi phân tử va chạm đàn hồi vào thành bình làm đổi hướng vận tốc, truyền xung lượng; tổng hợp vô số va chạm tạo nên áp suất liên tục."),
         ("Khi đun nóng đẳng tích một khối khí lí tưởng để nhiệt độ tuyệt đối tăng từ 300 K lên 600 K, động năng tịnh tiến trung bình của các phân tử khí sẽ:",
          "Tăng lên 2 lần",
          "Tăng lên 4 lần",
          "Không thay đổi",
          "Vì E_d = (3/2)kT tỉ lệ thuận với T bậc nhất, nên khi T tăng từ 300 K lên 600 K (gấp 2 lần) thì động năng trung bình tăng 2 lần.")
     ]),

    # Block 3: Từ trường và Cảm ứng điện từ (Lessons 11-15)
    ("magnetic-field", "Từ trường và đường sức từ",
     "Từ trường là dạng vật chất tồn tại xung quanh nam châm hoặc dòng điện, tác dụng lực từ lên kim nam châm hoặc hạt điện tích chuyển động. Đường sức từ là các đường cong khép kín có chiều quy ước: vào cực Nam (S) ra cực Bắc (N).",
     "Kim la bàn đặt tự do luôn định hướng theo trục Bắc - Nam địa lí chứng tỏ Trái Đất có từ trường riêng (địa từ trường).",
     [
         ("Từ trường không tác dụng lực lên đối tượng nào sau đây?",
          "Một điện tích đứng yên trong từ trường",
          "Một đoạn dây dẫn có dòng điện chạy qua",
          "Một hạt mang điện đang chuyển động cắt các đường sức từ",
          "Lực từ chỉ xuất hiện khi điện tích chuyển động (hoặc có dòng điện); điện tích đứng yên không chịu tác dụng của lực từ."),
         ("Tính chất hình học cơ bản của các đường sức từ là:",
          "Là những đường cong khép kín (hoặc vô hạn ở hai đầu)",
          "Là các đoạn thẳng hở bắt đầu từ cực dương và kết thúc ở cực âm",
          "Luôn cắt nhau tại các điểm có từ trường mạnh",
          "Từ trường là trường xoáy, không có điểm khởi đầu hay kết thúc nên các đường sức từ luôn là đường cong kín."),
         ("Hiện tượng kim la bàn đặt tự do trên mặt đất luôn định hướng theo phương gần trùng với phương Bắc - Nam chứng tỏ:",
          "Xung quanh Trái Đất tồn tại một từ trường gọi là địa từ trường",
          "Không khí xung quanh Trái Đất bị nhiễm điện tĩnh điện mạnh",
          "Trọng lực của Trái Đất chỉ tác dụng theo phương Bắc - Nam",
          "Trái Đất hoạt động như một nam châm khổng lồ có cực từ Nam nằm gần cực Bắc địa lí và ngược lại, định hướng kim la bàn.")
     ]),

    ("magnetic-force", "Lực từ tác dụng lên đoạn dây dẫn mang dòng điện",
     "Lực từ tác dụng lên đoạn dây dẫn thẳng mang dòng điện I dài L đặt trong từ trường đều B: F = B.I.L.sin θ (θ là góc giữa dây dẫn và B). Phương của F vuông góc với mặt phẳng chứa đoạn dây và vectơ B; chiều xác định theo quy tắc bàn tay trái.",
     "Dây dài 0,2 m mang dòng điện 5 A đặt vuông góc với B = 0,4 T: F = 0,4 * 5 * 0,2 * sin 90° = 0,4 N.",
     [
         ("Lực từ tác dụng lên đoạn dây dẫn mang dòng điện đặt trong từ trường đều đạt giá trị cực đại khi góc θ giữa đoạn dây và vectơ cảm ứng từ bằng:",
          "90°",
          "0°",
          "180°",
          "F = B·I·L·sin θ. Hàm sin θ đạt cực đại bằng 1 khi góc θ = 90° (dây dẫn vuông góc với đường sức từ)."),
         ("Đoạn dây dẫn thẳng dài 0,2 m mang dòng điện cường độ 5 A đặt vuông góc với các đường cảm ứng từ của từ trường đều B = 0,4 T. Độ lớn lực từ tác dụng lên đoạn dây là:",
          "0,4 N",
          "0,8 N",
          "0,2 N",
          "F = B · I · L · sin 90° = 0,4 · 5 · 0,2 · 1 = 0,4 N."),
         ("Đặt hai dây dẫn thẳng dài song song mang dòng điện cùng chiều gần nhau. Tương tác từ giữa hai dây dẫn là:",
          "Chúng hút nhau",
          "Chúng đẩy nhau",
          "Không có lực tương tác nào giữa hai dây",
          "Áp dụng quy tắc nắm tay phải để tìm chiều từ trường của dây 1 và quy tắc bàn tay trái để tìm lực từ tác dụng lên dây 2, ta thấy hai dòng điện cùng chiều hút nhau.")
     ]),

    ("magnetic-induction", "Cảm ứng từ của dòng điện có hình dạng đặc biệt",
     "Độ lớn cảm ứng từ B đặc trưng cho tác dụng lực từ: dòng điện thẳng dài: B = 2·10⁻⁷·(I/r); dòng điện tròn bán kính R tại tâm: B = 2π·10⁻⁷·(I/R); trong lòng ống dây dài n vòng/m: B = 4π·10⁻⁷·n·I (từ trường đều).",
     "Dòng điện 10 A trong dây thẳng dài gây ra tại điểm cách 0,05 m cảm ứng từ B = 2·10⁻⁷ * 10 / 0,05 = 4 × 10⁻⁵ T.",
     [
         ("Vectơ cảm ứng từ B tại một điểm đặc trưng cho từ trường tại điểm đó về phương diện:",
          "Tác dụng lực từ",
          "Năng lượng tĩnh điện",
          "Tốc độ truyền sóng",
          "Cảm ứng từ B là đại lượng vectơ đặc trưng cho từ trường về mặt gây ra lực từ tác dụng lên dòng điện hay điện tích chuyển động."),
         ("Tính cảm ứng từ do dòng điện thẳng dài cường độ 10 A gây ra tại một điểm cách dây dẫn 0,05 m trong không khí.",
          "4 × 10⁻⁵ T",
          "2 × 10⁻⁵ T",
          "4 × 10⁻⁶ T",
          "B = 2 · 10⁻⁷ · (I / r) = 2 · 10⁻⁷ · (10 / 0,05) = 2 · 10⁻⁷ · 200 = 4 · 10⁻⁵ T."),
         ("Trong lòng một ống dây hình trụ dài có dòng điện không đổi chạy qua, từ trường có đặc điểm quan trọng nào sau đây?",
          "Là từ trường đều với các đường sức từ song song và cách đều nhau",
          "Cảm ứng từ tại tâm bằng 0 và tăng dần ra hai mép ngoài",
          "Đường sức từ xoáy tròn đồng tâm quanh trục ống dây",
          "Bên trong lòng ống dây hình trụ dài mang dòng điện, từ trường là từ trường đều, có vectơ cảm ứng từ không đổi ở mọi điểm.")
     ]),

    ("lorentz-force", "Lực Lorentz tác dụng lên điện tích chuyển động",
     "Điện tích q chuyển động với vận tốc v trong từ trường B chịu lực Lorentz: f = |q|.v.B.sin α (α là góc giữa v và B). Phương của f vuông góc với cả v và B. Vì f luôn vuông góc với vectơ vận tốc tức thời nên lực Lorentz không sinh công, chỉ làm đổi hướng chuyển động.",
     "Một proton bay vuông góc vào B: chuyển động theo quỹ đạo tròn với bán kính R = (m.v)/(|q|.B).",
     [
         ("Phương của lực Lorentz tác dụng lên hạt mang điện chuyển động trong từ trường đều có đặc điểm nào?",
          "Vuông góc với cả vectơ vận tốc v và vectơ cảm ứng từ B",
          "Cùng phương với vectơ cảm ứng từ B",
          "Cùng phương với vectơ vận tốc v của hạt",
          "Lực Lorentz có phương vuông góc với mặt phẳng tạo bởi vectơ vận tốc v và vectơ cảm ứng từ B."),
         ("Một hạt mang điện tích q = 1,6·10⁻¹⁹ C bay với vận tốc v = 2·10⁶ m/s vuông góc vào từ trường đều B = 0,5 T. Độ lớn lực Lorentz tác dụng lên hạt là:",
          "1,6 × 10⁻¹³ N",
          "3,2 × 10⁻¹³ N",
          "0,8 × 10⁻¹³ N",
          "f = |q| · v · B · sin 90° = 1,6 · 10⁻¹⁹ · 2 · 10⁶ · 0,5 · 1 = 1,6 · 10⁻¹³ N."),
         ("Lực Lorentz có thực hiện công cơ học lên một điện tích chuyển động tự do trong từ trường đều không? Vì sao?",
          "Không thực hiện công, vì lực Lorentz luôn vuông góc với vectơ vận tốc tức thời của hạt",
          "Có thực hiện công dương làm tăng động năng của hạt",
          "Có thực hiện công âm làm hạt chuyển động chậm dần đều",
          "Công tức thời dA = f · v · dt. Do lực Lorentz f ⊥ v nên tích vô hướng f · v = 0, công toàn phần luôn bằng 0.")
     ]),

    ("electromagnetic-induction", "Hiện tượng cảm ứng điện từ và định luật Faraday",
     "Từ thông qua diện tích S: Φ = B.S.cos α. Khi từ thông qua mạch kín biến thiên, trong mạch xuất hiện dòng điện cảm ứng (hiện tượng cảm ứng điện từ). Độ lớn suất điện động cảm ứng: |e_c| = |ΔΦ/Δt|. Định luật Lenz: dòng điện cảm ứng có chiều chống lại sự biến thiên từ thông sinh ra nó.",
     "Từ thông giảm từ 0,08 Wb xuống 0,02 Wb trong 0,1 s: |e_c| = |(0,02 - 0,08)/0,1| = 0,6 V.",
     [
         ("Điều kiện tiên quyết để xuất hiện dòng điện cảm ứng trong một khung dây dẫn kín là:",
          "Từ thông qua diện tích giới hạn bởi khung dây biến thiên theo thời gian",
          "Khung dây phải được đặt trong từ trường có độ lớn cực kì lớn",
          "Khung dây phải luôn chuyển động nhanh dần đều",
          "Hiện tượng cảm ứng điện từ chỉ xảy ra khi có sự biến thiên từ thông gửi qua mạch kín theo thời gian."),
         ("Từ thông qua một mạch kín biến thiên đều giảm từ 0,08 Wb xuống 0,02 Wb trong khoảng thời gian 0,1 s. Độ lớn suất điện động cảm ứng xuất hiện trong mạch là:",
          "0,6 V",
          "0,06 V",
          "6,0 V",
          "Theo định luật Faraday: |e_c| = |ΔΦ / Δt| = |(0,02 - 0,08) / 0,1| = |-0,06 / 0,1| = 0,6 V."),
         ("Đưa cực Bắc của một thanh nam châm lại gần một vòng nhôm kín nhẹ treo tự do bằng sợi chỉ. Vòng nhôm sẽ phản ứng thế nào?",
          "Vòng nhôm bị đẩy ra xa thanh nam châm",
          "Vòng nhôm bị hút mạnh vào thanh nam châm",
          "Vòng nhôm đứng yên không dịch chuyển",
          "Theo định luật Lenz, dòng điện cảm ứng sinh ra từ trường chống lại sự lại gần của nam châm (mặt đối diện biến thành cực Bắc), gây lực đẩy đẩy vòng nhôm ra xa.")
     ]),

    # Block 4: Vật lí hạt nhân (Lessons 16-20)
    ("nuclear-structure", "Cấu tạo hạt nhân và năng lượng liên kết",
     "Hạt nhân nguyên tử cấu tạo từ Z proton và (A - Z) neutron. Khối lượng hạt nhân luôn nhỏ hơn tổng khối lượng các nucleon tạo nên nó một lượng gọi là độ hụt khối: Δm = Z.m_p + (A - Z).m_n - m_X. Năng lượng liên kết E_lk = Δm.c²; năng lượng liên kết riêng E_lk/A đặc trưng cho độ bền vững của hạt nhân (lớn nhất ở các hạt nhân có 50 < A < 80).",
     "Hạt He-4 có Δm = 0,0304 u: E_lk = 0,0304 * 931,5 ≈ 28,3 MeV. Năng lượng liên kết riêng là 28,3 / 4 ≈ 7,07 MeV/nucleon.",
     [
         ("Hạt nhân nguyên tử được cấu tạo bởi hai loại hạt cơ bản (gọi chung là nucleon) gồm:",
          "Proton và neutron",
          "Proton và electron",
          "Neutron và electron",
          "Nucleon cấu tạo nên hạt nhân nguyên tử bao gồm các hạt proton mang điện tích dương và neutron không mang điện."),
         ("Hạt nhân He-4 có độ hụt khối Δm = 0,0304 u. Cho biết 1 u ≈ 931,5 MeV/c². Năng lượng liên kết của hạt nhân He-4 xấp xỉ bằng:",
          "28,3 MeV",
          "14,2 MeV",
          "56,6 MeV",
          "E_lk = Δm · c² = 0,0304 u · c² = 0,0304 · 931,5 MeV ≈ 28,3 MeV."),
         ("Đại lượng vật lí nào sau đây đặc trưng cho mức độ bền vững của một hạt nhân nguyên tử?",
          "Năng lượng liên kết riêng (năng lượng liên kết tính trên một nucleon)",
          "Năng lượng liên kết toàn phần của hạt nhân",
          "Độ hụt khối toàn phần của hạt nhân",
          "Hạt nhân có năng lượng liên kết riêng E_lk/A càng lớn thì càng bền vững; bền vững nhất là các hạt nhân có số khối trung bình như sắt Fe-56.")
     ]),

    ("radioactivity", "Hiện tượng phóng xạ và định luật phóng xạ",
     "Phóng xạ là quá trình phân rã tự phát của hạt nhân không bền vững kèm theo phát ra các tia phóng xạ (α: hạt nhân He-4; β⁻: electron; β⁺: positron; γ: sóng điện từ photon năng lượng cao). Định luật phóng xạ: N(t) = N₀ · 2^(-t/T) = N₀ · e^(-λt) với T là chu kì bán rã.",
     "Chất phóng xạ có T = 8 ngày, sau 24 ngày (3 chu kì) số hạt nhân còn lại là N = N₀ / 2³ = N₀ / 8 = 12,5% ban đầu.",
     [
         ("Tia phóng xạ nào sau đây có bản chất là sóng điện từ có bước sóng cực ngắn và năng lượng photon rất cao?",
          "Tia gamma (γ)",
          "Tia alpha (α)",
          "Tia beta trừ (β⁻)",
          "Tia gamma là bức xạ điện từ không mang điện, có tần số rất cao và khả năng đâm xuyên mạnh nhất."),
         ("Một chất phóng xạ có chu kì bán rã T = 8 ngày. Sau khoảng thời gian 24 ngày, tỉ lệ phần trăm số hạt nhân phóng xạ còn lại chưa phân rã so với ban đầu là:",
          "12,5%",
          "25,0%",
          "6,25%",
          "Số chu kì bán rã trôi qua là k = t / T = 24 / 8 = 3. Số hạt còn lại: N / N₀ = (1/2)³ = 1/8 = 12,5%."),
         ("Đồng vị phóng xạ Carbon-14 (C-14) có chu kì bán rã khoảng 5730 năm được ứng dụng rộng rãi trong khoa học để:",
          "Xác định niên đại của các cổ vật hữu cơ và mẫu hóa thạch khảo cổ học",
          "Chế tạo nhiên liệu phản ứng nhiệt hạch trên tàu vũ trụ",
          "Làm nguồn phát tia laser trong phẫu thuật mắt",
          "Thực vật hấp thụ C-14 qua quang hợp khi còn sống; khi sinh vật chết lượng C-14 phân rã theo thời gian giúp xác định tuổi mẫu vật.")
     ]),

    ("nuclear-reactions", "Phản ứng hạt nhân và các định luật bảo toàn",
     "Phản ứng hạt nhân tuân theo 4 định luật bảo toàn: bảo toàn điện tích (Z), bảo toàn số nucleon (A), bảo toàn năng lượng toàn phần và bảo toàn động lượng. Không có định luật bảo toàn khối lượng nghỉ. Năng lượng tỏa ra: ΔE = (m_trước - m_sau).c².",
     "Phản ứng: ₁²H + ₁³H → ₂⁴He + ₀¹n. Tổng Z: 1 + 1 = 2 + 0; Tổng A: 2 + 3 = 4 + 1. Khối lượng giảm nên tỏa năng lượng ~17,6 MeV.",
     [
         ("Định luật nào sau đây KHÔNG áp dụng cho các phản ứng hạt nhân?",
          "Định luật bảo toàn khối lượng nghỉ",
          "Định luật bảo toàn điện tích",
          "Định luật bảo toàn số nucleon (số khối A)",
          "Trong phản ứng hạt nhân, tổng khối lượng nghỉ trước và sau phản ứng có sự chênh lệch (độ hụt khối chuyển hóa thành năng lượng) nên khối lượng nghỉ không bảo toàn."),
         ("Trong phản ứng hạt nhân: ₁²H + ₁³H → ₂⁴He + X. Hạt X sinh ra là hạt nào sau đây?",
          "Hạt neutron (₀¹n)",
          "Hạt proton (₁¹p)",
          "Hạt electron (-₁⁰e)",
          "Áp dụng bảo toàn điện tích Z: 1 + 1 = 2 + Z_X ⇒ Z_X = 0. Bảo toàn số nucleon A: 2 + 3 = 4 + A_X ⇒ A_X = 1. Hạt có Z = 0, A = 1 là neutron."),
         ("Nếu một phản ứng hạt nhân có tổng khối lượng nghỉ của các hạt trước phản ứng m_trước lớn hơn tổng khối lượng nghỉ các hạt sau phản ứng m_sau thì phản ứng đó:",
          "Tỏa năng lượng",
          "Thu năng lượng",
          "Không giải phóng hay hấp thụ năng lượng",
          "Độ biến thiên năng lượng ΔE = (m_trước - m_sau)c² > 0, phần khối lượng giảm chuyển thành động năng hạt và năng lượng photon tỏa ra môi trường.")
     ]),

    ("fission-fusion", "Phản ứng phân hạch và phản ứng nhiệt hạch",
     "Phân hạch là phản ứng hạt nhân nặng (như U-235) hấp thụ neutron chậm rồi vỡ thành hai mảnh trung bình và giải phóng 2-3 neutron mới (tạo phản ứng dây chuyền; k = 1 là tới hạn có kiểm soát). Nhiệt hạch là phản ứng kết hợp hai hạt nhân nhẹ (H-2, H-3) thành hạt nhân nặng hơn ở nhiệt độ hàng trăm triệu độ.",
     "Phản ứng nhiệt hạch tỏa năng lượng lớn hơn phân hạch tính trên cùng một đơn vị khối lượng và là nguồn năng lượng của các ngôi sao.",
     [
         ("Bản chất của phản ứng nhiệt hạch (phản ứng tổng hợp hạt nhân) là quá trình:",
          "Kết hợp hai hạt nhân rất nhẹ thành một hạt nhân nặng hơn ở nhiệt độ cực kì cao",
          "Bắn phá hạt nhân nặng bằng neutron làm nó vỡ thành hai mảnh nhẹ",
          "Hạt nhân tự phát bức xạ tia gamma mà không thay đổi cấu tạo",
          "Nhiệt hạch là phản ứng kết hợp hai hạt nhân có số khối nhỏ (như hydro, deuteri, triti) thành hạt nhân nặng hơn ở điều kiện nhiệt độ hàng trăm triệu độ."),
         ("Để duy trì phản ứng phân hạch dây chuyền tự duy trì có kiểm soát hoạt động ổn định trong lò phản ứng hạt nhân, hệ số nhân neutron k phải bằng:",
          "k = 1",
          "k > 1",
          "k < 1",
          "Hệ số k = 1 gọi là trạng thái tới hạn: số neutron sinh ra trung bình bù đắp đủ số neutron thất thoát và hấp thụ, duy trì công suất phát điện ổn định."),
         ("Nguồn năng lượng khổng lồ phát ra liên tục hàng tỉ năm của Mặt Trời và các vì sao trong vũ trụ có nguồn gốc từ:",
          "Các phản ứng nhiệt hạch tổng hợp hạt nhân hydrogen thành helium",
          "Các phản ứng phân hạch dây chuyền của uranium",
          "Sự cháy oxy hóa khử hóa học của khí methane",
          "Tại vùng lõi Mặt Trời với áp suất và nhiệt độ hàng chục triệu độ Kelvin, phản ứng tổng hợp hạt nhân hydro tạo heli diễn ra liên tục giải phóng năng lượng bức xạ.")
     ]),

    ("radiation-safety", "Ứng dụng bức xạ và an toàn hạt nhân",
     "Ứng dụng bức xạ: y học (xạ trị, máy đo PET, dao gamma), nông nghiệp (chiếu xạ giống đột biến, bảo quản nông sản), công nghiệp (kiểm tra khuyết tật mối hàn bằng tia gamma). Nguyên tắc an toàn: giảm thời gian phơi nhiễm, tăng khoảng cách tới nguồn và sử dụng vật liệu che chắn hấp thụ (chì, bê tông cốt thép).",
     "Phòng chụp X-quang hoặc xạ trị luôn có tường lót chì và kính pha chì để ngăn bức xạ lọt ra ngoài.",
     [
         ("Biện pháp nào sau đây giúp giảm liều chiếu xạ nguy hiểm cho người khi làm việc gần nguồn phóng xạ?",
          "Tăng khoảng cách tới nguồn và sử dụng tấm che chắn chì/bê tông dày",
          "Đứng càng gần nguồn phát phóng xạ càng tốt",
          "Tăng tối đa thời gian tiếp xúc trực tiếp với nguồn phóng xạ",
          "Liều bức xạ tỉ lệ nghịch với bình phương khoảng cách và giảm theo hàm mũ khi qua các tấm che chắn chì hoặc bê tông."),
         ("Ứng dụng quan trọng của nguồn phóng xạ phát tia gamma trong y học hiện đại là:",
          "Xạ trị tiêu diệt tế bào khối u ác tính hoặc phẫu thuật bằng dao mổ gamma",
          "Uống trực tiếp để cung cấp năng lượng trao đổi chất cho cơ thể",
          "Đun sôi nước khử trùng phòng bệnh nhanh chóng",
          "Tia gamma mang năng lượng photon lớn được hội tụ chính xác để phá hủy cấu trúc DNA của các tế bào ung thư mà ít tổn thương mô lành."),
         ("Biển báo có hình cánh quạt ba lá màu đen trên nền vàng tươi gắn tại cửa phòng chuyên dụng mang ý nghĩa cảnh báo gì?",
          "Khu vực có nguy cơ bức xạ ion hóa nguy hiểm cần tuân thủ an toàn",
          "Khu vực có quạt thông gió công suất cao đang quay nhanh",
          "Khu vực nguy cơ cháy nổ khí gas dễ bắt lửa",
          "Biểu tượng ba cánh quạt (trefoil) là kí hiệu quốc tế chuẩn cảnh báo khu vực chứa nguồn phóng xạ ion hóa hoặc thiết bị phát tia bức xạ hạt nhân.")
     ])
]

CHEMISTRY_LESSONS = [
    # Block 1: Ester - Lipid và Xà phòng (Lessons 1-5)
    ("ester-structure", "Cấu tạo, danh pháp và tính chất vật lí của ester",
     "Ester no đơn chức mạch hở có công thức chung C_n H_2n O_2 (n ≥ 2). Tên gọi: tên gốc hydrocarbon R' + tên gốc acid đuôi 'at'. Ester có nhiệt độ sôi thấp hơn alcohol và carboxylic acid có cùng phân tử khối do không có liên kết hydrogen liên phân tử. Thường ít tan trong nước và có mùi thơm hoa quả.",
     "Ester CH₃COOC₂H₅ có tên gọi là ethyl acetate; có mùi thơm dễ chịu của hoa quả chín.",
     [
         ("Tên gọi theo danh pháp thông thường của ester có công thức cấu tạo thu gọn CH₃COOC₂H₅ là:",
          "Ethyl acetate",
          "Methyl acetate",
          "Ethyl propionate",
          "Tên gọi gồm tên gốc ethyl (C₂H₅-) kết hợp với gốc acetate (CH₃COO-), thành ethyl acetate."),
         ("Trong các chất có khối lượng phân tử xấp xỉ nhau sau đây, chất nào có nhiệt độ sôi thấp nhất?",
          "HCOOCH₃ (Methyl formate)",
          "CH₃COOH (Acetic acid)",
          "C₃H₇OH (Propan-1-ol)",
          "Ester không có nguyên tử hydro linh động liên kết với oxy nên không tạo được liên kết hydro liên phân tử, do đó nhiệt độ sôi thấp hơn hẳn acid và alcohol."),
         ("Isoamyl acetate là ester có mùi thơm đặc trưng của quả chuối chín. Công thức phân tử tổng quát của một ester no, đơn chức, mạch hở là:",
          "CnH2nO2 (n ≥ 2)",
          "CnH2n+2O2 (n ≥ 1)",
          "CnH2n-2O2 (n ≥ 3)",
          "Ester no đơn chức mạch hở được tạo thành từ acid no đơn chức và alcohol no đơn chức, có dạng C_n H_2n O_2 với số nguyên tử cacbon n ≥ 2.")
     ]),

    ("ester-reactions", "Tính chất hóa học của ester",
     "Phản ứng thủy phân trong môi trường acid (H₂SO₄ loãng, đun nóng) là phản ứng thuận nghịch: RCOOR' + H₂O ⇌ RCOOH + R'OH. Phản ứng thủy phân trong dung dịch kiềm (xà phòng hóa) là phản ứng một chiều: RCOOR' + NaOH → RCOONa + R'OH.",
     "Thủy phân ethyl acetate bằng dung dịch NaOH: CH₃COOC₂H₅ + NaOH → CH₃COONa + C₂H₅OH.",
     [
         ("Phản ứng thủy phân ester trong dung dịch kiềm đun nóng còn được gọi là phản ứng gì?",
          "Phản ứng xà phòng hóa",
          "Phản ứng ester hóa",
          "Phản ứng trùng hợp",
          "Thủy phân ester tạo ra muối của acid béo (thành phần xà phòng) nên phản ứng này gọi là phản ứng xà phòng hóa."),
         ("Đun nóng ethyl formate (HCOOC₂H₅) với dung dịch NaOH vừa đủ thu được các sản phẩm là:",
          "HCOONa và C₂H₅OH",
          "CH₃COONa và CH₃OH",
          "HCOONa và CH₃OH",
          "Phương trình phản ứng: HCOOC₂H₅ + NaOH → HCOONa (sodium formate) + C₂H₅OH (ethanol)."),
         ("Điểm khác biệt căn bản về chiều phản ứng giữa thủy phân ester trong môi trường acid và kiềm là:",
          "Thủy phân trong acid là phản ứng thuận nghịch hai chiều, trong kiềm là phản ứng một chiều",
          "Thủy phân trong acid là phản ứng một chiều, trong kiềm là thuận nghịch",
          "Cả hai phản ứng đều là phản ứng thuận nghịch hai chiều",
          "Trong môi trường kiềm, muối sinh ra không phản ứng lại với alcohol để tái tạo ester nên phản ứng diễn ra một chiều hoàn toàn.")
     ]),

    ("lipid-triglyceride", "Cấu tạo của lipid và triglyceride",
     "Chất béo (triglyceride) là triester của glycerol với các acid béo mạch dài không phân nhánh có số carbon chẵn. Acid béo no: palmitic (C₁₅H₃₁COOH), stearic (C₁₇H₃₅COOH); tạo mỡ động vật thể rắn. Acid béo không no: oleic (C₁₇H₃₃COOH, 1 liên kết đôi C=C), linoleic (C₁₇H₃₁COOH); tạo dầu thực vật thể lỏng.",
     "Triolein ((C₁₇H₃₃COO)₃C₃H₅) là chất béo không no ở thể lỏng ở nhiệt độ thường; Tristearin ((C₁₇H₃₅COO)₃C₃H₅) là chất béo no ở thể rắn.",
     [
         ("Glycerol (glycerin) là polyalcohol ba chức tham gia cấu tạo nên mọi triglyceride có công thức là:",
          "C₃H₅(OH)₃",
          "C₂H₄(OH)₂",
          "C₃H₇OH",
          "Glycerol là propane-1,2,3-triol có 3 nhóm -OH liên kết với 3 nguyên tử carbon: C₃H₅(OH)₃."),
         ("Acid béo nào sau đây là acid béo no phổ biến trong mỡ động vật?",
          "Acid stearic (C₁₇H₃₅COOH)",
          "Acid oleic (C₁₇H₃₃COOH)",
          "Acid linoleic (C₁₇H₃₁COOH)",
          "Acid stearic có gốc hydrocarbon C₁₇H₃₅- no (dạng C_n H_2n+1) nên là acid béo no."),
         ("Chất béo triolein ((C₁₇H₃₃COO)₃C₃H₅) ở điều kiện nhiệt độ phòng tồn tại ở thể lỏng vì lý do cấu tạo nào?",
          "Phân tử chứa các gốc acid béo không no có nối đôi C=C làm giảm nhiệt độ nóng chảy",
          "Phân tử có khối lượng mol quá nhỏ so với tristearin",
          "Phân tử tan hoàn toàn trong nước tạo dung dịch",
          "Các liên kết đôi cis-C=C tạo cấu trúc gấp khúc ngăn cản sự xếp chặt các phân tử, làm giảm lực hút van der Waals và hạ nhiệt độ nóng chảy.")
     ]),

    ("lipid-reactions", "Phản ứng thủy phân và hydrogen hóa chất béo",
     "Thủy phân chất béo trong kiềm: (RCOO)₃C₃H₅ + 3NaOH → 3RCOONa + C₃H₅(OH)₃. Phản ứng hydrogen hóa chất béo lỏng (dầu thực vật) thành chất béo rắn (mỡ nhân tạo) dùng xúc tác Ni, t°: chuyển acid béo không no thành no. Hiện tượng ôi mỡ do liên kết đôi C=C bị oxi hóa chậm bởi oxygen không khí tạo peroxide rồi phân hủy thành aldehyde có mùi khó chịu.",
     "Xà phòng hóa 1 mol tristearin bằng NaOH cần đúng 3 mol NaOH và thu được 1 mol glycerol cùng 3 mol sodium stearate.",
     [
         ("Xà phòng hóa hoàn toàn 1 mol chất béo triglyceride bằng dung dịch NaOH luôn thu được bao nhiêu mol glycerol?",
          "1 mol",
          "3 mol",
          "2 mol",
          "Mỗi phân tử triglyceride liên kết 3 gốc acid với 1 khung glycerol, do đó thủy phân 1 mol chất béo luôn giải phóng đúng 1 mol glycerol."),
         ("Quá trình chuyển hóa dầu thực vật (chất béo lỏng) thành bơ nhân tạo (chất béo rắn) trong công nghiệp dựa trên phản ứng:",
          "Hydrogen hóa chất béo không no có xúc tác nickel ở nhiệt độ cao",
          "Thủy phân chất béo trong môi trường acid loãng",
          "Oxi hóa cắt mạch hydrocarbon bằng thuốc tím",
          "Cộng hydrogen vào các liên kết đôi C=C của gốc acid béo không no chuyển thành gốc acid no có nhiệt độ nóng chảy cao hơn ở thể rắn."),
         ("Dầu mỡ động thực vật để lâu ngày trong không khí thường bị ôi thiu và có mùi khét khó chịu. Nguyên nhân hóa học chính là do:",
          "Liên kết đôi C=C của acid béo không no bị oxi không khí oxi hóa chậm thành peroxide rồi phân hủy thành aldehyde",
          "Glycerol bị bốc hơi hoàn toàn vào không khí",
          "Chất béo tự phản ứng với khí nitơ trong khí quyển",
          "Sự oxi hóa tự phát các nối đôi C=C bởi oxi tạo các gốc tự do peroxide, phân cắt thành các aldehyde và ketone dễ bay hơi có mùi hôi.")
     ]),

    ("soap-detergent", "Xà phòng và chất giặt rửa tổng hợp",
     "Xà phòng là hỗn hợp muối sodium hoặc potassium của các acid béo (như C₁₇H₃₅COONa). Phân tử gồm đầu ưa nước (-COO⁻Na⁺) và đuôi kị nước (gốc hydrocarbon dài ưa dầu). Cơ chế giặt: đuôi kị nước bám vào vết dầu mỡ, đầu ưa nước hướng ra ngoài tạo hạt micelle phân tán vào nước. Chất giặt rửa tổng hợp không bị kết tủa trong nước cứng (chứa Ca²⁺, Mg²⁺).",
     "Khi rửa tay với xà phòng, các hạt dầu mỡ được bao bọc bởi các phân tử xà phòng và cuốn trôi theo dòng nước xối.",
     [
         ("Muối nào sau đây là thành phần hoạt chất chính trong bánh xà phòng thông thường?",
          "Sodium stearate (C₁₇H₃₅COONa)",
          "Sodium chloride (NaCl)",
          "Calcium carbonate (CaCO₃)",
          "Xà phòng là muối sodium hoặc potassium của các acid béo, phổ biến là sodium stearate và sodium palmitate."),
         ("Cơ chế làm sạch vết bẩn dầu mỡ của xà phòng dựa vào đặc điểm cấu tạo phân tử nào?",
          "Phân tử gồm một đầu ưa nước tan trong nước và một đuôi kị nước ưa dầu mỡ",
          "Phân tử có tính acid mạnh hòa tan mọi kim loại",
          "Phân tử đóng vai trò là chất oxi hóa cực mạnh đốt cháy dầu mỡ",
          "Đuôi kị nước găm vào giọt dầu mỡ, đầu ưa nước quay ra ngoài nước tạo thành các hạt micelle lơ lửng bị dòng nước cuốn đi."),
         ("Ưu điểm nổi bật của chất giặt rửa tổng hợp so với xà phòng truyền thống khi sử dụng trong môi trường nước cứng là:",
          "Vẫn duy trì khả năng giặt rửa tốt vì muối của chúng với Ca²⁺ và Mg²⁺ không bị kết tủa",
          "Hoàn toàn không có bọt nên xả nước rất nhanh",
          "Tự phân hủy thành khí cacbonic ngay sau khi hòa tan",
          "Xà phòng thông thường tạo kết tủa (RCOO)₂Ca và (RCOO)₂Mg làm mất tác dụng giặt, còn chất giặt rửa tổng hợp (muối sulfonate) tan tốt trong nước cứng.")
     ]),

    # Block 2: Carbohydrate (Lessons 6-10)
    ("carbohydrate-classification", "Phân loại carbohydrate và cấu trúc Glucose, Fructose",
     "Carbohydrate chia thành 3 nhóm: Monosaccharide (glucose, fructose C₆H₁₂O₆ không bị thủy phân); Disaccharide (saccharose, maltose C₁₂H₂₂O₁₁ thủy phân ra 2 monosaccharide); Polysaccharide (tinh bột, cellulose (C₆H₁₀O₅)_n thủy phân ra nhiều glucose). Glucose là polyhydroxyaldehyde; Fructose là polyhydroxyketone. Trong dung dịch, glucose chủ yếu tồn tại ở dạng mạch vòng α và β.",
     "Glucose có trong máu người với nồng độ ổn định khoảng 0,1% (khoảng 4,4 - 7,2 mmol/L).",
     [
         ("Chất nào sau đây thuộc loại monosaccharide không bị thủy phân?",
          "Glucose",
          "Saccharose",
          "Tinh bột",
          "Monosaccharide là carbohydrate đơn giản nhất không thể bị thủy phân thành đường nhỏ hơn, ví dụ glucose và fructose."),
         ("Glucose và fructose là hai chất:",
          "Đồng phân cấu tạo của nhau có cùng công thức phân tử C₆H₁₂O₆",
          "Đồng đẳng kế tiếp nhau trong cùng dãy đồng đẳng",
          "Thuộc hai loại đường disaccharide khác nhau",
          "Cả glucose và fructose đều có công thức phân tử C₆H₁₂O₆ nhưng glucose chứa nhóm -CHO còn fructose chứa nhóm >C=O, nên là đồng phân."),
         ("Trong dung dịch nước ở trạng thái cân bằng, các phân tử glucose chủ yếu tồn tại ở dạng cấu trúc nào?",
          "Dạng mạch vòng 6 cạnh (dạng α-glucose và β-glucose)",
          "Dạng mạch hở thẳng chiếm trên 99%",
          "Dạng phân tử bị phân li hoàn toàn thành các ion",
          "Trong dung dịch, nhóm -OH ở C5 cộng đóng vòng với nhóm -CHO ở C1 tạo vòng bán axetal 6 cạnh, chỉ dưới 0,1% ở dạng mạch hở.")
     ]),

    ("glucose-fructose-reactions", "Tính chất hóa học của Glucose và Fructose",
     "Nhóm polyalcohol: glucose hòa tan Cu(OH)₂ ở nhiệt độ thường tạo phức chất xanh lam thẫm. Nhóm aldehyde (-CHO): glucose tham gia phản ứng tráng bạc với [Ag(NH₃)₂]OH (tạo 2 Ag), làm mất màu dung dịch nước bromine, và khử Cu(OH)₂ đun nóng tạo kết tủa Cu₂O đỏ gạch. Fructose không có nhóm -CHO nhưng trong môi trường kiềm chuyển hóa thành glucose nên cũng tráng bạc.",
     "1 mol glucose tráng bạc hoàn toàn giải phóng 2 mol Ag kim loại sáng bóng bám lên thành ống nghiệm.",
     [
         ("Thuốc thử được dùng để nhận biết dung dịch glucose tạo dung dịch phức chất màu xanh lam thẫm ở nhiệt độ thường là:",
          "Cu(OH)₂ trong môi trường kiềm",
          "Dung dịch AgNO₃ trong NH₃",
          "Dung dịch nước vôi trong Ca(OH)₂",
          "Vì glucose có nhiều nhóm -OH liền kề (polyalcohol) nên hòa tan Cu(OH)₂ ở nhiệt độ thường tạo dung dịch màu xanh lam thẫm."),
         ("Cho 1 mol glucose tham gia phản ứng tráng bạc hoàn toàn với dung dịch AgNO₃ trong NH₃ dư, lượng Ag kim loại kết tủa thu được tối đa là:",
          "2 mol",
          "1 mol",
          "4 mol",
          "Phân tử glucose có 1 nhóm aldehyde -CHO, theo phương trình phản ứng tráng bạc: 1 mol -CHO giải phóng 2 mol Ag kim loại."),
         ("Để phân biệt dung dịch glucose và dung dịch fructose trong phòng thí nghiệm, người ta thường dùng thuốc thử nào?",
          "Nước bromine (nước brom)",
          "Dung dịch AgNO₃ trong NH₃",
          "Cu(OH)₂ đun nóng",
          "Glucose có nhóm -CHO khử được nước brom làm mất màu, trong khi fructose là ketone không làm mất màu nước brom trong môi trường acid.")
     ]),

    ("sucrose-maltose", "Saccharose và Maltose",
     "Saccharose (đường mía, C₁₂H₂₂O₁₁) gồm 1 gốc α-glucose liên kết với 1 gốc β-fructose qua liên kết α-1,2-glycosidic. Do không còn nhóm -CHO tự do nên saccharose không có tính khử (không tráng bạc, không mất màu nước brom). Khi thủy phân có xúc tác acid hoặc enzyme, 1 mol saccharose tạo 1 mol glucose và 1 mol fructose.",
     "Đường kính trắng sử dụng hàng ngày có độ tinh khiết cao với thành phần chính là saccharose chiết xuất từ mía đường.",
     [
         ("Đường mía, đường củ cải dùng làm chất tạo ngọt trong đời sống hàng ngày có thành phần chính là:",
          "Saccharose",
          "Glucose",
          "Maltose",
          "Saccharose là disaccharide phổ biến nhất, được sản xuất công nghiệp từ cây mía hoặc củ cải đường."),
         ("Khi thủy phân hoàn toàn 1 mol saccharose trong môi trường acid đun nóng, ta thu được sản phẩm gồm:",
          "1 mol glucose và 1 mol fructose",
          "2 mol glucose",
          "2 mol fructose",
          "Phân tử saccharose được cấu tạo từ 1 gốc glucose và 1 gốc fructose, khi thủy phân phân cắt liên kết glycosidic tạo tỉ lệ mol 1:1."),
         ("Dung dịch saccharose tinh khiết có tham gia phản ứng tráng bạc không? Vì sao?",
          "Không, vì phân tử không còn nhóm aldehyde (-CHO) dạng mạch hở tự do",
          "Có, vì phân tử có khối lượng lớn hơn glucose",
          "Có, vì phân tử chứa nhiều nhóm hydroxy (-OH)",
          "Hai gốc monosaccharide liên kết với nhau qua hai nguyên tử C anomer của cả hai nhóm carbonyl, khóa kín vòng nên không mở mạch tạo nhóm -CHO được.")
     ]),

    ("starch", "Cấu trúc và tính chất của Tinh bột",
     "Tinh bột là polysaccharide ((C₆H₁₀O₅)_n) gồm hai thành phần: amylose (20-30%, mạch dài không phân nhánh, xoắn ốc liên kết α-1,4-glycosidic) và amylopectin (70-80%, cấu trúc phân nhánh liên kết α-1,4 và α-1,6-glycosidic). Tác dụng đặc trưng: hồ tinh bột tác dụng với dung dịch iodine cho hợp chất màu xanh tím (mất màu khi đun nóng, xuất hiện lại khi để nguội).",
     "Nhỏ một giọt dung dịch cồn iodine lên lát cắt củ khoai tây, vị trí tiếp xúc lập tức chuyển sang màu xanh tím đậm.",
     [
         ("Thuốc thử đặc trưng dùng để nhận biết hồ tinh bột bằng phản ứng tạo màu xanh tím là:",
          "Dung dịch iodine (I₂)",
          "Dung dịch Cu(OH)₂",
          "Dung dịch AgNO₃/NH₃",
          "Cấu trúc xoắn ốc của amylose tạo các khoang rỗng hấp phụ phân tử iodine tạo phức chất có màu xanh tím đặc trưng."),
         ("Điểm khác biệt cơ bản về mặt cấu trúc giữa amylose và amylopectin trong hạt tinh bột là:",
          "Amylose có cấu trúc mạch không phân nhánh, còn amylopectin có cấu trúc mạch phân nhánh",
          "Amylose là đường đơn, còn amylopectin là đường đôi",
          "Amylose có liên kết peptide, còn amylopectin có liên kết ester",
          "Amylose gồm các gốc α-glucose liên kết α-1,4 mạch thẳng xoắn; amylopectin có thêm các nhánh tại vị trí liên kết α-1,6-glycosidic."),
         ("Phương trình hóa học tổng quát biểu diễn quá trình quang hợp của cây xanh tạo ra tinh bột dưới tác dụng của ánh sáng mặt trời là:",
          "6n CO₂ + 5n H₂O → (C₆H₁₀O₅)n + 6n O₂ (xúc tác ánh sáng, diệp lục)",
          "n CO₂ + n H₂O → (CH₂O)n + n O₂",
          "6 CO₂ + 6 H₂O → C₆H₁₂O₆ + 6 O₂",
          "Quá trình quang hợp của diệp lục hấp thụ năng lượng mặt trời chuyển hóa CO₂ và H₂O thành tinh bột (C₆H₁₀O₅)_n và giải phóng oxy O₂.")
     ]),

    ("cellulose", "Cấu tạo, tính chất và ứng dụng của Cellulose",
     "Cellulose ((C₆H₁₀O₅)_n hay [C₆H₇O₂(OH)₃]_n) là thành phần chính tạo nên màng tế bào thực vật (gỗ, bông gòn). Cấu tạo từ các gốc β-glucose liên kết β-1,4-glycosidic tạo mạch kéo dài không phân nhánh. Cellulose không tan trong nước và dung môi hữu cơ thường; tan trong nước Schweizer ([Cu(NH₃)₄](OH)₂). Tác dụng với HNO₃ đặc có H₂SO₄ đặc làm xúc tác tạo cellulose trinitrate dùng làm thuốc súng không khói.",
     "Sợi bông nõn chứa gần 98% cellulose tinh khiết, dùng làm sợi dệt vải may mặc.",
     [
         ("Trong công thức cấu tạo thu gọn [C₆H₇O₂(OH)₃]_n của cellulose, mỗi mắt xích C₆H₁₀O₅ chứa bao nhiêu nhóm hydroxy (-OH) tự do?",
          "3 nhóm -OH",
          "2 nhóm -OH",
          "5 nhóm -OH",
          "Mỗi mắt xích glucose trong mạch cellulose còn giữ lại đúng 3 nhóm alcohol tự do: [C₆H₇O₂(OH)₃]_n."),
         ("Sản phẩm thu được khi cho cellulose phản ứng hoàn toàn với hỗn hợp acid nitric đặc và acid sulfuric đặc làm thuốc súng không khói là:",
          "Cellulose trinitrate",
          "Cellulose triacetate",
          "Trinitrotoluene (TNT)",
          "Ba nhóm -OH trong mỗi mắt xích bị ester hóa bởi HNO₃ tạo [C₆H₇O₂(ONO₂)₃]_n (cellulose trinitrate), một chất nổ mạnh không khói."),
         ("Động vật ăn cỏ như trâu bò tiêu hóa được cỏ (chứa nhiều cellulose) nhưng con người thì không thể tiêu hóa được vì hệ tiêu hóa của người thiếu:",
          "Enzyme cellulase để phân cắt liên kết β-1,4-glycosidic",
          "Acid clohydric HCl trong dịch vị dạ dày",
          "Enzyme amylase trong tuyến nước bọt",
          "Liên kết β-1,4-glycosidic rất bền và đòi hỏi enzyme chuyên biệt cellulase (do vi sinh vật cộng sinh trong dạ cỏ động vật nhai lại tiết ra).")
     ]),

    # Block 3: Hợp chất chứa Nitrogen và Polymer (Lessons 11-15)
    ("amines", "Cấu tạo, bậc amine và tính bazơ",
     "Amine là hợp chất hình thành khi thay thế nguyên tử H trong NH₃ bằng gốc hydrocarbon. Bậc của amine là số gốc hydrocarbon liên kết với nguyên tử N (bậc 1: RNH₂, bậc 2: R₁R₂NH, bậc 3: R₁R₂R₃N). Tính base: do đôi electron tự do trên nguyên tử N: CH₃NH₂ > NH₃ > C₆H₅NH₂ (aniline). Aniline tạo kết tủa trắng với dung dịch nước bromine.",
     "Methylamine (CH₃NH₂) là chất khí có mùi khai tương tự amoniac, làm quỳ tím ẩm hóa xanh.",
     [
         ("Hợp chất methylamine có công thức CH₃NH₂ thuộc loại amine bậc mấy?",
          "Amine bậc 1",
          "Amine bậc 2",
          "Amine bậc 3",
          "Bậc của amine tính bằng số nguyên tử H trong NH₃ được thay thế bằng gốc hydrocarbon. CH₃NH₂ có 1 gốc CH₃- nên là amine bậc 1."),
         ("Dãy sắp xếp các chất theo thứ tự lực base giảm dần từ trái sang phải là:",
          "CH₃NH₂ > NH₃ > C₆H₅NH₂ (aniline)",
          "C₆H₅NH₂ > NH₃ > CH₃NH₂",
          "NH₃ > CH₃NH₂ > C₆H₅NH₂",
          "Gốc alkyl -CH₃ đẩy electron làm tăng mật độ điện tích trên N (tính base mạnh hơn NH₃); vòng phenyl -C₆H₅ hút electron làm giảm mật độ trên N (tính base yếu hơn NH₃)."),
         ("Để khử mùi tanh của cá mè do các amine bay hơi (như trimethylamine) gây ra, người nội trợ thường dùng biện pháp dân gian nào?",
          "Rửa cá bằng giấm ăn hoặc nước cốt chanh",
          "Ngâm cá trong nước muối thật mặn",
          "Rửa cá bằng nước xà phòng kiềm",
          "Acid acetic trong giấm hoặc acid citric trong chanh phản ứng với các amine tạo thành muối tan không bay hơi, làm mất mùi tanh.")
     ]),

    ("amino-acids", "Cấu trúc và tính chất lưỡng tính của Amino acid",
     "Amino acid là hợp chất hữu cơ tạp chức chứa đồng thời nhóm amino (-NH₂) và carboxyl (-COOH). Tồn tại chủ yếu dưới dạng ion lưỡng cực: H₃N⁺-CH(R)-COO⁻ (dạng tinh thể, nhiệt độ nóng chảy cao, tan tốt trong nước). Tính chất lưỡng tính: phản ứng với cả acid mạnh và base mạnh.",
     "Glycine (H₂N-CH₂-COOH) phản ứng với HCl tạo Cl⁻H₃N⁺-CH₂-COOH và phản ứng với NaOH tạo H₂N-CH₂-COONa + H₂O.",
     [
         ("Amino acid đơn giản nhất có tên gọi là glycine có công thức cấu tạo là:",
          "H₂N-CH₂-COOH",
          "CH₃-CH(NH₂)-COOH",
          "H₂N-CH₂-CH₂-COOH",
          "Glycine (acid aminoacetic) có phân tử khối bằng 75, công thức H₂N-CH₂-COOH."),
         ("Ở trạng thái tinh thể và trong dung dịch nước, phân tử amino acid tồn tại chủ yếu ở dạng cấu trúc nào?",
          "Ion lưỡng cực (muối nội phân tử H₃N⁺-CH(R)-COO⁻)",
          "Dạng phân tử trung hòa H₂N-CH(R)-COOH",
          "Dạng cation mang điện tích dương hoàn toàn",
          "Nhóm -COOH có tính acid nhường H⁺ cho nhóm -NH₂ có tính base ngay trong phân tử, tạo dạng ion lưỡng cực bền vững."),
         ("Amino acid có tính chất lưỡng tính vì nguyên nhân cấu tạo nào sau đây?",
          "Phân tử chứa đồng thời nhóm carboxyl mang tính acid và nhóm amino mang tính base",
          "Phân tử có chứa vòng thơm benzene",
          "Phân tử có khả năng hòa tan vô hạn trong các dung môi hữu cơ",
          "Nhóm -NH₂ nhận proton (phản ứng với acid), nhóm -COOH nhường proton (phản ứng với base), tạo nên tính chất lưỡng tính đặc trưng.")
     ]),

    ("peptides-proteins", "Peptide và Protein",
     "Liên kết peptide là liên kết -CO-NH- giữa hai đơn vị α-amino acid. Chuỗi peptide từ 2 đến 50 đơn vị; protein là polypeptide cao phân tử từ trên 50 đơn vị. Phản ứng màu biuret: peptide chứa từ 2 liên kết peptide trở lên (tripeptide trở lên) tác dụng Cu(OH)₂ tạo phức chất màu tím. Protein bị đông tụ dưới tác dụng của nhiệt độ, acid, kiềm hoặc ion kim loại nặng.",
     "Nấu canh cua, thịt cua nổi lên từng mảng (riêu cua) là hiện tượng đông tụ protein dưới tác dụng của nhiệt độ.",
     [
         ("Liên kết hóa học giữa nhóm -CO- và nhóm -NH- của hai đơn vị α-amino acid được gọi là:",
          "Liên kết peptide",
          "Liên kết glycosidic",
          "Liên kết ester",
          "Định nghĩa liên kết peptide là liên kết amide tạo bởi nhóm carboxyl của α-amino acid này với nhóm amino của α-amino acid kia."),
         ("Hợp chất nào sau đây tham gia phản ứng màu biuret với Cu(OH)₂ trong môi trường kiềm tạo dung dịch màu tím đặc trưng?",
          "Tripeptide trở lên và protein",
          "Dipeptide",
          "Monosaccharide",
          "Điều kiện của phản ứng màu biuret là phân tử phải có từ 2 liên kết peptide trở lên, tức từ tripeptide trở lên."),
         ("Hiện tượng riêu cua nổi lên khi đun sôi nước canh cua đồng hoặc lòng trắng trứng đông cứng khi luộc là do:",
          "Sự đông tụ của protein dưới tác dụng của nhiệt độ cao",
          "Sự bay hơi của các phân tử nước trong dung dịch",
          "Phản ứng thủy phân hoàn toàn protein thành amino acid",
          "Nhiệt độ cao làm phá vỡ các liên kết hydro và tương tác bậc cao trong cấu trúc protein, làm mạch duỗi ra và kết tụ lại thành khối rắn.")
     ]),

    ("polymer-basics", "Đại cương về polymer",
     "Polymer là hợp chất có phân tử khối rất lớn gồm nhiều mắt xích monomer lặp lại. Phản ứng trùng hợp: kết hợp nhiều monomer có liên kết đôi C=C hoặc vòng kém bền thành polymer mà không giải phóng phân tử nhỏ. Phản ứng trùng ngưng: kết hợp nhiều monomer có ít nhất 2 nhóm chức hoạt động thành polymer đồng thời giải phóng các phân tử nhỏ (như H₂O).",
     "Trùng hợp ethylene tạo polyethylene (PE): n CH₂=CH₂ → (-CH₂-CH₂-)_n.",
     [
         ("Phản ứng kết hợp nhiều phân tử nhỏ (monomer) giống nhau hay tương tự nhau thành phân tử lớn (polymer) gọi là:",
          "Phản ứng trùng hợp",
          "Phản ứng xà phòng hóa",
          "Phản ứng este hóa",
          "Định nghĩa phản ứng trùng hợp là quá trình cộng hợp liên tiếp nhiều phân tử nhỏ thành phân tử có khối lượng phân tử rất lớn."),
         ("Polyethylene (PE) là vật liệu polymer thông dụng được tổng hợp bằng phản ứng trùng hợp monomer nào?",
          "Ethylene (CH₂=CH₂)",
          "Propylene (CH₂=CH-CH₃)",
          "Vinyl chloride (CH₂=CH-Cl)",
          "Polyethylene được tạo ra từ phản ứng trùng hợp khí ethylene dưới áp suất và xúc tác thích hợp: n CH₂=CH₂ → (-CH₂-CH₂-)_n."),
         ("Tiêu chí then chốt để phân biệt phản ứng trùng hợp và phản ứng trùng ngưng là:",
          "Trùng hợp không giải phóng phân tử nhỏ, còn trùng ngưng có giải phóng phân tử nhỏ (như H₂O)",
          "Trùng hợp chỉ diễn ra ở nhiệt độ phòng",
          "Trùng ngưng không tạo ra liên kết hóa học giữa các mắt xích",
          "Phản ứng trùng ngưng kèm theo sự tách ra các phân tử nhỏ (như nước, amoniac) từ các nhóm chức tương tác, còn trùng hợp là phản ứng cộng hợp hoàn toàn.")
     ]),

    ("polymer-materials", "Một số vật liệu polymer thông dụng",
     "Chất dẻo: PE, PVC (ống dẫn nước), PS, Teflon (chống dính chảo). Tơ tổng hợp: nylon-6,6 (trùng ngưng hexamethylenediamine và adipic acid, tơ polyamide), tơ nitron/olon (trùng hợp vinyl cyanide). Cao su: cao su buna, cao su lưu hóa (tạo cầu nối -S-S- tăng đàn hồi và chịu nhiệt). Keo dán: keo dán epoxy, keo dán urea-formaldehyde.",
     "Chảo chống dính được tráng một lớp mỏng polymer Teflon (-CF₂-CF₂-)_n có độ bền nhiệt và tính trơ hóa học rất cao.",
     [
         ("Vật liệu polymer nào sau đây được phủ lên bề mặt chảo chống dính nhờ độ bền nhiệt cao và tính trơ hóa học tuyệt vời?",
          "Teflon (polytetrafluoroethylene)",
          "Polyvinyl chloride (PVC)",
          "Polyethylene (PE)",
          "Teflon (-CF₂-CF₂-)_n có liên kết C-F rất bền vững, không phản ứng với dầu mỡ ở nhiệt độ nấu nướng và có hệ số ma sát cực thấp."),
         ("Tơ nylon-6,6 là một loại tơ tổng hợp thuộc phân loại tơ nào sau đây?",
          "Tơ polyamide",
          "Tơ polyester",
          "Tơ bán tổng hợp (tơ nhân tạo)",
          "Nylon-6,6 được điều chế từ hexamethylenediamine và adipic acid, chứa các liên kết amide -CO-NH- lặp lại nên là tơ polyamide."),
         ("Mục đích chính của quá trình lưu hóa cao su thiên nhiên bằng lưu huỳnh trong công nghiệp sản xuất lốp xe là:",
          "Tạo các cầu nối disulfide (-S-S-) nối các mạch polymer thành mạng không gian giúp tăng tính đàn hồi và chịu mài mòn",
          "Làm cao su tan hoàn toàn trong nước để dễ đúc khuôn",
          "Biến cao su thành chất dẻo nhiệt mềm ở nhiệt độ cao",
          "Cầu nối -S-S- liên kết các mạch phân tử cao su riêng lẻ thành mạng lưới không gian ba chiều, hạn chế biến dạng vĩnh viễn và tăng độ bền cơ nhiệt.")
     ]),

    # Block 4: Pin điện và Đại cương kim loại (Lessons 16-20)
    ("electrode-potentials", "Thế điện cực chuẩn và dãy thế điện cực chuẩn",
     "Thế điện cực chuẩn E°(Ox/Red) đo xu hướng oxi hóa - khử của cặp ở điều kiện chuẩn so với điện cực chuẩn hydrogen (quy ước E°(2H⁺/H₂) = 0,00 V). E° càng lớn: dạng oxi hóa càng mạnh, dạng khử càng yếu. Quy tắc alpha: chất oxi hóa mạnh hơn phản ứng với chất khử mạnh hơn tạo chất oxi hóa yếu hơn và chất khử yếu hơn.",
     "E°(Zn²⁺/Zn) = -0,76 V và E°(Cu²⁺/Cu) = +0,34 V. Ion Cu²⁺ có tính oxi hóa mạnh hơn Zn²⁺; kim loại Zn khử được ion Cu²⁺: Zn + Cu²⁺ → Zn²⁺ + Cu.",
     [
         ("Trong thang đo thế điện cực chuẩn, điện cực chuẩn hydrogen được quy ước có thế điện cực ở 25 °C bằng:",
          "0,00 V",
          "1,00 V",
          "-1,00 V",
          "Điện cực hydro chuẩn (SHE) với H⁺ (1 M) và khí H₂ (1 bar) trên bề mặt platin được chọn làm mốc chuẩn có E° = 0,00 V."),
         ("Cho thế điện cực chuẩn: E°(Zn²⁺/Zn) = -0,76 V và E°(Cu²⁺/Cu) = +0,34 V. So sánh tính oxi hóa giữa hai ion kim loại:",
          "Ion Cu²⁺ có tính oxi hóa mạnh hơn ion Zn²⁺",
          "Ion Zn²⁺ có tính oxi hóa mạnh hơn ion Cu²⁺",
          "Hai ion có tính oxi hóa tương đương nhau",
          "Cặp có thế điện cực chuẩn lớn hơn (+0,34 V > -0,76 V) thì dạng oxi hóa (Cu²⁺) có tính oxi hóa mạnh hơn."),
         ("Nhúng một chiếc đinh sắt sạch vào dung dịch đồng(II) sulfate (CuSO₄), hiện tượng hóa học quan sát được là:",
          "Có lớp kim loại màu đỏ bám trên đinh sắt và màu xanh của dung dịch nhạt dần",
          "Có khí không màu mùi hắc thoát ra mãnh liệt",
          "Không có bất kì hiện tượng nào xảy ra",
          "Sắt có tính khử mạnh hơn đồng (E°(Fe²⁺/Fe) = -0,44 V < +0,34 V) nên khử Cu²⁺ thành Cu kim loại màu đỏ bám lên đinh sắt: Fe + Cu²⁺ → Fe²⁺ + Cu.")
     ]),

    ("galvanic-cells", "Pin điện hóa (Galvanic cell)",
     "Pin điện hóa biến đổi hóa năng thành điện năng nhờ phản ứng oxi hóa - khử tự phát. Anode (cực âm): xảy ra quá trình oxi hóa; Cathode (cực dương): xảy ra quá trình khử. Sức điện động chuẩn: E°_pin = E°_cathode - E°_anode > 0. Cầu muối (chứa dung dịch KCl hoặc KNO₃) đóng kín mạch và trung hòa điện tích hai nửa pin.",
     "Pin Daniell Zn-Cu: Zn | Zn²⁺(1M) || Cu²⁺(1M) | Cu. E°_pin = +0,34 - (-0,76) = 1,10 V.",
     [
         ("Trong pin điện hóa Zn - Cu (pin Daniell) khi hoạt động, tại cực âm (anode) xảy ra quá trình nào?",
          "Quá trình oxi hóa kẽm (Zn → Zn²⁺ + 2e)",
          "Quá trình khử ion kẽm (Zn²⁺ + 2e → Zn)",
          "Quá trình oxi hóa ion đồng (Cu → Cu²⁺ + 2e)",
          "Tại cực âm (anode), kẽm có tính khử mạnh hơn bị oxi hóa nhường electron đi vào mạch ngoài."),
         ("Cho E°(Zn²⁺/Zn) = -0,76 V và E°(Cu²⁺/Cu) = +0,34 V. Sức điện động chuẩn của pin điện hóa Zn - Cu bằng:",
          "1,10 V",
          "0,42 V",
          "-0,42 V",
          "E°_pin = E°_cathode - E°_anode = E°(Cu²⁺/Cu) - E°(Zn²⁺/Zn) = +0,34 - (-0,76) = +1,10 V."),
         ("Vai trò chủ yếu của cầu muối (salt bridge) trong một pin điện hóa hoạt động là:",
          "Khép kín mạch điện và duy trì tính trung hòa điện tích cho hai dung dịch điện cực",
          "Trực tiếp tạo ra các electron dẫn điện cho mạch ngoài",
          "Tăng nhiệt độ của pin điện hóa lên mức tối đa",
          "Cầu muối cho phép các ion di chuyển qua lại để bù đắp sự chênh lệch điện tích giữa hai bán pin, ngăn cản sự phân cực và khép kín mạch.")
     ]),

    ("electrolysis", "Điện phân dung dịch và điện phân nóng chảy",
     "Điện phân biến đổi điện năng thành hóa năng nhờ dòng điện một chiều cưỡng bức phản ứng oxi hóa - khử. Cathode (cực âm nối cực âm nguồn): xảy ra quá trình khử; Anode (cực dương nối cực dương nguồn): xảy ra quá trình oxi hóa. Các kim loại hoạt động mạnh (từ Al trở về trước) chỉ có thể điều chế bằng điện phân nóng chảy hợp chất (như nóng chảy Al₂O₃ có cryolite Na₃AlF₆).",
     "Điện phân dung dịch CuCl₂ với điện cực trơ: CuCl₂ → Cu (ở cathode) + Cl₂ (ở anode).",
     [
         ("Kim loại nhôm (Al) trong công nghiệp được sản xuất với quy mô lớn bằng phương pháp nào sau đây?",
          "Điện phân nóng chảy Al₂O₃ với sự có mặt của phụ gia cryolite (Na₃AlF₆)",
          "Nhiệt luyện dùng CO khử Al₂O₃ ở nhiệt độ cao",
          "Thủy luyện dùng sắt đẩy nhôm ra khỏi dung dịch muối AlCl₃",
          "Nhôm là kim loại có tính khử mạnh, ion Al³⁺ rất khó bị khử nên bắt buộc phải dùng phương pháp điện phân nóng chảy."),
         ("Trong quá trình điện phân dung dịch CuCl₂ bằng dòng điện một chiều với hai điện cực trơ, tại cathode (cực âm) xảy ra quá trình nào?",
          "Khử ion Cu²⁺ thành Cu kim loại (Cu²⁺ + 2e → Cu)",
          "Oxi hóa ion Cl⁻ thành khí Cl₂",
          "Oxi hóa nước giải phóng khí oxy",
          "Tại cathode (cực âm), cation Cu²⁺ bị hút về và nhận electron thực hiện quá trình khử tạo đồng kim loại bám lên điện cực."),
         ("Vai trò quan trọng của quặng cryolite (Na₃AlF₆) khi pha trộn với Al₂O₃ trong quá trình điện phân nóng chảy nhôm là:",
          "Hạ nhiệt độ nóng chảy của Al₂O₃ từ trên 2000 °C xuống khoảng 950 °C và tăng tính dẫn điện",
          "Làm tăng khối lượng nhôm thu được lên gấp đôi",
          "Ngăn không cho khí oxi phản ứng với nhôm lỏng",
          "Cryolite tạo hỗn hợp eutectic nóng chảy ở nhiệt độ thấp hơn nhiều (~950 °C so với 2050 °C), giúp tiết kiệm năng lượng điện khổng lồ.")
     ]),

    ("metal-corrosion", "Ăn mòn kim loại và các phương pháp bảo vệ",
     "Ăn mòn kim loại là sự phá hủy kim loại hoặc hợp kim do tác dụng của môi trường. Ăn mòn hóa học: không phát sinh dòng điện. Ăn mòn điện hóa: có 2 điện cực khác bản chất, tiếp xúc và cùng nhúng trong dung dịch điện li; cực âm (kim loại mạnh hơn) bị ăn mòn trước. Bảo vệ: phủ bề mặt (sơn, mạ) hoặc phương pháp điện hóa (dùng kim loại hi sinh như gắn tấm kẽm bảo vệ vỏ tàu thép).",
     "Vỏ tàu biển bằng thép ngâm trong nước biển (dung dịch điện li) được bảo vệ bằng cách gắn các khối kẽm (Zn); Zn bị ăn mòn thay thế cho Fe.",
     [
         ("Điều kiện để xảy ra hiện tượng ăn mòn điện hóa học kim loại bao gồm các yếu tố nào?",
          "Hai điện cực khác bản chất, tiếp xúc trực tiếp hoặc gián tiếp, và cùng tiếp xúc với dung dịch điện li",
          "Chỉ cần kim loại được đặt trong môi trường chân không tuyệt đối",
          "Chỉ xảy ra khi nối kim loại với một nguồn điện một chiều bên ngoài",
          "Ăn mòn điện hóa đòi hỏi 3 điều kiện: hai điện cực khác nhau, có tiếp xúc điện và cùng nhúng trong môi trường dẫn điện điện li."),
         ("Để bảo vệ vỏ tàu thủy bằng thép (chứa Fe) khỏi bị ăn mòn khi hoạt động trên biển, người ta thường gắn vào vỏ tàu các tấm kim loại nào?",
          "Tấm kẽm (Zn)",
          "Tấm đồng (Cu)",
          "Tấm bạc (Ag)",
          "Kẽm có tính khử mạnh hơn sắt (E°(Zn²⁺/Zn) = -0,76 V < -0,44 V) nên đóng vai trò là anode hi sinh, bị ăn mòn thay cho vỏ tàu thép."),
         ("Nối một đoạn dây sắt với một đoạn dây đồng rồi cùng nhúng vào cốc nước muối NaCl. Kim loại nào sẽ bị ăn mòn điện hóa trước?",
          "Dây sắt bị ăn mòn trước",
          "Dây đồng bị ăn mòn trước",
          "Cả hai kim loại bị ăn mòn với tốc độ hoàn toàn như nhau",
          "Sắt có tính khử mạnh hơn đồng nên đóng vai trò là cực âm (anode) và bị oxi hóa hòa tan trước, đồng là cathode chỉ truyền electron.")
     ]),

    ("metal-properties-extraction", "Tính chất chung và phương pháp điều chế kim loại",
     "Kim loại có liên kết kim loại (mạng tinh thể cation và các electron tự do). Tính chất vật lí chung do electron tự do quyết định: tính dẻo, tính dẫn điện, tính dẫn nhiệt và ánh kim. Dẫn điện tốt nhất: Ag > Cu > Au > Al > Fe. Phương pháp điều chế: Nhiệt luyện (khử oxit kim loại đứng sau Al bằng CO, H₂, C ở t° cao); Thủy luyện (dùng kim loại mạnh đẩy ion kim loại yếu ra khỏi dung dịch); Điện phân.",
     "Trong công nghiệp luyện kim, sắt (Fe) được sản xuất trong lò cao bằng phương pháp nhiệt luyện: dùng khí CO khử Fe₂O₃ ở nhiệt độ cao.",
     [
         ("Các tính chất vật lí chung của kim loại như tính dẫn điện, dẫn nhiệt, tính dẻo và ánh kim do thành phần nào trong tinh thể quyết định?",
          "Các electron tự do di chuyển trong toàn bộ mạng tinh thể kim loại",
          "Các liên kết cộng hóa trị phân cực giữa các nguyên tử",
          "Kích thước hạt nhân nguyên tử kim loại",
          "Mô hình biển electron tự do giải thích trọn vẹn 4 tính chất vật lí chung đặc trưng của giới kim loại."),
         ("Phương pháp nhiệt luyện dùng khí CO hoặc H₂ để khử oxit kim loại ở nhiệt độ cao thường được áp dụng để điều chế các kim loại nào?",
          "Các kim loại đứng sau nhôm trong dãy hoạt động hóa học (như Fe, Cu, Zn)",
          "Các kim loại kiềm và kiềm thổ (như Na, K, Ca)",
          "Chỉ dùng riêng cho kim loại nhôm (Al)",
          "CO và H₂ chỉ có thể khử được các ion kim loại có tính oxi hóa từ kẽm (Zn) trở đi (sau Al) ở nhiệt độ cao."),
         ("Kim loại có độ dẫn điện tốt nhất trong tất cả các kim loại ở điều kiện nhiệt độ phòng là:",
          "Bạc (Ag)",
          "Đồng (Cu)",
          "Vàng (Au)",
          "Thứ tự độ dẫn điện giảm dần của các kim loại phổ biến là: Ag > Cu > Au > Al > Fe; bạc dẫn điện tốt nhất.")
     ])
]


def build_course(subject, grade, title, units):
    lessons = []
    for number, (slug, name, guidance, example, questions) in enumerate(units):
        lid = f"{subject}-{grade}-{slug}"
        values = []
        for qn, (prompt, correct, wrong1, wrong2, explanation) in enumerate(questions):
            choices = [correct, wrong1, wrong2]
            shift = (number + qn) % 3
            choices = choices[shift:] + choices[:shift]
            options = [{"id": chr(97 + n), "text": text} for n, text in enumerate(choices)]
            values.append({
                "id": f"{lid}-q{qn + 1}",
                "prompt": prompt,
                "difficulty": ("EASY", "MEDIUM", "APPLIED")[qn],
                "options": options,
                "correctOptionId": next(o["id"] for o in options if o["text"] == correct),
                "explanation": explanation
            })
        lessons.append({
            "id": lid,
            "title": name,
            "guidance": [guidance],
            "examples": [example],
            "questions": values
        })
    return {
        "subjectId": subject,
        "grade": grade,
        "version": "intro-2026.1",
        "title": title,
        "published": True,
        "coverage": f"PARTIAL_GRADE_{grade}",
        "extendedReviews": False,
        "sourceNote": SOURCE_NOTE,
        "lessons": lessons
    }


def main():
    target_dir = BACKEND / "curriculum"
    target_dir.mkdir(exist_ok=True)

    packs = [
        ("math", 12, "Toán 12: Khảo sát hàm số, Hình học Oxyz và Thống kê - Xác suất", MATH_LESSONS),
        ("physics", 12, "Vật lí 12: Vật lí nhiệt, Khí lí tưởng, Từ trường và Hạt nhân", PHYSICS_LESSONS),
        ("chemistry", 12, "Hóa học 12: Hợp chất hữu cơ, Đại cương polymer và Điện hóa học", CHEMISTRY_LESSONS),
        ("biology", 12, "Sinh học 12: Di truyền học, Tiến hóa và Sinh thái học", BIOLOGY_LESSONS),
        ("literature", 12, "Ngữ văn 12: Đọc hiểu văn bản, Nghị luận và Kĩ năng giao tiếp", LITERATURE_LESSONS),
        ("english", 12, "Tiếng Anh 12: Global Issues, Future Careers and Academic Communication", ENGLISH_LESSONS),
        ("history", 12, "Lịch sử 12: Thế giới và Việt Nam từ thế kỉ XX đến nay", HISTORY_LESSONS),
        ("geography", 12, "Địa lí 12: Địa lí tự nhiên, dân cư và các vùng kinh tế Việt Nam", GEOGRAPHY_LESSONS),
        ("economics_law", 12, "Giáo dục kinh tế và pháp luật 12: Hội nhập kinh tế, chính sách an sinh và pháp luật quốc tế", ECONOMICS_LAW_LESSONS),
    ]

    for subject, grade, title, lessons in packs:
        course = build_course(subject, grade, title, lessons)
        Catalog.validate(course)
        out_path = target_dir / f"{subject}-{grade}.json"
        out_path.write_text(json.dumps(course, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"Generated and validated {out_path} ({len(course['lessons'])} lessons, {sum(len(l['questions']) for l in course['lessons'])} questions)")

    # Validate Catalog can load everything
    catalog = Catalog(target_dir)
    for subject, grade, _, lessons in packs:
        assert (subject, grade) in catalog.courses
        assert len(catalog.courses[(subject, grade)]["lessons"]) == len(lessons)
    print("Catalog successfully validated all Grade 12 packs alongside existing packs.")


if __name__ == "__main__":
    main()
