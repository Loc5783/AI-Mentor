import test from 'node:test';
import assert from 'node:assert/strict';
import {examPackSources,examPackPrompt,examPackResult} from './exam_pack.mjs';

const sources = [{name:'source.pdf',pageCount:3}];
const question = () => ({sourceFileIndex:0,sourcePageIndex:2,prompt:'2 × 5 = ?',
  options:{A:'8',B:'10',C:'12',D:'14'},correctOption:'B',explanation:'2 × 5 = 10',topic:'Arithmetic'});
const parse = questions => examPackResult(JSON.stringify({questions}),sources);

test('AI cannot assign identity, source filename or approval', () => {
  const result = parse([{...question(),id:'evil',userId:999,sourceFileName:'spoof.pdf',isApproved:true,isAnswerVerified:true}]);
  assert.equal(result.questions[0].sourceFileName,'source.pdf');
  assert.equal(result.questions[0].isApproved,false);
  assert.equal(result.questions[0].isAnswerVerified,false);
  assert.equal(result.questions[0].userId,undefined);
});
test('reimport duplicates collapse but changed numbers survive', () => {
  assert.equal(parse([question(),question(),{...question(),prompt:'2 × 6 = ?'}]).questions.length,2);
});
test('missing answers stay editable drafts', () => {
  const q={...question(),correctOption:null,explanation:null,topic:null};
  assert.equal(parse([q]).questions[0].correctOption,null);
});
test('malformed, truncated, empty and overlong results fail closed', () => {
  for(const text of ['bad','null','{}','{"questions":[]}',JSON.stringify({questions:Array(41).fill(question())})])
    assert.equal(examPackResult(text,sources).error_code,'INVALID_AI_OUTPUT');
  assert.equal(examPackResult(JSON.stringify({questions:[question()]}),sources,true).error_code,'INVALID_AI_OUTPUT');
});
test('invalid provenance and option schema fail closed', () => {
  for(const patch of [{sourceFileIndex:-1},{sourceFileIndex:1},{sourcePageIndex:0},
    {sourcePageIndex:4},{sourcePageIndex:1.5},{prompt:''},{options:{A:'1'}},
    {correctOption:'E'},{explanation:'x'.repeat(4001)}])
    assert.equal(parse([{...question(),...patch}]).error_code,'INVALID_AI_OUTPUT');
});
test('requires bounded PDF or image metadata before extraction', () => {
  assert.deepEqual(examPackSources({files:[{mime:'application/pdf',name:'source.pdf',pageCount:3}]}),sources);
  for(const file of [{mime:'text/plain',name:'x',pageCount:1},
    {mime:'image/png',name:'x',pageCount:2},{mime:'application/pdf',name:'x',pageCount:51},
    {mime:'application/pdf',name:'',pageCount:1}])
    assert.throws(()=>examPackSources({files:[file]}));
  assert.throws(()=>examPackSources({files:[]}));
  assert.match(examPackPrompt(sources),/Never mark anything approved/);
});
