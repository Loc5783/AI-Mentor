import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve, dirname } from 'node:path';

const migration = readFileSync(resolve(
  dirname(fileURLToPath(import.meta.url)),
  '../../supabase/migrations/202611140002_grade10_question_bank_and_physics_quiz.sql',
), 'utf8');
const lines = migration.split(/\r?\n/).filter(line => /^\s*\('g10-found-/.test(line));
const values = lines.map(line => [...line.matchAll(/'((?:[^']|'')*)'/g)]
  .map(match => match[1].replaceAll("''", "'")));

test('grade 10 question bank adds five usable questions per missing subject', () => {
  const subjects = [
    'math', 'chemistry', 'biology', 'literature', 'english',
    'history', 'geography', 'economics_law',
  ];
  assert.equal(values.length, subjects.length * 5);
  assert.equal(new Set(values.map(row => row[0])).size, values.length);
  for (const subject of subjects) {
    assert.equal(values.filter(row => row[1] === subject).length, 5, subject);
  }
  for (const row of values) {
    assert.equal(row.length, 9);
    const [id, subject, prompt, a, b, c, d, answer, explanation] = row;
    assert.ok(prompt.trim() && explanation.trim(), id);
    assert.ok(['A', 'B', 'C', 'D'].includes(answer), id);
    assert.equal(new Set([a, b, c, d].map(s => s.trim().toLocaleLowerCase())).size, 4, id);
    assert.ok([a, b, c, d].every(s => s.trim() && !s.includes('Cần biên tập')), id);
  }
});

test('starter physics quiz extends only five-question version with five published questions', () => {
  const codes = [...migration.matchAll(/'AIM_PHY10_M01_Q0[1-5]'/g)].map(m => m[0]);
  assert.equal(new Set(codes).size, 5);
  assert.match(migration, /q\.id = 'quiz_phy10_basic'/);
  assert.match(migration, /jsonb_array_length\(q\.questions\) = 5/);
  assert.match(migration, /and status = 'PUBLISHED'/);
  assert.match(migration, /and not exists \(/);
});
