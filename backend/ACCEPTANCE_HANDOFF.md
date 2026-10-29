# Acceptance handoff — 2026-10-29

## Scope and current decision

The initial run was source-only. Later on the same date the user supplied a
temporary key and connected a Redmi 14C. See the live Gemini and physical-device
updates below for current results; earlier pending statements are historical.

## Safe API acceptance runner

`tests/test_api_acceptance.py` executes `verify_api_acceptance.run_tests()`.
It uses FastAPI TestClient and a fresh TemporaryDirectory, not a listening port.
It therefore cannot mistake a running backend on port 18080 for its test server.
The database directory is passed directly through Settings. The fake provider
is explicitly installed on the isolated app instance. The TestClient and
temporary directory are closed in finally. No external Gemini calls are made.

The real role CLI targets the temporary database, revokes the initial token,
and the runner signs in again. It does not directly UPDATE users to grant admin.

Covered API flows:
- Student/admin authorization.
- CSV preview with valid and invalid rows, draft import, publication integrity,
  question/quiz publishing, student practice and wrong-question notebook.
- Import replay returns the same question IDs.
- Exact dailyXp, totalXp and streakDays comparison before/after library practice;
  missing fields fail instead of silently defaulting to zero.
- Exam creation, answer and repeated submission timestamp.
- Published Physics 10 enrollment and path readback (not a complete lesson).
- Fake AI answer, skip, completion and notebook source.

`test_roadmap_regression.py` separately checks actual lesson/placement completion,
historical recovery, and nonzero reward isolation across Library/Exam flows.
These use fixture curricula and are API regression tests, not phone evidence.

Run from the backend directory with its virtualenv Python:

```powershell
$env:AI_PROVIDER_TYPE = 'unconfigured'
.\.venv\Scripts\python.exe -m pytest tests/test_api_acceptance.py tests/test_roadmap_regression.py -q
```

## Evidence boundaries

- Android unit tests: 152 tests, zero failures/errors/skips; fresh rerun on this date.
- Debug APK: assembleDebug successful; no install/uninstall performed.
- Existing Chrome CDP tests: real browser, mock HTTP server. They do not prove
  a complete Web Admin import against the actual FastAPI backend.
- No phone: ADB list empty after the ADB server started successfully. The user
  then confirmed no phone is available. Do not infer a cable/driver defect.
- Gemini: no live calls, no live content-quality or account entitlement claim.
- Backend full suite: 184 passed, zero failures/skips, one Starlette/httpx
  deprecation warning, 156.15 seconds. Includes the new isolated API acceptance.
- Verification logs on this machine:
  `<local-verification-workspace>/final-acceptance-backend.log`
  and `final-acceptance-android.log` in the same directory. These are machine-local
  evidence paths, not runtime configuration or required deployment paths.

## Remaining final-acceptance checklist

1. Connect and authorize a phone. Confirm serial, active app/account and endpoint.
2. Use a separate test backend/database, verifying its identity before any writes.
   Never replace a real backend on 18080 automatically. If occupied, agree a safe
   transition first. Keep the user's established reverse-port configuration.
3. In a real browser, log into that backend, import and publish a test batch.
4. On the phone, load that batch, answer and complete, inspect notebook; run a
   short exam and roadmap lesson, background/reopen and test connection recovery.
5. When the key is supplied locally, configure an account-enabled GEMINI_MODEL
   and explicitly approve a small bounded live test. Check the generated answers
   manually. Do not put the key in chat, tracked files or logs.
6. Fix reproducible findings, rerun affected checks and update this handoff.

Do not call all four project stages complete while device/live-AI checks remain.
No commit, push, reset, stash, database migration or deployment is part of this run.

## Live Gemini verification update

User explicitly authorized a temporary Gemini key and small live verification.
No key was saved in source, configuration files or this report.

- Model listing authenticated successfully. Listing a model did not guarantee
  generation access: gemini-2.5-flash-lite returned HTTP 404 (unavailable to new
  users) and recommended gemini-3.5-flash-lite. A separate gemini-3.8-flash probe
  timed out; this does not prove that model is generally unavailable.
- Successful model: gemini-3.5-flash-lite, 45-second request timeout for this probe.
- Successful isolated session: three Physics 10 uniform-motion questions, HTTP
  201; one generation plus one explanation call. Three earlier generation probes
  failed (including a minimal diagnostic request); billing was not measured.
