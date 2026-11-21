begin;

-- Set up two test users
select set_config('test.user1', gen_random_uuid()::text, true), set_config('test.user2', gen_random_uuid()::text, true);
insert into auth.users (id, raw_user_meta_data) values
  (current_setting('test.user1')::uuid, '{}'),
  (current_setting('test.user2')::uuid, '{}');

set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('test.user1'), true);

do $$
declare
  v_res jsonb;
  v_uid1 uuid := current_setting('test.user1')::uuid;
  v_uid2 uuid := current_setting('test.user2')::uuid;
  v_sid text := 'misc_test_' || gen_random_uuid()::text;
begin
  -- 1. Start a misconception session for user 1
  v_res := public.mentor_misconception_api(
    p_action => 'start',
    p_id => v_sid,
    p_question_id => 'q_test_01',
    p_subject_id => 'math',
    p_grade => 11,
    p_question_prompt => 'Tìm tập xác định...',
    p_selected_option => 'A',
    p_misconception_reason => 'math_reason_01',
    p_reason_type => 'PRE_AUTHORED',
    p_ai_hint => 'Hint here',
    p_ai_counterexample => 'Counter here',
    p_prop1_prompt => 'Prop 1 prompt',
    p_prop1_correct => false,
    p_prop2_prompt => 'Prop 2 prompt',
    p_prop2_correct => false
  );

  if v_res->>'verificationStatus' <> 'NEEDS_REVIEW' then
    raise exception 'INITIAL_STATUS_MUST_BE_NEEDS_REVIEW';
  end if;

  -- 2. Verify answering 1 correct, 1 wrong -> status remains NEEDS_REVIEW
  v_res := public.mentor_misconception_api(
    p_action => 'verify',
    p_id => v_sid,
    p_prop1_ans => false, -- correct
    p_prop2_ans => true   -- wrong
  );

  if v_res->>'verificationStatus' <> 'NEEDS_REVIEW' then
    raise exception 'PARTIAL_ANSWERS_MUST_NOT_RESOLVE';
  end if;

  -- 3. Verify answering both correct -> status becomes RESOLVED
  v_res := public.mentor_misconception_api(
    p_action => 'verify',
    p_id => v_sid,
    p_prop1_ans => false, -- correct
    p_prop2_ans => false  -- correct
  );

  if v_res->>'verificationStatus' <> 'RESOLVED' then
    raise exception 'BOTH_CORRECT_MUST_RESOLVE';
  end if;

  -- 4. Idempotency test: resubmitting wrong answers on RESOLVED session must keep it RESOLVED
  v_res := public.mentor_misconception_api(
    p_action => 'verify',
    p_id => v_sid,
    p_prop1_ans => true, -- wrong
    p_prop2_ans => true  -- wrong
  );

  if v_res->>'verificationStatus' <> 'RESOLVED' then
    raise exception 'RESOLVED_MUST_BE_IDEMPOTENT';
  end if;

  -- 5. Security test: Client cannot directly update or insert into mentor_misconception_verifications
  begin
    update public.mentor_misconception_verifications set verification_status = 'RESOLVED' where id = v_sid;
    raise exception 'CLIENT_MUST_NOT_DIRECT_UPDATE';
  exception when insufficient_privilege then
    null; -- Expected: permission denied
  end;

  begin
    insert into public.mentor_misconception_verifications (
      id, user_id, question_id, subject_id, grade, misconception_reason, reason_type,
      prop1_prompt, prop1_correct, prop2_prompt, prop2_correct, verification_status
    ) values (
      'forged_session', v_uid1, 'q1', 'math', 11, 'reason', 'PRE_AUTHORED',
      'p1', true, 'p2', true, 'RESOLVED'
    );
    raise exception 'CLIENT_MUST_NOT_DIRECT_INSERT';
  exception when insufficient_privilege then
    null; -- Expected: permission denied
  end;

  -- 6. Isolation test: user 2 cannot see or verify user 1 session
  perform set_config('request.jwt.claim.sub', v_uid2::text, true);

  v_res := public.mentor_misconception_api(
    p_action => 'get',
    p_id => v_sid
  );
  if v_res is not null and v_res::text <> 'null' then
    raise exception 'USER2_CAN_SEE_USER1_SESSION';
  end if;

  begin
    perform public.mentor_misconception_api(
      p_action => 'verify',
      p_id => v_sid,
      p_prop1_ans => false,
      p_prop2_ans => false
    );
    raise exception 'USER2_CAN_VERIFY_USER1_SESSION';
  exception when sqlstate 'PT404' then
    null; -- Expected: session not found for user 2
  end;

end $$;

reset role;
select 'PASS: misconception buster server evaluation, idempotency, privilege restriction, and user isolation' as result;
rollback;
