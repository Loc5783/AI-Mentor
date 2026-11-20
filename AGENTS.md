# AIMentor engineering criteria

- Apply SOLID to all new work and substantial changes. Keep UI rendering, application state, domain policy, persistence, and transport in focused components.
- Depend on narrow contracts at boundaries; compose implementations in AppServices. Extend behavior through explicit policies or adapters when useful. Avoid speculative abstractions that add no practical value.
- The user explicitly chose a fresh nine-subject app on 21 October 2026. Remove the old four-module flows, old data imports and old local data. Preserve all subsequent new-app data; never repeat the one-time reset. Retain useful tools in the appropriate subject module.
- Verify changes with meaningful tests for behavior, lifecycle, identity isolation, and failures. Do not treat a successful build as proof that the entire app is complete.
- Git workflow (confirmed again on 21 October 2026): push module/*, feature/* or fix/* branches and open Pull Requests into integration. Leave main merges to the team lead. The user has explicitly adopted this workflow for AIMentor after the earlier mistaken attachment.
- Visual direction: use short, subtle motion lighter than Duolingo. Prefer clear text and restrained transitions over decorative icons. Keep essential navigation/camera/attachment affordances. Never gate learning, rewards or interaction on an animation; respect system animation settings and stop motion when its view is detached.

## Scope and evidence for each task

- Before editing, check the active branch, remotes, open PRs and uncommitted changes. Preserve unrelated work; use a separate `feature/*` or `fix/*` branch and PR into `integration`. The team lead alone decides merges into `main`.
- State the task scope and acceptance criteria before implementation. Keep the diff limited to that scope; record any follow-up work separately instead of silently expanding it.
- Select tests for the changed behavior: Android build/unit test/lint for app code, pytest for FastAPI, Node tests for Edge Functions, and local Supabase migrations plus SQL smoke tests for database changes. Add focused regression tests for logic, identity isolation and failure paths when they are material.
- Never use production data or the production Supabase project as a test fixture. Review migrations and data compatibility before any deployment; preserve all new-app user data and provide a rollback plan for data changes.
- In the PR, report exactly what passed, failed or was not run, with commands or CI links. Distinguish code present, local verification, cloud verification and deployment. A successful build alone does not establish that a feature works end to end.
- Keep credentials out of Git and CI logs. CI runs without production secrets and must fail when a required check fails; do not use `continue-on-error` for required checks.
