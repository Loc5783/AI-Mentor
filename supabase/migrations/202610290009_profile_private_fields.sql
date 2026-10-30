alter table public.mentor_preferences
  add column address text not null default '',
  add column phone text not null default '';

grant update(address,phone) on public.mentor_preferences to authenticated;
