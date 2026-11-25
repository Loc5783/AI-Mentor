-- Durable cleanup records survive the project row and its cascades.
create table if not exists public.mentor_team_file_cleanup (
  project_id text primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  file_paths jsonb not null check (jsonb_typeof(file_paths) = 'array'),
  created_at timestamptz not null default now()
);
alter table public.mentor_team_file_cleanup enable row level security;
revoke all on public.mentor_team_file_cleanup from public, anon;
grant select, insert, update, delete on public.mentor_team_file_cleanup to authenticated;
create policy team_file_cleanup_owner on public.mentor_team_file_cleanup
  for all to authenticated using (owner_id = auth.uid())
  with check (owner_id = auth.uid() and exists (
    select 1 from public.mentor_team_projects p
    where p.id = project_id and p.owner_id = auth.uid()));

drop policy if exists mentor_team_file_read on storage.objects;
create policy mentor_team_file_read on storage.objects
  for select to authenticated using (
    bucket_id = 'mentor-team-files' and (
      public.mentor_is_team_active_member((storage.foldername(name))[1], auth.uid())
      or exists (
        select 1 from public.mentor_team_file_cleanup c
        where c.project_id = (storage.foldername(name))[1]
          and c.owner_id = auth.uid() and c.file_paths ? name
      )
    )
  );

drop policy if exists mentor_team_file_delete on storage.objects;
create policy mentor_team_file_delete on storage.objects
  for delete to authenticated using (
    bucket_id = 'mentor-team-files' and (
      (public.mentor_is_team_active_member((storage.foldername(name))[1], auth.uid())
        and (public.mentor_get_team_role((storage.foldername(name))[1], auth.uid()) <> 'VIEWER'
          or owner_id::text = auth.uid()::text))
      or exists (
        select 1 from public.mentor_team_file_cleanup c
        where c.project_id = (storage.foldername(name))[1]
          and c.owner_id = auth.uid()
          and c.file_paths ? name
      )
    )
  );
