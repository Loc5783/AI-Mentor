import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

// Completes the existing lesson MCQs offline. Choice D is selected from a
// known-wrong answer in a different unit of the same subject, never Gemini.
const root = resolve(import.meta.dirname, '../..');
const subjects = ['biology','chemistry','economics_law','english','geography','history','literature'];
const sql = (s) => `'${String(s).replaceAll("'", "''")}'`;
const updates = [];

for (const subject of subjects) {
  const curriculum = JSON.parse(readFileSync(resolve(root, `backend/curriculum/${subject}-10.json`), 'utf8'));
  const all = curriculum.lessons.flatMap((lesson, lessonIndex) => (lesson.questions ?? []).map((q) => ({
    lesson: lesson.id, lessonIndex, q,
    answer: String(q.correctOptionId ?? q.correctOption ?? '').toUpperCase(),
  })));
  const candidates = all.flatMap((entry) => entry.q.options
    .filter((option) => String(option.id).toUpperCase() !== entry.answer)
    .map((option) => ({ lesson: entry.lesson, text: String(option.text ?? '').trim() })))
    .filter((option) => option.text.length > 0);
  const knownCorrectAnswers = new Set(all.flatMap((entry) => {
    const answer = entry.q.options.find((option) => String(option.id).toUpperCase() === entry.answer);
    return answer ? [String(answer.text ?? '').trim().toLocaleLowerCase()] : [];
  }));
  const chosen = [];
  for (const entry of all) {
    if (chosen.length === 45) break;
    const options = entry.q.options.map((option) => String(option.text ?? '').trim());
    if (options.length !== 3 || options.some((option) => !option) || !['A','B','C'].includes(entry.answer)) continue;
    const exact = new Set(options.map((option) => option.toLocaleLowerCase()));
    const answerText = options[['A','B','C'].indexOf(entry.answer)];
    const candidateList = candidates.filter((candidate) => candidate.lesson !== entry.lesson
      && !exact.has(candidate.text.toLocaleLowerCase())
      && !knownCorrectAnswers.has(candidate.text.toLocaleLowerCase())
      && candidate.text.toLocaleLowerCase() !== answerText.toLocaleLowerCase()
      && ((candidate.text.length < 18) === (answerText.length < 18)));
    if (!candidateList.length) continue;
    const seed = Number.parseInt(String(entry.q.id).match(/(\d+)$/)?.[1] ?? '1', 10);
    const choiceD = candidateList[(seed + entry.lessonIndex) % candidateList.length].text;
    chosen.push({ id: `g10-lesson-${entry.q.id}`, choiceD, options, answer: entry.answer, prompt: entry.q.prompt });
  }
  if (chosen.length !== 45) throw new Error(`${subject}: expected 45 eligible lesson questions, got ${chosen.length}`);
  for (const q of chosen) {
    if (q.options.some((option) => option.toLocaleLowerCase() === q.choiceD.toLocaleLowerCase()))
      throw new Error(`${subject}/${q.id}: repeated option`);
    updates.push([q.id, q.choiceD]);
  }
}

const values = updates.map(([id, choiceD]) => `  (${sql(id)}, ${sql(choiceD)})`).join(',\n');
const subjectValues = subjects.map(sql).join(',');
const output = `begin;
-- Finish 45 Grade 10 lesson questions per remaining subject. New distractors
-- come from an incorrect choice in a different lesson of the same subject.
with choices(id, option_d) as (values
${values}
), updated as (
  update public.mentor_questions q
  set option_d = c.option_d, status = 'PUBLISHED', updated_at = now()
  from choices c
  where q.id = c.id and q.grade = 10
    and q.subject_id in (${subjectValues})
    and q.status = 'DRAFT'
    and q.option_d like '[Cần biên tập%'
    and q.correct_option in ('A','B','C')
    and c.option_d not in (q.option_a,q.option_b,q.option_c)
  returning q.subject_id
)
select subject_id, count(*) as published_count from updated group by subject_id;

do $$
declare subject_row record;
declare published_count integer;
declare completed_count integer;
begin
  for subject_row in select unnest(array[${subjectValues}]) as subject_id loop
    select count(*) into published_count from public.mentor_questions
      where subject_id = subject_row.subject_id and grade = 10 and status = 'PUBLISHED';
    if published_count < 50 then
      raise exception 'Grade 10 % bank has only % published questions', subject_row.subject_id, published_count;
    end if;
    select count(*) into completed_count from public.mentor_questions
      where subject_id = subject_row.subject_id and grade = 10 and status = 'PUBLISHED'
        and id like 'g10-lesson-%' and option_d not like '[Cần biên tập%'
        and option_d not in (option_a,option_b,option_c)
        and correct_option in ('A','B','C');
    if completed_count < 45 then
      raise exception 'Grade 10 % has only % completed lesson questions', subject_row.subject_id, completed_count;
    end if;
  end loop;
end $$;
commit;
`;

writeFileSync(resolve(root, 'supabase/migrations/202611160017_grade10_banks_to_50.sql'), output, 'utf8');
console.log(`Prepared ${updates.length} question-bank updates across ${subjects.length} Grade 10 subjects.`);
