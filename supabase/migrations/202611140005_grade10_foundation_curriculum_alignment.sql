begin;

-- Correct foundation items that were general knowledge rather than explicit
-- Grade 10 lesson topics. Keep the stable IDs so existing references survive.
with corrections(id, prompt, option_a, option_b, option_c, option_d, correct_option, explanation) as (
  values
  ('g10-found-math-01','Giải bất phương trình 2x + 3 > 11. Điều kiện của x là gì?','x > 2','x > 3','x > 4','x < 4','C','Trừ 3 ở hai vế rồi chia cho 2 dương: x > 4.'),
  ('g10-found-eng-04','Complete the sentence: You ___ wear a helmet when riding a motorbike.','can','must','may','might','B','“Must” expresses an obligation or rule.'),
  ('g10-found-hist-01','Đâu là ví dụ về tư liệu hiện vật dùng để nghiên cứu lịch sử?','Trống đồng được khai quật','Truyện kể truyền miệng','Bài hồi kí','Bản ghi âm phỏng vấn','A','Trống đồng là di vật vật chất còn lại từ quá khứ nên thuộc tư liệu hiện vật.'),
  ('g10-found-hist-02','Năm 1500 trước Công nguyên thuộc thiên niên kỉ nào?','Thiên niên kỉ I TCN','Thiên niên kỉ II TCN','Thiên niên kỉ III TCN','Thiên niên kỉ IV TCN','B','Thiên niên kỉ II TCN kéo dài từ năm 2000 đến năm 1001 TCN.'),
  ('g10-found-hist-03','Nền văn minh Ai Cập cổ đại phát triển gắn với dòng sông nào?','Sông Hằng','Sông Nin','Sông Hoàng Hà','Sông Mê Kông','B','Sông Nin cung cấp nước và phù sa cho cư dân Ai Cập cổ đại.'),
  ('g10-found-hist-04','Chữ cái Latin gắn với di sản của nền văn minh cổ đại nào?','Ấn Độ','Trung Hoa','La Mã','Ai Cập','C','Chữ Latin được sử dụng và truyền bá rộng rãi qua nền văn minh La Mã.'),
  ('g10-found-hist-05','Nguồn năng lượng tiêu biểu cho Cách mạng công nghiệp lần thứ nhất là gì?','Hơi nước','Điện','Năng lượng hạt nhân','Năng lượng mặt trời','A','Máy hơi nước là động lực tiêu biểu của Cách mạng công nghiệp lần thứ nhất.')
)
update public.mentor_questions q
set prompt = c.prompt, option_a = c.option_a, option_b = c.option_b,
    option_c = c.option_c, option_d = c.option_d,
    correct_option = c.correct_option, explanation = c.explanation,
    updated_at = now()
from corrections c
where q.id = c.id and q.grade = 10 and q.status = 'PUBLISHED';

-- Classify each new item under an actual Grade 10 lesson already in the app.
with lesson_topics(id, lesson_title) as (
  values
  ('g10-found-math-01','Bất phương trình bậc nhất một ẩn'),
  ('g10-found-math-02','Tập hợp và cách biểu diễn'),
  ('g10-found-math-03','Tỉ số lượng giác trong tam giác vuông'),
  ('g10-found-math-04','Phương trình bậc hai'),
  ('g10-found-math-05','Hàm số bậc hai và đồ thị'),
  ('g10-found-chem-01','Số hiệu nguyên tử'),
  ('g10-found-chem-02','Đồng vị'),
  ('g10-found-chem-03','Viết cấu hình electron đơn giản'),
  ('g10-found-chem-04','Chu kì trong bảng tuần hoàn'),
  ('g10-found-chem-05','Liên kết ion'),
  ('g10-found-bio-01','Màng sinh chất'),
  ('g10-found-bio-02','DNA và RNA'),
  ('g10-found-bio-03','Sự phối hợp của các bào quan'),
  ('g10-found-bio-04','Hô hấp tế bào'),
  ('g10-found-bio-05','Vận chuyển qua màng'),
  ('g10-found-lit-01','Người kể chuyện và điểm nhìn'),
  ('g10-found-lit-02','Biện pháp tu từ và tác dụng'),
  ('g10-found-lit-03','Luận đề, luận điểm và lí lẽ'),
  ('g10-found-lit-04','Luận đề, luận điểm và lí lẽ'),
  ('g10-found-lit-05','Đọc văn bản thông tin'),
  ('g10-found-eng-01','Past simple và mốc thời gian đã kết thúc'),
  ('g10-found-eng-02','Câu điều kiện loại 0 và loại 1'),
  ('g10-found-eng-03','So sánh hơn'),
  ('g10-found-eng-04','Modal verbs: can, should, must'),
  ('g10-found-eng-05','Câu bị động cơ bản'),
  ('g10-found-hist-01','Lịch sử và bằng chứng'),
  ('g10-found-hist-02','Niên đại và dòng thời gian'),
  ('g10-found-hist-03','Những nền văn minh cổ đại ven sông'),
  ('g10-found-hist-04','Di sản Hy Lạp và La Mã'),
  ('g10-found-hist-05','Các cuộc cách mạng công nghiệp'),
  ('g10-found-geo-01','Bản đồ và tỉ lệ'),
  ('g10-found-geo-02','Chuyển động Trái Đất và mùa'),
  ('g10-found-geo-03','Bản đồ và tỉ lệ'),
  ('g10-found-geo-04','Khí quyển và nhiệt độ'),
  ('g10-found-geo-05','Bản đồ và tỉ lệ'),
  ('g10-found-law-01','Nhu cầu, khan hiếm và chi phí cơ hội'),
  ('g10-found-law-02','Nhu cầu, khan hiếm và chi phí cơ hội'),
  ('g10-found-law-03','Pháp luật và quy tắc xã hội'),
  ('g10-found-law-04','Cung, cầu và cân bằng thị trường'),
  ('g10-found-law-05','Quyền và nghĩa vụ công dân')
)
update public.mentor_questions q
set chapter = t.lesson_title, topic = t.lesson_title,
    source = 'AI Mentor biên soạn theo lộ trình lớp 10', updated_at = now()
from lesson_topics t
where q.id = t.id and q.grade = 10 and q.status = 'PUBLISHED';

commit;
