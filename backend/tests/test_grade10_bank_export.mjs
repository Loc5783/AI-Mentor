import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { exportBank, subjects } from '../tools/export_grade10_lesson_bank.mjs';

test('exports every existing Grade 10 lesson question as an unpublished review row', () => {
  const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
  const temp = mkdtempSync(join(tmpdir(), 'mentor-g10-bank-'));
  try {
    const manifest = exportBank(resolve(root, 'backend/curriculum'), temp);
    assert.equal(manifest.questionCount, 450);
    assert.equal(Object.keys(manifest.subjects).length, 9);
    for (const subject of subjects) {
      const csv = readFileSync(join(temp, `${subject}-10-review.csv`), 'utf8');
      assert.equal(csv.trim().split('\r\n').length - 1, manifest.subjects[subject]);
      assert.ok(csv.includes('"DRAFT"'));
      assert.ok(csv.includes('"","'), 'The fourth option must remain blank for editorial review');
    }
  } finally {
    rmSync(temp, { recursive: true, force: true });
  }
});
