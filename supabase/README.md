# Supabase — AI Mentor

Supabase is the cloud path when `SUPABASE_ENABLED=true`. The app keeps the old FastAPI path available as a fallback when Supabase is disabled. Course lesson content is intentionally unchanged in this UI/backend integration pass.

## Connected app areas

- **Accounts:** email sign-up/sign-in, email confirmation, refresh/restore session, sign-out, password recovery and password change. Android session tokens are stored using Android Keystore and remain separate from legacy backend sessions.
- **Profile and setup:** cloud profile nickname, selected grade/subjects, and profile lookup/avatar retrieval through the social API.
- **Learning overview:** selected-subject progress, XP, streak, weekly activity and rank tier are read from `mentor_overview`; lesson/quiz actions use `mentor_learning_api`.
- **Competition:** ranking data is read through `mentor_social_api`.
- **Friends:** global-rank entries can send requests; the competition screen lists accepted friends and incoming/outgoing requests, accepts/declines, and removes friends with revision checks. Profile displays accepted friends' current streaks.
- **Study ledger and badges:** `mentor_study_action` records short, bounded, idempotent active-study intervals; `mentor_study_summary` aggregates recorded seconds; `mentor_badges` unlocks badges only from completed lesson/XP/streak facts. Profile reads the actual study total and badge states, with explicit empty/unavailable states.
- **Practice library:** published quiz metadata, private answer-key snapshots, resumable sessions and wrong-answer retry are served by `mentor_library_api`; practice awards no XP or streak. Migration `202610290018_seed_practice_from_lessons.sql` builds 10–15 question review quizzes for complete five-lesson groups from the existing Lý–Hóa–Sinh lesson bank.
- **Schedule:** uses the Supabase schedule repository when cloud mode is enabled.
- **Chat:** folders, conversations, drafts, messages and attachments use the Supabase chat store; realtime chat changes trigger a refresh.
- **Realtime refresh:** profile, enrollment, learning, friendship, chat and schedule table changes are translated into app refresh events.
- **AI:** the `mentor-ai` Edge Function authenticates the bearer session, applies account quota, adapts to grade/language, and calls Gemini with server-side keys. `config.toml` keeps `verify_jwt = true`; it must be deployed and its Gemini secrets configured before cloud AI chat works.

## Still incomplete

- The seed creates review quizzes only for complete five-lesson groups that contain 10–15 valid questions. If a subject/grade has no such group, the practice library correctly remains empty for that selection.
- Friend discovery is currently through the global ranking list; QR invitations and nickname search are not supported yet.
- Chat data is cloud-backed when Supabase is enabled; verify the storage bucket and migration `202610290007_chat_storage.sql` are deployed for attachments.
- Foreground lesson/review attempts send bounded study intervals; the profile reads `mentor_study_summary` and `mentor_badges`. These sections show an explicit empty/unavailable message if the cloud contract is unavailable.
- Live Supabase auth/recovery, ranking and realtime behavior have not all been exercised against the user's project from this checkout.
- Gemini cloud chat cannot be verified until the Edge Function is deployed and Gemini secrets are present.

## Configuration and secrets

- `supabase.properties` contains only the project URL and public publishable key; never put a secret or `service_role` key in the app or Git.
- Use the ignored local override `supabase.local.properties` for developer-specific values. Do not commit that file.
- Keep `SUPABASE_ENABLED=false` to use the legacy backend path.
- Configure Gemini keys only as Supabase Edge Function secrets, never in Android resources, source code, or Git.

## Deploying Supabase changes

This checkout has no Supabase CLI session attached, so database migrations and Edge Functions have not been deployed from here. First compare remote migration history, then apply only pending migrations in ascending order. For the function, link the project `hnxphwuaccztzanjcuca` and deploy `mentor-ai`; keep JWT verification enabled. Add `GEMINI_API_KEY_1` (and optional `_2`, `_3`) through Supabase Edge Function Secrets. Never place those values in this repository, Android config, or chat. Set `GEMINI_MODEL` only if selecting a model other than the function default. Afterward run `tests/cloud_learning_smoke.sql` and verify the function with an authenticated app session.

## Database migrations

Apply migrations in `migrations/` in ascending order. `202610290001_foundation.sql` was previously confirmed as applied; verify the Supabase migration history before applying anything so an already-applied migration is not run twice. This checkout contains migrations through `202610290018_seed_practice_from_lessons.sql`; the app expects the RPC/table contracts from the relevant migrations to exist.

The latest overview RPC counts progress only for selected subjects and uses the existing rank thresholds: Silver 500 XP, Gold 2,000, Platinum 5,000 and Diamond 10,000. No XP or activity is fabricated for empty accounts.

After applying migrations, the SQL smoke scripts in `tests/` can be run from the Supabase SQL editor. `cloud_learning_smoke.sql` creates temporary test rows inside a transaction and rolls them back; it checks answer privacy, grading, wrong-answer retry, no practice XP, study interval bounds/idempotency and the badge catalog.

## Local verification

From the repository root:

```powershell
.\\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

The build verifies Android compilation and local unit tests. It does not prove that migrations are applied remotely, that Supabase email delivery is configured, or that the Edge Function and Gemini secrets are deployed.
