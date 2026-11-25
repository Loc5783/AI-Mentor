-- Runs against the disposable CI database; all fixtures are rolled back.
begin;

insert into auth.users(id, email) values
  ('a1111111-1111-4111-8111-111111111111', 'team-file-owner@example.test'),
  ('a2222222-2222-4222-8222-222222222222', 'team-file-member@example.test'),
  ('a3333333-3333-4333-8333-333333333333', 'team-file-viewer@example.test');
insert into public.mentor_team_projects(id, title, owner_id)
  values ('proj-team-file-smoke', 'Team file smoke', 'a1111111-1111-4111-8111-111111111111');
insert into public.mentor_team_members(project_id, user_id, role, status) values
  ('proj-team-file-smoke', 'a2222222-2222-4222-8222-222222222222', 'MEMBER', 'ACTIVE'),
  ('proj-team-file-smoke', 'a3333333-3333-4333-8333-333333333333', 'VIEWER', 'ACTIVE');

do $$
begin
  assert (select public from storage.buckets where id = 'mentor-team-files') = false,
    'Team file bucket must be private';
  assert (select count(*) from pg_policies where schemaname = 'storage' and tablename = 'objects'
    and policyname in ('mentor_team_file_insert', 'mentor_team_file_read', 'mentor_team_file_delete')) = 3,
    'Team file storage policies are incomplete';
end;
$$;

set local role authenticated;
select set_config('request.jwt.claim.sub', 'a3333333-3333-4333-8333-333333333333', true);
insert into public.mentor_team_documents(id, project_id, title, content, uploaded_by,
  file_path, file_name, file_mime, file_size)
values ('doc-team-file-smoke', 'proj-team-file-smoke', 'Tệp kiểm thử', '',
  'a3333333-3333-4333-8333-333333333333',
  'proj-team-file-smoke/doc-team-file-smoke/source.pdf', 'test.pdf', 'application/pdf', 2);
do $$ begin
  assert (select count(*) from public.mentor_team_documents where id = 'doc-team-file-smoke') = 1,
    'Active viewer should be allowed to upload a file';
end $$;

delete from public.mentor_team_projects where id = 'proj-team-file-smoke';
do $$ begin
  assert (select count(*) from public.mentor_team_projects where id = 'proj-team-file-smoke') = 1,
    'Non-owner must not delete project';
end $$;

select set_config('request.jwt.claim.sub', 'a1111111-1111-4111-8111-111111111111', true);
delete from public.mentor_team_projects where id = 'proj-team-file-smoke';
do $$ begin
  assert (select count(*) from public.mentor_team_projects where id = 'proj-team-file-smoke') = 0,
    'Owner should be able to delete project';
end $$;

rollback;
