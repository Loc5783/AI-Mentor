begin;
do $$ begin
  if not exists (select 1 from storage.buckets where id='mentor-exam-sources' and not public and file_size_limit=4194304) then
    raise exception 'Private exam source bucket missing';
  end if;
  if (select count(*) from pg_policies where schemaname='storage' and tablename='objects'
      and policyname like 'mentor_exam_source_%') <> 4 then
    raise exception 'Exam source access policies missing';
  end if;
end $$;
rollback;
