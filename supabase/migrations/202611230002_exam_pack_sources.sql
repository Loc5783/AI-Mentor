-- Private source documents for user-reviewed exam packs.
insert into storage.buckets(id, name, public, file_size_limit)
values ('mentor-exam-sources', 'mentor-exam-sources', false, 4194304)
on conflict (id) do nothing;

create policy mentor_exam_source_insert on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'mentor-exam-sources'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and exists (select 1 from public.mentor_exam_packs p
      where p.id = (storage.foldername(name))[2] and p.user_id = (select auth.uid()))
  );
create policy mentor_exam_source_read on storage.objects
  for select to authenticated
  using (
    bucket_id = 'mentor-exam-sources'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );
create policy mentor_exam_source_update on storage.objects
  for update to authenticated
  using (
    bucket_id = 'mentor-exam-sources'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  )
  with check (
    bucket_id = 'mentor-exam-sources'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and exists (select 1 from public.mentor_exam_packs p
      where p.id = (storage.foldername(name))[2] and p.user_id = (select auth.uid()))
  );
create policy mentor_exam_source_delete on storage.objects
  for delete to authenticated
  using (
    bucket_id = 'mentor-exam-sources'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );
