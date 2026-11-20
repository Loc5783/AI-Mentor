begin;

-- The Android ranking contract uses competition ranks (1, 1, 3), not dense
-- ranks (1, 1, 2). Keep the existing social API and its access controls.
do $$
declare
  definition text := pg_get_functiondef('public.mentor_social_api_v1(text,text,jsonb,jsonb)'::regprocedure);
  old_expression text := 'dense_rank() over(order by score desc)';
begin
  if position(old_expression in definition) = 0 then
    raise exception 'Expected ranking expression is missing from mentor_social_api_v1';
  end if;
  execute replace(definition, old_expression, 'rank() over(order by score desc)');
end;
$$;

commit;
