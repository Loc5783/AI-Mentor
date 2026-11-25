-- Run on a disposable test database after migrations, never on production.
begin;

do $$
begin
  if has_table_privilege('authenticated', 'public.mentor_practice_sessions', 'SELECT')
     or has_table_privilege('authenticated', 'public.mentor_practice_sessions', 'INSERT')
     or has_table_privilege('authenticated', 'public.mentor_practice_sessions', 'UPDATE')
     or has_table_privilege('authenticated', 'public.mentor_practice_sessions', 'DELETE') then
    raise exception 'PRACTICE_SESSION_DIRECT_ACCESS';
  end if;
  if has_table_privilege('authenticated', 'public.mentor_wrong_questions', 'INSERT')
     or has_table_privilege('authenticated', 'public.mentor_wrong_questions', 'UPDATE')
     or has_table_privilege('authenticated', 'public.mentor_wrong_questions', 'DELETE') then
    raise exception 'WRONG_NOTEBOOK_DIRECT_WRITE';
  end if;
  if not has_table_privilege('authenticated', 'public.mentor_wrong_questions', 'SELECT') then
    raise exception 'WRONG_NOTEBOOK_READ_MISSING';
  end if;
  if not has_function_privilege('authenticated',
      'public.mentor_library_api(text,text,jsonb,jsonb)', 'EXECUTE') then
    raise exception 'LIBRARY_RPC_UNAVAILABLE';
  end if;
end $$;

rollback;
