# Shared Supabase administrative catalog

Verified 2026-11-05 against group project hnxphwuaccztzanjcuca.

## Enabled scope

Web Admin questions, quizzes, exams, import preview and import confirmation use
Supabase. Web Admin login and role checks now use group Supabase Auth; local Admin tokens are rejected. Service
credentials never enter the browser or Android build. The runtime reads
backend/.env.supabase.local; environment variables override that file.
SUPABASE_SECRET_KEY is preferred; SUPABASE_SERVICE_ROLE_KEY is also accepted.
MENTOR_ADMIN_CATALOG=sqlite explicitly opts into the local development catalog.
There is no automatic local fallback when Supabase is unavailable.

## Transaction behavior

SupabaseCatalog loads an atomic cloud catalog snapshot and runs the existing
LibraryService/ExamService validation in a disposable SQLite policy workspace.
This workspace contains catalog data and one synthetic actor, no real users or
learner progress. Only changed rows are sent back to Supabase. A server-only
SECURITY INVOKER RPC verifies the snapshot has not changed and commits the batch
atomically; concurrent catalog edits return 409 and require reloading.
Import retries are recorded in mentor_admin_private.requests. Existing cloud
quizzes with three answer options keep their original embedded question format.
Standalone question listings do not silently promote embedded quiz questions
into the question bank. Local integer administrator ids are not mapped to Auth UUIDs; new cloud-authored records use the verified Supabase administrator UUID.

The SQL source is backend/sql/server_only_admin_catalog_transactions.sql.
Applied cloud migration: 20261105020627_server_only_admin_catalog_transactions.
Only service_role can invoke this RPC; anon/authenticated execution is revoked.
The private request table deliberately has RLS enabled with no client policies.

## Verification performed

- Backend suite: 262 passed, 1 warning (270.02 seconds).
- Final focused adapter/HTTP tests: 11 passed, 1 warning.
- PostgreSQL insert/update of all three catalog types, duplicate-code atomic
  rollback, stale snapshot rejection and import replay/conflict tested inside
  transactions that were rolled back. No QA catalog rows remain.
- Live server restarted on http://127.0.0.1:18080/admin.
- Existing Admin login succeeded. Authenticated HTTP GETs returned 235 questions,
  61 quizzes and 2 exams. GET /admin/storage reports catalog=supabase.
- Unauthenticated admin catalog access returned HTTP 401.
- 200 AIM_PHY10_BANK01 questions remain DRAFT; none were published by this work.
- Server env file is ignored by Git. Changes remain uncommitted and unpushed.
- No browser visual acceptance or Android build/install performed in this change.

## Remaining scope and limitations

This is the administrative catalog/Auth cutover, not a complete learner backend migration.
Local learner login, local learner backend endpoints/session/progress persistence and
uploads still use the existing backend. In particular the backend learner exam
service has not been switched to cloud exam catalogs. The Android cloud
repositories are separate; changing build configuration alone does not install
a newly targeted app on a phone. Do not claim every app flow uses the group
project based on this Admin verification.

At the current catalog size the adapter snapshots the whole catalog per request.
As it grows, replace this compatibility adapter with focused Postgres domain
operations/paginated reads. There is no external network call while cloud write
locks are held. Existing signed-in SECURITY DEFINER RPC advisory findings and
leaked-password-protection settings were not changed. See:
https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable
https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection

The server key was shared in chat: rotate it in the Supabase dashboard, replace
its value only in backend/.env.supabase.local, then restart the backend.
Never commit this private file or paste the replacement key into chat.


## Supabase Web Admin authentication (2026-11-05)

MENTOR_ADMIN_AUTH_PROVIDER=supabase is explicitly selected in the private server
configuration. Dedicated endpoints /admin/auth/sign-in, /admin/auth/refresh,
/admin/auth/me and /admin/auth/sign-out use group Supabase Auth. Learner /auth/*
endpoints were kept separate and are not an administrative authentication fallback.

A server-only identity RPC checks the live auth.sessions row, confirmation,
account ban status and current protected app_metadata.mentor_role. User-editable
metadata and stale roles in access tokens never grant Admin access. Only ADMIN
or TEACHER may access the content administration API. Anonymous/authenticated
clients cannot execute the identity RPC directly. Browser API calls refresh the
session once, share concurrent refreshes and discard late refresh responses after
logout or account changes. Supabase opaque refresh token lengths have their own
input model; the local learner token contract was not weakened.

The user explicitly requested creation of an internal account. Created exactly
one confirmed Supabase Auth user webadmin@ai-mentor.example, with mentor_role=ADMIN.
Existing five user roles were left unchanged. This is a reserved internal address
and cannot receive email/password-reset messages. The random password is stored
only in backend/.env.supabase-admin.local; this file and the server-key file are
both ignored by Git. Do not push, attach or paste either private file into chat.

Live verification using that account succeeded: sign-in 200, verified ADMIN
identity 200, catalog reads 235 questions/61 quizzes/2 exams, refresh 200, sign-out
200 and old access token rejected with 401 immediately after logout. No catalog
QA records were left behind and 200 Physics questions remain DRAFT. Session count
returned to its previous 13 rows after the test (no global logout).

Validation runs: 39 targeted backend/legacy admin/browser tests passed; after the
last provider error handling change 14 Auth tests passed; the new JavaScript auth
runner also passed. Existing Node and Chrome CDP admin tests were included in the
39-test run. Manual browser sign-in and Android installation were not performed.
Changes remain uncommitted/unpushed. The existing service key shared in chat still
needs rotation; the newly generated Admin password was never printed in chat.
