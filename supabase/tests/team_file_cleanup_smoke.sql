begin;
insert into auth.users(id,email) values
  ('b1111111-1111-4111-8111-111111111111','team-cleanup-owner@example.test');
insert into public.mentor_team_projects(id,title,owner_id) values
  ('proj-team-cleanup-smoke','Cleanup test','b1111111-1111-4111-8111-111111111111');
set local role authenticated;
select set_config('request.jwt.claim.sub','b1111111-1111-4111-8111-111111111111',true);
insert into public.mentor_team_file_cleanup(project_id,owner_id,file_paths) values
  ('proj-team-cleanup-smoke','b1111111-1111-4111-8111-111111111111',
   '["proj-team-cleanup-smoke/doc-11111111-1111-4111-8111-111111111111/source.pdf"]'::jsonb);
delete from public.mentor_team_projects where id='proj-team-cleanup-smoke';
do $$ begin
  assert (select count(*) from public.mentor_team_file_cleanup
    where project_id='proj-team-cleanup-smoke')=1,
    'Cleanup job must survive project deletion';
end $$;
delete from public.mentor_team_file_cleanup where project_id='proj-team-cleanup-smoke';
do $$ begin
  assert (select count(*) from public.mentor_team_file_cleanup
    where project_id='proj-team-cleanup-smoke')=0,
    'Owner must be able to finish cleanup after project deletion';
end $$;
rollback;
