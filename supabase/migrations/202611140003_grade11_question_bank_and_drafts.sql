begin;

-- 1. Insert Grade 11 Foundation Published Questions (4 options)
with foundation_seed(id, code, subject_id, grade, chapter, topic, prompt, option_a, option_b, option_c, option_d, correct_option, explanation) as (
  values
('g11-found-math-01', 'AIM_G11_FOUND_MATH_01', 'math', 11, 'Hàm số lượng giác', 'Tập giá trị', 'Tập giá trị của hàm số y = cos x là khoảng/đoạn nào?', '(-1; 1)', '[-1; 1]', '[0; 1]', '(-∞; +∞)', 'B', 'Hàm số cos x có tập giá trị T = [-1; 1] với mọi x ∈ R.'),
('g11-found-math-02', 'AIM_G11_FOUND_MATH_02', 'math', 11, 'Dãy số và Cấp số cộng', 'Số hạng tổng quát', 'Công thức số hạng tổng quát của cấp số cộng với số hạng đầu u₁ và công sai d là?', 'uₙ = u₁ + nd', 'uₙ = u₁ + (n - 1)d', 'uₙ = u₁ · dⁿ⁻¹', 'uₙ = u₁ + dⁿ', 'B', 'Số hạng tổng quát uₙ của cấp số cộng được tính bằng uₙ = u₁ + (n - 1)d.'),
('g11-found-math-03', 'AIM_G11_FOUND_MATH_03', 'math', 11, 'Cấp số nhân', 'Cấp số nhân', 'Cho cấp số nhân (uₙ) có u₁ = 3 và công bội q = 2. Số hạng u₃ bằng bao nhiêu?', '6', '12', '18', '24', 'B', 'Công thức u₃ = u₁ · q² = 3 · 2² = 12.'),
('g11-found-math-04', 'AIM_G11_FOUND_MATH_04', 'math', 11, 'Giới hạn dãy số', 'Giới hạn cơ bản', 'Giới hạn lim(1 / n) khi n tiến ra dương vô cực bằng bao nhiêu?', '1', '0', '+∞', '-1', 'B', 'Khi n → +∞ thì 1/n tiến dần về 0.'),
('g11-found-math-05', 'AIM_G11_FOUND_MATH_05', 'math', 11, 'Đạo hàm', 'Đạo hàm hàm đa thức', 'Đạo hàm của hàm số y = x³ tại điểm x là bao nhiêu?', '3x', '3x²', 'x²/3', 'x²', 'B', 'Áp dụng công thức tính đạo hàm (xⁿ)'' = n · xⁿ⁻¹ ta có (x³)'' = 3x².'),
('g11-found-phys-01', 'AIM_G11_FOUND_PHYS_01', 'physics', 11, 'Dao động điều hòa', 'Đại lượng đặc trưng', 'Phương trình dao động điều hòa của vật có dạng x = A cos(ωt + φ). Đại lượng A gọi là gì?', 'Tần số góc', 'Pha ban đầu', 'Biên độ dao động', 'Li độ', 'C', 'A là biên độ dao động, đại diện cho độ lệch cực đại của vật khỏi vị trí cân bằng.'),
('g11-found-phys-02', 'AIM_G11_FOUND_PHYS_02', 'physics', 11, 'Sóng cơ', 'Bước sóng', 'Mối liên hệ giữa bước sóng λ, tốc độ truyền sóng v và chu kỳ T là gì?', 'λ = v / T', 'λ = v · T', 'λ = T / v', 'λ = v · T²', 'B', 'Bước sóng λ là quãng đường sóng truyền được trong một chu kỳ T: λ = v · T.'),
('g11-found-phys-03', 'AIM_G11_FOUND_PHYS_03', 'physics', 11, 'Điện trường', 'Cường độ điện trường', 'Đơn vị đo cường độ điện trường trong hệ SI là gì?', 'Volt (V)', 'Coulomb (C)', 'Volt trên mét (V/m)', 'Ampe (A)', 'C', 'Cường độ điện trường E có đơn vị là Volt trên mét (V/m).'),
('g11-found-phys-04', 'AIM_G11_FOUND_PHYS_04', 'physics', 11, 'Dòng điện không đổi', 'Định luật Ohm toàn mạch', 'Theo định luật Ohm cho toàn mạch, cường độ dòng điện I tỉ lệ nghịch với đại lượng nào?', 'Suất điện động của nguồn', 'Tổng điện trở toàn mạch (R + r)', 'Điện tích di chuyển', 'Thời gian dòng điện chạy qua', 'B', 'Biểu thức I = E / (R + r), cường độ dòng điện tỉ lệ nghịch với tổng điện trở toàn mạch.'),
('g11-found-phys-05', 'AIM_G11_FOUND_PHYS_05', 'physics', 11, 'Tụ điện', 'Năng lượng tụ điện', 'Công thức tính năng lượng điện trường tích trữ trong tụ điện C khi điện áp giữa hai bản là U là?', 'W = C U', 'W = (1/2) C U²', 'W = C U²', 'W = (1/2) C² U', 'B', 'Năng lượng điện trường tích trữ trong tụ điện được tính bằng W = (1/2) C U².'),
('g11-found-chem-01', 'AIM_G11_FOUND_CHEM_01', 'chemistry', 11, 'Cân bằng hóa học', 'Trạng thái cân bằng', 'Khi một phản ứng thuận nghịch đạt trạng thái cân bằng hóa học thì phát biểu nào sau đây đúng?', 'Phản ứng dừng lại hoàn toàn', 'Tốc độ phản ứng thuận bằng tốc độ phản ứng nghịch', 'Nồng độ chất tham gia bằng 0', 'Chỉ còn lại sản phẩm', 'B', 'Cân bằng hóa học là cân bằng động, tại đó v_thuận = v_nghịch khác 0.'),
('g11-found-chem-02', 'AIM_G11_FOUND_CHEM_02', 'chemistry', 11, 'Cân bằng trong dung dịch nước', 'Độ pH', 'Dung dịch có pH = 3 có tính chất gì?', 'Tính kiềm (base)', 'Trung tính', 'Tính acid', 'Lưỡng tính', 'C', 'Dung dịch có pH < 7 có tính acid, pH = 3 do [H⁺] = 10⁻³ M > 10⁻⁷ M.'),
('g11-found-chem-03', 'AIM_G11_FOUND_CHEM_03', 'chemistry', 11, 'Nitrogen và Sulfur', 'Tính chất của Ammonia', 'Khí Ammonia (NH₃) làm quỳ tím ẩm chuyển sang màu gì?', 'Đỏ', 'Xanh', 'Mất màu', 'Vàng', 'B', 'NH₃ tan trong nước tạo dung dịch kiềm yếu NH₄⁺ + OH⁻ làm quỳ tím ẩm hóa xanh.'),
('g11-found-chem-04', 'AIM_G11_FOUND_CHEM_04', 'chemistry', 11, 'Hydrocarbon', 'Alkane', 'Công thức tổng quát của alkane (hydrocarbon no mạch hở) là gì?', 'CₙH₂ₙ (n ≥ 2)', 'CₙH₂ₙ₊₂ (n ≥ 1)', 'CₙH₂ₙ₋₂ (n ≥ 2)', 'CₙH₂ₙ₋₆ (n ≥ 6)', 'B', 'Alkane có công thức tổng quát là CₙH₂ₙ₊₂ với n ≥ 1.'),
('g11-found-chem-05', 'AIM_G11_FOUND_CHEM_05', 'chemistry', 11, 'Hydrocarbon', 'Nhận biết Alkene', 'Thuốc thử dùng để nhận biết alkene (hydrocarbon có liên kết đôi C=C) là dung dịch nào?', 'Bromine (Br₂)', 'NaOH', 'HCl', 'NaCl', 'A', 'Alkene phản ứng cộng với dung dịch Bromine làm mất màu da cam của dung dịch Bromine.'),
('g11-found-bio-01', 'AIM_G11_FOUND_BIO_01', 'biology', 11, 'Quang hợp ở thực vật', 'Pha sáng', 'Pha sáng của quá trình quang hợp ở thực vật diễn ra tại đâu trong lục lạp?', 'Chất nền stroma', 'Màng thylakoid', 'Màng ngoài lục lạp', 'Nhân tế bào', 'B', 'Pha sáng diễn ra trên màng thylakoid nhờ hệ sắc tố quang hợp hấp thụ năng lượng ánh sáng.'),
('g11-found-bio-02', 'AIM_G11_FOUND_BIO_02', 'biology', 11, 'Hô hấp ở thực vật', 'Đường phân', 'Giai đoạn đường phân trong hô hấp tế bào diễn ra tại vị trí nào?', 'Chất nền ti thể', 'Tế bào chất (bào tương)', 'Màng trong ti thể', 'Thành tế bào', 'B', 'Đường phân biến đổi 1 phân tử Glucose thành 2 phân tử Pyruvate diễn ra tại tế bào chất.'),
('g11-found-bio-03', 'AIM_G11_FOUND_BIO_03', 'biology', 11, 'Tuần hoàn ở động vật', 'Hệ tuần hoàn hở', 'Động vật nào sau đây có hệ tuần hoàn hở?', 'Côn trùng (Chân khớp)', 'Cá chép', 'Thỏ', 'Chim bồ câu', 'A', 'Côn trùng có hệ tuần hoàn hở, máu tiếp xúc trực tiếp với các tế bào trong khoang cơ thể.'),
('g11-found-bio-04', 'AIM_G11_FOUND_BIO_04', 'biology', 11, 'Tuần hoàn ở động vật', 'Huyết áp', 'Áp lực của máu tác dụng lên thành mạch khi tim co bóp gọi là gì?', 'Vận tốc máu', 'Huyết áp', 'Nhịp tim', 'Cung lượng tim', 'B', 'Huyết áp là áp lực máu tác dụng lên thành mạch do tim co bóp tạo ra.'),
('g11-found-bio-05', 'AIM_G11_FOUND_BIO_05', 'biology', 11, 'Cảm ứng ở động vật', 'Tập tính bẩm sinh', 'Tập tính sinh ra đã có, di truyền từ bố mẹ và đặc trưng cho loài gọi là gì?', 'Tập tính học được', 'Tập tính bẩm sinh', 'Tập tính hỗn hợp', 'Tập tính thích nghi', 'B', 'Tập tính bẩm sinh do gene quy định, sinh ra đã có và mang tính bền vững đặc trưng loài.'),
('g11-found-lit-01', 'AIM_G11_FOUND_LIT_01', 'literature', 11, 'Thơ trữ tình hiện đại', 'Tác giả Xuân Diệu', 'Nhà thơ nào được mệnh danh là “nhà thơ mới nhất trong các nhà thơ mới” với các tác phẩm về tình yêu và tuổi trẻ?', 'Huy Cận', 'Xuân Diệu', 'Hàn Mặc Tử', 'Chế Lan Viên', 'B', 'Xuân Diệu được Hoài Thanh đánh giá là nhà thơ mới nhất trong các nhà thơ mới tiêu biểu phong trào Thơ mới.'),
('g11-found-lit-02', 'AIM_G11_FOUND_LIT_02', 'literature', 11, 'Truyện ngắn Việt Nam 1930-1945', 'Tác phẩm Chí Phèo', 'Nhân vật Chí Phèo trong tác phẩm cùng tên của Nam Cao đại diện cho tầng lớp nào trong xã hội cũ?', 'Địa chủ phong kiến', 'Nông dân bị tha hóa, bần cùng hóa', 'Tầng lớp trí thức tiểu tư sản', 'Thương nhân đô thị', 'B', 'Chí Phèo là hình tượng điển hình cho bi kịch bần cùng hóa và lưu manh hóa của người nông dân Việt Nam trước 1945.'),
('g11-found-lit-03', 'AIM_G11_FOUND_LIT_03', 'literature', 11, 'Văn bản nghị luận', 'Phương pháp nghị luận', 'Văn bản nghị luận đòi hỏi các yếu tố cốt lõi nào để tăng sức thuyết phục?', 'Cốt truyện, nhân vật và hoàn cảnh', 'Luận điểm, luận cứ và lập luận chặt chẽ', 'Vần điệu, nhịp điệu và hình ảnh', 'Sự kịch tính và mâu thuẫn', 'B', 'Nghị luận thuyết phục người đọc nhờ hệ thống luận điểm rõ ràng, luận cứ chính xác và lập luận logic.'),
('g11-found-lit-04', 'AIM_G11_FOUND_LIT_04', 'literature', 11, 'Truyện ngắn Việt Nam 1930-1945', 'Tác phẩm Hai đứa trẻ', 'Trong truyện ngắn “Hai đứa trẻ” của Thạch Lam, hình ảnh chuyến tàu đêm đi qua phố huyện mang ý nghĩa gì?', 'Sự nguy hiểm rình rập', 'Ước mơ về một thế giới tươi sáng, rực rỡ khác hẳn cuộc sống tăm tối', 'Phương tiện vận chuyển hàng hóa thuần túy', 'Sự chia ly đau thương', 'B', 'Chuyến tàu đêm mang lại ánh sáng, âm thanh náo nhiệt và biểu tượng cho ước mơ vươn ra thế giới tươi sáng hơn.'),
('g11-found-lit-05', 'AIM_G11_FOUND_LIT_05', 'literature', 11, 'Tiếng Việt', 'Biện pháp hoán dụ', 'Biện pháp tu từ hoán dụ dựa trên mối quan hệ nào giữa các đối tượng?', 'Mối quan hệ tương đồng (giống nhau)', 'Mối quan hệ tương cận (gần gũi, đi kèm)', 'Mối quan hệ đối lập', 'Mối quan hệ nguyên nhân - kết quả', 'B', 'Hoán dụ gọi tên đối tượng này bằng tên đối tượng khác dựa trên mối quan hệ tương cận (gần gũi).'),
('g11-found-eng-01', 'AIM_G11_FOUND_ENG_01', 'english', 11, 'A long and healthy life', 'Pronunciation of -ed', 'Choose the word whose underlined part is pronounced differently: A. balanced B. practiced C. reduced D. improved', 'balanced', 'practiced', 'reduced', 'improved', 'D', '“Improved” is pronounced /d/, while the others are pronounced /t/.'),
('g11-found-eng-02', 'AIM_G11_FOUND_ENG_02', 'english', 11, 'A long and healthy life', 'Modal verbs for advice', 'Complete the sentence: You ___ eat too much fast food if you want to stay healthy.', 'must', 'shouldn''t', 'have to', 'need', 'B', '“Shouldn''t” is used to give advice against doing something unhealthy.'),
('g11-found-eng-03', 'AIM_G11_FOUND_ENG_03', 'english', 11, 'his and her stories', 'Past continuous vs Past simple', 'Choose the correct verb form: While I ___ home, it started to rain heavily.', 'walked', 'was walking', 'am walking', 'have walked', 'B', 'Past continuous (“was walking”) describes an action in progress when an interrupted past simple action occurred.'),
('g11-found-eng-04', 'AIM_G11_FOUND_ENG_04', 'english', 11, 'ASEAN and Viet Nam', 'Vocabulary', 'ASEAN stands for the Association of Southeast Asian ___.', 'Nations', 'Countries', 'States', 'Union', 'A', 'ASEAN stands for Association of Southeast Asian Nations.'),
('g11-found-eng-05', 'AIM_G11_FOUND_ENG_05', 'english', 11, 'Global warming', 'Participle clauses', 'Choose the best answer: ___ by human activities, global warming poses a serious threat to our planet.', 'Causing', 'Caused', 'To cause', 'Having caused', 'B', 'Past participle clause (“Caused by...”) expresses a passive meaning equivalent to “Which is caused by...”.'),
('g11-found-hist-01', 'AIM_G11_FOUND_HIST_01', 'history', 11, 'Chủ nghĩa xã hội từ 1917 đến nay', 'Cách mạng Tháng Mười Nga', 'Sự kiện lịch sử nào mở đầu thời kỳ lịch sử thế giới hiện đại vào năm 1917?', 'Cách mạng Pháp 1789', 'Cách mạng Tháng Mười Nga 1917', 'Chiến tranh thế giới thứ nhất kết thúc', 'Thành lập Liên Hợp Quốc', 'B', 'Cách mạng Tháng Mười Nga năm 1917 lật đổ chính quyền tư sản, thành lập nhà nước công nông đầu tiên.'),
('g11-found-hist-02', 'AIM_G11_FOUND_HIST_02', 'history', 11, 'Hồ Chí Minh và hành trình cứu nước', 'Ra đi tìm đường cứu nước', 'Nguyễn Tất Thành rời cảng Nhà Rồng ra đi tìm đường cứu nước vào ngày tháng năm nào?', '5/6/1911', '19/5/1890', '3/2/1930', '2/9/1945', 'A', 'Ngày 5/6/1911, người thanh niên Nguyễn Tất Thành lên tàu Amiral Latouche-Tréville ra đi tìm đường cứu nước.'),
('g11-found-hist-03', 'AIM_G11_FOUND_HIST_03', 'history', 11, 'Chủ quyền Biển Đông', 'Chủ quyền Hoàng Sa - Trường Sa', 'Quần đảo Hoàng Sa và Trường Sa thuộc chủ quyền lịch sử và pháp lý không tranh cãi của quốc gia nào?', 'Trung Quốc', 'Philippines', 'Việt Nam', 'Malaysia', 'C', 'Việt Nam có đầy đủ bằng chứng lịch sử và cơ sở pháp lý khẳng định chủ quyền đối với Hoàng Sa và Trường Sa.'),
('g11-found-hist-04', 'AIM_G11_FOUND_HIST_04', 'history', 11, 'Cách mạng tư sản', 'Chủ nghĩa tư bản', 'Hình thái kinh tế - xã hội nào thay thế chế độ phong kiến sau các cuộc cách mạng tư sản?', 'Chế độ chiếm hữu nô lệ', 'Chủ nghĩa tư bản', 'Chủ nghĩa xã hội', 'Chế độ công社 nguyên thủy', 'B', 'Các cuộc cách mạng tư sản lật đổ chế độ phong kiến, mở đường cho chủ nghĩa tư bản phát triển.'),
('g11-found-hist-05', 'AIM_G11_FOUND_HIST_05', 'history', 11, 'Đấu tranh giải phóng dân tộc Đông Nam Á', 'Thành lập ASEAN', 'Tổ chức ASEAN được thành lập vào năm nào tại Bangkok, Thái Lan?', '1945', '1967', '1975', '1995', 'B', 'ASEAN thành lập ngày 8/8/1967 tại Bangkok với 5 thành viên sáng lập ban đầu.'),
('g11-found-geo-01', 'AIM_G11_FOUND_GEO_01', 'geography', 11, 'Liên minh Châu Âu (EU)', 'Đặc điểm EU', 'Liên minh Châu Âu (EU) là tổ chức liên kết kinh tế khu vực cấp độ nào trên thế giới?', 'Cơ bản nhất', 'Thành công và chặt chẽ nhất', 'Lớn nhất về diện tích lãnh thổ', 'Mới thành lập gần đây', 'B', 'EU được đánh giá là mô hình liên kết kinh tế khu vực thành công và chặt chẽ nhất thế giới.'),
('g11-found-geo-02', 'AIM_G11_FOUND_GEO_02', 'geography', 11, 'Khu vực Đông Nam Á', 'Việt Nam trong ASEAN', 'Việt Nam chính thức gia nhập Hiệp hội các quốc gia Đông Nam Á (ASEAN) vào năm nào?', '1967', '1986', '1995', '2000', 'C', 'Việt Nam gia nhập ASEAN ngày 28/7/1995, trở thành thành viên thứ 7 của tổ chức.'),
('g11-found-geo-03', 'AIM_G11_FOUND_GEO_03', 'geography', 11, 'Vùng biển Việt Nam', 'Diện tích vùng biển', 'Vùng biển Việt Nam có diện tích khoảng bao nhiêu km² trong Biển Đông?', 'Khoảng 500.000 km²', 'Khoảng 1.000.000 km²', 'Khoảng 2.000.000 km²', 'Khoảng 3.447.000 km²', 'B', 'Vùng biển thuộc chủ quyền và quyền tài xế của Việt Nam rộng khoảng 1 triệu km² trên Biển Đông.'),
('g11-found-geo-04', 'AIM_G11_FOUND_GEO_04', 'geography', 11, 'Kinh tế biển Việt Nam', 'Các ngành kinh tế biển', 'Ngành kinh tế nào sau đây thuộc lĩnh vực kinh tế biển Việt Nam?', 'Chăn nuôi gia súc lớn trên đồng cỏ', 'Khai thác khoáng sản và hải sản biển', 'Trồng cây công nghiệp lâu năm', 'Trồng lúa nước', 'B', 'Khai thác hải sản, khoáng sản biển, du lịch biển và giao thông vận tải biển là các ngành kinh tế biển.'),
('g11-found-geo-05', 'AIM_G11_FOUND_GEO_05', 'geography', 11, 'Khu vực Đông Nam Á', 'Khí hậu Đông Nam Á', 'Khí hậu chủ yếu của khu vực Đông Nam Á là gì?', 'Nhiệt đới gió mùa và gió mùa xích đạo', 'Ôn đới lục địa', 'Hàn đới', 'Hoang mạc khô hạn', 'A', 'Đông Nam Á nằm trong đới nóng, có khí hậu nhiệt đới gió mùa và xích đạo ẩm ướt.'),
('g11-found-law-01', 'AIM_G11_FOUND_LAW_01', 'economics_law', 11, 'Cung - Cầu trong kinh tế thị trường', 'Quy luật cung', 'Khi giá cả của một hàng hóa tăng cao trên thị trường thì lượng cung của hàng hóa đó thường biến đổi thế nào?', 'Giảm xuống', 'Tăng lên', 'Không đổi', 'Bằng 0', 'B', 'Theo quy luật cung, giá tăng làm lợi nhuận kỳ vọng tăng nên người sản xuất tăng lượng cung.'),
('g11-found-law-02', 'AIM_G11_FOUND_LAW_02', 'economics_law', 11, 'Lạm phát', 'Khái niệm lạm phát', 'Hiện tượng giá trị sức mua của đồng tiền giảm sút liên tục dẫn đến giá cả hàng hóa tăng liên tục gọi là gì?', 'Tăng trưởng kinh tế', 'Lạm phát', 'Thất nghiệp', 'Bội thu ngân sách', 'B', 'Lạm phát là sự gia tăng mức giá chung liên tục của hàng hóa và dịch vụ theo thời gian.'),
('g11-found-law-03', 'AIM_G11_FOUND_LAW_03', 'economics_law', 11, 'Thị trường lao động và việc làm', 'Thất nghiệp', 'Tình trạng người trong độ tuổi lao động, có khả năng lao động và sẵn sàng làm việc nhưng chưa tìm được việc làm gọi là?', 'Thiếu lao động', 'Thất nghiệp', 'Nghỉ phép', 'Mất sức lao động', 'B', 'Thất nghiệp xảy ra khi người lao động muốn làm việc ở mức lương thị trường nhưng chưa tìm được việc.'),
('g11-found-law-04', 'AIM_G11_FOUND_LAW_04', 'economics_law', 11, 'Hệ thống pháp luật Việt Nam', 'Bộ luật Dân sự', 'Văn bản quy phạm pháp luật nào do Quốc hội ban hành quy định về quyền và nghĩa vụ dân sự?', 'Bộ luật Dân sự', 'Nghị định của Chính phủ', 'Thông tư của Bộ trưởng', 'Quyết định của UBND', 'A', 'Bộ luật Dân do Quốc hội ban hành điều chỉnh các quan hệ tài sản và quan hệ nhân thân.'),
('g11-found-law-05', 'AIM_G11_FOUND_LAW_05', 'economics_law', 11, 'Quyền và nghĩa vụ kinh tế', 'Nghĩa vụ nộp thuế', 'Nghĩa vụ quan trọng hàng đầu của người nộp thuế theo quy định pháp luật là gì?', 'Nộp thuế đúng hạn và đầy đủ vào ngân sách nhà nước', 'Chỉ nộp thuế khi có nhắc nhở', 'Tự quyết định mức nộp', 'Trì hoãn nộp thuế', 'A', 'Công dân và doanh nghiệp có nghĩa vụ khai báo trung thực và nộp thuế đầy đủ, đúng thời hạn.')
)
insert into public.mentor_questions (
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
