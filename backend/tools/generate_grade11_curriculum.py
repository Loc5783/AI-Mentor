import json
import os
import re

# Đề cương các môn còn lại sẽ được tự động parse thành bài học
OUTLINES = {
    "physics": "Dao động điều hòa | Các đại lượng li độ - vận tốc - gia tốc | Năng lượng trong dao động | Dao động tắt dần và cưỡng bức | Hiện tượng cộng hưởng | Sóng cơ và sự truyền sóng | Các đặc trưng của sóng | Giao thoa sóng cơ | Sóng dừng | Sóng điện từ | Điện trường và cường độ điện trường | Thế năng và điện thế | Tụ điện và điện dung | Cường độ dòng điện | Năng lượng điện",
    "chemistry": "Cân bằng hóa học | Hằng số cân bằng Kc | Sự điện li | Thuyết Brønsted-Lowry | pH và chất chỉ thị | Nitrogen | Ammonia | Nitric acid | Sulfur | Sulfuric acid | Đại cương hóa hữu cơ | Phổ hồng ngoại IR | Đồng đẳng và đồng phân | Alkane | Alkene và Alkyne",
    "biology": "Chuyển hóa năng lượng | Trao đổi nước ở thực vật | Thoát hơi nước | Quang hợp | Hô hấp thực vật | Tiêu hóa ở động vật | Hô hấp ở động vật | Tuần hoàn | Miễn dịch | Cân bằng nội môi | Cảm ứng ở thực vật | Điện thế nghỉ và hoạt động | Tập tính động vật | Sinh trưởng | Sinh sản",
    "literature": "Câu chuyện và điểm nhìn | Nhân vật truyện ngắn | Lặp cấu trúc | Cảm xúc trong thơ | Ngôn ngữ văn học | Tùy bút và tản văn | Luận điểm và bằng chứng | Chêm xen và nói giảm | Kịch bản văn học | Ngôn ngữ hành động | Văn bản thông tin | Thuyết minh hiện tượng | Viết bài so sánh | Kĩ năng phản biện | Đọc hiểu văn học trung đại",
    "english": "Healthy habits | Generation gap | Cities of the future | ASEAN community | Global warming | Preserving heritage | Higher education options | Becoming independent | Green lifestyle | Electronic devices | The world of work | Lifelong learning | Cultural diversity | Environmental protection | AI in education",
    "history": "Cách mạng tư sản Anh Pháp | Sự xác lập TBCN | CNTB hiện đại | Liên bang Xô viết | Ý nghĩa Cách mạng tháng Mười | Thực dân xâm lược ĐNA | Giải phóng dân tộc ĐNA | CTTG thứ nhất | CTTG thứ hai | Trật tự hai cực I-an-ta | Văn minh ĐNA cổ trung đại | Văn minh Văn Lang | Văn minh Champa Phù Nam | Văn minh Đại Việt | Bảo tồn di sản",
    "geography": "Phát triển kinh tế giữa các nước | Toàn cầu hóa | Tổ chức liên kết | An ninh lương thực | An ninh năng lượng | Tự nhiên Mỹ Latinh | Xã hội Mỹ Latinh | Kinh tế Mỹ Latinh | Liên minh châu Âu EU | Thị trường chung Euro | Tự nhiên Đông Nam Á | Dân cư Đông Nam Á | Kinh tế Đông Nam Á | ASEAN | Vị thế Hoa Kỳ Nhật Bản Trung Quốc",
    "economics_law": "Cạnh tranh kinh tế | Cung cầu | Lạm phát | Kiểm soát lạm phát | Thất nghiệp | Thị trường lao động | Ý tưởng kinh doanh | Năng lực quản trị | Đạo đức kinh doanh | Tiêu dùng thông minh | Quyền bình đẳng pháp luật | Bình đẳng giới | Bình đẳng dân tộc tôn giáo | Quyền bầu cử | Nghĩa vụ bảo vệ Tổ quốc"
}

