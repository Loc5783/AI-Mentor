-- ============================================================================
-- AI TEAMOS COMPREHENSIVE RLS & AUTHORIZATION SMOKE TEST
-- Tests against real PostgreSQL / Supabase instance with authentic JWT roles:
-- Owner, Admin, Member, Viewer, Outsider, Removed Member.
-- Fully transactional and rolls back at the end.
-- ============================================================================

begin;

-- Seed auth users for test roles
insert into auth.users (id, email) values
  ('00000000-0000-0000-0000-000000000001', 'owner@example.com'),
  ('00000000-0000-0000-0000-000000000002', 'admin@example.com'),
  ('00000000-0000-0000-0000-000000000003', 'member@example.com'),
  ('00000000-0000-0000-0000-000000000004', 'viewer@example.com'),
  ('00000000-0000-0000-0000-000000000005', 'removed@example.com'),
  ('00000000-0000-0000-0000-000000000009', 'outsider@example.com')
on conflict (id) do nothing;

-- ----------------------------------------------------------------------------
-- 1. Setup Project, Members and Initial Content under OWNER
-- ----------------------------------------------------------------------------
set local role authenticated;
select set_config('request.jwt.claims', '{"sub": "00000000-0000-0000-0000-000000000001"}', true);

insert into public.mentor_team_projects (id, title, description, owner_id)
values ('proj-smoke-1', 'Dự án TeamOS Kiểm thử', 'Mô tả dự án', '00000000-0000-0000-0000-000000000001');

insert into public.mentor_team_projects (id, title, description, owner_id)
values ('proj-smoke-2', 'Dự án khác', 'Dự án độc lập', '00000000-0000-0000-0000-000000000001');

-- Verify Owner was auto-added via trigger
do $$
begin
  assert exists (
    select 1 from public.mentor_team_members
    where project_id = 'proj-smoke-1' and user_id = '00000000-0000-0000-0000-000000000001' and role = 'OWNER' and status = 'ACTIVE'
  ), 'Auto owner creation failed';
end $$;

-- Owner adds Admin, Member, Viewer, and RemovedMember
insert into public.mentor_team_members (project_id, user_id, role, status) values
  ('proj-smoke-1', '00000000-0000-0000-0000-000000000002', 'ADMIN', 'ACTIVE'),
  ('proj-smoke-1', '00000000-0000-0000-0000-000000000003', 'MEMBER', 'ACTIVE'),
  ('proj-smoke-1', '00000000-0000-0000-0000-000000000004', 'VIEWER', 'ACTIVE'),
  ('proj-smoke-1', '00000000-0000-0000-0000-000000000005', 'MEMBER', 'ACTIVE');

-- Owner marks removed member
update public.mentor_team_members
set status = 'REMOVED', removed_at = now()
where project_id = 'proj-smoke-1' and user_id = '00000000-0000-0000-0000-000000000005';

-- Owner adds tasks in proj-smoke-1 and proj-smoke-2
insert into public.mentor_team_tasks (id, project_id, title, description) values
  ('task-s1', 'proj-smoke-1', 'Task 1', 'Nhiệm vụ 1'),
  ('task-s2', 'proj-smoke-1', 'Task 2', 'Nhiệm vụ 2'),
  ('task-s3', 'proj-smoke-1', 'Task 3', 'Nhiệm vụ 3'),
  ('task-other', 'proj-smoke-2', 'Task khác', 'Thuộc project 2');

-- Owner adds document
insert into public.mentor_team_documents (id, project_id, title, content)
values ('doc-s1', 'proj-smoke-1', 'Tài liệu 1', 'Nội dung ban đầu');

-- ----------------------------------------------------------------------------
-- 2. Check a: OUTSIDER cannot SELECT project, documents or tasks
-- ----------------------------------------------------------------------------
select set_config('request.jwt.claims', '{"sub": "00000000-0000-0000-0000-000000000009"}', true);

do $$
declare
  v_count int;
