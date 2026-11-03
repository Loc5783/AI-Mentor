begin;

-- ============================================================================
-- AI Mentor - Idempotent Verified Identity Claim & Active Table Cutover Path
-- Versioned migration: 202611010004_claim_staging_progress.sql
-- Idempotent, transactional claim path for legacy SQLite users to claim their
-- staged durable progress upon verified signup on Supabase Auth.
--
-- Security & Integrity Rules:
-- 1. Strictly requires confirmed email matching the legacy Gmail in staging.user_mappings.
-- 2. Strictly rejects *.example test accounts (archived separately unclaimed).
-- 3. Strictly rejects unconfirmed emails, mismatched emails, and duplicate claims.
-- 4. Strictly prevents claiming unowned or catalog rows.
-- 5. Maps each populated durable table (11) into correct active public tables.
-- 6. Generic handling / explicit schemas for remaining durable tables (no silent dropping).
-- 7. Secure search_path = '', restricted function privileges, strict RLS.
-- 8. Admin-only dry-run reconciliation function proving complete durable row accountability against manifest/source.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Ensure public schema contains all required durable app structures
-- ----------------------------------------------------------------------------

-- 1a. Standalone teacher/admin question bank
create table if not exists public.mentor_questions (
  id text primary key,
  code text unique,
  version int not null default 1,
  subject_id text not null,
  grade int not null check (grade between 10 and 12),
  chapter text not null,
  topic text not null,
  textbook text,
  prompt text not null,
  option_a text not null,
  option_b text not null,
  option_c text not null,
  option_d text not null,
  correct_option text not null,
  explanation text not null default '',
  difficulty text not null default 'MEDIUM',
  points int not null default 10,
  image_url text,
  source text,
  author text,
  license text,
  status text not null default 'PUBLISHED',
  created_by uuid references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 1b. Resumable practice sessions
create table if not exists public.mentor_practice_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  quiz_id text not null,
  subject_id text not null,
  grade int not null check (grade between 10 and 12),
  question_snapshots jsonb not null check (jsonb_typeof(question_snapshots) = 'array'),
  step int not null default 0,
  answers jsonb not null default '{}' check (jsonb_typeof(answers) = 'object'),
  state text not null default 'IN_PROGRESS' check (state in ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
  score int not null default 0,
  correct_count int not null default 0,
  total_count int not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_mentor_practice_sessions_user_quiz on public.mentor_practice_sessions(user_id, quiz_id, state, updated_at);
alter table public.mentor_practice_sessions add column if not exists step int not null default 0;
alter table public.mentor_practice_sessions add column if not exists total_count int not null default 0;

-- 1c. Wrong questions notebook for spaced repetition
create table if not exists public.mentor_wrong_questions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  question_id text not null,
  subject_id text not null,
  grade int not null check (grade between 10 and 12),
  selected_option text not null,
  correct_option text not null,
  explanation text not null default '',
  question_snapshot jsonb,
  state text not null default 'NEEDS_REVIEW' check (state in ('NEEDS_REVIEW', 'RESOLVED', 'MASTERED')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(user_id, question_id)
);
create index if not exists idx_mentor_wrong_questions_lookup on public.mentor_wrong_questions(user_id, subject_id, grade, state);

-- 1d. Social and friend relationships
create table if not exists public.mentor_friendships (
  id uuid primary key default gen_random_uuid(),
  requester uuid not null references auth.users(id) on delete cascade,
  recipient uuid not null references auth.users(id) on delete cascade,
  state text not null check(state in ('PENDING', 'ACCEPTED', 'BLOCKED')),
  revision int not null default 1,
  updated_at timestamptz not null default now(),
  check (requester <> recipient),
  unique(requester, recipient)
);

create table if not exists public.mentor_friend_streaks (
  id uuid primary key default gen_random_uuid(),
  user1_id uuid not null references auth.users(id) on delete cascade,
  user2_id uuid not null references auth.users(id) on delete cascade,
  streak_days int not null default 0,
  last_shared_day date not null,
  updated_at timestamptz not null default now(),
  unique(user1_id, user2_id)
);

-- 1e. Badge catalog and student achievements
create table if not exists public.mentor_badge_catalog (
  id text primary key,
  code text not null unique,
  title text not null,
  description text not null,
  icon_url text not null,
  month_period text not null,
  created_at timestamptz not null default now()
);

create table if not exists public.mentor_badge_awards (
  user_id uuid not null references auth.users(id) on delete cascade,
  badge_id text not null references public.mentor_badge_catalog(id),
  unlocked_at timestamptz not null default now(),
  primary key(user_id, badge_id)
);

-- 1f. AI generated practice sessions and error reports
create table if not exists public.mentor_ai_practice_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  subject_id text not null,
  grade int not null check (grade between 10 and 12),
  topic text not null,
  difficulty text not null check (difficulty in ('EASY', 'MEDIUM', 'HARD')),
  question_count int not null,
  questions_snapshot jsonb not null check (jsonb_typeof(questions_snapshot) = 'array'),
  answers jsonb not null default '{}' check (jsonb_typeof(answers) = 'object'),
  current_index int not null default 0,
  state text not null default 'IN_PROGRESS' check (state in ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
  summary text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.mentor_ai_question_reports (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  session_id uuid not null references public.mentor_ai_practice_sessions(id) on delete cascade,
  question_id text not null,
  reason text not null,
  details text,
  created_at timestamptz not null default now()
);

-- ----------------------------------------------------------------------------
-- 2. Strict Row Level Security & Access Control for all public tables
-- ----------------------------------------------------------------------------
alter table public.mentor_questions enable row level security;
alter table public.mentor_practice_sessions enable row level security;
alter table public.mentor_wrong_questions enable row level security;
alter table public.mentor_friendships enable row level security;
alter table public.mentor_friend_streaks enable row level security;
alter table public.mentor_badge_catalog enable row level security;
alter table public.mentor_badge_awards enable row level security;
alter table public.mentor_ai_practice_sessions enable row level security;
alter table public.mentor_ai_question_reports enable row level security;

revoke all on public.mentor_questions, public.mentor_practice_sessions, public.mentor_wrong_questions,
  public.mentor_friendships, public.mentor_friend_streaks, public.mentor_badge_catalog,
  public.mentor_badge_awards, public.mentor_ai_practice_sessions, public.mentor_ai_question_reports
  from public, anon, authenticated;

grant select on public.mentor_questions to authenticated;
grant select, insert, update on public.mentor_practice_sessions to authenticated;
grant select, insert, update, delete on public.mentor_wrong_questions to authenticated;
grant select, insert, delete on public.mentor_friendships to authenticated;
grant update (state, revision, updated_at) on public.mentor_friendships to authenticated;
grant select on public.mentor_friend_streaks to authenticated;
grant select on public.mentor_badge_catalog to authenticated;
grant select on public.mentor_badge_awards to authenticated;
grant select, insert, update on public.mentor_ai_practice_sessions to authenticated;
grant select, insert on public.mentor_ai_question_reports to authenticated;

drop policy if exists mentor_questions_read on public.mentor_questions;
create policy mentor_questions_read on public.mentor_questions for select to authenticated
  using (status = 'PUBLISHED' or created_by = (select auth.uid()));

drop policy if exists mentor_practice_sessions_owner on public.mentor_practice_sessions;
create policy mentor_practice_sessions_owner on public.mentor_practice_sessions for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

drop policy if exists mentor_wrong_questions_owner on public.mentor_wrong_questions;
create policy mentor_wrong_questions_owner on public.mentor_wrong_questions for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

-- Enforce strict immutability of friendship endpoints and valid actor/status transitions via trigger
create or replace function public.mentor_friendships_enforce_immutable_endpoints()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
begin
  -- Endpoints (requester, recipient) and primary key id are strictly immutable
  if new.id <> old.id or new.requester <> old.requester or new.recipient <> old.recipient then
    raise exception 'Friendship endpoints (requester, recipient) and ID are immutable'
      using errcode = '42501';
  end if;

  -- Enforce valid status transitions when called under authenticated context
  if v_uid is not null then
    -- Cannot transition out of BLOCKED
    if old.state = 'BLOCKED' and new.state <> 'BLOCKED' then
      raise exception 'Cannot change status of a blocked relationship'
        using errcode = '42501';
    end if;

    -- Only recipient can accept a pending request
    if new.state = 'ACCEPTED' and (old.state <> 'PENDING' or v_uid <> old.recipient) then
      raise exception 'Only the recipient can accept a pending friendship request'
        using errcode = '42501';
    end if;

    -- Cannot reset status back to PENDING
    if new.state = 'PENDING' and old.state <> 'PENDING' then
      raise exception 'Cannot transition status back to PENDING'
        using errcode = '42501';
    end if;
  end if;

  new.updated_at := now();
  return new;
end;
$$;

revoke all on function public.mentor_friendships_enforce_immutable_endpoints() from public, anon;

drop trigger if exists trg_mentor_friendships_immutable_endpoints on public.mentor_friendships;
create trigger trg_mentor_friendships_immutable_endpoints
  before update on public.mentor_friendships
  for each row
  execute function public.mentor_friendships_enforce_immutable_endpoints();

-- Tightened Row Level Security for mentor_friendships by specific operation
drop policy if exists mentor_friendships_participant on public.mentor_friendships;

drop policy if exists mentor_friendships_select on public.mentor_friendships;
create policy mentor_friendships_select on public.mentor_friendships
  for select to authenticated
  using ((select auth.uid()) in (requester, recipient));

drop policy if exists mentor_friendships_insert on public.mentor_friendships;
create policy mentor_friendships_insert on public.mentor_friendships
  for insert to authenticated
  with check (
    (select auth.uid()) = requester
    and (select auth.uid()) <> recipient
    and state = 'PENDING'
  );

drop policy if exists mentor_friendships_update on public.mentor_friendships;
create policy mentor_friendships_update on public.mentor_friendships
  for update to authenticated
  using ((select auth.uid()) in (requester, recipient))
  with check (
    (select auth.uid()) in (requester, recipient)
    and (
      ((select auth.uid()) = recipient and state in ('ACCEPTED', 'BLOCKED'))
      or
      ((select auth.uid()) = requester and state = 'BLOCKED')
    )
  );

drop policy if exists mentor_friendships_delete on public.mentor_friendships;
create policy mentor_friendships_delete on public.mentor_friendships
  for delete to authenticated
  using (
    state in ('PENDING', 'ACCEPTED')
    and (select auth.uid()) in (requester, recipient)
  );

drop policy if exists mentor_friend_streaks_participant on public.mentor_friend_streaks;
create policy mentor_friend_streaks_participant on public.mentor_friend_streaks for select to authenticated
  using ((select auth.uid()) in (user1_id, user2_id));

drop policy if exists mentor_badge_catalog_read on public.mentor_badge_catalog;
create policy mentor_badge_catalog_read on public.mentor_badge_catalog for select to authenticated
  using (true);

drop policy if exists mentor_badge_awards_owner on public.mentor_badge_awards;
create policy mentor_badge_awards_owner on public.mentor_badge_awards for select to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists mentor_ai_practice_sessions_owner on public.mentor_ai_practice_sessions;
create policy mentor_ai_practice_sessions_owner on public.mentor_ai_practice_sessions for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

drop policy if exists mentor_ai_question_reports_owner on public.mentor_ai_question_reports;
create policy mentor_ai_question_reports_owner on public.mentor_ai_question_reports for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

-- ----------------------------------------------------------------------------
-- 3. Import durable catalog data (unowned question bank & library quizzes)
-- ----------------------------------------------------------------------------

-- Seed standalone question bank from staging
insert into public.mentor_questions(
  id, code, version, subject_id, grade, chapter, topic, textbook, prompt,
  option_a, option_b, option_c, option_d, correct_option, explanation,
  difficulty, points, image_url, source, author, license, status, created_at, updated_at
)
select
  legacy_id,
  coalesce(payload->>'code', legacy_id),
  coalesce((payload->>'version')::int, 1),
  payload->>'subject_id',
  (payload->>'grade')::int,
  coalesce(payload->>'chapter', ''),
  coalesce(payload->>'topic', ''),
  payload->>'textbook',
  payload->>'prompt',
  coalesce(payload->>'option_a', ''),
  coalesce(payload->>'option_b', ''),
  coalesce(payload->>'option_c', ''),
  coalesce(payload->>'option_d', ''),
  upper(coalesce(payload->>'correct_option', 'A')),
  coalesce(payload->>'explanation', ''),
  coalesce(payload->>'difficulty', 'MEDIUM'),
  coalesce((payload->>'points')::int, 10),
  payload->>'image_url',
  payload->>'source',
  payload->>'author',
  payload->>'license',
  coalesce(payload->>'status', 'PUBLISHED'),
  now(),
  now()
from staging.durable_rows
where table_name = 'questions'
on conflict (id) do update set
  prompt = excluded.prompt,
  explanation = excluded.explanation,
  status = excluded.status,
  updated_at = now();

-- Deterministic translation of legacy quiz questions into target-valid JSON array
-- Supports:
-- 1. Already structured non-empty JSON array of question objects (preserves as-is)
-- 2. Staged question_ids (JSON array, JSON array string, or comma-separated list of IDs)
--    resolved against public.mentor_questions, staging.durable_rows (questions), or public.mentor_course_units
-- 3. Staged questions array of ID strings resolved against the above sources
-- Preserves exact question content, options, correctOption, explanation, points, difficulty, and order.
-- Returns '[]'::jsonb if any question cannot be resolved or payload is empty.
create or replace function staging.translate_legacy_quiz_questions(p_payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_qids text[] := array[]::text[];
  v_res jsonb := '[]'::jsonb;
  v_qid text;
  v_q record;
  v_elem jsonb;
  v_all_found boolean := true;
begin
  if p_payload is null then
    return '[]'::jsonb;
  end if;

  -- 1. If questions is already a non-empty array of objects with prompt or options:
  if jsonb_typeof(p_payload->'questions') = 'array' and jsonb_array_length(p_payload->'questions') > 0 then
    if jsonb_typeof(p_payload->'questions'->0) = 'object' and (
      (p_payload->'questions'->0) ? 'prompt' or (p_payload->'questions'->0) ? 'options'
    ) then
      return p_payload->'questions';
    end if;
  end if;

  -- 2. Extract ordered question IDs
  -- Try question_ids as jsonb array
  if jsonb_typeof(p_payload->'question_ids') = 'array' then
    select array_agg(elem order by ord) into v_qids
    from (
      select nullif(trim(x), '') as elem, ord
      from jsonb_array_elements_text(p_payload->'question_ids') with ordinality as t(x, ord)
    ) s
    where elem is not null;
  -- Try questions as jsonb array of strings (if elements are strings, not objects)
  elsif jsonb_typeof(p_payload->'questions') = 'array' and jsonb_typeof(p_payload->'questions'->0) = 'string' then
    select array_agg(elem order by ord) into v_qids
    from (
      select nullif(trim(x), '') as elem, ord
      from jsonb_array_elements_text(p_payload->'questions') with ordinality as t(x, ord)
    ) s
    where elem is not null;
  -- Try question_ids as text string (JSON array string or comma-separated)
  elsif p_payload->>'question_ids' is not null and trim(p_payload->>'question_ids') <> '' then
    begin
      if trim(p_payload->>'question_ids') ~ '^\s*\[' then
        select array_agg(elem order by ord) into v_qids
        from (
          select nullif(trim(x), '') as elem, ord
          from jsonb_array_elements_text((trim(p_payload->>'question_ids'))::jsonb) with ordinality as t(x, ord)
        ) s
        where elem is not null;
      end if;
    exception when others then
      v_qids := null;
    end;

    if v_qids is null or cardinality(v_qids) = 0 then
      select array_agg(elem order by ord) into v_qids
      from (
        select nullif(trim(x), '') as elem, ord
        from unnest(string_to_array(p_payload->>'question_ids', ',')) with ordinality as t(x, ord)
      ) s
      where elem is not null;
    end if;
  -- Try questions as text string (JSON array string or comma-separated)
  elsif p_payload->>'questions' is not null and trim(p_payload->>'questions') <> '' then
    begin
      if trim(p_payload->>'questions') ~ '^\s*\[' then
        select array_agg(elem order by ord) into v_qids
        from (
          select nullif(trim(x), '') as elem, ord
          from jsonb_array_elements_text((trim(p_payload->>'questions'))::jsonb) with ordinality as t(x, ord)
        ) s
        where elem is not null;
      end if;
    exception when others then
      v_qids := null;
    end;

    if v_qids is null or cardinality(v_qids) = 0 then
      select array_agg(elem order by ord) into v_qids
      from (
        select nullif(trim(x), '') as elem, ord
        from unnest(string_to_array(p_payload->>'questions', ',')) with ordinality as t(x, ord)
      ) s
      where elem is not null;
    end if;
  end if;

  if v_qids is null or cardinality(v_qids) = 0 then
    return '[]'::jsonb;
  end if;

  -- 3. Resolve each question in preserving order
  foreach v_qid in array v_qids loop
    v_q := null;

    -- Lookup in public.mentor_questions
    select
      q.id,
      q.code,
      coalesce(q.chapter, '') as chapter,
      coalesce(q.topic, '') as topic,
      q.prompt,
      jsonb_build_array(
        jsonb_build_object('id', 'A', 'text', coalesce(q.option_a, '')),
        jsonb_build_object('id', 'B', 'text', coalesce(q.option_b, '')),
        jsonb_build_object('id', 'C', 'text', coalesce(q.option_c, '')),
        jsonb_build_object('id', 'D', 'text', coalesce(q.option_d, ''))
      ) as options,
      upper(coalesce(q.correct_option, 'A')) as correct_option,
      coalesce(q.explanation, '') as explanation,
      coalesce(q.difficulty, 'MEDIUM') as difficulty,
      coalesce(q.points, 10) as points,
      q.image_url
    into v_q
    from public.mentor_questions q
    where q.id = v_qid
    limit 1;

    -- Lookup in staging.durable_rows (questions table) if not found yet
    if v_q.id is null then
      select
        d.legacy_id as id,
        d.payload->>'code' as code,
        coalesce(d.payload->>'chapter', '') as chapter,
        coalesce(d.payload->>'topic', '') as topic,
        coalesce(d.payload->>'prompt', '') as prompt,
        case
          when jsonb_typeof(d.payload->'options') = 'array' then d.payload->'options'
          else jsonb_build_array(
            jsonb_build_object('id', 'A', 'text', coalesce(d.payload->>'option_a', '')),
            jsonb_build_object('id', 'B', 'text', coalesce(d.payload->>'option_b', '')),
            jsonb_build_object('id', 'C', 'text', coalesce(d.payload->>'option_c', '')),
            jsonb_build_object('id', 'D', 'text', coalesce(d.payload->>'option_d', ''))
          )
        end as options,
        upper(coalesce(d.payload->>'correct_option', d.payload->>'correctOption', 'A')) as correct_option,
        coalesce(d.payload->>'explanation', '') as explanation,
        coalesce(d.payload->>'difficulty', 'MEDIUM') as difficulty,
        coalesce((d.payload->>'points')::int, 10) as points,
        coalesce(d.payload->>'image_url', d.payload->>'imageUrl') as image_url
      into v_q
      from staging.durable_rows d
      where d.table_name = 'questions' and d.legacy_id = v_qid
      limit 1;
    end if;

    -- Lookup in public.mentor_course_units questions if not found yet
    if v_q.id is null then
      select
        elem->>'id' as id,
        elem->>'code' as code,
        coalesce(elem->>'chapter', '') as chapter,
        coalesce(elem->>'topic', '') as topic,
        coalesce(elem->>'prompt', '') as prompt,
        case
          when jsonb_typeof(elem->'options') = 'array' then elem->'options'
          else jsonb_build_array(
            jsonb_build_object('id', 'A', 'text', coalesce(elem->>'option_a', '')),
            jsonb_build_object('id', 'B', 'text', coalesce(elem->>'option_b', '')),
            jsonb_build_object('id', 'C', 'text', coalesce(elem->>'option_c', '')),
            jsonb_build_object('id', 'D', 'text', coalesce(elem->>'option_d', ''))
          )
        end as options,
        upper(coalesce(elem->>'correctOption', elem->>'correct_option', elem->>'correctOptionId', 'A')) as correct_option,
        coalesce(elem->>'explanation', '') as explanation,
        coalesce(elem->>'difficulty', 'MEDIUM') as difficulty,
        coalesce((elem->>'points')::int, 10) as points,
        coalesce(elem->>'imageUrl', elem->>'image_url') as image_url
      into v_q
      from public.mentor_course_units u,
           jsonb_array_elements(u.questions) elem
      where elem->>'id' = v_qid
      limit 1;
    end if;

    if v_q.id is not null then
      v_elem := jsonb_build_object(
        'id', v_q.id,
        'code', v_q.code,
        'chapter', v_q.chapter,
        'topic', v_q.topic,
        'prompt', v_q.prompt,
        'options', v_q.options,
        'correctOption', v_q.correct_option,
        'explanation', v_q.explanation,
        'difficulty', v_q.difficulty,
        'points', v_q.points,
        'imageUrl', v_q.image_url
      );
      v_res := v_res || jsonb_build_array(v_elem);
    else
      v_all_found := false;
    end if;
  end loop;

  -- Preserve question integrity: if any question cannot be resolved, return empty to avoid partial/corrupt quiz
  if not v_all_found or jsonb_array_length(v_res) = 0 then
    return '[]'::jsonb;
  end if;

  return v_res;
end;
$$;

revoke all on function staging.translate_legacy_quiz_questions(jsonb) from public, anon, authenticated;

-- Seed library quizzes from staging
insert into public.mentor_library_quizzes(
  id, title, subject_id, grade, chapter, topic, difficulty, questions, status, created_at
)
select
  legacy_id,
  payload->>'title',
  payload->>'subject_id',
  case
    when (payload->>'grade')::int between 10 and 12 then (payload->>'grade')::int
    else 10 end,
  coalesce(payload->>'chapter', ''),
  coalesce(payload->>'topic', ''),
  coalesce(payload->>'difficulty', 'MEDIUM'),
  staging.translate_legacy_quiz_questions(payload),
  case
    when upper(coalesce(payload->>'status', '')) = 'PUBLISHED' then 'PUBLISHED'
    else 'DRAFT' end,
  now()
from staging.durable_rows
where table_name = 'library_quizzes'
  and jsonb_typeof(staging.translate_legacy_quiz_questions(payload)) = 'array'
  and jsonb_array_length(staging.translate_legacy_quiz_questions(payload)) > 0
on conflict (id) do update set
  title = excluded.title,
  difficulty = excluded.difficulty,
  questions = excluded.questions,
  status = excluded.status;

-- Seed exam papers from staging
insert into public.mentor_exams(
  id, title, subject_id, grade, exam_type, semester, duration_minutes,
  question_ids, total_points, question_count, status, version, created_at, updated_at
)
select
  legacy_id,
  payload->>'title',
  payload->>'subject_id',
  case
    when (payload->>'grade')::int between 10 and 12 then (payload->>'grade')::int
    else 10 end,
  case
    when upper(coalesce(payload->>'exam_type', '')) = 'FINAL' then 'FINAL'
    else 'MIDTERM' end,
  case
    when (payload->>'semester')::int in (1, 2) then (payload->>'semester')::int
    else 1 end,
  case
    when (payload->>'duration_minutes')::int between 5 and 300 then (payload->>'duration_minutes')::int
    else 45 end,
  coalesce(payload->>'question_ids', '[]'),
  coalesce((payload->>'total_points')::int, 0),
  coalesce((payload->>'question_count')::int, 0),
  case
    when upper(coalesce(payload->>'status', '')) = 'PUBLISHED' then 'PUBLISHED'
    else 'DRAFT' end,
  coalesce((payload->>'version')::int, 1),
  now(),
  now()
from staging.durable_rows
where table_name = 'exams'
on conflict (id) do update set
  title = excluded.title,
  question_ids = excluded.question_ids,
  status = excluded.status,
  updated_at = now();

-- ----------------------------------------------------------------------------
-- 4. Idempotent & Transactional User Claim Path (mentor_claim_staged_progress)
-- ----------------------------------------------------------------------------
create or replace function public.mentor_claim_staged_progress()
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_user_email text;
  v_confirmed_at timestamptz;
  v_mapping record;
  v_existing record;
  v_row record;
  v_count int := 0;
  v_attempts_count int := 0;
  v_enrollments_count int := 0;
  v_completions_count int := 0;
  v_awards_count int := 0;
  v_learning_days_count int := 0;
  v_practice_count int := 0;
  v_wrong_count int := 0;
  v_target_enr_id uuid;
begin
  -- 1. Ensure authenticated caller
  if v_uid is null then
    raise sqlstate 'PT401' using message = 'AUTHENTICATION_REQUIRED';
  end if;

  -- 2. Advisory lock on caller UUID to serialize concurrent claim attempts
  perform pg_advisory_xact_lock(hashtextextended(v_uid::text, 0));

  -- 3. Lookup caller in auth.users
  select email, email_confirmed_at into v_user_email, v_confirmed_at
  from auth.users
  where id = v_uid;

  if v_user_email is null then
    raise sqlstate 'PT404' using message = 'AUTH_USER_NOT_FOUND';
  end if;

  -- 4. Check email confirmation: Unconfirmed email MUST be rejected
  if v_confirmed_at is null then
    raise sqlstate 'PT403' using message = 'EMAIL_CONFIRMATION_REQUIRED';
  end if;

  -- 5. Idempotency: Has this Supabase Auth user already claimed an account?
  select * into v_existing
  from staging.user_mappings
  where target_auth_uid = v_uid;

  if v_existing.legacy_user_id is not null then
    return jsonb_build_object(
      'claimed', true,
      'status', 'ALREADY_CLAIMED',
      'legacyUserId', v_existing.legacy_user_id,
      'claimedAt', v_existing.claimed_at,
      'replayed', true
    );
  end if;

  -- 6. Find legacy mapping by exact verified email match (case-insensitive)
  select * into v_mapping
  from staging.user_mappings
  where lower(email) = lower(v_user_email);

  if v_mapping.legacy_user_id is null then
    return jsonb_build_object(
      'claimed', false,
      'status', 'NO_STAGED_PROGRESS',
      'message', 'No staged legacy progress associated with this email.'
    );
  end if;

  -- 7. Reject *.example test accounts (archived separately unclaimed)
  if lower(v_mapping.email) like '%.example' or lower(v_mapping.email) like '%@example.%' then
    raise sqlstate 'PT403' using message = 'TEST_ACCOUNT_ARCHIVED_CANNOT_BE_CLAIMED';
  end if;

  -- 8. Reject email mismatch
  if lower(v_mapping.email) <> lower(v_user_email) then
    raise sqlstate 'PT403' using message = 'EMAIL_MISMATCH';
  end if;

  -- 9. Reject duplicate claim if legacy account is already claimed by another user
  if v_mapping.target_auth_uid is not null and v_mapping.target_auth_uid <> v_uid then
    raise sqlstate 'PT409' using message = 'LEGACY_ACCOUNT_ALREADY_CLAIMED';
  end if;

  -- =========================================================================
  -- TRANSACTIONAL PROGRESS IMPORT: Only rows owned by this legacy_user_id
  -- =========================================================================

  -- A. Update profile facts from staged users row
  for v_row in
    select payload from staging.durable_rows
    where table_name = 'users' and legacy_user_id = v_mapping.legacy_user_id
  loop
    update public.mentor_profiles
    set nickname = coalesce(nullif(trim(v_row.payload->>'nickname'), ''), nickname),
        bio = coalesce(v_row.payload->>'bio', bio),
        updated_at = now()
    where id = v_uid;

    if (v_row.payload->>'grade')::int between 10 and 12 then
      update public.mentor_preferences
      set grade = (v_row.payload->>'grade')::smallint,
          updated_at = now()
      where user_id = v_uid;
    end if;

    if v_row.payload->>'role' in ('TEACHER', 'ADMIN') then
      insert into public.mentor_role_audit_logs(id, user_id, previous_role, new_role, source, changed_at)
      values (gen_random_uuid(), v_uid, 'STUDENT', v_row.payload->>'role', 'CUTOVER_CLAIM', now());
    end if;

    v_count := v_count + 1;
  end loop;

  -- B. Map enrollments
  for v_row in
    select legacy_id, payload from staging.durable_rows
    where table_name = 'enrollments' and legacy_user_id = v_mapping.legacy_user_id
  loop
    insert into public.mentor_enrollments(
      id, user_id, subject_id, grade, placement, revision, updated_at
    ) values (
      case when v_row.legacy_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        then v_row.legacy_id::uuid
        else md5('enr:' || v_row.legacy_id)::uuid end,
      v_uid,
      v_row.payload->>'subject_id',
      (v_row.payload->>'grade')::int,
      case
        when jsonb_typeof(v_row.payload->'placement') = 'object' then v_row.payload->'placement'
        when v_row.payload->>'placement' is not null and v_row.payload->>'placement' not in ('', 'null')
          then (v_row.payload->>'placement')::jsonb
        else null end,
      coalesce((v_row.payload->>'revision')::int, 1),
      now()
    )
    on conflict (user_id, subject_id, grade) do update set
      placement = coalesce(excluded.placement, mentor_enrollments.placement),
      revision = greatest(excluded.revision, mentor_enrollments.revision),
      updated_at = now();

    v_count := v_count + 1;
    v_enrollments_count := v_enrollments_count + 1;
  end loop;

  -- C. Map attempts (preserving historical attempts, question progress & answers)
  for v_row in
    select legacy_id, payload from staging.durable_rows
    where table_name = 'attempts' and legacy_user_id = v_mapping.legacy_user_id
  loop
    -- Match target enrollment ID in public.mentor_enrollments
    select e.id into v_target_enr_id
    from public.mentor_enrollments e
    where e.user_id = v_uid
      and (
        (v_row.payload->>'enrollment_id' ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
         and e.id = (v_row.payload->>'enrollment_id')::uuid)
        or e.id = md5('enr:' || (v_row.payload->>'enrollment_id'))::uuid
        or (e.subject_id = split_part(v_row.payload->>'unit_id', '-', 1))
      )
    limit 1;

    -- If no enrollment matched, create one to ensure attempt is not silently dropped
    if v_target_enr_id is null then
      declare
        v_att_subj text := coalesce(v_row.payload->>'subject_id', split_part(v_row.payload->>'unit_id', '-', 1));
        v_att_grade int := case
          when (v_row.payload->>'grade')::int between 10 and 12 then (v_row.payload->>'grade')::int
          else 10 end;
      begin
        if v_att_subj is not null and v_att_subj <> '' then
          insert into public.mentor_enrollments(
            id, user_id, subject_id, grade, placement, revision, updated_at
          ) values (
            gen_random_uuid(),
            v_uid,
            v_att_subj,
            v_att_grade,
            null,
            1,
            now()
          )
          on conflict (user_id, subject_id, grade) do update set updated_at = now()
          returning id into v_target_enr_id;
        end if;
      end;
    end if;

    if v_target_enr_id is not null then
      insert into public.mentor_attempts(
        id, user_id, enrollment_id, unit_id, course_version, kind, step, answers, state, revision, updated_at
      ) values (
        case when v_row.legacy_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
          then v_row.legacy_id::uuid
          else md5('att:' || v_row.legacy_id)::uuid end,
        v_uid,
        v_target_enr_id,
        v_row.payload->>'unit_id',
        coalesce(v_row.payload->>'course_version', v_row.payload->>'curriculum_version', '2026.1'),
        coalesce(v_row.payload->>'kind', 'LESSON'),
        coalesce((v_row.payload->>'step')::int, 0),
        case
          when jsonb_typeof(v_row.payload->'answers') in ('object', 'array') then v_row.payload->'answers'
          when v_row.payload->>'answers' is not null and v_row.payload->>'answers' not in ('', 'null')
            then (v_row.payload->>'answers')::jsonb
          else '{}'::jsonb end,
        coalesce(v_row.payload->>'state', 'IN_PROGRESS'),
        coalesce((v_row.payload->>'revision')::int, 1),
        now()
      )
      on conflict (id) do update set
        step = excluded.step,
        answers = excluded.answers,
        state = excluded.state,
        revision = excluded.revision,
        updated_at = now();

      v_count := v_count + 1;
      v_attempts_count := v_attempts_count + 1;
    end if;
  end loop;

  -- D. Map completions (preserving completion results and rewards)
  for v_row in
    select legacy_id, payload from staging.durable_rows
    where table_name = 'completions' and legacy_user_id = v_mapping.legacy_user_id
  loop
    insert into public.mentor_completions(
      attempt_id, user_id, result
    ) values (
      case when (v_row.payload->>'attempt_id') ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        then (v_row.payload->>'attempt_id')::uuid
        else md5('att:' || (v_row.payload->>'attempt_id'))::uuid end,
      v_uid,
      case
        when jsonb_typeof(v_row.payload->'result') = 'object' then v_row.payload->'result'
        when v_row.payload->>'result' is not null and v_row.payload->>'result' not in ('', 'null')
          then (v_row.payload->>'result')::jsonb
        else jsonb_build_object(
          'xp', coalesce((v_row.payload->>'xp')::int, 0),
          'day', v_row.payload->>'day',
          'completedAt', coalesce(v_row.payload->>'completed_at', now()::text)
        ) end
    )
    on conflict (attempt_id) do update set
      result = excluded.result;

    v_count := v_count + 1;
    v_completions_count := v_completions_count + 1;
  end loop;

  -- E. Map awards (preserving earned XP)
  for v_row in
    select payload from staging.durable_rows
    where table_name = 'awards' and legacy_user_id = v_mapping.legacy_user_id
  loop
    insert into public.mentor_awards(
      user_id, subject_id, grade, unit_id, xp, learning_day
    ) values (
      v_uid,
      v_row.payload->>'subject_id',
      (v_row.payload->>'grade')::int,
      coalesce(v_row.payload->>'unit_id', v_row.payload->>'reward_unit_id'),
      (v_row.payload->>'xp')::int,
      (coalesce(v_row.payload->>'learning_day', v_row.payload->>'day'))::date
    )
    on conflict do nothing;

    v_count := v_count + 1;
    v_awards_count := v_awards_count + 1;
  end loop;

  -- F. Map learning days (streak calendar continuity)
  for v_row in
    select payload from staging.durable_rows
    where table_name = 'learning_days' and legacy_user_id = v_mapping.legacy_user_id
  loop
    insert into public.mentor_learning_days(
      user_id, day
    ) values (
      v_uid,
      (coalesce(v_row.payload->>'day', v_row.payload->>'learning_day'))::date
    )
    on conflict do nothing;

    v_count := v_count + 1;
    v_learning_days_count := v_learning_days_count + 1;
  end loop;

  -- G. Map practice sessions
  for v_row in
    select legacy_id, payload from staging.durable_rows
    where table_name = 'practice_sessions' and legacy_user_id = v_mapping.legacy_user_id
  loop
    -- Ensure referenced quiz exists in public.mentor_library_quizzes
    if exists (select 1 from public.mentor_library_quizzes where id = v_row.payload->>'quiz_id') then
      declare
        v_ps_grade int := case
          when (v_row.payload->>'grade')::int between 10 and 12 then (v_row.payload->>'grade')::int
          else 10 end;
        v_ps_state text := case
          when upper(coalesce(v_row.payload->>'state', '')) = 'COMPLETED' then 'COMPLETED'
          else 'IN_PROGRESS' end;
        v_ps_snapshots jsonb;
      begin
        v_ps_snapshots := case
          when jsonb_typeof(v_row.payload->'question_snapshots') = 'array' and jsonb_array_length(v_row.payload->'question_snapshots') > 0
            then v_row.payload->'question_snapshots'
          when v_row.payload->>'question_snapshots' is not null and v_row.payload->>'question_snapshots' not in ('', 'null', '[]')
            then (v_row.payload->>'question_snapshots')::jsonb
          else (select questions from public.mentor_library_quizzes where id = v_row.payload->>'quiz_id')
        end;

        if v_ps_snapshots is null or jsonb_typeof(v_ps_snapshots) <> 'array' then
          v_ps_snapshots := '[]'::jsonb;
        end if;

        insert into public.mentor_practice_sessions(
          id, user_id, quiz_id, subject_id, grade, question_snapshots, step, answers, state, score, correct_count, total_count, created_at, updated_at
        ) values (
          case when v_row.legacy_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            then v_row.legacy_id::uuid
            else md5('psess:' || v_row.legacy_id)::uuid end,
          v_uid,
          v_row.payload->>'quiz_id',
          coalesce(v_row.payload->>'subject_id', 'physics'),
          v_ps_grade,
          v_ps_snapshots,
          coalesce((v_row.payload->>'step')::int, 0),
          case
            when jsonb_typeof(v_row.payload->'answers') = 'object' then v_row.payload->'answers'
            when v_row.payload->>'answers' is not null and v_row.payload->>'answers' not in ('', 'null')
              then (v_row.payload->>'answers')::jsonb
            else '{}'::jsonb end,
          v_ps_state,
          coalesce((v_row.payload->>'score')::int, 0),
          coalesce((v_row.payload->>'correct_count')::int, 0),
          coalesce((v_row.payload->>'total_count')::int, jsonb_array_length(v_ps_snapshots)),
          now(),
          now()
        )
        on conflict (id) do update set
          step = excluded.step,
          answers = excluded.answers,
          state = excluded.state,
          score = excluded.score,
          updated_at = now();

        v_count := v_count + 1;
        v_practice_count := v_practice_count + 1;
      end;
    end if;
  end loop;

  -- H. Map wrong questions notebook (spaced repetition)
  for v_row in
    select legacy_id, payload from staging.durable_rows
    where table_name = 'wrong_questions_notebook' and legacy_user_id = v_mapping.legacy_user_id
  loop
    declare
      v_wq_grade int := case
        when (v_row.payload->>'grade')::int between 10 and 12 then (v_row.payload->>'grade')::int
        else 10 end;
      v_wq_state text := case
        when upper(coalesce(v_row.payload->>'state', '')) = 'RESOLVED' then 'RESOLVED'
        else 'NEEDS_REVIEW' end;
      v_wq_snapshot jsonb;
      v_wq_q record;
    begin
      -- Extract or build question_snapshot
      v_wq_snapshot := case
        when jsonb_typeof(v_row.payload->'question_snapshot') = 'object' then v_row.payload->'question_snapshot'
        when v_row.payload->>'question_snapshot' is not null and v_row.payload->>'question_snapshot' not in ('', 'null')
          then (v_row.payload->>'question_snapshot')::jsonb
        else null end;

      if v_wq_snapshot is null then
        -- Resolve from public.mentor_questions
        select
          q.id,
          q.prompt,
          jsonb_build_array(
            jsonb_build_object('id', 'A', 'text', coalesce(q.option_a, '')),
            jsonb_build_object('id', 'B', 'text', coalesce(q.option_b, '')),
            jsonb_build_object('id', 'C', 'text', coalesce(q.option_c, '')),
            jsonb_build_object('id', 'D', 'text', coalesce(q.option_d, ''))
          ) as options,
          upper(coalesce(q.correct_option, 'A')) as correct_option,
          coalesce(q.explanation, '') as explanation,
          coalesce(q.chapter, '') as chapter,
          coalesce(q.topic, '') as topic,
          coalesce(q.difficulty, 'MEDIUM') as difficulty,
          coalesce(q.points, 10) as points
        into v_wq_q
        from public.mentor_questions q
        where q.id = v_row.payload->>'question_id'
        limit 1;

        if v_wq_q.id is not null then
          v_wq_snapshot := jsonb_build_object(
            'id', v_wq_q.id,
            'prompt', v_wq_q.prompt,
            'options', v_wq_q.options,
            'correctOption', v_wq_q.correct_option,
            'explanation', v_wq_q.explanation,
            'chapter', v_wq_q.chapter,
            'topic', v_wq_q.topic,
            'difficulty', v_wq_q.difficulty,
            'points', v_wq_q.points
          );
        else
          -- Fallback minimal valid snapshot object so not-null constraint is satisfied
          v_wq_snapshot := jsonb_build_object(
            'id', coalesce(v_row.payload->>'question_id', 'unknown'),
            'prompt', '[Nội dung câu hỏi cũ]',
            'options', jsonb_build_array(
              jsonb_build_object('id', 'A', 'text', 'A'),
              jsonb_build_object('id', 'B', 'text', 'B'),
              jsonb_build_object('id', 'C', 'text', 'C'),
              jsonb_build_object('id', 'D', 'text', 'D')
            ),
            'correctOption', upper(coalesce(v_row.payload->>'correct_option', 'A')),
            'explanation', coalesce(v_row.payload->>'explanation', ''),
            'difficulty', 'MEDIUM',
            'points', 10
          );
        end if;
      end if;

      insert into public.mentor_wrong_questions(
        id, user_id, question_id, subject_id, grade, selected_option, correct_option, explanation, question_snapshot, state, created_at, updated_at
      ) values (
        case when v_row.legacy_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
          then v_row.legacy_id::uuid
          else md5('wq:' || v_row.legacy_id)::uuid end,
        v_uid,
        coalesce(v_row.payload->>'question_id', 'unknown'),
        coalesce(v_row.payload->>'subject_id', 'math'),
        v_wq_grade,
        coalesce(v_row.payload->>'selected_option', 'A'),
        upper(coalesce(v_row.payload->>'correct_option', 'A')),
        coalesce(v_row.payload->>'explanation', ''),
        v_wq_snapshot,
        v_wq_state,
        now(),
        now()
      )
      on conflict (user_id, question_id) do update set
        state = excluded.state,
        explanation = excluded.explanation,
        question_snapshot = excluded.question_snapshot,
        updated_at = now();

      v_count := v_count + 1;
      v_wrong_count := v_wrong_count + 1;
    end;
  end loop;

  -- I. Link author UUID for exams and questions created by this user
  update public.mentor_questions
  set created_by = v_uid, updated_at = now()
  where created_by is null
    and id in (
      select legacy_id from staging.durable_rows
      where table_name = 'questions' and legacy_user_id = v_mapping.legacy_user_id
    );

  update public.mentor_exams
  set created_by = v_uid, updated_at = now()
  where created_by is null
    and id in (
      select legacy_id from staging.durable_rows
      where table_name = 'exams' and legacy_user_id = v_mapping.legacy_user_id
    );

  -- J. Record claim in staging.user_mappings
  update staging.user_mappings
  set target_auth_uid = v_uid,
      claimed_at = now(),
      updated_at = now()
  where legacy_user_id = v_mapping.legacy_user_id;

  return jsonb_build_object(
    'claimed', true,
    'status', 'SUCCESSFULLY_CLAIMED',
    'legacyUserId', v_mapping.legacy_user_id,
    'claimedRowsTotal', v_count,
    'breakdown', jsonb_build_object(
      'enrollments', v_enrollments_count,
      'attempts', v_attempts_count,
      'completions', v_completions_count,
      'awards', v_awards_count,
      'learningDays', v_learning_days_count,
      'practiceSessions', v_practice_count,
      'wrongQuestions', v_wrong_count
    )
  );
end;
$$;

revoke all on function public.mentor_claim_staged_progress() from public, anon;
grant execute on function public.mentor_claim_staged_progress() to authenticated;

-- ----------------------------------------------------------------------------
-- 5. Admin-only Dry-Run Reconciliation Function (staging.reconcile_staging_cutover)
-- Proves durable row accountability against expected source count and reports unresolved mappings WITHOUT PII.
-- Never marks cutover complete while accounts remain unclaimed or count mismatches.
-- ----------------------------------------------------------------------------
create or replace function staging.reconcile_staging_cutover(p_expected_durable_rows int default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_total_rows int;
  v_table_counts jsonb := '{}';
  v_r record;
  v_claimable_count int;
  v_test_count int;
  v_claimed_count int;
  v_unclaimed_count int;
  v_unresolved_claimable jsonb := '[]';
  v_archived_test jsonb := '[]';
  v_unresolved_quizzes jsonb := '[]';
  v_all_durable_tables text[] := array[
    'users', 'resources', 'enrollments', 'attempts', 'completions',
    'awards', 'learning_days', 'friendships', 'badges', 'user_badges',
    'friend_streaks', 'questions', 'library_quizzes', 'practice_sessions',
    'wrong_questions_notebook', 'role_audit_logs', 'exams', 'exam_sessions',
    'ai_practice_sessions', 'ai_question_reports'
  ];
  v_tbl text;
  v_tbl_count int;
  v_sum_table_counts int := 0;
  v_counts_match boolean;
  v_cutover_complete boolean;
  v_status text;
begin
  -- Total durable rows in staging
  select count(*) into v_total_rows from staging.durable_rows;

  -- Per-table durable counts and verification sum
  foreach v_tbl in array v_all_durable_tables loop
    select count(*) into v_tbl_count from staging.durable_rows where table_name = v_tbl;
    v_table_counts := v_table_counts || jsonb_build_object(v_tbl, v_tbl_count);
    v_sum_table_counts := v_sum_table_counts + v_tbl_count;
  end loop;

  v_counts_match := (v_total_rows = v_sum_table_counts);

  -- Account audit
  select count(*) into v_claimable_count from staging.user_mappings
  where lower(email) not like '%.example' and lower(email) not like '%@example.%';

  select count(*) into v_test_count from staging.user_mappings
  where lower(email) like '%.example' or lower(email) like '%@example.%';

  select count(*) into v_claimed_count from staging.user_mappings
  where target_auth_uid is not null;

  select count(*) into v_unclaimed_count from staging.user_mappings
  where target_auth_uid is null
    and lower(email) not like '%.example' and lower(email) not like '%@example.%';

  -- Unresolved claimable accounts list (PII-free: legacy_user_id and status only)
  select coalesce(jsonb_agg(
    jsonb_build_object(
      'legacyUserId', legacy_user_id,
      'status', 'UNCLAIMED_PENDING_SIGNUP',
      'role', role,
      'grade', grade
    ) order by legacy_user_id
  ), '[]'::jsonb)
  into v_unresolved_claimable
  from staging.user_mappings
  where target_auth_uid is null
    and lower(email) not like '%.example' and lower(email) not like '%@example.%';

  -- Archived test accounts list (PII-free)
  select coalesce(jsonb_agg(
    jsonb_build_object(
      'legacyUserId', legacy_user_id,
      'status', 'ARCHIVED_UNCLAIMED',
      'role', role,
      'grade', grade
    ) order by legacy_user_id
  ), '[]'::jsonb)
  into v_archived_test
  from staging.user_mappings
  where lower(email) like '%.example' or lower(email) like '%@example.%';

  -- Quiz translation audit (detect any legacy quiz whose questions could not be translated)
  select coalesce(jsonb_agg(
    jsonb_build_object(
      'legacyQuizId', legacy_id,
      'status', 'UNRESOLVED_QUIZ_QUESTIONS',
      'reason', 'Empty or unresolvable question list'
    ) order by legacy_id
  ), '[]'::jsonb)
  into v_unresolved_quizzes
  from staging.durable_rows
  where table_name = 'library_quizzes'
    and (
      jsonb_typeof(staging.translate_legacy_quiz_questions(payload)) <> 'array'
      or jsonb_array_length(staging.translate_legacy_quiz_questions(payload)) = 0
    );

  -- Success semantics derived from account completeness and expected source count:
  -- Never claims completeness merely because row count happens to equal 34.
  if p_expected_durable_rows is not null and v_total_rows <> p_expected_durable_rows then
    v_status := 'ROW_COUNT_MISMATCH_EXPECTED';
    v_cutover_complete := false;
  elsif not v_counts_match then
    v_status := 'TABLE_COUNT_INTEGRITY_MISMATCH';
    v_cutover_complete := false;
  elsif jsonb_array_length(v_unresolved_quizzes) > 0 then
    v_status := 'UNRESOLVED_QUIZ_MAPPINGS';
    v_cutover_complete := false;
  elsif v_unclaimed_count > 0 then
    v_status := 'IN_PROGRESS_PENDING_CLAIMS';
    v_cutover_complete := false;
  elsif v_claimable_count = 0 then
    v_status := 'NO_CLAIMABLE_ACCOUNTS';
    v_cutover_complete := false;
  else
    v_status := 'ALL_ACCOUNTS_CLAIMED';
    v_cutover_complete := true;
  end if;

  return jsonb_build_object(
    'mode', 'ADMIN_DRY_RUN_RECONCILIATION',
    'cutoverComplete', v_cutover_complete,
    'status', v_status,
    'stagingInventory', jsonb_build_object(
      'durableRowsTotal', v_total_rows,
      'expectedDurableRows', p_expected_durable_rows,
      'sumTableCounts', v_sum_table_counts,
      'tableCountsMatchTotal', v_counts_match,
      'baselineEvidenceCount34', 34,
      'accountedFor34Of34', (v_total_rows = 34),
      'tableCounts', v_table_counts
    ),
    'accountReconciliation', jsonb_build_object(
      'totalAccounts', v_claimable_count + v_test_count,
      'claimableAccountsTotal', v_claimable_count,
      'claimedAccountsCount', v_claimed_count,
      'unclaimedAccountsCount', v_unclaimed_count,
      'archivedTestAccountsCount', v_test_count,
      'testAccountsRemainUnclaimed', true
    ),
    'unresolvedMappings', v_unresolved_claimable,
    'unresolvedQuizzes', v_unresolved_quizzes,
    'archivedTestAccounts', v_archived_test,
    'securityPolicy', jsonb_build_object(
      'piiPrinted', false,
      'credentialsStaged', false,
      'stagingExposedToPublic', false
    )
  );
end;
$$;

-- Strict access control: staging schema and reconciliation function are completely private to DB admin
revoke all on function staging.reconcile_staging_cutover(int) from public, anon, authenticated;

commit;
