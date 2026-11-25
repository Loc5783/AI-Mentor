begin;

-- Enhance mentor_misconception_verifications with sessionId, question prompt, learner selected option, answer timestamp
alter table public.mentor_misconception_verifications
  add column if not exists session_id uuid references public.mentor_ai_practice_sessions(id) on delete set null,
  add column if not exists question_prompt text,
  add column if not exists selected_option text,
  add column if not exists answered_at timestamptz default now();

-- All reads and writes go through the security-definer API. In particular,
-- clients must not be able to read prop*_correct from their own rows.
revoke all on public.mentor_misconception_verifications from public, anon, authenticated;
drop policy if exists misconception_verifications_owner on public.mentor_misconception_verifications;

-- Server-side catalog table for trusted misconception propositions & answer keys.
-- PROTECTED: authenticated users have NO DIRECT SELECT on this table to prevent leaking answer keys!
create table if not exists public.mentor_misconception_catalog (
  reason_id text primary key,
  subject_id text not null,
  grade int not null,
  reason_text text not null,
  verified_hint text not null,
  counterexample text not null,
  prop1_prompt text not null,
  prop1_correct boolean not null,
  prop2_prompt text not null,
  prop2_correct boolean not null,
  created_at timestamptz not null default now()
);

alter table public.mentor_misconception_catalog enable row level security;
revoke all on public.mentor_misconception_catalog from public, anon, authenticated;

-- Seed pre-verified catalog items
insert into public.mentor_misconception_catalog (
  reason_id, subject_id, grade, reason_text, verified_hint, counterexample,
  prop1_prompt, prop1_correct, prop2_prompt, prop2_correct
) values
(
  'math_reason_01', 'math', 10,
  'Nhầm lẫn giữa mệnh đề đảo và mệnh đề phản đảo (Không suy ra hai chiều)',
  'Mệnh đề P ⇒ Q không có nghĩa Q ⇒ P luôn đúng. Phản ví dụ: x = 2 ⇒ x² = 4, nhưng x² = 4 ⇒ x = 2 hoặc x = -2.',
  'Nếu x² = 9 thì x có nhất thiết bằng 3 không? Không, vì x có thể bằng -3.',
  'Mệnh đề ''Nếu x > 2 thì x² > 4'' có mệnh đề đảo luôn đúng?', false,
  'Nếu x² > 4 thì x có nhất thiết phải lớn hơn 2 không?', false
),
(
  'math_reason_02', 'math', 10,
  'Quên trường hợp hệ số a = 0 khi xét phương trình bậc hai ax² + bx + c = 0',
  'Khi hệ số a chứa tham số m, nếu a = 0 phương trình trở thành bậc nhất bx + c = 0, có thể có 1 nghiệm thay vì vô nghiệm hoặc 2 nghiệm.',
  'Xét (m-1)x² + 2x + 1 = 0 với m = 1: phương trình thành 2x + 1 = 0, có nghiệm x = -1/2.',
  'Phương trình (m-1)x² + 2x + 1 = 0 luôn là phương trình bậc hai với mọi m?', false,
  'Khi m = 1, phương trình (m-1)x² + 2x + 1 = 0 có đúng 1 nghiệm?', true
),
(
  'math_reason_03', 'math', 11,
  'Nhầm lẫn tập giá trị của sin x và cos x khi giải phương trình',
  'Với mọi x ∈ R, ta luôn có -1 ≤ sin x ≤ 1 và -1 ≤ cos x ≤ 1. Phương trình sin x = 2 vô nghiệm.',
  'sin x = 1.5 không có nghiệm thực nào.',
  'Phương trình sin x = -1.2 có nghiệm thực x?', false,
  'Tập giá trị của hàm số y = 2 cos x là [-2; 2]?', true
)
on conflict (reason_id) do update set
  subject_id = excluded.subject_id,
  grade = excluded.grade,
  reason_text = excluded.reason_text,
  verified_hint = excluded.verified_hint,
  counterexample = excluded.counterexample,
  prop1_prompt = excluded.prop1_prompt,
  prop1_correct = excluded.prop1_correct,
  prop2_prompt = excluded.prop2_prompt,
  prop2_correct = excluded.prop2_correct;

alter table public.mentor_misconception_verifications
  add column if not exists reason_id text references public.mentor_misconception_catalog(reason_id);

update public.mentor_misconception_verifications v
set reason_id = c.reason_id
from public.mentor_misconception_catalog c
where v.reason_id is null
  and v.reason_type = 'PRE_AUTHORED'
  and v.subject_id = c.subject_id
  and v.grade = c.grade
  and v.misconception_reason = c.reason_text;

-- Server-side API for Xưởng phá mệnh đề (Misconception Buster)
create or replace function public.mentor_misconception_api(
  p_action text,
  p_id text default null,
  p_session_id uuid default null,
  p_question_id text default null,
  p_reason_id text default null,
  p_prop1_ans boolean default null,
  p_prop2_ans boolean default null,
  p_subject_id text default null,
  p_grade int default null
) returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_rec public.mentor_misconception_verifications%rowtype;
  v_practice public.mentor_ai_practice_sessions%rowtype;
  v_cat public.mentor_misconception_catalog%rowtype;
  v_question jsonb;
  v_ans jsonb;
  v_resolved boolean;
  v_status text;
  v_reasons jsonb;
