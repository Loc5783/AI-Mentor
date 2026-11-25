create table if not exists public.mentor_exam_provenance (
  exam_id text primary key references public.mentor_exams(id) on delete cascade,
  manifest_payload jsonb not null check (jsonb_typeof(manifest_payload) = 'object'),
  sha256 text not null check (sha256 ~ '^[0-9a-f]{64}$'),
  updated_at timestamptz not null default now()
);
alter table public.mentor_exam_provenance enable row level security;
revoke all on public.mentor_exam_provenance from public, anon, authenticated;;
