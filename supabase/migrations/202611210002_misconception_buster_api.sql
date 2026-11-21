begin;

-- Enhance mentor_misconception_verifications with sessionId, question prompt, learner selected option, answer timestamp
alter table public.mentor_misconception_verifications
  add column if not exists session_id uuid references public.mentor_ai_practice_sessions(id) on delete set null,
  add column if not exists question_prompt text,
  add column if not exists selected_option text,
  add column if not exists answered_at timestamptz default now();

-- Revoke direct modification on mentor_misconception_verifications from authenticated users.
-- Client cannot forge verification_status or prop*_correct directly!
revoke insert, update, delete on public.mentor_misconception_verifications from authenticated;
grant select on public.mentor_misconception_verifications to authenticated;

-- Server-side API for Xưởng phá mệnh đề (Misconception Buster)
create or replace function public.mentor_misconception_api(
  p_action text,
  p_id text,
  p_session_id uuid default null,
  p_question_id text default null,
  p_subject_id text default null,
  p_grade int default null,
  p_question_prompt text default null,
  p_selected_option text default null,
  p_misconception_reason text default null,
  p_reason_type text default null,
  p_ai_hint text default null,
  p_ai_counterexample text default null,
  p_prop1_prompt text default null,
  p_prop1_correct boolean default null,
  p_prop2_prompt text default null,
  p_prop2_correct boolean default null,
  p_prop1_ans boolean default null,
  p_prop2_ans boolean default null
) returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_rec public.mentor_misconception_verifications%rowtype;
  v_practice public.mentor_ai_practice_sessions%rowtype;
  v_ans jsonb;
  v_resolved boolean;
  v_status text;
