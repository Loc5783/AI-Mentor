begin;

create table public.mentor_study_sessions(
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  subject_id text not null,
  grade int not null check(grade between 10 and 12),
  state text not null default 'ACTIVE' check(state in ('ACTIVE','CLOSED')),
  started_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  ended_at timestamptz,
  total_seconds int not null default 0 check(total_seconds>=0),
  unique(id,user_id)
);
create table public.mentor_study_events(
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  session_id uuid not null,
  event_key uuid not null,
  subject_id text not null,
  grade int not null check(grade between 10 and 12),
  learning_day date not null,
  seconds int not null check(seconds between 0 and 120),
  created_at timestamptz not null default now(),
  unique(user_id,event_key),
  foreign key(session_id,user_id) references public.mentor_study_sessions(id,user_id) on delete cascade
);
create unique index mentor_one_active_study_session on public.mentor_study_sessions(user_id) where state='ACTIVE';
create index mentor_study_events_user_day on public.mentor_study_events(user_id,learning_day);
alter table public.mentor_study_sessions enable row level security;
alter table public.mentor_study_events enable row level security;
revoke all on public.mentor_study_sessions,public.mentor_study_events from public,anon,authenticated;

create function public.mentor_study_action(op text,data jsonb default '{}') returns jsonb
language plpgsql security definer set search_path='' as $$
declare
  uid uuid:=auth.uid(); sid uuid:=nullif(data->>'sessionId','')::uuid;
  event_id uuid:=nullif(data->>'eventKey','')::uuid;
  subject text; g int; row_session public.mentor_study_sessions;
  elapsed int; day date:=(now() at time zone 'Asia/Ho_Chi_Minh')::date;
begin
  if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
  if sid is null then raise sqlstate 'PT400' using message='INVALID_SESSION_ID'; end if;
  if op='start' then
    perform pg_advisory_xact_lock(hashtextextended(uid::text,2));
    subject:=lower(trim(coalesce(data->>'subjectId',''))); g:=(data->>'grade')::int;
    if g not between 10 and 12 then raise sqlstate 'PT400' using message='INVALID_GRADE'; end if;
    if not exists(select 1 from public.mentor_enrollments where user_id=uid and subject_id=subject and grade=g and selected) then
      raise sqlstate 'PT409' using message='SUBJECT_NOT_SELECTED';
    end if;
    select * into row_session from public.mentor_study_sessions where id=sid and user_id=uid;
    if row_session.id is not null then
      if row_session.subject_id<>subject or row_session.grade<>g then raise sqlstate 'PT409' using message='STUDY_SESSION_SCOPE_CONFLICT'; end if;
      return jsonb_build_object('sessionId',sid,'subjectId',subject,'grade',g,'state',row_session.state,'studySeconds',row_session.total_seconds);
    end if;
    update public.mentor_study_sessions set state='CLOSED',ended_at=now(),last_seen_at=now()
      where user_id=uid and state='ACTIVE';
    insert into public.mentor_study_sessions(id,user_id,subject_id,grade)
      values(sid,uid,subject,g) on conflict(id) do nothing;
    select * into row_session from public.mentor_study_sessions where id=sid and user_id=uid;
    if row_session.id is null then raise sqlstate 'PT404' using message='STUDY_SESSION_NOT_FOUND'; end if;
    if row_session.subject_id<>subject or row_session.grade<>g then raise sqlstate 'PT409' using message='STUDY_SESSION_SCOPE_CONFLICT'; end if;
    return jsonb_build_object('sessionId',sid,'subjectId',subject,'grade',g,'state',row_session.state,'studySeconds',row_session.total_seconds);
  elsif op not in ('heartbeat','stop') or event_id is null then
    raise sqlstate 'PT400' using message='INVALID_STUDY_OPERATION';
  end if;

  select * into row_session from public.mentor_study_sessions where id=sid and user_id=uid for update;
  if row_session.id is null then raise sqlstate 'PT404' using message='STUDY_SESSION_NOT_FOUND'; end if;
  if exists(select 1 from public.mentor_study_events where user_id=uid and event_key=event_id) then
    return jsonb_build_object('sessionId',sid,'state',row_session.state,'studySeconds',row_session.total_seconds,'addedSeconds',0,'replayed',true);
  end if;
  if row_session.state<>'ACTIVE' then raise sqlstate 'PT409' using message='STUDY_SESSION_CLOSED'; end if;
  elapsed:=least(120,greatest(0,floor(extract(epoch from (now()-row_session.last_seen_at)))::int));
  insert into public.mentor_study_events(user_id,session_id,event_key,subject_id,grade,learning_day,seconds)
    values(uid,sid,event_id,row_session.subject_id,row_session.grade,day,elapsed);
  update public.mentor_study_sessions set last_seen_at=now(),
    total_seconds=total_seconds+elapsed,
    state=case when op='stop' then 'CLOSED' else state end,
    ended_at=case when op='stop' then now() else ended_at end
    where id=sid and user_id=uid returning * into row_session;
  return jsonb_build_object('sessionId',sid,'state',row_session.state,
    'studySeconds',row_session.total_seconds,'addedSeconds',elapsed,'replayed',false);
