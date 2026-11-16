create table if not exists public.mentor_exam_question_payloads (
  id text primary key,
  exam_id text not null references public.mentor_exams(id) on delete cascade,
  position integer not null check (position >= 1),
  payload jsonb not null check (jsonb_typeof(payload) = 'object'),
  sha256 text not null check (sha256 ~ '^[0-9a-f]{64}$'),
  updated_at timestamptz not null default now(),
  unique (exam_id, position)
);
create index if not exists mentor_exam_question_payloads_exam_position_idx
  on public.mentor_exam_question_payloads (exam_id, position);
alter table public.mentor_exam_question_payloads enable row level security;
revoke all on public.mentor_exam_question_payloads from public, anon, authenticated;

create table if not exists public.mentor_exam_assets (
  storage_path text primary key,
  kind text not null check (kind in ('IMAGE', 'SOURCE_PDF')),
  sha256 text not null check (sha256 ~ '^[0-9a-f]{64}$'),
  byte_size bigint not null check (byte_size > 0),
  bucket_id text not null default 'mentor-exam-assets',
  updated_at timestamptz not null default now()
);
alter table public.mentor_exam_assets enable row level security;
revoke all on public.mentor_exam_assets from public, anon, authenticated;;
