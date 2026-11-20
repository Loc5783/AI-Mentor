do $$
declare s text;
declare total_count integer;
declare invalid_count integer;
begin
  foreach s in array array['physics','chemistry'] loop
    select count(*) into total_count from public.mentor_questions
      where subject_id=s and grade=11 and status='PUBLISHED';
    if total_count<50 then raise exception 'Grade 11 % bank has only % published questions',s,total_count; end if;
    select count(*) into invalid_count from public.mentor_questions
      where subject_id=s and grade=11 and status='PUBLISHED'
        and (trim(prompt)='' or trim(option_a)='' or trim(option_b)='' or trim(option_c)='' or trim(option_d)=''
          or option_d in(option_a,option_b,option_c) or correct_option not in('A','B','C','D') or trim(explanation)='');
    if invalid_count>0 then raise exception 'Grade 11 % bank has % invalid published questions',s,invalid_count; end if;
  end loop;
end $$;
