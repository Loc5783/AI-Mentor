begin;

-- Independently authored, four-choice foundation questions. The imported
-- three-choice lesson questions stay in DRAFT until their fourth options are
-- reviewed; never publish their editorial placeholder as an answer.
with seed(id, subject_id, prompt, option_a, option_b, option_c, option_d, correct_option, explanation) as (
  values
  ('g10-found-math-01','math','Giải phương trình 2x + 3 = 11. Giá trị của x là bao nhiêu?','2','3','4','5','C','Trừ 3 ở hai vế rồi chia cho 2: x = 4.'),
  ('g10-found-math-02','math','Tập hợp {1, 2} hợp với tập hợp {2, 3} bằng tập hợp nào?','{2}','{1, 3}','{1, 2, 3}','{1, 2, 4}','C','Phép hợp lấy mỗi phần tử xuất hiện trong ít nhất một tập và không lặp lại phần tử.'),
  ('g10-found-math-03','math','Giá trị của sin 30° là bao nhiêu?','0','1/2','√2/2','1','B','Trong tam giác vuông có góc 30°, cạnh đối diện góc ấy bằng một nửa cạnh huyền.'),
  ('g10-found-math-04','math','Biệt thức Δ của phương trình x² - 4x + 3 = 0 là bao nhiêu?','1','2','4','16','C','Với a = 1, b = -4, c = 3, ta có Δ = b² - 4ac = 16 - 12 = 4.'),
  ('g10-found-math-05','math','Đỉnh của đồ thị hàm số y = x² là điểm nào?','(0; 0)','(1; 0)','(0; 1)','(-1; 0)','A','Parabol y = x² có đỉnh tại gốc tọa độ.'),

  ('g10-found-chem-01','chemistry','Số hiệu nguyên tử của một nguyên tố cho biết số hạt nào trong hạt nhân?','Neutron','Proton','Electron','Phân tử','B','Số hiệu nguyên tử Z bằng số proton trong hạt nhân.'),
  ('g10-found-chem-02','chemistry','Hai đồng vị của cùng một nguyên tố luôn có cùng số hạt nào?','Neutron','Proton','Số khối','Tổng proton và neutron','B','Đồng vị có cùng số proton nhưng khác số neutron.'),
  ('g10-found-chem-03','chemistry','Nguyên tử natri có Z = 11. Sự phân bố electron theo các lớp từ trong ra ngoài là gì?','2, 8, 1','2, 7, 2','2, 9','8, 2, 1','A','11 electron phân bố lần lượt 2 ở lớp thứ nhất, 8 ở lớp thứ hai, 1 ở lớp thứ ba.'),
  ('g10-found-chem-04','chemistry','Các nguyên tố cùng một chu kì trong bảng tuần hoàn có cùng đặc điểm nào?','Số proton','Số lớp electron','Số electron hóa trị','Nguyên tử khối','B','Số thứ tự chu kì tương ứng với số lớp electron của nguyên tử.'),
  ('g10-found-chem-05','chemistry','Liên kết chủ yếu giữa ion Na⁺ và ion Cl⁻ trong tinh thể NaCl là gì?','Liên kết kim loại','Liên kết cộng hóa trị không cực','Liên kết ion','Liên kết hydrogen','C','Lực hút tĩnh điện giữa hai ion trái dấu tạo liên kết ion.'),

  ('g10-found-bio-01','biology','Cấu trúc nào kiểm soát chọn lọc các chất đi vào và ra khỏi tế bào?','Màng sinh chất','Nhân tế bào','Ribosome','Thành tế bào','A','Màng sinh chất có tính thấm chọn lọc, điều tiết trao đổi chất với môi trường.'),
  ('g10-found-bio-02','biology','Trong phân tử DNA, base adenine (A) bắt cặp với base nào?','Guanine (G)','Cytosine (C)','Thymine (T)','Uracil (U)','C','Theo nguyên tắc bổ sung của DNA, A bắt cặp với T.'),
  ('g10-found-bio-03','biology','Bào quan nào trực tiếp tham gia tổng hợp protein?','Lục lạp','Ribosome','Không bào','Lysosome','B','Ribosome là nơi các amino acid được nối thành chuỗi polypeptide.'),
  ('g10-found-bio-04','biology','Bào quan nào là nơi diễn ra phần lớn quá trình tạo ATP trong hô hấp tế bào nhân thực?','Ti thể','Bộ máy Golgi','Nhân','Lưới nội chất','A','Ti thể thực hiện các giai đoạn tạo phần lớn ATP của hô hấp hiếu khí.'),
  ('g10-found-bio-05','biology','Thẩm thấu là sự di chuyển của chất nào qua màng bán thấm?','Protein','Glucose','Nước','DNA','C','Thẩm thấu là sự khuếch tán của nước qua màng bán thấm.'),

  ('g10-found-lit-01','literature','Trong văn bản tự sự, người kể chuyện xưng “tôi” thường thuộc ngôi kể nào?','Ngôi thứ nhất','Ngôi thứ hai','Ngôi thứ ba','Không có ngôi kể','A','Người kể trực tiếp xưng tôi là dấu hiệu của ngôi kể thứ nhất.'),
  ('g10-found-lit-02','literature','Câu “Mặt trời là quả cầu lửa” sử dụng biện pháp tu từ nào?','Nói giảm','Ẩn dụ','Liệt kê','Điệp ngữ','B','Câu gọi mặt trời bằng hình ảnh quả cầu lửa dựa trên nét tương đồng.'),
  ('g10-found-lit-03','literature','Trong bài văn nghị luận, luận điểm là gì?','Thông tin về tác giả','Ý kiến chính cần được làm sáng tỏ','Một câu trích dẫn bất kỳ','Tên của văn bản','B','Luận điểm thể hiện ý kiến, tư tưởng mà người viết muốn chứng minh hoặc làm rõ.'),
  ('g10-found-lit-04','literature','Dẫn chứng trong bài nghị luận chủ yếu dùng để làm gì?','Thay thế luận điểm','Làm rõ và tăng sức thuyết phục cho luận điểm','Trang trí bài viết','Kéo dài số chữ','B','Dẫn chứng cụ thể giúp người đọc có cơ sở đánh giá luận điểm.'),
  ('g10-found-lit-05','literature','Khi tóm tắt một văn bản, điều nào cần được giữ lại?','Mọi câu chữ của bản gốc','Các ý chính và mạch nội dung','Chỉ câu kết','Chỉ thông tin về tác giả','B','Tóm tắt rút gọn văn bản nhưng vẫn giữ các ý chính và quan hệ giữa chúng.'),

  ('g10-found-eng-01','english','Choose the past simple form of “go”.','goed','went','gone','going','B','“Went” is the past simple form of the irregular verb “go”.'),
  ('g10-found-eng-02','english','Complete the sentence: If it rains, we ___ at home.','stayed','will stay','staying','had stayed','B','In a first conditional sentence, the main clause uses “will” plus the base verb.'),
  ('g10-found-eng-03','english','Choose the comparative form of “tall”.','tallest','more tall','taller','most tall','C','A one-syllable adjective normally adds “-er” in the comparative form.'),
  ('g10-found-eng-04','english','Complete the sentence: She has lived here ___ 2020.','for','since','during','at','B','“Since” introduces the starting point of a period continuing to the present.'),
  ('g10-found-eng-05','english','Which sentence is in the passive voice?','Lan writes the letter.','Lan is writing the letter.','The letter was written by Lan.','Lan wrote a letter.','C','“Was written” is a past passive verb form: was + past participle.'),

  ('g10-found-hist-01','history','Ai lãnh đạo chiến thắng Bạch Đằng năm 938?','Ngô Quyền','Lý Thường Kiệt','Trần Hưng Đạo','Quang Trung','A','Ngô Quyền đánh bại quân Nam Hán trên sông Bạch Đằng năm 938.'),
  ('g10-found-hist-02','history','Năm 1010, vua Lý Thái Tổ dời đô tới địa điểm nào?','Hoa Lư','Phú Xuân','Thăng Long','Tây Đô','C','Chiếu dời đô đưa kinh đô từ Hoa Lư ra Đại La, được đổi tên thành Thăng Long.'),
  ('g10-found-hist-03','history','Nhà nước Văn Lang gắn với thời đại của ai?','Các vua Hùng','Các vua Lý','Các vua Trần','Các vua Nguyễn','A','Theo truyền thống lịch sử, các vua Hùng đứng đầu nhà nước Văn Lang.'),
  ('g10-found-hist-04','history','Bản Tuyên ngôn Độc lập khai sinh nước Việt Nam Dân chủ Cộng hòa được công bố vào ngày nào?','2/9/1945','19/8/1945','30/4/1975','7/5/1954','A','Chủ tịch Hồ Chí Minh đọc Tuyên ngôn Độc lập ngày 2/9/1945 tại Hà Nội.'),
  ('g10-found-hist-05','history','Chiến thắng Điện Biên Phủ kết thúc vào năm nào?','1945','1954','1968','1975','B','Chiến dịch Điện Biên Phủ giành thắng lợi ngày 7/5/1954.'),

  ('g10-found-geo-01','geography','Vĩ tuyến gốc (Xích đạo) có vĩ độ bằng bao nhiêu?','0°','23,5°','66,5°','90°','A','Xích đạo được quy ước là vĩ tuyến 0°.'),
  ('g10-found-geo-02','geography','Sự luân phiên ngày và đêm trên Trái Đất chủ yếu do chuyển động nào?','Trái Đất tự quay quanh trục','Trái Đất quay quanh Mặt Trời','Mặt Trăng quay quanh Trái Đất','Mặt Trời quay quanh Trái Đất','A','Trái Đất tự quay khiến các khu vực lần lượt được chiếu sáng và khuất sáng.'),
  ('g10-found-geo-03','geography','Trên bản đồ tỉ lệ 1:100 000, 1 cm trên bản đồ tương ứng bao nhiêu ngoài thực địa?','100 m','1 km','10 km','100 km','B','100 000 cm ngoài thực địa bằng 1 000 m, tức 1 km.'),
  ('g10-found-geo-04','geography','Tầng khí quyển nào diễn ra phần lớn hiện tượng thời tiết?','Tầng đối lưu','Tầng bình lưu','Tầng trung lưu','Tầng nhiệt','A','Mây, mưa và phần lớn hiện tượng thời tiết hình thành trong tầng đối lưu.'),
  ('g10-found-geo-05','geography','Kinh tuyến gốc được quy ước có kinh độ bằng bao nhiêu?','0°','30° Đông','90° Tây','180°','A','Kinh tuyến đi qua đài thiên văn Greenwich được quy ước là kinh tuyến 0°.'),

  ('g10-found-law-01','economics_law','Trong kinh tế học, chi phí cơ hội là gì?','Toàn bộ số tiền đang có','Giá trị của phương án tốt nhất bị từ bỏ khi lựa chọn','Tiền thuế phải nộp','Giá niêm yết của sản phẩm','B','Khi chọn một phương án, ta từ bỏ lợi ích của phương án tốt nhất còn lại.'),
  ('g10-found-law-02','economics_law','Khi nguồn lực có hạn nhưng nhu cầu đa dạng, con người cần làm gì?','Ngừng mọi hoạt động kinh tế','Lựa chọn cách sử dụng nguồn lực','Chỉ dùng hàng miễn phí','Bỏ qua mọi chi phí','B','Sự khan hiếm đòi hỏi lựa chọn cách phân bổ nguồn lực.'),
  ('g10-found-law-03','economics_law','Văn bản nào có hiệu lực pháp lý cao nhất trong hệ thống pháp luật Việt Nam?','Nội quy lớp học','Thông tư','Hiến pháp','Hợp đồng dân sự','C','Hiến pháp là luật cơ bản, có hiệu lực pháp lý cao nhất.'),
  ('g10-found-law-04','economics_law','Khi giá một mặt hàng tăng và các yếu tố khác không đổi, lượng cầu của mặt hàng đó thường thay đổi thế nào?','Tăng','Giảm','Luôn bằng không','Không bao giờ thay đổi','B','Theo quy luật cầu thông thường, giá tăng thì lượng cầu giảm nếu các yếu tố khác không đổi.'),
  ('g10-found-law-05','economics_law','Công dân thực hiện quyền của mình cần tôn trọng điều gì?','Chỉ ý muốn của bản thân','Quyền và lợi ích hợp pháp của người khác','Chỉ nội quy riêng của nhóm','Không cần giới hạn nào','B','Việc thực hiện quyền phải phù hợp pháp luật và tôn trọng quyền, lợi ích hợp pháp của người khác.')
)
insert into public.mentor_questions (
  id, code, subject_id, grade, chapter, topic, textbook, prompt,
  option_a, option_b, option_c, option_d, correct_option, explanation,
  difficulty, points, source, author, license, status
)
select id, upper(replace(id, '-', '_')), subject_id, 10, 'Kiến thức nền lớp 10',
  'Ôn tập kiến thức nền', null, prompt, option_a, option_b, option_c,
  option_d, correct_option, explanation, 'EASY', 10,
  'AI Mentor biên soạn', 'AI Mentor', 'Nội bộ', 'PUBLISHED'
