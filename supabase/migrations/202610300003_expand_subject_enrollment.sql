begin;
create or replace function public.mentor_select_subjects(g int, subjects text[])
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  uid uuid := auth.uid();
  s text;
  allowed constant text[] := array[
    'math','literature','english','history','geography',
    'physics','chemistry','biology','economics_law'
  ];
begin
  if uid is null then
    raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED';
  end if;
  if g not between 10 and 12
     or subjects is null
     or cardinality(subjects) > cardinality(allowed)
     or not (subjects <@ allowed) then
    raise sqlstate 'PT400' using message='INVALID_SUBJECTS';
  end if;

  perform pg_advisory_xact_lock(hashtextextended(uid::text, 0));
  update public.mentor_enrollments
    set selected=false
    where user_id=uid and grade=g;

  foreach s in array subjects loop
    insert into public.mentor_enrollments(user_id,subject_id,grade,selected)
    values(uid,s,g,true)
    on conflict(user_id,subject_id,grade)
    do update set selected=true;
  end loop;
  return public.mentor_selected_subjects(g);
end;
$$;
revoke all on function public.mentor_select_subjects(int,text[]) from public,anon;
grant execute on function public.mentor_select_subjects(int,text[]) to authenticated;
commit;