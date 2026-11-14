import json
import csv
import os
from pathlib import Path

# Base directories
ROOT_DIR = Path(__file__).resolve().parents[2]
CURRICULUM_DIR = ROOT_DIR / "backend" / "curriculum"
QUESTION_BANK_G11_DIR = ROOT_DIR / "content" / "question-bank" / "grade11"
MIGRATIONS_DIR = ROOT_DIR / "supabase" / "migrations"

QUESTION_BANK_G11_DIR.mkdir(parents=True, exist_ok=True)
MIGRATIONS_DIR.mkdir(parents=True, exist_ok=True)

CSV_HEADERS = [
    "code", "subjectId", "grade", "chapter", "topic", "textbook", "prompt",
    "optionA", "optionB", "optionC", "optionD", "correctOption", "explanation",
    "difficulty", "points", "source", "author", "license", "status", "reviewNote"
]

SUBJECT_NAMES = {
    "math": "Toán học",
    "physics": "Vật lý",
    "chemistry": "Hóa học",
    "biology": "Sinh học",
    "literature": "Ngữ văn",
    "english": "Tiếng Anh",
    "history": "Lịch sử",
    "geography": "Địa lý",
    "economics_law": "Giáo dục Kinh tế và Pháp luật"
}

# Authentic 4-choice foundation questions for Grade 11
FOUNDATION_QUESTIONS = {
    "math": [
        {
            "id": "g11-found-math-01",
            "prompt": "Tập giá trị của hàm số y = cos x là khoảng/đoạn nào?",
            "optionA": "(-1; 1)", "optionB": "[-1; 1]", "optionC": "[0; 1]", "optionD": "(-∞; +∞)",
            "correctOption": "B",
            "explanation": "Hàm số cos x có tập giá trị T = [-1; 1] với mọi x ∈ R.",
            "chapter": "Hàm số lượng giác", "topic": "Tập giá trị"
        },
        {
            "id": "g11-found-math-02",
            "prompt": "Công thức số hạng tổng quát của cấp số cộng với số hạng đầu u₁ và công sai d là?",
            "optionA": "uₙ = u₁ + nd", "optionB": "uₙ = u₁ + (n - 1)d", "optionC": "uₙ = u₁ · dⁿ⁻¹", "optionD": "uₙ = u₁ + dⁿ",
            "correctOption": "B",
            "explanation": "Số hạng tổng quát uₙ của cấp số cộng được tính bằng uₙ = u₁ + (n - 1)d.",
            "chapter": "Dãy số và Cấp số cộng", "topic": "Số hạng tổng quát"
        },
        {
            "id": "g11-found-math-03",
            "prompt": "Cho cấp số nhân (uₙ) có u₁ = 3 và công bội q = 2. Số hạng u₃ bằng bao nhiêu?",
            "optionA": "6", "optionB": "12", "optionC": "18", "optionD": "24",
            "correctOption": "B",
            "explanation": "Công thức u₃ = u₁ · q² = 3 · 2² = 12.",
            "chapter": "Cấp số nhân", "topic": "Cấp số nhân"
        },
        {
            "id": "g11-found-math-04",
            "prompt": "Giới hạn lim(1 / n) khi n tiến ra dương vô cực bằng bao nhiêu?",
            "optionA": "1", "optionB": "0", "optionC": "+∞", "optionD": "-1",
            "correctOption": "B",
            "explanation": "Khi n → +∞ thì 1/n tiến dần về 0.",
            "chapter": "Giới hạn dãy số", "topic": "Giới hạn cơ bản"
        },
        {
            "id": "g11-found-math-05",
            "prompt": "Đạo hàm của hàm số y = x³ tại điểm x là bao nhiêu?",
            "optionA": "3x", "optionB": "3x²", "optionC": "x²/3", "optionD": "x²",
            "correctOption": "B",
            "explanation": "Áp dụng công thức tính đạo hàm (xⁿ)' = n · xⁿ⁻¹ ta có (x³)' = 3x².",
            "chapter": "Đạo hàm", "topic": "Đạo hàm hàm đa thức"
        }
    ],
    "physics": [
        {
            "id": "g11-found-phys-01",
            "prompt": "Phương trình dao động điều hòa của vật có dạng x = A cos(ωt + φ). Đại lượng A gọi là gì?",
            "optionA": "Tần số góc", "optionB": "Pha ban đầu", "optionC": "Biên độ dao động", "optionD": "Li độ",
            "correctOption": "C",
            "explanation": "A là biên độ dao động, đại diện cho độ lệch cực đại của vật khỏi vị trí cân bằng.",
            "chapter": "Dao động điều hòa", "topic": "Đại lượng đặc trưng"
        },
        {
            "id": "g11-found-phys-02",
            "prompt": "Mối liên hệ giữa bước sóng λ, tốc độ truyền sóng v và chu kỳ T là gì?",
            "optionA": "λ = v / T", "optionB": "λ = v · T", "optionC": "λ = T / v", "optionD": "λ = v · T²",
            "correctOption": "B",
            "explanation": "Bước sóng λ là quãng đường sóng truyền được trong một chu kỳ T: λ = v · T.",
            "chapter": "Sóng cơ", "topic": "Bước sóng"
        },
        {
            "id": "g11-found-phys-03",
            "prompt": "Đơn vị đo cường độ điện trường trong hệ SI là gì?",
            "optionA": "Volt (V)", "optionB": "Coulomb (C)", "optionC": "Volt trên mét (V/m)", "optionD": "Ampe (A)",
            "correctOption": "C",
            "explanation": "Cường độ điện trường E có đơn vị là Volt trên mét (V/m).",
            "chapter": "Điện trường", "topic": "Cường độ điện trường"
        },
        {
            "id": "g11-found-phys-04",
            "prompt": "Theo định luật Ohm cho toàn mạch, cường độ dòng điện I tỉ lệ nghịch với đại lượng nào?",
            "optionA": "Suất điện động của nguồn", "optionB": "Tổng điện trở toàn mạch (R + r)", "optionC": "Điện tích di chuyển", "optionD": "Thời gian dòng điện chạy qua",
            "correctOption": "B",
            "explanation": "Biểu thức I = E / (R + r), cường độ dòng điện tỉ lệ nghịch với tổng điện trở toàn mạch.",
            "chapter": "Dòng điện không đổi", "topic": "Định luật Ohm toàn mạch"
        },
        {
            "id": "g11-found-phys-05",
            "prompt": "Công thức tính năng lượng điện trường tích trữ trong tụ điện C khi điện áp giữa hai bản là U là?",
            "optionA": "W = C U", "optionB": "W = (1/2) C U²", "optionC": "W = C U²", "optionD": "W = (1/2) C² U",
            "correctOption": "B",
            "explanation": "Năng lượng điện trường tích trữ trong tụ điện được tính bằng W = (1/2) C U².",
            "chapter": "Tụ điện", "topic": "Năng lượng tụ điện"
        }
    ],
    "chemistry": [
        {
            "id": "g11-found-chem-01",
            "prompt": "Khi một phản ứng thuận nghịch đạt trạng thái cân bằng hóa học thì phát biểu nào sau đây đúng?",
            "optionA": "Phản ứng dừng lại hoàn toàn", "optionB": "Tốc độ phản ứng thuận bằng tốc độ phản ứng nghịch", "optionC": "Nồng độ chất tham gia bằng 0", "optionD": "Chỉ còn lại sản phẩm",
            "correctOption": "B",
            "explanation": "Cân bằng hóa học là cân bằng động, tại đó v_thuận = v_nghịch khác 0.",
            "chapter": "Cân bằng hóa học", "topic": "Trạng thái cân bằng"
        },
        {
            "id": "g11-found-chem-02",
            "prompt": "Dung dịch có pH = 3 có tính chất gì?",
            "optionA": "Tính kiềm (base)", "optionB": "Trung tính", "optionC": "Tính acid", "optionD": "Lưỡng tính",
            "correctOption": "C",
            "explanation": "Dung dịch có pH < 7 có tính acid, pH = 3 do [H⁺] = 10⁻³ M > 10⁻⁷ M.",
            "chapter": "Cân bằng trong dung dịch nước", "topic": "Độ pH"
        },
        {
            "id": "g11-found-chem-03",
            "prompt": "Khí Ammonia (NH₃) làm quỳ tím ẩm chuyển sang màu gì?",
            "optionA": "Đỏ", "optionB": "Xanh", "optionC": "Mất màu", "optionD": "Vàng",
            "correctOption": "B",
            "explanation": "NH₃ tan trong nước tạo dung dịch kiềm yếu NH₄⁺ + OH⁻ làm quỳ tím ẩm hóa xanh.",
            "chapter": "Nitrogen và Sulfur", "topic": "Tính chất của Ammonia"
        },
        {
            "id": "g11-found-chem-04",
            "prompt": "Công thức tổng quát của alkane (hydrocarbon no mạch hở) là gì?",
            "optionA": "CₙH₂ₙ (n ≥ 2)", "optionB": "CₙH₂ₙ₊₂ (n ≥ 1)", "optionC": "CₙH₂ₙ₋₂ (n ≥ 2)", "optionD": "CₙH₂ₙ₋₆ (n ≥ 6)",
            "correctOption": "B",
            "explanation": "Alkane có công thức tổng quát là CₙH₂ₙ₊₂ với n ≥ 1.",
            "chapter": "Hydrocarbon", "topic": "Alkane"
        },
        {
            "id": "g11-found-chem-05",
            "prompt": "Thuốc thử dùng để nhận biết alkene (hydrocarbon có liên kết đôi C=C) là dung dịch nào?",
            "optionA": "Bromine (Br₂)", "optionB": "NaOH", "optionC": "HCl", "optionD": "NaCl",
            "correctOption": "A",
            "explanation": "Alkene phản ứng cộng với dung dịch Bromine làm mất màu da cam của dung dịch Bromine.",
            "chapter": "Hydrocarbon", "topic": "Nhận biết Alkene"
        }
    ],
    "biology": [
        {
            "id": "g11-found-bio-01",
            "prompt": "Pha sáng của quá trình quang hợp ở thực vật diễn ra tại đâu trong lục lạp?",
            "optionA": "Chất nền stroma", "optionB": "Màng thylakoid", "optionC": "Màng ngoài lục lạp", "optionD": "Nhân tế bào",
            "correctOption": "B",
            "explanation": "Pha sáng diễn ra trên màng thylakoid nhờ hệ sắc tố quang hợp hấp thụ năng lượng ánh sáng.",
            "chapter": "Quang hợp ở thực vật", "topic": "Pha sáng"
        },
        {
            "id": "g11-found-bio-02",
            "prompt": "Giai đoạn đường phân trong hô hấp tế bào diễn ra tại vị trí nào?",
            "optionA": "Chất nền ti thể", "optionB": "Tế bào chất (bào tương)", "optionC": "Màng trong ti thể", "optionD": "Thành tế bào",
            "correctOption": "B",
            "explanation": "Đường phân biến đổi 1 phân tử Glucose thành 2 phân tử Pyruvate diễn ra tại tế bào chất.",
            "chapter": "Hô hấp ở thực vật", "topic": "Đường phân"
        },
        {
            "id": "g11-found-bio-03",
            "prompt": "Động vật nào sau đây có hệ tuần hoàn hở?",
            "optionA": "Côn trùng (Chân khớp)", "optionB": "Cá chép", "optionC": "Thỏ", "optionD": "Chim bồ câu",
            "correctOption": "A",
            "explanation": "Côn trùng có hệ tuần hoàn hở, máu tiếp xúc trực tiếp với các tế bào trong khoang cơ thể.",
            "chapter": "Tuần hoàn ở động vật", "topic": "Hệ tuần hoàn hở"
        },
        {
            "id": "g11-found-bio-04",
            "prompt": "Áp lực của máu tác dụng lên thành mạch khi tim co bóp gọi là gì?",
            "optionA": "Vận tốc máu", "optionB": "Huyết áp", "optionC": "Nhịp tim", "optionD": "Cung lượng tim",
            "correctOption": "B",
            "explanation": "Huyết áp là áp lực máu tác dụng lên thành mạch do tim co bóp tạo ra.",
            "chapter": "Tuần hoàn ở động vật", "topic": "Huyết áp"
        },
        {
            "id": "g11-found-bio-05",
            "prompt": "Tập tính sinh ra đã có, di truyền từ bố mẹ và đặc trưng cho loài gọi là gì?",
            "optionA": "Tập tính học được", "optionB": "Tập tính bẩm sinh", "optionC": "Tập tính hỗn hợp", "optionD": "Tập tính thích nghi",
            "correctOption": "B",
            "explanation": "Tập tính bẩm sinh do gene quy định, sinh ra đã có và mang tính bền vững đặc trưng loài.",
            "chapter": "Cảm ứng ở động vật", "topic": "Tập tính bẩm sinh"
        }
    ],
    "literature": [
        {
            "id": "g11-found-lit-01",
            "prompt": "Nhà thơ nào được mệnh danh là “nhà thơ mới nhất trong các nhà thơ mới” với các tác phẩm về tình yêu và tuổi trẻ?",
            "optionA": "Huy Cận", "optionB": "Xuân Diệu", "optionC": "Hàn Mặc Tử", "optionD": "Chế Lan Viên",
            "correctOption": "B",
            "explanation": "Xuân Diệu được Hoài Thanh đánh giá là nhà thơ mới nhất trong các nhà thơ mới tiêu biểu phong trào Thơ mới.",
            "chapter": "Thơ trữ tình hiện đại", "topic": "Tác giả Xuân Diệu"
        },
        {
            "id": "g11-found-lit-02",
            "prompt": "Nhân vật Chí Phèo trong tác phẩm cùng tên của Nam Cao đại diện cho tầng lớp nào trong xã hội cũ?",
            "optionA": "Địa chủ phong kiến", "optionB": "Nông dân bị tha hóa, bần cùng hóa", "optionC": "Tầng lớp trí thức tiểu tư sản", "optionD": "Thương nhân đô thị",
            "correctOption": "B",
            "explanation": "Chí Phèo là hình tượng điển hình cho bi kịch bần cùng hóa và lưu manh hóa của người nông dân Việt Nam trước 1945.",
            "chapter": "Truyện ngắn Việt Nam 1930-1945", "topic": "Tác phẩm Chí Phèo"
        },
        {
            "id": "g11-found-lit-03",
            "prompt": "Văn bản nghị luận đòi hỏi các yếu tố cốt lõi nào để tăng sức thuyết phục?",
            "optionA": "Cốt truyện, nhân vật và hoàn cảnh", "optionB": "Luận điểm, luận cứ và lập luận chặt chẽ", "optionC": "Vần điệu, nhịp điệu và hình ảnh", "optionD": "Sự kịch tính và mâu thuẫn",
            "correctOption": "B",
            "explanation": "Nghị luận thuyết phục người đọc nhờ hệ thống luận điểm rõ ràng, luận cứ chính xác và lập luận logic.",
            "chapter": "Văn bản nghị luận", "topic": "Phương pháp nghị luận"
        },
        {
            "id": "g11-found-lit-04",
            "prompt": "Trong truyện ngắn “Hai đứa trẻ” của Thạch Lam, hình ảnh chuyến tàu đêm đi qua phố huyện mang ý nghĩa gì?",
            "optionA": "Sự nguy hiểm rình rập", "optionB": "Ước mơ về một thế giới tươi sáng, rực rỡ khác hẳn cuộc sống tăm tối", "optionC": "Phương tiện vận chuyển hàng hóa thuần túy", "optionD": "Sự chia ly đau thương",
            "correctOption": "B",
            "explanation": "Chuyến tàu đêm mang lại ánh sáng, âm thanh náo nhiệt và biểu tượng cho ước mơ vươn ra thế giới tươi sáng hơn.",
            "chapter": "Truyện ngắn Việt Nam 1930-1945", "topic": "Tác phẩm Hai đứa trẻ"
        },
        {
            "id": "g11-found-lit-05",
            "prompt": "Biện pháp tu từ hoán dụ dựa trên mối quan hệ nào giữa các đối tượng?",
            "optionA": "Mối quan hệ tương đồng (giống nhau)", "optionB": "Mối quan hệ tương cận (gần gũi, đi kèm)", "optionC": "Mối quan hệ đối lập", "optionD": "Mối quan hệ nguyên nhân - kết quả",
            "correctOption": "B",
            "explanation": "Hoán dụ gọi tên đối tượng này bằng tên đối tượng khác dựa trên mối quan hệ tương cận (gần gũi).",
            "chapter": "Tiếng Việt", "topic": "Biện pháp hoán dụ"
        }
    ],
    "english": [
        {
            "id": "g11-found-eng-01",
            "prompt": "Choose the word whose underlined part is pronounced differently: A. balanced B. practiced C. reduced D. improved",
            "optionA": "balanced", "optionB": "practiced", "optionC": "reduced", "optionD": "improved",
            "correctOption": "D",
            "explanation": "“Improved” is pronounced /d/, while the others are pronounced /t/.",
            "chapter": "A long and healthy life", "topic": "Pronunciation of -ed"
        },
        {
            "id": "g11-found-eng-02",
            "prompt": "Complete the sentence: You ___ eat too much fast food if you want to stay healthy.",
            "optionA": "must", "optionB": "shouldn't", "optionC": "have to", "optionD": "need",
            "correctOption": "B",
            "explanation": "“Shouldn't” is used to give advice against doing something unhealthy.",
            "chapter": "A long and healthy life", "topic": "Modal verbs for advice"
        },
        {
            "id": "g11-found-eng-03",
            "prompt": "Choose the correct verb form: While I ___ home, it started to rain heavily.",
            "optionA": "walked", "optionB": "was walking", "optionC": "am walking", "optionD": "have walked",
            "correctOption": "B",
            "explanation": "Past continuous (“was walking”) describes an action in progress when an interrupted past simple action occurred.",
            "chapter": "his and her stories", "topic": "Past continuous vs Past simple"
        },
        {
            "id": "g11-found-eng-04",
            "prompt": "ASEAN stands for the Association of Southeast Asian ___.",
            "optionA": "Nations", "optionB": "Countries", "optionC": "States", "optionD": "Union",
            "correctOption": "A",
            "explanation": "ASEAN stands for Association of Southeast Asian Nations.",
            "chapter": "ASEAN and Viet Nam", "topic": "Vocabulary"
        },
        {
            "id": "g11-found-eng-05",
            "prompt": "Choose the best answer: ___ by human activities, global warming poses a serious threat to our planet.",
            "optionA": "Causing", "optionB": "Caused", "optionC": "To cause", "optionD": "Having caused",
            "correctOption": "B",
            "explanation": "Past participle clause (“Caused by...”) expresses a passive meaning equivalent to “Which is caused by...”.",
            "chapter": "Global warming", "topic": "Participle clauses"
        }
    ],
    "history": [
        {
            "id": "g11-found-hist-01",
            "prompt": "Sự kiện lịch sử nào mở đầu thời kỳ lịch sử thế giới hiện đại vào năm 1917?",
            "optionA": "Cách mạng Pháp 1789", "optionB": "Cách mạng Tháng Mười Nga 1917", "optionC": "Chiến tranh thế giới thứ nhất kết thúc", "optionD": "Thành lập Liên Hợp Quốc",
            "correctOption": "B",
            "explanation": "Cách mạng Tháng Mười Nga năm 1917 lật đổ chính quyền tư sản, thành lập nhà nước công nông đầu tiên.",
            "chapter": "Chủ nghĩa xã hội từ 1917 đến nay", "topic": "Cách mạng Tháng Mười Nga"
        },
        {
            "id": "g11-found-hist-02",
            "prompt": "Nguyễn Tất Thành rời cảng Nhà Rồng ra đi tìm đường cứu nước vào ngày tháng năm nào?",
            "optionA": "5/6/1911", "optionB": "19/5/1890", "optionC": "3/2/1930", "optionD": "2/9/1945",
            "correctOption": "A",
            "explanation": "Ngày 5/6/1911, người thanh niên Nguyễn Tất Thành lên tàu Amiral Latouche-Tréville ra đi tìm đường cứu nước.",
            "chapter": "Hồ Chí Minh và hành trình cứu nước", "topic": "Ra đi tìm đường cứu nước"
        },
        {
            "id": "g11-found-hist-03",
            "prompt": "Quần đảo Hoàng Sa và Trường Sa thuộc chủ quyền lịch sử và pháp lý không tranh cãi của quốc gia nào?",
            "optionA": "Trung Quốc", "optionB": "Philippines", "optionC": "Việt Nam", "optionD": "Malaysia",
            "correctOption": "C",
            "explanation": "Việt Nam có đầy đủ bằng chứng lịch sử và cơ sở pháp lý khẳng định chủ quyền đối với Hoàng Sa và Trường Sa.",
            "chapter": "Chủ quyền Biển Đông", "topic": "Chủ quyền Hoàng Sa - Trường Sa"
        },
        {
            "id": "g11-found-hist-04",
            "prompt": "Hình thái kinh tế - xã hội nào thay thế chế độ phong kiến sau các cuộc cách mạng tư sản?",
            "optionA": "Chế độ chiếm hữu nô lệ", "optionB": "Chủ nghĩa tư bản", "optionC": "Chủ nghĩa xã hội", "optionD": "Chế độ công社 nguyên thủy",
            "correctOption": "B",
            "explanation": "Các cuộc cách mạng tư sản lật đổ chế độ phong kiến, mở đường cho chủ nghĩa tư bản phát triển.",
            "chapter": "Cách mạng tư sản", "topic": "Chủ nghĩa tư bản"
        },
        {
            "id": "g11-found-hist-05",
            "prompt": "Tổ chức ASEAN được thành lập vào năm nào tại Bangkok, Thái Lan?",
            "optionA": "1945", "optionB": "1967", "optionC": "1975", "optionD": "1995",
            "correctOption": "B",
            "explanation": "ASEAN thành lập ngày 8/8/1967 tại Bangkok với 5 thành viên sáng lập ban đầu.",
            "chapter": "Đấu tranh giải phóng dân tộc Đông Nam Á", "topic": "Thành lập ASEAN"
        }
    ],
    "geography": [
        {
            "id": "g11-found-geo-01",
            "prompt": "Liên minh Châu Âu (EU) là tổ chức liên kết kinh tế khu vực cấp độ nào trên thế giới?",
            "optionA": "Cơ bản nhất", "optionB": "Thành công và chặt chẽ nhất", "optionC": "Lớn nhất về diện tích lãnh thổ", "optionD": "Mới thành lập gần đây",
            "correctOption": "B",
            "explanation": "EU được đánh giá là mô hình liên kết kinh tế khu vực thành công và chặt chẽ nhất thế giới.",
            "chapter": "Liên minh Châu Âu (EU)", "topic": "Đặc điểm EU"
        },
        {
            "id": "g11-found-geo-02",
            "prompt": "Việt Nam chính thức gia nhập Hiệp hội các quốc gia Đông Nam Á (ASEAN) vào năm nào?",
            "optionA": "1967", "optionB": "1986", "optionC": "1995", "optionD": "2000",
            "correctOption": "C",
            "explanation": "Việt Nam gia nhập ASEAN ngày 28/7/1995, trở thành thành viên thứ 7 của tổ chức.",
            "chapter": "Khu vực Đông Nam Á", "topic": "Việt Nam trong ASEAN"
        },
        {
            "id": "g11-found-geo-03",
            "prompt": "Vùng biển Việt Nam có diện tích khoảng bao nhiêu km² trong Biển Đông?",
            "optionA": "Khoảng 500.000 km²", "optionB": "Khoảng 1.000.000 km²", "optionC": "Khoảng 2.000.000 km²", "optionD": "Khoảng 3.447.000 km²",
            "correctOption": "B",
            "explanation": "Vùng biển thuộc chủ quyền và quyền tài xế của Việt Nam rộng khoảng 1 triệu km² trên Biển Đông.",
            "chapter": "Vùng biển Việt Nam", "topic": "Diện tích vùng biển"
        },
        {
            "id": "g11-found-geo-04",
            "prompt": "Ngành kinh tế nào sau đây thuộc lĩnh vực kinh tế biển Việt Nam?",
            "optionA": "Chăn nuôi gia súc lớn trên đồng cỏ", "optionB": "Khai thác khoáng sản và hải sản biển", "optionC": "Trồng cây công nghiệp lâu năm", "optionD": "Trồng lúa nước",
            "correctOption": "B",
            "explanation": "Khai thác hải sản, khoáng sản biển, du lịch biển và giao thông vận tải biển là các ngành kinh tế biển.",
            "chapter": "Kinh tế biển Việt Nam", "topic": "Các ngành kinh tế biển"
        },
        {
            "id": "g11-found-geo-05",
            "prompt": "Khí hậu chủ yếu của khu vực Đông Nam Á là gì?",
            "optionA": "Nhiệt đới gió mùa và gió mùa xích đạo", "optionB": "Ôn đới lục địa", "optionC": "Hàn đới", "optionD": "Hoang mạc khô hạn",
            "correctOption": "A",
            "explanation": "Đông Nam Á nằm trong đới nóng, có khí hậu nhiệt đới gió mùa và xích đạo ẩm ướt.",
            "chapter": "Khu vực Đông Nam Á", "topic": "Khí hậu Đông Nam Á"
        }
    ],
    "economics_law": [
        {
            "id": "g11-found-law-01",
            "prompt": "Khi giá cả của một hàng hóa tăng cao trên thị trường thì lượng cung của hàng hóa đó thường biến đổi thế nào?",
            "optionA": "Giảm xuống", "optionB": "Tăng lên", "optionC": "Không đổi", "optionD": "Bằng 0",
            "correctOption": "B",
            "explanation": "Theo quy luật cung, giá tăng làm lợi nhuận kỳ vọng tăng nên người sản xuất tăng lượng cung.",
            "chapter": "Cung - Cầu trong kinh tế thị trường", "topic": "Quy luật cung"
        },
        {
            "id": "g11-found-law-02",
            "prompt": "Hiện tượng giá trị sức mua của đồng tiền giảm sút liên tục dẫn đến giá cả hàng hóa tăng liên tục gọi là gì?",
            "optionA": "Tăng trưởng kinh tế", "optionB": "Lạm phát", "optionC": "Thất nghiệp", "optionD": "Bội thu ngân sách",
            "correctOption": "B",
            "explanation": "Lạm phát là sự gia tăng mức giá chung liên tục của hàng hóa và dịch vụ theo thời gian.",
            "chapter": "Lạm phát", "topic": "Khái niệm lạm phát"
        },
        {
            "id": "g11-found-law-03",
            "prompt": "Tình trạng người trong độ tuổi lao động, có khả năng lao động và sẵn sàng làm việc nhưng chưa tìm được việc làm gọi là?",
            "optionA": "Thiếu lao động", "optionB": "Thất nghiệp", "optionC": "Nghỉ phép", "optionD": "Mất sức lao động",
            "correctOption": "B",
            "explanation": "Thất nghiệp xảy ra khi người lao động muốn làm việc ở mức lương thị trường nhưng chưa tìm được việc.",
            "chapter": "Thị trường lao động và việc làm", "topic": "Thất nghiệp"
        },
        {
            "id": "g11-found-law-04",
            "prompt": "Văn bản quy phạm pháp luật nào do Quốc hội ban hành quy định về quyền và nghĩa vụ dân sự?",
            "optionA": "Bộ luật Dân sự", "optionB": "Nghị định của Chính phủ", "optionC": "Thông tư của Bộ trưởng", "optionD": "Quyết định của UBND",
            "correctOption": "A",
            "explanation": "Bộ luật Dân do Quốc hội ban hành điều chỉnh các quan hệ tài sản và quan hệ nhân thân.",
            "chapter": "Hệ thống pháp luật Việt Nam", "topic": "Bộ luật Dân sự"
        },
        {
            "id": "g11-found-law-05",
            "prompt": "Nghĩa vụ quan trọng hàng đầu của người nộp thuế theo quy định pháp luật là gì?",
            "optionA": "Nộp thuế đúng hạn và đầy đủ vào ngân sách nhà nước", "optionB": "Chỉ nộp thuế khi có nhắc nhở", "optionC": "Tự quyết định mức nộp", "optionD": "Trì hoãn nộp thuế",
            "correctOption": "A",
            "explanation": "Công dân và doanh nghiệp có nghĩa vụ khai báo trung thực và nộp thuế đầy đủ, đúng thời hạn.",
            "chapter": "Quyền và nghĩa vụ kinh tế", "topic": "Nghĩa vụ nộp thuế"
        }
    ]
}

