import {test} from 'node:test';
import {strict as assert} from 'node:assert';
import {essayPrompt,parseEssayGrade} from './essay_grader.mjs';

test('grades stay within the question rubric and point range', () => {
  const prompt=essayPrompt({grade:12,subject:'Toán',question:'Tính đạo hàm',rubric:'Nêu quy tắc và kết quả',answer:'y=2x',maxPoints:50});
  assert.match(prompt,/Thang điểm\/đáp án/);
  assert.deepEqual(parseEssayGrade('{"points":35,"feedback":"Đúng phương pháp, thiếu kết quả."}',50),
    {points:35,feedback:'Đúng phương pháp, thiếu kết quả.'});
  assert.throws(()=>parseEssayGrade('{"points":51,"feedback":"Tốt"}',50));
  assert.throws(()=>parseEssayGrade('{"points":12.5,"feedback":"Tốt"}',50));
  assert.throws(()=>parseEssayGrade('{"points":25,"feedback":""}',50));
});
