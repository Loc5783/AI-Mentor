/** Deterministic, independently authored exercises for the 15 Math 10 units. */
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const output = resolve(root, 'supabase/migrations/20261114134001_grade10_math_question_bank.sql');
const lessons = JSON.parse(readFileSync(resolve(root, 'backend/curriculum/math-10.json'), 'utf8')).lessons;
const pair = (a, b) => `(${a}; ${b})`;
const set = (...items) => `{${items.join(', ')}}`;

export function buildMathQuestions() {
  const questions = [];
  function add(topicNo, variant, prompt, correct, distractors, explanation) {
    const values = [String(correct), ...distractors.map(String)];
    if (new Set(values).size !== 4) throw new Error(`Repeated option: ${topicNo}/${variant}`);
    const rotation = (topicNo + variant) % 4;
    const rotated = values.slice(rotation).concat(values.slice(0, rotation));
    questions.push({
      id: `g10-math-original-${String(topicNo + 1).padStart(2, '0')}-${String(variant).padStart(2, '0')}`,
      topic: lessons[topicNo === 2 ? 3 : topicNo].title, prompt,
      option_a: rotated[0], option_b: rotated[1],
      option_c: rotated[2], option_d: rotated[3],
      correct_option: 'ABCD'[rotated.indexOf(values[0])], explanation,
      difficulty: variant <= 5 ? 'EASY' : 'MEDIUM',
    });
  }

  for (let i = 1; i <= 13; i++) {
    let a = i + 2;
    add(0, i, `Cho A = ${set(a, a + 1)} và B = ${set(a + 1, a + 2)}. Tìm A ∪ B.`,
      set(a, a + 1, a + 2), [set(a + 1), set(a, a + 1), set(a + 1, a + 2)],
      'Hợp của hai tập chứa tất cả phần tử thuộc A hoặc B, mỗi phần tử chỉ viết một lần.');

    const center = i + 5, radius = i % 4 + 1;
    add(1, i, `Tìm tập nghiệm của |x - ${center}| < ${radius}.`,
      pair(center - radius, center + radius),
      [`[${center - radius}; ${center + radius}]`, `(-∞; ${center - radius}) ∪ (${center + radius}; +∞)`,
        pair(center - radius, center)],
      `|x - ${center}| < ${radius} tương đương ${center - radius} < x < ${center + radius}.`);

    const limit = i + 3;
    add(2, i, `Điểm nào thỏa mãn x ≥ 0, y ≥ 0 và x + y ≤ ${limit}?`,
      pair(limit - 1, 1), [pair(limit + 1, 0), pair(-1, 1), pair(1, -1)],
      `Điểm ${pair(limit - 1, 1)} có hai tọa độ không âm và tổng bằng ${limit}; các điểm khác vi phạm ít nhất một điều kiện.`);

    const bound = i + 4;
    add(3, i, `Với x ≥ 0, y ≥ 0 và x + y ≤ ${bound}, giá trị lớn nhất của x + y là bao nhiêu?`,
      bound, [bound - 1, bound + 1, 2 * bound],
      `Ràng buộc cho x + y ≤ ${bound}; dấu bằng đạt được, chẳng hạn tại (${bound}; 0).`);

    const h = i + 1, k = i + 2;
    add(4, i, `Đỉnh của parabol y = (x - ${h})² + ${k} là điểm nào?`,
      pair(h, k), [pair(-h, k), pair(h, -k), pair(-h, -k)],
      `Dạng đỉnh y = (x - h)² + k cho tọa độ đỉnh là (h; k) = ${pair(h, k)}.`);

    const left = i, right = i + 3;
    add(5, i, `Tập nghiệm của (x - ${left})(x - ${right}) > 0 là gì?`,
      `(-∞; ${left}) ∪ (${right}; +∞)`,
      [pair(left, right), `(-∞; ${right})`, `(${left}; +∞)`],
      `Tích hai thừa số cùng dấu dương khi x < ${left} hoặc x > ${right}.`);

    const r1 = i + 1, r2 = i + 4;
    add(6, i, `Tập nghiệm của phương trình (x - ${r1})(x - ${r2}) = 0 là gì?`,
      set(r1, r2), [set(r1), set(r2), set(-r1, -r2)],
      `Tích bằng 0 khi x - ${r1} = 0 hoặc x - ${r2} = 0.`);

    const primitive = [[3, 4, 5], [5, 12, 13], [8, 15, 17], [7, 24, 25]][(i - 1) % 4];
    const scale = Math.floor((i - 1) / 4) + 1;
    const [opposite, adjacent, hypotenuse] = primitive.map(n => n * scale);
    add(7, i, `Tam giác vuông có cạnh đối với góc α dài ${opposite} cm, cạnh kề dài ${adjacent} cm và cạnh huyền dài ${hypotenuse} cm. sin α bằng bao nhiêu?`,
      `${opposite}/${hypotenuse}`,
      [`${adjacent}/${hypotenuse}`, `${opposite}/${adjacent}`, `${hypotenuse}/${opposite}`],
      `Trong tam giác vuông, sin α = cạnh đối/cạnh huyền = ${opposite}/${hypotenuse}.`);

    const vx = i + 1, vy = i + 2, wx = 2 * i + 1, wy = i + 3;
    add(8, i, `Cho u = ${pair(vx, vy)} và v = ${pair(wx, wy)}. Tọa độ u + v là gì?`,
      pair(vx + wx, vy + wy),
      [pair(vx + wx + 1, vy + wy), pair(vx + wx, vy + wy + 1), pair(vx - wx, vy - wy)],
      `Cộng từng tọa độ tương ứng: u + v = ${pair(vx + wx, vy + wy)}.`);

    const p = i + 1, q = i + 2, r = i + 3, s = i + 4;
    const dot = p * r + q * s;
    add(9, i, `Cho u = ${pair(p, q)} và v = ${pair(r, s)}. Tích vô hướng u · v bằng bao nhiêu?`,
      dot, [p * r, q * s, p * r - q * s],
      `u · v = ${p} × ${r} + ${q} × ${s} = ${dot}.`);

    const px = i, py = i + 1, qx = 2 * i + 2, qy = 3 * i + 4;
    const dx = qx - px, dy = qy - py;
    add(10, i, `Trong mặt phẳng tọa độ, P = ${pair(px, py)}, Q = ${pair(qx, qy)}. Tọa độ vectơ PQ là gì?`,
      pair(dx, dy), [pair(-dx, -dy), pair(px + qx, py + qy), pair(dy, dx)],
      `Tọa độ vectơ PQ = (xQ - xP; yQ - yP) = ${pair(dx, dy)}.`);

    const slope = i + 1, intercept = i + 20;
    add(11, i, `Đường thẳng đi qua ${pair(0, intercept)} và ${pair(1, intercept + slope)} có phương trình nào?`,
      `y = ${slope}x + ${intercept}`,
      [`y = ${slope}x - ${intercept}`, `y = ${intercept}x + ${slope}`,
        `y = ${slope + 1}x + ${intercept}`],
      `Hệ số góc là (${intercept + slope} - ${intercept})/(1 - 0) = ${slope}; tung độ gốc bằng ${intercept}.`);

    const shirts = i + 2, trousers = i + 4;
    add(12, i, `Có ${shirts} áo và ${trousers} quần khác nhau. Có bao nhiêu cách chọn một áo và một quần?`,
      shirts * trousers, [shirts + trousers, shirts * trousers + 1, shirts + trousers + 1],
      `Theo quy tắc nhân, số cách chọn là ${shirts} × ${trousers} = ${shirts * trousers}.`);

    const n = i + 5, chooseTwo = n * (n - 1) / 2;
    add(13, i, `Từ ${n} học sinh khác nhau, có bao nhiêu cách chọn 2 học sinh vào một nhóm?`,
      chooseTwo, [n, n * (n - 1), n * (n + 1) / 2],
      `Số nhóm không xét thứ tự là C(${n}, 2) = ${n} × ${n - 1}/2 = ${chooseTwo}.`);

    const first = i + 2;
    add(14, i, `Trung bình cộng của ba số ${first}, ${first + 2}, ${first + 4} là bao nhiêu?`,
      first + 2, [first, first + 4, 3 * first + 6],
      `Tổng ba số là ${3 * first + 6}; chia cho 3 được ${first + 2}.`);
  }
  return questions;
}

