begin;
create function public.mentor_public_learner(profile_id uuid) returns jsonb language sql stable security definer set search_path='' as $$
 select jsonb_build_object('id',p.id,'nickname',p.nickname,'bio',p.bio,'avatarUrl',p.avatar_path,'learningDay',(now() at time zone 'Asia/Ho_Chi_Minh')::date,'dailyXp',coalesce((select sum(xp) from public.mentor_awards where user_id=p.id and learning_day=(now() at time zone 'Asia/Ho_Chi_Minh')::date),0),'streakDays',public.mentor_streak(p.id)) from public.mentor_profiles p where p.id=profile_id;
$$;
revoke all on function public.mentor_public_learner(uuid) from public,anon,authenticated;
create function public.mentor_overview(g int) returns jsonb language plpgsql security definer set search_path='' as $$
declare uid uuid:=auth.uid(); items jsonb; series jsonb; total bigint; daily bigint; completed int; lessons int; practice jsonb;
begin
 if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
 if g not between 10 and 12 then raise sqlstate 'PT400' using message='INVALID_GRADE'; end if;
 select jsonb_agg(jsonb_build_object('subjectId',s,'curriculumAvailable',exists(select 1 from public.mentor_course_units where subject_id=s and grade=g),'totalXp',coalesce((select sum(xp) from public.mentor_awards where user_id=uid and subject_id=s),0),'completedLessons',(select count(*) from public.mentor_awards a join public.mentor_course_units u on u.subject_id=a.subject_id and u.grade=a.grade and u.unit_id=a.unit_id and u.kind='LESSON' where a.user_id=uid and a.subject_id=s and a.grade=g),'totalLessons',(select count(*) from public.mentor_course_units where subject_id=s and grade=g and kind='LESSON'))) into items from unnest(array['math','literature','english','history','geography','physics','chemistry','biology','economics_law']) s;
 select coalesce(sum((value->>'totalXp')::bigint),0),coalesce(sum((value->>'completedLessons')::int),0),coalesce(sum((value->>'totalLessons')::int),0) into total,completed,lessons from jsonb_array_elements(items);
 select coalesce(sum(xp),0) into daily from public.mentor_awards where user_id=uid and learning_day=(now() at time zone 'Asia/Ho_Chi_Minh')::date;
 select jsonb_build_object('questionCount',count(*),'firstTryCorrect',count(*) filter(where (v.value->>'firstTryCorrect')::boolean),'assisted',count(*) filter(where (v.value->>'assisted')::boolean)) into practice from public.mentor_attempts a cross join lateral jsonb_each(a.answers) v where a.user_id=uid and a.kind<>'PLACEMENT';
 select jsonb_agg(jsonb_build_object('day',d::date,'xp',coalesce((select sum(xp) from public.mentor_awards where user_id=uid and learning_day=d::date),0),'streakLit',exists(select 1 from public.mentor_learning_days where user_id=uid and day=d::date)) order by d) into series from generate_series((now() at time zone 'Asia/Ho_Chi_Minh')::date-6,(now() at time zone 'Asia/Ho_Chi_Minh')::date,interval '1 day') d;
 return jsonb_build_object('profileId',uid,'grade',g,'totalXp',total,'dailyXp',daily,'completedLessons',completed,'totalLessons',lessons,'streakDays',public.mentor_streak(uid),'rankTier',case when total=0 then 'UNRANKED' else 'BRONZE' end,'subjects',items,'practice',practice,'week',series);