- Answers hidden initially; create replay returned the same session without a
  second generation call. Answer, skip, notebook source AI_PRACTICE, completion
  and identical completion replay all passed through the product API.
- Manual review of all three generated questions: SI velocity unit m/s;
  distance s=v*t; 20 m/s for 5 s gives 100 m. Correct options and explanations
  matched these facts. This does not establish academic quality for all topics.
- Evidence without credentials:
  `<local-verification-workspace>/live-ai-verification.json`.
- Physical device remains pending: Windows reports Xiaomi MTP (serial
  <redmi-test-device>), but ADB still lists no devices despite the user's report of
  enabling debugging. No app install or live-database mutation was performed.

The temporary key was shared in chat; rotate/revoke it after verification.
The earlier no-live-calls statements describe the previous test run only.

## Physical device and real browser update

- Device: Redmi 14C, serial <redmi-test-device>, authorized after resetting USB
  debugging authorization. App and test APK installed with `adb install -r`;
  application data was not cleared. USB reverse remains 18080 -> 18080.
- Database: separate `work/phone-acceptance-1029` directory under the local Codex
  workspace. The original database was not used. Test accounts were created on
  phone/backend; the prior ActiveUser and endpoint preference are restored in
  finally. Test account records remain separate from the user's account.
- Chrome real DOM -> actual FastAPI: sign-in, file upload/preview, publish two
  CSV questions, create/publish `PHONE_OCT29 Web to Phone` quiz. No mock fetch or
  mock API server was used. The admin was provisioned by the real role CLI on
  the temporary database. Browser used a separate temporary profile.
- On the physical device, the product LibraryPracticeScreen displayed that
  exact quiz (Compose semantics assertion). Subsequent answer/completion,
  notebook retry, Exam and AI demo flows use the actual Android repositories
  and HTTP transport, not simulated UI button clicks for every step.
- Library: two answers, one correct, completed-session readback, wrong question
  with four options and successful retry; dailyXp/totalXp/streakDays unchanged.
- Exam: published two-question exam, hidden answers before submission, save,
  submit and repeated submit with unchanged submittedAt; no roadmap rewards.
- Roadmap: complete the actual published introductory Physics 10 placement
  (zero XP), then first lesson (200 XP), read completion back; totalXp=200 and
  streakDays=1. Answer fixtures come from the current introductory course pack.
- AI demo on device: generated session, answer, skip, completion. This is Fake;
  live Gemini was separately tested through the backend as documented above.
- Genuine outage: stopped only this run's verified test-backend process, observed
  phone transport failure, restarted with the same test database, and read the
  same session successfully. Merely removing adb reverse was insufficient to
  close already pooled HTTP connections; that earlier probe was rejected.
- Device suite: final explicit-fixture run passed 14 tests, no failures or skips,
  in 24.106s (an earlier integrated run also passed in 24.075s).
  Includes nine controlled-client tests, three Activity lifecycle scenarios,
  one real path roundtrip and the Web-published integration case.
- Fixed a test fixture: Chat-return scenario now seeds the saved attempt before
  bind, asserts restoration, then checks reload. The original failing assertions
  for attempt ID, step, revision, questions and answers remain intact.
- The WebPublishedPhoneTest is opt-in via instrumentation argument
  `acceptanceFixture=PHONE_OCT29`; it requires the published fixture and a host
  watching PHONE_NETWORK_DROP_READY / PHONE_NETWORK_RESTORE_READY. Default
  instrumentation must not run this controlled outage against arbitrary data.

Evidence in the local workspace `work` directory: `verify-real-web.js`,
`phone-live-flow.log`, `phone-instrumentation-final.log`, `device-web-build.log`,
and `live-ai-verification.json`. No API key is contained in those artifacts.

This is bounded integration acceptance, not exhaustive visual testing of every
screen, all nine subjects, or a production deployment. No commit/push was made.

All four agreed work groups are closed for the bounded acceptance above: safer
API tests, physical-device integration, live Gemini sample and documented handoff.
The isolated backend is stopped after the final run; the installed debug app,
prior account/endpoint preference and USB reverse mapping remain. To run normal
app data again, start the user's normal backend separately using the documented
environment configuration. The verification key is not persisted for runtime.
