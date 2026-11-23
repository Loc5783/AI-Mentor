begin;

-- Restore the most recent verification for the same practice question on another device.
-- The response deliberately excludes both answer keys from the private catalog.
create or replace function public.mentor_misconception_resume(
  p_session_id uuid,
  p_question_id text
) returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_rec public.mentor_misconception_verifications%rowtype;
begin
  if v_uid is null then
    raise exception using errcode = 'PT401', message = 'AUTHENTICATION_REQUIRED';
  end if;
  if p_session_id is null or p_question_id is null or length(trim(p_question_id)) = 0 then
    raise exception using errcode = 'PT400', message = 'SESSION_AND_QUESTION_REQUIRED';
  end if;

  select * into v_rec
  from public.mentor_misconception_verifications
  where user_id = v_uid and session_id = p_session_id and question_id = p_question_id
  order by updated_at desc, created_at desc, id desc
  limit 1;

  if v_rec.id is null then return 'null'::jsonb; end if;
  return jsonb_build_object(
    'id', v_rec.id,
    'userId', v_rec.user_id,
    'sessionId', v_rec.session_id,
    'questionId', v_rec.question_id,
    'reasonId', v_rec.reason_id,
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
    'updatedAtEpochMs', (extract(epoch from v_rec.updated_at) * 1000)::bigint
  );
end;
$$;

revoke all on function public.mentor_misconception_resume(uuid, text) from public, anon;
grant execute on function public.mentor_misconception_resume(uuid, text) to authenticated;

commit;
