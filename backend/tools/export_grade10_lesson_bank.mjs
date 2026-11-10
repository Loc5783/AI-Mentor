/** Export existing Grade 10 lesson questions for editorial review. */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve, dirname } from 'node:path';
import assert from 'node:assert/strict';

export const subjects = [
  'math', 'physics', 'chemistry', 'biology', 'literature', 'english',
  'history', 'geography', 'economics_law',
];
const headers = [
  'code', 'subjectId', 'grade', 'chapter', 'topic', 'textbook', 'prompt',
  'optionA', 'optionB', 'optionC', 'optionD', 'correctOption', 'explanation',
  'difficulty', 'points', 'source', 'author', 'license', 'status', 'reviewNote',
];
const csvValue = value => `"${String(value ?? '').replaceAll('"', '""')}"`;

export function exportBank(curriculumDir, outputDir) {
  mkdirSync(outputDir, { recursive: true });
  const counts = {};
  const seenCodes = new Set();
  for (const subject of subjects) {
    const course = JSON.parse(readFileSync(resolve(curriculumDir, `${subject}-10.json`), 'utf8'));
    const rows = [];
    for (const lesson of course.lessons) {
      for (const question of lesson.questions) {
        assert.equal(question.options.length, 3, question.id);
        const answerIndex = question.options.findIndex(
          option => option.id.toLowerCase() === question.correctOptionId.toLowerCase());
        assert.ok(answerIndex >= 0 && question.explanation?.trim(), question.id);
        const code = `AIM_G10_LESSON_${question.id.replaceAll('-', '_').toUpperCase()}`;
        assert.ok(!seenCodes.has(code), `Duplicate code ${code}`);
        seenCodes.add(code);
        rows.push({
          code, subjectId: subject, grade: 10, chapter: lesson.title, topic: lesson.title,
          textbook: '', prompt: question.prompt,
          optionA: question.options[0].text, optionB: question.options[1].text,
          optionC: question.options[2].text, optionD: '',
          correctOption: String.fromCharCode(65 + answerIndex),
          explanation: question.explanation,
          difficulty: ['EASY', 'HARD'].includes(question.difficulty) ? question.difficulty : 'MEDIUM',
          points: 10, source: `course_unit:${lesson.id}`, author: 'AI Mentor', license: 'Nội bộ',
          status: 'DRAFT', reviewNote: 'Biên tập và xác minh phương án D; giáo viên duyệt trước khi xuất bản',
        });
      }
    }
    const csv = [headers.join(','), ...rows.map(row => headers.map(header => csvValue(row[header])).join(','))].join('\r\n') + '\r\n';
    writeFileSync(resolve(outputDir, `${subject}-10-review.csv`), '\uFEFF' + csv, 'utf8');
    counts[subject] = rows.length;
  }
  const manifest = { grade: 10, status: 'DRAFT', source: 'existing lessons',
    questionCount: Object.values(counts).reduce((sum, count) => sum + count, 0), subjects: counts };
  writeFileSync(resolve(outputDir, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n', 'utf8');
  return manifest;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
  const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
  console.log(exportBank(resolve(root, 'backend/curriculum'), resolve(root, 'content/question-bank/grade10')));
}
