do $$
declare
  table_name text;
begin
  foreach table_name in array array[
    'mentor_profiles',
    'mentor_preferences',
    'mentor_enrollments',
    'mentor_attempts',
    'mentor_chat_threads',
    'mentor_chat_messages',
    'mentor_timetable',
    'mentor_todos',
    'mentor_schedule_logs'
  ] loop
    if to_regclass(format('public.%I', table_name)) is not null
       and not exists (
         select 1 from pg_publication_tables
         where pubname = 'supabase_realtime'
           and schemaname = 'public'
           and tablename = table_name
       ) then
      execute format('alter publication supabase_realtime add table public.%I', table_name);
    end if;
  end loop;
end $$;
