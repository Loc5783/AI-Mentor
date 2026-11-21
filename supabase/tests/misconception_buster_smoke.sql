begin;

-- Set up two test users
select set_config('test.user1', gen_random_uuid()::text, true), set_config('test.user2', gen_random_uuid()::text, true);
insert into auth.users (id, raw_user_meta_data) values
  (current_setting('test.user1')::uuid, '{}'),
  (current_setting('test.user2')::uuid, '{}');

do $$
declare
  v_uid1 uuid := current_setting('test.user1')::uuid;
  v_uid2 uuid := current_setting('test.user2')::uuid;
  v_sess_id uuid := gen_random_uuid();
  v_sess_user2 uuid := gen_random_uuid();
  v_res jsonb;
  v_sid text := 'misc_test_' || gen_random_uuid()::text;
begin
  -- Create a real practice session for user 1 with question q_math_wrong (wrong answer) and q_math_right (correct answer)
  insert into public.mentor_ai_practice_sessions (
    id, user_id, subject_id, grade, topic, difficulty, question_count, questions_snapshot, answers
  ) values (
    v_sess_id, v_uid1, 'math', 11, 'Hàm số', 'MEDIUM', 2,
    jsonb_build_array(
      jsonb_build_object('id', 'q_math_wrong', 'prompt', 'Tính giới hạn...', 'order', 1),
      jsonb_build_object('id', 'q_math_right', 'prompt', 'Tính đạo hàm...', 'order', 2)
    ),
    jsonb_build_object(
      'q_math_wrong', jsonb_build_object('selectedOption', 'A', 'isCorrect', false, 'revealed', true),
      'q_math_right', jsonb_build_object('selectedOption', 'B', 'isCorrect', true, 'revealed', true)
    )
  );

  -- Create a practice session for user 2
  insert into public.mentor_ai_practice_sessions (
    id, user_id, subject_id, grade, topic, difficulty, question_count, questions_snapshot, answers
  ) values (
    v_sess_user2, v_uid2, 'math', 11, 'Hàm số', 'MEDIUM', 1,
    jsonb_build_array(
      jsonb_build_object('id', 'q_user2', 'prompt', 'Câu hỏi user 2...', 'order', 1)
    ),
    jsonb_build_object(
      'q_user2', jsonb_build_object('selectedOption', 'C', 'isCorrect', false, 'revealed', true)
    )
  );

  -- Set context to User 1
  perform set_config('request.jwt.claim.sub', v_uid1::text, true);

  -- 1. Must reject if session does not exist or belongs to another user
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_user2, -- User 2's session
      p_question_id => 'q_user2',
      p_reason_id => 'math_reason_01'
    );
    raise exception 'MUST_REJECT_OTHER_USER_SESSION';
  exception when sqlstate 'PT404' then
    null; -- Expected: SESSION_NOT_FOUND_OR_FORBIDDEN
  end;

  -- 2. Must reject if question does not belong to session
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_id,
      p_question_id => 'q_nonexistent',
      p_reason_id => 'math_reason_01'
    );
    raise exception 'MUST_REJECT_NONEXISTENT_QUESTION';
  exception when sqlstate 'PT404' then
    null; -- Expected: QUESTION_NOT_IN_SESSION
  end;

  -- 3. Must reject if question was answered correctly
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_id,
      p_question_id => 'q_math_right',
      p_reason_id => 'math_reason_01'
    );
    raise exception 'MUST_REJECT_CORRECT_ANSWER';
  exception when sqlstate 'PT409' then
    null; -- Expected: QUESTION_NOT_ANSWERED_WRONGLY
  end;

  -- 4. Must reject unverified reason_id
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_id,
      p_question_id => 'q_math_wrong',
      p_reason_id => 'unverified_bogus_reason'
    );
    raise exception 'MUST_REJECT_UNVERIFIED_REASON';
  exception when sqlstate 'PT404' then
    null; -- Expected: UNVERIFIED_OR_UNKNOWN_REASON
  end;

  -- 5. Start successfully with verified reason: math_reason_03 (prop1: false, prop2: true)
  v_res := public.mentor_misconception_api(
    p_action => 'start',
    p_id => v_sid,
    p_session_id => v_sess_id,
    p_question_id => 'q_math_wrong',
    p_reason_id => 'math_reason_03'
  );

  if v_res->>'verificationStatus' <> 'NEEDS_REVIEW' then
    raise exception 'INITIAL_STATUS_MUST_BE_NEEDS_REVIEW';
  end if;

  -- Client cannot pass prop1_correct or prop2_correct to forge answers.
  -- math_reason_03 has prop1: false, prop2: true.
  -- 6. Attempting verify with 1 wrong answer (prop1: true, prop2: true) -> must remain NEEDS_REVIEW
  v_res := public.mentor_misconception_api(
    p_action => 'verify',
    p_id => v_sid,
    p_prop1_ans => true, -- wrong (correct is false)
    p_prop2_ans => true  -- correct
  );

  if v_res->>'verificationStatus' <> 'NEEDS_REVIEW' then
    raise exception 'WRONG_ANSWER_MUST_NOT_RESOLVE';
  end if;

  -- 7. Verify answering correctly based on SERVER catalog (prop1: false, prop2: true) -> RESOLVED
  v_res := public.mentor_misconception_api(
    p_action => 'verify',
    p_id => v_sid,
    p_prop1_ans => false, -- correct
    p_prop2_ans => true   -- correct
  );

  if v_res->>'verificationStatus' <> 'RESOLVED' then
    raise exception 'CORRECT_ANSWERS_MUST_RESOLVE';
  end if;

  -- 8. Idempotency test: subsequent calls on RESOLVED session keep it RESOLVED
  v_res := public.mentor_misconception_api(
    p_action => 'verify',
    p_id => v_sid,
    p_prop1_ans => true,
    p_prop2_ans => false
  );

  if v_res->>'verificationStatus' <> 'RESOLVED' then
    raise exception 'RESOLVED_MUST_BE_IDEMPOTENT';
  end if;

  -- 9. Security test: Direct table modification is prohibited
  begin
    update public.mentor_misconception_verifications set verification_status = 'RESOLVED' where id = v_sid;
    raise exception 'CLIENT_MUST_NOT_DIRECT_UPDATE';
  exception when insufficient_privilege then
    null; -- Expected: permission denied
  end;

  -- 10. Isolation test: User 2 cannot access User 1 session
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
      p_prop2_ans => true
    );
    raise exception 'USER2_CAN_VERIFY_USER1_SESSION';
  exception when sqlstate 'PT404' then
    null; -- Expected
  end;

end $$;

select 'PASS: verified catalog answer keys, session validation, privilege enforcement, and identity isolation' as result;
rollback;
