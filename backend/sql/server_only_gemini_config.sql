-- No Gemini credentials are granted to mobile/web users.
create or replace function public.mentor_ai_server_config()
returns jsonb language sql stable security definer set search_path = '' as $$
  select decrypted_secret::jsonb from vault.decrypted_secrets
  where name='mentor_gemini_config' limit 1
$$;
revoke all on function public.mentor_ai_server_config() from public,anon,authenticated;
grant execute on function public.mentor_ai_server_config() to service_role;

create or replace function public.mentor_set_ai_server_config(p_config jsonb)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare secret_id uuid;
begin
  if jsonb_typeof(p_config->'keys') is distinct from 'array' then
    raise invalid_parameter_value using message='INVALID_AI_CONFIG';
  end if;
  if jsonb_array_length(p_config->'keys') not between 1 and 16
    or exists(select 1 from jsonb_array_elements(p_config->'keys') k
      where jsonb_typeof(k)<>'string' or length(trim(k#>>'{}')) not between 1 and 512)
    or coalesce(p_config->>'model','') !~ '^[a-zA-Z0-9._-]+$' then
    raise invalid_parameter_value using message='INVALID_AI_CONFIG';
  end if;
  select id into secret_id from vault.secrets where name='mentor_gemini_config';
  if secret_id is null then
    perform vault.create_secret(p_config::text,'mentor_gemini_config','Server-only Gemini failover configuration');
  else
    perform vault.update_secret(secret_id,p_config::text,'mentor_gemini_config','Server-only Gemini failover configuration');
  end if;
  return jsonb_build_object('key_count',jsonb_array_length(p_config->'keys'));
end $$;
revoke all on function public.mentor_set_ai_server_config(jsonb) from public,anon,authenticated;
grant execute on function public.mentor_set_ai_server_config(jsonb) to service_role;