begin
  if v_uid is null then
    raise exception using errcode = 'PT401', message = 'AUTHENTICATION_REQUIRED';
  end if;

  if p_action is null then
    raise exception using errcode = 'PT400', message = 'INVALID_REQUEST';
  end if;

  -- ACTION: list_reasons (Safe public catalog info without answer keys)
  if p_action = 'list_reasons' then
    if p_subject_id is null or p_grade is null then
      raise exception using errcode = 'PT400', message = 'SUBJECT_AND_GRADE_REQUIRED';
    end if;

    select coalesce(jsonb_agg(
      jsonb_build_object(
        'reasonId', reason_id,
        'subjectId', subject_id,
        'grade', grade,
        'reasonText', reason_text,
        'verifiedHint', verified_hint,
        'counterexample', counterexample,
        'prop1Prompt', prop1_prompt,
        'prop2Prompt', prop2_prompt
      ) order by reason_id
    ), '[]'::jsonb) into v_reasons
    from public.mentor_misconception_catalog
    where subject_id = p_subject_id and grade = p_grade;

    return jsonb_build_object('items', v_reasons);
  end if;

  if p_id is null or length(trim(p_id)) = 0 then
    raise exception using errcode = 'PT400', message = 'SESSION_ID_REQUIRED';
  end if;

  if p_action = 'start' then
    -- PRIORITIZED SECURITY: Must link to a real AI practice session belonging to auth.uid()
    if p_session_id is null or p_question_id is null or p_reason_id is null then
      raise exception using errcode = 'PT400', message = 'SESSION_AND_REASON_REQUIRED';
    end if;

    select * into v_practice from public.mentor_ai_practice_sessions
    where id = p_session_id and user_id = v_uid;
    if v_practice.id is null then
      raise exception using errcode = 'PT404', message = 'SESSION_NOT_FOUND_OR_FORBIDDEN';
    end if;

    -- Check question belongs to session snapshot
    select item.value into v_question
    from jsonb_array_elements(v_practice.questions_snapshot) as item(value)
    where item.value->>'id' = p_question_id;

    if v_question is null then
      raise exception using errcode = 'PT404', message = 'QUESTION_NOT_IN_SESSION';
    end if;

    -- Check question was actually answered incorrectly, reject if skipped or not revealed or correct
    v_ans := v_practice.answers -> p_question_id;
    if v_ans is null
       or (v_ans ->> 'revealed') is distinct from 'true'
       or (v_ans ->> 'skipped') is distinct from 'false'
       or (v_ans ->> 'isCorrect') is distinct from 'false' then
      raise exception using errcode = 'PT409', message = 'QUESTION_NOT_ANSWERED_WRONGLY';
    end if;

    -- Load pre-verified catalog item. Must match subject_id and grade of practice session!
    select * into v_cat from public.mentor_misconception_catalog
    where reason_id = p_reason_id;
    if v_cat.reason_id is null then
      raise exception using errcode = 'PT404', message = 'UNVERIFIED_OR_UNKNOWN_REASON';
    end if;

    if v_cat.subject_id <> v_practice.subject_id or v_cat.grade <> v_practice.grade then
      raise exception using errcode = 'PT400', message = 'REASON_SUBJECT_OR_GRADE_MISMATCH';
    end if;

    -- Idempotent session recovery if already exists
    select * into v_rec from public.mentor_misconception_verifications
    where id = p_id and user_id = v_uid;

    if v_rec.id is not null then
      if v_rec.session_id is distinct from p_session_id
         or v_rec.question_id is distinct from p_question_id
         or v_rec.reason_id is distinct from p_reason_id then
        raise exception using errcode = 'PT409', message = 'MISCONCEPTION_SESSION_CONFLICT';
      end if;

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
    end if;

    insert into public.mentor_misconception_verifications (
      id, user_id, session_id, question_id, reason_id, subject_id, grade,
      question_prompt, selected_option, misconception_reason, reason_type,
      ai_hint, ai_counterexample,
      prop1_prompt, prop1_correct, prop2_prompt, prop2_correct,
      verification_status, created_at, updated_at
    ) values (
      p_id, v_uid, p_session_id, p_question_id, p_reason_id, v_practice.subject_id, v_practice.grade,
      v_question->>'prompt', v_ans->>'selectedOption', v_cat.reason_text, 'PRE_AUTHORED',
      v_cat.verified_hint, v_cat.counterexample,
      v_cat.prop1_prompt, v_cat.prop1_correct, v_cat.prop2_prompt, v_cat.prop2_correct,
      'NEEDS_REVIEW', now(), now()
    ) returning * into v_rec;

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
        'verifiedAt', v_rec.verified_at,
        'updatedAtEpochMs', (extract(epoch from v_rec.updated_at) * 1000)::bigint
      );
    end if;

    -- SERVER-SIDE EVALUATION: BOTH propositions must match their server-stored correct boolean
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
      'verifiedAt', v_rec.verified_at,
      'updatedAtEpochMs', (extract(epoch from v_rec.updated_at) * 1000)::bigint
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

  else
    raise exception using errcode = 'PT400', message = 'UNKNOWN_ACTION';
  end if;
end;
$$;

revoke all on function public.mentor_misconception_api(text, text, uuid, text, text, boolean, boolean, text, int) from public, anon;
grant execute on function public.mentor_misconception_api(text, text, uuid, text, text, boolean, boolean, text, int) to authenticated;

commit;
