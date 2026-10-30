begin;

alter table public.mentor_friendships
  drop constraint if exists mentor_friendships_state_check;
alter table public.mentor_friendships
  add constraint mentor_friendships_state_check
  check (state in ('PENDING','ACCEPTED','DECLINED'));
alter table public.mentor_friendships
  add column if not exists revision int not null default 1,
  add column if not exists updated_at timestamptz not null default now();

create table if not exists public.mentor_social_requests(
  user_id uuid not null references auth.users(id) on delete cascade,
  request_key text not null,
  fingerprint text not null,
  result jsonb not null,
  created_at timestamptz not null default now(),
  primary key(user_id,request_key)
);
alter table public.mentor_social_requests enable row level security;
revoke all on public.mentor_social_requests from public,anon,authenticated;

alter function public.mentor_social_api(text,text,jsonb,jsonb) rename to mentor_social_api_v1;
revoke all on function public.mentor_social_api_v1(text,text,jsonb,jsonb) from public,anon,authenticated;

create function public.mentor_social_api(method text,path text,body jsonb default '{}',query jsonb default '{}')
returns jsonb language plpgsql security definer set search_path='' as $$
declare
  uid uuid:=auth.uid();
  target uuid;
  item uuid;
  expected int;
  current_revision int;
  state_value text;
  result jsonb;
  rows jsonb;
  request_key text:=nullif(query->>'requestKey','');
  fingerprint text:=md5(method||':'||path||':'||coalesce(body,'{}'::jsonb)::text);
  cached public.mentor_social_requests;
begin
  if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
  if path not like '/friendships%' then
    return public.mentor_social_api_v1(method,path,body,query);
  end if;

  perform pg_advisory_xact_lock(hashtextextended(uid::text,1));
  if method='POST' and request_key is not null then
    if char_length(request_key) not between 1 and 100 then raise sqlstate 'PT400' using message='INVALID_REQUEST_KEY'; end if;
    select * into cached from public.mentor_social_requests where user_id=uid and mentor_social_requests.request_key=request_key;
    if cached.request_key is not null then
      if cached.fingerprint<>fingerprint then raise sqlstate 'PT409' using message='IDEMPOTENCY_CONFLICT'; end if;
      return cached.result;
    end if;
  end if;

  if path='/friendships' and method='GET' then
    select coalesce(jsonb_agg(jsonb_build_object(
      'id',f.id,'requesterProfileId',f.requester,'recipientProfileId',f.recipient,
      'state',f.state,'status',f.state,'revision',f.revision,
      'updatedAt',f.updated_at,'direction',case when f.requester=uid then 'OUTGOING' else 'INCOMING' end,
      'profile',public.mentor_public_learner(case when f.requester=uid then f.recipient else f.requester end)
    ) order by f.updated_at desc,f.id),'[]') into rows
    from public.mentor_friendships f where f.requester=uid or f.recipient=uid;
    return jsonb_build_object('items',rows,'nextCursor',null,'asOf',now());
  elsif path='/friendships' and method='POST' then
    target:=coalesce(nullif(body->>'recipientProfileId',''),nullif(body->>'profileId',''))::uuid;
    if target is null then raise sqlstate 'PT400' using message='INVALID_PROFILE'; end if;
    if target=uid then raise sqlstate 'PT400' using message='CANNOT_FRIEND_SELF'; end if;
    if not exists(select 1 from public.mentor_profiles where id=target) then raise sqlstate 'PT404' using message='PROFILE_NOT_FOUND'; end if;
    if exists(select 1 from public.mentor_friendships where least(requester,recipient)=least(uid,target) and greatest(requester,recipient)=greatest(uid,target)) then
      raise sqlstate 'PT409' using message='FRIENDSHIP_EXISTS';
    end if;
    insert into public.mentor_friendships(requester,recipient,state,revision,updated_at)
      values(uid,target,'PENDING',1,now()) returning id into item;
    result:=jsonb_build_object('id',item,'requesterProfileId',uid,'recipientProfileId',target,
      'state','PENDING','status','PENDING','revision',1,'updatedAt',now(),'direction','OUTGOING');
    if request_key is not null then
      insert into public.mentor_social_requests(user_id,request_key,fingerprint,result)
        values(uid,request_key,fingerprint,result);
    end if;
    return result;
  elsif path ~ '^/friendships/[^/]+$' and method='PATCH' then
    item:=split_part(path,'/',3)::uuid;
    expected:=nullif(query->>'expectedRevision','')::int;
    select revision,state into current_revision,state_value from public.mentor_friendships
      where id=item and recipient=uid for update;
    if current_revision is null then raise sqlstate 'PT404' using message='FRIENDSHIP_NOT_FOUND'; end if;
    if expected is not null and expected<>current_revision then raise sqlstate 'PT409' using message='REVISION_CONFLICT'; end if;
    if state_value<>'PENDING' then raise sqlstate 'PT409' using message='FRIENDSHIP_ALREADY_DECIDED'; end if;
    state_value:=case upper(coalesce(body->>'decision','')) when 'ACCEPT' then 'ACCEPTED' when 'DECLINE' then 'DECLINED' else null end;
    if state_value is null then raise sqlstate 'PT400' using message='INVALID_DECISION'; end if;
    update public.mentor_friendships set state=state_value,revision=revision+1,updated_at=now()
      where id=item returning jsonb_build_object('id',id,'requesterProfileId',requester,
        'recipientProfileId',recipient,'state',state,'status',state,'revision',revision,'updatedAt',updated_at) into result;
    return result;
  elsif path ~ '^/friendships/[^/]+$' and method='DELETE' then
    item:=split_part(path,'/',3)::uuid;
    expected:=nullif(query->>'expectedRevision','')::int;
    select revision into current_revision from public.mentor_friendships
      where id=item and (requester=uid or recipient=uid) for update;
    if current_revision is null then raise sqlstate 'PT404' using message='FRIENDSHIP_NOT_FOUND'; end if;
    if expected is not null and expected<>current_revision then raise sqlstate 'PT409' using message='REVISION_CONFLICT'; end if;
    delete from public.mentor_friendships where id=item;
    return '{}';
  end if;
  raise sqlstate 'PT404' using message='API_NOT_IMPLEMENTED';
end; $$;
revoke all on function public.mentor_social_api(text,text,jsonb,jsonb) from public,anon;
grant execute on function public.mentor_social_api(text,text,jsonb,jsonb) to authenticated;

commit;
