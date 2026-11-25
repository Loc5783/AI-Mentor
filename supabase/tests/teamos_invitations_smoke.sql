begin;
insert into auth.users(id,email) values
 ('a1111111-1111-4111-8111-111111111111','team-invite-owner@example.test'),
 ('a2222222-2222-4222-8222-222222222222','team-invite-guest@example.test');
insert into public.mentor_team_projects(id,title,owner_id)
values('proj-team-invite-smoke','Invite smoke','a1111111-1111-4111-8111-111111111111');
set local role authenticated;
select set_config('request.jwt.claim.sub','a1111111-1111-4111-8111-111111111111',true);
select public.mentor_team_invite('proj-team-invite-smoke','a2222222-2222-4222-8222-222222222222','MEMBER');
do $$ begin
 assert (select status from public.mentor_team_members where project_id='proj-team-invite-smoke' and user_id='a2222222-2222-4222-8222-222222222222')='INVITED';
 assert not has_table_privilege('authenticated','public.mentor_team_members','UPDATE');
end $$;
select set_config('request.jwt.claim.sub','a2222222-2222-4222-8222-222222222222',true);
do $$ begin
 assert not public.mentor_is_team_active_member('proj-team-invite-smoke',auth.uid());
 assert (select count(*) from public.mentor_team_my_invites())=1;
end $$;
select public.mentor_team_respond_invite('proj-team-invite-smoke',true);
do $$ begin assert public.mentor_is_team_active_member('proj-team-invite-smoke',auth.uid()); end $$;
select set_config('request.jwt.claim.sub','a1111111-1111-4111-8111-111111111111',true);
select public.mentor_team_change_role('proj-team-invite-smoke','a2222222-2222-4222-8222-222222222222','VIEWER');
do $$ begin
 assert (select role from public.mentor_team_members where project_id='proj-team-invite-smoke' and user_id='a2222222-2222-4222-8222-222222222222')='VIEWER';
end $$;
select public.mentor_team_remove_member('proj-team-invite-smoke','a2222222-2222-4222-8222-222222222222');
select set_config('request.jwt.claim.sub','a2222222-2222-4222-8222-222222222222',true);
do $$ begin assert not public.mentor_is_team_active_member('proj-team-invite-smoke',auth.uid()); end $$;
rollback;
