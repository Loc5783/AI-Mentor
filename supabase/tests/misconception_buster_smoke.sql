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
begin
  -- Create practice sessions with various question states
  insert into public.mentor_ai_practice_sessions (
    id, user_id, subject_id, grade, topic, difficulty, question_count, questions_snapshot, answers
  ) values (
    v_sess_id, v_uid1, 'math', 11, 'Lượng giác', 'MEDIUM', 5,
    jsonb_build_array(
      jsonb_build_object('id', 'q_math_wrong', 'prompt', 'Phương trình sin x = 2...', 'order', 1),
      jsonb_build_object('id', 'q_math_right', 'prompt', 'Tính đạo hàm...', 'order', 2),
      jsonb_build_object('id', 'q_math_skipped', 'prompt', 'Tính tích phân...', 'order', 3),
      jsonb_build_object('id', 'q_math_unrevealed', 'prompt', 'Tìm tập xác định...', 'order', 4),
      jsonb_build_object('id', 'q_math_unanswered', 'prompt', 'Giải bất phương trình...', 'order', 5)
    ),
    jsonb_build_object(
      'q_math_wrong', jsonb_build_object('selectedOption', 'A', 'isCorrect', false, 'revealed', true, 'skipped', false),
      'q_math_right', jsonb_build_object('selectedOption', 'B', 'isCorrect', true, 'revealed', true, 'skipped', false),
      'q_math_skipped', jsonb_build_object('selectedOption', null, 'isCorrect', false, 'revealed', true, 'skipped', true),
      'q_math_unrevealed', jsonb_build_object('selectedOption', 'A', 'isCorrect', false, 'revealed', false, 'skipped', false)
    )
  );

  insert into public.mentor_ai_practice_sessions (
    id, user_id, subject_id, grade, topic, difficulty, question_count, questions_snapshot, answers
  ) values (
    v_sess_user2, v_uid2, 'math', 11, 'Hàm số', 'MEDIUM', 1,
    jsonb_build_array(
      jsonb_build_object('id', 'q_user2', 'prompt', 'Câu hỏi user 2...', 'order', 1)
    ),
    jsonb_build_object(
      'q_user2', jsonb_build_object('selectedOption', 'C', 'isCorrect', false, 'revealed', true, 'skipped', false)
    )
  );

  perform set_config('test.practice1', v_sess_id::text, true);
  perform set_config('test.practice2', v_sess_user2::text, true);

  insert into public.mentor_misconception_catalog (
    reason_id, subject_id, grade, reason_text, verified_hint, counterexample,
    prop1_prompt, prop1_correct, prop2_prompt, prop2_correct
  ) values (
    'test_economics_reason_11', 'economics-law', 11, 'Test reason', 'Test hint',
    'Test counterexample', 'Test proposition 1', true, 'Test proposition 2', false
  );
end $$;

-- Switch to AUTHENTICATED role as User 1
set local role authenticated;
select set_config('request.jwt.claim.sub', current_setting('test.user1'), true);

do $$
declare
  v_uid1 uuid := current_setting('test.user1')::uuid;
  v_uid2 uuid := current_setting('test.user2')::uuid;
  v_sess_id uuid := current_setting('test.practice1')::uuid;
  v_sess_user2 uuid := current_setting('test.practice2')::uuid;
  v_res jsonb;
  v_sid text := 'misc_test_' || gen_random_uuid()::text;
  v_secret_prop1 boolean;
  v_secret_prop2 boolean;
