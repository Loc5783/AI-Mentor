# Gemini server key failover

Configured 2026-11-05 for group project hnxphwuaccztzanjcuca. No provider keys
are bundled in Android, source files, test fixtures or this document.

## Configuration

- Four authorization keys are encrypted in Supabase Vault under the unique name
  mentor_gemini_config. Only service_role can read/update them through the two
  mentor_ai_server_config/mentor_set_ai_server_config RPCs. Anonymous and normal
  authenticated users cannot invoke either or read vault.decrypted_secrets.
- Supabase mentor-ai Edge Function version 2 is ACTIVE, with verify_jwt=true.
  Existing gateway and in-function Supabase Auth verification were preserved.
- Local backend loads backend/.env.gemini.local (ignored by Git): GEMINI_API_KEYS
  is a JSON array, GEMINI_MODEL is gemini-3.1-flash-lite. Explicit environment
  variables override private file settings. Never share/commit the private file.
- Edge configuration is cached per worker for up to 60 seconds. Vault is
  authoritative; indexed env keys or GEMINI_API_KEYS are only legacy fallbacks
  when Vault configuration does not exist. Vault outages fail closed.

## Behavior

Healthy key stays active. Recognized invalid/expired key errors (including Google
HTTP 400 API_KEY_INVALID), 401/403, 429, selected 5xx and network/timeouts switch
to the next eligible key. Every request tries at most four distinct keys.
Per-worker cooldowns: authentication 300s, quota 60s, transient failure 15s;
Retry-After can extend the pause. States reset on worker/process restart.
Edge provider work has a 45s total deadline; backend uses its 15s total provider
budget. Deadlines can stop retries before all slots are tried. Successful answer
parsing, safety rejection, malformed output, invalid input/model errors are not
retried with another key. No infinite retries or raw provider/key error logs.

The app user quota is still reserved once per chat request. Key failover never
bypasses that quota. Gemini quotas are per Google Cloud project, so keys in the
same Google project still share quota:
https://aistudio.google.com/docs/rate-limits

## Verification

All four keys could list Gemini models and access the configured model metadata.
One live chat through the public app publishable key and an authenticated ES256
user token returned HTTP 200 and OK. This proves real inference for the active
key, not remaining generation quota for all four keys.

34 targeted Python/legacy AI tests passed. The final 13 Node tests passed,
covering fourth-key fallback, auth/network/quota/server failures, bounded retries,
Retry-After, Vault priority/caching, auth rejection, safety and one user-quota
reservation. Fault injection used fake keys; no real faults were manufactured.
Backend was restarted; /ai-practice/config reports configured=true, provider=gemini.
A scan of Git-eligible changed/new files found none of the four actual keys.
No APK rebuild is required for server-only changes. Phone AI practice flows were
not re-tested end-to-end in this change. Changes remain uncommitted/unpushed.
