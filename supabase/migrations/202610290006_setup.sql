begin;
alter table public.mentor_preferences add column onboarding_step int not null default 0 check(onboarding_step between 0 and 5);
alter table public.mentor_preferences add column goal int not null default 0 check(goal between 0 and 3);
alter table public.mentor_preferences add column minutes int not null default 0 check(minutes=0 or minutes between 5 and 240);
alter table public.mentor_preferences add column consented_at timestamptz;
alter table public.mentor_enrollments add column selected boolean not null default true;
create function public.mentor_setup(data jsonb) returns jsonb language plpgsql security definer set search_path='' as $$
declare uid uuid:=auth.uid(); p public.mentor_preferences; next_step int; chosen text[];
begin
 if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
 select * into p from public.mentor_preferences where user_id=uid for update;
 next_step:=greatest(p.onboarding_step,coalesce((data->>'step')::int,p.onboarding_step));
 if data ? 'subjects' then select array_agg(value) into chosen from jsonb_array_elements_text(data->'subjects'); end if;
 if next_step=5 and (coalesce((data->>'grade')::int,p.grade,0) not between 10 and 12 or char_length(coalesce(data->>'nickname',(select nickname from public.mentor_profiles where id=uid)))<2) then raise sqlstate 'PT400' using message='INVALID_PROFILE'; end if;
 update public.mentor_preferences set onboarding_step=next_step,grade=coalesce((data->>'grade')::int,grade),subjects=case when data ? 'subjects' then coalesce(chosen,'{}') else subjects end,goal=coalesce((data->>'goal')::int,goal),minutes=coalesce((data->>'minutes')::int,minutes),consented_at=case when next_step=5 then coalesce(consented_at,now()) else consented_at end where user_id=uid returning * into p;
 if data ? 'nickname' then update public.mentor_profiles set nickname=data->>'nickname' where id=uid; end if;
 return to_jsonb(p);
end; $$;
revoke all on function public.mentor_setup(jsonb) from public,anon;
grant execute on function public.mentor_setup(jsonb) to authenticated;
create function public.mentor_selected_subjects(g int) returns jsonb language sql security definer set search_path='' as $$
 select jsonb_build_object('items',coalesce(jsonb_agg(subject_id),'[]')) from public.mentor_enrollments where user_id=auth.uid() and grade=g and selected;
$$;
revoke all on function public.mentor_selected_subjects(int) from public,anon;
grant execute on function public.mentor_selected_subjects(int) to authenticated;
create function public.mentor_select_subjects(g int,subjects text[]) returns jsonb language plpgsql security definer set search_path='' as $$
declare uid uuid:=auth.uid(); s text;
begin
 if uid is null then raise sqlstate 'PT401' using message='AUTHENTICATION_REQUIRED'; end if;
 if g not between 10 and 12 or not(subjects <@ array['physics','chemistry','biology']) then raise sqlstate 'PT400' using message='INVALID_SUBJECTS'; end if;
 perform pg_advisory_xact_lock(hashtextextended(uid::text,0));
 update public.mentor_enrollments set selected=false where user_id=uid and grade=g;
 foreach s in array subjects loop
  insert into public.mentor_enrollments(user_id,subject_id,grade,selected) values(uid,s,g,true) on conflict(user_id,subject_id,grade) do update set selected=true;
 end loop;
 return public.mentor_selected_subjects(g);
end; $$;
revoke all on function public.mentor_select_subjects(int,text[]) from public,anon;
grant execute on function public.mentor_select_subjects(int,text[]) to authenticated;
commit;