begin
  -- 1. The authenticated role has neither table nor RLS-policy access to answer data.
  if has_table_privilege('authenticated', 'public.mentor_misconception_catalog', 'select')
     or has_column_privilege('authenticated', 'public.mentor_misconception_catalog', 'prop1_correct', 'select')
     or has_column_privilege('authenticated', 'public.mentor_misconception_catalog', 'prop2_correct', 'select') then
    raise exception 'AUTHENTICATED_HAS_CATALOG_ANSWER_PRIVILEGE';
  end if;

  if has_table_privilege('authenticated', 'public.mentor_misconception_verifications', 'select')
     or has_column_privilege('authenticated', 'public.mentor_misconception_verifications', 'prop1_correct', 'select')
     or has_column_privilege('authenticated', 'public.mentor_misconception_verifications', 'prop2_correct', 'select') then
    raise exception 'AUTHENTICATED_HAS_VERIFICATION_ANSWER_PRIVILEGE';
  end if;

  if not (select relrowsecurity from pg_class where oid = 'public.mentor_misconception_catalog'::regclass)
     or not (select relrowsecurity from pg_class where oid = 'public.mentor_misconception_verifications'::regclass) then
    raise exception 'MISCONCEPTION_TABLE_RLS_MUST_BE_ENABLED';
  end if;

  if exists (
    select 1 from pg_policies
    where schemaname = 'public'
      and tablename in ('mentor_misconception_catalog', 'mentor_misconception_verifications')
      and ('authenticated' = any(roles) or 'public' = any(roles))
  ) then
    raise exception 'MISCONCEPTION_TABLE_MUST_NOT_HAVE_CLIENT_POLICIES';
  end if;

  -- Authenticated users cannot read either answer-key column on the catalog.
  begin
    select prop1_correct, prop2_correct into v_secret_prop1, v_secret_prop2
    from public.mentor_misconception_catalog limit 1;
    raise exception 'AUTHENTICATED_MUST_NOT_READ_CATALOG_ANSWER_KEYS';
  exception when insufficient_privilege then
    null; -- Expected: permission denied
  end;

  -- 2. Security: Authenticated user CANNOT directly INSERT or UPDATE mentor_misconception_verifications
  begin
    insert into public.mentor_misconception_verifications (
      id, user_id, session_id, question_id, subject_id, grade, misconception_reason, reason_type,
      prop1_prompt, prop1_correct, prop2_prompt, prop2_correct, verification_status
    ) values (
      'forged_row', v_uid1, v_sess_id, 'q_math_wrong', 'math', 11, 'Forged', 'PRE_AUTHORED',
      'p1', true, 'p2', true, 'RESOLVED'
    );
    raise exception 'AUTHENTICATED_MUST_NOT_DIRECT_INSERT';
  exception when insufficient_privilege then
    null; -- Expected: permission denied
  end;

  -- 3. list_reasons returns display fields but never answer keys.
  v_res := public.mentor_misconception_api(
    p_action => 'list_reasons',
    p_subject_id => 'math',
    p_grade => 11
  );
  if jsonb_array_length(v_res->'items') = 0 then
    raise exception 'LIST_REASONS_SHOULD_RETURN_GRADE_11_MATH';
  end if;
  if (v_res->'items'->0) ? 'prop1_correct'
     or (v_res->'items'->0) ? 'prop2_correct'
     or (v_res->'items'->0) ? 'prop1Correct'
     or (v_res->'items'->0) ? 'prop2Correct' then
    raise exception 'LIST_REASONS_LEAKED_ANSWER_KEYS';
  end if;
  if not ((v_res->'items'->0) ? 'verifiedHint')
     or not ((v_res->'items'->0) ? 'counterexample')
     or not ((v_res->'items'->0) ? 'prop1Prompt')
     or not ((v_res->'items'->0) ? 'prop2Prompt') then
    raise exception 'LIST_REASONS_MISSING_DISPLAY_FIELDS';
  end if;

  -- 4. Must reject if session belongs to another user
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_user2,
      p_question_id => 'q_user2',
      p_reason_id => 'math_reason_03'
    );
    raise exception 'MUST_REJECT_OTHER_USER_SESSION';
  exception when sqlstate 'PT404' then
    null; -- Expected
  end;

  -- 5. Must reject if question was skipped
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_id,
      p_question_id => 'q_math_skipped',
      p_reason_id => 'math_reason_03'
    );
    raise exception 'MUST_REJECT_SKIPPED_QUESTION';
  exception when sqlstate 'PT409' then
    null; -- Expected: QUESTION_NOT_ANSWERED_WRONGLY
  end;

  -- 6. Must reject a question that has not been revealed.
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_id,
      p_question_id => 'q_math_unrevealed',
      p_reason_id => 'math_reason_03'
    );
    raise exception 'MUST_REJECT_UNREVEALED_QUESTION';
  exception when sqlstate 'PT409' then
    null; -- Expected
  end;

  -- 7. Must reject if question was answered correctly
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_id,
      p_question_id => 'q_math_right',
      p_reason_id => 'math_reason_03'
    );
    raise exception 'MUST_REJECT_CORRECT_ANSWER';
  exception when sqlstate 'PT409' then
    null; -- Expected
  end;

  -- 8. Must reject a question without a recorded answer.
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_id,
      p_question_id => 'q_math_unanswered',
      p_reason_id => 'math_reason_03'
    );
    raise exception 'MUST_REJECT_UNANSWERED_QUESTION';
  exception when sqlstate 'PT409' then
    null; -- Expected
  end;

  -- 9. Must reject a reason from a different grade.
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_id,
      p_question_id => 'q_math_wrong',
      p_reason_id => 'math_reason_01'
    );
    raise exception 'MUST_REJECT_GRADE_MISMATCH';
  exception when sqlstate 'PT400' then
    null; -- Expected: REASON_SUBJECT_OR_GRADE_MISMATCH
  end;

  -- 10. Must reject a reason from a different subject even when the grade matches.
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => v_sess_id,
      p_question_id => 'q_math_wrong',
      p_reason_id => 'test_economics_reason_11'
    );
    raise exception 'MUST_REJECT_SUBJECT_MISMATCH';
  exception when sqlstate 'PT400' then
    null; -- Expected: REASON_SUBJECT_OR_GRADE_MISMATCH
  end;

  -- 11. Must reject a nonexistent practice session.
  begin
    perform public.mentor_misconception_api(
      p_action => 'start',
      p_id => v_sid,
      p_session_id => gen_random_uuid(),
      p_question_id => 'q_math_wrong',
      p_reason_id => 'math_reason_03'
    );
    raise exception 'MUST_REJECT_MISSING_SESSION';
  exception when sqlstate 'PT404' then
    null; -- Expected
  end;

  -- 12. Start successfully with matching grade 11 reason: math_reason_03 (correct: false, true).
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

  v_res := public.mentor_misconception_api(p_action => 'get', p_id => v_sid);
  if v_res ? 'prop1_correct' or v_res ? 'prop2_correct'
     or v_res ? 'prop1Correct' or v_res ? 'prop2Correct' then
    raise exception 'GET_API_LEAKED_ANSWER_KEYS';
  end if;

  -- Reopening the same practice question on a second device finds its cloud state.
  v_res := public.mentor_misconception_resume(v_sess_id, 'q_math_wrong');
  if v_res->>'id' <> v_sid or v_res ? 'prop1Correct' or v_res ? 'prop2Correct' then
    raise exception 'RESUME_MUST_RETURN_OWN_SESSION_WITHOUT_ANSWER_KEYS';
  end if;
  if public.mentor_misconception_resume(v_sess_id, 'q_math_unanswered') is not null then
    raise exception 'RESUME_RETURNED_UNSTARTED_QUESTION';
  end if;

  -- Authenticated users cannot read answer keys from verification rows or alter status.
  begin
    select prop1_correct, prop2_correct into v_secret_prop1, v_secret_prop2
    from public.mentor_misconception_verifications where id = v_sid;
    raise exception 'AUTHENTICATED_MUST_NOT_READ_VERIFICATION_ANSWER_KEYS';
  exception when insufficient_privilege then
    null; -- Expected: permission denied
  end;

  begin
    update public.mentor_misconception_verifications
    set verification_status = 'RESOLVED' where id = v_sid;
    raise exception 'AUTHENTICATED_MUST_NOT_DIRECT_UPDATE';
  exception when insufficient_privilege then
    null; -- Expected: permission denied
  end;

  -- 13. Verify answering 1 wrong (true, true) -> must remain NEEDS_REVIEW.
  v_res := public.mentor_misconception_api(
    p_action => 'verify',
    p_id => v_sid,
    p_prop1_ans => true, -- wrong
    p_prop2_ans => true  -- right
  );
  if v_res->>'verificationStatus' <> 'NEEDS_REVIEW' then
    raise exception 'WRONG_ANSWER_MUST_NOT_RESOLVE';
  end if;

  -- 14. Verify answering both correctly (false, true) -> RESOLVED.
  v_res := public.mentor_misconception_api(
    p_action => 'verify',
    p_id => v_sid,
    p_prop1_ans => false,
    p_prop2_ans => true
  );
  if v_res->>'verificationStatus' <> 'RESOLVED' then
    raise exception 'CORRECT_ANSWERS_MUST_RESOLVE';
  end if;

  -- 15. Idempotency test: resubmitting wrong answers on RESOLVED session keeps RESOLVED.
  v_res := public.mentor_misconception_api(
    p_action => 'verify',
    p_id => v_sid,
    p_prop1_ans => true,
    p_prop2_ans => false
  );
  if v_res->>'verificationStatus' <> 'RESOLVED' then
    raise exception 'RESOLVED_MUST_BE_IDEMPOTENT';
  end if;

  -- 16. Isolation test: User 2 cannot access User 1 session.
  perform set_config('request.jwt.claim.sub', v_uid2::text, true);

  v_res := public.mentor_misconception_api(
    p_action => 'get',
    p_id => v_sid
  );
  if v_res is not null and v_res::text <> 'null' then
    raise exception 'USER2_CAN_SEE_USER1_SESSION';
  end if;

  if public.mentor_misconception_resume(v_sess_id, 'q_math_wrong') is not null then
    raise exception 'USER2_CAN_RESUME_USER1_SESSION';
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

reset role;
select 'PASS: authenticated grants and RLS, safe catalog fields, server-side status, wrong-answer validation, subject/grade matching, skipped rejection, and user isolation' as result;
rollback;
