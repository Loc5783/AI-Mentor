import { writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

// Manually authored, deterministic Grade 11 science questions. Offline only.
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../..');
const questions=[];
function add(subject,topic,prompt,correct,wrong,explanation){
  const choices=[String(correct),...wrong.map(String)];
  if(choices.some(x=>!x)||new Set(choices).size!==4)throw new Error(`Invalid choices: ${prompt}`);
  const rotation=questions.length%4, options=choices.slice(rotation).concat(choices.slice(0,rotation));
  questions.push({id:`g11-${subject}-authored-${String(questions.filter(q=>q.subject===subject).length+1).padStart(2,'0')}`,subject,topic,prompt,options,correct:'ABCD'[options.indexOf(String(correct))],explanation});
}

for(let i=1;i<=6;i++){
  const A=i+2,w=i+2,answer=A*w;
  add('physics','Dao động điều hòa',`Vật dao động điều hòa có biên độ ${A} cm và tần số góc ${w} rad/s. Tốc độ cực đại là bao nhiêu?`,`${answer} cm/s`,[`${answer+A} cm/s`,`${answer-w} cm/s`,`${answer+2} cm/s`],`vmax = ωA = ${w} × ${A} = ${answer} cm/s.`);
}
for(let i=1;i<=6;i++){
  const v=12+2*i,f=2,answer=v/f;
  add('physics','Sóng cơ',`Sóng truyền với tốc độ ${v} m/s và tần số ${f} Hz. Bước sóng bằng bao nhiêu?`,`${answer} m`,[`${answer+1} m`,`${answer-1} m`,`${v} m`],`λ = v/f = ${v}/${f} = ${answer} m.`);
}
for(let i=1;i<=6;i++){
  const R=i+2,U=R*(i+3),answer=U/R;
  add('physics','Dòng điện không đổi',`Đặt hiệu điện thế ${U} V vào điện trở ${R} Ω. Cường độ dòng điện là bao nhiêu?`,`${answer} A`,[`${answer+1} A`,`${answer-1} A`,`${U+R} A`],`Theo định luật Ohm, I = U/R = ${U}/${R} = ${answer} A.`);
}
for(let i=1;i<=6;i++){
  const q=i+1,answer=i+2,F=q*answer;
  add('physics','Điện trường',`Điện tích thử ${q} μC chịu lực điện ${F} N. Độ lớn cường độ điện trường là bao nhiêu?`,`${answer} × 10⁶ N/C`,[`${answer+1} × 10⁶ N/C`,`${answer-1} × 10⁶ N/C`,`${F*q} × 10⁶ N/C`],`E = F/q = ${F}/(${q} × 10⁻⁶) = ${answer} × 10⁶ N/C.`);
}
for(let i=1;i<=6;i++){
  const C=i+1,U=i+2,answer=C*U;
  add('physics','Tụ điện',`Tụ điện có điện dung ${C} μF được đặt dưới hiệu điện thế ${U} V. Điện tích của tụ là bao nhiêu?`,`${answer} μC`,[`${answer+C} μC`,`${answer-U} μC`,`${C+U} μC`],`Q = CU = ${C} × ${U} = ${answer} μC.`);
}

const equilibria=[
  ['Tăng nồng độ N₂','Chuyển dịch theo chiều thuận','Chuyển dịch theo chiều nghịch','Không chuyển dịch','Khi tăng nồng độ chất tham gia N₂, cân bằng chuyển dịch theo chiều tiêu thụ N₂.'],
  ['Tăng nồng độ NH₃','Chuyển dịch theo chiều nghịch','Chuyển dịch theo chiều thuận','Không chuyển dịch','Khi tăng nồng độ sản phẩm NH₃, cân bằng chuyển dịch theo chiều nghịch để tiêu thụ NH₃.'],
  ['Tăng áp suất của hệ','Chuyển dịch theo chiều thuận','Chuyển dịch theo chiều nghịch','Không chuyển dịch','Vế trái có 4 mol khí, vế phải có 2 mol khí; tăng áp suất làm cân bằng dịch về phía có ít mol khí hơn.'],
  ['Giảm áp suất của hệ','Chuyển dịch theo chiều nghịch','Chuyển dịch theo chiều thuận','Không chuyển dịch','Giảm áp suất làm cân bằng dịch về phía có số mol khí lớn hơn, tức chiều nghịch.'],
  ['Lấy bớt NH₃ ra khỏi hệ','Chuyển dịch theo chiều thuận','Chuyển dịch theo chiều nghịch','Không chuyển dịch','Lấy bớt sản phẩm NH₃ khiến cân bằng dịch theo chiều thuận để tạo thêm NH₃.'],
  ['Thêm chất xúc tác','Không làm chuyển dịch cân bằng','Chuyển dịch theo chiều thuận','Chuyển dịch theo chiều nghịch','Chất xúc tác làm tăng tốc độ hai chiều như nhau, không làm thay đổi vị trí cân bằng.'],
];
for(const [change,correct,w1,w2,why] of equilibria)add('chemistry','Cân bằng hóa học',`Với cân bằng tỏa nhiệt N₂(k) + 3H₂(k) ⇌ 2NH₃(k) + nhiệt, khi ${change.toLowerCase()} thì hệ thay đổi thế nào?`,correct,[w1,w2,'Phản ứng dừng hẳn'],why);

for(let p=1;p<=6;p++){
  const correct=String(p), wrong=[String(p+1),String(p+2),String(p+3)];
  const superscript=['⁻¹','⁻²','⁻³','⁻⁴','⁻⁵','⁻⁶'][p-1];
  add('chemistry','Cân bằng trong dung dịch nước',`Dung dịch HCl 10${superscript} M (phân li hoàn toàn) có pH bằng bao nhiêu?`,correct,wrong,`HCl là acid mạnh một nấc nên [H⁺] = 10${superscript} M; pH = ${p}.`);
}

const nitrogen=[
  ['Khi tiếp xúc với quỳ tím ẩm, khí NH₃ làm giấy đổi sang màu nào?','Xanh','Đỏ','Không đổi màu','NH₃ tan trong nước tạo môi trường base, làm quỳ tím chuyển xanh.'],
  ['Liên kết trong phân tử N₂ bền vững chủ yếu do đâu?','Có liên kết ba giữa hai nguyên tử N','Có liên kết đơn dễ đứt','Phân tử có liên kết ion','Liên kết ba N≡N có năng lượng liên kết lớn nên N₂ khá trơ ở điều kiện thường.'],
  ['Khói trắng tạo ra khi NH₃ gặp HCl đặc chủ yếu là chất nào?','NH₄Cl','NH₄OH','N₂H₄','NH₃ + HCl tạo muối ammonium chloride NH₄Cl dạng hạt nhỏ màu trắng.'],
  ['Trong quy trình Haber điều chế NH₃, chất nào thường dùng làm xúc tác?','Sắt','Đồng','Natri chloride','Quy trình Haber sử dụng xúc tác sắt, cùng nhiệt độ và áp suất thích hợp.'],
  ['Trong không khí khô, nitrogen chiếm gần bao nhiêu phần trăm thể tích?','78%','21%','1%','Nitrogen chiếm khoảng 78% thể tích không khí khô; oxygen khoảng 21%.'],
  ['Sản phẩm khử phổ biến của nitrogen khi phản ứng với hydrogen trong quy trình Haber là gì?','Ammonia (NH₃)','Nitric acid (HNO₃)','Nitrogen dioxide (NO₂)','Phản ứng tổng hợp của N₂ với H₂ tạo ammonia: N₂ + 3H₂ ⇌ 2NH₃.'],
];
for(const [prompt,correct,w1,w2,why] of nitrogen)add('chemistry','Nitrogen và Ammonia',prompt,correct,[w1,w2,'Không có sản phẩm'],why);

const organic=[
  ['Dãy đồng đẳng alkane mạch hở có công thức chung nào?','CₙH₂ₙ₊₂','CₙH₂ₙ','CₙH₂ₙ₋₂','Alkane no, mạch hở có công thức CₙH₂ₙ₊₂.'],
  ['Các chất có cùng công thức phân tử nhưng khác công thức cấu tạo được gọi là gì?','Đồng phân cấu tạo','Đồng vị','Đồng đẳng','Đồng phân cấu tạo có cùng công thức phân tử nhưng cách liên kết giữa các nguyên tử khác nhau.'],
  ['Hai chất liên tiếp trong một dãy đồng đẳng thường hơn kém nhau nhóm nguyên tử nào?','–CH₂–','–OH','–COOH','Các chất liên tiếp trong dãy đồng đẳng hơn kém nhau một hay nhiều nhóm –CH₂–.'],
  ['Hydrocarbon no mạch hở chỉ chứa loại liên kết nào giữa các nguyên tử carbon?','Liên kết đơn','Liên kết đôi','Liên kết ba','Hydrocarbon no chỉ có liên kết đơn C–C trong mạch carbon.'],
  ['Chất nào là alkene đơn giản nhất?','Ethene (C₂H₄)','Methane (CH₄)','Ethyne (C₂H₂)','Ethene có một liên kết đôi C=C và là alkene đơn giản nhất.'],
  ['Alkene thường làm mất màu dung dịch nào do phản ứng cộng?','Bromine','Natri chloride','Nước vôi trong','Liên kết đôi C=C cộng Br₂, làm dung dịch bromine bị mất màu.'],
];
for(const [prompt,correct,w1,w2,why] of organic)add('chemistry','Đại cương hữu cơ và Hydrocarbon',prompt,correct,[w1,w2,'Không thuộc hợp chất hữu cơ'],why);

const hydrocarbon=[
  ['Để phân biệt ethene và methane bằng phản ứng đặc trưng, có thể dùng thuốc thử nào?','Dung dịch bromine','Dung dịch NaCl','Nước cất','Ethene cộng Br₂ làm mất màu dung dịch bromine; methane không phản ứng trong điều kiện thường.'],
  ['Sản phẩm chính khi propene cộng HBr theo quy tắc Markovnikov là chất nào?','2-bromopropane','1-bromopropane','1,2-dibromopropane','Theo quy tắc Markovnikov, H cộng vào carbon đang có nhiều H hơn; Br gắn vào carbon giữa, tạo 2-bromopropane.'],
  ['Đốt cháy hoàn toàn methane trong oxygen tạo ra những sản phẩm nào?','CO₂ và H₂O','CO và H₂','C và H₂O','Phản ứng cháy hoàn toàn CH₄ + 2O₂ → CO₂ + 2H₂O.'],
  ['Phản ứng đặc trưng của alkane với chlorine dưới ánh sáng là loại phản ứng nào?','Thế','Cộng','Trùng hợp','Alkane thường tham gia phản ứng thế gốc tự do với halogen khi có ánh sáng.'],
  ['Hydrogen hóa hoàn toàn propene với xúc tác Ni tạo ra chất nào?','Propane','Propyne','Ethanol','H₂ cộng vào liên kết đôi C=C của propene tạo alkane propane.'],
  ['Alkene mạch hở có một liên kết đôi thường có công thức chung nào?','CₙH₂ₙ','CₙH₂ₙ₊₂','CₙH₂ₙ₋₂','Alkene mạch hở có một liên kết đôi có công thức chung CₙH₂ₙ.'],
];
for(const [prompt,correct,w1,w2,why] of hydrocarbon)add('chemistry','Phản ứng của Hydrocarbon',prompt,correct,[w1,w2,'Không xảy ra phản ứng'],why);

if(questions.length!==60)throw new Error(`Expected 60 questions, got ${questions.length}`);
for(const subject of ['physics','chemistry']){
  const list=questions.filter(q=>q.subject===subject);
  if(list.length!==30||new Set(list.map(q=>q.prompt)).size!==30)throw new Error(`Invalid ${subject} question count/duplicates`);
}
function sql(s){return `'${String(s).replaceAll("'","''")}'`;}
const data=JSON.stringify(questions.map(q=>({id:q.id,subject_id:q.subject,topic:q.topic,prompt:q.prompt,option_a:q.options[0],option_b:q.options[1],option_c:q.options[2],option_d:q.options[3],correct_option:q.correct,explanation:q.explanation})));
const output=`begin;
-- Sixty authored Grade 11 Physics and Chemistry questions; no model API used.
with seed as (select * from jsonb_to_recordset(${sql(data)}::jsonb) as q(
  id text,subject_id text,topic text,prompt text,option_a text,option_b text,
  option_c text,option_d text,correct_option text,explanation text
))
insert into public.mentor_questions(
  id,code,subject_id,grade,chapter,topic,textbook,prompt,option_a,option_b,option_c,option_d,
  correct_option,explanation,difficulty,points,source,author,license,status
)
select id,upper(replace(id,'-','_')),subject_id,11,topic,topic,'Kết nối tri thức',prompt,
  option_a,option_b,option_c,option_d,correct_option,explanation,'EASY',10,
  'AI Mentor tự biên soạn theo công thức và nội dung Kết nối tri thức lớp 11; đáp án kiểm tra tự động',
  'AI Mentor','Nội bộ','PUBLISHED' from seed
on conflict(id) do nothing;

do $$
declare s text; n integer;
begin
  foreach s in array array['physics','chemistry'] loop
    select count(*) into n from public.mentor_questions where subject_id=s and grade=11 and status='PUBLISHED';
    if n<50 then raise exception 'Grade 11 % bank has only % published questions',s,n; end if;
    select count(*) into n from public.mentor_questions where subject_id=s and grade=11 and status='PUBLISHED'
      and (trim(prompt)='' or trim(option_a)='' or trim(option_b)='' or trim(option_c)='' or trim(option_d)=''
        or option_d in(option_a,option_b,option_c) or correct_option not in('A','B','C','D') or trim(explanation)='');
    if n>0 then raise exception 'Grade 11 % has % malformed published questions',s,n; end if;
  end loop;
end $$;
commit;
`;
writeFileSync(resolve(root,'supabase/migrations/202611160019_grade11_physics_chemistry_to_50.sql'),output,'utf8');
console.log(`Generated ${questions.length} Grade 11 questions: 30 Physics + 30 Chemistry.`);