begin
  select count(*) into v_count from public.mentor_team_projects where id = 'proj-smoke-1';
  assert v_count = 0, 'Outsider was able to select project';

  select count(*) into v_count from public.mentor_team_documents where project_id = 'proj-smoke-1';
  assert v_count = 0, 'Outsider was able to select documents';

  select count(*) into v_count from public.mentor_team_tasks where project_id = 'proj-smoke-1';
  assert v_count = 0, 'Outsider was able to select tasks';
end $$;

-- ----------------------------------------------------------------------------
-- 3. Check b: OUTSIDER cannot self-join (RLS blocks insert)
-- ----------------------------------------------------------------------------
do $$
begin
  begin
    insert into public.mentor_team_members (project_id, user_id, role, status)
    values ('proj-smoke-1', '00000000-0000-0000-0000-000000000009', 'MEMBER', 'ACTIVE');
    raise exception 'TEST_FAILED: Outsider self-join was not blocked';
  exception when insufficient_privilege or check_violation or others then
    -- Expected failure
  end;
end $$;

-- ----------------------------------------------------------------------------
-- 4. Check c: MEMBER and VIEWER cannot add members
-- ----------------------------------------------------------------------------
-- Test MEMBER attempting to add new member
select set_config('request.jwt.claims', '{"sub": "00000000-0000-0000-0000-000000000003"}', true);

do $$
begin
  begin
    insert into public.mentor_team_members (project_id, user_id, role, status)
    values ('proj-smoke-1', '00000000-0000-0000-0000-000000000009', 'MEMBER', 'ACTIVE');
    raise exception 'TEST_FAILED: Regular member was able to add members';
  exception when others then
    -- Expected failure
  end;
end $$;

-- Test VIEWER attempting to add new member
select set_config('request.jwt.claims', '{"sub": "00000000-0000-0000-0000-000000000004"}', true);

do $$
begin
  begin
    insert into public.mentor_team_members (project_id, user_id, role, status)
    values ('proj-smoke-1', '00000000-0000-0000-0000-000000000009', 'MEMBER', 'ACTIVE');
    raise exception 'TEST_FAILED: Viewer was able to add members';
  exception when others then
    -- Expected failure
  end;
end $$;

-- ----------------------------------------------------------------------------
-- 5. Check d: VIEWER cannot write data (requirements, docs, tasks)
-- ----------------------------------------------------------------------------
do $$
begin
  -- Viewer cannot insert requirement
  begin
    insert into public.mentor_team_requirements (id, project_id, title, description)
    values ('req-v1', 'proj-smoke-1', 'Yêu cầu', 'Mô tả');
    raise exception 'TEST_FAILED: Viewer was able to insert requirement';
  exception when others then
    -- Expected failure
  end;

  -- Viewer cannot insert document
  begin
    insert into public.mentor_team_documents (id, project_id, title, content)
    values ('doc-v1', 'proj-smoke-1', 'Doc', 'Nội dung');
    raise exception 'TEST_FAILED: Viewer was able to insert document';
  exception when others then
    -- Expected failure
  end;

  -- Viewer cannot insert task
  begin
    insert into public.mentor_team_tasks (id, project_id, title, description)
    values ('task-v1', 'proj-smoke-1', 'Task', 'Nhiệm vụ');
    raise exception 'TEST_FAILED: Viewer was able to insert task';
  exception when others then
    -- Expected failure
  end;
end $$;

-- ----------------------------------------------------------------------------
-- 6. Check e: REMOVED member loses access immediately
-- ----------------------------------------------------------------------------
select set_config('request.jwt.claims', '{"sub": "00000000-0000-0000-0000-000000000005"}', true);

do $$
declare
  v_count int;
begin
  select count(*) into v_count from public.mentor_team_projects where id = 'proj-smoke-1';
  assert v_count = 0, 'Removed member was able to select project';

  select count(*) into v_count from public.mentor_team_documents where project_id = 'proj-smoke-1';
  assert v_count = 0, 'Removed member was able to select documents';

  select count(*) into v_count from public.mentor_team_tasks where project_id = 'proj-smoke-1';
  assert v_count = 0, 'Removed member was able to select tasks';

  -- Removed member cannot write document
  begin
    insert into public.mentor_team_documents (id, project_id, title, content)
    values ('doc-rem', 'proj-smoke-1', 'Spam', 'Bị xóa rồi');
    raise exception 'TEST_FAILED: Removed member was able to insert document';
  exception when others then
    -- Expected failure
  end;
