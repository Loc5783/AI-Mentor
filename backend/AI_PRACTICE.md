# AI Practice operation and safety policy

`AI_PROVIDER_TYPE` accepts `auto` (default), `gemini`, `fake`, or `unconfigured`.
`auto` uses an existing server-side `GEMINI_API_KEY`; without it, only explicit
`MENTOR_DEMO_MODE=1` selects the fake provider. Otherwise AI is unavailable.
`fake` is a demonstration, not verified educational content. The Android setup
screen labels this explicitly. No real provider requests run in default tests.

Generation clients should send a stable `Idempotency-Key` across transport
retries. An atomic SQLite reservation prevents overlapping paid calls even
across service instances. A second in-flight request gets
`409 AI_REQUEST_IN_PROGRESS`; after completion it receives the cached session.
Changing the payload under the same key returns `409 IDEMPOTENCY_CONFLICT`.
If a process dies during generation, that key stays uncertain and is not
automatically replayed. Start a new operation explicitly instead. A reservation
stops counting toward concurrent-call limits after 120 seconds, but remains
recorded for replay protection and quota. Generation does not hold a database
transaction while waiting for the provider.

Limits are persisted in `ai_provider_operations`: 20 generation calls and 50
follow-up explanations per account over a rolling 24 hours; at most one active
call per account and four across the server/database. Failed calls count too,
because the upstream provider might have performed billable work. Cached
generation responses do not consume additional quota. These are application
limits, not a substitute for provider billing limits.

An answer may include `submissionId`. Retrying that submission returns its
original result without incrementing attempts; using its ID for different
content is rejected. Deliberately practicing again creates a new submission.
Legacy clients repeating the current identical selection are deduplicated.
The initial correctness flag is immutable across later practice.

Full follow-up explanations require a revealed question (answered/skipped), or
a completed session. The backend enforces this even for direct API calls.

Database schema initialization adds the operation ledger without deleting
existing data. Validate deployment migrations against a backup before upgrading
a real installation. This change was tested only with temporary databases.

## Cross-feature integration

Wrong and skipped AI questions enter the shared notebook with source
`AI_PRACTICE`. Its public `options` field uses the same array contract as library
questions; stored AI snapshots may still use a letter-keyed object. Notebook
practice does not rewrite the AI session's first-attempt statistics or grant
roadmap rewards. Replaying session completion preserves its original summary.

`accuracyFirstTry` is a percentage from 0 through 100, not a fraction. Android
displays that percentage without multiplying it again. The Android repository
keeps an active-session pointer scoped to endpoint/account. Returning to setup
offers resumption, which reads fresh server state rather than generating a new
quiz. The pointer is removed when completion is confirmed. No answer key or
provider secret is stored with that pointer.

The AI controller owns a child coroutine scope. Resetting/changing questions
invalidates old callbacks; disposing the screen cancels only its owned jobs,
never the caller's scope. These behaviors are covered by JVM tests, not a claim
of physical-device lifecycle acceptance.

## Gemini Provider Security & Model Policy

1. **Header-Only Authentication**:
   - `GEMINI_API_KEY` is sent exclusively via the `x-goog-api-key` HTTP header.
   - The key is strictly omitted from the request URL (`?key=...`) to avoid exposure in network access logs and error traces.
   - Error messages, exceptions, and server logs do not include the raw key.

2. **Model Policy (Explicit Configuration Required)**:
   - When enabling Gemini (`AI_PROVIDER_TYPE=gemini` or supplying `GEMINI_API_KEY`), `GEMINI_MODEL` is mandatory.
   - Google Developer API model availability varies depending on the account, creation date, and project entitlement. For example, legacy projects may access `gemini-2.5-flash`, whereas new projects are assigned newer options (such as `gemini-3.8-flash` or `gemini-3.5-flash-lite`).
   - Because no single model name is guaranteed to exist across all accounts, no implicit hard-coded default is assumed; users must supply the model identifier enabled in their Google AI Studio project.
   - Provider types `fake` and `unconfigured` require neither `GEMINI_API_KEY` nor `GEMINI_MODEL`.

3. **Strict Failure Boundary**:
   - Upstream Gemini errors (401, 429, 500, timeout, safety filter blocks) surface corresponding error codes to the client.
   - The service never silently falls back to the demo/fake provider upon encountering a Gemini error.
