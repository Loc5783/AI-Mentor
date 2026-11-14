import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve, dirname } from 'node:path';

const migration = readFileSync(resolve(
  dirname(fileURLToPath(import.meta.url)),
  '../../supabase/migrations/202611140002_grade10_question_bank_and_physics_quiz.sql',
), 'utf8');
const alignment = readFileSync(resolve(
  dirname(fileURLToPath(import.meta.url)),
  '../../supabase/migrations/202611140005_grade10_foundation_curriculum_alignment.sql',
), 'utf8');
const mixedSets = readFileSync(resolve(
  dirname(fileURLToPath(import.meta.url)),
  '../../supabase/migrations/202611140004_grade10_mixed_practice_sets.sql',
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

test('new question topics match actual Grade 10 lessons', () => {
  const aliases = { chem: 'chemistry', bio: 'biology', lit: 'literature',
    eng: 'english', hist: 'history', geo: 'geography', law: 'economics_law' };
  const topics = [...alignment.matchAll(/\('g10-found-([a-z]+)-\d\d','([^']+)'\)/g)]
    .map(([, shortSubject, title]) => [aliases[shortSubject] ?? shortSubject, title]);
  assert.equal(topics.length, 40);
  for (const [subject, title] of topics) {
    const curriculum = JSON.parse(readFileSync(resolve(
      dirname(fileURLToPath(import.meta.url)), `../curriculum/${subject}-10.json`,
    ), 'utf8'));
    assert.ok(curriculum.lessons.some(lesson => lesson.title === title), `${subject}: ${title}`);
  }
  assert.match(alignment, /g10-found-hist-05','Nguồn năng lượng tiêu biểu/);
  assert.match(alignment, /g10-found-math-01','Giải bất phương trình/);
});

test('mixed practice sets use enough unique Grade 10 lesson questions for all nine subjects', () => {
  const subjects = [
    'math', 'literature', 'english', 'history', 'geography',
    'economics_law', 'physics', 'chemistry', 'biology',
  ];
  for (const subject of subjects) {
    const curriculum = JSON.parse(readFileSync(resolve(
      dirname(fileURLToPath(import.meta.url)), `../curriculum/${subject}-10.json`,
    ), 'utf8'));
    const questions = curriculum.lessons.flatMap(lesson => lesson.questions);
    assert.ok(questions.length >= 15, subject);
    assert.equal(new Set(questions.map(question => question.id)).size, questions.length, subject);
  }
  assert.match(mixedSets, /c\.grade = 10 and c\.kind = 'LESSON'/);
  assert.match(mixedSets, /question_no <= 15/);
  assert.match(mixedSets, /'Luyện tổng hợp lộ trình hiện có'/);
});
