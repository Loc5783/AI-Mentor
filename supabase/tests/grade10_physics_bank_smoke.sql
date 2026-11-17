do $$
declare total_count integer;
declare invalid_count integer;
begin
  select count(*) into total_count
  from public.mentor_questions
  where subject_id = 'physics' and grade = 10 and status = 'PUBLISHED';

  select count(*) into invalid_count
  from public.mentor_questions
  where subject_id = 'physics' and grade = 10 and status = 'PUBLISHED'
    and (coalesce(trim(prompt), '') = ''
      or coalesce(trim(option_a), '') = '' or coalesce(trim(option_b), '') = ''
      or coalesce(trim(option_c), '') = '' or coalesce(trim(option_d), '') = ''
      or correct_option not in ('A', 'B', 'C', 'D')
      or coalesce(trim(explanation), '') = '');

  if total_count < 50 then
    raise exception 'Physics 10 bank must contain at least 50 published questions; found %', total_count;
  end if;
  if invalid_count > 0 then
    raise exception 'Physics 10 bank contains % incomplete published questions', invalid_count;
  end if;
end $$;
