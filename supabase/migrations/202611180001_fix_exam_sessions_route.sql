-- The generic exam detail route also matches /exams/sessions and returns EXAM_NOT_FOUND.
-- Keep the latest exam API implementation intact while excluding that reserved path.
do $$
declare
  definition text;
  old_route constant text := 'elsif path ~ ''^/exams/[^/]+$'' and method=''GET'' then';
  new_route constant text := 'elsif path ~ ''^/exams/[^/]+$'' and path <> ''/exams/sessions'' and method=''GET'' then';
begin
  definition := pg_get_functiondef('public.mentor_exam_api(text,text,jsonb,jsonb)'::regprocedure);
  if position(new_route in definition) > 0 then
    return;
  end if;
  if position(old_route in definition) = 0 then
    raise exception 'mentor_exam_api route has changed; inspect before applying this migration';
  end if;
  execute replace(definition, old_route, new_route);
end $$;
