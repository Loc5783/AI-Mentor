-- Search public learner names with or without Vietnamese diacritics.
create extension if not exists unaccent with schema extensions;

create or replace function public.mentor_search_learners(p_query text)
returns table(id uuid, nickname text)
language sql stable security invoker set search_path = '' as $$
  select p.id, p.nickname
  from public.mentor_profiles p
  where auth.uid() is not null
    and char_length(trim(p_query)) between 2 and 40
    and position(
      extensions.unaccent(translate(lower(trim(p_query)), 'đ', 'd'))
      in extensions.unaccent(translate(lower(p.nickname), 'đ', 'd'))
    ) > 0
  order by p.nickname asc
  limit 30;
$$;
revoke all on function public.mentor_search_learners(text) from public, anon;
grant execute on function public.mentor_search_learners(text) to authenticated;
