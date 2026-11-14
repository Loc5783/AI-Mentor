import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { buildMathQuestions, renderMigration } from '../tools/build_grade10_math_bank.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const questions = buildMathQuestions();
const curriculum = JSON.parse(readFileSync(resolve(root, 'backend/curriculum/math-10.json'), 'utf8'));
const migration = readFileSync(resolve(root,
  'supabase/migrations/20261114134001_grade10_math_question_bank.sql'), 'utf8');

test('195 original Math 10 questions are unique and cover implemented units', () => {
  assert.equal(questions.length, 195);
  assert.equal(new Set(questions.map(q => q.id)).size, 195);
  assert.equal(new Set(questions.map(q => q.prompt)).size, 195);
  const topics = new Set(curriculum.lessons.map(lesson => lesson.title));
  for (const q of questions) {
    assert.ok(topics.has(q.topic), q.id);
    assert.ok(q.explanation.trim() && !/NaN|undefined/.test(q.explanation), q.id);
    const options = [q.option_a, q.option_b, q.option_c, q.option_d];
    assert.equal(new Set(options).size, 4, q.id);
    assert.ok(options.every(option => option.trim()), q.id);
    assert.ok('ABCD'.includes(q.correct_option), q.id);
  }
});

test('answer key matches independent arithmetic for every generated family', () => {
  for (const q of questions) {
    const [, , , familyText, variantText] = q.id.split('-');
    const family = Number(familyText);
    const i = Number(variantText);
    const correct = q[`option_${q.correct_option.toLowerCase()}`];
    let expected;
    switch (family) {
      case 1: expected = `{${i + 2}, ${i + 3}, ${i + 4}}`; break;
      case 2: { const c = i + 5, r = i % 4 + 1;
        expected = `(${c - r}; ${c + r})`; break; }
      case 3: expected = `(${i + 2}; 1)`; break;
      case 4: expected = String(i + 4); break;
      case 5: expected = `(${i + 1}; ${i + 2})`; break;
      case 6: expected = `(-∞; ${i}) ∪ (${i + 3}; +∞)`; break;
      case 7: expected = `{${i + 1}, ${i + 4}}`; break;
      case 8: { const triples = [[3,4,5],[5,12,13],[8,15,17],[7,24,25]];
        const [o,,h] = triples[(i - 1) % 4]; const scale = Math.floor((i - 1) / 4) + 1;
        expected = `${o * scale}/${h * scale}`; break; }
      case 9: expected = `(${3 * i + 2}; ${2 * i + 5})`; break;
      case 10: expected = String((i + 1) * (i + 3) + (i + 2) * (i + 4)); break;
      case 11: expected = `(${i + 2}; ${2 * i + 3})`; break;
      case 12: expected = `y = ${i + 1}x + ${i + 20}`; break;
      case 13: expected = String((i + 2) * (i + 4)); break;
      case 14: expected = String((i + 5) * (i + 4) / 2); break;
      case 15: expected = String(i + 4); break;
      default: assert.fail(q.id);
    }
    assert.equal(correct, expected, q.id);
  }
});

test('checked questions exactly reproduce committed migration', () => {
  assert.equal(migration, renderMigration(questions));
  assert.match(migration, /'math', 10, topic/);
  assert.match(migration, /'PUBLISHED' from seed/);
});
