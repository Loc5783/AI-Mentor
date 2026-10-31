-- Phase 1: Supabase Auth owns credentials; these tables contain application data only.
-- Run the entire file in Supabase SQL Editor. It never deletes local or existing app data.
begin;
create table public.mentor_profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  nickname text not null default 'Học viên' check (char_length(nickname) between 1 and 80),
  bio text not null default '' check (char_length(bio) <= 500),
  avatar_path text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create table public.mentor_preferences (
  user_id uuid primary key references auth.users(id) on delete cascade,
  grade smallint check (grade in (10,11,12)),
  subjects text[] not null default '{}',
  language text not null default 'vi' check (language in ('vi','en')),
  theme text not null default 'system' check (theme in ('system','light','dark')),
  updated_at timestamptz not null default now(),
  check (subjects <@ array['math','literature','english','history','geography','chemistry','physics','biology','economics_law']::text[])
);
create table public.mentor_timetable (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  day smallint not null check (day between 1 and 7),
  period smallint not null check (period between 1 and 10),
  subject text not null check (char_length(subject) between 1 and 120),
  teacher text check (char_length(teacher) <= 120),
  notes text check (char_length(notes) <= 2000),
  updated_at timestamptz not null default now(),
  unique (user_id,day,period)
);

create function public.mentor_stamp_updated_at() returns trigger language plpgsql set search_path = '' as $$
begin new.updated_at = now(); return new; end;
$$;
create trigger mentor_profiles_updated before update on public.mentor_profiles for each row execute function public.mentor_stamp_updated_at();
create trigger mentor_preferences_updated before update on public.mentor_preferences for each row execute function public.mentor_stamp_updated_at();
create trigger mentor_timetable_updated before update on public.mentor_timetable for each row execute function public.mentor_stamp_updated_at();

-- No role, EXP or reward fields come from user metadata.
create function public.mentor_create_user() returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.mentor_profiles(id,nickname)
    values(new.id,coalesce(nullif(left(trim(new.raw_user_meta_data->>'nickname'),80),''),'Học viên'));
  insert into public.mentor_preferences(user_id) values(new.id);
  return new;
end;
$$;
revoke all on function public.mentor_create_user() from public,anon,authenticated;
create trigger mentor_auth_user_created after insert on auth.users for each row execute function public.mentor_create_user();
-- Backfill accounts created before installing this migration.
insert into public.mentor_profiles(id) select id from auth.users on conflict do nothing;
insert into public.mentor_preferences(user_id) select id from auth.users on conflict do nothing;

alter table public.mentor_profiles enable row level security;
alter table public.mentor_preferences enable row level security;
alter table public.mentor_timetable enable row level security;
revoke all on public.mentor_profiles,public.mentor_preferences,public.mentor_timetable from anon,authenticated;
grant select on public.mentor_profiles to authenticated;
grant update(nickname,bio,avatar_path) on public.mentor_profiles to authenticated;
grant select on public.mentor_preferences to authenticated;
grant update(grade,subjects,language,theme) on public.mentor_preferences to authenticated;
grant select,insert,update,delete on public.mentor_timetable to authenticated;
create policy mentor_profile_read on public.mentor_profiles for select to authenticated using (true);
create policy mentor_profile_edit on public.mentor_profiles for update to authenticated using ((select auth.uid())=id) with check ((select auth.uid())=id);
create policy mentor_preferences_owner on public.mentor_preferences for all to authenticated using ((select auth.uid())=user_id) with check ((select auth.uid())=user_id);
create policy mentor_timetable_owner on public.mentor_timetable for all to authenticated using ((select auth.uid())=user_id) with check ((select auth.uid())=user_id);

-- Explicit publication only for tables needed by the first sync phase.
do $$ declare t text; begin
  foreach t in array array['mentor_profiles','mentor_preferences','mentor_timetable'] loop
    if not exists(select 1 from pg_publication_tables where pubname='supabase_realtime' and schemaname='public' and tablename=t) then
      execute format('alter publication supabase_realtime add table public.%I',t);
    end if;
  end loop;
end $$;
commit;

