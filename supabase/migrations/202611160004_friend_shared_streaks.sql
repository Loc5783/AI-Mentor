begin;

-- The pair-streak table was imported for legacy compatibility but had no writer.
-- Derive shared study streaks from authoritative learning days instead.
do $$ begin
  if to_regclass('public.mentor_friend_streaks') is not null then
    comment on table public.mentor_friend_streaks is
      'Legacy imported snapshot; current shared streaks are derived from mentor_learning_days.';
  end if;
end $$;

create or replace function public.mentor_friend_shared_streaks()
returns jsonb
language plpgsql
stable
security definer
set search_path=''
as $$
declare
  uid uuid := auth.uid();
  today date := (now() at time zone 'Asia/Ho_Chi_Minh')::date;
  result jsonb;
begin
  if uid is null then
    raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED';
  end if;

  with friends as (
    select f.id friendship_id,
      case when f.requester=uid then f.recipient else f.requester end friend_id
    from public.mentor_friendships f
    where f.state='ACCEPTED' and (f.requester=uid or f.recipient=uid)
  ), scores as (
    select f.friendship_id,f.friend_id,
      coalesce(s.streak_days,0)::int streak_days,
      s.last_shared_day
    from friends f
    left join lateral (
      with shared_days as (
        select mine.day
        from public.mentor_learning_days mine
        join public.mentor_learning_days theirs on theirs.day=mine.day and theirs.user_id=f.friend_id
        where mine.user_id=uid and mine.day<=today
      ), ranked as (
        select day,row_number() over(order by day desc) n,max(day) over() last_day
        from shared_days
      )
      select case
          when max(last_day)>=today-1
            then count(*) filter(where day=last_day-(n::int-1))::int
          else 0
        end streak_days,
        max(last_day) last_shared_day
      from ranked
    ) s on true
  )
  select jsonb_build_object('streaks',coalesce(jsonb_agg(jsonb_build_object(
    'friendshipId',scores.friendship_id,
    'friendId',scores.friend_id,
    'streakDays',scores.streak_days,
    'lastSharedDay',scores.last_shared_day
  ) order by scores.friend_id),'[]'::jsonb))
  into result from scores;

  return result;
end;
$$;

revoke all on function public.mentor_friend_shared_streaks() from public,anon;
grant execute on function public.mentor_friend_shared_streaks() to authenticated;

commit;