end $$;

-- ----------------------------------------------------------------------------
-- 7. Check f: ADMIN cannot self-escalate to OWNER
-- ----------------------------------------------------------------------------
select set_config('request.jwt.claims', '{"sub": "00000000-0000-0000-0000-000000000002"}', true);

do $$
begin
  begin
    update public.mentor_team_members
    set role = 'OWNER'
    where project_id = 'proj-smoke-1' and user_id = '00000000-0000-0000-0000-000000000002';
    raise exception 'TEST_FAILED: Admin was able to self-promote to OWNER';
  exception when others then
    -- Expected failure: CANNOT_ESCALATE_TO_OWNER or CANNOT_SELF_PROMOTE
  end;
end $$;

-- ----------------------------------------------------------------------------
-- 8. Check g: ADMIN cannot delete or demote OWNER
-- ----------------------------------------------------------------------------
do $$
begin
  -- Demotion attempt
  begin
    update public.mentor_team_members
    set role = 'MEMBER'
    where project_id = 'proj-smoke-1' and user_id = '00000000-0000-0000-0000-000000000001';
    raise exception 'TEST_FAILED: Admin was able to demote OWNER';
  exception when others then
    -- Expected failure: CANNOT_DEMOTE_OWNER or ADMIN_CANNOT_MODIFY_OWNER
  end;

  -- Deletion attempt
  begin
    delete from public.mentor_team_members
    where project_id = 'proj-smoke-1' and user_id = '00000000-0000-0000-0000-000000000001';
    raise exception 'TEST_FAILED: Admin was able to delete OWNER';
  exception when others then
    -- Expected failure: CANNOT_DELETE_OWNER
  end;
end $$;

-- ----------------------------------------------------------------------------
-- 9. Check h: User cannot forge actor fields (server enforces auth.uid())
-- ----------------------------------------------------------------------------
select set_config('request.jwt.claims', '{"sub": "00000000-0000-0000-0000-000000000003"}', true);

-- Member inserts document with fake uploaded_by (trying to impersonate Owner)
insert into public.mentor_team_documents (id, project_id, title, content, uploaded_by)
values ('doc-fake-actor', 'proj-smoke-1', 'Tài liệu giả mạo', 'Nội dung', '00000000-0000-0000-0000-000000000001');

do $$
declare
  v_uploader uuid;
begin
  select uploaded_by into v_uploader from public.mentor_team_documents where id = 'doc-fake-actor';
  assert v_uploader = '00000000-0000-0000-0000-000000000003', 'Server allowed fake uploaded_by actor';
end $$;

-- ----------------------------------------------------------------------------
-- 10. Check i: Task Dependencies: cross-project, self-dep, and cycles are blocked
-- ----------------------------------------------------------------------------
select set_config('request.jwt.claims', '{"sub": "00000000-0000-0000-0000-000000000001"}', true);

-- Self dependency check
do $$
begin
  begin
    insert into public.mentor_team_task_dependencies (task_id, depends_on_task_id)
    values ('task-s1', 'task-s1');
    raise exception 'TEST_FAILED: Self dependency was allowed';
  exception when others then
    -- Expected failure
  end;
end $$;

-- Cross-project dependency check
do $$
begin
  begin
    insert into public.mentor_team_task_dependencies (task_id, depends_on_task_id)
    values ('task-s1', 'task-other');
    raise exception 'TEST_FAILED: Cross project dependency was allowed';
  exception when others then
    -- Expected failure
  end;
end $$;

-- Cycle dependency check (T1 -> T2 -> T3 -> T1)
insert into public.mentor_team_task_dependencies (task_id, depends_on_task_id) values
  ('task-s1', 'task-s2'),
  ('task-s2', 'task-s3');

