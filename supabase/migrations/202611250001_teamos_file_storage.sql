-- Team documents keep their existing text content; file metadata is optional.
alter table public.mentor_team_documents
  add column if not exists file_path text,
  add column if not exists file_name text,
  add column if not exists file_mime text,
  add column if not exists file_size bigint;

alter table public.mentor_team_documents
  add constraint mentor_team_documents_file_metadata_check check (
    (file_path is null and file_name is null and file_mime is null and file_size is null)
    or (file_path is not null and file_name is not null and file_mime is not null and file_size between 1 and 8388608
      and file_path like project_id || '/%'
      and char_length(file_name) between 1 and 180
      and file_mime in ('application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'text/plain', 'image/jpeg', 'image/png', 'image/webp'))
  );

insert into storage.buckets(id, name, public, file_size_limit)
values ('mentor-team-files', 'mentor-team-files', false, 8388608)
on conflict (id) do nothing;

-- Every active member, including a read-only member in other TeamOS areas, may upload.
drop policy if exists team_docs_insert on public.mentor_team_documents;
create policy team_docs_insert on public.mentor_team_documents
  for insert to authenticated
  with check (public.mentor_is_team_active_member(project_id, auth.uid()) and uploaded_by = auth.uid());

create policy mentor_team_file_insert on storage.objects
  for insert to authenticated with check (
    bucket_id = 'mentor-team-files'
    and public.mentor_is_team_active_member((storage.foldername(name))[1], auth.uid())
  );
create policy mentor_team_file_read on storage.objects
  for select to authenticated using (
    bucket_id = 'mentor-team-files'
    and public.mentor_is_team_active_member((storage.foldername(name))[1], auth.uid())
  );
create policy mentor_team_file_delete on storage.objects
  for delete to authenticated using (
    bucket_id = 'mentor-team-files'
    and public.mentor_is_team_active_member((storage.foldername(name))[1], auth.uid())
    and (public.mentor_get_team_role((storage.foldername(name))[1], auth.uid()) <> 'VIEWER'
      or owner_id::text = auth.uid()::text)
  );
