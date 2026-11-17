begin;
-- Assessment sessions may be independent of course enrollment. The client
-- starts a timer only while a learner is working in a foreground assessment.
create or replace function public.mentor_study_action(op text,data jsonb default '{}') returns jsonb
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
    if subject not in ('math','literature','english','history','geography',
      'economics_law','physics','chemistry','biology') then
      raise sqlstate 'PT400' using message='INVALID_SUBJECT';
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
commit;
