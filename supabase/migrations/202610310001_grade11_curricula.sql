-- Migration: Grade 11 Curriculum Seeding
-- Generated automatically by AI Mentor tools

insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-quang-hop-thuc-vat',
    1,
    'LESSON',
    'Quang hợp ở thực vật',
    'Sinh học 11: Trao đổi chất & Chuyển hóa năng lượng (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Sinh học 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Quang hợp là quá trình lá cây sử dụng năng lượng ánh sáng mặt trời tổng hợp chất hữu cơ (Glucose) từ CO2 và H2O, giải phóng O2.", "Sắc tố quang hợp chính là diệp lục (chlorophyll a và b), sắc tố phụ là carotenoid.", "Pha sáng diễn ra ở màng thylakoid (tạo ATP, NADPH, O2). Pha tối (chu trình Calvin) diễn ra ở chất nền stroma của lục lạp cố định CO2 thành đường."]$json$::jsonb,
    $json$["Phương trình tổng quát: 6CO2 + 6H2O --(ánh sáng, diệp lục)--> C6H12O6 + 6O2."]$json$::jsonb,
    $json$[{"id": "bio-11-qh-q1", "prompt": "Sắc tố trực tiếp tham gia chuyển hóa năng lượng ánh sáng thành năng lượng hóa học trong ATP và NADPH là?", "difficulty": "EASY", "options": [{"id": "a", "text": "Diệp lục a ở trung tâm phản ứng"}, {"id": "b", "text": "Carotenoid"}, {"id": "c", "text": "Diệp lục b"}], "correctOptionId": "a", "explanation": "Chỉ diệp lục a trung tâm mới có khả năng giải phóng electron kích thích cho chuỗi truyền."}, {"id": "bio-11-qh-q2", "prompt": "Pha sáng của quá trình quang hợp diễn ra tại vị trí nào trong lục lạp?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Màng thylakoid"}, {"id": "b", "text": "Chất nền stroma"}, {"id": "c", "text": "Màng ngoài lục lạp"}], "correctOptionId": "a", "explanation": "Màng thylakoid chứa hệ sắc tố và chuỗi truyền electron cho pha sáng."}, {"id": "bio-11-qh-q3", "prompt": "Khí O2 được giải phóng trong quang hợp có nguồn gốc từ quá trình nào?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Quang phân li nước H2O ở pha sáng"}, {"id": "b", "text": "Cố định khí CO2 ở pha tối"}, {"id": "c", "text": "Phân giải ATP"}], "correctOptionId": "a", "explanation": "2H2O -> 4H+ + 4e- + O2 dưới tác dụng của ánh sáng ở pha sáng."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-ho-hap-thuc-vat',
    2,
    'LESSON',
    'Hô hấp ở thực vật',
    'Sinh học 11: Trao đổi chất & Chuyển hóa năng lượng (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Sinh học 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Hô hấp ở thực vật là quá trình oxy hóa chất hữu cơ (chủ yếu là Glucose) thành CO2 và H2O, giải phóng năng lượng dưới dạng ATP.", "Hô hấp hiếu khí gồm 3 giai đoạn: Đường phân (trong tế bào chất), Chu trình Krebs và Chuỗi truyền electron hô hấp (trong ti thể).", "Một phân tử Glucose qua hô hấp hiếu khí tạo ra khoảng 30-32 ATP."]$json$::jsonb,
    $json$["Hạt đang nảy mầm có cường độ hô hấp rất mạnh để cung cấp năng lượng cho sự phát triển của mầm."]$json$::jsonb,
    $json$[{"id": "bio-11-hh-q1", "prompt": "Bào quan chính thực hiện quá trình hô hấp hiếu khí ở tế bào thực vật là?", "difficulty": "EASY", "options": [{"id": "a", "text": "Ti thể"}, {"id": "b", "text": "Lục lạp"}, {"id": "c", "text": "Bộ máy Golgi"}], "correctOptionId": "a", "explanation": "Ti thể là nhà máy năng lượng, chứa chu trình Krebs và chuỗi truyền electron."}, {"id": "bio-11-hh-q2", "prompt": "Giai đoạn đường phân trong hô hấp tế bào diễn ra ở?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Tế bào chất (cytosol)"}, {"id": "b", "text": "Màng trong ti thể"}, {"id": "c", "text": "Chất nền ti thể"}], "correctOptionId": "a", "explanation": "Đường phân biến đổi Glucose thành Pyruvate diễn ra tại tế bào chất không cần O2."}, {"id": "bio-11-hh-q3", "prompt": "Tại sao trong bảo quản hạt nông sản (ngô, lúa) cần phải phơi khô làm giảm độ ẩm?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Giảm cường độ hô hấp tránh tiêu hao chất dinh dưỡng và sinh nhiệt"}, {"id": "b", "text": "Để tiêu diệt toàn bộ tế bào hạt"}, {"id": "c", "text": "Tăng tốc độ nảy mầm"}], "correctOptionId": "a", "explanation": "Nước là môi trường phản ứng; giảm ẩm làm hô hấp giảm xuống mức tối thiểu giúp bảo quản lâu dài."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-tieu-hoa-dong-vat',
    3,
    'LESSON',
    'Dinh dưỡng & Tiêu hóa ở động vật',
    'Sinh học 11: Trao đổi chất & Chuyển hóa năng lượng (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Sinh học 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Tiêu hóa là quá trình biến đổi các chất dinh dưỡng phức tạp trong thức ăn thành các chất đơn giản cơ thể có thể hấp thụ được.", "Hình thức tiêu hóa: Tiêu hóa nội bào (ở động vật đơn bào) -> Tiêu hóa ngoại bào trong túi tiêu hóa -> Tiêu hóa ngoại bào trong ống tiêu hóa (ở động vật có xương sống).", "Ống tiêu hóa có sự phối hợp giữa tiêu hóa cơ học (nhai, đảo) và tiêu hóa hóa học (enzyme)."]$json$::jsonb,
    $json$["Ở người, dạ dày tiêu hóa cơ học và tiêu hóa hóa học protein nhờ enzyme pepsin và acid HCl."]$json$::jsonb,
    $json$[{"id": "bio-11-th-q1", "prompt": "Động vật nào sau đây có hệ tiêu hóa dạng ống tiêu hóa?", "difficulty": "EASY", "options": [{"id": "a", "text": "Thỏ (Động vật có xương sống)"}, {"id": "b", "text": "Thủy tức"}, {"id": "c", "text": "Trùng biến hình (Đơn bào)"}], "correctOptionId": "a", "explanation": "Thỏ và động vật có xương sống có ống tiêu hóa hoàn chỉnh từ miệng đến hậu môn."}, {"id": "bio-11-th-q2", "prompt": "Ở ruột nhỏ của người, sự hấp thụ chất dinh dưỡng diễn ra hiệu quả chủ yếu nhờ?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Bề mặt niêm mạc có nhiều nếp gấp và lông hấp thụ cực lớn"}, {"id": "b", "text": "Chiều dài ruột rất ngắn"}, {"id": "c", "text": "Dạ dày tiết acid HCl"}], "correctOptionId": "a", "explanation": "Vi nhung mao và lông ruột tăng diện tích bề mặt hấp thụ lên gấp hàng trăm lần."}, {"id": "bio-11-th-q3", "prompt": "Thức ăn của động vật ăn thực vật (trâu, bò) giàu cellulose nhưng khó tiêu hóa. Chúng tiêu hóa cellulose nhờ?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hệ vi sinh vật cộng sinh trong dạ dày/manh tràng tiết enzyme cellulase"}, {"id": "b", "text": "Enzyme pepsin do dạ dày tự tiết ra"}, {"id": "c", "text": "Nhiệt độ cơ thể cao"}], "correctOptionId": "a", "explanation": "Trâu bò không tự tiết cellulase mà nhờ vi sinh vật cộng sinh phân giải cellulose."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-tuan-hoan-mau',
    4,
    'LESSON',
    'Tuần hoàn máu & Hệ tuần hoàn',
    'Sinh học 11: Trao đổi chất & Chuyển hóa năng lượng (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Sinh học 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Hệ tuần hoàn vận chuyển các chất (O2, CO2, chất dinh dưỡng, chất thải) đến và đi từ các tế bào.", "Hệ tuần hoàn hở (ở thân mềm, chân khớp): Máu tràn vào khoang cơ thể trộn lẫn dịch tế bào.", "Hệ tuần hoàn kín (ở đốt sống, đốt cao): Máu lưu thông liên tục trong mạch kín dưới áp lực cao hơn (gồm tuần hoàn đơn ở cá, tuần hoàn kép ở lưỡng cư, bò sát, chim, thú)."]$json$::jsonb,
    $json$["Người có hệ tuần hoàn kép: Vòng tuần hoàn nhỏ đưa máu lên phổi trao đổi khí, vòng tuần hoàn lớn đưa máu đi khắp cơ thể."]$json$::jsonb,
    $json$[{"id": "bio-11-thm-q1", "prompt": "Động vật nào sau đây có hệ tuần hoàn hở?", "difficulty": "EASY", "options": [{"id": "a", "text": "Châu chấu (Chân khớp)"}, {"id": "b", "text": "Cá chép"}, {"id": "c", "text": "Chim bồ câu"}], "correctOptionId": "a", "explanation": "Côn trùng/Chân khớp có hệ tuần hoàn hở máu tiếp xúc trực tiếp với tế bào trong khoang."}, {"id": "bio-11-thm-q2", "prompt": "Huyết áp là áp lực của máu tác dụng lên?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Thành mạch máu"}, {"id": "b", "text": "Tim"}, {"id": "c", "text": "Phổi"}], "correctOptionId": "a", "explanation": "Tim co bóp đẩy máu tạo áp lực đẩy vào thành mạch, gọi là huyết áp."}, {"id": "bio-11-thm-q3", "prompt": "Huyết áp giảm dần trong hệ mạch theo chiều nào sau đây?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Động mạch -> Mao mạch -> Tĩnh mạch"}, {"id": "b", "text": "Tĩnh mạch -> Mao mạch -> Động mạch"}, {"id": "c", "text": "Mao mạch -> Động mạch -> Tĩnh mạch"}], "correctOptionId": "a", "explanation": "Do ma sát với thành mạch, huyết áp cao nhất ở động mạch chủ và thấp nhất ở tĩnh mạch chủ."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'biology',
    11,
    'intro-2026.1',
    'biology-11-cam-ung-sinh-vat',
    5,
    'FIVE_LESSON_REVIEW',
    'Cảm ứng & Tập tính ở sinh vật',
    'Sinh học 11: Trao đổi chất & Chuyển hóa năng lượng (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Sinh học 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Cảm ứng là khả năng tiếp nhận kích thích và phản ứng lại các kích thích từ môi trường của sinh vật.", "Ở thực vật: Hướng động (hướng sáng, hướng trọng lực, hướng nước) và Ứng động (ứng động sinh trưởng, sức trương nước).", "Ở động vật: Phản xạ không điều kiện (bẩm sinh, di truyền) và Phản xạ có điều kiện (học tập, thành thục qua trải nghiệm)."]$json$::jsonb,
    $json$["Cây mọc hướng về phía có ánh sáng là hướng sáng dương. Chim sâu làm tổ là tập tính bẩm sinh."]$json$::jsonb,
    $json$[{"id": "bio-11-cu-q1", "prompt": "Hiện tượng ngọn cây xanh luôn vươn về phía có ánh sáng gọi là?", "difficulty": "EASY", "options": [{"id": "a", "text": "Hướng sáng dương"}, {"id": "b", "text": "Hướng trọng lực âm"}, {"id": "c", "text": "Hướng hóa âm"}], "correctOptionId": "a", "explanation": "Thân ngọn cây hướng về nguồn sáng gọi là hướng sáng dương."}, {"id": "bio-11-cu-q2", "prompt": "Tập tính bẩm sinh ở động vật có đặc điểm nào sau đây?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Sinh ra đã có, di truyền từ bố mẹ và đặc trưng cho loài"}, {"id": "b", "text": "Hình thành qua quá trình học tập trải nghiệm"}, {"id": "c", "text": "Dễ bị mất đi khi môi trường thay đổi"}], "correctOptionId": "a", "explanation": "Tập tính bẩm sinh do di truyền quy định, sinh ra đã có không cần học."}, {"id": "bio-11-cu-q3", "prompt": "Chú chó nghe tiếng chuông báo ăn nhiều lần thì tiết nước bọt ngay cả khi chưa thấy thức ăn. Đây là ví dụ về?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Phản xạ có điều kiện (Tập tính học được)"}, {"id": "b", "text": "Phản xạ không điều kiện Bẩm sinh"}, {"id": "c", "text": "Hướng động thực vật"}], "correctOptionId": "a", "explanation": "Thí nghiệm Pavlov: Sự kết hợp giữa tiếng chuông và thức ăn tạo nên phản xạ có điều kiện."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-can-bang-hoa-hoc',
    1,
    'LESSON',
    'Khái niệm về cân bằng hóa học',
    'Hóa học 11: Cân bằng hóa học & Hóa học hữu cơ (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Hóa học 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Phản ứng thuận nghịch là phản ứng diễn ra theo hai chiều ngược nhau trong cùng điều kiện.", "Cân bằng hóa học là trạng thái của phản ứng thuận nghịch khi tốc độ phản ứng thuận bằng tốc độ phản ứng nghịch (v_thuận = v_nghịch). Đây là cân bằng động.", "Hằng số cân bằng Kc = [C]^c [D]^d / ([A]^a [B]^b) chỉ phụ thuộc vào bản chất phản ứng và nhiệt độ."]$json$::jsonb,
    $json$["Phản ứng tổng hợp Ammonia: N2(k) + 3H2(k) ⇌ 2NH3(k). Hằng số Kc = [NH3]² / ([N2] * [H2]³)."]$json$::jsonb,
    $json$[{"id": "chem-11-cb-q1", "prompt": "Trạng thái cân bằng hóa học của phản ứng thuận nghịch là trạng thái khi?", "difficulty": "EASY", "options": [{"id": "a", "text": "Tốc độ phản ứng thuận bằng tốc độ phản ứng nghịch"}, {"id": "b", "text": "Phản ứng dừng lại hoàn toàn"}, {"id": "c", "text": "Nồng độ chất tham gia bằng 0"}], "correctOptionId": "a", "explanation": "Cân bằng hóa học là cân bằng động, tại đó v_thuận = v_nghịch."}, {"id": "chem-11-cb-q2", "prompt": "Hằng số cân bằng Kc của một phản ứng thuận nghịch phụ thuộc vào yếu tố nào?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Bản chất phản ứng và nhiệt độ"}, {"id": "b", "text": "Nồng độ các chất ban đầu"}, {"id": "c", "text": "Áp suất chung của hệ"}], "correctOptionId": "a", "explanation": "Kc chỉ thay đổi khi nhiệt độ thay đổi đối với một phản ứng xác định."}, {"id": "chem-11-cb-q3", "prompt": "Theo nguyên lý Le Chatelier, khi tăng áp suất hệ cân bằng N2(k) + 3H2(k) ⇌ 2NH3(k) thì cân bằng chuyển dịch theo chiều nào?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Chiều thuận (chiều giảm số phân tử khí)"}, {"id": "b", "text": "Chiều nghịch"}, {"id": "c", "text": "Không chuyển dịch"}], "correctOptionId": "a", "explanation": "Tăng áp suất làm cân bằng chuyển dịch theo chiều giảm số phân tử khí (4 mol khí -> 2 mol khí, tức chiều thuận)."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-su-dien-li-ph',
    2,
    'LESSON',
    'Cân bằng trong dung dịch nước & Thuyết Brønsted-Lowry',
    'Hóa học 11: Cân bằng hóa học & Hóa học hữu cơ (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Hóa học 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Sự điện li là quá trình phân li các chất trong nước ra ion. Chất điện li mạnh phân li hoàn toàn; chất điện li yếu phân li một phần.", "Theo thuyết Brønsted-Lowry: Acid là chất cho proton H+; Base là chất nhận proton H+.", "pH = -log[H+]. Dung dịch trung tính có pH = 7, dung dịch acid có pH < 7, dung dịch base có pH > 7."]$json$::jsonb,
    $json$["Dung dịch HCl 0,01 M có [H+] = 10^-2 M nên pH = 2 (dung dịch tính acid mạnh)."]$json$::jsonb,
    $json$[{"id": "chem-11-ph-q1", "prompt": "Dung dịch nào sau đây có pH > 7 ở nhiệt độ 25°C?", "difficulty": "EASY", "options": [{"id": "a", "text": "Dung dịch NaOH (base)"}, {"id": "b", "text": "Dung dịch HCl (acid)"}, {"id": "c", "text": "Nước tinh khiết"}], "correctOptionId": "a", "explanation": "Dung dịch base có [OH-] > [H+] nên pH > 7."}, {"id": "chem-11-ph-q2", "prompt": "Theo thuyết Brønsted-Lowry, chất nào đóng vai trò là Acid?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Chất có khả năng cho H+"}, {"id": "b", "text": "Chất có khả năng nhận H+"}, {"id": "c", "text": "Chất phân li ra OH-"}], "correctOptionId": "a", "explanation": "Định nghĩa Brønsted-Lowry: Acid là cho H+, Base là nhận H+."}, {"id": "chem-11-ph-q3", "prompt": "Tính pH của dung dịch HNO3 0,001 M (phân li hoàn toàn).", "difficulty": "APPLIED", "options": [{"id": "a", "text": "pH = 3"}, {"id": "b", "text": "pH = 1"}, {"id": "c", "text": "pH = 11"}], "correctOptionId": "a", "explanation": "[H+] = 0,001 M = 10^-3 M => pH = -log(10^-3) = 3."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-nitrogen-ammonia',
    3,
    'LESSON',
    'Nitrogen và Hợp chất Ammonia',
    'Hóa học 11: Cân bằng hóa học & Hóa học hữu cơ (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Hóa học 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Nitrogen (N2) có liên kết ba N≡N rất bền nên ở nhiệt độ thường khá kém hoạt động.", "Ammonia (NH3) là khí không màu, mùi khai, tan nhiều trong nước tạo dung dịch có tính base yếu.", "NH3 có tính khử mạnh do số oxi hóa của N là -3."]$json$::jsonb,
    $json$["NH3 phản ứng với O2 khi có xúc tác Pt tạo NO và H2O."]$json$::jsonb,
    $json$[{"id": "chem-11-nh3-q1", "prompt": "Khí Ammonia (NH3) làm quỳ tím ẩm chuyển sang màu gì?", "difficulty": "EASY", "options": [{"id": "a", "text": "Xanh"}, {"id": "b", "text": "Đỏ"}, {"id": "c", "text": "Mất màu"}], "correctOptionId": "a", "explanation": "NH3 tan trong nước tạo ion OH- làm dung dịch có tính base yếu, làm quỳ tím hóa xanh."}, {"id": "chem-11-nh3-q2", "prompt": "Ở điều kiện thường, phân tử N2 khá inert (trơ về mặt hóa học) chủ yếu do?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Liên kết ba N≡N giữa 2 nguyên tử N rất bền"}, {"id": "b", "text": "Phân tử N2 có bán kính lớn"}, {"id": "c", "text": "Nitrogen là phi kim yếu"}], "correctOptionId": "a", "explanation": "Năng lượng liên kết ba N≡N lớn (945 kJ/mol) làm N2 trơ ở nhiệt độ thường."}, {"id": "chem-11-nh3-q3", "prompt": "Hiện tượng khói trắng xuất hiện khi đưa hai đầu đũa thủy tinh nhúng dung dịch NH3 đặc và dung dịch HCl đặc lại gần nhau là do tạo thành?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Tinh thể muối NH4Cl"}, {"id": "b", "text": "Khí N2"}, {"id": "c", "text": "Khí Cl2"}], "correctOptionId": "a", "explanation": "NH3(k) + HCl(k) -> NH4Cl(r, khói trắng tinh thể)."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-dai-cuong-huu-co',
    4,
    'LESSON',
    'Đại cương về Hóa học hữu cơ',
    'Hóa học 11: Cân bằng hóa học & Hóa học hữu cơ (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Hóa học 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Hợp chất hữu cơ là hợp chất của Carbon (trừ CO, CO2, muối carbonate, cyanide,...).", "Liên kết hóa học chủ yếu trong hợp chất hữu cơ là liên kết cộng hóa trị.", "Đồng đẳng là các chất có cấu tạo hóa học tương tự nhau nhưng hơn kém nhau một hay nhiều nhóm -CH2-. Đồng phân là các chất có cùng công thức phân tử nhưng công thức cấu tạo khác nhau."]$json$::jsonb,
    $json$["CH4 và C2H6 là đồng đẳng của nhau. CH3-CH2-OH và CH3-O-CH3 là hai đồng phân có cùng CTPT C2H6O."]$json$::jsonb,
    $json$[{"id": "chem-11-hc-q1", "prompt": "Hợp chất nào sau đây thuộc loại hợp chất hữu cơ?", "difficulty": "EASY", "options": [{"id": "a", "text": "CH4 (Methane)"}, {"id": "b", "text": "CO2"}, {"id": "c", "text": "CaCO3"}], "correctOptionId": "a", "explanation": "CH4 là hydrocarbon hữu cơ đơn giản nhất; CO2 và CaCO3 là hợp chất vô cơ."}, {"id": "chem-11-hc-q2", "prompt": "Các chất có cùng công thức phân tử nhưng công thức cấu tạo khác nhau được gọi là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Đồng phân của nhau"}, {"id": "b", "text": "Đồng đẳng của nhau"}, {"id": "c", "text": "Đồng vị của nhau"}], "correctOptionId": "a", "explanation": "Đồng phân là cùng CTPT, khác CTCT làm tính chất khác nhau."}, {"id": "chem-11-hc-q3", "prompt": "Phân tích nguyên tố hợp chất A thu được 85,7% C và 14,3% H về khối lượng. Công thức đơn giản nhất của A là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "CH2"}, {"id": "b", "text": "CH4"}, {"id": "c", "text": "C2H5"}], "correctOptionId": "a", "explanation": "nC : nH = (85,7/12) : (14,3/1) = 7,14 : 14,3 ≈ 1 : 2 => CTĐGN là CH2."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'chemistry',
    11,
    'intro-2026.1',
    'chemistry-11-alkane-alkene',
    5,
    'FIVE_LESSON_REVIEW',
    'Hydrocarbon no (Alkane) & Không no (Alkene)',
    'Hóa học 11: Cân bằng hóa học & Hóa học hữu cơ (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Hóa học 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Alkane là hydrocarbon no, mạch hở, CTPT chung C_n H_(2n+2) (n ≥ 1). Phản ứng đặc trưng là phản ứng thế halogen.", "Alkene là hydrocarbon không no có 1 liên kết đôi C=C, CTPT C_n H_2n (n ≥ 2). Phản ứng đặc trưng là phản ứng cộng (cộng H2, Br2, H2O) và làm mất màu dung dịch Bromine."]$json$::jsonb,
    $json$["CH2=CH2 + Br2 (dung dịch da cam) -> CH2Br-CH2Br (không màu)."]$json$::jsonb,
    $json$[{"id": "chem-11-alk-q1", "prompt": "Công thức chung của dãy đồng đẳng Alkane (hydrocarbon no mạch hở) là?", "difficulty": "EASY", "options": [{"id": "a", "text": "C_n H_(2n+2) (n ≥ 1)"}, {"id": "b", "text": "C_n H_2n (n ≥ 2)"}, {"id": "c", "text": "C_n H_(2n-2) (n ≥ 2)"}], "correctOptionId": "a", "explanation": "Alkane có công thức tổng quát C_n H_(2n+2)."}, {"id": "chem-11-alk-q2", "prompt": "Thử nghiệm dùng để phân biệt Alkene (CH2=CH2) và Alkane (CH4) là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Sục qua dung dịch Bromine (làm mất màu đun nóng/điều kiện thường)"}, {"id": "b", "text": "Thử quỳ tím"}, {"id": "c", "text": "Cho qua dung dịch NaCl"}], "correctOptionId": "a", "explanation": "Alkene có liên kết đôi C=C làm mất màu dung dịch Bromine ngay ở điều kiện thường."}, {"id": "chem-11-alk-q3", "prompt": "Khi cho Propene (CH3-CH=CH2) cộng HBr theo quy tắc Markovnikov, sản phẩm chính thu được là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "2-bromopropane (CH3-CHBr-CH3)"}, {"id": "b", "text": "1-bromopropane (CH3-CH2-CH2Br)"}, {"id": "c", "text": "Propane"}], "correctOptionId": "a", "explanation": "Quy tắc Markovnikov: H ưu tiên cộng vào C mang nhiều H hơn (C1), Br vào C bậc cao hơn (C2)."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-thi-truong-lao-dong',
    1,
    'LESSON',
    'Thị trường lao động và Việc làm',
    'GDKT & PL 11: Thị trường lao động & Quyền công dân (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Giáo dục Kinh tế & Pháp luật 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Thị trường lao động là nơi diễn ra hoạt động mua bán sức lao động giữa người lao động (cung) và người sử dụng lao động (cầu).", "Tiền lương là giá cả của sức lao động, do hai bên thỏa thuận phù hợp với quy định pháp luật.", "Nâng cao trình độ chuyên môn, kỹ năng mềm và kỷ luật lao động giúp người lao động tăng cơ hội việc làm và thu nhập."]$json$::jsonb,
    $json$["Sự xuất hiện của trí tuệ nhân tạo (AI) đặt ra yêu cầu người lao động phải không ngừng học tập suốt đời để không bị thay thế."]$json$::jsonb,
    $json$[{"id": "eco-11-ld-q1", "prompt": "Trong thị trường lao động, bên cung ứng sức lao động là đối tượng nào?", "difficulty": "EASY", "options": [{"id": "a", "text": "Người lao động"}, {"id": "b", "text": "Doanh nghiệp tuyển dụng"}, {"id": "c", "text": "Nhà nước"}], "correctOptionId": "a", "explanation": "Người lao động cung ứng sức lao động của bản thân để nhận tiền lương."}, {"id": "eco-11-ld-q2", "prompt": "Yếu tố quyết định giá cả sức lao động (tiền lương) trên thị trường là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Quan hệ cung - cầu lao động và chất lượng trình độ sức lao động"}, {"id": "b", "text": "Chỉ do người sử dụng lao động đơn phương áp đặt"}, {"id": "c", "text": "Sự may mắn"}], "correctOptionId": "a", "explanation": "Tiền lương phụ thuộc vào chất lượng lao động, năng suất và quy luật cung cầu."}, {"id": "eco-11-ld-q3", "prompt": "Giải pháp quan trọng nhất đối với học sinh trung học phổ thông để chuẩn bị gia nhập thị trường lao động tương lai là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Chủ động học tập kiến thức, rèn luyện kỹ năng mềm và hướng nghiệp phù hợp"}, {"id": "b", "text": "Chờ đợi sự sắp xếp của gia đình"}, {"id": "c", "text": "Không cần rèn luyện thêm"}], "correctOptionId": "a", "explanation": "Tự trang bị trình độ và kỹ năng tạo thế chủ động cho học sinh thích ứng thị trường."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-lam-phat-that-nghiep',
    2,
    'LESSON',
    'Lạm phát và Thất nghiệp trong nền kinh tế',
    'GDKT & PL 11: Thị trường lao động & Quyền công dân (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Giáo dục Kinh tế & Pháp luật 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Lạm phát là sự tăng mức giá chung một cách liên tục của hàng hóa và dịch vụ trong nền kinh tế theo thời gian, làm giảm sức mua của đồng tiền.", "Thất nghiệp là tình trạng người trong độ tuổi lao động, có khả năng lao động, đang tìm kiếm việc làm nhưng chưa có việc làm.", "Kiểm soát lạm phát và giảm thất nghiệp là mục tiêu quản lý vĩ mô quan trọng của Nhà nước."]$json$::jsonb,
    $json$["Khi lạm phát cao, giá cả nhu yếu phẩm tăng vọt khiến đời sống người lao động gặp khó khăn."]$json$::jsonb,
    $json$[{"id": "eco-11-lp-q1", "prompt": "Hiện tượng lạm phát trong nền kinh tế dẫn đến hậu quả trực tiếp nào?", "difficulty": "EASY", "options": [{"id": "a", "text": "Giảm sức mua của đồng tiền"}, {"id": "b", "text": "Tăng giá trị đồng tiền"}, {"id": "c", "text": "Hàng hóa rẻ đi"}], "correctOptionId": "a", "explanation": "Lạm phát làm đồng tiền mất giá, cùng một lượng tiền mua được ít hàng hóa hơn."}, {"id": "eco-11-lp-q2", "prompt": "Thất nghiệp cơ cấu xảy ra do nguyên nhân nào sau đây?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Sự mất cân đối giữa kỹ năng của người lao động và yêu cầu công việc mới"}, {"id": "b", "text": "Người lao động lười biếng"}, {"id": "c", "text": "Thời tiết thay đổi theo mùa"}], "correctOptionId": "a", "explanation": "Thất nghiệp cơ cấu do sự thay đổi công nghệ/cơ cấu kinh tế làm kỹ năng cũ không còn phù hợp."}, {"id": "eco-11-lp-q3", "prompt": "Biện pháp tài chính nào giúp Nhà nước kiềm chế lạm phát khi nền kinh tế tăng nóng?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Thắt chặt tiền tệ và giảm chi tiêu công"}, {"id": "b", "text": "In thêm nhiều tiền giấy"}, {"id": "c", "text": "Tăng mạnh chi tiêu ngân sách"}], "correctOptionId": "a", "explanation": "Thắt chặt tiền tệ giảm lượng tiền lưu thông giúp hạ nhiệt lạm phát."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-binh-dang-phap-luat',
    3,
    'LESSON',
    'Quyền bình đẳng của công dân trước pháp luật',
    'GDKT & PL 11: Thị trường lao động & Quyền công dân (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Giáo dục Kinh tế & Pháp luật 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Quyền bình đẳng trước pháp luật: Mọi công dân không phân biệt nam nữ, dân tộc, tín ngưỡng, thành phần xã hội đều bình đẳng về quyền và nghĩa vụ.", "Mọi hành vi vi phạm pháp luật đều bị xử lý nghiêm minh theo quy định, không có ngoại lệ.", "Bình đẳng giới: Nam, nữ bình đẳng trong các lĩnh vực chính trị, kinh tế, văn hóa, xã hội và gia đình."]$json$::jsonb,
    $json$["Công dân đủ 18 tuổi đều có quyền bầu cử Quốc hội không phân biệt giàu nghèo hay nam nữ."]$json$::jsonb,
    $json$[{"id": "eco-11-bd-q1", "prompt": "Quyền bình đẳng trước pháp luật có nghĩa là gì?", "difficulty": "EASY", "options": [{"id": "a", "text": "Mọi công dân đều bình đẳng về quyền, nghĩa vụ và trách nhiệm pháp lý"}, {"id": "b", "text": "Mọi người có thu nhập bằng nhau"}, {"id": "c", "text": "Người giàu không phải nộp thuế"}], "correctOptionId": "a", "explanation": "Bình đẳng trước pháp luật không phân biệt địa vị, thành phần hay tôn giáo."}, {"id": "eco-11-bd-q2", "prompt": "Bình đẳng về trách nhiệm pháp lý nghĩa là bất kỳ ai vi phạm pháp luật đều sẽ?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Bị xử lý nghiêm minh theo quy định pháp luật"}, {"id": "b", "text": "Được miễn trách nhiệm nếu có địa vị cao"}, {"id": "c", "text": "Tự thỏa thuận không cần pháp luật"}], "correctOptionId": "a", "explanation": "Pháp luật nghiêm minh áp dụng chung không có vùng cấm."}, {"id": "eco-11-bd-q3", "prompt": "Việc Nhà nước ưu tiên cộng điểm thi đại học cho học sinh vùng sâu vùng xa thể hiện chính sách gì?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Bình đẳng về cơ hội phát triển và hỗ trợ vùng khó khăn"}, {"id": "b", "text": "Bất bình đẳng xã hội"}, {"id": "c", "text": "Sự thiên vị bất hợp lý"}], "correctOptionId": "a", "explanation": "Chính sách ưu tiên giúp thu hẹp khoảng cách điều kiện, tạo bình đẳng cơ hội thực sự."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-quyen-tu-do-co-ban',
    4,
    'LESSON',
    'Quyền tự do cơ bản của công dân',
    'GDKT & PL 11: Thị trường lao động & Quyền công dân (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Giáo dục Kinh tế & Pháp luật 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Các quyền tự do cơ bản: Quyền bất khả xâm phạm về thân thể; Quyền được pháp luật bảo hộ về tính mạng, sức khỏe, danh dự và nhân phẩm; Quyền bất khả xâm phạm về chỗ ở; Quyền bảo đảm an toàn và bí mật thư tín, điện thoại, điện văn.", "Không ai bị bắt nếu không có quyết định của Tòa án hoặc phê chuẩn của Viện kiểm sát (trừ trường hợp phạm tội quả đắng)."]$json$::jsonb,
    $json$["Tự ý khám xét nhà người khác khi chưa có lệnh của cơ quan có thẩm quyền là vi phạm quyền bất khả xâm phạm về chỗ ở."]$json$::jsonb,
    $json$[{"id": "eco-11-td-q1", "prompt": "Công dân có quyền bất khả xâm phạm về thân thể có nghĩa là không ai bị bắt nếu không có quyết định của?", "difficulty": "EASY", "options": [{"id": "a", "text": "Tòa án hoặc phê chuẩn của Viện kiểm sát (trừ phạm tội quả đắng)"}, {"id": "b", "text": "Hàng xóm"}, {"id": "c", "text": "Bất kỳ cá nhân nào"}], "correctOptionId": "a", "explanation": "Quyền bất khả xâm phạm thân thể được bảo vệ nghiêm ngặt bởi Hiến pháp."}, {"id": "eco-11-td-q2", "prompt": "Hành vi tự ý đọc tin nhắn, mở thư của người khác khi không được phép là vi phạm quyền nào?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Quyền được bảo đảm an toàn và bí mật thư tín, điện thoại"}, {"id": "b", "text": "Quyền tự do ngôn luận"}, {"id": "c", "text": "Quyền bầu cử"}], "correctOptionId": "a", "explanation": "Thư tín, điện thoại, tin nhắn riêng tư được pháp luật bảo đảm bí mật."}, {"id": "eco-11-td-q3", "prompt": "Trường hợp nào sau đây cơ quan chức năng được phép bắt người mà không cần lệnh trước?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Người đang thực hiện hành vi phạm tội quả đắng hoặc đang bị truy nã"}, {"id": "b", "text": "Người đang nghi ngờ chung chung"}, {"id": "c", "text": "Người tranh chấp dân sự"}], "correctOptionId": "a", "explanation": "Trường hợp phạm tội quả đắng hoặc truy nã cho phép bắt khẩn cấp để ngăn chặn hậu quả."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'economics_law',
    11,
    'intro-2026.1',
    'economics_law-11-nghia-vu-cong-dan',
    5,
    'FIVE_LESSON_REVIEW',
    'Nghĩa vụ cơ bản của công dân & Bảo vệ Tổ quốc',
    'GDKT & PL 11: Thị trường lao động & Quyền công dân (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Giáo dục Kinh tế & Pháp luật 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Bảo vệ Tổ quốc là nghĩa vụ thiêng liêng và quyền cao quý của công dân.", "Nghĩa vụ công dân: Tuân theo Hiến pháp và pháp luật; Nộp thuế theo quy định; Tham gia nghĩa vụ quân sự và bảo vệ an ninh quốc gia.", "Mọi công dân nam trong độ tuổi quy định có nghĩa vụ thực hiện nghĩa vụ quân sự."]$json$::jsonb,
    $json$["Thanh niên đủ 18 tuổi hăng hái tham gia khám tuyển nghĩa vụ quân sự thể hiện ý thức trách nhiệm bảo vệ Tổ quốc."]$json$::jsonb,
    $json$[{"id": "eco-11-nv-q1", "prompt": "Nghĩa vụ thiêng liêng và quyền cao quý của mọi công dân Việt Nam là?", "difficulty": "EASY", "options": [{"id": "a", "text": "Bảo vệ Tổ quốc"}, {"id": "b", "text": "Đi du lịch nước ngoài"}, {"id": "c", "text": "Kinh doanh tự do"}], "correctOptionId": "a", "explanation": "Hiến pháp quy định Bảo vệ Tổ quốc là nghĩa vụ thiêng liêng cao quý."}, {"id": "eco-11-nv-q2", "prompt": "Trốn tránh thực hiện nghĩa vụ quân sự sẽ bị xử lý như thế nào?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Bị xử phạt hành chính hoặc truy cứu trách nhiệm hình sự"}, {"id": "b", "text": "Không bị xử lý gì"}, {"id": "c", "text": "Chỉ bị nhắc nhở"}], "correctOptionId": "a", "explanation": "Trốn nghĩa vụ quân sự vi phạm Luật Nghĩa vụ quân sự và bị xử lý nghiêm."}, {"id": "eco-11-nv-q3", "prompt": "Học sinh THPT thể hiện trách nhiệm bảo vệ an ninh mạng bằng hành động nào sau đây?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Không chia sẻ tin giả, xấu độc và tuân thủ Luật An ninh mạng"}, {"id": "b", "text": "Lan truyền các thông tin chưa kiểm chứng"}, {"id": "c", "text": "Tấn công mạng các website"}], "correctOptionId": "a", "explanation": "Sử dụng mạng xã hội văn minh, tôn trọng Luật An ninh mạng là trách nhiệm của mỗi học sinh."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-global-warming',
    1,
    'LESSON',
    'Global Warming & Present Perfect Continuous',
    'Tiếng Anh 11: Global Success & Lifelong Learning (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Tiếng Anh 11 Global Success - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Global warming is caused by greenhouse gas emissions trapping heat in the Earth's atmosphere.", "Present Perfect Continuous: S + have/has + been + V-ing.", "Usage: Emphasizes the duration or ongoing nature of an action that started in the past and continues to the present, or has just stopped with present results."]$json$::jsonb,
    $json$["Scientists have been studying climate change for decades.", "They have been cutting down forests, which increases carbon dioxide levels."]$json$::jsonb,
    $json$[{"id": "eng-11-gw-q1", "prompt": "Which grammar form is the Present Perfect Continuous tense?", "difficulty": "EASY", "options": [{"id": "a", "text": "have/has + been + V-ing"}, {"id": "b", "text": "had + V3/ed"}, {"id": "c", "text": "am/is/are + V-ing"}], "correctOptionId": "a", "explanation": "Present Perfect Continuous uses have/has + been + V-ing."}, {"id": "eng-11-gw-q2", "prompt": "Choose the correct option: The temperature ____ rising steadily for the past few years.", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "has been"}, {"id": "b", "text": "is"}, {"id": "c", "text": "was"}], "correctOptionId": "a", "explanation": "'For the past few years' indicates an ongoing action starting in the past -> has been rising."}, {"id": "eng-11-gw-q3", "prompt": "Which of the following actions directly contributes to deforestation?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Cutting down trees for timber and agriculture"}, {"id": "b", "text": "Planting new saplings in urban parks"}, {"id": "c", "text": "Recycling plastic bottles"}], "correctOptionId": "a", "explanation": "Deforestation means clearing forests or cutting down trees on a large scale."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-independent-life',
    2,
    'LESSON',
    'Independent Life & To-Infinitive Clauses',
    'Tiếng Anh 11: Global Success & Lifelong Learning (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Tiếng Anh 11 Global Success - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["To-infinitive clauses (to + V) can be used after certain adjectives (it is important to do...) and nouns (the decision to leave...).", "Key skills for teenager independence: time management, self-care, decision making, and problem solving."]$json$::jsonb,
    $json$["It is necessary to learn how to cook for yourself.", "She has the ability to manage her own budget wisely."]$json$::jsonb,
    $json$[{"id": "eng-11-ind-q1", "prompt": "Complete the sentence: It is important for teenagers ____ time management skills.", "difficulty": "EASY", "options": [{"id": "a", "text": "to learn"}, {"id": "b", "text": "learning"}, {"id": "c", "text": "learned"}], "correctOptionId": "a", "explanation": "Structure: It is + adj + (for sb) + to-V."}, {"id": "eng-11-ind-q2", "prompt": "Which adjective means 'able to rely on oneself without needing help from others'?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Independent"}, {"id": "b", "text": "Dependent"}, {"id": "c", "text": "Helpless"}], "correctOptionId": "a", "explanation": "Independent means self-reliant and autonomous."}, {"id": "eng-11-ind-q3", "prompt": "Choose the correct sentence combining: She made a plan. She wanted to complete the project on time.", "difficulty": "APPLIED", "options": [{"id": "a", "text": "She made a plan to complete the project on time."}, {"id": "b", "text": "She made a plan completing the project on time."}, {"id": "c", "text": "She made a plan completed the project on time."}], "correctOptionId": "a", "explanation": "Use a to-infinitive clause to express purpose or modify the noun 'plan'."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-asean-vietnam',
    3,
    'LESSON',
    'ASEAN Community & Gerunds / Participle Clauses',
    'Tiếng Anh 11: Global Success & Lifelong Learning (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Tiếng Anh 11 Global Success - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["ASEAN (Association of Southeast Asian Nations) promotes economic growth, cultural exchange, and regional stability.", "Gerunds (V-ing) function as nouns (subject, object of preposition).", "Participle clauses (V-ing / V-ed) reduce relative clauses to make sentences concise."]$json$::jsonb,
    $json$["Joining ASEAN in 1995 helped Viet Nam integrate into the regional economy.", "Countries located in Southeast Asia cooperate closely."]$json$::jsonb,
    $json$[{"id": "eng-11-asean-q1", "prompt": "Viet Nam became an official member of ASEAN in which year?", "difficulty": "EASY", "options": [{"id": "a", "text": "1995"}, {"id": "b", "text": "1967"}, {"id": "c", "text": "2007"}], "correctOptionId": "a", "explanation": "Viet Nam joined ASEAN on July 28, 1995."}, {"id": "eng-11-asean-q2", "prompt": "Choose the correct gerund form: ____ in community activities helps students build teamwork skills.", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Participating"}, {"id": "b", "text": "Participate"}, {"id": "c", "text": "Participated"}], "correctOptionId": "a", "explanation": "Gerund (V-ing) acts as the subject of the sentence."}, {"id": "eng-11-asean-q3", "prompt": "Reduce the clause: 'Students who study abroad gain cultural experience.' -> 'Students ____ abroad gain cultural experience.'", "difficulty": "APPLIED", "options": [{"id": "a", "text": "studying"}, {"id": "b", "text": "studied"}, {"id": "c", "text": "to study"}], "correctOptionId": "a", "explanation": "Active relative clause reduces to Present Participle (V-ing)."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-preserving-heritage',
    4,
    'LESSON',
    'Preserving Heritage Sites & Cleft Sentences',
    'Tiếng Anh 11: Global Success & Lifelong Learning (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Tiếng Anh 11 Global Success - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Cleft sentences (It is/was ... that ...) are used to emphasize a specific part of a sentence (subject, object, or adverbial).", "Heritage preservation protects cultural landscapes, ancient monuments, and intangible traditions for future generations."]$json$::jsonb,
    $json$["It was in Ha Long Bay that we saw beautiful limestone islands.", "It is youth enthusiasm that helps promote local traditions."]$json$::jsonb,
    $json$[{"id": "eng-11-her-q1", "prompt": "Which structure represents a Cleft Sentence used for emphasis?", "difficulty": "EASY", "options": [{"id": "a", "text": "It is / was + [emphasized part] + that + [rest of sentence]"}, {"id": "b", "text": "Although + S + V"}, {"id": "c", "text": "If + S + V"}], "correctOptionId": "a", "explanation": "Cleft sentence structure: It is/was ... that ..."}, {"id": "eng-11-her-q2", "prompt": "Emphasize the subject: 'The local government restored the ancient citadel.'", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "It was the local government that restored the ancient citadel."}, {"id": "b", "text": "The ancient citadel was restored by local government."}, {"id": "c", "text": "Local government had restored ancient citadel."}], "correctOptionId": "a", "explanation": "It was + [the local government] + that + restored..."}, {"id": "eng-11-her-q3", "prompt": "Intangible cultural heritage includes which of the following?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Folk music, traditional festivals, and craftsmanship"}, {"id": "b", "text": "Stone temples and brick towers"}, {"id": "c", "text": "Ancient tombs and castles"}], "correctOptionId": "a", "explanation": "Intangible heritage consists of non-physical traditions and living expressions."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'english',
    11,
    'intro-2026.1',
    'english-11-education-pathways',
    5,
    'FIVE_LESSON_REVIEW',
    'Education Pathways & Vocational Training',
    'Tiếng Anh 11: Global Success & Lifelong Learning (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Tiếng Anh 11 Global Success - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["High school graduates can choose higher education (universities/colleges) or vocational training pathways.", "Key vocabulary: higher education, vocational school, qualification, tuition fee, career prospects."]$json$::jsonb,
    $json$["Vocational education provides practical hands-on skills for immediate employment."]$json$::jsonb,
    $json$[{"id": "eng-11-edu-q1", "prompt": "Which term refers to education that prepares students for a specific skilled trade or job?", "difficulty": "EASY", "options": [{"id": "a", "text": "Vocational education"}, {"id": "b", "text": "Primary education"}, {"id": "c", "text": "Kindergarten"}], "correctOptionId": "a", "explanation": "Vocational training focuses on practical skills for specific careers."}, {"id": "eng-11-edu-q2", "prompt": "Choose the word with different stress pattern: university, qualification, degree, graduation.", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "degree (stress on 2nd syllable)"}, {"id": "b", "text": "university (stress on 3rd)"}, {"id": "c", "text": "graduation (stress on 3rd)"}], "correctOptionId": "a", "explanation": "degree /dɪˈɡriː/ is stressed on the 2nd syllable, others on 3rd/4th."}, {"id": "eng-11-edu-q3", "prompt": "Complete: Many students apply for ____ to help pay their university tuition fees.", "difficulty": "APPLIED", "options": [{"id": "a", "text": "scholarships"}, {"id": "b", "text": "fines"}, {"id": "c", "text": "taxes"}], "correctOptionId": "a", "explanation": "Scholarships provide financial aid for study."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-toan-cau-hoa',
    1,
    'LESSON',
    'Toàn cầu hóa & Khu vực hóa kinh tế',
    'Địa lý 11: Địa lý kinh tế - xã hội thế giới & Khu vực (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Địa lý 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Toàn cầu hóa kinh tế là quá trình liên kết các nền kinh tế trên thế giới về thương mại, đầu tư, tài chính và công nghệ.", "Khu vực hóa là sự liên kết kinh tế giữa các quốc gia có nét tương đồng về địa lý, văn hóa hoặc mục tiêu phát triển.", "Các tổ chức khu vực lớn: Liên minh Châu Âu (EU), Hiệp hội các quốc gia Đông Nam Á (ASEAN), Diễn đàn Hợp tác Kinh tế Châu Á - Thái Bình Dương (APEC)."]$json$::jsonb,
    $json$["Sự lưu thông tự do của hàng hóa, vốn và lao động trong thị trường chung EU."]$json$::jsonb,
    $json$[{"id": "geo-11-tch-q1", "prompt": "Biểu hiện rõ nhất của xu hướng Toàn cầu hóa kinh tế là?", "difficulty": "EASY", "options": [{"id": "a", "text": "Thương mại quốc tế và dòng vốn đầu tư toàn cầu tăng nhanh"}, {"id": "b", "text": "Đóng cửa thị trường nội địa"}, {"id": "c", "text": "Tăng thuế quan nhập khẩu"}], "correctOptionId": "a", "explanation": "Thương mại toàn cầu bùng nổ là động lực của toàn cầu hóa."}, {"id": "geo-11-tch-q2", "prompt": "Tổ chức liên kết kinh tế khu vực nào có mức độ hợp tác và nhất thể hóa cao nhất thế giới?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Liên minh Châu Âu (EU)"}, {"id": "b", "text": "APEC"}, {"id": "c", "text": "NAFTA"}], "correctOptionId": "a", "explanation": "EU có chính sách chung, thị trường chung và đồng tiền chung Euro."}, {"id": "geo-11-tch-q3", "prompt": "Cơ hội lớn nhất của các nước đang phát triển khi hội nhập toàn cầu hóa là gì?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Thu hút vốn đầu tư nước ngoài (FDI) và tiếp thu công nghệ hiện đại"}, {"id": "b", "text": "Không gặp bất kỳ cạnh tranh nào"}, {"id": "c", "text": "Tự túc hoàn toàn nền kinh tế"}], "correctOptionId": "a", "explanation": "Toàn cầu hóa giúp các nước đang phát triển tiếp cận nguồn lực và công nghệ toàn cầu."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-my-latinh',
    2,
    'LESSON',
    'Khu vực Mỹ Latinh: Tự nhiên, Dân cư & Kinh tế',
    'Địa lý 11: Địa lý kinh tế - xã hội thế giới & Khu vực (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Địa lý 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Mỹ Latinh giàu tài nguyên khoáng sản, rừng Amazon và đất đai phì nhiêu.", "Thách thức: Đô thị hóa tự phát (đô thị hóa không gắn liền với công nghiệp hóa), phân hóa giàu nghèo sâu sắc và nợ nước ngoài cao.", "Kinh tế đang chuyển dịch theo hướng công nghiệp hóa và hiện đại hóa."]$json$::jsonb,
    $json$["Brazil và Mexico là hai nền kinh tế lớn nhất khu vực Mỹ Latinh."]$json$::jsonb,
    $json$[{"id": "geo-11-mlt-q1", "prompt": "Đặc điểm đô thị hóa nổi bật tại khu vực Mỹ Latinh là?", "difficulty": "EASY", "options": [{"id": "a", "text": "Tỷ lệ dân đô thị cao nhưng mang tính tự phát"}, {"id": "b", "text": "Tỷ lệ dân đô thị rất thấp"}, {"id": "c", "text": "Phát triển đồng đều ở nông thôn"}], "correctOptionId": "a", "explanation": "Dân nghèo di cư vào thành phố tạo nên hiện tượng đô thị hóa tự phát."}, {"id": "geo-11-mlt-q2", "prompt": "Loại tài nguyên thiên nhiên nổi tiếng giàu có ở Mỹ Latinh là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Khoáng sản kim loại và Dầu mỏ"}, {"id": "b", "text": "Băng tuyết"}, {"id": "c", "text": "Hoang mạc cát"}], "correctOptionId": "a", "explanation": "Mỹ Latinh nổi tiếng về trữ lượng dầu mỏ, quặng sắt, đồng, bạc."}, {"id": "geo-11-mlt-q3", "prompt": "Giải pháp quan trọng để Mỹ Latinh phát triển kinh tế bền vững là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Cải cách kinh tế, thu hút đầu tư và bảo vệ rừng Amazon"}, {"id": "b", "text": "Khai thác cạn kiệt rừng mưa Amazon"}, {"id": "c", "text": "Ngừng giao thương quốc tế"}], "correctOptionId": "a", "explanation": "Bảo vệ rừng Amazon kết hợp cải cách kinh tế đảm bảo phát triển bền vững."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-lien-minh-chau-au',
    3,
    'LESSON',
    'Liên minh Châu Âu (EU)',
    'Địa lý 11: Địa lý kinh tế - xã hội thế giới & Khu vực (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Địa lý 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["EU là trung tâm kinh tế, thương mại và tài chính hàng đầu thế giới.", "Thị trường chung EU tự do lưu thông 4 mặt: Hàng hóa, Dịch vụ, Con người và Tiền vốn.", "Đồng tiền chung Euro giúp giảm chi phí giao dịch và tăng cường liên kết kinh tế."]$json$::jsonb,
    $json$["Người dân các nước EU có thể đi lại, làm việc và sinh sống tự do giữa các quốc gia thành viên."]$json$::jsonb,
    $json$[{"id": "geo-11-eu-q1", "prompt": "Thị trường chung Châu Âu (EU) cho phép tự do lưu thông 4 yếu tố nào?", "difficulty": "EASY", "options": [{"id": "a", "text": "Hàng hóa, Dịch vụ, Con người và Tiền vốn"}, {"id": "b", "text": "Vũ khí, Hàng hóa, Rác thải và Đất đai"}, {"id": "c", "text": "Chỉ hàng hóa và tiền vốn"}], "correctOptionId": "a", "explanation": "4 trụ cột tự do lưu thông của thị trường chung EU."}, {"id": "geo-11-eu-q2", "prompt": "Đồng tiền chung được đưa vào sử dụng rộng rãi trong nhiều nước EU là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Euro (€)"}, {"id": "b", "text": "USD ($)"}, {"id": "c", "text": "Bảng Anh (£)"}], "correctOptionId": "a", "explanation": "Đồng Euro là đồng tiền chung chính thức của Khu vực đồng Euro (Eurozone)."}, {"id": "geo-11-eu-q3", "prompt": "Mục đích lớn nhất của việc thiết lập Thị trường chung EU là gì?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Tăng cường sức mạnh kinh tế và khả năng cạnh tranh toàn cầu"}, {"id": "b", "text": "Xóa bỏ ranh giới lãnh thổ quốc gia"}, {"id": "c", "text": "Tăng mức thuế nhập khẩu"}], "correctOptionId": "a", "explanation": "Hợp tác chặt chẽ nâng cao sức mạnh tổng hợp của toàn khối EU trên trường quốc tế."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-dong-nam-a',
    4,
    'LESSON',
    'Khu vực Đông Nam Á & ASEAN',
    'Địa lý 11: Địa lý kinh tế - xã hội thế giới & Khu vực (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Địa lý 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Đông Nam Á gồm 11 quốc gia, vị trí cầu nối giữa Ấn Độ Dương và Thái Bình Dương.", "Khí hậu nhiệt đới ẩm gió mùa, giàu khoáng sản và có nguồn lao động dồi dào.", "Cộng đồng ASEAN thành lập trên 3 trụ cột: Chính trị - An ninh, Kinh tế và Văn hóa - Xã hội."]$json$::jsonb,
    $json$["Sự phát triển năng động của Cộng đồng Kinh tế ASEAN (AEC) tạo thuận lợi cho thương mại Việt Nam."]$json$::jsonb,
    $json$[{"id": "geo-11-asean-q1", "prompt": "Khu vực Đông Nam Á bao gồm bao nhiêu quốc gia?", "difficulty": "EASY", "options": [{"id": "a", "text": "11 quốc gia"}, {"id": "b", "text": "10 quốc gia"}, {"id": "c", "text": "12 quốc gia"}], "correctOptionId": "a", "explanation": "Đông Nam Á gồm 10 nước thành viên ASEAN + Đông Timor."}, {"id": "geo-11-asean-q2", "prompt": "Thế mạnh nông nghiệp nổi bật của hầu hết các nước Đông Nam Á là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Trồng lúa nước và cây công nghiệp nhiệt đới (cao su, cà phê)"}, {"id": "b", "text": "Trồng lúa mì và chăn nuôi cừu"}, {"id": "c", "text": "Chăn nuôi hươu bắc cực"}], "correctOptionId": "a", "explanation": "Khí hậu nhiệt đới ẩm gió mùa rất thuận lợi cho cây lúa nước và cây nhiệt đới."}, {"id": "geo-11-asean-q3", "prompt": "Thách thức lớn về mặt xã hội của nhiều nước Đông Nam Á hiện nay là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Chênh lệch giàu nghèo, phân bố dân cư chưa hợp lý và môi trường đô thị"}, {"id": "b", "text": "Thiếu hụt nguồn lao động trầm trọng"}, {"id": "c", "text": "Dân số già hóa quá nhanh"}], "correctOptionId": "a", "explanation": "Tốc độ đô thị hóa nhanh gây sức ép về hạ tầng, việc làm và chất lượng sống."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'geography',
    11,
    'intro-2026.1',
    'geography-11-kinh-te-hoit-nhap',
    5,
    'FIVE_LESSON_REVIEW',
    'Vị thế các trung tâm kinh tế lớn thế giới',
    'Địa lý 11: Địa lý kinh tế - xã hội thế giới & Khu vực (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Địa lý 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Các trung tâm kinh tế lớn: Hoa Kỳ, EU, Trung Quốc, Nhật Bản.", "Sự trỗi dậy của kinh tế Trung Quốc và Châu Á - Thái Bình Dương tái định hình trật tự kinh tế toàn cầu.", "Việt Nam đẩy mạnh hội nhập kinh tế quốc tế sâu rộng thông qua các Hiệp định FTA thế hệ mới (CPTPP, EVFTA)."]$json$::jsonb,
    $json$["Hiệp định EVFTA mở ra cơ hội xuất khẩu nông sản và may mặc Việt Nam sang 27 nước EU."]$json$::jsonb,
    $json$[{"id": "geo-11-ttkt-q1", "prompt": "Quốc gia có quy mô GDP lớn nhất thế giới hiện nay là?", "difficulty": "EASY", "options": [{"id": "a", "text": "Hoa Kỳ"}, {"id": "b", "text": "Nhật Bản"}, {"id": "c", "text": "Đức"}], "correctOptionId": "a", "explanation": "Hoa Kỳ duy trì vị thế nền kinh tế số một thế giới về GDP."}, {"id": "geo-11-ttkt-q2", "prompt": "Các Hiệp định Thương mại tự do thế hệ mới (FTA) mang lại lợi ích gì cho Việt Nam?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Cắt giảm thuế quan, mở rộng thị trường xuất khẩu"}, {"id": "b", "text": "Giảm bớt tiêu chuẩn chất lượng hàng hóa"}, {"id": "c", "text": "Hạn chế doanh nghiệp nước ngoài"}], "correctOptionId": "a", "explanation": "FTA giúp hàng hóa Việt Nam tiếp cận các thị trường lớn với mức thuế ưu đãi."}, {"id": "geo-11-ttkt-q3", "prompt": "Để cạnh tranh thành công trên thị trường quốc tế, doanh nghiệp Việt Nam cần chú trọng nhất vào?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Nâng cao chất lượng sản phẩm, giá trị gia tăng và đổi mới công nghệ"}, {"id": "b", "text": "Chỉ bán giá rẻ không cần chất lượng"}, {"id": "c", "text": "Phụ thuộc vào trợ cấp nhà nước"}], "correctOptionId": "a", "explanation": "Năng lực cạnh tranh cốt lõi đến từ chất lượng, uy tín thương hiệu và công nghệ."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-su-phat-trien-cnxh',
    1,
    'LESSON',
    'Sự phát triển của Chủ nghĩa Xã hội thế giới',
    'Lịch sử 11: Lịch sử thế giới hiện đại & Việt Nam (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Lịch sử 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Thắng lợi của Cách mạng tháng Mười Nga năm 1917 lật đổ chính quyền tư sản, thành lập nhà nước công nông đầu tiên trên thế giới.", "Năm 1922, Liên bang Cộng hòa Xã hội Chủ nghĩa Xô viết (Liên Xô) thành lập. Sau Chiến tranh thế giới thứ hai, CNXH phát triển thành hệ thống thế giới.", "Công cuộc Đổi mới ở Việt Nam (khởi xướng 1986) và Cải cách mở cửa ở Trung Quốc khẳng định sự vận dụng sáng tạo chủ nghĩa Mác - Lênin."]$json$::jsonb,
    $json$["Cách mạng tháng Mười Nga làm thay đổi cục diện thế giới, cổ vũ mạnh mẽ phong trào giải phóng dân tộc ở các nước thuộc địa."]$json$::jsonb,
    $json$[{"id": "his-11-cnxh-q1", "prompt": "Sự kiện lịch sử nào năm 1917 đã mở ra thời đại mới trong lịch sử loài người?", "difficulty": "EASY", "options": [{"id": "a", "text": "Cách mạng tháng Mười Nga"}, {"id": "b", "text": "Chiến tranh thế giới thứ nhất bùng nổ"}, {"id": "c", "text": "Thành lập Liên Hợp Quốc"}], "correctOptionId": "a", "explanation": "Cách mạng tháng Mười Nga thành công thành lập nhà nước XHCN đầu tiên."}, {"id": "his-11-cnxh-q2", "prompt": "Liên bang Cộng hòa Xã hội Chủ nghĩa Xô viết (Liên Xô) được thành lập vào năm nào?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "1922"}, {"id": "b", "text": "1917"}, {"id": "c", "text": "1945"}], "correctOptionId": "a", "explanation": "Tháng 12 năm 1922, Liên Xô chính thức được thành lập trên cơ sở tự nguyện."}, {"id": "his-11-cnxh-q3", "prompt": "Trọng tâm của Công cuộc Đổi mới ở Việt Nam từ năm 1986 là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Đổi mới kinh tế toàn diện, xây dựng nền kinh tế thị trường định hướng XHCN"}, {"id": "b", "text": "Chỉ đổi mới văn hóa"}, {"id": "c", "text": "Từ bỏ mục tiêu tiến lên XHCN"}], "correctOptionId": "a", "explanation": "Đổi mới tập trung vào kinh tế làm trọng tâm, gắn liền với đổi mới hệ thống chính trị."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-chu-nghia-tu-ban-hien-dai',
    2,
    'LESSON',
    'Chủ nghĩa tư bản hiện đại & Cách mạng công nghiệp',
    'Lịch sử 11: Lịch sử thế giới hiện đại & Việt Nam (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Lịch sử 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Chủ nghĩa tư bản chuyển từ giai đoạn tự do cạnh tranh sang giai đoạn độc quyền (đầu thế kỷ XX) và chủ nghĩa tư bản hiện đại (sau 1945).", "Đặc điểm CNTB hiện đại: Lực lượng sản xuất phát triển cao nhờ khoa học công nghệ, có sự điều tiết của nhà nước, tính chất toàn cầu hóa."]$json$::jsonb,
    $json$["Các tập đoàn độc quyền xuyên quốc gia (TNCs) chi phối mạnh mẽ nền kinh tế toàn cầu."]$json$::jsonb,
    $json$[{"id": "his-11-cntb-q1", "prompt": "Đặc điểm nổi bật của Chủ nghĩa tư bản hiện đại từ sau Chiến tranh thế giới thứ hai là?", "difficulty": "EASY", "options": [{"id": "a", "text": "Có sự điều tiết của nhà nước độc quyền tư bản và liên kết quốc tế"}, {"id": "b", "text": "Chỉ dựa vào nông nghiệp thủ công"}, {"id": "c", "text": "Không sử dụng khoa học công nghệ"}], "correctOptionId": "a", "explanation": "Nhà nước tư bản can thiệp điều tiết nền kinh tế kết hợp với các công ty xuyên quốc gia."}, {"id": "his-11-cntb-q2", "prompt": "Thách thức lớn nhất đối với Chủ nghĩa tư bản hiện đại là gì?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Khủng hoảng kinh tế chu kỳ, phân hóa giàu nghèo và mâu thuẫn xã hội"}, {"id": "b", "text": "Thiếu nguyên nhiên liệu hoàn toàn"}, {"id": "c", "text": "Không có thị trường tiêu thụ"}], "correctOptionId": "a", "explanation": "Bản chất mâu thuẫn giữa tính chất xã hội hóa và chế độ chiếm hữu tư nhân vẫn tồn tại."}, {"id": "his-11-cntb-q3", "prompt": "Các cuộc cách mạng công nghiệp thời hiện đại đã làm thay đổi nền sản xuất như thế nào?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Chuyển từ lao động cơ khí sang tự động hóa, tin học hóa và trí tuệ nhân tạo"}, {"id": "b", "text": "Quay lại sản xuất thủ công"}, {"id": "c", "text": "Giảm năng suất lao động"}], "correctOptionId": "a", "explanation": "Thúc đẩy lực lượng sản xuất phát triển vượt bậc với công nghệ cao."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-dong-nam-a-giai-phong',
    3,
    'LESSON',
    'Phong trào giải phóng dân tộc ở Đông Nam Á',
    'Lịch sử 11: Lịch sử thế giới hiện đại & Việt Nam (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Lịch sử 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Đông Nam Á hầu hết trở thành thuộc địa của thực dân phương Tây (trừ Xiêm/Thái Lan duy trì độc lập nhờ chính sách ngoại giao khéo léo).", "Sau Chiến tranh thế giới thứ hai (1945), nhân cơ hội Nhật Bản hàng hàng, Indonesia, Việt Nam và Lào đã chớp thời cơ tuyên bố độc lập."]$json$::jsonb,
    $json$["Cách mạng tháng Tám 1945 ở Việt Nam khai sinh nước Việt Nam Dân chủ Cộng hòa."]$json$::jsonb,
    $json$[{"id": "his-11-dna-q1", "prompt": "Quốc gia duy nhất ở khu vực Đông Nam Á giữ được độc lập trước sự xâm lược của thực dân phương Tây là?", "difficulty": "EASY", "options": [{"id": "a", "text": "Xiêm (Thái Lan)"}, {"id": "b", "text": "Việt Nam"}, {"id": "c", "text": "Indonesia"}], "correctOptionId": "a", "explanation": "Thái Lan thực hiện chính sách cải cách và ngoại giao vùng đệm giữ độc lập."}, {"id": "his-11-dna-q2", "prompt": "Năm 1945, ba quốc gia Đông Nam Á đầu tiên giành được độc lập là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Indonesia, Việt Nam, Lào"}, {"id": "b", "text": "Thái Lan, Myanmar, Singapore"}, {"id": "c", "text": "Campuchia, Malaysia, Philippines"}], "correctOptionId": "a", "explanation": "Chớp thời cơ Nhật hàng hằng, Indonesia (17/8), Việt Nam (2/9) và Lào (12/10) tuyên bố độc lập năm 1945."}, {"id": "his-11-dna-q3", "prompt": "Sự thành lập Hiệp hội các quốc gia Đông Nam Á (ASEAN năm 1967) đáp ứng nhu cầu nào?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hợp tác phát triển kinh tế, văn hóa và ổn định hòa bình khu vực"}, {"id": "b", "text": "Thành lập liên minh quân sự chống nước khác"}, {"id": "c", "text": "Đồng hóa văn hóa giữa các nước"}], "correctOptionId": "a", "explanation": "ASEAN ra đời nhằm thúc đẩy tăng trưởng kinh tế, hòa bình và hợp tác khu vực."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-chien-tranh-the-gioi',
    4,
    'LESSON',
    'Chiến tranh thế giới thứ nhất và thứ hai',
    'Lịch sử 11: Lịch sử thế giới hiện đại & Việt Nam (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Lịch sử 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Chiến tranh thế giới thứ nhất (1914 - 1918) và thứ hai (1939 - 1945) là các cuộc chiến tranh tàn phá thảm khốc nhất lịch sử.", "Nguyên nhân sâu xa: Mâu thuẫn giữa các nước đế quốc về thị trường và thuộc địa.", "Kết thúc CTTG II: Khối Đồng minh (Liên Xô, Mỹ, Anh) đánh bại chủ nghĩa phát xít, dẫn đến sự ra đời của Tổ chức Liên Hợp Quốc."]$json$::jsonb,
    $json$["Hội nghị Yalta (1945) phân chia phạm vi ảnh hưởng tạo nên Trật tự hai cực I-an-ta."]$json$::jsonb,
    $json$[{"id": "his-11-ct-q1", "prompt": "Chiến tranh thế giới thứ hai (1939 - 1945) kết thúc với sự thất bại hoàn toàn của khối nào?", "difficulty": "EASY", "options": [{"id": "a", "text": "Khối Phát xít (Đức, Ý, Nhật)"}, {"id": "b", "text": "Khối Đồng minh"}, {"id": "c", "text": "Khối Hiệp ước"}], "correctOptionId": "a", "explanation": "Khối phát xít bị tiêu diệt hoàn toàn năm 1945."}, {"id": "his-11-ct-q2", "prompt": "Tổ chức quốc tế lớn nhất được thành lập sau Chiến tranh thế giới thứ hai nhằm duy trì hòa bình an ninh thế giới là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Liên Hợp Quốc (UN)"}, {"id": "b", "text": "Quốc liên"}, {"id": "c", "text": "NATO"}], "correctOptionId": "a", "explanation": "Liên Hợp Quốc chính thức hoạt động từ ngày 24/10/1945."}, {"id": "his-11-ct-q3", "prompt": "Bài học lịch sử lớn nhất rút ra từ hai cuộc Chiến tranh thế giới đối với hòa bình hiện nay là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Giải quyết các xung đột quốc tế bằng thương lượng hòa bình và đối thoại"}, {"id": "b", "text": "Chạy đua vũ trang tối tân"}, {"id": "c", "text": "Đóng cửa biên giới không giao thiệp"}], "correctOptionId": "a", "explanation": "Hợp tác đối thoại hòa bình là con đường bảo vệ sự tồn vong nhân loại."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'history',
    11,
    'intro-2026.1',
    'history-11-van-minh-dai-viet',
    5,
    'FIVE_LESSON_REVIEW',
    'Văn minh Đại Việt & Bảo tồn di sản',
    'Lịch sử 11: Lịch sử thế giới hiện đại & Việt Nam (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Lịch sử 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Văn minh Đại Việt trải qua các triều đại Lý, Trần, Lê, Nguyễn với những thành tựu rực rỡ về chính trị, kinh tế, văn hóa, giáo dục.", "Bảo tồn di sản lịch sử văn hóa giúp gìn giữ bản sắc dân tộc, giáo dục truyền thống và phát triển du lịch bền vững."]$json$::jsonb,
    $json$["Văn Miếu - Quốc Tử Giám là biểu tượng rực rỡ cho truyền thống hiếu học và tôn sư trọng đạo của dân tộc."]$json$::jsonb,
    $json$[{"id": "his-11-vm-q1", "prompt": "Văn Miếu được xây dựng dưới triều đại phong kiến nào của Việt Nam?", "difficulty": "EASY", "options": [{"id": "a", "text": "Triều Lý (1070)"}, {"id": "b", "text": "Triều Trần"}, {"id": "c", "text": "Triều Nguyễn"}], "correctOptionId": "a", "explanation": "Văn Miếu được xây dựng năm 1070 dưới thời vua Lý Thánh Tông."}, {"id": "his-11-vm-q2", "prompt": "Chữ viết chính thức dùng trong khoa cử và văn bản hành chính thời Lý - Trần - Lê là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Chữ Hán và chữ Nôm"}, {"id": "b", "text": "Chữ Quốc ngữ"}, {"id": "c", "text": "Chữ Phạn"}], "correctOptionId": "a", "explanation": "Chữ Hán dùng chính thức trong khoa cử; chữ Nôm phát triển sáng tạo văn học dân tộc."}, {"id": "his-11-vm-q3", "prompt": "Việc bảo tồn các di sản văn hóa như Cố đô Huế, Phố cổ Hội An có ý nghĩa gì?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Gìn giữ bản sắc dân tộc, giáo dục truyền thống và phát triển du lịch bền vững"}, {"id": "b", "text": "Làm lãng phí ngân sách nhà nước"}, {"id": "c", "text": "Cản trở đô thị hóa hiện đại"}], "correctOptionId": "a", "explanation": "Di sản là tài sản vô giá kết nối quá khứ với hiện tại và phát triển kinh tế văn hóa."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-nguyen-du-truyen-kieu',
    1,
    'LESSON',
    'Nguyễn Du và Giá trị nhân đạo Truyện Kiều',
    'Ngữ văn 11: Đọc hiểu thể loại & Viết bài nghị luận (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Ngữ văn 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Đại thi hào Nguyễn Du (1765 - 1820) là danh nhân văn hóa thế giới.", "Truyện Kiều là kiệt tác thơ lục bát bằng chữ Nôm. Tác phẩm thể hiện giá trị nhân đạo cao cả: tiếng nói xót thương số phận con người (đặc biệt là người phụ nữ), tố cáo các lực lượng tàn bạo, trân trọng khát vọng tình yêu và tự do.", "Đoạn trích Trao duyên miêu tả bi kịch tình yêu tan vỡ và diễn biến tâm trạng phức tạp, giằng xé nội tâm của Thúy Kiều."]$json$::jsonb,
    $json$["\"Cậy em em có chịu lời / Sit lên cho chị lạy rồi sẽ thưa\" - Lời khẩn cầu tha thiết, trân trọng và đau đớn của Thúy Kiều khi nhờ Thúy Vân nối duyên với Kim Trọng."]$json$::jsonb,
    $json$[{"id": "lit-11-kieu-q1", "prompt": "Tác phẩm Truyện Kiều của Đại thi hào Nguyễn Du được viết bằng thể thơ truyền thống nào?", "difficulty": "EASY", "options": [{"id": "a", "text": "Lục bát"}, {"id": "b", "text": "Song thất lục bát"}, {"id": "c", "text": "Thất ngôn bát cú"}], "correctOptionId": "a", "explanation": "Truyện Kiều gồm 3254 câu thơ lục bát chữ Nôm đỉnh cao của văn học dân tộc."}, {"id": "lit-11-kieu-q2", "prompt": "Nội dung cốt lõi của Giá trị nhân đạo trong Truyện Kiều thể hiện ở?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Xót thương con người bất hạnh và trân trọng khát vọng quyền sống, tình yêu"}, {"id": "b", "text": "Tập trung ca ngợi triều đại phong kiến"}, {"id": "c", "text": "Miêu tả thuần túy cảnh quan thiên nhiên"}], "correctOptionId": "a", "explanation": "Giá trị nhân đạo đặt con người vào trung tâm sự xót thương và khẳng định phẩm giá."}, {"id": "lit-11-kieu-q3", "prompt": "Từ 'cậy' trong câu thơ 'Cậy em em có chịu lời' thể hiện thái độ tâm lý nào của Thúy Kiều?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Gửi gắm niềm hy vọng tha thiết kèm sự nài nỉ, nương tựa"}, {"id": "b", "text": "Thái độ ra lệnh bắt buộc"}, {"id": "c", "text": "Sự nghi ngờ không tin tưởng"}], "correctOptionId": "a", "explanation": "'Cậy' vừa gửi gắm vừa trông mong, chứa đựng sự quỵ lụy tha thiết trong hoàn cảnh khó nói."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-tho-truyenthong-nom',
    2,
    'LESSON',
    'Thơ Nôm đường luật và Thơ mới',
    'Ngữ văn 11: Đọc hiểu thể loại & Viết bài nghị luận (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Ngữ văn 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Thơ Nôm Đường luật vận dụng luật thơ Đường (Thất ngôn bát cú, Thất ngôn tứ tuyệt) kết hợp với từ ngữ dân tộc, hình ảnh đời thường gần gũi.", "Phong trào Thơ mới (1932 - 1945) bứt phá khỏi quy phạm cổ điển, giải phóng cái 'Tôi' cá nhân, đổi mới hình thức thể thơ và cảm xúc tự do.", "Các tác giả tiêu biểu: Xuân Diệu (thơ tình nồng nàn), Huy Cận (nỗi sầu vũ trụ), Hàn Mặc Tử (thơ điên nồng cháy)."]$json$::jsonb,
    $json$["Xuân Diệu trong Vội vàng: \"Tôi muốn tắt nắng đi / Cho màu đừng nhạt mất / Tôi muốn buộc gió lại / Cho hương đừng bay đi\"."]$json$::jsonb,
    $json$[{"id": "lit-11-tm-q1", "prompt": "Điểm đổi mới nổi bật nhất của phong trào Thơ mới (1932 - 1945) là gì?", "difficulty": "EASY", "options": [{"id": "a", "text": "Giải phóng cái 'Tôi' cá nhân và phá vỡ các quy phạm cổ điển"}, {"id": "b", "text": "Dùng hoàn toàn chữ Hán"}, {"id": "c", "text": "Chỉ viết theo thể thất ngôn bát cú"}], "correctOptionId": "a", "explanation": "Thơ mới khẳng định cái 'Tôi' cá nhân với muôn màu sắc cảm xúc linh hoạt."}, {"id": "lit-11-tm-q2", "prompt": "Nhà thơ nào được mệnh danh là 'Nhà thơ mới nhất trong các nhà thơ mới' với hồn thơ khát khao giao cảm vội vàng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Xuân Diệu"}, {"id": "b", "text": "Huy Cận"}, {"id": "c", "text": "Tố Hữu"}], "correctOptionId": "a", "explanation": "Hoài Thanh nhận xét Xuân Diệu là nhà thơ mới nhất, sôi nổi yêu đời nồng nàn."}, {"id": "lit-11-tm-q3", "prompt": "Cảm hứng chủ đạo trong bài thơ 'Vội vàng' của Xuân Diệu là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Lòng yêu đời, trân trọng từng phút giây tuổi trẻ và thiên nhiên mùa xuân"}, {"id": "b", "text": "Nỗi hoài cổ tiếc thương quá khứ"}, {"id": "c", "text": "Lòng u uất bế tắc trước thời cuộc"}], "correctOptionId": "a", "explanation": "Xuân Diệu giục giã sống vội vàng để tận hưởng vẻ đẹp thiên nhiên và tình yêu tuổi trẻ."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-nghi-luan-xa-hoi',
    3,
    'LESSON',
    'Văn bản nghị luận & Nghệ thuật lập luận',
    'Ngữ văn 11: Đọc hiểu thể loại & Viết bài nghị luận (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Ngữ văn 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Văn bản nghị luận hướng tới thuyết phục người đọc về một tư tưởng, quan điểm bằng hệ thống Luận điểm, Lý lẽ và Dẫn chứng.", "Luận điểm là ý kiến khẳng định/bác bỏ; Lý lẽ giải thích nguyên nhân; Dẫn chứng là bằng chứng thực tế khách quan.", "Yêu cầu: Lập luận chặt chẽ, dẫn chứng xác thực, tư duy phản biện sắc bén và cảm xúc chân thành."]$json$::jsonb,
    $json$["Nghị luận về văn hóa ứng xử trên mạng xã hội: Luận điểm -> Lý lẽ tác hại của tin giả/bạo lực mạng -> Dẫn chứng sự việc thực tế."]$json$::jsonb,
    $json$[{"id": "lit-11-nl-q1", "prompt": "Thành phần nào đóng vai trò là cột mốc ý kiến khẳng định chính trong bài văn nghị luận?", "difficulty": "EASY", "options": [{"id": "a", "text": "Luận điểm"}, {"id": "b", "text": "Dẫn chứng"}, {"id": "c", "text": "Mở bài"}], "correctOptionId": "a", "explanation": "Luận điểm là các ý kiến thể hiện quan điểm của người viết về vấn đề."}, {"id": "lit-11-nl-q2", "prompt": "Yêu cầu quan trọng đối với Dẫn chứng trong bài văn nghị luận là gì?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Chính xác, tiêu biểu, chân thực và có tính thuyết phục"}, {"id": "b", "text": "Càng dài và hư cấu càng tốt"}, {"id": "c", "text": "Không cần liên quan đến luận điểm"}], "correctOptionId": "a", "explanation": "Dẫn chứng phải tiêu biểu và chân thực thì lập luận mới có sức nặng thuyết phục."}, {"id": "lit-11-nl-q3", "prompt": "Tư duy phản biện trong bài văn nghị luận giúp ích gì cho bài viết?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Xem xét vấn đề đa chiều, lật lại vấn đề để lập luận toàn diện hơn"}, {"id": "b", "text": "Phủ nhận toàn bộ quan điểm của người khác"}, {"id": "c", "text": "Làm bài viết dài hơn"}], "correctOptionId": "a", "explanation": "Phản biện giúp nhìn nhận các góc khuất, phủ định ý kiến phiến diện để khẳng định luận điểm."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-bi-kich-kich-ban',
    4,
    'LESSON',
    'Bi kịch và Kịch bản văn học',
    'Ngữ văn 11: Đọc hiểu thể loại & Viết bài nghị luận (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Ngữ văn 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Bi kịch là thể loại kịch phản ánh xung đột gay gắt giữa khát vọng cao đẹp của nhân vật với hoàn cảnh thực tại nghiệt ngã.", "Nhân vật bi kịch mang phẩm chất cao đẹp, chiến đấu dũng cảm nhưng phải chịu thất bại hoặc hy sinh đau thương, gợi sự thương cảm và thanh lọc tâm hồn người xem.", "Tác phẩm tiêu biểu: Vũ Như Tô (Nguyễn Huy Tưởng) phản ánh bi kịch giữa khát vọng nghệ thuật siêu đàm và lợi ích nhân dân."]$json$::jsonb,
    $json$["Vũ Như Tô đắm đuối xây Cửu Trùng Đài đẹp cho đời nhưng vỡ mộng khi nhân dân lầm than nổi dậy đốt đài."]$json$::jsonb,
    $json$[{"id": "lit-11-bk-q1", "prompt": "Đặc trưng mấu chốt của thể loại Bi kịch là gì?", "difficulty": "EASY", "options": [{"id": "a", "text": "Xung đột gay gắt giữa khát vọng nhân vật và hoàn cảnh thực tại"}, {"id": "b", "text": "Tạo ra tiếng cười gây hái hài hước"}, {"id": "c", "text": "Kết thúc luôn có hậu vui vẻ"}], "correctOptionId": "a", "explanation": "Xung đột bi kịch không thể hòa giải dẫn đến kết thúc đau thương của nhân vật."}, {"id": "lit-11-bk-q2", "prompt": "Bi kịch của nhân vật Vũ Như Tô trong đoạn trích 'Vĩnh biệt Cửu Trùng Đài' là bi kịch gì?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Mâu thuẫn giữa khát vọng nghệ thuật thuần túy và đời sống lầm than của nhân dân"}, {"id": "b", "text": "Mâu thuẫn giữa tình yêu nam nữ"}, {"id": "c", "text": "Tranh chấp tài sản gia đình"}], "correctOptionId": "a", "explanation": "Vũ Như Tô muốn xây công trình vĩ đại nhưng lại làm kiệt quệ sức dân trong hoàn cảnh đất nước khó khăn."}, {"id": "lit-11-bk-q3", "prompt": "Tác dụng tâm lý 'Thanh lọc' (Catharsis) của bi kịch đối với khán giả là gì?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Gợi sự thương cảm và khiếp sợ, từ đó làm trong sạch và hướng thiện tâm hồn"}, {"id": "b", "text": "Làm khán giả tuyệt vọng bỏ cuộc"}, {"id": "c", "text": "Gây ra sự tức giận căm thù"}], "correctOptionId": "a", "explanation": "Bi kịch giúp con người đồng cảm với nỗi đau và nâng cao tinh thần trân trọng cái đẹp."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'literature',
    11,
    'intro-2026.1',
    'literature-11-tuy-but-tan-van',
    5,
    'FIVE_LESSON_REVIEW',
    'Tùy bút và Tản văn hiện đại',
    'Ngữ văn 11: Đọc hiểu thể loại & Viết bài nghị luận (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Ngữ văn 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Tùy bút và Tản văn là thể loại ký chất trữ tình đậm nét, tự do phóng túng trong biểu hiện cảm xúc.", "Tùy bút bộc lộ cái 'Tôi' của nhà văn qua quan sát, liên tưởng phong phú và vốn văn hóa sâu rộng.", "Tác phẩm tiêu biểu: Người lái đò Sông Đà (Nguyễn Tuân), Ai đã đặt tên cho dòng sông (Hoàng Phủ Ngọc Tường)."]$json$::jsonb,
    $json$["Nguyễn Tuân miêu tả Sông Đà vừa 'hung bạo' với bão đá thác rống, vừa 'trữ tình' thơ mộng như một áng tóc trữ tình."]$json$::jsonb,
    $json$[{"id": "lit-11-tb-q1", "prompt": "Nét đặc trưng nổi bật nhất của thể loại Tùy bút là gì?", "difficulty": "EASY", "options": [{"id": "a", "text": "Đậm đà nét trữ tình, thể hiện cái 'Tôi' độc đáo và liên tưởng phong phú của tác giả"}, {"id": "b", "text": "Tập trung xây dựng cốt truyện ly kỳ nhiều hư cấu"}, {"id": "c", "text": "Viết theo quy tắc luật thơ nghiêm ngặt"}], "correctOptionId": "a", "explanation": "Tùy bút thể hiện cái 'Tôi' tài hoa, uyên bác qua nét bút tự do trữ tình."}, {"id": "lit-11-tb-q2", "prompt": "Hình ảnh con sông Đà trong tùy bút của Nguyễn Tuân hiện lên với hai nét tính cách đối lập là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Hung bạo và Trữ tình"}, {"id": "b", "text": "Phôi pha và Cổ kính"}, {"id": "c", "text": "Êm đềm và Lạnh lẽo"}], "correctOptionId": "a", "explanation": "Nguyễn Tuân miêu tả Sông Đà hung bạo như kẻ thù số một và trữ tình đằm thắm như một cố nhân."}, {"id": "lit-11-tb-q3", "prompt": "Trong 'Ai đã đặt tên cho dòng sông', Hoàng Phủ Ngọc Tường đã thể hiện tình cảm gì với dòng sông Hương và xứ Huế?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Tình yêu thiết tha, niềm tự hào sâu sắc về vẻ đẹp thiên nhiên và văn hóa Huế"}, {"id": "b", "text": "Nỗi buồn hoang vắng bế tắc"}, {"id": "c", "text": "Sự phê phán lối sống cổ hủ"}], "correctOptionId": "a", "explanation": "Tác giả soi chiếu Sông Hương dưới góc độ địa lý, lịch sử và văn hóa nghệ thuật xứ Huế."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-trig',
    1,
    'LESSON',
    'Hàm số lượng giác & Phương trình lượng giác',
    'Toán 11: Hàm số, Dãy số, Giới hạn và Hình học không gian',
    'Bài học Lớp 11 do AI Mentor biên soạn theo yêu cầu cần đạt Chương trình GDPT 2018.',
    $json$["Hàm số sin, cos, tan có tính tuần hoàn. Phương trình sin x = sin a có nghiệm x = a + k2π hoặc x = π - a + k2π."]$json$::jsonb,
    $json$["Giải sin x = 1/2 => x = π/6 + k2π hoặc x = 5π/6 + k2π (k ∈ Z)."]$json$::jsonb,
    $json$[{"id": "math-11-trig-q1", "prompt": "Tập giá trị của hàm số y = sin x là?", "difficulty": "EASY", "options": [{"id": "a", "text": "[-1; 1]"}, {"id": "b", "text": "[0; 1]"}, {"id": "c", "text": "R"}], "correctOptionId": "a", "explanation": "Giá trị sin x luôn nằm trong đoạn [-1; 1]."}, {"id": "math-11-trig-q2", "prompt": "Phương trình sin x = 0 có nghiệm tổng quát là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "x = kπ"}, {"id": "b", "text": "x = k2π"}, {"id": "c", "text": "x = π/2 + kπ"}], "correctOptionId": "a", "explanation": "sin x = 0 tại các điểm x = kπ với k ∈ Z."}, {"id": "math-11-trig-q3", "prompt": "Chu kỳ tuần hoàn của hàm số y = tan x là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "2π"}, {"id": "b", "text": "π"}, {"id": "c", "text": "π/2"}], "correctOptionId": "b", "explanation": "Hàm tan x lặp lại giá trị sau mỗi khoảng π."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-sequences',
    2,
    'LESSON',
    'Dãy số, Cấp số cộng & Cấp số nhân',
    'Toán 11: Hàm số, Dãy số, Giới hạn và Hình học không gian',
    'Bài học Lớp 11 do AI Mentor biên soạn theo yêu cầu cần đạt Chương trình GDPT 2018.',
    $json$["Cấp số cộng có u_(n+1) = u_n + d. Cấp số nhân có u_(n+1) = u_n * q."]$json$::jsonb,
    $json$["Dãy 2, 5, 8, 11 là cấp số cộng có u_1 = 2, d = 3. Dãy 3, 6, 12, 24 là cấp số nhân có q = 2."]$json$::jsonb,
    $json$[{"id": "math-11-seq-q1", "prompt": "Công thức số hạng tổng quát của cấp số cộng u_n là?", "difficulty": "EASY", "options": [{"id": "a", "text": "u_n = u_1 + (n-1)d"}, {"id": "b", "text": "u_n = u_1 * d^(n-1)"}, {"id": "c", "text": "u_n = u_1 + nd"}], "correctOptionId": "a", "explanation": "Mỗi bước cộng thêm d, sau n-1 bước thu được u_n."}, {"id": "math-11-seq-q2", "prompt": "Cho cấp số nhân có u_1 = 3, công bội q = 2. Số hạng u_4 bằng bao nhiêu?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "24"}, {"id": "b", "text": "18"}, {"id": "c", "text": "12"}], "correctOptionId": "a", "explanation": "u_4 = u_1 * q^3 = 3 * 2^3 = 24."}, {"id": "math-11-seq-q3", "prompt": "Tổng n số hạng đầu của cấp số cộng u_1 = 1, d = 2 (tổng các số lẻ) là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "n^2"}, {"id": "b", "text": "2n"}, {"id": "c", "text": "n(n+1)"}], "correctOptionId": "a", "explanation": "S_n = n/2 * (2*1 + (n-1)*2) = n^2."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-limits',
    3,
    'LESSON',
    'Giới hạn của dãy số & Hàm số',
    'Toán 11: Hàm số, Dãy số, Giới hạn và Hình học không gian',
    'Bài học Lớp 11 do AI Mentor biên soạn theo yêu cầu cần đạt Chương trình GDPT 2018.',
    $json$["lim (1/n) = 0 khi n -> ∞. Nếu |q| < 1 thì lim q^n = 0."]$json$::jsonb,
    $json$["lim (2n+1)/(n+3) = 2 khi n -> ∞."]$json$::jsonb,
    $json$[{"id": "math-11-lim-q1", "prompt": "Giá trị lim (1/n) khi n tiến ra vô cùng là?", "difficulty": "EASY", "options": [{"id": "a", "text": "0"}, {"id": "b", "text": "1"}, {"id": "c", "text": "Vô cực"}], "correctOptionId": "a", "explanation": "Khi mẫu số tiến ra vô cực thì phân số tiến về 0."}, {"id": "math-11-lim-q2", "prompt": "lim (3n^2 + 2)/(n^2 - 1) khi n -> ∞ bằng?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "3"}, {"id": "b", "text": "2"}, {"id": "c", "text": "1"}], "correctOptionId": "a", "explanation": "Chia cả tử và mẫu cho n^2 ta thu được 3/1 = 3."}, {"id": "math-11-lim-q3", "prompt": "Hàm số f(x) liên tục tại x_0 khi nào?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "lim x->x_0 f(x) = f(x_0)"}, {"id": "b", "text": "f(x_0) = 0"}, {"id": "c", "text": "lim x->x_0 f(x) = 0"}], "correctOptionId": "a", "explanation": "Định nghĩa hàm số liên tục tại 1 điểm."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-derivatives',
    4,
    'LESSON',
    'Đạo hàm & Ý nghĩa hình học',
    'Toán 11: Hàm số, Dãy số, Giới hạn và Hình học không gian',
    'Bài học Lớp 11 do AI Mentor biên soạn theo yêu cầu cần đạt Chương trình GDPT 2018.',
    $json$["Đạo hàm f'(x_0) là hệ số góc của tiếp tuyến của đồ thị hàm số tại x_0. (x^n)' = n * x^(n-1)."]$json$::jsonb,
    $json$["Đạo hàm của f(x) = x^3 - 2x là f'(x) = 3x^2 - 2."]$json$::jsonb,
    $json$[{"id": "math-11-der-q1", "prompt": "Đạo hàm của hàm số f(x) = x^2 tại x = 3 là?", "difficulty": "EASY", "options": [{"id": "a", "text": "6"}, {"id": "b", "text": "9"}, {"id": "c", "text": "3"}], "correctOptionId": "a", "explanation": "f'(x) = 2x => f'(3) = 2 * 3 = 6."}, {"id": "math-11-der-q2", "prompt": "Đạo hàm của sin x là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "cos x"}, {"id": "b", "text": "-cos x"}, {"id": "c", "text": "-sin x"}], "correctOptionId": "a", "explanation": "(sin x)' = cos x."}, {"id": "math-11-der-q3", "prompt": "Ý nghĩa hình học của đạo hàm f'(x_0) là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "Hệ số góc của tiếp tuyến tại x_0"}, {"id": "b", "text": "Khoảng cách tới gốc tọa độ"}, {"id": "c", "text": "Diện tích hình phẳng"}], "correctOptionId": "a", "explanation": "f'(x_0) chính là k trong phương trình tiếp tuyến y = k(x - x_0) + y_0."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'math',
    11,
    'intro-2026.1',
    'math-11-geometry3d',
    5,
    'FIVE_LESSON_REVIEW',
    'Đường thẳng & Mặt phẳng trong không gian',
    'Toán 11: Hàm số, Dãy số, Giới hạn và Hình học không gian',
    'Bài học Lớp 11 do AI Mentor biên soạn theo yêu cầu cần đạt Chương trình GDPT 2018.',
    $json$["Hai đường thẳng song song, cắt nhau hoặc chéo nhau. Đường thẳng vuông góc với mặt phẳng khi vuông góc với 2 đường cắt nhau trong mặt phẳng."]$json$::jsonb,
    $json$["Trong hình lập phương ABCD.A'B'C'D', AA' vuông góc với mặt phẳng (ABCD)."]$json$::jsonb,
    $json$[{"id": "math-11-geo-q1", "prompt": "Hai đường thẳng không cùng thuộc một mặt phẳng gọi là?", "difficulty": "EASY", "options": [{"id": "a", "text": "Chéo nhau"}, {"id": "b", "text": "Song song"}, {"id": "c", "text": "Cắt nhau"}], "correctOptionId": "a", "explanation": "Định nghĩa hai đường thẳng chéo nhau trong không gian 3D."}, {"id": "math-11-geo-q2", "prompt": "Nếu d ⊥ a và d ⊥ b (a, b cắt nhau nằm trong (P)) thì?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "d ⊥ (P)"}, {"id": "b", "text": "d // (P)"}, {"id": "c", "text": "d ⊂ (P)"}], "correctOptionId": "a", "explanation": "Điều kiện đường thẳng vuông góc với mặt phẳng."}, {"id": "math-11-geo-q3", "prompt": "Góc giữa hai đường thẳng vuông góc nhau bằng?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "90°"}, {"id": "b", "text": "180°"}, {"id": "c", "text": "0°"}], "correctOptionId": "a", "explanation": "Hai đường thẳng vuông góc có góc bằng 90 độ."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-dao-dong-dieu-hoa',
    1,
    'LESSON',
    'Mô tả dao động điều hòa',
    'Vật lý 11: Dao động, Sóng và Điện trường (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Vật lý 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Dao động điều hòa là dao động trong đó ly độ x của vật là một hàm cosin (hoặc xin) theo thời gian: x = A cos(ωt + φ).", "A là biên độ (độ lệch cực đại khỏi VCB), ω là tần số góc, (ωt + φ) là pha dao động tại thời điểm t, φ là pha ban đầu."]$json$::jsonb,
    $json$["Phương trình x = 5 cos(4πt + π/3) cm có biên độ A = 5 cm, tần số góc ω = 4π rad/s, pha ban đầu φ = π/3 rad."]$json$::jsonb,
    $json$[{"id": "phy-11-ddh-q1", "prompt": "Phương trình dao động điều hòa có dạng x = A cos(ωt + φ). Đại lượng A đại diện cho?", "difficulty": "EASY", "options": [{"id": "a", "text": "Biên độ dao động"}, {"id": "b", "text": "Pha ban đầu"}, {"id": "c", "text": "Tần số góc"}], "correctOptionId": "a", "explanation": "A là ly độ cực đại của vật dao động, được gọi là biên độ dao động (A > 0)."}, {"id": "phy-11-ddh-q2", "prompt": "Một vật dao động điều hòa với phương trình x = 6 cos(2πt - π/6) cm. Pha ban đầu của dao động là?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "6 cm"}, {"id": "b", "text": "-π/6 rad"}, {"id": "c", "text": "2π rad/s"}], "correctOptionId": "b", "explanation": "So sánh với dạng chuẩn x = A cos(ωt + φ), ta thấy φ = -π/6 rad."}, {"id": "phy-11-ddh-q3", "prompt": "Vật dao động điều hòa thực hiện 20 dao động toàn phần trong 10 giây. Tần số góc ω của dao động bằng?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "4π rad/s"}, {"id": "b", "text": "2π rad/s"}, {"id": "c", "text": "2 Hz"}], "correctOptionId": "a", "explanation": "Tần số f = N/t = 20/10 = 2 Hz. Tần số góc ω = 2πf = 2π * 2 = 4π rad/s."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-van-toc-gia-toc',
    2,
    'LESSON',
    'Vận tốc và gia tốc trong dao động điều hòa',
    'Vật lý 11: Dao động, Sóng và Điện trường (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Vật lý 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Vận tốc là đạo hàm bậc nhất của ly độ theo thời gian: v = x' = -ωA sin(ωt + φ). Vận tốc cực đại tại VCB: v_max = ωA.", "Gia tốc là đạo hàm của vận tốc: a = v' = -ω^2 x. Gia tốc luôn hướng về VCB và biến thiên ngược pha với ly độ x."]$json$::jsonb,
    $json$["Tại vị trí biên (x = ±A), ly độ cực đại nên gia tốc cực đại a_max = ω^2 A, còn vận tốc v = 0."]$json$::jsonb,
    $json$[{"id": "phy-11-vt-gt-q1", "prompt": "Vật dao động điều hòa có vận tốc đạt độ lớn cực đại khi vật qua vị trí nào?", "difficulty": "EASY", "options": [{"id": "a", "text": "Vị trí cân bằng"}, {"id": "b", "text": "Vị trí biên dương"}, {"id": "c", "text": "Vị trí biên âm"}], "correctOptionId": "a", "explanation": "Khi qua vị trí cân bằng, x = 0 nên độ lớn vận tốc đạt cực đại v_max = ωA."}, {"id": "phy-11-vt-gt-q2", "prompt": "Phát biểu nào sau đây đúng về gia tốc của vật dao động điều hòa?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Gia tốc luôn biến thiên ngược pha với ly độ"}, {"id": "b", "text": "Gia tốc có độ lớn cực đại tại vị trí cân bằng"}, {"id": "c", "text": "Gia tốc luôn cùng chiều với chuyển động"}], "correctOptionId": "a", "explanation": "Do a = -ω^2 x nên gia tốc luôn đổi chiều và biến thiên ngược pha với ly độ x."}, {"id": "phy-11-vt-gt-q3", "prompt": "Vật dao động có biên độ A = 4 cm, tần số góc ω = 10 rad/s. Gia tốc cực đại của vật là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "400 cm/s² (4 m/s²)"}, {"id": "b", "text": "40 cm/s²"}, {"id": "c", "text": "4 m/s"}], "correctOptionId": "a", "explanation": "a_max = ω² A = 10² * 4 = 100 * 4 = 400 cm/s² = 4 m/s²."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-nang-luong-dao-dong',
    3,
    'LESSON',
    'Năng lượng trong dao động điều hòa',
    'Vật lý 11: Dao động, Sóng và Điện trường (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Vật lý 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Động năng Wd = 1/2 m v² biến thiên theo thời gian. Thế năng Wt = 1/2 k x² biến thiên theo thời gian.", "Cơ năng W = Wd + Wt = 1/2 k A² = 1/2 m ω² A² là một hằng số bảo toàn, tỉ lệ với bình phương biên độ."]$json$::jsonb,
    $json$["Khi động năng bằng thế năng (Wd = Wt), ly độ của vật bằng x = ±A / √2."]$json$::jsonb,
    $json$[{"id": "phy-11-nl-q1", "prompt": "Cơ năng của một vật dao động điều hòa tỉ lệ với đại lượng nào?", "difficulty": "EASY", "options": [{"id": "a", "text": "Bình phương biên độ dao động"}, {"id": "b", "text": "Biên độ dao động"}, {"id": "c", "text": "Tần số dao động"}], "correctOptionId": "a", "explanation": "Công thức cơ năng W = 1/2 m ω² A², nên W tỉ lệ thuận với A²."}, {"id": "phy-11-nl-q2", "prompt": "Trong quá trình vật dao động điều hòa chuyển động từ vị trí biên về vị trí cân bằng thì?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Thế năng chuyển hóa thành động năng"}, {"id": "b", "text": "Động năng chuyển hóa thành thế năng"}, {"id": "c", "text": "Cơ năng tăng dần"}], "correctOptionId": "a", "explanation": "Từ biên về VCB, độ lớn ly độ x giảm (thế năng giảm) và tốc độ v tăng (động năng tăng)."}, {"id": "phy-11-nl-q3", "prompt": "Tại vị trí ly độ x = A/2, tỉ số giữa thế năng và động năng của vật là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "1/3"}, {"id": "b", "text": "1/2"}, {"id": "c", "text": "3"}], "correctOptionId": "a", "explanation": "Wt = 1/2 k (A/2)² = 1/4 W. Do đó Wd = W - Wt = 3/4 W. Tỉ số Wt/Wd = (1/4)/(3/4) = 1/3."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-song-co',
    4,
    'LESSON',
    'Sóng cơ và sự truyền sóng',
    'Vật lý 11: Dao động, Sóng và Điện trường (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Vật lý 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Sóng cơ là sự lan truyền dao động cơ trong môi trường vật chất theo thời gian.", "Sóng dọc có phương dao động trùng với phương truyền sóng (truyền trong chất khí, lỏng, rắn). Sóng ngang có phương dao động vuông góc với phương truyền sóng (truyền trên bề mặt chất lỏng và trong chất rắn).", "Bước sóng λ = v * T = v / f là khoảng cách giữa hai điểm gần nhất trên cùng một phương truyền sóng dao động cùng pha."]$json$::jsonb,
    $json$["Sóng âm truyền trong không khí là sóng dọc. Sóng trên mặt nước là sóng ngang."]$json$::jsonb,
    $json$[{"id": "phy-11-song-q1", "prompt": "Bước sóng λ được xác định bởi công thức nào sau đây?", "difficulty": "EASY", "options": [{"id": "a", "text": "λ = v / f"}, {"id": "b", "text": "λ = v * f"}, {"id": "c", "text": "λ = f / v"}], "correctOptionId": "a", "explanation": "Bước sóng λ = v * T = v / f, trong đó v là vận tốc truyền sóng, f là tần số."}, {"id": "phy-11-song-q2", "prompt": "Sóng dọc là sóng có phương dao động của các phần tử môi trường?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Trùng với phương truyền sóng"}, {"id": "b", "text": "Vuông góc với phương truyền sóng"}, {"id": "c", "text": "Nằm ngang bất kỳ"}], "correctOptionId": "a", "explanation": "Định nghĩa: Sóng dọc có phương dao động trùng với phương truyền sóng."}, {"id": "phy-11-song-q3", "prompt": "Một nguồn sóng dao động với tần số f = 50 Hz truyền đi với vận tốc v = 15 m/s. Bước sóng λ là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "0,3 m (30 cm)"}, {"id": "b", "text": "750 m"}, {"id": "c", "text": "3,33 m"}], "correctOptionId": "a", "explanation": "λ = v / f = 15 / 50 = 0,3 m = 30 cm."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
insert into public.mentor_course_units(subject_id,grade,course_version,unit_id,position,kind,title,course_title,source_note,guidance,examples,questions,sources) values(
    'physics',
    11,
    'intro-2026.1',
    'physics-11-giao-thoa-song',
    5,
    'FIVE_LESSON_REVIEW',
    'Giao thoa sóng cơ & Sóng dừng',
    'Vật lý 11: Dao động, Sóng và Điện trường (SGK Kết nối tri thức)',
    'Nội dung biên soạn bám sát chuẩn kiến thức SGK Vật lý 11 - Bộ sách Kết nối tri thức với cuộc sống.',
    $json$["Giao thoa là hiện tượng hai sóng kết hợp gặp nhau tạo nên các cực đại và cực tiểu giao thoa xen kẽ.", "Điều kiện cực đại giao thoa: d2 - d1 = k λ. Điều kiện cực tiểu giao thoa: d2 - d1 = (k + 0.5) λ.", "Sóng dừng hình thành do sự phản xạ và giao thoa của sóng tới và sóng phản xạ. Khoảng cách giữa 2 nút sóng liên tiếp bằng λ/2."]$json$::jsonb,
    $json$["Dây hai đầu cố định chiều dài L = k * (λ/2) sinh ra k bụng sóng."]$json$::jsonb,
    $json$[{"id": "phy-11-gt-q1", "prompt": "Trong hiện tượng sóng dừng trên sợi dây, khoảng cách giữa hai nút sóng liên tiếp bằng?", "difficulty": "EASY", "options": [{"id": "a", "text": "Nửa bước sóng (λ/2)"}, {"id": "b", "text": "Một bước sóng (λ)"}, {"id": "c", "text": "Một phần tư bước sóng (λ/4)"}], "correctOptionId": "a", "explanation": "Hai nút sóng (hoặc hai bụng sóng) liên tiếp cách nhau đúng λ/2."}, {"id": "phy-11-gt-q2", "prompt": "Điều kiện để hai sóng gặp nhau tạo ra hiện tượng giao thoa ổn định là hai nguồn phải?", "difficulty": "MEDIUM", "options": [{"id": "a", "text": "Cùng tần số và hiệu số pha không đổi theo thời gian"}, {"id": "b", "text": "Cùng biên độ và cùng tốc độ"}, {"id": "c", "text": "Phát ra từ cùng một điểm"}], "correctOptionId": "a", "explanation": "Hai nguồn kết hợp phải cùng phương, cùng tần số và có hiệu số pha không đổi."}, {"id": "phy-11-gt-q3", "prompt": "Một sợi dây dài L = 1,2 m hai đầu cố định đang có sóng dừng với 3 bụng sóng. Bước sóng λ trên dây là?", "difficulty": "APPLIED", "options": [{"id": "a", "text": "0,8 m (80 cm)"}, {"id": "b", "text": "0,4 m"}, {"id": "c", "text": "2,4 m"}], "correctOptionId": "a", "explanation": "Với 2 đầu cố định, L = k (λ/2) với k là số bụng. 1,2 = 3 * (λ/2) => λ = 2,4 / 3 = 0,8 m."}]$json$::jsonb,
    '[]'::jsonb
) on conflict(subject_id,grade,unit_id) do nothing;
