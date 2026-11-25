begin;

-- The library RPC owns answer persistence and scoring. The authenticated role
-- must not read raw question_snapshots or write score/answers on its own.
revoke all on table public.mentor_practice_sessions from public, anon, authenticated;

-- The notebook remains readable for its owner's Realtime subscription, but
-- changes to answers and review state must pass through mentor_library_api.
revoke insert, update, delete on table public.mentor_wrong_questions
  from public, anon, authenticated;

commit;
