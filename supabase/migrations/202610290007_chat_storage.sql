begin;
create table public.mentor_chat_folders(id uuid primary key default gen_random_uuid(),user_id uuid not null references auth.users(id) on delete cascade,title text not null check(char_length(title) between 1 and 100),unique(id,user_id));
create table public.mentor_chat_threads(id uuid primary key default gen_random_uuid(),user_id uuid not null references auth.users(id) on delete cascade,folder_id uuid,title text not null default '' check(char_length(title)<=100),draft text not null default '' check(char_length(draft)<=30000),automatic_title boolean not null default true,updated_at timestamptz not null default now(),unique(id,user_id),foreign key(folder_id,user_id) references public.mentor_chat_folders(id,user_id));
create table public.mentor_chat_messages(id uuid primary key default gen_random_uuid(),user_id uuid not null,thread_id uuid not null,turn_index int not null,role text not null check(role in ('USER','ASSISTANT')),text text not null check(char_length(text)<=120000),status text not null check(status in ('PENDING','SENT','FAILED','CANCELLED')),error text not null default '',truncated boolean not null default false,pending_device text,updated_at timestamptz not null default now(),unique(thread_id,turn_index,role),unique(id,user_id,thread_id),foreign key(thread_id,user_id) references public.mentor_chat_threads(id,user_id) on delete cascade);
create table public.mentor_chat_files(id uuid primary key,user_id uuid not null,thread_id uuid not null,message_id uuid,metadata jsonb not null,foreign key(thread_id,user_id) references public.mentor_chat_threads(id,user_id) on delete cascade,foreign key(message_id,user_id,thread_id) references public.mentor_chat_messages(id,user_id,thread_id) on delete cascade);
alter table public.mentor_chat_folders enable row level security;
alter table public.mentor_chat_threads enable row level security;
alter table public.mentor_chat_messages enable row level security;
alter table public.mentor_chat_files enable row level security;
revoke all on public.mentor_chat_folders,public.mentor_chat_threads,public.mentor_chat_messages,public.mentor_chat_files from anon,authenticated;
create function public.mentor_chat_thread_view(tid uuid) returns jsonb language sql security definer set search_path='' as $$
 select jsonb_build_object('id',id,'title',title,'folderId',folder_id,'draft',draft,'automaticTitle',automatic_title,'updatedAt',(extract(epoch from updated_at)*1000)::bigint) from public.mentor_chat_threads where id=tid and user_id=auth.uid();
