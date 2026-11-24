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
  const context='Only selected weekday 2 was loaded. Giờ rảnh hôm nay: 19:00–21:00. Total study time limit: 60 minutes.';
  const task={day:2,startTime:'19:00',minutes:30,title:'Ôn Toán',subject:'Toán',priority:1};
  const ok=plannerResult('todo_plan',JSON.stringify({status:'OK',tasks:[task]}),false,context);
  assert.match(ok.text,/```ai_todo/);
  assert.equal(plannerResult('todo_plan','{"status":"NEED_DETAILS"}').error_code,'TODO_DETAILS_REQUIRED');
  for (const tasks of [
    [{...task,day:3}],
    [task,{...task,title:'Làm đề Toán',startTime:'19:15'}],
    [task,{...task,title:'Làm đề Toán',startTime:'20:00',minutes:40}],
    [{...task,startTime:'18:00'}],
  ]) assert.equal(plannerResult('todo_plan',JSON.stringify({status:'OK',tasks}),false,context).error_code,'INVALID_AI_OUTPUT');
});

test('exam plan stays inside requested dates, subjects and daily time budget', () => {
  const request=JSON.stringify({today:'2026-11-19',examDate:'2026-11-25',dailyMinutes:60,
    availability:'19:00-20:00',subjects:[{name:'Toán',lessons:['Hàm số']}],existingTimetable:[]});
  const task={dueDate:'2026-11-20',startTime:'19:00',title:'Ôn Hàm số',subject:'Toán',minutes:30,priority:1};
  assert.match(plannerResult('exam_plan',JSON.stringify({status:'OK',tasks:[task]}),false,request).text,/ai_exam_plan/);
  assert.equal(plannerResult('exam_plan',JSON.stringify({status:'OK',tasks:[{...task,dueDate:'2026-11-25'}]}),false,request).error_code,'INVALID_AI_OUTPUT');
  assert.equal(plannerResult('exam_plan',JSON.stringify({status:'OK',tasks:[{...task,subject:'Vật lý'}]}),false,request).error_code,'INVALID_AI_OUTPUT');
  assert.equal(plannerResult('exam_plan',JSON.stringify({status:'OK',tasks:[task,{...task,title:'Luyện thêm',minutes:40}]}),false,request).error_code,'INVALID_AI_OUTPUT');
});