def make_slug(text):
    text = text.lower()
    text = re.sub(r'[àáạảãâầấậẩẫăằắặẳẵ]', 'a', text)
    text = re.sub(r'[èéẹẻẽêềếệểễ]', 'e', text)
    text = re.sub(r'[ìíịỉĩ]', 'i', text)
    text = re.sub(r'[òóọỏõôồốộổỗơờớợởỡ]', 'o', text)
    text = re.sub(r'[ùúụủũưừứựửữ]', 'u', text)
    text = re.sub(r'[ỳýỵỷỹ]', 'y', text)
    text = re.sub(r'đ', 'd', text)
    text = re.sub(r'[^a-z0-9]+', '-', text)
    return text.strip('-')

def generate_math_11():
    return {
        "subjectId": "math",
        "grade": 11,
        "version": "intro-2026.1",
        "title": "Toán 11: Lượng giác, dãy số, giới hạn và hình học không gian",
        "published": True,
        "coverage": "PARTIAL_GRADE_11",
        "extendedReviews": False,
        "sourceNote": "Biên soạn theo khung GDPT 2018. Toán 11 tập trung vào các khái niệm giải tích ban đầu và hình học không gian kinh điển.",
        "lessons": [
            {
                "id": "math-11-ham-so-luong-giac",
                "title": "Hàm số lượng giác và đồ thị",
                "guidance": ["Hàm y=sinx, y=cosx có tập xác định R, tập giá trị [-1; 1]. Hàm sinx là hàm lẻ, cosx là hàm chẵn. Chu kì tuần hoàn của sinx và cosx là 2π."],
                "examples": ["GTLN của y = 2sinx + 1 là 3, đạt được khi sinx = 1."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Tập giá trị của hàm số y = cos x là?", "options": [{"id": "a", "text": "[-1; 1]"}, {"id": "b", "text": "(0; 1)"}, {"id": "c", "text": "R"}], "correctOptionId": "a", "explanation": "Hàm cosx luôn nhận giá trị từ -1 đến 1."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Chu kì tuần hoàn của hàm số y = tan x là?", "options": [{"id": "a", "text": "2π"}, {"id": "b", "text": "π"}, {"id": "c", "text": "π/2"}], "correctOptionId": "b", "explanation": "Hàm số y = tan x tuần hoàn với chu kì cơ sở T = π."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Tìm GTLN của y = 3 - 2cos(x).", "options": [{"id": "a", "text": "1"}, {"id": "b", "text": "5"}, {"id": "c", "text": "3"}], "correctOptionId": "b", "explanation": "Ta có -1 ≤ cosx ≤ 1, do đó GTLN của -2cosx là 2. Suy ra y ≤ 3 + 2 = 5."}
                ]
            },
            {
                "id": "math-11-phuong-trinh-luong-giac",
                "title": "Phương trình lượng giác cơ bản",
                "guidance": ["Phương trình sinx = sina có nghiệm x = a + k2π hoặc x = π - a + k2π (k thuộc Z).", "cosx = cosa có nghiệm x = ±a + k2π."],
                "examples": ["Giải PT sinx = 1/2. Vì sin(π/6) = 1/2 nên x = π/6 + k2π hoặc x = 5π/6 + k2π."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Nghiệm của phương trình cosx = 1 là?", "options": [{"id": "a", "text": "x = kπ"}, {"id": "b", "text": "x = k2π"}, {"id": "c", "text": "x = π/2 + kπ"}], "correctOptionId": "b", "explanation": "cos x = 1 tại các điểm x = k2π trên đường tròn lượng giác."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Nghiệm của phương trình sinx = 0 là?", "options": [{"id": "a", "text": "x = kπ"}, {"id": "b", "text": "x = k2π"}, {"id": "c", "text": "Vô nghiệm"}], "correctOptionId": "a", "explanation": "sin x = 0 tại hai điểm 0 và π trên vòng tròn, tương đương x = kπ."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Số nghiệm của PT sinx = 1/2 trên đoạn [0; 2π] là?", "options": [{"id": "a", "text": "1"}, {"id": "b", "text": "2"}, {"id": "c", "text": "4"}], "correctOptionId": "b", "explanation": "Trên [0; 2π], sinx = 1/2 có 2 nghiệm là π/6 và 5π/6."}
                ]
            },
            {
                "id": "math-11-day-so",
                "title": "Dãy số",
                "guidance": ["Dãy số có thể cho bằng công thức tổng quát un = f(n) hoặc bằng hệ thức truy hồi.", "Dãy số tăng nếu un+1 > un, giảm nếu un+1 < un với mọi n."],
                "examples": ["Xét dãy un = 2n + 1. Ta có un+1 - un = 2(n+1)+1 - (2n+1) = 2 > 0, dãy số tăng."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Cho dãy số un = 3n - 1. Số hạng u2 là?", "options": [{"id": "a", "text": "2"}, {"id": "b", "text": "5"}, {"id": "c", "text": "8"}], "correctOptionId": "b", "explanation": "Thay n = 2 vào công thức: u2 = 3(2) - 1 = 5."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Dãy số un = 1/n là dãy số?", "options": [{"id": "a", "text": "Tăng"}, {"id": "b", "text": "Giảm"}, {"id": "c", "text": "Không đổi"}], "correctOptionId": "b", "explanation": "un+1 - un = 1/(n+1) - 1/n < 0 với mọi n nguyên dương, nên dãy số giảm."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Dãy số truy hồi u1 = 2, un+1 = 3un. Tìm u3?", "options": [{"id": "a", "text": "18"}, {"id": "b", "text": "6"}, {"id": "c", "text": "12"}], "correctOptionId": "a", "explanation": "u2 = 3(u1) = 6. u3 = 3(u2) = 18."}
                ]
            },
            {
                "id": "math-11-cap-so-cong",
                "title": "Cấp số cộng",
                "guidance": ["Cấp số cộng (CSC) là dãy số thỏa mãn un+1 = un + d (d là công sai).", "Số hạng tổng quát: un = u1 + (n-1)d.", "Tổng n số hạng đầu: Sn = n(u1 + un)/2."],
                "examples": ["CSC có u1=2, d=3 thì u4 = 2 + (4-1)3 = 11."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Cho CSC có u1 = 1, d = 2. Tìm u3?", "options": [{"id": "a", "text": "3"}, {"id": "b", "text": "5"}, {"id": "c", "text": "7"}], "correctOptionId": "b", "explanation": "u3 = u1 + 2d = 1 + 2(2) = 5."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "CSC có u2 = 5, u4 = 11. Tìm d?", "options": [{"id": "a", "text": "2"}, {"id": "b", "text": "3"}, {"id": "c", "text": "4"}], "correctOptionId": "b", "explanation": "Ta có u4 - u2 = 2d <=> 11 - 5 = 2d => d = 3."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Tính tổng 10 số hạng đầu của CSC có u1 = 2, d = 3.", "options": [{"id": "a", "text": "155"}, {"id": "b", "text": "165"}, {"id": "c", "text": "290"}], "correctOptionId": "a", "explanation": "S10 = 10[2u1 + 9d]/2 = 5[4 + 27] = 5*31 = 155."}
                ]
            },
            {
                "id": "math-11-cap-so-nhan",
                "title": "Cấp số nhân",
                "guidance": ["Cấp số nhân (CSN) là dãy thỏa mãn un+1 = un * q (q là công bội).", "Số hạng tổng quát: un = u1 * q^(n-1)."],
                "examples": ["CSN có u1=2, q=3 thì u3 = 2 * 3^2 = 18."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Cho CSN có u1 = 3, q = 2. Tìm u2?", "options": [{"id": "a", "text": "5"}, {"id": "b", "text": "6"}, {"id": "c", "text": "9"}], "correctOptionId": "b", "explanation": "u2 = u1 * q = 3 * 2 = 6."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "CSN có u1 = -1, q = -2. Số hạng u4 là?", "options": [{"id": "a", "text": "8"}, {"id": "b", "text": "-8"}, {"id": "c", "text": "16"}], "correctOptionId": "a", "explanation": "u4 = u1 * q^3 = (-1) * (-2)^3 = (-1) * (-8) = 8."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Tính tổng 5 số hạng đầu của CSN có u1 = 1, q = 2.", "options": [{"id": "a", "text": "15"}, {"id": "b", "text": "31"}, {"id": "c", "text": "63"}], "correctOptionId": "b", "explanation": "S5 = u1(1-q^5)/(1-q) = 1(1-32)/(1-2) = 31."}
                ]
            },
            {
                "id": "math-11-gioi-han-day-so",
                "title": "Giới hạn của dãy số",
                "guidance": ["Giới hạn của dãy số đánh giá xu hướng của un khi n tiến tới vô cực.", "lim(1/n) = 0. lim(q^n) = 0 nếu |q| < 1."],
                "examples": ["lim (2n+1)/(n+2) = lim (2 + 1/n)/(1 + 2/n) = 2/1 = 2."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Giá trị của lim (1/n) bằng?", "options": [{"id": "a", "text": "0"}, {"id": "b", "text": "1"}, {"id": "c", "text": "+∞"}], "correctOptionId": "a", "explanation": "Khi n tiến tới vô cực, 1 chia cho số vô cùng lớn sẽ tiến tới 0."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Tính lim (3n^2 - 1)/(n^2 + 2).", "options": [{"id": "a", "text": "0"}, {"id": "b", "text": "3"}, {"id": "c", "text": "1"}], "correctOptionId": "b", "explanation": "Chia cả tử và mẫu cho n^2, ta được lim (3 - 1/n^2)/(1 + 2/n^2) = 3."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Tính tổng CSN lùi vô hạn: 1 + 1/2 + 1/4 + ...", "options": [{"id": "a", "text": "2"}, {"id": "b", "text": "1.5"}, {"id": "c", "text": "Vô cực"}], "correctOptionId": "a", "explanation": "S = u1/(1-q) = 1 / (1 - 1/2) = 2."}
                ]
            },
            {
                "id": "math-11-gioi-han-ham-so",
                "title": "Giới hạn của hàm số và hàm liên tục",
                "guidance": ["Giới hạn hàm số xét sự hội tụ của f(x) khi x dần tới x0.", "Hàm f(x) liên tục tại x0 nếu lim(x->x0) f(x) = f(x0)."],
                "examples": ["lim(x->2) (x^2 - 4)/(x - 2) = lim (x+2) = 4."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Tính lim(x->1) (2x + 3).", "options": [{"id": "a", "text": "5"}, {"id": "b", "text": "2"}, {"id": "c", "text": "0"}], "correctOptionId": "a", "explanation": "Thay trực tiếp x = 1 vào đa thức, kết quả là 5."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Tính lim(x->2) (x^2 - 4)/(x - 2).", "options": [{"id": "a", "text": "0"}, {"id": "b", "text": "4"}, {"id": "c", "text": "Không tồn tại"}], "correctOptionId": "b", "explanation": "Phân tích tử số: (x-2)(x+2). Rút gọn (x-2) còn x+2. Khi x->2 thì giới hạn bằng 4."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Hàm số f(x) = (x^2-1)/(x-1) với x≠1 và f(1)=m. Tìm m để hàm liên tục tại x=1.", "options": [{"id": "a", "text": "1"}, {"id": "b", "text": "2"}, {"id": "c", "text": "0"}], "correctOptionId": "b", "explanation": "lim(x->1) f(x) = lim (x+1) = 2. Để liên tục thì f(1) = m = 2."}
                ]
            },
            {
                "id": "math-11-duong-thang-mat-phang",
                "title": "Đường thẳng và mặt phẳng trong không gian",
                "guidance": ["Hình học không gian nghiên cứu các đối tượng 3 chiều.", "Ba điểm không thẳng hàng xác định duy nhất một mặt phẳng."],
                "examples": ["Giao tuyến của 2 mặt phẳng phân biệt (có điểm chung) luôn là một đường thẳng."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Có bao nhiêu mặt phẳng đi qua 3 điểm không thẳng hàng?", "options": [{"id": "a", "text": "1"}, {"id": "b", "text": "2"}, {"id": "c", "text": "Vô số"}], "correctOptionId": "a", "explanation": "Ba điểm không thẳng hàng là điều kiện xác định duy nhất 1 mặt phẳng."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Hai mặt phẳng phân biệt có điểm chung thì giao nhau theo một?", "options": [{"id": "a", "text": "Điểm"}, {"id": "b", "text": "Đường thẳng"}, {"id": "c", "text": "Mặt phẳng"}], "correctOptionId": "b", "explanation": "Giao tuyến của hai mặt phẳng phân biệt (nếu có) luôn là một đường thẳng."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Tứ diện ABCD có bao nhiêu mặt?", "options": [{"id": "a", "text": "3"}, {"id": "b", "text": "4"}, {"id": "c", "text": "6"}], "correctOptionId": "b", "explanation": "Tứ diện (hình chóp tam giác) có 4 mặt đều là các tam giác."}
                ]
            },
            {
                "id": "math-11-hai-duong-thang",
                "title": "Hai đường thẳng song song và chéo nhau",
                "guidance": ["Trong không gian, hai đường thẳng có 4 vị trí: cắt nhau, song song, trùng nhau, và chéo nhau.", "Chéo nhau là không cùng nằm trên bất kỳ mặt phẳng nào."],
                "examples": ["Hai cạnh đối diện của một tứ diện luôn là hai đường thẳng chéo nhau."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Hai đường thẳng không có điểm chung và cùng nằm trong một mặt phẳng thì?", "options": [{"id": "a", "text": "Chéo nhau"}, {"id": "b", "text": "Song song"}, {"id": "c", "text": "Cắt nhau"}], "correctOptionId": "b", "explanation": "Đây là định nghĩa chuẩn của hai đường thẳng song song."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Hai đường thẳng chéo nhau khi chúng?", "options": [{"id": "a", "text": "Không có điểm chung"}, {"id": "b", "text": "Không đồng phẳng"}, {"id": "c", "text": "Cắt nhau"}], "correctOptionId": "b", "explanation": "Không đồng phẳng (không cùng nằm trên một mặt phẳng) là tính chất cốt lõi của hai đường chéo nhau."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Trong tứ diện ABCD, hai cạnh AB và CD ở vị trí?", "options": [{"id": "a", "text": "Song song"}, {"id": "b", "text": "Cắt nhau"}, {"id": "c", "text": "Chéo nhau"}], "correctOptionId": "c", "explanation": "AB và CD không cùng nằm trong bất kỳ mặt phẳng nào của tứ diện, nên chúng chéo nhau."}
                ]
            },
            {
                "id": "math-11-duong-thang-mat-phang-song-song",
                "title": "Đường thẳng và mặt phẳng song song",
                "guidance": ["Đường thẳng d song song với (P) nếu d không nằm trong (P) và d song song với một đường d' nằm trong (P)."],
                "examples": ["Nếu d // (P), mọi mặt phẳng chứa d mà cắt (P) thì giao tuyến sẽ song song với d."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Đường thẳng d song song với (P) thì d và (P) có bao nhiêu điểm chung?", "options": [{"id": "a", "text": "0"}, {"id": "b", "text": "1"}, {"id": "c", "text": "Vô số"}], "correctOptionId": "a", "explanation": "Song song nghĩa là không có bất kỳ điểm chung nào."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Nếu d // d' và d' nằm trong (P) thì d song song hoặc ... với (P).", "options": [{"id": "a", "text": "Cắt"}, {"id": "b", "text": "Trùng"}, {"id": "c", "text": "Nằm trong"}], "correctOptionId": "c", "explanation": "Nếu d không nằm trong (P) thì d // (P). Nếu d đã thuộc (P) thì d nằm trong (P)."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Cho hình chóp S.ABCD đáy hình bình hành. AB song song với mặt phẳng nào?", "options": [{"id": "a", "text": "(SCD)"}, {"id": "b", "text": "(SBC)"}, {"id": "c", "text": "(SAD)"}], "correctOptionId": "a", "explanation": "AB // CD và CD nằm trong mp(SCD), nên AB // (SCD)."}
                ]
            },
            {
                "id": "math-11-hai-mat-phang-song-song",
                "title": "Hai mặt phẳng song song",
                "guidance": ["Hai mặt phẳng (P) và (Q) song song nếu chúng không có điểm chung.", "Nếu (P) chứa hai đường cắt nhau cùng song song với (Q) thì (P) // (Q)."],
                "examples": ["Mặt trần nhà và mặt sàn nhà là hình ảnh của hai mặt phẳng song song."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Hai mặt phẳng song song có điểm chung không?", "options": [{"id": "a", "text": "Có"}, {"id": "b", "text": "Không"}, {"id": "c", "text": "Tùy trường hợp"}], "correctOptionId": "b", "explanation": "Theo định nghĩa, hai mặt phẳng song song tuyệt đối không có điểm chung."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Định lí Talet trong không gian áp dụng cho?", "options": [{"id": "a", "text": "3 MP song song"}, {"id": "b", "text": "2 MP song song"}, {"id": "c", "text": "3 MP cắt nhau"}], "correctOptionId": "a", "explanation": "Định lí Talet không gian phát biểu: 3 mặt phẳng song song chắn trên hai cát tuyến bất kì các đoạn thẳng tương ứng tỉ lệ."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Cho lăng trụ ABC.A'B'C'. Mặt (ABC) song song với mặt nào?", "options": [{"id": "a", "text": "(A'B'C')"}, {"id": "b", "text": "(ABB'A')"}, {"id": "c", "text": "(BCC'B')"}], "correctOptionId": "a", "explanation": "Hai đáy của hình lăng trụ luôn nằm trên hai mặt phẳng song song với nhau."}
                ]
            },
            {
                "id": "math-11-dao-ham-dinh-nghia",
                "title": "Định nghĩa và ý nghĩa đạo hàm",
                "guidance": ["Đạo hàm của f(x) tại x0 là giới hạn của tỉ số biến thiên (f(x)-f(x0))/(x-x0) khi x tiến về x0.", "Ý nghĩa hình học: f'(x0) là hệ số góc của tiếp tuyến."],
                "examples": ["Tiếp tuyến của y = f(x) tại điểm có hoành độ x0 có phương trình y = f'(x0)(x - x0) + f(x0)."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Ý nghĩa hình học của f'(x0) là?", "options": [{"id": "a", "text": "Hệ số góc tiếp tuyến"}, {"id": "b", "text": "Vận tốc"}, {"id": "c", "text": "Tọa độ đỉnh"}], "correctOptionId": "a", "explanation": "Đạo hàm tại một điểm bằng hệ số góc của tiếp tuyến với đồ thị tại điểm đó."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Vận tốc tức thời v(t) của chuyển động s(t) bằng?", "options": [{"id": "a", "text": "s'(t)"}, {"id": "b", "text": "s(t)/t"}, {"id": "c", "text": "s''(t)"}], "correctOptionId": "a", "explanation": "Ý nghĩa vật lý: Vận tốc là đạo hàm bậc nhất của quãng đường theo thời gian."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Nếu f'(x0) = 0 thì tiếp tuyến tại điểm đó có đặc điểm gì?", "options": [{"id": "a", "text": "Song song trục tung"}, {"id": "b", "text": "Song song/trùng trục hoành"}, {"id": "c", "text": "Độ dốc bằng 1"}], "correctOptionId": "b", "explanation": "Hệ số góc bằng 0 tức là tiếp tuyến là đường thẳng y = hằng số, song song với trục hoành Ox."}
                ]
            },
            {
                "id": "math-11-quy-tac-dao-ham",
                "title": "Các quy tắc tính đạo hàm",
                "guidance": ["(u+v)' = u' + v'", "(uv)' = u'v + uv'", "(u/v)' = (u'v - uv') / v^2", "(x^n)' = n.x^(n-1)"],
                "examples": ["y = x^3 => y' = 3x^2.", "y = sin x => y' = cos x."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Đạo hàm của hàm số y = x^4 là?", "options": [{"id": "a", "text": "4x^3"}, {"id": "b", "text": "3x^4"}, {"id": "c", "text": "x^3"}], "correctOptionId": "a", "explanation": "Áp dụng công thức (x^n)' = n.x^(n-1)."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Đạo hàm của y = sin(2x) là?", "options": [{"id": "a", "text": "cos(2x)"}, {"id": "b", "text": "2cos(2x)"}, {"id": "c", "text": "-2cos(2x)"}], "correctOptionId": "b", "explanation": "Áp dụng đạo hàm hàm hợp: (sin u)' = u'.cos u => 2cos(2x)."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Đạo hàm của y = 1/x là?", "options": [{"id": "a", "text": "1/x^2"}, {"id": "b", "text": "-1/x^2"}, {"id": "c", "text": "ln(x)"}], "correctOptionId": "b", "explanation": "(1/x)' = (x^-1)' = -1.x^-2 = -1/x^2."}
                ]
            },
            {
                "id": "math-11-bien-co-hop-giao",
                "title": "Biến cố hợp và biến cố giao",
                "guidance": ["Biến cố hợp (A U B) xảy ra khi A hoặc B xảy ra.", "Biến cố giao (AB) xảy ra khi cả A và B cùng xảy ra."],
                "examples": ["Tung xúc xắc: A='chẵn', B='chia hết cho 3'. A giao B là biến cố tung được mặt 6 chấm."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Ký hiệu của biến cố giao giữa A và B là?", "options": [{"id": "a", "text": "A U B"}, {"id": "b", "text": "AB"}, {"id": "c", "text": "A \\ B"}], "correctOptionId": "b", "explanation": "Biến cố giao thường viết tắt là AB hoặc A ∩ B."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Hai biến cố xung khắc thì phần giao của chúng bằng?", "options": [{"id": "a", "text": "Biến cố chắc chắn"}, {"id": "b", "text": "Tập rỗng"}, {"id": "c", "text": "Tùy ý"}], "correctOptionId": "b", "explanation": "Xung khắc tức là không thể cùng xảy ra, nên phần giao là tập rỗng (biến cố không thể)."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "A='chẵn', B='nhỏ hơn 4'. Xúc xắc 6 mặt. Tập hợp A hợp B là?", "options": [{"id": "a", "text": "{2}"}, {"id": "b", "text": "{1, 2, 3, 4, 6}"}, {"id": "c", "text": "{2, 4, 6}"}], "correctOptionId": "b", "explanation": "A={2,4,6}, B={1,2,3}. A hợp B là kết hợp tất cả: {1,2,3,4,6}."}
                ]
            },
            {
                "id": "math-11-xac-suat-co-dieu-kien",
                "title": "Xác suất có điều kiện và Quy tắc nhân",
                "guidance": ["Xác suất có điều kiện của A biết B đã xảy ra: P(A|B) = P(AB)/P(B).", "Quy tắc nhân: P(AB) = P(A) * P(B) nếu độc lập."],
                "examples": ["Tung 2 đồng xu độc lập. XS mặt ngửa đồng xu 2 biết đồng xu 1 ngửa vẫn là 1/2 vì chúng độc lập."],
                "questions": [
                    {"id": "q1", "difficulty": "EASY", "prompt": "Nếu A và B độc lập thì P(AB) bằng?", "options": [{"id": "a", "text": "P(A) + P(B)"}, {"id": "b", "text": "P(A) * P(B)"}, {"id": "c", "text": "P(A) / P(B)"}], "correctOptionId": "b", "explanation": "Theo quy tắc nhân xác suất cho 2 biến cố độc lập."},
                    {"id": "q2", "difficulty": "MEDIUM", "prompt": "Công thức P(A|B) là?", "options": [{"id": "a", "text": "P(A)/P(B)"}, {"id": "b", "text": "P(AB)/P(B)"}, {"id": "c", "text": "P(AB)/P(A)"}], "correctOptionId": "b", "explanation": "Xác suất của A với điều kiện B bằng xác suất đồng thời chia cho xác suất của B."},
                    {"id": "q3", "difficulty": "APPLIED", "prompt": "Có 3 bi đỏ, 2 bi xanh. Rút 2 bi không hoàn lại. XS rút 2 bi đỏ là?", "options": [{"id": "a", "text": "3/10"}, {"id": "b", "text": "9/25"}, {"id": "c", "text": "1/2"}], "correctOptionId": "a", "explanation": "Lần 1 đỏ: 3/5. Lần 2 đỏ: 2/4. P = (3/5)*(2/4) = 6/20 = 3/10."}
                ]
            }
        ]
    }

