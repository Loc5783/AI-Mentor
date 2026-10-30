create or replace function public.mentor_overview(g int) returns jsonb
language plpgsql security definer set search_path='' as $$
declare
  uid uuid:=auth.uid();
  items jsonb;
  series jsonb;
  total bigint;
  daily bigint;
  completed int;
  lessons int;
  practice jsonb;
begin
  if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
  if g not between 10 and 12 then raise sqlstate 'PT400' using message='INVALID_GRADE'; end if;

  select jsonb_agg(jsonb_build_object(
    'subjectId',s,
    'curriculumAvailable',exists(select 1 from public.mentor_course_units where subject_id=s and grade=g),
    'totalXp',coalesce((select sum(xp) from public.mentor_awards where user_id=uid and subject_id=s),0),
    'completedLessons',case when exists(select 1 from public.mentor_enrollments e where e.user_id=uid and e.grade=g and e.subject_id=s and e.selected)
      then (select count(*) from public.mentor_awards a join public.mentor_course_units u on u.subject_id=a.subject_id and u.grade=a.grade and u.unit_id=a.unit_id and u.kind='LESSON' where a.user_id=uid and a.subject_id=s and a.grade=g)
      else 0 end,
    'totalLessons',case when exists(select 1 from public.mentor_enrollments e where e.user_id=uid and e.grade=g and e.subject_id=s and e.selected)
      then (select count(*) from public.mentor_course_units where subject_id=s and grade=g and kind='LESSON')
      else 0 end
  )) into items
  from unnest(array['math','literature','english','history','geography','physics','chemistry','biology','economics_law']) s;

  select coalesce(sum((value->>'totalXp')::bigint),0),
         coalesce(sum((value->>'completedLessons')::int),0),
         coalesce(sum((value->>'totalLessons')::int),0)
    into total,completed,lessons from jsonb_array_elements(items);
  select coalesce(sum(xp),0) into daily from public.mentor_awards
    where user_id=uid and learning_day=(now() at time zone 'Asia/Ho_Chi_Minh')::date;
  select jsonb_build_object('questionCount',count(*),
      'firstTryCorrect',count(*) filter(where (v.value->>'firstTryCorrect')::boolean),
      'assisted',count(*) filter(where (v.value->>'assisted')::boolean))
    into practice from public.mentor_attempts a cross join lateral jsonb_each(a.answers) v
    where a.user_id=uid and a.kind<>'PLACEMENT';
  select jsonb_agg(jsonb_build_object('day',d::date,
      'xp',coalesce((select sum(xp) from public.mentor_awards where user_id=uid and learning_day=d::date),0),
      'streakLit',exists(select 1 from public.mentor_learning_days where user_id=uid and day=d::date)) order by d)
    into series from generate_series((now() at time zone 'Asia/Ho_Chi_Minh')::date-6,
      (now() at time zone 'Asia/Ho_Chi_Minh')::date,interval '1 day') d;

  return jsonb_build_object(
    'profileId',uid,'grade',g,'totalXp',total,'dailyXp',daily,'completedLessons',completed,
    'totalLessons',lessons,'streakDays',public.mentor_streak(uid),
    'rankTier',case when total>=10000 then 'DIAMOND' when total>=5000 then 'PLATINUM'
      when total>=2000 then 'GOLD' when total>=500 then 'SILVER' when total>0 then 'BRONZE' else 'UNRANKED' end,
    'subjects',items,'practice',coalesce(practice,jsonb_build_object('questionCount',0,'firstTryCorrect',0,'assisted',0)),
    'week',series);
end; $$;

revoke all on function public.mentor_overview(int) from public,anon;
grant execute on function public.mentor_overview(int) to authenticated;
