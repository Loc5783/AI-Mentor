create schema if not exists migration_archive;
revoke all on schema migration_archive from public,anon,authenticated;
grant usage on schema migration_archive to service_role;
create table if not exists migration_archive.legacy_records(
 source_id text not null, table_name text not null, legacy_key text not null,
 payload_sha256 text not null check(length(payload_sha256)=64),
 payload jsonb not null, imported_at timestamptz not null default now(),
 primary key(source_id,table_name,legacy_key,payload_sha256));
alter table migration_archive.legacy_records enable row level security;
revoke all on migration_archive.legacy_records from public,anon,authenticated;
grant select,insert on migration_archive.legacy_records to service_role;
create table if not exists public.mentor_questions(
 id text primary key, code text unique, version integer not null default 1,
 subject_id text not null,grade integer not null check(grade between 10 and 12),
 chapter text not null,topic text not null,textbook text,prompt text not null,
 option_a text not null,option_b text not null,option_c text not null,option_d text not null,
 correct_option text not null check(correct_option in ('A','B','C','D')),
 explanation text not null default '',difficulty text not null,points integer not null,
 image_url text,source text,author text,license text,
 status text not null default 'DRAFT' check(status in ('DRAFT','PUBLISHED')),
 created_by uuid references auth.users(id),
 created_at timestamptz not null default now(),updated_at timestamptz not null default now());
alter table public.mentor_questions enable row level security;
revoke all on public.mentor_questions from public,anon,authenticated;
grant select,insert,update on public.mentor_questions to service_role;
create table if not exists public.mentor_exams(
 id text primary key,title text not null,subject_id text not null,grade integer not null check(grade between 10 and 12),
 exam_type text not null,semester integer not null,duration_minutes integer not null,
 question_ids jsonb not null check(jsonb_typeof(question_ids)='array'),total_points integer not null,
 question_count integer not null,status text not null check(status in ('DRAFT','PUBLISHED')),
 version integer not null default 1,created_by uuid references auth.users(id),
 created_at timestamptz not null default now(),updated_at timestamptz not null default now());
alter table public.mentor_exams enable row level security;
revoke all on public.mentor_exams from public,anon,authenticated;
grant select,insert,update on public.mentor_exams to service_role;;
