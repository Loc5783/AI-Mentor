import { writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

// Deterministic offline questions: no Gemini or other network service.
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const migration = resolve(root, 'supabase/migrations/202611160016_grade11_math_bank_to_50.sql');
const questions = [];
function add(topic, prompt, correct, distractors, explanation) {
  const values = [String(correct), ...distractors.map(String)];
  if (new Set(values).size !== 4) throw new Error(`Duplicate choices for: ${prompt}`);
  const rotate = questions.length % 4;
  const options = values.slice(rotate).concat(values.slice(0, rotate));
  questions.push({
    id: `g11-math-authored-${String(questions.length + 1).padStart(2, '0')}`,
    topic, prompt, options,
    correct: 'ABCD'[options.indexOf(String(correct))], explanation,
  });
}

// Trigonometric values at standard angles.
const trig = [
  ['0', '0'], ['π/6', '1/2'], ['π/4', '√2/2'], ['π/3', '√3/2'],
  ['π/2', '1'], ['2π/3', '√3/2'], ['3π/4', '√2/2'], ['5π/6', '1/2'], ['π', '0'],
];
const trigValues = ['-1', '-√3/2', '-√2/2', '-1/2', '0', '1/2', '√2/2', '√3/2', '1'];
trig.forEach(([angle, value], i) => {
  const wrong = trigValues.filter((x) => x !== value);
  const start = (i * 2) % (wrong.length - 2);
  add('Hàm số lượng giác', `Giá trị của sin(${angle}) bằng bao nhiêu?`, value,
    [wrong[start], wrong[start + 1], wrong[start + 2]],
    `Dựa vào các giá trị lượng giác của góc đặc biệt, sin(${angle}) = ${value}.`);
});

// Arithmetic progressions with distinct data and directly checkable terms.
for (let i = 1; i <= 9; i++) {
  const u1 = i + 1, d = (i % 4) + 2, n = i + 3;
  const answer = u1 + (n - 1) * d;
  add('Cấp số cộng', `Một cấp số cộng có u₁ = ${u1}, công sai d = ${d}. Tính u_${n}.`, answer,
    [answer + d, answer - d, u1 + n * d + 1],
    `uₙ = u₁ + (n − 1)d = ${u1} + (${n} − 1)·${d} = ${answer}.`);
}

// Rational limits at infinity; coefficients are chosen so every result is integral.
for (let i = 1; i <= 9; i++) {
  const a = 2 * (i + 1), b = i + 3, d = 2 * i + 5, answer = i + 1;
  add('Giới hạn dãy số', `Tính lim khi n → +∞ của (${a}n + ${b}) / (2n + ${d}).`, answer,
    [answer + 1, answer - 1, b],
    `Chia tử và mẫu cho n; các hạng chứa 1/n tiến về 0, nên giới hạn bằng ${a}/2 = ${answer}.`);
}

// Polynomial derivatives evaluated at integer points.
for (let i = 1; i <= 9; i++) {
  const x = i + 1, b = (i % 5) + 1, answer = 2 * x + b;
  add('Đạo hàm', `Cho f(x) = x² + ${b}x − 4. Giá trị f'(${x}) bằng bao nhiêu?`, answer,
    [answer + 2, answer - 2, x * x + b + 3],
    `f'(x) = 2x + ${b}; do đó f'(${x}) = 2·${x} + ${b} = ${answer}.`);
}

// Core Grade 11 spatial-geometry facts, each with one unambiguous correct statement.
const geometry = [
  ['Nếu hai điểm phân biệt của một đường thẳng cùng thuộc một mặt phẳng thì đường thẳng đó có quan hệ nào với mặt phẳng?', 'Nằm trong mặt phẳng', ['Song song với mặt phẳng', 'Vuông góc với mặt phẳng', 'Không có điểm chung với mặt phẳng'], 'Một mặt phẳng chứa hai điểm phân biệt của đường thẳng thì chứa cả đường thẳng qua hai điểm ấy.'],
  ['Hai mặt phẳng phân biệt có một điểm chung thì giao tuyến của chúng là gì?', 'Một đường thẳng', ['Một điểm duy nhất', 'Một đoạn thẳng hữu hạn', 'Hai đường thẳng song song'], 'Hai mặt phẳng phân biệt nếu có điểm chung thì giao nhau theo một đường thẳng.'],
  ['Qua một điểm nằm ngoài một đường thẳng, có bao nhiêu mặt phẳng chứa đường thẳng và điểm đó?', 'Đúng một mặt phẳng', ['Không có mặt phẳng nào', 'Đúng hai mặt phẳng', 'Vô số mặt phẳng'], 'Một đường thẳng và một điểm không thuộc nó xác định duy nhất một mặt phẳng.'],
  ['Hai đường thẳng song song phân biệt xác định bao nhiêu mặt phẳng?', 'Đúng một mặt phẳng', ['Không có mặt phẳng nào', 'Đúng hai mặt phẳng', 'Vô số mặt phẳng'], 'Hai đường thẳng song song phân biệt cùng nằm trong đúng một mặt phẳng.'],
  ['Nếu đường thẳng d song song với mặt phẳng (P) và d không nằm trong (P), d có bao nhiêu điểm chung với (P)?', 'Không có điểm chung', ['Một điểm chung', 'Vô số điểm chung', 'Hai điểm chung'], 'Đường thẳng song song với mặt phẳng và không nằm trong mặt phẳng thì không cắt mặt phẳng.'],
  ['Hai mặt phẳng song song bị cắt bởi mặt phẳng thứ ba thì hai giao tuyến có quan hệ nào?', 'Song song với nhau', ['Cắt nhau vuông góc', 'Trùng nhau', 'Chéo nhau'], 'Các giao tuyến của một mặt phẳng với hai mặt phẳng song song là hai đường thẳng song song.'],
  ['Đường thẳng vuông góc với mặt phẳng tại H vuông góc với đường nào trong mặt phẳng?', 'Mọi đường thẳng của mặt phẳng đi qua H', ['Chỉ đường thẳng song song với nó', 'Chỉ đường thẳng không đi qua H', 'Không đường thẳng nào'], 'Theo định nghĩa, đường thẳng vuông góc mặt phẳng tại H vuông góc mọi đường trong mặt phẳng đi qua H.'],
  ['Nếu đường thẳng a song song với đường thẳng b nằm trong mặt phẳng (P), đồng thời a không thuộc (P), thì a và (P) có quan hệ nào?', 'Song song', ['Vuông góc', 'Cắt tại hai điểm', 'Nằm trong mặt phẳng'], 'Đường thẳng song song với một đường nằm trong mặt phẳng và không nằm trong mặt phẳng ấy thì song song với mặt phẳng.'],
  ['Hai mặt phẳng phân biệt cùng vuông góc với một đường thẳng thì có quan hệ nào?', 'Song song với nhau', ['Vuông góc với nhau', 'Cắt nhau theo đường thẳng vuông góc với đường đã cho', 'Trùng nhau'], 'Hai mặt phẳng cùng vuông góc với một đường thẳng thì song song với nhau.'],
];
geometry.forEach(([prompt, correct, wrong, explanation]) => add('Hình học không gian', prompt, correct, wrong, explanation));

if (questions.length !== 45) throw new Error(`Expected 45 additions, got ${questions.length}`);
const prompts = new Set(questions.map((q) => q.prompt));
if (prompts.size !== questions.length) throw new Error('Duplicate prompts');

function sql(value) { return `'${String(value).replaceAll("'", "''")}'`; }
const payload = JSON.stringify(questions.map((q) => ({
  id: q.id, topic: q.topic, prompt: q.prompt,
  option_a: q.options[0], option_b: q.options[1], option_c: q.options[2], option_d: q.options[3],
  correct_option: q.correct, explanation: q.explanation,
})));

const output = `begin;
-- Forty-five authored, deterministic Grade 11 math questions; no Gemini is used.
with seed as (
  select * from jsonb_to_recordset(${sql(payload)}::jsonb) as q(
    id text, topic text, prompt text, option_a text, option_b text,
    option_c text, option_d text, correct_option text, explanation text
  )
)
insert into public.mentor_questions(
  id, code, subject_id, grade, chapter, topic, textbook, prompt,
  option_a, option_b, option_c, option_d, correct_option, explanation,
  difficulty, points, source, author, license, status
)
select id, upper(replace(id, '-', '_')), 'math', 11, topic, topic,
  'Kết nối tri thức', prompt, option_a, option_b, option_c, option_d,
  correct_option, explanation, 'EASY', 10,
  'AI Mentor tự biên soạn theo quy tắc Toán 11; đáp án kiểm tra được',
  'AI Mentor', 'Nội bộ', 'PUBLISHED'
from seed
on conflict (id) do nothing;

do $$
declare inserted_count integer;
begin
  select count(*) into inserted_count from public.mentor_questions
  where id between 'g11-math-authored-01' and 'g11-math-authored-45'
    and status = 'PUBLISHED';
  if inserted_count <> 45 then
    raise exception 'Expected 45 published Math 11 additions; found %', inserted_count;
  end if;
end $$;
commit;
`;
writeFileSync(migration, output, 'utf8');
console.log(`Generated ${questions.length} unique Grade 11 math questions.`);
