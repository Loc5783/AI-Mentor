import test from 'node:test';
import assert from 'node:assert/strict';
import {plannerResult} from './planner.mjs';

test('rejects blurry and unrelated timetable images', () => {
  assert.equal(plannerResult('timetable_scan','{"status":"IMAGE_BLURRY"}').error_code,'IMAGE_BLURRY');
  assert.equal(plannerResult('timetable_scan','{"status":"NOT_TIMETABLE"}').error_code,'NOT_TIMETABLE');
});

test('validates extracted slots before exposing a draft', () => {
  const ok=plannerResult('timetable_scan',JSON.stringify({status:'OK',entries:[{day:1,period:2,subject:'Toán'}]}));
  assert.match(ok.text,/```ai_schedule/);
  const duplicate=plannerResult('timetable_scan',JSON.stringify({status:'OK',entries:[{day:1,period:2,subject:'Toán'},{day:1,period:2,subject:'Lý'}]}));
  assert.equal(duplicate.error_code,'INVALID_AI_OUTPUT');
  assert.equal(plannerResult('timetable_scan','not json').error_code,'INVALID_AI_OUTPUT');
});

test('to-do planning remains a separate draft', () => {
  const ok=plannerResult('todo_plan',JSON.stringify({status:'OK',tasks:[{day:2,title:'Ôn Toán',subject:'Toán',priority:1}]}));
  assert.match(ok.text,/```ai_todo/);
  assert.equal(plannerResult('todo_plan','{"status":"NEED_DETAILS"}').error_code,'TODO_DETAILS_REQUIRED');
});
