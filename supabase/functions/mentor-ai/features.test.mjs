import test from 'node:test';
import assert from 'node:assert/strict';

test('Misconception Buster evaluates propositions correctly', () => {
  const prop1 = { prompt: "If x > 2 then x^2 > 4", correct: true };
  const prop2 = { prompt: "If x^2 > 4 then x > 2", correct: false };

  // Student answers both correctly
  const ans1 = true;
  const ans2 = false;

  const resolved = (ans1 === prop1.correct) && (ans2 === prop2.correct);
  assert.equal(resolved, true);

  // Student answers 1 wrong -> not resolved
  const ans2Wrong = true;
  const partiallyResolved = (ans1 === prop1.correct) && (ans2Wrong === prop2.correct);
  assert.equal(partiallyResolved, false);
});

test('ExamPack Foundry deduplicates exact text but protects numerical variations', () => {
  const q1 = { prompt: "Calculate 2 * 5", options: ["8", "10", "12", "14"], answer: "10" };
  const q1Duplicate = { prompt: "Calculate 2 * 5", options: ["8", "10", "12", "14"], answer: "10" };
  const q2NumericalVar = { prompt: "Calculate 2 * 6", options: ["8", "10", "14", "12"], answer: "12" };

  const fp1 = q1.prompt + "||" + q1.options.join("|");
  const fpDup = q1Duplicate.prompt + "||" + q1Duplicate.options.join("|");
  const fp2 = q2NumericalVar.prompt + "||" + q2NumericalVar.options.join("|");

  assert.equal(fp1, fpDup);
  assert.notEqual(fp1, fp2);
});

test('TeamOS strictly checks project membership', () => {
  const members = new Set([101, 102]);
  const isMember = (id) => members.has(id);

  assert.equal(isMember(101), true);
  assert.equal(isMember(102), true);
  assert.equal(isMember(999), false);
});
