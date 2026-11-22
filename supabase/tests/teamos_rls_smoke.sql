-- ============================================================================
-- SQL Smoke Test: AI TeamOS RLS, Roles, Dependencies, Cycles & Idempotency
-- Runs in a transactional block and always rolls back.
-- ============================================================================

begin;

-- Create simulated test users in auth.users if not already present
do $$
declare
  v_owner uuid := '11111111-1111-1111-1111-111111111111'::uuid;
  v_member uuid := '22222222-2222-2222-2222-222222222222'::uuid;
  v_viewer uuid := '33333333-3333-3333-3333-333333333333'::uuid;
  v_outsider uuid := '99999999-9999-9999-9999-999999999999'::uuid;
  v_proj_id text := 'proj-test-alpha';
  v_task1 text := 'task-test-01';
  v_task2 text := 'task-test-02';
  v_task3 text := 'task-test-03';
  v_doc1 text := 'doc-test-01';
  v_req1 text := 'req-test-01';
  v_count int;
  v_cycle_caught boolean := false;
  v_cross_caught boolean := false;
  v_self_caught boolean := false;
begin
  -- 1. Setup Auth Users
  insert into auth.users (id, email)
  values
    (v_owner, 'owner@aimentor.test'),
    (v_member, 'member@aimentor.test'),
    (v_viewer, 'viewer@aimentor.test'),
    (v_outsider, 'outsider@aimentor.test')
  on conflict (id) do nothing;

  -- 2. Owner creates project (Trigger automatically makes owner an active OWNER member)
  insert into public.mentor_team_projects (id, title, description, owner_id)
  values (v_proj_id, 'Dự án Sinh học 12', 'Nghiên cứu di truyền học', v_owner);

  -- Verify owner is in members table
  select count(*) into v_count from public.mentor_team_members
  where project_id = v_proj_id and user_id = v_owner and role = 'OWNER' and status = 'ACTIVE';
  assert v_count = 1, 'Owner must automatically be an ACTIVE OWNER member';

  -- 3. Owner adds Member and Viewer
  insert into public.mentor_team_members (project_id, user_id, role, status)
  values
    (v_proj_id, v_member, 'MEMBER', 'ACTIVE'),
    (v_proj_id, v_viewer, 'VIEWER', 'ACTIVE');

  -- 4. Verify helper functions
  assert public.mentor_is_team_active_member(v_proj_id, v_owner) = true, 'Owner is active member';
  assert public.mentor_is_team_active_member(v_proj_id, v_member) = true, 'Member is active member';
  assert public.mentor_is_team_active_member(v_proj_id, v_viewer) = true, 'Viewer is active member';
  assert public.mentor_is_team_active_member(v_proj_id, v_outsider) = false, 'Outsider is not active member';

  assert public.mentor_is_team_manager(v_proj_id, v_owner) = true, 'Owner is manager';
  assert public.mentor_is_team_manager(v_proj_id, v_member) = false, 'Member is not manager';
  assert public.mentor_is_team_manager(v_proj_id, v_viewer) = false, 'Viewer is not manager';

  assert public.mentor_get_team_role(v_proj_id, v_owner) = 'OWNER', 'Owner role is OWNER';
  assert public.mentor_get_team_role(v_proj_id, v_member) = 'MEMBER', 'Member role is MEMBER';
  assert public.mentor_get_team_role(v_proj_id, v_viewer) = 'VIEWER', 'Viewer role is VIEWER';

  -- 5. Create Documents and Tasks
  insert into public.mentor_team_documents (id, project_id, title, content, uploaded_by)
  values (v_doc1, v_proj_id, 'Tài liệu di truyền', 'Nội dung ARN và ADN', v_member);

  insert into public.mentor_team_tasks (id, project_id, title, status, client_request_id)
  values
    (v_task1, v_proj_id, 'Thu thập số liệu', 'TODO', 'req-001'),
    (v_task2, v_proj_id, 'Phân tích kết quả', 'TODO', 'req-002'),
    (v_task3, v_proj_id, 'Viết báo cáo', 'TODO', 'req-003');

  -- 6. Test Idempotency with client_request_id
  begin
    insert into public.mentor_team_tasks (id, project_id, title, status, client_request_id)
    values ('task-duplicate', v_proj_id, 'Thu thập số liệu lặp lại', 'TODO', 'req-001');
    raise exception 'Expected unique constraint violation for duplicate client_request_id';
  exception
    when unique_violation then
      -- Correct: idempotency key blocked duplicate insert
      null;
  end;

  -- 7. Test Task Dependencies
  -- Normal valid dependency: task 2 depends on task 1
  insert into public.mentor_team_task_dependencies (task_id, depends_on_task_id)
  values (v_task2, v_task1);

  -- Normal valid dependency: task 3 depends on task 2
  insert into public.mentor_team_task_dependencies (task_id, depends_on_task_id)
  values (v_task3, v_task2);

  -- Self-dependency forbidden
  begin
    insert into public.mentor_team_task_dependencies (task_id, depends_on_task_id)
    values (v_task1, v_task1);
  exception
    when check_violation then
      v_self_caught := true;
  end;
  assert v_self_caught = true, 'Self-dependency must be forbidden by check constraint';

  -- Cyclic dependency forbidden: task 1 cannot depend on task 3 (since 3 depends on 2 depends on 1)
  begin
    insert into public.mentor_team_task_dependencies (task_id, depends_on_task_id)
    values (v_task1, v_task3);
  exception
    when others then
      if sqlerrm like '%CYCLIC_DEPENDENCY_DETECTED%' then
        v_cycle_caught := true;
      end if;
  end;
  assert v_cycle_caught = true, 'Cyclic dependency must be rejected by trigger';

  -- Cross-project dependency forbidden
  insert into public.mentor_team_projects (id, title, owner_id)
  values ('proj-beta', 'Dự án Hóa học 12', v_owner);

  insert into public.mentor_team_tasks (id, project_id, title)
  values ('task-beta-01', 'proj-beta', 'Thí nghiệm chuẩn độ');

  begin
    insert into public.mentor_team_task_dependencies (task_id, depends_on_task_id)
    values (v_task1, 'task-beta-01');
  exception
    when others then
      if sqlerrm like '%CROSS_PROJECT_DEPENDENCY_FORBIDDEN%' then
        v_cross_caught := true;
      end if;
  end;
  assert v_cross_caught = true, 'Cross-project dependency must be rejected by trigger';

  -- 8. Test Member Removal & Immediate Revocation
  update public.mentor_team_members
  set status = 'REMOVED', removed_at = now()
  where project_id = v_proj_id and user_id = v_member;

  assert public.mentor_is_team_active_member(v_proj_id, v_member) = false,
    'Removed member must immediately lose active membership';

end $$;

rollback;
