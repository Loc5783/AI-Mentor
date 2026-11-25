begin;
insert into auth.users(id,email) values
 ('a3333333-3333-4333-8333-333333333333','team-search-owner@example.test'),
 ('a4444444-4444-4444-8444-444444444444','team-search-other@example.test');
update public.mentor_profiles set nickname='Mạnh Quỳnh' where id='a3333333-3333-4333-8333-333333333333';
update public.mentor_profiles set nickname='Đỗ Linh' where id='a4444444-4444-4444-8444-444444444444';
set local role authenticated;
select set_config('request.jwt.claim.sub','a3333333-3333-4333-8333-333333333333',true);
do $$ begin
 assert exists(select 1 from public.mentor_search_learners('Quynh') where id='a3333333-3333-4333-8333-333333333333');
 assert exists(select 1 from public.mentor_search_learners('Do Linh') where id='a4444444-4444-4444-8444-444444444444');
 assert (select count(*) from public.mentor_search_learners('Q'))=0;
end $$;
rollback;