begin
  if v_uid is null then
    raise exception using errcode = 'PT401', message = 'AUTHENTICATION_REQUIRED';
  end if;

  if p_action is null or p_id is null or length(trim(p_id)) = 0 then
    raise exception using errcode = 'PT400', message = 'INVALID_REQUEST';
  end if;

  if p_action = 'start' then
    if p_question_id is null or p_subject_id is null or p_grade is null
       or p_misconception_reason is null or p_reason_type is null
       or p_prop1_prompt is null or p_prop1_correct is null
       or p_prop2_prompt is null or p_prop2_correct is null then
      raise exception using errcode = 'PT400', message = 'MISSING_FIELDS';
    end if;

    if p_reason_type not in ('PRE_AUTHORED', 'CUSTOM') then
      raise exception using errcode = 'PT400', message = 'INVALID_REASON_TYPE';
    end if;

    -- If linked to a practice session, ensure session belongs to user and question is wrong
    if p_session_id is not null then
      select * into v_practice from public.mentor_ai_practice_sessions
      where id = p_session_id and user_id = v_uid;
      if v_practice.id is not null then
        v_ans := v_practice.answers -> p_question_id;
        if v_ans is not null and coalesce((v_ans ->> 'isCorrect')::boolean, false) = true then
          raise exception using errcode = 'PT409', message = 'QUESTION_WAS_NOT_WRONG';
        end if;
      end if;
    end if;

    -- Check if session already exists for this user and id
    select * into v_rec from public.mentor_misconception_verifications
    where id = p_id and user_id = v_uid;

    if v_rec.id is not null then
      -- If already resolved, cannot be downgraded
      return jsonb_build_object(
        'id', v_rec.id,
        'userId', v_rec.user_id,
        'sessionId', v_rec.session_id,
        'questionId', v_rec.question_id,
        'subjectId', v_rec.subject_id,
        'grade', v_rec.grade,
        'questionPrompt', v_rec.question_prompt,
        'selectedOption', v_rec.selected_option,
        'misconceptionReason', v_rec.misconception_reason,
        'reasonType', v_rec.reason_type,
        'aiHint', v_rec.ai_hint,
        'aiCounterexample', v_rec.ai_counterexample,
        'prop1Prompt', v_rec.prop1_prompt,
        'prop1UserAns', v_rec.prop1_user_ans,
        'prop2Prompt', v_rec.prop2_prompt,
        'prop2UserAns', v_rec.prop2_user_ans,
        'verificationStatus', v_rec.verification_status,
        'verifiedAt', v_rec.verified_at,
        'updatedAt', v_rec.updated_at
      );
    end if;

    insert into public.mentor_misconception_verifications (
      id, user_id, session_id, question_id, subject_id, grade,
      question_prompt, selected_option, misconception_reason, reason_type,
      ai_hint, ai_counterexample,
      prop1_prompt, prop1_correct, prop2_prompt, prop2_correct,
      verification_status, created_at, updated_at
    ) values (
      p_id, v_uid, p_session_id, p_question_id, p_subject_id, p_grade,
      p_question_prompt, p_selected_option, trim(p_misconception_reason), p_reason_type,
      p_ai_hint, p_ai_counterexample,
      p_prop1_prompt, p_prop1_correct, p_prop2_prompt, p_prop2_correct,
      'NEEDS_REVIEW', now(), now()
    ) returning * into v_rec;

    return jsonb_build_object(
      'id', v_rec.id,
      'userId', v_rec.user_id,
      'sessionId', v_rec.session_id,
      'questionId', v_rec.question_id,
      'subjectId', v_rec.subject_id,
      'grade', v_rec.grade,
      'questionPrompt', v_rec.question_prompt,
      'selectedOption', v_rec.selected_option,
      'misconceptionReason', v_rec.misconception_reason,
      'reasonType', v_rec.reason_type,
      'aiHint', v_rec.ai_hint,
      'aiCounterexample', v_rec.ai_counterexample,
      'prop1Prompt', v_rec.prop1_prompt,
      'prop1UserAns', v_rec.prop1_user_ans,
      'prop2Prompt', v_rec.prop2_prompt,
      'prop2UserAns', v_rec.prop2_user_ans,
      'verificationStatus', v_rec.verification_status,
      'verifiedAt', v_rec.verified_at,
      'updatedAt', v_rec.updated_at
    );

  elsif p_action = 'verify' then
    select * into v_rec from public.mentor_misconception_verifications
    where id = p_id and user_id = v_uid for update;

    if v_rec.id is null then
      raise exception using errcode = 'PT404', message = 'MISCONCEPTION_SESSION_NOT_FOUND';
    end if;

    if p_prop1_ans is null or p_prop2_ans is null then
      raise exception using errcode = 'PT400', message = 'ANSWERS_REQUIRED';
    end if;

    -- If already resolved, maintain RESOLVED idempotently!
    if v_rec.verification_status = 'RESOLVED' then
      return jsonb_build_object(
        'id', v_rec.id,
        'verificationStatus', 'RESOLVED',
        'prop1UserAns', v_rec.prop1_user_ans,
        'prop2UserAns', v_rec.prop2_user_ans,
        'verifiedAt', v_rec.verified_at
      );
    end if;

    -- SERVER-SIDE EVALUATION: BOTH propositions must match their correct boolean
    v_resolved := (p_prop1_ans = v_rec.prop1_correct) and (p_prop2_ans = v_rec.prop2_correct);
    v_status := case when v_resolved then 'RESOLVED' else 'NEEDS_REVIEW' end;

    update public.mentor_misconception_verifications set
      prop1_user_ans = p_prop1_ans,
      prop2_user_ans = p_prop2_ans,
      verification_status = v_status,
      verified_at = case when v_resolved then now() else verified_at end,
      updated_at = now()
    where id = v_rec.id and user_id = v_uid
    returning * into v_rec;

    return jsonb_build_object(
      'id', v_rec.id,
      'verificationStatus', v_rec.verification_status,
      'prop1UserAns', v_rec.prop1_user_ans,
      'prop2UserAns', v_rec.prop2_user_ans,
      'verifiedAt', v_rec.verified_at
    );

  elsif p_action = 'get' then
    select * into v_rec from public.mentor_misconception_verifications
    where id = p_id and user_id = v_uid;

    if v_rec.id is null then
      return 'null'::jsonb;
    end if;

    return jsonb_build_object(
      'id', v_rec.id,
      'userId', v_rec.user_id,
      'sessionId', v_rec.session_id,
      'questionId', v_rec.question_id,
      'subjectId', v_rec.subject_id,
      'grade', v_rec.grade,
      'questionPrompt', v_rec.question_prompt,
      'selectedOption', v_rec.selected_option,
      'misconceptionReason', v_rec.misconception_reason,
      'reasonType', v_rec.reason_type,
      'aiHint', v_rec.ai_hint,
      'aiCounterexample', v_rec.ai_counterexample,
      'prop1Prompt', v_rec.prop1_prompt,
      'prop1UserAns', v_rec.prop1_user_ans,
      'prop2Prompt', v_rec.prop2_prompt,
      'prop2UserAns', v_rec.prop2_user_ans,
      'verificationStatus', v_rec.verification_status,
      'verifiedAt', v_rec.verified_at,
      'updatedAt', v_rec.updated_at
    );

  else
    raise exception using errcode = 'PT400', message = 'UNKNOWN_ACTION';
  end if;
end;
$$;

revoke all on function public.mentor_misconception_api(text, text, uuid, text, text, int, text, text, text, text, text, text, text, boolean, text, boolean, boolean, boolean) from public, anon;
grant execute on function public.mentor_misconception_api(text, text, uuid, text, text, int, text, text, text, text, text, text, text, boolean, text, boolean, boolean, boolean) to authenticated;

commit;
