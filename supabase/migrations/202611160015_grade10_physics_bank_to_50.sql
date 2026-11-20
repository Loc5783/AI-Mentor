begin;

-- Fifteen manually authored, calculation-checkable Physics 10 questions.
-- Together with the existing 35 published questions this brings Physics 10
-- to the requested 50-question target. No AI service is used to generate them.
with seed(id, prompt, option_a, option_b, option_c, option_d, correct_option, explanation) as (
  values
  ('g10-physics-core-036','Một vật đi được 180 m trong 12 s. Tốc độ trung bình của vật là bao nhiêu?','12 m/s','15 m/s','18 m/s','21 m/s','B','Tốc độ trung bình v = s/t = 180/12 = 15 m/s.'),
  ('g10-physics-core-037','Đổi 72 km/h sang m/s được kết quả nào?','15 m/s','18 m/s','20 m/s','25 m/s','C','Đổi đơn vị: 72 km/h = 72 × 1000/3600 = 20 m/s.'),
  ('g10-physics-core-038','Một xe tăng tốc đều từ 4 m/s lên 16 m/s trong 3 s. Gia tốc của xe bằng bao nhiêu?','3 m/s²','4 m/s²','5 m/s²','6 m/s²','B','Gia tốc a = (v − v₀)/t = (16 − 4)/3 = 4 m/s².'),
  ('g10-physics-core-039','Một vật rơi tự do từ trạng thái nghỉ trong 2 s. Lấy g = 10 m/s², tốc độ của vật sau 2 s là bao nhiêu?','10 m/s','15 m/s','20 m/s','25 m/s','C','Vật rơi tự do từ nghỉ có v = gt = 10 × 2 = 20 m/s.'),
  ('g10-physics-core-040','Hợp lực 18 N tác dụng lên vật khối lượng 6 kg. Gia tốc của vật là bao nhiêu?','2 m/s²','3 m/s²','6 m/s²','108 m/s²','B','Theo định luật II Newton, a = F/m = 18/6 = 3 m/s².'),
  ('g10-physics-core-041','Khi xe buýt phanh gấp, hành khách có xu hướng chúi về phía trước chủ yếu do tính chất nào?','Quán tính','Đàn hồi','Dẫn nhiệt','Tán sắc','A','Do quán tính, cơ thể có xu hướng duy trì chuyển động về phía trước khi xe giảm tốc.'),
  ('g10-physics-core-042','Một lực 10 N làm vật dịch chuyển 5 m cùng hướng lực. Công của lực là bao nhiêu?','2 J','15 J','50 J','100 J','C','Khi lực cùng hướng độ dịch chuyển, A = F.s = 10 × 5 = 50 J.'),
  ('g10-physics-core-043','Một động cơ thực hiện công 900 J trong 15 s. Công suất trung bình của động cơ là bao nhiêu?','15 W','45 W','60 W','135 W','C','Công suất P = A/t = 900/15 = 60 W.'),
  ('g10-physics-core-044','Vật khối lượng 2 kg chuyển động với tốc độ 3 m/s. Động năng của vật bằng bao nhiêu?','3 J','6 J','9 J','18 J','C','Động năng Wđ = ½mv² = ½ × 2 × 3² = 9 J.'),
  ('g10-physics-core-045','Vật khối lượng 3 kg ở độ cao 4 m so với mốc thế năng. Lấy g = 10 m/s². Thế năng trọng trường là bao nhiêu?','12 J','40 J','70 J','120 J','D','Thế năng Wt = mgh = 3 × 10 × 4 = 120 J.'),
  ('g10-physics-core-046','Một vật khối lượng 2 kg chuyển động với tốc độ 5 m/s. Độ lớn động lượng của vật là bao nhiêu?','2,5 kg·m/s','7 kg·m/s','10 kg·m/s','25 kg·m/s','C','Động lượng p = mv = 2 × 5 = 10 kg·m/s.'),
  ('g10-physics-core-047','Một lực ma sát trượt tác dụng lên hộp đang trượt trên sàn nằm ngang thường có hướng nào?','Cùng hướng chuyển động','Ngược hướng chuyển động','Thẳng đứng lên trên','Luôn hướng về tâm Trái Đất','B','Lực ma sát trượt có phương tiếp tuyến mặt tiếp xúc và ngược hướng chuyển động tương đối.'),
  ('g10-physics-core-048','Một vật chuyển động tròn đều với tốc độ 6 m/s trên quỹ đạo bán kính 3 m. Gia tốc hướng tâm là bao nhiêu?','2 m/s²','6 m/s²','12 m/s²','18 m/s²','C','Gia tốc hướng tâm aₕₜ = v²/r = 6²/3 = 12 m/s².'),
  ('g10-physics-core-049','Một vật chịu hai lực cùng phương, ngược chiều có độ lớn 12 N và 7 N. Độ lớn hợp lực là bao nhiêu?','5 N','7 N','12 N','19 N','A','Hai lực ngược chiều nên độ lớn hợp lực bằng hiệu: |12 − 7| = 5 N.'),
  ('g10-physics-core-050','Một vật có khối lượng 1,5 kg và thể tích 0,0005 m³. Khối lượng riêng của vật là bao nhiêu?','300 kg/m³','750 kg/m³','1500 kg/m³','3000 kg/m³','D','Khối lượng riêng ρ = m/V = 1,5/0,0005 = 3000 kg/m³.')
), checked as (
  select *,
    (option_a <> option_b and option_a <> option_c and option_a <> option_d
      and option_b <> option_c and option_b <> option_d and option_c <> option_d) as distinct_options
  from seed
)
insert into public.mentor_questions(
  id, code, subject_id, grade, chapter, topic, textbook, prompt,
  option_a, option_b, option_c, option_d, correct_option, explanation,
  difficulty, points, source, author, license, status
)
select id, upper(replace(id, '-', '_')), 'physics', 10, 'Ôn tập Vật lí 10',
  'Động học, động lực học và năng lượng', 'Kết nối tri thức', prompt,
  option_a, option_b, option_c, option_d, correct_option, explanation,
  'EASY', 10, 'AI Mentor tự biên soạn; tính toán và đáp án được kiểm tra tự động',
  'AI Mentor', 'Nội bộ', 'PUBLISHED'
from checked
where distinct_options
  and correct_option in ('A','B','C','D')
  and length(trim(prompt)) > 15
  and length(trim(explanation)) > 15
on conflict (id) do nothing;

do $$
declare inserted_count integer;
begin
  select count(*) into inserted_count from public.mentor_questions
  where id between 'g10-physics-core-036' and 'g10-physics-core-050'
    and status = 'PUBLISHED';
  if inserted_count <> 15 then
    raise exception 'Expected 15 published Physics 10 additions; found %', inserted_count;
  end if;
end $$;

commit;