export function renderMigration(questions) {
  const json = JSON.stringify(questions).replaceAll("'", "''");
  return `begin;\n\n-- Original Grade 10 exercises generated from deterministic mathematical rules.\n-- Only the 15 currently implemented Math 10 topics are covered.\nwith seed as (\n  select * from jsonb_to_recordset('${json}'::jsonb) as q(\n    id text, topic text, prompt text, option_a text, option_b text,\n    option_c text, option_d text, correct_option text, explanation text, difficulty text\n  )\n)\ninsert into public.mentor_questions(\n  id, code, subject_id, grade, chapter, topic, prompt, option_a, option_b,\n  option_c, option_d, correct_option, explanation, difficulty, points, source,\n  author, license, status\n)\nselect id, upper(replace(id, '-', '_')), 'math', 10, topic, topic, prompt,\n  option_a, option_b, option_c, option_d, correct_option, explanation,\n  difficulty, 10, 'AI Mentor tự biên soạn theo chủ đề Toán 10',\n  'AI Mentor', 'Nội bộ', 'PUBLISHED' from seed\non conflict (id) do nothing;\n\ncommit;\n`;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const questions = buildMathQuestions();
  if (questions.length !== 195) throw new Error(`Expected 195 questions, got ${questions.length}`);
  writeFileSync(output, renderMigration(questions), 'utf8');
  process.stdout.write(`Generated ${questions.length} Math 10 questions.\n`);
}