end; $$;
revoke all on function public.mentor_overview(int) from public,anon;
grant execute on function public.mentor_overview(int) to authenticated;
create table public.mentor_friendships(id uuid primary key default gen_random_uuid(),requester uuid not null references auth.users(id) on delete cascade,recipient uuid not null references auth.users(id) on delete cascade,state text not null default 'PENDING' check(state in ('PENDING','ACCEPTED')),created_at timestamptz not null default now(),check(requester<>recipient));
create unique index mentor_one_friendship on public.mentor_friendships(least(requester,recipient),greatest(requester,recipient));
alter table public.mentor_friendships enable row level security;
revoke all on public.mentor_friendships from anon,authenticated;
create function public.mentor_social_api(method text,path text,body jsonb default '{}',query jsonb default '{}') returns jsonb language plpgsql security definer set search_path='' as $$
declare uid uuid:=auth.uid(); result jsonb; metric text:=coalesce(query->>'metric','STREAK'); scope text:=coalesce(query->>'scope','GLOBAL'); target uuid; item uuid; rows jsonb;
begin
 if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
 if path='/rankings' then
  if metric not in ('STREAK','DAILY_XP') or scope not in ('FRIENDS','GLOBAL') then raise sqlstate 'PT400' using message='INVALID_RANKING'; end if;
  select coalesce(jsonb_agg(jsonb_build_object('rank',r,'value',score,'profile',profile) order by score desc,pid),'[]') into rows from (
   select pid,profile,score,dense_rank() over(order by score desc) r from (
    select p.id pid,public.mentor_public_learner(p.id) profile,case when metric='STREAK' then public.mentor_streak(p.id)::bigint else coalesce((select sum(xp) from public.mentor_awards where user_id=p.id and learning_day=(now() at time zone 'Asia/Ho_Chi_Minh')::date),0) end score
    from public.mentor_profiles p where scope='GLOBAL' or p.id=uid or exists(select 1 from public.mentor_friendships where state='ACCEPTED' and ((requester=uid and recipient=p.id) or (recipient=uid and requester=p.id)))
   ) scores order by score desc,pid limit 100
  ) ranked;
  result:=jsonb_build_object('metric',metric,'scope',scope,'learningDay',(now() at time zone 'Asia/Ho_Chi_Minh')::date,'entries',rows);
 elsif path='/me/public-profile' and method='PATCH' then
  update public.mentor_profiles set nickname=coalesce(body->>'nickname',nickname),bio=coalesce(body->>'bio',bio) where id=uid;
  result:=public.mentor_public_learner(uid);
 elsif path='/me/public-profile' then result:=public.mentor_public_learner(uid);
 elsif path ~ '^/profiles/[^/]+$' then result:=public.mentor_public_learner(split_part(path,'/',3)::uuid);
 elsif path='/me/account' then
  result:=jsonb_build_object('id',uid,'username',uid,'profile',public.mentor_public_learner(uid));
 elsif path='/friendships' and method='GET' then
  select jsonb_build_object('items',coalesce(jsonb_agg(jsonb_build_object('id',f.id,'status',f.state,'direction',case when f.requester=uid then 'OUTGOING' else 'INCOMING' end,'profile',public.mentor_public_learner(case when f.requester=uid then f.recipient else f.requester end))),'[]')) into result from public.mentor_friendships f where f.requester=uid or f.recipient=uid;
 elsif path='/friendships' and method='POST' then
  target:=(body->>'profileId')::uuid;
  insert into public.mentor_friendships(requester,recipient) values(uid,target) returning id into item;
  result:=jsonb_build_object('id',item,'status','PENDING');
 elsif path ~ '^/friendships/[^/]+$' and method='DELETE' then
  delete from public.mentor_friendships where id=split_part(path,'/',3)::uuid and (requester=uid or recipient=uid);
  result:='{}';
 elsif path ~ '^/friendships/[^/]+$' and method='PATCH' then
  update public.mentor_friendships set state='ACCEPTED' where id=split_part(path,'/',3)::uuid and recipient=uid returning id into item;
  if item is null then raise sqlstate 'PT404' using message='FRIENDSHIP_NOT_FOUND'; end if;
  result:=jsonb_build_object('id',item,'status','ACCEPTED');
 else raise sqlstate 'PT404' using message='API_NOT_IMPLEMENTED'; end if;
 if result is null then raise sqlstate 'PT404' using message='PROFILE_NOT_FOUND'; end if;
 return result;
end; $$;
revoke all on function public.mentor_social_api(text,text,jsonb,jsonb) from public,anon;
grant execute on function public.mentor_social_api(text,text,jsonb,jsonb) to authenticated;
commit;
