-- Disposable local database only. The transaction leaves no test accounts or projects behind.
begin;

do $$
declare s text;
declare n integer;
begin
  foreach s in array array['biology','chemistry','economics_law','english',
      'geography','history','literature','physics'] loop
    select count(*) into n from public.mentor_questions
    where grade=11 and subject_id=s and status='PUBLISHED'
      and id like 'g11-lesson-%'
      and nullif(trim(option_d),'') is not null
      and option_d not like '[Cần biên tập%'
      and option_d not in (option_a,option_b,option_c);
    if n<12 then raise exception 'Grade 11 % has only % complete lesson questions',s,n; end if;
  end loop;

  if to_regprocedure('public.mentor_is_team_member(text,uuid)') is null then
    raise exception 'Team membership must use Supabase UUID identities';
  end if;
  if exists (
    select 1 from pg_attribute a join pg_class c on c.oid=a.attrelid
    join pg_namespace n on n.oid=c.relnamespace
    where n.nspname='public' and
      ((c.relname='mentor_exam_packs' and a.attname='user_id') or
       (c.relname='mentor_team_projects' and a.attname='owner_id') or
       (c.relname='mentor_misconception_verifications' and a.attname='user_id'))
      and a.atttypid<>'uuid'::regtype
  ) then raise exception 'Feature tables must reference UUID users'; end if;
end $$;

select set_config('test.uid1',gen_random_uuid()::text,true);
select set_config('test.uid2',gen_random_uuid()::text,true);
insert into auth.users(id,raw_user_meta_data)
values (current_setting('test.uid1')::uuid,'{}'),
       (current_setting('test.uid2')::uuid,'{}');

set local role authenticated;
select set_config('request.jwt.claim.sub',current_setting('test.uid1'),true);
insert into public.mentor_exam_packs(id,user_id,title,subject_id,grade)
values ('smoke-private-pack',current_setting('test.uid1')::uuid,'Private','math',11);
insert into public.mentor_exam_pack_questions(
  id,pack_id,user_id,prompt,options,source_file_name)
values ('smoke-private-question','smoke-private-pack',
  current_setting('test.uid1')::uuid,'Test?','[]'::jsonb,'local.pdf');
insert into public.mentor_team_projects(id,title,owner_id)
values ('smoke-private-team','Private',current_setting('test.uid1')::uuid);

do $$
begin
  if (select count(*) from public.mentor_team_members where project_id='smoke-private-team')<>1 then
    raise exception 'Team owner membership was not created';
  end if;
end $$;

select set_config('request.jwt.claim.sub',current_setting('test.uid2'),true);
do $$
begin
  if exists(select 1 from public.mentor_exam_packs where id='smoke-private-pack')
     or exists(select 1 from public.mentor_exam_pack_questions where id='smoke-private-question')
     or exists(select 1 from public.mentor_team_projects where id='smoke-private-team') then
    raise exception 'Other learner can read private feature data';
  end if;
  update public.mentor_exam_packs set title='Changed' where id='smoke-private-pack';
  if found then raise exception 'Other learner can edit a private exam pack'; end if;
  begin
    insert into public.mentor_exam_pack_questions(
      id,pack_id,user_id,prompt,options,source_file_name)
    values ('smoke-foreign-question','smoke-private-pack',
      current_setting('test.uid2')::uuid,'Test?','[]'::jsonb,'local.pdf');
    raise exception 'Other learner added a question to a private pack';
  exception
    when insufficient_privilege then null;
  end;
  begin
    insert into public.mentor_team_members(project_id,user_id)
    values ('smoke-private-team',current_setting('test.uid2')::uuid);
    raise exception 'Other learner joined a private team without invitation';
  exception
    when insufficient_privilege then null;
  end;
end $$;

rollback;