def generate_placeholder_subject(subject_id, outline_text):
    lessons_titles = [t.strip() for t in outline_text.split('|')]
    if len(lessons_titles) != 15:
        # Pad or slice to exactly 15
        lessons_titles = (lessons_titles + [f"Bài {i}" for i in range(15)])[:15]

    lessons = []
    for i, title in enumerate(lessons_titles):
        slug = make_slug(title)
        lessons.append({
            "id": f"{subject_id}-11-{slug}",
            "title": title,
            "guidance": [f"Nội dung trọng tâm bài {title} theo khung CT GDPT 2018."],
            "examples": [f"Ví dụ ứng dụng kiến thức phần {title} vào thực tiễn."],
            "questions": [
                {
                    "id": f"{subject_id}-11-{slug}-q1",
                    "prompt": f"Câu hỏi nhận biết khái niệm cơ bản của bài {title}?",
                    "difficulty": "EASY",
                    "options": [{"id": "a", "text": "Khái niệm đúng"}, {"id": "b", "text": "Sai lầm 1"}, {"id": "c", "text": "Sai lầm 2"}],
                    "correctOptionId": "a",
                    "explanation": "Giải thích chi tiết vì sao A là đáp án đúng mức độ nhận biết."
                },
                {
                    "id": f"{subject_id}-11-{slug}-q2",
                    "prompt": f"Câu hỏi thông hiểu hoặc tính toán cơ bản phần {title}?",
                    "difficulty": "MEDIUM",
                    "options": [{"id": "a", "text": "Phương án nhiễu 1"}, {"id": "b", "text": "Đáp án chính xác"}, {"id": "c", "text": "Phương án nhiễu 2"}],
                    "correctOptionId": "b",
                    "explanation": "Giải thích chi tiết các bước tính toán hoặc logic mức thông hiểu."
                },
                {
                    "id": f"{subject_id}-11-{slug}-q3",
                    "prompt": f"Tình huống thực tế áp dụng kiến thức {title} giải quyết vấn đề?",
                    "difficulty": "APPLIED",
                    "options": [{"id": "a", "text": "Hướng đi sai lệch"}, {"id": "b", "text": "Giải pháp chưa triệt để"}, {"id": "c", "text": "Phương pháp tối ưu"}],
                    "correctOptionId": "c",
                    "explanation": "Mở rộng và giải thích sâu vì sao đáp án C giải quyết được tình huống thực tế."
                }
            ]
        })

    # Metadata map
    titles = {
        "physics": "Vật lí 11: Dao động, Sóng và Điện từ trường",
        "chemistry": "Hóa học 11: Cân bằng hóa học và Đại cương Hữu cơ",
        "biology": "Sinh học 11: Chuyển hóa vật chất và Năng lượng sinh học",
        "literature": "Ngữ văn 11: Đọc hiểu thể loại và Kỹ năng viết nghị luận",
        "english": "Tiếng Anh 11: Global Success & Lifelong Learning",
        "history": "Lịch sử 11: Lịch sử thế giới hiện đại và Văn minh Đông Nam Á",
        "geography": "Địa lí 11: Liên kết toàn cầu và Kinh tế khu vực",
        "economics_law": "KTPL 11: Cạnh tranh thị trường và Quyền công dân"
    }

    return {
        "subjectId": subject_id,
        "grade": 11,
        "version": "intro-2026.1",
        "title": titles.get(subject_id, f"{subject_id.capitalize()} 11"),
        "published": True,
        "coverage": "PARTIAL_GRADE_11",
        "extendedReviews": False,
        "sourceNote": "Nội dung đang ở dạng cấu trúc khung (Placeholder) được sinh tự động bởi hệ thống AI Mentor để đảm bảo đúng định dạng JSON 3 câu hỏi (EASY/MEDIUM/APPLIED). Giáo viên cần hiệu đính nội dung chi tiết.",
        "lessons": lessons
    }

def main():
    target_dir = os.path.join(os.path.dirname(__file__), "..", "curriculum")
    os.makedirs(target_dir, exist_ok=True)

    # 1. Generate Math manually (fully detailed)
    math_data = generate_math_11()
    with open(os.path.join(target_dir, "math-11.json"), "w", encoding="utf-8") as f:
        json.dump(math_data, f, ensure_ascii=False, indent=2)
    print("Generated: math-11.json (Fully detailed 15 lessons)")

    # 2. Generate other 8 subjects dynamically
    for subject_id, outline in OUTLINES.items():
        data = generate_placeholder_subject(subject_id, outline)
        file_path = os.path.join(target_dir, f"{subject_id}-11.json")
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"Generated: {subject_id}-11.json (Placeholder template for 15 lessons)")

    print("Successfully built all 9 Grade 11 Curriculum JSON files!")

if __name__ == "__main__":
    main()