end; $$;
revoke all on function public.mentor_study_action(text,jsonb) from public,anon;
grant execute on function public.mentor_study_action(text,jsonb) to authenticated;

create table public.mentor_badge_catalog(
  code text primary key,
  title text not null,
  description text not null,
  rule text not null check(rule in ('FIRST_LESSON','TEN_LESSONS','SEVEN_DAY_STREAK','ONE_THOUSAND_XP'))
);
create table public.mentor_badge_awards(
  user_id uuid not null references auth.users(id) on delete cascade,
  badge_code text not null references public.mentor_badge_catalog(code),
  unlocked_at timestamptz not null default now(),
  primary key(user_id,badge_code)
);
insert into public.mentor_badge_catalog(code,title,description,rule) values
 ('FIRST_LESSON','Bước khởi đầu','Hoàn thành bài học đầu tiên.','FIRST_LESSON'),
 ('TEN_LESSONS','Bền bỉ tiến bước','Hoàn thành 10 bài học.','TEN_LESSONS'),
 ('SEVEN_DAY_STREAK','Một tuần kiên trì','Học liên tiếp 7 ngày.','SEVEN_DAY_STREAK'),
 ('ONE_THOUSAND_XP','Người tích lũy','Đạt 1.000 EXP từ học tập.','ONE_THOUSAND_XP')
on conflict(code) do update set title=excluded.title,description=excluded.description,rule=excluded.rule;
alter table public.mentor_badge_catalog enable row level security;
alter table public.mentor_badge_awards enable row level security;
revoke all on public.mentor_badge_catalog,public.mentor_badge_awards from public,anon,authenticated;

create function public.mentor_badges() returns jsonb language plpgsql security definer set search_path='' as $$
declare uid uuid:=auth.uid(); rows jsonb;
begin
  if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
  insert into public.mentor_badge_awards(user_id,badge_code)
  select uid,b.code from public.mentor_badge_catalog b where
    case b.rule
      when 'FIRST_LESSON' then exists(select 1 from public.mentor_awards a join public.mentor_course_units u on u.subject_id=a.subject_id and u.grade=a.grade and u.unit_id=a.unit_id and u.kind='LESSON' where a.user_id=uid)
      when 'TEN_LESSONS' then (select count(*) from public.mentor_awards a join public.mentor_course_units u on u.subject_id=a.subject_id and u.grade=a.grade and u.unit_id=a.unit_id and u.kind='LESSON' where a.user_id=uid)>=10
      when 'SEVEN_DAY_STREAK' then public.mentor_streak(uid)>=7
      when 'ONE_THOUSAND_XP' then (select coalesce(sum(xp),0) from public.mentor_awards a where a.user_id=uid)>=1000
      else false end
  on conflict(user_id,badge_code) do nothing;
  select coalesce(jsonb_agg(jsonb_build_object('code',b.code,'title',b.title,'description',b.description,
      'unlocked',a.badge_code is not null,'unlockedAt',a.unlocked_at) order by b.code),'[]') into rows
    from public.mentor_badge_catalog b left join public.mentor_badge_awards a on a.badge_code=b.code and a.user_id=uid;
  return jsonb_build_object('badges',rows);
end; $$;
revoke all on function public.mentor_badges() from public,anon;
grant execute on function public.mentor_badges() to authenticated;

create function public.mentor_study_summary() returns jsonb language sql security definer set search_path='' as $$
  select jsonb_build_object(
    'totalSeconds',coalesce((select sum(seconds) from public.mentor_study_events where user_id=auth.uid()),0),
    'week',coalesce((select jsonb_agg(jsonb_build_object('day',d::date,'seconds',coalesce(x.seconds,0)) order by d)
      from generate_series((now() at time zone 'Asia/Ho_Chi_Minh')::date-6,
        (now() at time zone 'Asia/Ho_Chi_Minh')::date,interval '1 day') d
      left join (select learning_day,sum(seconds)::int seconds from public.mentor_study_events
        where user_id=auth.uid() and learning_day>=(now() at time zone 'Asia/Ho_Chi_Minh')::date-6
        group by learning_day) x on x.learning_day=d::date),'[]'),
    'subjects',coalesce((select jsonb_agg(jsonb_build_object('subjectId',subject_id,'seconds',seconds) order by subject_id)
      from (select subject_id,sum(seconds)::int seconds from public.mentor_study_events
        where user_id=auth.uid() group by subject_id) s),'[]')
  ) where auth.uid() is not null;
$$;
revoke all on function public.mentor_study_summary() from public,anon;
grant execute on function public.mentor_study_summary() to authenticated;

do $$ begin
  if exists(select 1 from pg_publication where pubname='supabase_realtime') then
    alter publication supabase_realtime add table public.mentor_study_sessions;
    alter publication supabase_realtime add table public.mentor_study_events;
    alter publication supabase_realtime add table public.mentor_badge_awards;
  end if;
exception when duplicate_object then null; end $$;

commit;
