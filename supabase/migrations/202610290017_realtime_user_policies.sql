begin;

do $$
declare item record;
begin
  for item in select * from (values
    ('mentor_awards','user_id = auth.uid()'),
    ('mentor_learning_days','user_id = auth.uid()'),
    ('mentor_profiles','id = auth.uid()'),
    ('mentor_preferences','user_id = auth.uid()'),
    ('mentor_enrollments','user_id = auth.uid()'),
    ('mentor_attempts','user_id = auth.uid()'),
    ('mentor_friendships','requester = auth.uid() or recipient = auth.uid()'),
    ('mentor_chat_threads','user_id = auth.uid()'),
    ('mentor_chat_messages','user_id = auth.uid()'),
    ('mentor_timetable','user_id = auth.uid()'),
    ('mentor_todos','user_id = auth.uid()'),
    ('mentor_schedule_logs','user_id = auth.uid()'),
    ('mentor_study_sessions','user_id = auth.uid()'),
    ('mentor_study_events','user_id = auth.uid()'),
    ('mentor_badge_awards','user_id = auth.uid()'),
    ('mentor_practice_sessions','user_id = auth.uid()'),
    ('mentor_wrong_questions','user_id = auth.uid()')
  ) as policies(table_name,predicate)
  loop
    if to_regclass('public.'||item.table_name) is not null then
      execute format('alter table public.%I enable row level security',item.table_name);
      execute format('grant select on public.%I to authenticated',item.table_name);
      if not exists(select 1 from pg_policies where schemaname='public' and tablename=item.table_name and policyname='mentor_realtime_owner_read') then
        execute format('create policy mentor_realtime_owner_read on public.%I for select to authenticated using (%s)',item.table_name,item.predicate);
      end if;
    end if;
  end loop;
  if exists(select 1 from pg_publication where pubname='supabase_realtime') then
    for item in select unnest(array['mentor_study_sessions','mentor_study_events','mentor_badge_awards','mentor_practice_sessions','mentor_wrong_questions']) as table_name
    loop
      if not exists(select 1 from pg_publication_tables where pubname='supabase_realtime' and schemaname='public' and tablename=item.table_name) then
        execute format('alter publication supabase_realtime add table public.%I',item.table_name);
      end if;
    end loop;
  end if;
end $$;

commit;