from seed
on conflict (id) do nothing;

-- The legacy physics starter quiz has only five questions. Extend it using
-- five published, verified questions absent from that quiz. A session keeps
-- its original snapshot, so an in-progress attempt is not changed.
with additions as (
  select jsonb_agg(jsonb_build_object(
    'id', id, 'prompt', prompt,
    'options', jsonb_build_array(
      jsonb_build_object('id','A','text',option_a),
      jsonb_build_object('id','B','text',option_b),
      jsonb_build_object('id','C','text',option_c),
      jsonb_build_object('id','D','text',option_d)
    ),
    'correctOption', correct_option, 'explanation', explanation,
    'difficulty', difficulty, 'points', points
  ) order by code) as questions
  from public.mentor_questions
  where code in ('AIM_PHY10_M01_Q01','AIM_PHY10_M01_Q02','AIM_PHY10_M01_Q03',
                 'AIM_PHY10_M01_Q04','AIM_PHY10_M01_Q05')
    and status = 'PUBLISHED'
)
update public.mentor_library_quizzes q
set questions = q.questions || a.questions
from additions a
where q.id = 'quiz_phy10_basic'
  and jsonb_array_length(q.questions) = 5
  and jsonb_array_length(a.questions) = 5
  and not exists (
    select 1 from jsonb_array_elements(q.questions) existing
    join jsonb_array_elements(a.questions) added on existing->>'id' = added->>'id'
  );

commit;