$$;
revoke all on function public.mentor_chat_thread_view(uuid) from public,anon,authenticated;
create function public.mentor_chat_action(op text,data jsonb default '{}') returns jsonb language plpgsql security definer set search_path='' as $$
declare uid uuid:=auth.uid(); tid uuid; fid uuid; mid uuid; item uuid; result jsonb; rows jsonb; turn int; rowmsg public.mentor_chat_messages;
begin
 if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
 perform pg_advisory_xact_lock(hashtextextended(uid::text,0));
 tid:=nullif(data->>'threadId','')::uuid; fid:=nullif(data->>'folderId','')::uuid; mid:=nullif(data->>'messageId','')::uuid;
 if tid is not null and not exists(select 1 from public.mentor_chat_threads where id=tid and user_id=uid) then raise sqlstate 'PT404' using message='CHAT_NOT_FOUND'; end if;
 if fid is not null and not exists(select 1 from public.mentor_chat_folders where id=fid and user_id=uid) then raise sqlstate 'PT404' using message='FOLDER_NOT_FOUND'; end if;
 if op='create' then
  insert into public.mentor_chat_threads(user_id,folder_id) values(uid,fid) returning id into tid;
  return public.mentor_chat_thread_view(tid);
 elsif op='thread' then return public.mentor_chat_thread_view(tid);
 elsif op='threads' then
  select coalesce(jsonb_agg(public.mentor_chat_thread_view(id) order by updated_at desc,id),'[]') into rows from public.mentor_chat_threads where user_id=uid and (coalesce(data->>'query','')='' or strpos(lower(title),lower(data->>'query'))>0 or strpos(lower(draft),lower(data->>'query'))>0);
  return jsonb_build_object('items',rows);
 elsif op='folders' then
  select coalesce(jsonb_agg(jsonb_build_object('id',id,'title',title) order by title,id),'[]') into rows from public.mentor_chat_folders where user_id=uid;
  return jsonb_build_object('items',rows);
 elsif op='create_folder' then
  insert into public.mentor_chat_folders(user_id,title) values(uid,trim(data->>'title')) returning id into item;
  return jsonb_build_object('id',item);
 elsif op='rename_folder' then update public.mentor_chat_folders set title=trim(data->>'title') where id=fid and user_id=uid;
 elsif op='delete_folder' then
  if coalesce((data->>'deleteChats')::boolean,false) then delete from public.mentor_chat_threads where user_id=uid and folder_id=fid;
  else update public.mentor_chat_threads set folder_id=null,updated_at=now() where user_id=uid and folder_id=fid; end if;
  delete from public.mentor_chat_folders where user_id=uid and id=fid;
 elsif op='rename_thread' then update public.mentor_chat_threads set title=trim(data->>'title'),automatic_title=false,updated_at=now() where id=tid and user_id=uid;
 elsif op='move_thread' then update public.mentor_chat_threads set folder_id=fid,updated_at=now() where id=tid and user_id=uid;
 elsif op='delete_thread' then delete from public.mentor_chat_threads where id=tid and user_id=uid;
 elsif op='draft' then update public.mentor_chat_threads set draft=data->>'text' where id=tid and user_id=uid;
 elsif op='add_file' then
  if (select count(*) from public.mentor_chat_files where user_id=uid and thread_id=tid and message_id is null)>=4 then raise sqlstate 'PT400' using message='TOO_MANY_FILES'; end if;
  if (data->'file'->>'size')::int not between 1 and 8388608 or (data->'file'->>'storageKey') !~ '^[0-9a-fA-F-]{36}\.bin$' then raise sqlstate 'PT400' using message='INVALID_FILE'; end if;
  insert into public.mentor_chat_files(id,user_id,thread_id,metadata) values((data->'file'->>'id')::uuid,uid,tid,data->'file');
 elsif op='remove_file' then delete from public.mentor_chat_files where user_id=uid and thread_id=tid and id=(data->>'fileId')::uuid and message_id is null;
 elsif op='stage' then
  if length(trim(coalesce(data->>'text','')))=0 and not exists(select 1 from public.mentor_chat_files where user_id=uid and thread_id=tid and message_id is null) then raise sqlstate 'PT400' using message='EMPTY_MESSAGE'; end if;
  if char_length(data->>'text')>30000 then raise sqlstate 'PT400' using message='MESSAGE_TOO_LONG'; end if;
  select coalesce(max(turn_index),0)+1 into turn from public.mentor_chat_messages where user_id=uid and thread_id=tid;
  insert into public.mentor_chat_messages(user_id,thread_id,turn_index,role,text,status,pending_device) values(uid,tid,turn,'USER',data->>'text','PENDING',data->>'device_id') returning id into mid;
  update public.mentor_chat_files set message_id=mid where user_id=uid and thread_id=tid and message_id is null;
  update public.mentor_chat_threads set draft='',updated_at=now(),title=case when automatic_title and title='' then left(coalesce(nullif(trim(data->>'text'),''),'Chat'),70) else title end where user_id=uid and id=tid;
  return jsonb_build_object('id',mid);
 elsif op='retry' then
  update public.mentor_chat_messages set status='PENDING',error='',pending_device=data->>'device_id',updated_at=now() where user_id=uid and thread_id=tid and id=mid and role='USER' and status in ('FAILED','CANCELLED');
  if not found then raise sqlstate 'PT409' using message='MESSAGE_NOT_RETRYABLE'; end if;
 elsif op='finish' then
  select * into rowmsg from public.mentor_chat_messages where user_id=uid and thread_id=tid and id=mid and role='USER' and status='PENDING' for update;
  if rowmsg.id is null then return jsonb_build_object('changed',false); end if;
  update public.mentor_chat_messages set status=case when data ? 'reply' then 'SENT' when data->>'error'='CANCELLED' then 'CANCELLED' else 'FAILED' end,error=coalesce(data->>'error',''),updated_at=now() where id=mid;
  if data ? 'reply' then insert into public.mentor_chat_messages(user_id,thread_id,turn_index,role,text,status,truncated) values(uid,tid,rowmsg.turn_index,'ASSISTANT',data->>'reply','SENT',coalesce((data->>'truncated')::boolean,false)); end if;
  update public.mentor_chat_threads set updated_at=now() where id=tid;
  return jsonb_build_object('changed',true);
 elsif op='recover' then
  update public.mentor_chat_messages set status='CANCELLED',error='CANCELLED',updated_at=now() where user_id=uid and role='USER' and status='PENDING' and (pending_device=data->>'device_id' or updated_at<now()-interval '10 minutes');
 elsif op='snapshot' then
  select coalesce(jsonb_agg(jsonb_build_object('id',m.id,'text',m.text,'role',m.role,'status',m.status,'error',m.error,'truncated',m.truncated,'files',coalesce((select jsonb_agg(f.metadata) from public.mentor_chat_files f where f.user_id=uid and f.thread_id=tid and f.message_id=m.id),'[]')) order by m.turn_index,case when m.role='USER' then 0 else 1 end),'[]') into rows from public.mentor_chat_messages m where m.user_id=uid and m.thread_id=tid;
  return jsonb_build_object('thread',public.mentor_chat_thread_view(tid),'messages',rows,'draftFiles',coalesce((select jsonb_agg(metadata) from public.mentor_chat_files where user_id=uid and thread_id=tid and message_id is null),'[]'));
 else raise sqlstate 'PT400' using message='UNKNOWN_OPERATION'; end if;
 return '{}';
end; $$;
revoke all on function public.mentor_chat_action(text,jsonb) from public,anon;
grant execute on function public.mentor_chat_action(text,jsonb) to authenticated;
insert into storage.buckets(id,name,public,file_size_limit) values('mentor-chat-files','mentor-chat-files',false,8388608),('mentor-avatars','mentor-avatars',false,2097152) on conflict do nothing;
create policy mentor_file_insert on storage.objects for insert to authenticated with check(bucket_id in ('mentor-chat-files','mentor-avatars') and (storage.foldername(name))[1]=(select auth.uid())::text);
create policy mentor_file_read on storage.objects for select to authenticated using((bucket_id='mentor-chat-files' and (storage.foldername(name))[1]=(select auth.uid())::text) or bucket_id='mentor-avatars');
create policy mentor_file_update on storage.objects for update to authenticated using(bucket_id in ('mentor-chat-files','mentor-avatars') and (storage.foldername(name))[1]=(select auth.uid())::text) with check(bucket_id in ('mentor-chat-files','mentor-avatars') and (storage.foldername(name))[1]=(select auth.uid())::text);
create policy mentor_file_delete on storage.objects for delete to authenticated using(bucket_id in ('mentor-chat-files','mentor-avatars') and (storage.foldername(name))[1]=(select auth.uid())::text);
commit;
