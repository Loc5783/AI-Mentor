do $$
declare subject_row record;
declare total_count integer;
declare invalid_count integer;
begin
  for subject_row in select unnest(array[
    'biology','chemistry','economics_law','english','geography','history','literature','math','physics'
  ]) as subject_id
  loop
    select count(*) into total_count
    from public.mentor_questions
    where subject_id = subject_row.subject_id and grade = 10 and status = 'PUBLISHED';
    if total_count < 50 then
      raise exception 'Grade 10 % bank has only % published questions', subject_row.subject_id, total_count;
    end if;

    select count(*) into invalid_count
    from public.mentor_questions
    where subject_id = subject_row.subject_id and grade = 10 and status = 'PUBLISHED'
      and (coalesce(trim(prompt), '') = ''
        or coalesce(trim(option_a), '') = '' or coalesce(trim(option_b), '') = ''
        or coalesce(trim(option_c), '') = '' or coalesce(trim(option_d), '') = ''
        or correct_option not in ('A', 'B', 'C', 'D')
        or option_d in (option_a, option_b, option_c)
        or coalesce(trim(explanation), '') = '');
    if invalid_count > 0 then
      raise exception 'Grade 10 % bank contains % incomplete published questions',
        subject_row.subject_id, invalid_count;
    end if;
  end loop;
end $$;
