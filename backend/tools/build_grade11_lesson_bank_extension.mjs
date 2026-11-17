import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

// Complete existing 3-choice Grade 11 lesson questions offline.
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const subjects = ['biology','chemistry','economics_law','english','geography','history','literature','physics'];
const sql = (s) => `'${String(s).replaceAll("'", "''")}'`;
const updates=[];
for (const subject of subjects) {
  const curriculum=JSON.parse(readFileSync(resolve(root,`backend/curriculum/${subject}-11.json`),'utf8'));
  const all=curriculum.lessons.flatMap((lesson,lessonIndex)=>(lesson.questions??[]).map((q)=>({lesson:lesson.id,lessonIndex,q,answer:String(q.correctOptionId??q.correctOption??'').toUpperCase()})));
  const knownCorrect=new Set(all.flatMap(({q,answer})=>{const o=q.options.find(x=>String(x.id).toUpperCase()===answer);return o?[String(o.text).trim().toLocaleLowerCase()]:[]}));
  const candidates=all.flatMap(({lesson,q,answer})=>q.options.filter(o=>String(o.id).toUpperCase()!==answer).map(o=>({lesson,text:String(o.text??'').trim()}))).filter(x=>x.text);
  const selected=[];
  for(const entry of all){
    const options=entry.q.options.map(o=>String(o.text??'').trim());
    if(options.length!==3||options.some(x=>!x)||!['A','B','C'].includes(entry.answer))continue;
    const current=new Set(options.map(x=>x.toLocaleLowerCase()));
    const answerText=options[['A','B','C'].indexOf(entry.answer)];
    const pool=candidates.filter(c=>c.lesson!==entry.lesson&&!current.has(c.text.toLocaleLowerCase())&&!knownCorrect.has(c.text.toLocaleLowerCase())&&(c.text.length<18)===(answerText.length<18));
    if(!pool.length)continue;
    const seed=Number.parseInt(String(entry.q.id).match(/(\d+)$/)?.[1]??'1',10);
    const choiceD=pool[(seed+entry.lessonIndex)%pool.length].text;
    selected.push({id:`g11-lesson-${entry.q.id}`,choiceD,options});
  }
  if(selected.length!==15)throw new Error(`${subject}: expected 15 completed lesson questions, got ${selected.length}`);
  for(const q of selected){if(q.options.some(x=>x.toLocaleLowerCase()===q.choiceD.toLocaleLowerCase()))throw new Error(`${subject}/${q.id}: repeated option`);updates.push([q.id,q.choiceD]);}
}
const values=updates.map(([id,d])=>`  (${sql(id)}, ${sql(d)})`).join(',\n');
const subjectValues=subjects.map(sql).join(',');
const output=`begin;
-- Give each existing Grade 11 lesson question a distinct fourth choice drawn
-- from a known-wrong option in another lesson of the same subject.
with choices(id,option_d) as (values
${values}
), updated as (
  update public.mentor_questions q
  set option_d=c.option_d,status='PUBLISHED',updated_at=now()
  from choices c
  where q.id=c.id and q.grade=11 and q.subject_id in (${subjectValues})
    and q.status='DRAFT' and q.option_d like '[Cần biên tập%'
    and q.correct_option in ('A','B','C')
    and c.option_d not in (q.option_a,q.option_b,q.option_c)
  returning q.subject_id
)
select subject_id,count(*) as published_count from updated group by subject_id;

do $$
declare subject_row record;
declare completed_count integer;
begin
  for subject_row in select unnest(array[${subjectValues}]) as subject_id loop
    select count(*) into completed_count from public.mentor_questions
    where subject_id=subject_row.subject_id and grade=11 and status='PUBLISHED'
      and id like 'g11-lesson-%' and option_d not like '[Cần biên tập%'
      and option_d not in (option_a,option_b,option_c)
      and correct_option in ('A','B','C');
    if completed_count<15 then raise exception 'Grade 11 % has only % completed lesson questions',subject_row.subject_id,completed_count; end if;
  end loop;
end $$;
commit;
`;
writeFileSync(resolve(root,'supabase/migrations/202611160018_grade11_lesson_bank_extension.sql'),output,'utf8');
console.log(`Prepared ${updates.length} Grade 11 lesson questions across ${subjects.length} subjects.`);
