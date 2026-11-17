do $$
declare fn regprocedure := to_regprocedure('public.mentor_choose_learning_start(uuid)');
begin
  if fn is null then
    raise exception 'Supabase is missing mentor_choose_learning_start(uuid); apply migration 202611160005_start_course_from_beginning.sql';
  end if;
  if not has_function_privilege('authenticated', fn, 'EXECUTE') then
    raise exception 'authenticated role cannot execute mentor_choose_learning_start(uuid)';
  end if;
end $$;
