-- Server-only transaction boundary for the existing Python catalog policies.
create schema if not exists mentor_admin_private;
revoke all on schema mentor_admin_private from public, anon, authenticated;
grant usage on schema mentor_admin_private to service_role;
create table if not exists mentor_admin_private.requests (
  request_key text primary key, request_hash text not null,
  response jsonb not null, created_at timestamptz not null default now()
);
alter table mentor_admin_private.requests enable row level security;
revoke all on mentor_admin_private.requests from public, anon, authenticated;
grant select, insert on mentor_admin_private.requests to service_role;

create or replace function public.mentor_admin_catalog(
  p_action text, p_token text default null, p_changes jsonb default '{}'::jsonb,
  p_request_key text default null, p_request_hash text default null,
  p_response jsonb default null
) returns jsonb language plpgsql security invoker set search_path = '' as $$
declare
  snapshot jsonb; cached mentor_admin_private.requests%rowtype;
  target text; table_name text; cols text; updates text; records jsonb;
begin
  if current_user <> 'service_role' then
    raise insufficient_privilege using message = 'SERVER_ONLY';
  end if;
  if p_action not in ('snapshot','commit','cached') then
    raise invalid_parameter_value using message = 'INVALID_ACTION';
  end if;
  if p_action = 'commit' then
    -- Covers both validation dependencies and mutations, including other REST writers.
    lock table public.mentor_questions, public.mentor_library_quizzes,
      public.mentor_exams in share row exclusive mode;
  end if;
  if p_request_key is not null then
    select * into cached from mentor_admin_private.requests where request_key=p_request_key;
    if found then
      if cached.request_hash is distinct from p_request_hash then
        raise sqlstate 'PT409' using message = 'IDEMPOTENCY_CONFLICT';
      end if;
      return jsonb_build_object('cached',true,'response',cached.response);
    end if;
  end if;
  if p_action = 'cached' then return jsonb_build_object('cached',false); end if;
  select jsonb_build_object(
    'questions', (select coalesce(jsonb_agg(to_jsonb(t) order by t.id),'[]'::jsonb) from public.mentor_questions t),
    'quizzes', (select coalesce(jsonb_agg(to_jsonb(t) order by t.id),'[]'::jsonb) from public.mentor_library_quizzes t),
    'exams', (select coalesce(jsonb_agg(to_jsonb(t) order by t.id),'[]'::jsonb) from public.mentor_exams t)
  ) into snapshot;
  if p_action='snapshot' then
    return jsonb_build_object('data',snapshot,'token',md5(snapshot::text));
  end if;
  if p_token is distinct from md5(snapshot::text) then
    raise sqlstate 'PT409' using message = 'CATALOG_CHANGED_RELOAD';
  end if;
  if jsonb_typeof(p_changes) <> 'object' or
    exists(select 1 from jsonb_object_keys(p_changes) k where k not in ('questions','quizzes','exams')) then
    raise invalid_parameter_value using message = 'INVALID_CHANGES';
  end if;
  foreach target in array array['questions','quizzes','exams'] loop
    records := coalesce(p_changes->target,'[]'::jsonb);
    if jsonb_typeof(records) <> 'array' or jsonb_array_length(records)>2000 then
      raise invalid_parameter_value using message = 'INVALID_BATCH';
    end if;
    if jsonb_array_length(records)=0 then continue; end if;
    table_name := case target when 'questions' then 'mentor_questions'
      when 'quizzes' then 'mentor_library_quizzes' else 'mentor_exams' end;
    select string_agg(quote_ident(a.attname),',' order by a.attnum),
      string_agg(format('%I=excluded.%I',a.attname,a.attname),',' order by a.attnum)
        filter(where a.attname not in ('id','created_at','created_by'))
      into cols,updates from pg_catalog.pg_attribute a
      where a.attrelid=format('public.%I',table_name)::regclass
        and a.attnum>0 and not a.attisdropped;
    execute format('insert into public.%I (%s) select %s from jsonb_populate_recordset(null::public.%I,$1) on conflict(id) do update set %s',
      table_name,cols,cols,table_name,updates) using records;
  end loop;
  if p_request_key is not null then
    insert into mentor_admin_private.requests(request_key,request_hash,response)
      values(p_request_key,p_request_hash,p_response);
  end if;
  return jsonb_build_object('cached',false,'response',p_response);
end $$;
revoke all on function public.mentor_admin_catalog(text,text,jsonb,text,text,jsonb) from public, anon, authenticated;
grant execute on function public.mentor_admin_catalog(text,text,jsonb,text,text,jsonb) to service_role;
