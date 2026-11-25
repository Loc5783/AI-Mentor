# TeamOS project file cleanup

The Android client records all project file paths before deleting the project row. The record survives the project cascade. After deletion, both the client and the server worker can remove the Storage objects and then delete the cleanup record. Failed deletions leave the record for retry. The worker skips records whose project still exists.

Deploy `202611250001_teamos_file_storage.sql` and `202611250002_team_file_cleanup.sql` before shipping the Android change. Deploy the `mentor-team-cleanup` Edge Function with JWT verification disabled. Set `TEAM_CLEANUP_SECRET` to a random secret; keep it outside Git. Supabase supplies `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` to the function. Invoke `POST /functions/v1/mentor-team-cleanup` with the `x-team-cleanup-secret` header on a trusted server schedule. A successful response contains `completed` and `failed`; HTTP 503 means at least one record remains for retry. Never place the cleanup secret or service role key in the Android app.

The function is not scheduled by this repository. Configure a trusted scheduler after deploying it, and monitor the remaining row count in `public.mentor_team_file_cleanup`. Until that schedule exists, the app retries when the owner opens the TeamOS project list. An old app version that deletes projects without creating cleanup records cannot be repaired retroactively by this worker.

Rollback: stop the scheduler and redeploy the previous app. Keep the cleanup table and Storage policies while any cleanup records remain; removing them early would strand files. The migration does not delete existing user data.