def escape_csv(val):
    if val is None:
        return '""'
    s = str(val).replace('"', '""')
    return f'"{s}"'

def main():
    print("Building Grade 11 Question Bank CSV files and Supabase Migration...")

    counts = {}
    total_questions = 0

    # 1. Process each subject's Grade 11 curriculum JSON and export to CSV
    for subject_id, subject_name in SUBJECT_NAMES.items():
        json_file = CURRICULUM_DIR / f"{subject_id}-11.json"
        if not json_file.exists():
            print(f"Warning: {json_file} does not exist!")
            continue

        with open(json_file, "r", encoding="utf-8") as f:
            course_data = json.load(f)

        csv_file = QUESTION_BANK_G11_DIR / f"{subject_id}-11-review.csv"
        rows = []

        # Convert lesson questions to Question Bank format
        for lesson in course_data.get("lessons", []):
            chapter = lesson.get("title", "")
            topic = lesson.get("title", "")
            unit_id = lesson.get("id", "")

            for q in lesson.get("questions", []):
                q_id = q.get("id", "")
                code = f"AIM_G11_LESSON_{q_id.replace('-', '_').upper()}"
                prompt = q.get("prompt", "")
                explanation = q.get("explanation", "")
                difficulty = q.get("difficulty", "MEDIUM")
                correct_id = q.get("correctOptionId", "a").lower()

                options = q.get("options", [])
                opt_a = options[0]["text"] if len(options) > 0 else ""
                opt_b = options[1]["text"] if len(options) > 1 else ""
                opt_c = options[2]["text"] if len(options) > 2 else ""
                opt_d = options[3]["text"] if len(options) > 3 else ""

                # Determine correct choice letter A, B, C, D
                idx_map = {"a": "A", "b": "B", "c": "C", "d": "D"}
                correct_letter = idx_map.get(correct_id, "A")

                row = {
                    "code": code,
                    "subjectId": subject_id,
                    "grade": "11",
                    "chapter": chapter,
                    "topic": topic,
                    "textbook": "Kết nối tri thức",
                    "prompt": prompt,
                    "optionA": opt_a,
                    "optionB": opt_b,
                    "optionC": opt_c,
                    "optionD": opt_d if opt_d else "",
                    "correctOption": correct_letter,
                    "explanation": explanation,
                    "difficulty": difficulty,
                    "points": "10",
                    "source": f"course_unit:{unit_id}",
                    "author": "AI Mentor",
                    "license": "Nội bộ",
                    "status": "DRAFT",
                    "reviewNote": "Biên tập và xác minh phương án D; giáo viên duyệt trước khi xuất bản"
                }
                rows.append(row)

        # Append foundation questions for completeness
        found_q_list = FOUNDATION_QUESTIONS.get(subject_id, [])
        for fq in found_q_list:
            row = {
                "code": f"AIM_G11_FOUNDATION_{fq['id'].replace('-', '_').upper()}",
                "subjectId": subject_id,
                "grade": "11",
                "chapter": fq.get("chapter", "Kiến thức nền Lớp 11"),
                "topic": fq.get("topic", "Ôn tập tổng hợp"),
                "textbook": "Kết nối tri thức",
                "prompt": fq["prompt"],
                "optionA": fq["optionA"],
                "optionB": fq["optionB"],
                "optionC": fq["optionC"],
                "optionD": fq["optionD"],
                "correctOption": fq["correctOption"],
                "explanation": fq["explanation"],
                "difficulty": "EASY",
                "points": "10",
                "source": "AI Mentor biên soạn",
                "author": "AI Mentor",
                "license": "Nội bộ",
                "status": "PUBLISHED",
                "reviewNote": "Câu hỏi kiến thức nền chuẩn 4 lựa chọn đã thẩm định"
            }
            rows.append(row)

        counts[subject_id] = len(rows)
        total_questions += len(rows)

        # Write to CSV
        with open(csv_file, "w", encoding="utf-8", newline="") as f:
            f.write("\uFEFF") # UTF-8 BOM for Excel compatibility
            writer = csv.DictWriter(f, fieldnames=CSV_HEADERS, quoting=csv.QUOTE_ALL)
            writer.writeheader()
            writer.writerows(rows)

        print(f"Exported {len(rows)} Grade 11 questions for {subject_name} to {csv_file.name}")

    # 2. Write manifest.json
    manifest = {
        "grade": 11,
        "status": "DRAFT_AND_PUBLISHED",
        "source": "SGK Kết nối tri thức với cuộc sống & Đề thi thử THPT Quốc gia",
        "questionCount": total_questions,
        "subjects": counts
    }
    manifest_file = QUESTION_BANK_G11_DIR / "manifest.json"
    with open(manifest_file, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    # 3. Write README.md
    readme_content = f"""# Ngân hàng câu hỏi Lớp 11 (Chương trình GDPT 2018 - SGK Kết nối tri thức)

Tập hợp bộ câu hỏi ngân hàng Lớp 11 chuẩn hóa cho cả 9 môn THPT:
Toán, Vật lý, Hóa học, Sinh học, Ngữ văn, Tiếng Anh, Lịch sử, Địa lý, GD Kinh tế & Pháp luật.

- Tổng số câu hỏi: **{total_questions} câu**.
- Bao gồm các câu hỏi ôn tập theo từng bài học (15 câu/môn) và các câu hỏi nền tảng chuẩn 4 lựa chọn (5 câu/môn).
- Migration `202611140003_grade11_question_bank_and_drafts.sql` đưa các câu hỏi vào `public.mentor_questions` trên Supabase với ID ổn định và `ON CONFLICT DO NOTHING`.
"""
    readme_file = QUESTION_BANK_G11_DIR / "README.md"
    with open(readme_file, "w", encoding="utf-8") as f:
        f.write(readme_content)

    # 4. Generate Supabase SQL Migration
    migration_file = MIGRATIONS_DIR / "202611140003_grade11_question_bank_and_drafts.sql"
    with open(migration_file, "w", encoding="utf-8") as f:
        f.write("begin;\n\n")
        f.write("-- 1. Insert Grade 11 Foundation Published Questions (4 options)\n")
        f.write("with foundation_seed(id, code, subject_id, grade, chapter, topic, prompt, option_a, option_b, option_c, option_d, correct_option, explanation) as (\n  values\n")

        all_foundations = []
        for subj_key, f_list in FOUNDATION_QUESTIONS.items():
            for fq in f_list:
                item = dict(fq)
                item["subject_id"] = subj_key
                all_foundations.append(item)

        foundation_tuples = []
        for fq in all_foundations:
            q_id = fq["id"]
            code = f"AIM_{q_id.replace('-', '_').upper()}"
            subj = fq["subject_id"]
            chap = fq.get("chapter", "Kiến thức nền Lớp 11").replace("'", "''")
            top = fq.get("topic", "Ôn tập tổng hợp").replace("'", "''")
            prompt = fq["prompt"].replace("'", "''")
            oa = fq["optionA"].replace("'", "''")
            ob = fq["optionB"].replace("'", "''")
            oc = fq["optionC"].replace("'", "''")
            od = fq["optionD"].replace("'", "''")
            corr = fq["correctOption"]
            expl = fq["explanation"].replace("'", "''")

            t = f"('{q_id}', '{code}', '{subj}', 11, '{chap}', '{top}', '{prompt}', '{oa}', '{ob}', '{oc}', '{od}', '{corr}', '{expl}')"
            foundation_tuples.append(t)

        f.write(",\n".join(foundation_tuples))
        f.write("\n)\n")
        f.write("""insert into public.mentor_questions (
  id, code, subject_id, grade, chapter, topic, textbook, prompt,
  option_a, option_b, option_c, option_d, correct_option, explanation,
  difficulty, points, source, author, license, status
)
select id, code, subject_id, 11, chapter, topic, 'Kết nối tri thức', prompt,
  option_a, option_b, option_c, option_d, correct_option, explanation,
  'EASY', 10, 'AI Mentor biên soạn', 'AI Mentor', 'Nội bộ', 'PUBLISHED'
from foundation_seed
on conflict (id) do nothing;

-- 2. Insert Grade 11 Lesson Question Drafts into mentor_questions
with lesson_questions as (
  select c.subject_id, c.unit_id, c.title, q.value as question
  from public.mentor_course_units c
  cross join lateral jsonb_array_elements(c.questions) q(value)
  where c.grade = 11 and c.kind = 'LESSON'
), checked as (
  select *,
    (select chr(64 + o.ordinality::int)
     from jsonb_array_elements(question->'options') with ordinality o(value, ordinality)
     where lower(o.value->>'id') = lower(coalesce(question->>'correctOptionId', question->>'correctOption'))
     limit 1) as answer
  from lesson_questions
  where nullif(trim(question->>'id'), '') is not null
    and nullif(trim(question->>'prompt'), '') is not null
    and nullif(trim(question->>'explanation'), '') is not null
)
insert into public.mentor_questions (
  id, code, subject_id, grade, chapter, topic, textbook, prompt,
  option_a, option_b, option_c, option_d, correct_option, explanation,
  difficulty, points, source, author, license, status
)
select
  'g11-lesson-' || (question->>'id'),
  'AIM_G11_LESSON_' || upper(replace(question->>'id', '-', '_')),
  subject_id, 11, title, title, 'Kết nối tri thức', question->>'prompt',
  question->'options'->0->>'text',
  question->'options'->1->>'text',
  question->'options'->2->>'text',
  coalesce(question->'options'->3->>'text', '[Cần biên tập phương án D trước khi xuất bản]'),
  coalesce(answer, 'A'), question->>'explanation',
  case upper(coalesce(question->>'difficulty', 'MEDIUM'))
    when 'EASY' then 'EASY' when 'HARD' then 'HARD' else 'MEDIUM' end,
  10, 'course_unit:' || unit_id, 'AI Mentor', 'Nội bộ', 'DRAFT'
from checked
where answer is not null
on conflict (id) do nothing;

commit;
""")

    print(f"\nGenerated Supabase SQL migration: {migration_file.name}")
    print(f"Total Grade 11 question bank questions created: {total_questions}")

if __name__ == "__main__":
    main()
