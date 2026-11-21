begin;

-- Confidence is a learner report about one graded attempt, never a score or mastery label.
create table public.mentor_ai_answer_confidence (
  user_id uuid not null references auth.users(id) on delete cascade,
  session_id uuid not null references public.mentor_ai_practice_sessions(id) on delete cascade,
  question_id text not null,
  attempt_count integer not null check (attempt_count > 0),
  confidence text not null check (confidence in ('LOW', 'MEDIUM', 'HIGH')),
  updated_at timestamptz not null default now(),
  primary key (session_id, question_id)
);

create index mentor_ai_answer_confidence_owner_idx
  on public.mentor_ai_answer_confidence (user_id, updated_at desc);

alter table public.mentor_ai_answer_confidence enable row level security;
revoke all on public.mentor_ai_answer_confidence from public, anon, authenticated;

create function public.mentor_ai_answer_confidence_api(
  p_action text,
  p_session_id uuid,
  p_question_id text,
  p_confidence text default null,
  p_attempt_count integer default null
) returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_session public.mentor_ai_practice_sessions%rowtype;
  v_answer jsonb;
  v_saved public.mentor_ai_answer_confidence%rowtype;
  v_current_attempt integer;
begin
  if v_uid is null then
    raise exception using errcode='PT401', message='AUTHENTICATION_REQUIRED';
  end if;
  if p_action is null or p_action not in ('get', 'set') or p_session_id is null
    or length(coalesce(p_question_id, '')) not between 1 and 100 then
    raise exception using errcode='PT400', message='INVALID_CONFIDENCE_REQUEST';
  end if;

  select * into v_session from public.mentor_ai_practice_sessions
    where id = p_session_id and user_id = v_uid for update;
  if v_session.id is null then
    raise exception using errcode='PT404', message='SESSION_NOT_FOUND';
  end if;
  v_answer := v_session.answers -> p_question_id;
  if v_answer is null or coalesce((v_answer ->> 'revealed')::boolean, false) = false
    or coalesce((v_answer ->> 'skipped')::boolean, false) then
    raise exception using errcode='PT409', message='GRADED_ANSWER_REQUIRED';
  end if;
  v_current_attempt := (v_answer ->> 'attemptsCount')::integer;
  if v_current_attempt is null or v_current_attempt < 1 then
    raise exception using errcode='PT409', message='GRADED_ANSWER_REQUIRED';
  end if;

  if p_action = 'set' then
    if p_confidence is null or p_confidence not in ('LOW', 'MEDIUM', 'HIGH')
      or p_attempt_count is distinct from v_current_attempt then
      raise exception using errcode='PT409', message='ANSWER_CHANGED';
    end if;
    insert into public.mentor_ai_answer_confidence
      (user_id, session_id, question_id, attempt_count, confidence)
    values (v_uid, p_session_id, p_question_id, v_current_attempt, p_confidence)
    on conflict (session_id, question_id) do update
      set attempt_count = excluded.attempt_count,
          confidence = excluded.confidence,
          updated_at = now()
      where public.mentor_ai_answer_confidence.user_id = v_uid;
    if not found then
      raise exception using errcode='PT409', message='CONFIDENCE_OWNER_CONFLICT';
    end if;
    return jsonb_build_object('confidence', p_confidence, 'attemptCount', v_current_attempt);
  end if;

  select * into v_saved from public.mentor_ai_answer_confidence
    where user_id = v_uid and session_id = p_session_id and question_id = p_question_id;
  if v_saved.session_id is null or v_saved.attempt_count <> v_current_attempt then
    return jsonb_build_object('confidence', null, 'attemptCount', v_current_attempt);
  end if;
  return jsonb_build_object('confidence', v_saved.confidence,
    'attemptCount', v_saved.attempt_count);
end;
$$;

revoke all on function public.mentor_ai_answer_confidence_api(text,uuid,text,text,integer)
  from public, anon;
grant execute on function public.mentor_ai_answer_confidence_api(text,uuid,text,text,integer)
  to authenticated;

commit;
