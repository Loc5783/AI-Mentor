begin;

-- Persist this decision per enrollment in the existing placement JSON column.
-- Lesson order/review gates remain active; only the placement requirement is skipped.
create or replace function public.mentor_choose_learning_start(p_enrollment_id uuid)
returns jsonb
language plpgsql
security definer
set search_path=''
as $$
declare
  uid uuid := auth.uid();
  enrollment public.mentor_enrollments;
begin
  if uid is null then
    raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED';
  end if;

  select * into enrollment
  from public.mentor_enrollments
  where id=p_enrollment_id and user_id=uid
  for update;
  if enrollment.id is null then
    raise sqlstate 'PT404' using message='ENROLLMENT_NOT_FOUND';
  end if;

  if enrollment.placement is null then
    update public.mentor_enrollments
    set placement=jsonb_build_object('mode','FROM_BEGINNING','skippedUnitIds','[]'::jsonb),
        revision=revision+1,
        updated_at=now()
    where id=enrollment.id and user_id=uid;
  elsif enrollment.placement->>'mode' is distinct from 'FROM_BEGINNING' then
    raise sqlstate 'PT409' using message='PLACEMENT_ALREADY_COMPLETED';
  end if;

  return public.mentor_learning_api(
    'GET',
    '/enrollments/' || enrollment.id::text || '/path',
    '{}'::jsonb,
    null,
    null,
    '{}'::jsonb
  );
end;
$$;

revoke all on function public.mentor_choose_learning_start(uuid) from public,anon;
grant execute on function public.mentor_choose_learning_start(uuid) to authenticated;

commit;