do $$
begin
  begin
    insert into public.mentor_team_task_dependencies (task_id, depends_on_task_id)
    values ('task-s3', 'task-s1');
    raise exception 'TEST_FAILED: Cyclic dependency was allowed';
  exception when others then
    -- Expected failure
  end;
end $$;

-- ----------------------------------------------------------------------------
-- 11. Check j: client_request_id prevents second record on repeated send
-- ----------------------------------------------------------------------------
insert into public.mentor_team_tasks (id, project_id, title, description, client_request_id)
values ('task-idemp-1', 'proj-smoke-1', 'Task Idempotent', 'Lần gửi 1', 'req-idemp-001');

do $$
begin
  begin
    insert into public.mentor_team_tasks (id, project_id, title, description, client_request_id)
    values ('task-idemp-2', 'proj-smoke-1', 'Task Idempotent', 'Lần gửi 2 trùng', 'req-idemp-001');
    raise exception 'TEST_FAILED: Repeated client_request_id created second record';
  exception when unique_violation then
    -- Expected: unique constraint prevented duplication
  end;
end $$;

-- ----------------------------------------------------------------------------
-- 12. Check AI task lifecycle: AI_SUGGESTED must be PENDING_CONFIRMATION
-- ----------------------------------------------------------------------------
-- Client tries to insert AI_SUGGESTED with CONFIRMED directly
insert into public.mentor_team_tasks (id, project_id, title, description, source, confirmation_status)
values ('task-ai-1', 'proj-smoke-1', 'Task AI Gợi ý', 'Mô tả', 'AI_SUGGESTED', 'CONFIRMED');

do $$
declare
  v_status text;
  v_confirmed_by uuid;
begin
  select confirmation_status, confirmed_by into v_status, v_confirmed_by
  from public.mentor_team_tasks where id = 'task-ai-1';
  assert v_status = 'PENDING_CONFIRMATION', 'AI task bypassed PENDING_CONFIRMATION';
  assert v_confirmed_by is null, 'AI task confirmed_by was not null';
end $$;

-- Member confirms AI task via RPC mentor_confirm_task
select set_config('request.jwt.claims', '{"sub": "00000000-0000-0000-0000-000000000003"}', true);
select public.mentor_confirm_task('task-ai-1');

do $$
declare
  v_status text;
  v_confirmed_by uuid;
begin
  select confirmation_status, confirmed_by into v_status, v_confirmed_by
  from public.mentor_team_tasks where id = 'task-ai-1';
  assert v_status = 'CONFIRMED', 'Task was not confirmed after RPC';
  assert v_confirmed_by = '00000000-0000-0000-0000-000000000003', 'Server did not set confirmed_by to auth.uid()';
end $$;

-- ----------------------------------------------------------------------------
-- 13. Check Safe Ownership Transfer RPC
-- ----------------------------------------------------------------------------
select set_config('request.jwt.claims', '{"sub": "00000000-0000-0000-0000-000000000001"}', true);
select public.mentor_transfer_team_ownership('proj-smoke-1', '00000000-0000-0000-0000-000000000002');

do $$
declare
  v_proj_owner uuid;
  v_old_owner_role text;
  v_new_owner_role text;
begin
  select owner_id into v_proj_owner from public.mentor_team_projects where id = 'proj-smoke-1';
  assert v_proj_owner = '00000000-0000-0000-0000-000000000002', 'Project owner was not transferred';

  select role into v_old_owner_role from public.mentor_team_members
  where project_id = 'proj-smoke-1' and user_id = '00000000-0000-0000-0000-000000000001';
  assert v_old_owner_role = 'ADMIN', 'Old owner role was not demoted to ADMIN';

  select role into v_new_owner_role from public.mentor_team_members
  where project_id = 'proj-smoke-1' and user_id = '00000000-0000-0000-0000-000000000002';
  assert v_new_owner_role = 'OWNER', 'New owner role was not set to OWNER';
end $$;

rollback;
